package fr.kilian.plugins.util;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.model.Crate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CrateMessageFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final MessageService MESSAGES = CrateCreator.getInstance().messages();

    private CrateMessageFormatter() {
    }

    public static Component format(String message, Player player, Crate crate) {
        String template = message
                .replace("{player}", "<crate_player>")
                .replace("{name}", "<crate_name>")
                .replace("{id}", "<crate_id>")
                .replace("{world}", "<crate_world>")
                .replace("{x}", "<crate_x>")
                .replace("{y}", "<crate_y>")
                .replace("{z}", "<crate_z>")
                .replace("{key}", "<crate_key>")
                .replace("{particle}", "<crate_particle>");

        ItemStack key = crate.key();
        String keyName;
        if(key == null) {
            keyName = MESSAGES.plain("gui.no-key-value");
        } else {
            ItemMeta meta = key.getItemMeta();
            keyName = meta != null && meta.hasDisplayName()
                    ? MINI_MESSAGE.serialize(meta.displayName()) : key.getType().name();
        }

        return MINI_MESSAGE.deserialize(
                template,
                Placeholder.unparsed("crate_player", player.getName()),
                Placeholder.parsed("crate_name", crate.name()),
                Placeholder.unparsed("crate_id", crate.id()),
                Placeholder.unparsed("crate_world", crate.location().getWorld() == null
                        ? MESSAGES.plain("gui.unknown-world") : crate.location().getWorld().getName()),
                Placeholder.unparsed("crate_x", Integer.toString(crate.location().getBlockX())),
                Placeholder.unparsed("crate_y", Integer.toString(crate.location().getBlockY())),
                Placeholder.unparsed("crate_z", Integer.toString(crate.location().getBlockZ())),
                Placeholder.parsed("crate_key", keyName),
                Placeholder.parsed("crate_particle", crate.particle() == null
                        ? MESSAGES.plain("gui.no-particle") : crate.particle().name())
        );
    }
}
