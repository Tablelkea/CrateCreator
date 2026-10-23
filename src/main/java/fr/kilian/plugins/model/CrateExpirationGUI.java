package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CrateExpirationGUI implements InventoryHolder {
    public static final int PREVIOUS_MONTH_SLOT = 0;
    public static final int NEXT_MONTH_SLOT = 8;
    public static final int MONTH_SLOT = 9;
    public static final int CLEAR_SLOT = 18;
    public static final int BACK_SLOT = 26;

    private final Crate crate;
    private final MessageService messages;
    private final Inventory inventory;
    private LocalDate month;

    public CrateExpirationGUI(Crate crate) {
        this.crate = Objects.requireNonNull(crate, "crate");
        messages = CrateCreator.getInstance().messages();
        LocalDate now = LocalDate.now();
        month = crate.expiresAt() == null
                ? now.withDayOfMonth(1)
                : crate.expiresAt().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 54, messages.text("gui.expiration-calendar-title"));
        refresh();
    }

    public void refresh() {
        inventory.clear();
        LocalDate today = LocalDate.now();
        int offset = month.getDayOfWeek().getValue() - 1;
        for(int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.withDayOfMonth(day);
            int position = offset + day - 1;
            int row = position / 7;
            int column = position % 7 + 1;
            int slot = row * 9 + column;
            boolean expired = date.isBefore(today);
            ItemStack item = new ItemStack(expired ? Material.GRAY_STAINED_GLASS_PANE
                    : date.equals(today) ? Material.LIME_CONCRETE : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(messages.textParsed("gui.expiration-day", Map.of(
                    "day", Integer.toString(day)
            )).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    messages.textParsed("gui.expiration-weekday", Map.of(
                            "weekday", date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    )).decoration(TextDecoration.ITALIC, false),
                    messages.text(expired ? "gui.expiration-past-day" : "gui.expiration-select-help")
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
        }

        boolean canGoBack = month.isAfter(today.withDayOfMonth(1));
        inventory.setItem(PREVIOUS_MONTH_SLOT, button(canGoBack ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
                "gui.expiration-previous"));
        inventory.setItem(NEXT_MONTH_SLOT, button(Material.ARROW, "gui.expiration-next"));
        inventory.setItem(MONTH_SLOT, button(Material.CLOCK, "gui.expiration-current-month", Map.of(
                "month", month.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH),
                "year", Integer.toString(month.getYear())
        )));
        inventory.setItem(CLEAR_SLOT, button(Material.BARRIER, "gui.expiration-clear"));
        inventory.setItem(BACK_SLOT, button(Material.OAK_DOOR, "gui.expiration-back"));
    }

    public LocalDate dateAt(int slot) {
        if(slot < 0 || slot >= inventory.getSize() || slot % 9 == 0 || slot % 9 == 8) return null;
        int row = slot / 9;
        int column = slot % 9 - 1;
        int day = row * 7 + column - (month.getDayOfWeek().getValue() - 1) + 1;
        if(day < 1 || day > month.lengthOfMonth()) return null;
        LocalDate date = month.withDayOfMonth(day);
        return date.isBefore(LocalDate.now()) ? null : date;
    }

    public void previousMonth() {
        if(month.isAfter(LocalDate.now().withDayOfMonth(1))) month = month.minusMonths(1);
        refresh();
    }

    public void nextMonth() {
        month = month.plusMonths(1);
        refresh();
    }

    public Crate crate() {
        return crate;
    }

    private ItemStack button(Material material, String messageKey) {
        return button(material, messageKey, Map.of());
    }

    private ItemStack button(Material material, String messageKey, Map<String, ?> placeholders) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text(messageKey, placeholders).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
