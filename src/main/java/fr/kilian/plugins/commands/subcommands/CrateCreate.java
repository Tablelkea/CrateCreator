package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

import java.util.Map;

@SubCommandRegister(command = "crate", value = "create")
public class CrateCreate implements SubCommand {

    private final MessageService messages = CrateCreator.getInstance().messages();
    private final CrateManager manager = CrateCreator.getInstance().manager();

    @Override
    public void execute(Player player, String[] args) {
        if(args.length < 2) {
            messages.send(player, "chat.crate.create-usage");
            return;
        }

        Crate crate = new Crate(
                player.getLocation(),
                args[0],
                args[1]
        );

        if(!manager.createCrate(crate,
                CrateManager.horizontalFacing(player.getLocation().getYaw()))) {
            messages.send(player, "chat.crate.create-failed");
            return;
        }
        messages.send(player, "chat.crate.created", Map.of("name", crate.name()));
    }
}
