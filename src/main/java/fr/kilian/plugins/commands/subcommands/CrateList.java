package fr.kilian.plugins.commands.subcommands;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.Map;

@SubCommandRegister(command = "crate", value = "list")
public final class CrateList implements SubCommand {
    private final MessageService messages = CrateCreator.getInstance().messages();
    private final CrateManager crates = CrateCreator.getInstance().manager();

    @Override
    public void execute(Player player, String[] args) {
        var allCrates = crates.getCrates();
        if(allCrates.isEmpty()) {
            messages.send(player, "chat.crate.list-empty");
            return;
        }

        player.sendMessage(messages.text("chat.crate.list-title", Map.of(
                "count", Integer.toString(allCrates.size())
        )));
        for(Crate crate : allCrates) {
            String id = crate.id();
            Component line = messages.text("chat.crate.list-entry", Map.of(
                    "name", crate.name(),
                    "id", id
            ));
            line = line.append(Component.text(" ")).append(action("list-teleport", "/crate tp " + id))
                    .append(Component.text(" ")).append(action("list-edit", "/crate edit " + id))
                    .append(Component.text(" ")).append(action("list-info", "/crate info " + id));
            player.sendMessage(line);
        }
    }

    private Component action(String key, String command) {
        Component label = messages.text("chat.crate." + key);
        return label.clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(messages.text("chat.crate." + key + "-hover")));
    }
}
