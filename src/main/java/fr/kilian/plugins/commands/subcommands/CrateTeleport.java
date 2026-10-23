package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.List;
import java.util.Locale;

@SubCommandRegister(command = "crate", value = "tp")
public final class CrateTeleport implements SubCommand {
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

        Location target = crate.location().add(0.5, 1.0, 0.5);
        player.teleport(target);
        messages.send(player, "chat.crate.teleported", Map.of("name", crate.name()));
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
