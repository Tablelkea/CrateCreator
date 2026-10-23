package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.time.Instant;

public class Crate {

    private Location location;
    private String name;
    private String id;
    private final String persistenceId;
    private final Instant createdAt;
    private Instant expiresAt;
    private final CrateGUI gui;
    private Particle particle;
    private double particleDistance;
    private boolean constantParticle;
    private String openMessage;
    private boolean broadcastOpenMessage;
    private ItemStack key;
    private final String keyId;
    private final NamespacedKey crateKeyIdKey;
    private final NamespacedKey rewardWeightKey;
    private final NamespacedKey commandRewardKey;
    private final ItemStack[] contents = new ItemStack[45];
    private final CrateContentGUI contentGUI;
    private final CrateRewardsGUI rewardsGUI;
    private final CrateRewardTypeGUI rewardTypeGUI;
    private final CrateItemRewardGUI itemRewardGUI;

    public Crate(Location location, String name, String id) {
        this(location, name, id, UUID.randomUUID().toString(), UUID.randomUUID().toString(), Instant.now());
    }

    public Crate(Location location, String name, String id, String keyId,
                 String persistenceId, Instant createdAt) {
        this.location = Objects.requireNonNull(location, "location").getBlock().getLocation().clone();
        this.name = Objects.requireNonNull(name, "name");
        this.id = Objects.requireNonNull(id, "id");
        this.keyId = Objects.requireNonNull(keyId, "keyId");
        this.persistenceId = Objects.requireNonNull(persistenceId, "persistenceId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.particle = null;
        this.particleDistance = 0.8;
        this.constantParticle = false;
        this.openMessage = null;
        this.broadcastOpenMessage = false;
        this.key = null;
        this.rewardWeightKey = new NamespacedKey(CrateCreator.getInstance(), "reward_weight");
        this.commandRewardKey = new NamespacedKey(CrateCreator.getInstance(), "command_reward");
        this.crateKeyIdKey = new NamespacedKey(CrateCreator.getInstance(), "crate_key_id");
        this.gui = new CrateGUI(CrateCreator.getInstance(), this, 54, name);
        this.contentGUI = new CrateContentGUI(CrateCreator.getInstance(), this, 54, name);
        this.rewardsGUI = new CrateRewardsGUI(CrateCreator.getInstance(), this);
        this.rewardTypeGUI = new CrateRewardTypeGUI(CrateCreator.getInstance(), this);
        this.itemRewardGUI = new CrateItemRewardGUI(CrateCreator.getInstance(), this);
    }

    public Location location() {
        return location.clone();
    }

    public String name() {
        return name;
    }

    public String id() {
        return id;
    }

    public String persistenceId() {
        return persistenceId;
    }

    public String keyId() {
        return keyId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        gui.refresh();
        CrateCreator.getInstance().manager().refreshHologram(this);
        persist();
    }

    public boolean isExpired() {
        return expiresAt != null && !Instant.now().isBefore(expiresAt);
    }

    public CrateGUI gui() {
        return gui;
    }

    public CrateContentGUI contentGUI() {
        return contentGUI;
    }

    public CrateRewardsGUI rewardsGUI() {
        return rewardsGUI;
    }

    public CrateRewardTypeGUI rewardTypeGUI() {
        return rewardTypeGUI;
    }

    public CrateItemRewardGUI itemRewardGUI() {
        return itemRewardGUI;
    }

    public Particle particle() {
        return particle;
    }

    public double particleDistance() {
        return particleDistance;
    }

    public void setParticleDistance(double particleDistance) {
        if(!Double.isFinite(particleDistance) || particleDistance < 0.0 || particleDistance > 5.0) {
            throw new IllegalArgumentException("Particle distance must be between 0 and 5");
        }
        this.particleDistance = particleDistance;
        gui.refresh();
        persist();
    }

    public boolean constantParticle() {
        return constantParticle;
    }

    public void setConstantParticle(boolean constantParticle) {
        this.constantParticle = constantParticle;
        gui.refresh();
        persist();
    }

    public String openMessage() {
        return openMessage;
    }

    public boolean broadcastOpenMessage() {
        return broadcastOpenMessage;
    }

    public void setBroadcastOpenMessage(boolean broadcastOpenMessage) {
        this.broadcastOpenMessage = broadcastOpenMessage;
        gui.refresh();
        persist();
    }

    public double rewardWeight(int slot) {
        if(slot < 0 || slot >= contents.length) {
            throw new IndexOutOfBoundsException("Crate reward slot must be between 0 and 44");
        }
        return rewardWeight(contents[slot]);
    }

    public double rewardWeight(ItemStack item) {
        if(item == null || item.getType().isAir()) return 1.0;
        Double weight = item.getItemMeta().getPersistentDataContainer()
                .get(rewardWeightKey, PersistentDataType.DOUBLE);
        return weight == null || !Double.isFinite(weight) || weight <= 0.0 || weight > 100.0
                ? 1.0 : weight;
    }

    public double rewardChance(int slot) {
        return rewardWeight(slot);
    }

    public void setRewardWeight(int slot, double weight) {
        if(slot < 0 || slot >= contents.length) {
            throw new IndexOutOfBoundsException("Crate reward slot must be between 0 and 44");
        }
        if(!Double.isFinite(weight) || weight <= 0.0 || weight > 100.0) {
            throw new IllegalArgumentException("Crate reward chance must be between 0 and 100 percent");
        }
        ItemStack item = contents[slot];
        if(item == null || item.getType().isAir()) {
            throw new IllegalStateException("Cannot set the weight of an empty reward slot");
        }
        double otherChances = 0.0;
        for(int index = 0; index < contents.length; index++) {
            if(index != slot && contents[index] != null && !contents[index].getType().isAir()) {
                otherChances += rewardWeight(contents[index]);
            }
        }
        if(otherChances + weight > 100.0) {
            throw new IllegalArgumentException("Total crate reward chance cannot exceed 100 percent");
        }
        setRewardWeight(item, weight);
        contentGUI.refresh();
        rewardsGUI.refresh();
        persist();
    }

    private void setRewardWeight(ItemStack item, double weight) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(rewardWeightKey, PersistentDataType.DOUBLE, weight);
        item.setItemMeta(meta);
    }

    public ItemStack key() {
        return key == null ? null : key.clone();
    }

    public boolean hasKey(ItemStack item) {
        return key != null && isKeyItem(item);
    }

    public boolean isKeyItem(ItemStack item) {
        if(item == null || item.getType().isAir()) return false;
        String itemKeyId = item.getItemMeta().getPersistentDataContainer()
                .get(crateKeyIdKey, PersistentDataType.STRING);
        return keyId.equals(itemKeyId);
    }

    public ItemStack[] contents() {
        return Arrays.stream(contents)
                .map(item -> item == null ? null : item.clone())
                .toArray(ItemStack[]::new);
    }

    public void setContent(int slot, ItemStack item) {
        if(slot < 0 || slot >= contents.length) {
            throw new IndexOutOfBoundsException("Crate content slot must be between 0 and 44");
        }
        contents[slot] = item == null ? null : item.clone();
        contentGUI.refresh();
        rewardsGUI.refresh();
        gui.refresh();
        persist();
    }

    public boolean addReward(ItemStack item) {
        Objects.requireNonNull(item, "item");
        if(!hasRewardSpace()) return false;
        double totalChance = Arrays.stream(contents)
                .filter(existing -> existing != null && !existing.getType().isAir())
                .mapToDouble(this::rewardWeight)
                .sum();
        if(totalChance + 1.0 > 100.0) return false;
        for(int slot = 0; slot < contents.length; slot++) {
            if(contents[slot] == null || contents[slot].getType().isAir()) {
                contents[slot] = item.clone();
                setRewardWeight(contents[slot], 1.0);
                contentGUI.refresh();
                rewardsGUI.refresh();
                gui.refresh();
                persist();
                return true;
            }
        }
        return false;
    }

    public boolean addCommandReward(String command) {
        String normalizedCommand = Objects.requireNonNull(command, "command").trim();
        if(normalizedCommand.startsWith("/")) normalizedCommand = normalizedCommand.substring(1).trim();
        if(normalizedCommand.isBlank()) throw new IllegalArgumentException("Command cannot be blank");

        ItemStack reward = new ItemStack(org.bukkit.Material.COMMAND_BLOCK);
        ItemMeta meta = reward.getItemMeta();
        var messages = CrateCreator.getInstance().messages();
        meta.displayName(messages.text("gui.command-reward-name").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(messages.text("gui.command-reward-lore-prefix")
                .append(Component.text(normalizedCommand, NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false))
                .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(commandRewardKey, PersistentDataType.STRING, normalizedCommand);
        reward.setItemMeta(meta);
        return addReward(reward);
    }

    public String commandReward(ItemStack item) {
        if(item == null || item.getType().isAir()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(commandRewardKey, PersistentDataType.STRING);
    }

    public boolean hasRewardSpace() {
        return Arrays.stream(contents).anyMatch(item -> item == null || item.getType().isAir());
    }

    public void setContents(ItemStack[] items) {
        Objects.requireNonNull(items, "items");
        if(items.length != contents.length) {
            throw new IllegalArgumentException("Crate must have exactly " + contents.length + " reward slots");
        }
        for(int slot = 0; slot < contents.length; slot++) {
            contents[slot] = items[slot] == null ? null : items[slot].clone();
            if(contents[slot] != null && !contents[slot].getType().isAir()
                    && contents[slot].getItemMeta().getPersistentDataContainer()
                    .has(rewardWeightKey, PersistentDataType.DOUBLE)) {
                continue;
            }
            if(contents[slot] != null && !contents[slot].getType().isAir()) {
                setRewardWeight(contents[slot], 1.0);
            }
        }
        contentGUI.refresh();
        rewardsGUI.refresh();
        gui.refresh();
        persist();
    }

    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location, "location").getBlock().getLocation().clone();
        gui.refresh();
    }

    public void setName(String name) {
        this.name = name;
        gui.refresh();
        contentGUI.refresh();
        CrateCreator.getInstance().manager().refreshHologram(this);
        persist();
    }

    public void setId(String id) {
        this.id = id;
        gui.refresh();
        contentGUI.refresh();
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
        gui.refresh();
        persist();
    }

    public void setOpenMessage(String openMessage) {
        this.openMessage = openMessage;
        gui.refresh();
        persist();
    }

    public void setKey(ItemStack key) {
        ItemStack updatedKey = key == null ? null : key.clone();
        if(updatedKey != null) {
            ItemMeta meta = updatedKey.getItemMeta();
            meta.getPersistentDataContainer().set(crateKeyIdKey, PersistentDataType.STRING, keyId);
            updatedKey.setItemMeta(meta);
        }
        boolean changed = !Objects.equals(this.key, updatedKey);
        this.key = updatedKey;
        if(changed) CrateCreator.getInstance().manager().updateOnlineKeys(this);
        gui.refresh();
        contentGUI.refresh();
        CrateCreator.getInstance().manager().refreshHologram(this);
        persist();
    }

    private void persist() {
        CrateCreator.getInstance().manager().saveCrate(this);
    }

}
