package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.model.MessageConfigGUI;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.List;

@SubCommandRegister(command = "crate", value = "config")
public final class CrateConfig implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();

    @Override
    public void execute(Player player, String[] args) {
        if(!player.isOp()) {
            messages.send(player, "chat.crate.edit-denied");
            return;
        }
        if(args.length == 0) {
            player.openInventory(new MessageConfigGUI().getInventory());
            return;
        }
        if(args[0].equalsIgnoreCase("set")) {
            if(args.length < 2) {
                messages.send(player, "chat.crate.config-key-required");
                return;
            }
            String key = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            CrateCreator.getInstance().listener().beginConfigEdit(player, key);
            return;
        }

        String key = String.join(" ", args);
        if(!messages.hasKey(key)) {
            player.sendMessage(messages.text("chat.crate.config-key-unknown", java.util.Map.of("key", key)));
            return;
        }
        player.sendMessage(Component.text(key, NamedTextColor.GRAY));
        String currentValue = messages.value(key);
        try {
            player.sendMessage(messages.parse(currentValue));
        } catch(IllegalArgumentException exception) {
            player.sendMessage(Component.text(currentValue, NamedTextColor.RED));
        }
        player.sendMessage(Component.text("[modifier]", NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/crate config set " + key))
                .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour modifier ce message"))));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if(!player.isOp()) return List.of();
        if(args.length > 0 && args[0].equalsIgnoreCase("set")) {
            String keyPrefix = args.length == 1 ? "" : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return messages.keys().stream().filter(key -> key.startsWith(keyPrefix)).toList();
        }
        String prefix = args.length == 0 ? "" : String.join(" ", args);
        if(args.length <= 1 && "set".startsWith(prefix.toLowerCase(java.util.Locale.ROOT))) {
            return java.util.stream.Stream.concat(java.util.stream.Stream.of("set"), messages.keys().stream())
                    .filter(key -> key.toLowerCase(java.util.Locale.ROOT)
                            .startsWith(prefix.toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return messages.keys().stream().filter(key -> key.startsWith(prefix)).toList();
    }
}
