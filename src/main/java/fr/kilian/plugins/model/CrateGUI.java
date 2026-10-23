package fr.kilian.plugins.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CrateGUI implements CrateInterface {
    public static final int INFO_SLOT = 13;
    public static final int ID_SLOT = 19;
    public static final int PARTICLE_SLOT = 20;
    public static final int NAME_SLOT = 21;
    public static final int KEY_SLOT = 22;
    public static final int MESSAGE_SLOT = 23;
    public static final int LOCATION_SLOT = 24;
    public static final int REWARDS_SLOT = 25;
    public static final int BROADCAST_SLOT = 31;
    public static final int DELETE_SLOT = 34;
    public static final int EXPIRATION_SLOT = 29;

    private final Crate crate;
    private final Inventory inventory;

    public CrateGUI(Plugin plugin, Crate crate, int size, String title) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(crate, "crate");
        Objects.requireNonNull(title, "title");

        if(size < 27 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 from 27 to 54");
        }

        this.inventory = plugin.getServer().createInventory(
                this,
                size,
                messages().textParsed("gui.edit-title", java.util.Map.of("name", title))
        );
        populate();
    }

    private void populate() {
        inventory.clear();
        ItemStack pane = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                messages().text("gui.pane"),
                List.of()
        );
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if(row == 0 || row == inventory.getSize() / 9 - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, pane);
            }
        }

        inventory.setItem(INFO_SLOT, createInfoItem());
        inventory.setItem(ID_SLOT, createItem(
                Material.NAME_TAG,
                messages().text("gui.id-title"),
                List.of(
                        messages().text("gui.value", java.util.Map.of("value", crate.id())),
                        messages().text("gui.click-edit")
                )
        ));
        inventory.setItem(PARTICLE_SLOT, createItem(
                Material.ENDER_PEARL,
                messages().text("gui.particle-title"),
                List.of(
                        messages().text("gui.selection", java.util.Map.of("value", particleName(crate.particle()))),
                        messages().text("gui.click-change")
                )
        ));
        inventory.setItem(NAME_SLOT, createItem(
                Material.OAK_SIGN,
                messages().text("gui.name-title"),
                List.of(
                        messages().text("gui.value", java.util.Map.of("value", crate.name())),
                        messages().text("gui.click-edit")
                )
        ));
        inventory.setItem(KEY_SLOT, createItem(
                crate.key() == null ? Material.BARRIER : crate.key().getType(),
                messages().text(crate.key() == null ? "gui.no-key-title" : "gui.key-title"),
                List.of(
                        messages().text("gui.object", java.util.Map.of("value",
                                crate.key() == null ? messages().plain("gui.unconfigured") : crate.key().getType().name())),
                        messages().text("gui.set-key"),
                        messages().text("gui.clear-key")
                )
        ));
        inventory.setItem(MESSAGE_SLOT, createItem(
                Material.PAPER,
                messages().text("gui.message-title"),
                List.of(
                        messages().text("gui.value", java.util.Map.of("value",
                                crate.openMessage() == null || crate.openMessage().isBlank()
                                        ? messages().plain("gui.no-value") : crate.openMessage())),
                        messages().text("gui.click-edit")
                )
        ));
        inventory.setItem(LOCATION_SLOT, createItem(
                Material.COMPASS,
                messages().text("gui.location-title"),
                List.of(
                        messages().text("gui.value", java.util.Map.of("value", locationDescription(crate.location()))),
                        messages().text("gui.move-crate")
                )
        ));
        inventory.setItem(REWARDS_SLOT, createItem(
                Material.CHEST,
                messages().text("gui.rewards-button-title"),
                List.of(
                        messages().text("gui.items-count", java.util.Map.of("value", Integer.toString(rewardCount()))),
                        messages().text("gui.edit-content")
                )
        ));
        inventory.setItem(BROADCAST_SLOT, createItem(
                crate.broadcastOpenMessage() ? Material.BELL : Material.LEVER,
                messages().text(crate.broadcastOpenMessage()
                        ? "gui.broadcast-enabled-title" : "gui.broadcast-disabled-title"),
                List.of(messages().text(crate.broadcastOpenMessage()
                        ? "gui.broadcast-enabled-description" : "gui.broadcast-disabled-description"))
        ));
        inventory.setItem(DELETE_SLOT, createItem(
                Material.TNT,
                messages().text("gui.delete-crate-title"),
                List.of(messages().text("gui.delete-crate-help"))
        ));
        inventory.setItem(EXPIRATION_SLOT, createItem(
                Material.CLOCK,
                messages().text("gui.expiration-title"),
                List.of(
                        messages().textParsed("gui.expiration-created", java.util.Map.of(
                                "date", java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                        .withZone(java.time.ZoneId.systemDefault()).format(crate.createdAt())
                        )),
                        messages().textParsed("gui.expiration-value", java.util.Map.of(
                                "date", crate.expiresAt() == null ? messages().plain("gui.no-expiration")
                                        : java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                                        .withZone(java.time.ZoneId.systemDefault()).format(crate.expiresAt())
                        )),
                        messages().text("gui.expiration-change")
                )
        ));
    }

    public void refresh() {
        populate();
    }

    private ItemStack createInfoItem() {
        Location location = crate.location();
        List<Component> lore = new ArrayList<>();
        lore.add(messages().text("gui.name", java.util.Map.of("value", crate.name())));
        lore.add(messages().text("gui.id", java.util.Map.of("value", crate.id())));
        lore.add(messages().text("gui.position", java.util.Map.of("value", location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ())));
        lore.add(messages().text("gui.world", java.util.Map.of("value",
                location.getWorld() == null ? messages().plain("gui.unknown-world") : location.getWorld().getName())));
        lore.add(messages().text("gui.particle", java.util.Map.of("value", particleName(crate.particle()))));
        lore.add(messages().text("gui.message", java.util.Map.of("value",
                crate.openMessage() == null || crate.openMessage().isBlank()
                        ? messages().plain("gui.no-value") : crate.openMessage())));
        lore.add(messages().text("gui.key", java.util.Map.of("value",
                crate.key() == null ? messages().plain("gui.no-value") : crate.key().getType().name())));
        return createItem(
                Material.ENDER_EYE,
                messages().text("gui.preview-title"),
                lore
        );
    }

    private ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if(!lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private MessageService messages() {
        return CrateCreator.getInstance().messages();
    }

    private String particleName(Particle particle) {
        return particle == null ? messages().plain("gui.no-particle") : particle.name();
    }

    private String locationDescription(Location location) {
        String world = location.getWorld() == null
                ? messages().plain("gui.unknown-world")
                : location.getWorld().getName();
        return world + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private int rewardCount() {
        int count = 0;
        for(ItemStack item : crate.contents()) {
            if(item != null && !item.getType().isAir()) count++;
        }
        return count;
    }

    @Override
    public String crateId() {
        return crate.id();
    }

    @Override
    public Type type() {
        return Type.EDIT;
    }

    @Override
    public Crate crate() {
        return crate;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
