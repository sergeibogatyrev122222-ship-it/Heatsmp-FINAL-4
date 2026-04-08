package dev.heatsmp.heat;

import dev.heatsmp.HeatSMPPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

public class HeatParticles {

    private final HeatSMPPlugin plugin;
    private final Random random = new Random();

    public HeatParticles(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnHeatParticles(Player player) {
        double heat = plugin.getHeatManager().getHeat(player);
        HeatManager.HeatTier tier = plugin.getHeatManager().getTier(heat);
        Location loc = player.getLocation().add(0, 1, 0);

        switch (tier) {
            case COLD -> {} // No particles when cold

            case WARM -> {
                // Small smoke wisps
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 3, 0.3, 0.5, 0.3, 0.01);
            }

            case HOT -> {
                // Orange/flame particles
                player.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.3, 0.5, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.2, 0.4, 0.2, 0.01);
            }

            case BLAZING -> {
                // Large flames + lava drip effect (orange glow)
                player.getWorld().spawnParticle(Particle.FLAME, loc, 12, 0.4, 0.6, 0.4, 0.04);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 4, 0.3, 0.5, 0.3, 0);
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 6, 0.3, 0.5, 0.3, 0.02);
                // Occasional crackling sound
                if (random.nextInt(4) == 0) {
                    player.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.2f);
                }
            }

            case OVERHEAT -> {
                // Full fire explosion effect
                player.getWorld().spawnParticle(Particle.FLAME, loc, 20, 0.5, 0.8, 0.5, 0.06);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 8, 0.4, 0.6, 0.4, 0.02);
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 6, 0.4, 0.6, 0.4, 0.02);
                // Loud crackling
                if (random.nextInt(2) == 0) {
                    boolean broadcastSound = plugin.getConfig().getBoolean("effects.broadcast-overheat-sound", true);
                    if (broadcastSound) {
                        player.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
                    } else {
                        player.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
                    }
                }
            }
        }
    }

    // ─── Ability Effects ──────────────────────────────────────────────────────

    public void spawnFlameDashEffect(Location from, Location to) {
        // Trail of fire particles from start to end
        double distance = from.distance(to);
        int steps = (int) (distance * 3);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Location point = from.clone().add(
                    (to.getX() - from.getX()) * t,
                    (to.getY() - from.getY()) * t,
                    (to.getZ() - from.getZ()) * t
            );
            from.getWorld().spawnParticle(Particle.FLAME, point, 3, 0.1, 0.1, 0.1, 0.01);
        }
        from.getWorld().playSound(from, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.5f);
    }

    public void spawnHeatBurstEffect(Location center) {
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
        center.getWorld().spawnParticle(Particle.FLAME, center, 40, 1.5, 0.5, 1.5, 0.15);
        center.getWorld().spawnParticle(Particle.LAVA, center, 15, 1.0, 0.3, 1.0, 0);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f);
    }

    public void spawnEmergencyCoolEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.FALLING_WATER, loc, 60, 0.5, 0.8, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.SPLASH, loc, 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(loc, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.2f);
    }

    public void spawnCoolingPearlEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.FALLING_WATER, loc, 25, 0.3, 0.6, 0.3, 0.05);
        player.playSound(loc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.5f);
    }
}
