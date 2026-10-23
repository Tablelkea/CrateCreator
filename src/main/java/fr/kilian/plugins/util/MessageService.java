package fr.kilian.plugins.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MessageService {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("messages.yml", false);
        reload();
    }

    public void reload() {
        YamlConfiguration loaded;
        try(var reader = java.nio.file.Files.newBufferedReader(
                messagesFile(),
                StandardCharsets.UTF_8
        )) {
            loaded = YamlConfiguration.loadConfiguration(reader);
        } catch(IOException exception) {
            throw new IllegalStateException("Unable to load messages.yml", exception);
        }

        boolean addedDefaults = false;
        try(InputStream defaultsStream = plugin.getResource("messages.yml")) {
            if(defaultsStream == null) throw new IllegalStateException("Bundled messages.yml is missing");
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            for(String key : defaults.getKeys(true)) {
                if(defaults.getConfigurationSection(key) == null && !loaded.contains(key)) {
                    loaded.set(key, defaults.get(key));
                    addedDefaults = true;
                }
            }
        } catch(IOException exception) {
            throw new IllegalStateException("Unable to read bundled messages.yml", exception);
        }

        if(addedDefaults) {
            try {
                loaded.save(messagesFile().toFile());
            } catch(IOException exception) {
                throw new IllegalStateException("Unable to add default messages", exception);
            }
        }
        messages = loaded;
    }

    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        for(String key : messages.getKeys(true)) {
            if(messages.getConfigurationSection(key) == null) keys.add(key);
        }
        keys.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(keys);
    }

    public boolean hasKey(String key) {
        return messages.getConfigurationSection(key) == null && messages.contains(key);
    }

    public String value(String key) {
        Object value = messages.get(key);
        return value == null ? "" : value.toString();
    }

    public void setValue(String key, String value) {
        messages.set(key, value);
        try {
            messages.save(messagesFile().toFile());
        } catch(IOException exception) {
            throw new IllegalStateException("Unable to save messages.yml", exception);
        }
        reload();
    }

    private Path messagesFile() {
        return plugin.getDataFolder().toPath().resolve("messages.yml");
    }

    public Component text(String key) {
        return text(key, Map.of());
    }

    public String plain(String key) {
        String message = messages.getString(key);
        if(message == null) {
            throw new IllegalArgumentException("Missing message key: " + key);
        }
        return message;
    }

    public Component parse(String value) {
        return miniMessage.deserialize(value);
    }

    public Component text(String key, Map<String, ?> placeholders) {
        return deserialize(key, placeholders, true);
    }

    public Component textParsed(String key, Map<String, ?> placeholders) {
        return deserialize(key, placeholders, true);
    }

    private Component deserialize(String key, Map<String, ?> placeholders, boolean parsePlaceholders) {
        String template = messages.getString(key);
        if(template == null) {
            throw new IllegalArgumentException("Missing message key: " + key);
        }

        TagResolver.Builder resolver = TagResolver.builder();
        for(Map.Entry<String, ?> placeholder : placeholders.entrySet()) {
            String tag = "message_" + placeholder.getKey();
            template = template.replace("{" + placeholder.getKey() + "}", "<" + tag + ">");
            String value = Objects.toString(placeholder.getValue(), "");
            resolver.resolver(parsePlaceholders
                    ? Placeholder.parsed(tag, value)
                    : Placeholder.unparsed(tag, value));
        }
        return miniMessage.deserialize(template, resolver.build());
    }

    public void send(Player player, String key) {
        player.sendMessage(text(key));
    }

    public void send(Player player, String key, Map<String, ?> placeholders) {
        player.sendMessage(text(key, placeholders));
    }
}
