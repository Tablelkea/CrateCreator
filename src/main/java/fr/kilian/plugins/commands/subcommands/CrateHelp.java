package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

@SubCommandRegister(command = "crate", value = "help")
public class CrateHelp implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();

    @Override
    public void execute(Player player, String[] args) {
        messages.send(player, "chat.crate.help-create");
        messages.send(player, "chat.crate.help-edit");
        messages.send(player, "chat.crate.help-list");
        messages.send(player, "chat.crate.help-config");
        messages.send(player, "chat.crate.help-run-test");
        messages.send(player, "chat.crate.help-key");
        messages.send(player, "chat.crate.help-history");
        messages.send(player, "chat.crate.help-help");
    }
}
