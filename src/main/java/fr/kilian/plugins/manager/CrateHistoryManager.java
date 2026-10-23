package fr.kilian.plugins.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class CrateHistoryManager {
    private final File file;
    private final Logger logger;
    private final YamlConfiguration history;

    public CrateHistoryManager(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "history.yml");
        this.logger = logger;
        this.history = YamlConfiguration.loadConfiguration(file);
    }

    public void recordOpen(Player player, String crateId, String crateName, ItemStack reward) {
        String path = "players." + player.getUniqueId();
        history.set(path + ".name", player.getName());
        history.set(path + ".count", history.getInt(path + ".count") + 1);

        List<Map<?, ?>> existing = history.getMapList(path + ".entries");
        List<Map<String, Object>> entries = new ArrayList<>();
        for(Map<?, ?> entry : existing) {
            Map<String, Object> copy = new HashMap<>();
            entry.forEach((key, value) -> copy.put(String.valueOf(key), value));
            entries.add(copy);
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("time", System.currentTimeMillis());
        entry.put("crateId", crateId);
        entry.put("crateName", crateName);
        entry.put("reward", itemName(reward));
        entry.put("amount", reward.getAmount());
        entry.put("material", reward.getType().name());
        entries.add(entry);
        history.set(path + ".entries", entries);
        save();
    }

    public PlayerHistory findPlayer(String name) {
        var players = history.getConfigurationSection("players");
        if(players == null) return null;
        for(String key : players.getKeys(false)) {
            String playerName = history.getString("players." + key + ".name", "");
            if(playerName.equalsIgnoreCase(name)) {
                String path = "players." + key;
                List<HistoryEntry> entries = history.getMapList(path + ".entries").stream()
                        .map(this::toEntry)
                        .toList();
                return new PlayerHistory(playerName, history.getInt(path + ".count"), entries);
            }
        }
        return null;
    }

    public List<String> playerNames() {
        var players = history.getConfigurationSection("players");
        if(players == null) return List.of();
        return players.getKeys(false).stream()
                .map(key -> history.getString("players." + key + ".name", ""))
                .filter(name -> !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private HistoryEntry toEntry(Map<?, ?> entry) {
        Object time = entry.get("time");
        Object amount = entry.get("amount");
        return new HistoryEntry(
                time instanceof Number number ? number.longValue() : 0L,
                valueOrDefault(entry.get("crateId"), "?"),
                valueOrDefault(entry.get("crateName"), "?"),
                valueOrDefault(entry.get("reward"), "?"),
                amount instanceof Number number ? number.intValue() : 1,
                valueOrDefault(entry.get("material"), "PAPER")
        );
    }

    private String valueOrDefault(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if(meta != null && meta.hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(meta.displayName());
        }
        return item.getType().name();
    }

    private void save() {
        try {
            history.save(file);
        } catch(IOException exception) {
            logger.severe("Unable to save crate history: " + exception.getMessage());
        }
    }

    public record PlayerHistory(String name, int count, List<HistoryEntry> entries) {
    }

    public record HistoryEntry(long time, String crateId, String crateName, String reward, int amount,
                               String material) {
    }
}
