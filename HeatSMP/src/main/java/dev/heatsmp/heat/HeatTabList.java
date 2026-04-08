package dev.heatsmp.heat;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class HeatTabList {

    private final HeatSMPPlugin plugin;

    public HeatTabList(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player player) {
        double heat = plugin.getHeatManager().getHeat(player);
        HeatManager.HeatTier tier = plugin.getHeatManager().getTier(heat);

        NamedTextColor color = switch (tier) {
            case COLD    -> NamedTextColor.AQUA;
            case WARM    -> NamedTextColor.YELLOW;
            case HOT     -> NamedTextColor.GOLD;
            case BLAZING -> NamedTextColor.RED;
            case OVERHEAT -> NamedTextColor.DARK_RED;
        };

        String icon = switch (tier) {
            case COLD     -> "❄";
            case WARM     -> "🌡";
            case HOT      -> "🔥";
            case BLAZING  -> "🔥";
            case OVERHEAT -> "💀";
        };

        // Format: PlayerName  🔥 74
        Component tabName = Component.text(player.getName())
                .color(NamedTextColor.WHITE)
                .append(Component.text("  " + icon + " " + (int) heat)
                        .color(color)
                        .decoration(TextDecoration.BOLD, tier == HeatManager.HeatTier.OVERHEAT));

        player.playerListName(tabName);
    }

    /** Reset a player's tab name back to their plain username. */
    public void reset(Player player) {
        player.playerListName(Component.text(player.getName()).color(NamedTextColor.WHITE));
    }
}
