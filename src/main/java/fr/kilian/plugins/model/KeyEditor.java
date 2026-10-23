package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class KeyEditor implements InventoryHolder {
    public static final int NAME_SLOT = 19;
    public static final int LORE_SLOT = 21;
    public static final int PREVIEW_SLOT = 22;
    public static final int DONE_SLOT = 49;
    public static final int CLEAR_SLOT = 51;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final Crate crate;
    private final MessageService messages;
    private final Inventory inventory;
    private ItemStack key;

    public KeyEditor(Crate crate) {
        this.crate = Objects.requireNonNull(crate, "crate");
        messages = CrateCreator.getInstance().messages();
        key = crate.key();
        boolean configured = key != null;
        if(key == null) key = new ItemStack(Material.PAPER);
        normalizeKeyText();
        if(configured) crate.setKey(key);
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 54, messages.text("gui.key-editor-title"));
        refresh();
    }

    public void selectBaseItem(ItemStack item) {
        ItemStack selected = item.clone();
        selected.setAmount(1);
        ItemMeta currentMeta = key.getItemMeta();
        ItemMeta selectedMeta = selected.getItemMeta();
        if(currentMeta.hasDisplayName()) {
            selectedMeta.displayName(currentMeta.displayName().decoration(TextDecoration.ITALIC, false));
        }
        if(currentMeta.hasLore()) selectedMeta.lore(currentMeta.lore());
        selected.setItemMeta(selectedMeta);
        key = selected;
        normalizeKeyText();
        crate.setKey(key);
        refresh();
    }

    public void setName(String name) {
        ItemMeta meta = key.getItemMeta();
        if(name.isBlank()) {
            meta.displayName(null);
        } else {
            meta.displayName(MINI_MESSAGE.deserialize(name).decoration(TextDecoration.ITALIC, false));
        }
        key.setItemMeta(meta);
        normalizeKeyText();
        crate.setKey(key);
        refresh();
    }

    public void setLore(String lore) {
        ItemMeta meta = key.getItemMeta();
        if(lore.isBlank()) {
            meta.lore(null);
        } else {
            List<Component> lines = Arrays.stream(lore.split("\\|", -1))
                    .map(String::trim)
                    .map(MINI_MESSAGE::deserialize)
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList();
            meta.lore(lines);
        }
        if(meta.hasDisplayName()) {
            meta.displayName(meta.displayName().decoration(TextDecoration.ITALIC, false));
        }
        key.setItemMeta(meta);
        normalizeKeyText();
        crate.setKey(key);
        refresh();
    }

    public void clearKey() {
        crate.setKey(null);
        key = new ItemStack(Material.PAPER);
        refresh();
    }

    public Crate crate() {
        return crate;
    }

    public void refresh() {
        normalizeKeyText();
        inventory.clear();
        ItemStack pane = button(Material.BLACK_STAINED_GLASS_PANE, "gui.pane");
        for(int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);

        inventory.setItem(NAME_SLOT, button(Material.NAME_TAG, "gui.key-editor-name"));
        inventory.setItem(LORE_SLOT, button(Material.BOOK, "gui.key-editor-lore"));
        inventory.setItem(PREVIEW_SLOT, key.clone());
        ItemStack instructions = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta instructionMeta = instructions.getItemMeta();
        instructionMeta.displayName(messages.text("gui.key-editor-select-info")
                .decoration(TextDecoration.ITALIC, false));
        instructionMeta.lore(List.of(messages.text("gui.key-editor-select-help")
                .decoration(TextDecoration.ITALIC, false)));
        instructions.setItemMeta(instructionMeta);
        inventory.setItem(13, instructions);
        inventory.setItem(DONE_SLOT, button(Material.LIME_CONCRETE, "gui.key-editor-done"));
        inventory.setItem(CLEAR_SLOT, button(Material.BARRIER, "gui.key-editor-clear"));
    }

    private void normalizeKeyText() {
        ItemMeta meta = key.getItemMeta();
        if(meta.hasDisplayName()) {
            meta.displayName(meta.displayName().decoration(TextDecoration.ITALIC, false));
        }
        if(meta.hasLore()) {
            meta.lore(meta.lore().stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .toList());
        }
        key.setItemMeta(meta);
    }

    private ItemStack button(Material material, String messageKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text(messageKey).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
