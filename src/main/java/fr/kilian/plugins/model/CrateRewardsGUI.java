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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CrateRewardsGUI implements CrateInterface {
    public static final int REWARD_SIZE = 45;
    public static final int ADD_REWARD_SLOT = 49;
    public static final int QUIT_SLOT = 53;

    private final Crate crate;
    private final Inventory inventory;

    public CrateRewardsGUI(Plugin plugin, Crate crate) {
        Objects.requireNonNull(plugin, "plugin");
        this.crate = Objects.requireNonNull(crate, "crate");
        inventory = plugin.getServer().createInventory(
                this,
                54,
                messages().textParsed("gui.rewards-title", java.util.Map.of("name", crate.name()))
        );
        refresh();
    }

    public void refresh() {
        inventory.clear();
        ItemStack[] contents = crate.contents();
        for(int slot = 0; slot < REWARD_SIZE; slot++) {
            inventory.setItem(slot, decorateReward(slot, contents[slot]));
        }

        ItemStack pane = button(Material.BLACK_STAINED_GLASS_PANE, messages().text("gui.pane"));
        for(int slot = REWARD_SIZE; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane);
        }
        inventory.setItem(ADD_REWARD_SLOT, button(
                Material.EMERALD,
                messages().text("gui.add-reward")
        ));
        inventory.setItem(QUIT_SLOT, button(
                Material.BARRIER,
                messages().text("gui.finish")
        ));
    }

    private ItemStack decorateReward(int slot, ItemStack reward) {
        if(reward == null || reward.getType().isAir()) return reward;
        ItemStack displayed = reward.clone();
        ItemMeta meta = displayed.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(messages().text("gui.reward-chance", Map.of(
                "value", String.format(Locale.US, "%.2f", crate.rewardWeight(slot))
        )).decoration(TextDecoration.ITALIC, false));
        lore.add(messages().text("gui.reward-weight-help").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        displayed.setItemMeta(meta);
        return displayed;
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
        return Type.REWARDS;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
