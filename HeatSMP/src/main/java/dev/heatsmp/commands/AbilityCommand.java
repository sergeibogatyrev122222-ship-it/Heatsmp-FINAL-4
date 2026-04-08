package dev.heatsmp.commands;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AbilityCommand implements CommandExecutor {

    private final HeatSMPPlugin plugin;

    public AbilityCommand(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use abilities.").color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("heatsmp.use")) {
            player.sendMessage(Component.text("You don't have permission to use abilities.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("━━━ Abilities ━━━").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("/ability flamedash").color(NamedTextColor.YELLOW).append(
                    Component.text(" — Dash forward, leave fire trail (20 heat)").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/ability heatburst").color(NamedTextColor.YELLOW).append(
                    Component.text(" — Knockback nearby players (40 heat)").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/ability emergencycool").color(NamedTextColor.YELLOW).append(
                    Component.text(" — Reset heat to 0, gain Weakness").color(NamedTextColor.GRAY)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "flamedash", "fd", "dash" -> plugin.getAbilityManager().flameDash(player);
            case "heatburst", "hb", "burst" -> plugin.getAbilityManager().heatBurst(player);
            case "emergencycool", "ec", "cool" -> plugin.getAbilityManager().emergencyCool(player);
            default -> player.sendMessage(Component.text("Unknown ability. Use /ability for a list.").color(NamedTextColor.RED));
        }

        return true;
    }
}
