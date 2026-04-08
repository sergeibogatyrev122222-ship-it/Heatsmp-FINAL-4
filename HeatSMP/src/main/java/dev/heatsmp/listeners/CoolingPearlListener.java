package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CoolingPearlListener implements Listener {

    private final HeatSMPPlugin plugin;

    public CoolingPearlListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        // Only trigger on main hand right-click
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();

        if (!plugin.getCoolingPearl().isCoolingPearl(item)) return;

        event.setCancelled(true); // Prevent ender pearl teleport behavior
        plugin.getCoolingPearl().use(player);
    }
}
