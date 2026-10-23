package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandRegister(command = "crate", value = "key")
public final class CrateKey implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();
    private final CrateManager crates = CrateCreator.getInstance().manager();

    @Override
    public void execute(Player player, String[] args) {
        if(!player.isOp()) {
            messages.send(player, "chat.crate.edit-denied");
            return;
        }
        if(args.length != 2) {
            messages.send(player, "chat.crate.key-usage");
            return;
        }

        Crate crate = crates.getCrateById(args[0]);
        if(crate == null) {
            messages.send(player, "chat.crate.crate-not-found", Map.of("id", args[0]));
            return;
        }
        ItemStack key = crate.key();
        if(key == null) {
            messages.send(player, "chat.crate.key-not-configured");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch(NumberFormatException exception) {
            messages.send(player, "chat.crate.key-invalid-amount");
            return;
        }
        if(amount < 1 || amount > 64) {
            messages.send(player, "chat.crate.key-invalid-amount");
            return;
        }

        key.setAmount(amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        messages.send(player, "chat.crate.key-given", Map.of(
                "amount", Integer.toString(amount),
                "crate", crate.name()
        ));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if(!player.isOp()) return List.of();
        if(args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return crates.getCrateIds().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        if(args.length == 2) {
            return List.of("1", "2", "4", "8", "16", "32", "64").stream()
                    .filter(value -> value.startsWith(args[1]))
                    .toList();
        }
        return List.of();
    }
}
