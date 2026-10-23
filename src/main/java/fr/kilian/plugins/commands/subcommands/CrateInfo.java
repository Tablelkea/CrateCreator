package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.List;
import java.util.Locale;

@SubCommandRegister(command = "crate", value = "info")
public final class CrateInfo implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();
    private final CrateManager crates = CrateCreator.getInstance().manager();

    @Override
    public void execute(Player player, String[] args) {
        if(args.length != 1) return;
        Crate crate = crates.getCrateById(args[0]);
        if(crate == null) {
            messages.send(player, "chat.crate.crate-not-found", Map.of("id", args[0]));
            return;
        }

        Location location = crate.location();
        ItemStack[] contents = crate.contents();
        int itemCount = 0;
        for(ItemStack item : contents) {
            if(item != null && !item.getType().isAir()) itemCount++;
        }
        String world = location.getWorld() == null ? messages.plain("gui.unknown-world") : location.getWorld().getName();

        messages.send(player, "chat.crate.info-title", Map.of("name", crate.name()));
        messages.send(player, "chat.crate.info-id", Map.of("id", crate.id()));
        messages.send(player, "chat.crate.info-items", Map.of("count", Integer.toString(itemCount)));
        messages.send(player, "chat.crate.info-location", Map.of(
                "world", world,
                "x", Integer.toString(location.getBlockX()),
                "y", Integer.toString(location.getBlockY()),
                "z", Integer.toString(location.getBlockZ())
        ));
        messages.send(player, "chat.crate.info-particle", Map.of(
                "particle", crate.particle() == null ? messages.plain("gui.no-particle") : crate.particle().name()
        ));
        messages.send(player, "chat.crate.info-particle-distance", Map.of(
                "distance", String.format(Locale.US, "%.2f", crate.particleDistance())
        ));
        messages.send(player, "chat.crate.info-key", Map.of(
                "key", keyName(crate.key())
        ));
        messages.send(player, "chat.crate.info-message", Map.of(
                "message", crate.openMessage() == null || crate.openMessage().isBlank()
                        ? messages.plain("gui.no-value") : crate.openMessage()
        ));
    }

    private String keyName(ItemStack item) {
        if(item == null) return messages.plain("gui.no-key-value");
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                ? MiniMessage.miniMessage().serialize(meta.displayName()) : item.getType().name();
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if(args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return crates.getCrateIds().stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
