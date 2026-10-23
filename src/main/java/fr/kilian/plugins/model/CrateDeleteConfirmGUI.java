package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;

public final class CrateDeleteConfirmGUI implements InventoryHolder {
    public static final int CONFIRM_SLOT = 11;
    public static final int CANCEL_SLOT = 15;

    private final Crate crate;
    private final Inventory inventory;

    public CrateDeleteConfirmGUI(Crate crate) {
        this.crate = Objects.requireNonNull(crate, "crate");
        MessageService messages = CrateCreator.getInstance().messages();
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 27, messages.text("gui.delete-confirm-title"));
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, button(Material.BLACK_STAINED_GLASS_PANE, messages.text("gui.pane"), null));
        }
        inventory.setItem(CONFIRM_SLOT, button(Material.LIME_CONCRETE,
                messages.text("gui.delete-confirm-yes"),
                messages.text("gui.delete-confirm-yes-help")));
        inventory.setItem(CANCEL_SLOT, button(Material.RED_CONCRETE,
                messages.text("gui.delete-confirm-no"),
                messages.text("gui.delete-confirm-no-help")));
    }

    private ItemStack button(Material material, net.kyori.adventure.text.Component name,
                             net.kyori.adventure.text.Component lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if(lore != null) meta.lore(List.of(lore.decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    public Crate crate() {
        return crate;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
