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

import java.util.List;
import java.util.Objects;

public final class CrateItemRewardGUI implements CrateInterface {
    public static final int PREVIEW_SLOT = 22;
    public static final int CONFIRM_SLOT = 48;
    public static final int CANCEL_SLOT = 50;

    private final Crate crate;
    private final Inventory inventory;
    private ItemStack selectedItem;

    public CrateItemRewardGUI(Plugin plugin, Crate crate) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(crate, "crate");
        inventory = plugin.getServer().createInventory(
                this,
                54,
                messages().text("gui.item-picker-title")
        );
        refresh();
    }

    public void select(ItemStack item) {
        selectedItem = item.clone();
        refresh();
    }

    public ItemStack selectedItem() {
        return selectedItem == null ? null : selectedItem.clone();
    }

    public void clearSelection() {
        selectedItem = null;
        refresh();
    }

    private void refresh() {
        inventory.clear();
        ItemStack pane = createButton(
                Material.BLACK_STAINED_GLASS_PANE,
                messages().text("gui.pane"),
                Component.empty()
        );
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }

        if(selectedItem == null) {
            inventory.setItem(PREVIEW_SLOT, createButton(
                    Material.BARRIER,
                    messages().text("gui.no-item-selected"),
                    messages().text("gui.object-select-help")
            ));
        } else {
            inventory.setItem(PREVIEW_SLOT, selectedItem.clone());
        }
        inventory.setItem(CONFIRM_SLOT, createButton(
                Material.LIME_CONCRETE,
                messages().text("gui.confirm"),
                messages().text("gui.confirm-add")
        ));
        inventory.setItem(CANCEL_SLOT, createButton(
                Material.RED_CONCRETE,
                messages().text("gui.cancel"),
                messages().text("gui.cancel-add")
        ));
    }

    private ItemStack createButton(Material material, Component name, Component description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if(!description.equals(Component.empty())) {
            meta.lore(List.of(description.decoration(TextDecoration.ITALIC, false)));
        }
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
        return Type.ITEM_REWARD;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
