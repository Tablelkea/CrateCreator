package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateHistoryManager;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CrateHistoryGUI implements InventoryHolder {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final String playerName;
    private final String crateFilter;
    private final List<CrateHistoryManager.HistoryEntry> entries;
    private final MessageService messages;
    private final Inventory inventory;
    private int page;

    public CrateHistoryGUI(String playerName, String crateFilter,
                           List<CrateHistoryManager.HistoryEntry> entries) {
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.crateFilter = crateFilter;
        this.entries = List.copyOf(entries);
        messages = CrateCreator.getInstance().messages();
        String titleKey = crateFilter == null ? "gui.history-title" : "gui.history-filter-title";
        Map<String, String> titleValues = crateFilter == null
                ? Map.of("player", playerName, "count", Integer.toString(entries.size()))
                : Map.of("player", playerName, "crate", crateFilter,
                        "count", Integer.toString(entries.size()));
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 54, messages.textParsed(titleKey, titleValues));
        refresh();
    }

    public void refresh() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for(int index = start; index < end; index++) {
            inventory.setItem(index - start, entryItem(entries.get(index)));
        }
        inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "gui.history-previous"));
        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "gui.history-close"));
        inventory.setItem(NEXT_SLOT, button(Material.ARROW, "gui.history-next"));
    }

    public void previousPage() {
        if(page > 0) page--;
        refresh();
    }

    public void nextPage() {
        if((page + 1) * PAGE_SIZE < entries.size()) page++;
        refresh();
    }

    private ItemStack entryItem(CrateHistoryManager.HistoryEntry entry) {
        Material material = Material.matchMaterial(entry.material());
        if(material == null || material.isAir()) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        item.setAmount(Math.max(1, Math.min(entry.amount(), material.getMaxStackSize())));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.textParsed("gui.history-reward-title", Map.of(
                "reward", entry.reward()
        )).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                messages.textParsed("gui.history-date", Map.of(
                        "date", DATE_FORMAT.format(Instant.ofEpochMilli(entry.time()))
                )).decoration(TextDecoration.ITALIC, false),
                messages.textParsed("gui.history-crate", Map.of(
                        "crate", entry.crateName(), "id", entry.crateId()
                )).decoration(TextDecoration.ITALIC, false),
                messages.textParsed("gui.history-amount", Map.of(
                        "amount", Integer.toString(entry.amount())
                )).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text(key).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
