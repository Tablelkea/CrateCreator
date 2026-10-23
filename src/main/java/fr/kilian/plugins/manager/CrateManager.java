package fr.kilian.plugins.manager;

import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.CrateCreator;
import org.bukkit.Bukkit;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public class CrateManager {
    private final Map<Location, Crate> cratesByLocation = new HashMap<>();
    private final Map<String, Crate> cratesById = new HashMap<>();
    private final Map<String, Crate> cratesByKeyId = new HashMap<>();
    private final Map<Crate, List<TextDisplay>> holograms = new IdentityHashMap<>();
    private final File crateFolder = new File(CrateCreator.getInstance().getDataFolder(), "crates");
    private final NamespacedKey crateKeyIdTag = new NamespacedKey(CrateCreator.getInstance(), "crate_key_id");
    private static final DateTimeFormatter HOLOGRAM_DATE = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    public boolean createCrate(Crate crate) {
        return createCrate(crate, BlockFace.SOUTH);
    }

    public boolean createCrate(Crate crate, BlockFace facing) {
        String id = normalizeId(crate.id());
        Location location = normalizeLocation(crate.location());
        if(id.isEmpty() || cratesById.containsKey(id) || cratesByLocation.containsKey(location)
                || !location.getBlock().getType().isAir()) {
            return false;
        }
        placeChest(location, facing);
        cratesByLocation.put(location, crate);
        cratesById.put(id, crate);
        cratesByKeyId.put(crate.keyId(), crate);
        refreshHologram(crate);
        saveCrate(crate);
        return true;
    }

    public Crate getCrateById(String id) {
        if(id == null) return null;
        return cratesById.get(normalizeId(id));
    }

    public List<String> getCrateIds() {
        return cratesById.keySet().stream().sorted().toList();
    }

    public List<Crate> getCrates() {
        return cratesById.values().stream()
                .sorted((first, second) -> first.id().compareToIgnoreCase(second.id()))
                .toList();
    }

    public boolean updateCrateId(Crate crate, String newId) {
        String normalizedId = normalizeId(newId);
        if(normalizedId.isEmpty()) return false;
        Crate existing = cratesById.get(normalizedId);
        if(existing != null && existing != crate) return false;

        cratesById.remove(normalizeId(crate.id()));
        crate.setId(newId);
        cratesById.put(normalizedId, crate);
        saveCrate(crate);
        return true;
    }

    public boolean updateCrateLocation(Crate crate, Location location) {
        return updateCrateLocation(crate, location, BlockFace.SOUTH);
    }

    public boolean updateCrateLocation(Crate crate, Location location, BlockFace facing) {
        Location oldLocation = normalizeLocation(crate.location());
        Location newLocation = normalizeLocation(location);
        if(oldLocation.equals(newLocation)) {
            placeChest(newLocation, facing);
            refreshHologram(crate);
            saveCrate(crate);
            return true;
        }
        if(cratesByLocation.containsKey(newLocation) || !newLocation.getBlock().getType().isAir()) {
            return false;
        }

        cratesByLocation.remove(oldLocation);
        oldLocation.getBlock().setType(Material.AIR);
        crate.setLocation(location);
        placeChest(newLocation, facing);
        cratesByLocation.put(newLocation, crate);
        refreshHologram(crate);
        saveCrate(crate);
        return true;
    }

    public static BlockFace horizontalFacing(float yaw) {
        float normalizedYaw = (yaw % 360 + 360) % 360;
        if(normalizedYaw < 45 || normalizedYaw >= 315) return BlockFace.SOUTH;
        if(normalizedYaw < 135) return BlockFace.WEST;
        if(normalizedYaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private void placeChest(Location location, BlockFace facing) {
        location.getBlock().setType(Material.CHEST);
        setChestFacing(location, facing);
    }

    private void setChestFacing(Location location, BlockFace facing) {
        BlockData data = location.getBlock().getBlockData();
        if(data instanceof Directional directional) {
            directional.setFacing(facing);
            location.getBlock().setBlockData(directional, false);
        }
    }

    public void refreshHologram(Crate crate) {
        removeHologram(crate);
        if(crate == null || cratesById.get(normalizeId(crate.id())) != crate) return;

        Location base = crate.location();
        if(base.getWorld() == null) return;
        Location nameLocation = base.clone().add(0.5, 2.15, 0.5);
        Location createdLocation = base.clone().add(0.5, 1.55, 0.5);
        Location expirationLocation = base.clone().add(0.5, 1.25, 0.5);
        var messages = CrateCreator.getInstance().messages();
        List<TextDisplay> displays = new ArrayList<>(3);
        displays.add(spawnText(nameLocation, messages.textParsed("hologram.name",
                Map.of("name", crate.name())), 2.0f));
        displays.add(spawnText(createdLocation, messages.textParsed("hologram.created", Map.of(
                "date", HOLOGRAM_DATE.format(crate.createdAt())
        ))));
        Component expirationText = crate.expiresAt() == null
                ? messages.text("hologram.no-expiration")
                : messages.textParsed("hologram.expires", Map.of(
                        "date", HOLOGRAM_DATE.format(crate.expiresAt())
                ));
        displays.add(spawnText(expirationLocation, expirationText));
        holograms.put(crate, displays);
    }

    private TextDisplay spawnText(Location location, Component text) {
        return spawnText(location, text, 1.0f);
    }

    private TextDisplay spawnText(Location location, Component text, float scale) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setShadowed(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            display.setPersistent(false);
        });
    }

    public void removeHologram(Crate crate) {
        List<TextDisplay> displays = holograms.remove(crate);
        if(displays != null) displays.forEach(TextDisplay::remove);
    }

    public void clearHolograms() {
        holograms.values().forEach(displays -> displays.forEach(TextDisplay::remove));
        holograms.clear();
    }

    public void refreshHologramsInChunk(World world, int chunkX, int chunkZ) {
        for(Crate crate : getCrates()) {
            Location location = crate.location();
            if(location.getWorld() == world && (location.getBlockX() >> 4) == chunkX
                    && (location.getBlockZ() >> 4) == chunkZ) {
                refreshHologram(crate);
            }
        }
    }

    public void refreshLoadedHolograms() {
        for(Crate crate : getCrates()) {
            Location location = crate.location();
            if(location.getWorld() != null && location.getWorld().isChunkLoaded(
                    location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                refreshHologram(crate);
            }
        }
    }

    public boolean hasHologram(Crate crate) {
        List<TextDisplay> displays = holograms.get(crate);
        return displays != null && displays.size() == 3
                && displays.stream().allMatch(display -> display.isValid() && !display.isDead());
    }

    public void updateOnlineKeys(Crate crate) {
        for(Player player : Bukkit.getOnlinePlayers()) {
            notifyKeyRefresh(player, crate, refreshPlayerKeys(player, crate));
        }
    }

    public void refreshPlayerKeys(Player player) {
        for(Crate crate : getCrates()) {
            notifyKeyRefresh(player, crate, refreshPlayerKeys(player, crate));
        }
    }

    public boolean isCrateKey(ItemStack item) {
        return crateForKey(item) != null;
    }

    public void refreshPlayerKeys(Player player, ItemStack transferredKey) {
        Crate crate = crateForKey(transferredKey);
        if(crate != null) notifyKeyRefresh(player, crate, refreshPlayerKeys(player, crate));
    }

    private Crate crateForKey(ItemStack item) {
        if(item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String keyId = item.getItemMeta().getPersistentDataContainer()
                .get(crateKeyIdTag, PersistentDataType.STRING);
        return keyId == null ? null : cratesByKeyId.get(keyId);
    }

    private boolean refreshPlayerKeys(Player player, Crate crate) {
        ItemStack configuredKey = crate.key();
        boolean updated = updateKeyInventory(player, player.getInventory(), crate, configuredKey);
        updated |= updateKeyInventory(player, player.getEnderChest(), crate, configuredKey);
        updated |= updateKeyCursor(player, crate, configuredKey);
        return updated;
    }

    private boolean updateKeyCursor(Player player, Crate crate, ItemStack configuredKey) {
        ItemStack current = player.getItemOnCursor();
        if(!crate.isKeyItem(current)) return false;
        if(configuredKey == null) {
            player.setItemOnCursor(null);
            return true;
        }
        int maxStackSize = Math.max(1, configuredKey.getMaxStackSize());
        if(current.isSimilar(configuredKey) && current.getAmount() <= maxStackSize) return false;
        int amount = current.getAmount();
        ItemStack replacement = configuredKey.clone();
        replacement.setAmount(Math.min(amount, maxStackSize));
        player.setItemOnCursor(replacement);
        amount -= replacement.getAmount();
        while(amount > 0) {
            ItemStack remainder = configuredKey.clone();
            int stackAmount = Math.min(amount, maxStackSize);
            remainder.setAmount(stackAmount);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(remainder);
            overflow.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
            amount -= stackAmount;
        }
        return true;
    }

    private void notifyKeyRefresh(Player player, Crate crate, boolean updated) {
        if(!updated) return;
        String message = crate.key() == null
                ? "chat.crate.key-invalidated" : "chat.crate.key-refreshed";
        CrateCreator.getInstance().messages().send(player, message, Map.of("crate", crate.name()));
    }

    private boolean updateKeyInventory(Player player, org.bukkit.inventory.Inventory inventory,
                                       Crate crate, ItemStack configuredKey) {
        int keyCount = 0;
        int maxStackSize = configuredKey == null ? 0 : Math.max(1, configuredKey.getMaxStackSize());
        ItemStack[] contents = inventory.getContents();
        for(int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if(!crate.isKeyItem(current)) continue;
            if(configuredKey != null && current.isSimilar(configuredKey)
                    && current.getAmount() <= maxStackSize) continue;
            keyCount += current.getAmount();
            inventory.setItem(slot, null);
        }
        if(keyCount == 0) return false;
        if(configuredKey == null) return true;

        while(keyCount > 0) {
            ItemStack replacement = configuredKey.clone();
            int stackAmount = Math.min(keyCount, maxStackSize);
            replacement.setAmount(stackAmount);
            Map<Integer, ItemStack> overflow = inventory.addItem(replacement);
            overflow.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
            keyCount -= stackAmount;
        }
        return true;
    }

    public void loadCrates() {
        if(!crateFolder.exists() && !crateFolder.mkdirs()) {
            CrateCreator.getInstance().getLogger().severe("Unable to create crate data folder: " + crateFolder);
            return;
        }
        File[] files = crateFolder.listFiles((folder, name) -> name.endsWith(".yml"));
        if(files == null) return;
        for(File file : files) {
            try {
                loadCrate(file);
            } catch(RuntimeException exception) {
                CrateCreator.getInstance().getLogger().warning(
                        "Unable to load crate file " + file.getName() + ": " + exception.getMessage());
            }
        }
    }

    private void loadCrate(File file) {
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        String worldName = data.getString("world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if(world == null) {
            CrateCreator.getInstance().getLogger().warning("Skipping crate " + file.getName()
                    + ": world '" + worldName + "' is not loaded.");
            return;
        }

        Location location = new Location(world, data.getInt("x"), data.getInt("y"), data.getInt("z"));
        String id = data.getString("id");
        String name = data.getString("name");
        if(id == null || name == null) throw new IllegalArgumentException("Missing crate id or name");
        String keyId = data.getString("key-id", UUID.randomUUID().toString());
        String persistenceId = data.getString("persistence-id", file.getName().replaceFirst("\\.yml$", ""));
        if(persistenceId == null || !persistenceId.matches("[a-zA-Z0-9_-]+")) {
            persistenceId = file.getName().replaceFirst("\\.yml$", "");
        }
        Instant createdAt = Instant.ofEpochMilli(data.getLong("created-at", System.currentTimeMillis()));
        Crate crate = new Crate(location, name, id, keyId, persistenceId, createdAt);

        String particleName = data.getString("particle");
        if(particleName != null && !particleName.isBlank()) crate.setParticle(Particle.valueOf(particleName));
        crate.setOpenMessage(data.getString("open-message"));
        crate.setBroadcastOpenMessage(data.getBoolean("broadcast-open-message", false));
        if(data.contains("expires-at")) crate.setExpiresAt(Instant.ofEpochMilli(data.getLong("expires-at")));
        ItemStack key = data.getItemStack("key");
        if(key != null) crate.setKey(key);
        ItemStack[] contents = new ItemStack[45];
        for(int slot = 0; slot < contents.length; slot++) {
            contents[slot] = data.getItemStack("contents." + slot);
        }
        crate.setContents(contents);

        String facingName = data.getString("facing", "SOUTH");
        BlockFace facing;
        try {
            facing = BlockFace.valueOf(facingName == null ? "SOUTH" : facingName);
            if(facing.getModX() == 0 && facing.getModZ() == 0) facing = BlockFace.SOUTH;
        } catch(IllegalArgumentException exception) {
            facing = BlockFace.SOUTH;
        }
        String normalizedId = normalizeId(id);
        Location normalizedLocation = normalizeLocation(location);
        if(normalizedId.isEmpty() || cratesById.containsKey(normalizedId)
                || cratesByLocation.containsKey(normalizedLocation)) {
            throw new IllegalArgumentException("Duplicate crate id or location");
        }
        if(location.getBlock().getType() == Material.CHEST) setChestFacing(location, facing);
        else if(location.getBlock().getType().isAir()) placeChest(location, facing);
        else throw new IllegalArgumentException("Crate location is occupied");
        cratesById.put(normalizedId, crate);
        cratesByLocation.put(normalizedLocation, crate);
        cratesByKeyId.put(crate.keyId(), crate);
        updateOnlineKeys(crate);
        refreshHologram(crate);
        saveCrate(crate);
    }

    public void saveCrate(Crate crate) {
        if(crate == null || cratesById.get(normalizeId(crate.id())) != crate) return;
        if(!crateFolder.exists() && !crateFolder.mkdirs()) {
            CrateCreator.getInstance().getLogger().severe("Unable to create crate data folder: " + crateFolder);
            return;
        }
        Location location = crate.location();
        if(location.getWorld() == null) return;
        YamlConfiguration data = new YamlConfiguration();
        data.set("persistence-id", crate.persistenceId());
        data.set("id", crate.id());
        data.set("name", crate.name());
        data.set("world", location.getWorld().getName());
        data.set("x", location.getBlockX());
        data.set("y", location.getBlockY());
        data.set("z", location.getBlockZ());
        BlockData blockData = location.getBlock().getBlockData();
        data.set("facing", blockData instanceof Directional directional
                ? directional.getFacing().name() : BlockFace.SOUTH.name());
        data.set("key-id", crate.keyId());
        data.set("created-at", crate.createdAt().toEpochMilli());
        data.set("expires-at", crate.expiresAt() == null ? null : crate.expiresAt().toEpochMilli());
        data.set("particle", crate.particle() == null ? null : crate.particle().name());
        data.set("open-message", crate.openMessage());
        data.set("broadcast-open-message", crate.broadcastOpenMessage());
        data.set("key", crate.key());
        ItemStack[] contents = crate.contents();
        for(int slot = 0; slot < contents.length; slot++) {
            data.set("contents." + slot, contents[slot]);
        }
        try {
            data.save(crateFile(crate));
        } catch(IOException exception) {
            CrateCreator.getInstance().getLogger().severe("Unable to save crate " + crate.id()
                    + ": " + exception.getMessage());
        }
    }

    private File crateFile(Crate crate) {
        String persistenceId = crate.persistenceId();
        if(!persistenceId.matches("[a-zA-Z0-9_-]+")) {
            persistenceId = UUID.randomUUID().toString();
        }
        return new File(crateFolder, persistenceId + ".yml");
    }

    public boolean deleteCrate(Crate crate) {
        if(crate == null || cratesById.get(normalizeId(crate.id())) != crate) return false;
        Location location = normalizeLocation(crate.location());
        cratesById.remove(normalizeId(crate.id()));
        cratesByKeyId.remove(crate.keyId());
        cratesByLocation.remove(location);
        removeHologram(crate);
        if(location.getWorld() != null) location.getBlock().setType(Material.AIR);
        File file = crateFile(crate);
        if(file.exists() && !file.delete()) {
            CrateCreator.getInstance().getLogger().warning("Unable to delete crate data file " + file.getName());
        }
        return true;
    }

    public boolean isCrate(Location location) {
        return cratesByLocation.containsKey(normalizeLocation(location));
    }

    public Crate getCrate(Location location) {
        return cratesByLocation.get(normalizeLocation(location));
    }

    private String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private Location normalizeLocation(Location location) {
        return location.getBlock().getLocation();
    }

}
