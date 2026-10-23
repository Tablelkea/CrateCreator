package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.model.RunTestGUI;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

@SubCommandRegister(command = "crate", value = "runTest")
public final class CrateRunTest implements SubCommand {
    @Override
    public void execute(Player player, String[] args) {
        MessageService messages = CrateCreator.getInstance().messages();
        if(!player.isOp()) {
            messages.send(player, "chat.crate.edit-denied");
            return;
        }
        player.openInventory(new RunTestGUI().getInventory());
    }
}
