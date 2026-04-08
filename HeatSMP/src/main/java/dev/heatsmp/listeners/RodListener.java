package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.rods.RodManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RodListener implements Listener {

    private final HeatSMPPlugin plugin;
    // Players currently mid-scorch ability (immune to fall damage)
    private final Set<UUID> scorchFallImmune = new HashSet<>();

    public RodListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();
        RodManager.RodType type = plugin.getRodManager().getRodType(item);
        if (type == null) return;

        event.setCancelled(true);
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Mark scorch users for fall immunity
            if (type == RodManager.RodType.SCORCH) {
                scorchFallImmune.add(player.getUniqueId());
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    scorchFallImmune.remove(player.getUniqueId()), 120L); // 6 seconds
            }
            plugin.getRodManager().useRightClick(player, type);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (type == RodManager.RodType.SCORCH) {
                scorchFallImmune.add(player.getUniqueId());
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    scorchFallImmune.remove(player.getUniqueId()), 120L);
            }
            plugin.getRodManager().useLeftClick(player, type);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Cancel fall damage for scorch abilities
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (scorchFallImmune.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHitWithRod(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        var item = attacker.getInventory().getItemInMainHand();
        RodManager.RodType type = plugin.getRodManager().getRodType(item);

        // Assassin rod blind on hit
        if (type == RodManager.RodType.ASSASSIN) {
            plugin.getRodManager().applyAssassinBlind(attacker, victim);
        }

        // Molten armor absorption
        if (plugin.getRodManager().isMoltenArmorActive(victim)) {
            event.setCancelled(true);
            plugin.getRodManager().absorbHitAsMoltenArmor(victim, attacker);
        }
    }
}
