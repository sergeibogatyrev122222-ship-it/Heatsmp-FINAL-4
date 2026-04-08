package dev.heatsmp.commands;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HeatCommand implements CommandExecutor {

    private final HeatSMPPlugin plugin;

    public HeatCommand(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: /heat <player>").color(NamedTextColor.RED));
            return true;
        }

        HeatManager hm = plugin.getHeatManager();
        double heat = hm.getHeat(target);
        double max = hm.getMaxHeat();
        HeatManager.HeatTier tier = hm.getTier(heat);

        String tierName = switch (tier) {
            case COLD -> "❄ Cold";
            case WARM -> "🌡 Warm";
            case HOT -> "🔥 Hot";
            case BLAZING -> "🔥🔥 Blazing";
            case OVERHEAT -> "💀 OVERHEATING";
        };

        NamedTextColor color = switch (tier) {
            case COLD -> NamedTextColor.AQUA;
            case WARM -> NamedTextColor.YELLOW;
            case HOT -> NamedTextColor.GOLD;
            case BLAZING -> NamedTextColor.RED;
            case OVERHEAT -> NamedTextColor.DARK_RED;
        };

        sender.sendMessage(Component.text("━━━ Heat Status ━━━").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Player: ").color(NamedTextColor.GRAY)
                .append(Component.text(target.getName()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Heat: ").color(NamedTextColor.GRAY)
                .append(Component.text((int) heat + " / " + (int) max).color(color)));
        sender.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                .append(Component.text(tierName).color(color)));
        sender.sendMessage(Component.text("In Combat: ").color(NamedTextColor.GRAY)
                .append(Component.text(hm.isInCombat(target) ? "Yes" : "No")
                        .color(hm.isInCombat(target) ? NamedTextColor.RED : NamedTextColor.GREEN)));

        // Ability cooldowns (only shown to the player themselves)
        if (sender instanceof Player p && p.equals(target)) {
            var am = plugin.getAbilityManager();
            double fd = am.getFlameDashCooldownRemaining(target);
            double hb = am.getHeatBurstCooldownRemaining(target);
            double ec = am.getEmergencyCoolCooldownRemaining(target);
            sender.sendMessage(Component.text("━━━ Abilities ━━━").color(NamedTextColor.GRAY));
            sender.sendMessage(cooldownLine("🔥 Flame Dash", fd));
            sender.sendMessage(cooldownLine("💥 Heat Burst", hb));
            sender.sendMessage(cooldownLine("🧯 Emergency Cool", ec));
        }

        return true;
    }

    private Component cooldownLine(String name, double cd) {
        Component status = cd > 0
                ? Component.text(String.format("%.1fs", cd)).color(NamedTextColor.RED)
                : Component.text("READY").color(NamedTextColor.GREEN);
        return Component.text("  " + name + ": ").color(NamedTextColor.GRAY).append(status);
    }
}
