package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final HeatSMPPlugin plugin;

    public CombatListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        HeatManager hm = plugin.getHeatManager();

        // Register combo hit
        hm.registerHit(attacker);

        // Attacker gains heat
        double attackerHeat = hm.getHitHeatGain(attacker);
        hm.addHeat(attacker, attackerHeat);
        hm.tagCombat(attacker);

        // Victim gains heat from being hit
        hm.addHeat(victim, hm.getHitReceivedGain());
        hm.tagCombat(victim);

        // Update boss bars immediately
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (attacker.isOnline()) plugin.getHeatManager().applyHeatEffects(attacker, hm.getHeat(attacker));
            if (victim.isOnline()) plugin.getHeatManager().applyHeatEffects(victim, hm.getHeat(victim));
        }, 1L);
    }
}
