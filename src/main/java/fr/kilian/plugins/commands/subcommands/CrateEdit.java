package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandRegister(command = "crate", value = "edit")
public final class CrateEdit implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();
    private final CrateManager crateManager = CrateCreator.getInstance().manager();

    @Override
    public void execute(Player player, String[] args) {
        if(!player.isOp()) {
            messages.send(player, "chat.crate.edit-denied");
            return;
        }
        if(args.length != 1) {
            messages.send(player, "chat.crate.edit-usage");
            return;
        }

        Crate crate = crateManager.getCrateById(args[0]);
        if(crate == null) {
            messages.send(player, "chat.crate.crate-not-found", Map.of("id", args[0]));
            return;
        }
        player.openInventory(crate.gui().getInventory());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if(!player.isOp() || args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return crateManager.getCrateIds().stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
