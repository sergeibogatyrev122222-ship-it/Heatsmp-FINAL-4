package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatBar;
import dev.heatsmp.heat.HeatManager;
import dev.heatsmp.heat.HeatParticles;
import dev.heatsmp.heat.HeatTabList;
import dev.heatsmp.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class HeatTickListener implements Listener {

    private final HeatSMPPlugin plugin;
    private final HeatBar heatBar;
    private final HeatParticles particles;
    private final HeatTabList tabList;

    private final java.util.Set<java.util.UUID> wasOverheated = new java.util.HashSet<>();

    public HeatTickListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.heatBar = new HeatBar(plugin);
        this.particles = new HeatParticles(plugin);
        this.tabList = new HeatTabList(plugin);

        int particleTicks = plugin.getConfig().getInt("effects.particle-update-ticks", 10);

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 20L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickSprint, 20L, 20L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickParticles, particleTicks, particleTicks);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickOverheatFire, 10L, 10L);
    }

    private void tickAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            HeatManager hm = plugin.getHeatManager();

            hm.tickCool(player);

            double heat = hm.getHeat(player);
            boolean isOverheated = heat >= hm.getOverheatMin();

            if (isOverheated) {
                hm.tickOverheatDamage(player);
                if (!wasOverheated.contains(player.getUniqueId())) {
                    MessageUtil.send(player, "overheat");
                    wasOverheated.add(player.getUniqueId());
                }
            } else {
                if (wasOverheated.contains(player.getUniqueId()) && heat <= 0) {
                    MessageUtil.send(player, "cooled-down");
                    wasOverheated.remove(player.getUniqueId());
                }
            }

            // Boss bar
            if (heat > 0 || isOverheated) {
                heatBar.update(player);
            } else {
                heatBar.hide(player);
            }

            // Tab list
            tabList.update(player);
        }
    }

    private void tickSprint() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.isSprinting() && plugin.getHeatManager().isInCombat(player)) {
                plugin.getHeatManager().addHeat(player, plugin.getHeatManager().getSprintHeatPerSecond());
            }
        }
    }

    private void tickParticles() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            double heat = plugin.getHeatManager().getHeat(player);
            if (heat > 0) {
                particles.spawnHeatParticles(player);
            }
        }
    }

    private void tickOverheatFire() {
        // Fire ticks removed — overheat deals damage without visual fire
        // Just extinguish any lingering fire from other sources
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            double heat = plugin.getHeatManager().getHeat(player);
            if (heat < plugin.getHeatManager().getOverheatMin()) {
                if (player.getFireTicks() > 0 && !player.getWorld().getBlockAt(player.getLocation()).getType().name().contains("LAVA")) {
                    player.setFireTicks(0);
                }
            }
        }
    }

    public HeatBar getHeatBar() { return heatBar; }
    public HeatTabList getTabList() { return tabList; }
}
