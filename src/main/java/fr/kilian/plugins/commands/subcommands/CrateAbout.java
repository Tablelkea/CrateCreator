package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.stream.Collectors;

@SubCommandRegister(command = "crate", value = "root")
public final class CrateAbout implements SubCommand {
    private final CrateCreator plugin = CrateCreator.getInstance();
    private final MessageService messages = plugin.messages();

    @Override
    public void execute(Player player, String[] args) {
        String author = plugin.getDescription().getAuthors().stream()
                .collect(Collectors.joining(", "));
        messages.send(player, "chat.crate.plugin-info", Map.of(
                "plugin", plugin.getDescription().getName(),
                "author", author.isBlank() ? "Inconnu" : author,
                "version", plugin.getDescription().getVersion()
        ));
    }
}
