package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.TreeSet;

public final class MessageConfigGUI implements InventoryHolder {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int RELOAD_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final MessageService messages;
    private final Inventory inventory;
    private String category;
    private int page;

    public MessageConfigGUI() {
        messages = CrateCreator.getInstance().messages();
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 54, messages.text("gui.config-title"));
        refresh();
    }

    public void refresh() {
        inventory.clear();
        if(category == null) {
            List<String> categories = categories();
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, categories.size());
            for(int index = start; index < end; index++) {
                String value = categories.get(index);
                inventory.setItem(index - start, categoryButton(value));
            }
        } else {
            List<String> keys = categoryKeys();
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, keys.size());
            for(int index = start; index < end; index++) {
                String key = keys.get(index);
                inventory.setItem(index - start, entry(key, messages.value(key)));
            }
        }
        if(page > 0) {
            inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "gui.config-previous"));
        } else if(category != null) {
            inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "gui.config-previous"));
        } else {
            inventory.setItem(PREVIOUS_SLOT, button(Material.BLACK_STAINED_GLASS_PANE, "gui.pane"));
        }
        inventory.setItem(RELOAD_SLOT, button(Material.SUNFLOWER, "gui.config-reload"));
        inventory.setItem(NEXT_SLOT, button(Material.ARROW, "gui.config-next"));
    }

    public String keyAt(int slot) {
        if(category == null || slot < 0 || slot >= PAGE_SIZE) return null;
        int index = page * PAGE_SIZE + slot;
        List<String> keys = categoryKeys();
        return index >= keys.size() ? null : keys.get(index);
    }

    public String categoryAt(int slot) {
        if(category != null || slot < 0 || slot >= PAGE_SIZE) return null;
        int index = page * PAGE_SIZE + slot;
        List<String> categories = categories();
        return index >= categories.size() ? null : categories.get(index);
    }

    public void selectCategory(String category) {
        if(!categories().contains(category)) return;
        this.category = category;
        page = 0;
        refresh();
    }

    public void previousPage() {
        if(page > 0) {
            page--;
        } else if(category != null) {
            category = null;
        }
        refresh();
    }

    public void nextPage() {
        int count = category == null ? categories().size() : categoryKeys().size();
        if((page + 1) * PAGE_SIZE < count) page++;
        refresh();
    }

    private List<String> categories() {
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for(String key : messages.keys()) {
            String[] parts = key.split("\\.");
            categories.add(parts.length > 2 && parts[0].equals("chat")
                    ? parts[0] + "." + parts[1] : parts[0]);
        }
        return List.copyOf(categories);
    }

    private List<String> categoryKeys() {
        return messages.keys().stream()
                .filter(key -> key.startsWith(category + "."))
                .toList();
    }

    private ItemStack entry(String key, String value) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(key).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(messages.parse(value).decoration(TextDecoration.ITALIC, false)));
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

    private ItemStack categoryButton(String category) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(category).decoration(TextDecoration.ITALIC, false));
        long count = messages.keys().stream().filter(key -> key.startsWith(category + ".")).count();
        meta.lore(List.of(Component.text(count + " valeur(s)")
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
