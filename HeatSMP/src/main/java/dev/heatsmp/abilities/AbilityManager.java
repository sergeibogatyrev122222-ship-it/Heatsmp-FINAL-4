package dev.heatsmp.abilities;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatParticles;
import dev.heatsmp.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {

    private final HeatSMPPlugin plugin;
    private final HeatParticles particles;

    // Cooldown maps: UUID -> last use timestamp
    private final Map<UUID, Long> flameDashCooldowns = new HashMap<>();
    private final Map<UUID, Long> heatBurstCooldowns = new HashMap<>();
    private final Map<UUID, Long> emergencyCoolCooldowns = new HashMap<>();

    public AbilityManager(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.particles = new HeatParticles(plugin);
    }

    // ─── Flame Dash ──────────────────────────────────────────────────────────

    public boolean flameDash(Player player) {
        var cfg = plugin.getConfig();
        double heatCost = cfg.getDouble("abilities.flame-dash.heat-cost", 20);
        long cooldownMs = (long) (cfg.getDouble("abilities.flame-dash.cooldown-seconds", 8) * 1000);
        double dashPower = cfg.getDouble("abilities.flame-dash.dash-power", 1.4);
        int fireTrailTicks = cfg.getInt("abilities.flame-dash.fire-trail-duration-ticks", 40);

        if (isOnCooldown(flameDashCooldowns, player, cooldownMs)) {
            double remaining = getRemainingCooldown(flameDashCooldowns, player, cooldownMs);
            MessageUtil.send(player, "ability-on-cooldown", Map.of("time", String.format("%.1f", remaining)));
            return false;
        }

        double heat = plugin.getHeatManager().getHeat(player);
        if (heat < heatCost) {
            MessageUtil.send(player, "ability-no-heat");
            return false;
        }

        Location from = player.getLocation().clone();

        // Dash in the direction the player is facing
        Vector direction = player.getLocation().getDirection().normalize().multiply(dashPower);
        direction.setY(Math.max(direction.getY(), 0.3)); // slight upward
        player.setVelocity(direction);

        // Schedule fire trail effect
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location to = player.getLocation().clone();
            particles.spawnFlameDashEffect(from, to);
            // Set fire on blocks in trail
            leaveFireTrail(from, to, fireTrailTicks);
        }, 5L);

        plugin.getHeatManager().addHeat(player, heatCost);
        flameDashCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        MessageUtil.send(player, "ability-used-flamedash");
        return true;
    }

    private void leaveFireTrail(Location from, Location to, int fireTicks) {
        double distance = from.distance(to);
        int steps = Math.max(1, (int) distance);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Location point = from.clone().add(
                    (to.getX() - from.getX()) * t,
                    (to.getY() - from.getY()) * t,
                    (to.getZ() - from.getZ()) * t
            );
            var block = point.getBlock();
            if (block.getType().isAir()) {
                var below = block.getRelative(0, -1, 0);
                if (!below.getType().isAir()) {
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, point, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }

    // ─── Heat Burst ──────────────────────────────────────────────────────────

    public boolean heatBurst(Player player) {
        var cfg = plugin.getConfig();
        double heatCost = cfg.getDouble("abilities.heat-burst.heat-cost", 40);
        long cooldownMs = (long) (cfg.getDouble("abilities.heat-burst.cooldown-seconds", 12) * 1000);
        double knockbackPower = cfg.getDouble("abilities.heat-burst.knockback-power", 2.0);
        double radius = cfg.getDouble("abilities.heat-burst.explosion-radius", 3.0);

        if (isOnCooldown(heatBurstCooldowns, player, cooldownMs)) {
            double remaining = getRemainingCooldown(heatBurstCooldowns, player, cooldownMs);
            MessageUtil.send(player, "ability-on-cooldown", Map.of("time", String.format("%.1f", remaining)));
            return false;
        }

        double heat = plugin.getHeatManager().getHeat(player);
        if (heat < heatCost) {
            MessageUtil.send(player, "ability-no-heat");
            return false;
        }

        Location center = player.getLocation();
        particles.spawnHeatBurstEffect(center);

        // Knockback all nearby players
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player nearby) {
                Vector direction = nearby.getLocation().subtract(center).toVector().normalize();
                direction.setY(0.4);
                nearby.setVelocity(direction.multiply(knockbackPower));
                // Add heat to hit players
                plugin.getHeatManager().addHeat(nearby, plugin.getHeatManager().getHitReceivedGain());
            }
        }

        plugin.getHeatManager().addHeat(player, heatCost);
        heatBurstCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        MessageUtil.send(player, "ability-used-heatburst");
        return true;
    }

    // ─── Emergency Cool ──────────────────────────────────────────────────────

    public boolean emergencyCool(Player player) {
        var cfg = plugin.getConfig();
        int weaknessDuration = cfg.getInt("abilities.emergency-cool.weakness-duration-seconds", 4);
        long cooldownMs = (long) (cfg.getDouble("abilities.emergency-cool.cooldown-seconds", 30) * 1000);

        if (isOnCooldown(emergencyCoolCooldowns, player, cooldownMs)) {
            double remaining = getRemainingCooldown(emergencyCoolCooldowns, player, cooldownMs);
            MessageUtil.send(player, "ability-on-cooldown", Map.of("time", String.format("%.1f", remaining)));
            return false;
        }

        particles.spawnEmergencyCoolEffect(player);

        plugin.getHeatManager().resetHeat(player);
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration * 20, 0, true, true, true));

        emergencyCoolCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        MessageUtil.send(player, "ability-used-emergencycool");
        return true;
    }

    // ─── Cooldown Helpers ─────────────────────────────────────────────────────

    private boolean isOnCooldown(Map<UUID, Long> map, Player player, long cooldownMs) {
        Long last = map.get(player.getUniqueId());
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    private double getRemainingCooldown(Map<UUID, Long> map, Player player, long cooldownMs) {
        Long last = map.get(player.getUniqueId());
        if (last == null) return 0;
        double remaining = (cooldownMs - (System.currentTimeMillis() - last)) / 1000.0;
        return Math.max(0, remaining);
    }

    public double getFlameDashCooldownRemaining(Player player) {
        long ms = (long) (plugin.getConfig().getDouble("abilities.flame-dash.cooldown-seconds", 8) * 1000);
        return getRemainingCooldown(flameDashCooldowns, player, ms);
    }

    public double getHeatBurstCooldownRemaining(Player player) {
        long ms = (long) (plugin.getConfig().getDouble("abilities.heat-burst.cooldown-seconds", 12) * 1000);
        return getRemainingCooldown(heatBurstCooldowns, player, ms);
    }

    public double getEmergencyCoolCooldownRemaining(Player player) {
        long ms = (long) (plugin.getConfig().getDouble("abilities.emergency-cool.cooldown-seconds", 30) * 1000);
        return getRemainingCooldown(emergencyCoolCooldowns, player, ms);
    }

    public void clearCooldowns(Player player) {
        UUID id = player.getUniqueId();
        flameDashCooldowns.remove(id);
        heatBurstCooldowns.remove(id);
        emergencyCoolCooldowns.remove(id);
    }
}
