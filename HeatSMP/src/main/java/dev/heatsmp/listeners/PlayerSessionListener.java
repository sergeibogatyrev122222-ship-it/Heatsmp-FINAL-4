package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatTabList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    private final HeatSMPPlugin plugin;
    private final HeatTabList tabList;

    public PlayerSessionListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.tabList = new HeatTabList(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var hm = plugin.getHeatManager();

        double heat = hm.getHeat(player);
        hm.applyHeatEffects(player, heat);

        if (heat <= 0) {
            player.setFireTicks(0);
        }

        // Set initial tab entry right away so it's never blank
        tabList.update(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();

        // Reset tab name to plain username before they leave
        tabList.reset(player);

        plugin.getHeatManager().remove(player.getUniqueId());
        plugin.getCoolingPearl().remove(player.getUniqueId());
        plugin.getAbilityManager().clearCooldowns(player);
    }
}
