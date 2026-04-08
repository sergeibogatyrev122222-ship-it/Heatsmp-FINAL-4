package dev.heatsmp.utils;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;

public class MessageUtil {

    private static HeatSMPPlugin plugin;

    public static void send(Player player, String messageKey) {
        if (plugin == null) plugin = HeatSMPPlugin.getInstance();

        send(player, messageKey, Collections.emptyMap());
    }

    public static void send(Player player, String messageKey, Map<String, String> placeholders) {
        if (plugin == null) plugin = HeatSMPPlugin.getInstance();
        if (plugin == null) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&c🔥 Heat&8] &r");
        String raw = plugin.getConfig().getString("messages." + messageKey, "");
        if (raw.isEmpty()) return;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + raw);
        player.sendMessage(msg);
    }
}
