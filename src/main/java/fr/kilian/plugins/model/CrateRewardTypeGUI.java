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

import java.util.Objects;

public final class CrateRewardTypeGUI implements CrateInterface {
    public static final int ITEM_SLOT = 20;
    public static final int MONEY_SLOT = 22;
    public static final int EXPERIENCE_SLOT = 24;
    public static final int COMMAND_SLOT = 31;
    public static final int MUSEUM_SLOT = 33;
    public static final int BACK_SLOT = 49;

    private final Crate crate;
    private final Inventory inventory;

    public CrateRewardTypeGUI(Plugin plugin, Crate crate) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(crate, "crate");
        inventory = plugin.getServer().createInventory(
                this,
                54,
                messages().text("gui.reward-types-title")
        );
        populate();
    }

    private void populate() {
        ItemStack pane = button(Material.BLACK_STAINED_GLASS_PANE, messages().text("gui.pane"));
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }

        inventory.setItem(ITEM_SLOT, button(Material.DIAMOND, messages().text("gui.item"),
                messages().text("gui.add-object")));
        inventory.setItem(MONEY_SLOT, button(Material.GOLD_INGOT, messages().text("gui.money"),
                messages().text("gui.economy-required")));
        inventory.setItem(EXPERIENCE_SLOT, button(Material.EXPERIENCE_BOTTLE, messages().text("gui.experience"),
                messages().text("gui.experience-required")));
        inventory.setItem(COMMAND_SLOT, button(Material.COMMAND_BLOCK, messages().text("gui.command"),
                messages().text("gui.command-required")));
        inventory.setItem(MUSEUM_SLOT, button(Material.PAINTING, messages().text("gui.museum"),
                messages().text("gui.museum-required")));
        inventory.setItem(BACK_SLOT, button(Material.ARROW, messages().text("gui.back"),
                messages().text("gui.rewards-back")));
    }

    private ItemStack button(Material material, Component name, Component description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(description.decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, Component name) {
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
        return Type.REWARD_TYPES;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
