package dev.heatsmp.heat;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeatBar {

    private final HeatSMPPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HeatBar(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void update(Player player) {
        double heat = plugin.getHeatManager().getHeat(player);
        double max = plugin.getHeatManager().getMaxHeat();
        float progress = (float) Math.min(1.0, heat / max);

        HeatManager.HeatTier tier = plugin.getHeatManager().getTier(heat);

        BossBar.Color color = switch (tier) {
            case COLD -> BossBar.Color.BLUE;
            case WARM -> BossBar.Color.YELLOW;
            case HOT -> BossBar.Color.YELLOW;
            case BLAZING -> BossBar.Color.RED;
            case OVERHEAT -> BossBar.Color.RED;
        };

        String emoji = switch (tier) {
            case COLD -> "❄";
            case WARM -> "🌡";
            case HOT -> "🔥";
            case BLAZING -> "🔥🔥";
            case OVERHEAT -> "💀 OVERHEAT";
        };

        Component title = Component.text("Heat: " + (int) heat + "/" + (int) max + "  " + emoji)
                .color(tier == HeatManager.HeatTier.OVERHEAT ? NamedTextColor.RED : NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, tier == HeatManager.HeatTier.OVERHEAT);

        BossBar bar = bars.computeIfAbsent(player.getUniqueId(), id ->
                BossBar.bossBar(title, progress, color, BossBar.Overlay.NOTCHED_10));

        bar.name(title);
        bar.progress(progress);
        bar.color(color);

        player.showBossBar(bar);
    }

    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void hideAll() {
        bars.forEach((uuid, bar) -> {
            var player = plugin.getServer().getPlayer(uuid);
            if (player != null) player.hideBossBar(bar);
        });
        bars.clear();
    }
}
