package dev.heatsmp.commands;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.items.HeatEnchant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HeatAdminCommand implements CommandExecutor {

    private final HeatSMPPlugin plugin;
    private final HeatEnchant heatEnchant;

    public HeatAdminCommand(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.heatEnchant = new HeatEnchant(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("heatsmp.admin")) {
            sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /heatadmin set <player> <value>").color(NamedTextColor.RED)); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED)); return true; }
                try {
                    double value = Double.parseDouble(args[2]);
                    plugin.getHeatManager().setHeat(target, value);
                    sender.sendMessage(Component.text("Set " + target.getName() + "'s heat to " + (int)value).color(NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid number.").color(NamedTextColor.RED));
                }
            }
            case "reset" -> {
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /heatadmin reset <player>").color(NamedTextColor.RED)); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED)); return true; }
                plugin.getHeatManager().resetHeat(target);
                sender.sendMessage(Component.text("Reset " + target.getName() + "'s heat to 0.").color(NamedTextColor.GREEN));
            }
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getHeatManager().loadConfig();
                sender.sendMessage(Component.text("HeatSMP config reloaded!").color(NamedTextColor.GREEN));
            }
            case "give" -> {
                // /heatadmin give <player> cooling_pearl
                if (args.length < 3) { sender.sendMessage(Component.text("Usage: /heatadmin give <player> cooling_pearl").color(NamedTextColor.RED)); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED)); return true; }
                if (args[2].equalsIgnoreCase("cooling_pearl")) {
                    target.getInventory().addItem(plugin.getCoolingPearl().createItem());
                    sender.sendMessage(Component.text("Gave Cooling Pearl to " + target.getName()).color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Unknown item. Available: cooling_pearl").color(NamedTextColor.RED));
                }
            }
            case "enchant" -> {
                // /heatadmin enchant <player> — applies Heat Enchant to held weapon
                if (args.length < 2) { sender.sendMessage(Component.text("Usage: /heatadmin enchant <player>").color(NamedTextColor.RED)); return true; }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) { sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED)); return true; }
                var item = target.getInventory().getItemInMainHand();
                if (item.getType().isAir()) { sender.sendMessage(Component.text(target.getName() + " is not holding an item.").color(NamedTextColor.RED)); return true; }
                heatEnchant.applyToItem(item);
                target.getInventory().setItemInMainHand(item);
                sender.sendMessage(Component.text("Applied Heat Enchant to " + target.getName() + "'s held item.").color(NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("━━━ HeatSMP Admin ━━━").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/heatadmin set <player> <value>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/heatadmin reset <player>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/heatadmin give <player> cooling_pearl").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/heatadmin enchant <player>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/heatadmin reload").color(NamedTextColor.YELLOW));
    }
}
