package fr.kilian.plugins.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CrateContentGUI implements CrateInterface {
    public static final int KEY_SLOT = 49;
    public static final int QUIT_SLOT = 53;
    private static final int CONTENT_SIZE = 45;

    private final Crate crate;
    private final Inventory inventory;

    public CrateContentGUI(Plugin plugin, Crate crate, int size, String title) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(crate, "crate");
        Objects.requireNonNull(title, "title");
        if(size != 54) {
            throw new IllegalArgumentException("Crate content GUI must have exactly 54 slots");
        }

        inventory = plugin.getServer().createInventory(
                this,
                size,
                messages().textParsed("gui.content-title", java.util.Map.of("name", title))
        );
        refresh();
    }

    public void refresh() {
        inventory.clear();
        ItemStack[] contents = crate.contents();
        for(int slot = 0; slot < CONTENT_SIZE; slot++) {
            inventory.setItem(slot, withRewardChance(slot, contents[slot]));
        }

        ItemStack pane = createButton(
                Material.BLACK_STAINED_GLASS_PANE,
                messages().text("gui.pane")
        );
        for(int slot = CONTENT_SIZE; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }

        ItemStack key = crate.key();
        inventory.setItem(KEY_SLOT, key == null
                ? createButton(
                        Material.BARRIER,
                        messages().text("gui.no-key")
                )
                : key);
        inventory.setItem(QUIT_SLOT, createButton(
                Material.BARRIER,
                messages().text("gui.quit")
        ));
    }

    private ItemStack withRewardChance(int slot, ItemStack item) {
        if(item == null || item.getType().isAir()) return item;
        ItemStack displayed = item.clone();
        ItemMeta meta = displayed.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(messages().text("gui.reward-chance", Map.of(
                "value", String.format(Locale.US, "%.2f", crate.rewardChance(slot))
        )).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        displayed.setItemMeta(meta);
        return displayed;
    }

    private ItemStack createButton(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private MessageService messages() {
        return CrateCreator.getInstance().messages();
    }

    @Override
    public String crateId() {
        return crate.id();
    }

    @Override
    public Crate crate() {
        return crate;
    }

    @Override
    public Type type() {
        return Type.CONTENT;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
