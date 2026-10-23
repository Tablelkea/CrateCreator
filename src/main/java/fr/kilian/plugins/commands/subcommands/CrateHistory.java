package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateHistoryManager;
import fr.kilian.plugins.model.CrateHistoryGUI;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SubCommandRegister(command = "crate", value = "history")
public final class CrateHistory implements SubCommand {
    private final CrateHistoryManager history = CrateCreator.getInstance().history();
    private final MessageService messages = CrateCreator.getInstance().messages();

    @Override
    public void execute(Player player, String[] args) {
        if(!player.isOp()) {
            messages.send(player, "chat.crate.edit-denied");
            return;
        }
        if(args.length < 1 || args.length > 2) {
            messages.send(player, "chat.crate.history-usage");
            return;
        }

        CrateHistoryManager.PlayerHistory playerHistory = history.findPlayer(args[0]);
        if(playerHistory == null || playerHistory.entries().isEmpty()) {
            messages.send(player, "chat.crate.history-empty", Map.of("player", args[0]));
            return;
        }

        String crateFilter = args.length == 2 ? args[1] : null;
        List<CrateHistoryManager.HistoryEntry> entries = playerHistory.entries();
        if(crateFilter != null) {
            entries = entries.stream()
                    .filter(entry -> entry.crateId().equalsIgnoreCase(crateFilter)
                            || entry.crateName().equalsIgnoreCase(crateFilter))
                    .toList();
            if(entries.isEmpty()) {
                messages.send(player, "chat.crate.history-crate-empty", Map.of(
                        "player", playerHistory.name(), "crate", crateFilter
                ));
                return;
            }
        }
        player.openInventory(new CrateHistoryGUI(playerHistory.name(), crateFilter, entries).getInventory());
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if(!player.isOp()) return List.of();
        if(args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>(history.playerNames());
            for(Player online : player.getServer().getOnlinePlayers()) {
                if(names.stream().noneMatch(name -> name.equalsIgnoreCase(online.getName()))) {
                    names.add(online.getName());
                }
            }
            return names.stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        if(args.length == 2) {
            CrateHistoryManager.PlayerHistory playerHistory = history.findPlayer(args[0]);
            if(playerHistory == null) return List.of();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return playerHistory.entries().stream()
                    .map(CrateHistoryManager.HistoryEntry::crateId)
                    .distinct()
                    .filter(crate -> crate.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }
}
