package dev.heatsmp.listeners;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.rods.RodManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class MaterialCheckListener implements Listener {

    private final HeatSMPPlugin plugin;

    // Materials needed per recipe: name -> [material, count]
    private record Ingredient(Material mat, int count) {}

    private static final Map<String, Ingredient[]> RECIPES = Map.of(
        "❄ Cooling Pearl", new Ingredient[]{
            new Ingredient(Material.ICE, 4),
            new Ingredient(Material.ENDER_PEARL, 1)
        },
        "🔥 Ember Rod", new Ingredient[]{
            new Ingredient(Material.BLAZE_ROD, 1),
            new Ingredient(Material.STICK, 2)
        },
        "💥 Scorch Rod", new Ingredient[]{
            new Ingredient(Material.BLAZE_ROD, 3),
            new Ingredient(Material.FIRE_CHARGE, 2)
        },
        "🌋 Inferno Rod", new Ingredient[]{
            new Ingredient(Material.BLAZE_ROD, 3),
            new Ingredient(Material.MAGMA_BLOCK, 4)
        },
        "☄ Molten Rod", new Ingredient[]{
            new Ingredient(Material.BLAZE_ROD, 2),
            new Ingredient(Material.MAGMA_BLOCK, 4),
            new Ingredient(Material.NETHERITE_INGOT, 2),
            new Ingredient(Material.LAVA_BUCKET, 1)
        },
        "🗡 Assassin's Rod", new Ingredient[]{
            new Ingredient(Material.DIAMOND, 2),
            new Ingredient(Material.POTION, 2),
            new Ingredient(Material.OBSIDIAN, 1)
        }
    );

    public MaterialCheckListener(HeatSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Schedule check after item is added to inventory
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            checkRecipes(player);
        }, 1L);
    }

    private void checkRecipes(Player player) {
        Inventory inv = player.getInventory();

        for (Map.Entry<String, Ingredient[]> entry : RECIPES.entrySet()) {
            String name = entry.getKey();
            Ingredient[] ingredients = entry.getValue();

            boolean hasAll = true;
            for (Ingredient ing : ingredients) {
                if (!hasEnough(inv, ing.mat(), ing.count())) {
                    hasAll = false;
                    break;
                }
            }

            if (hasAll) {
                player.sendMessage(
                    Component.text("✅ You have all the materials to craft ")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(name).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text("! Type /recipe to craft it.").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
                );
            }
        }
    }

    private boolean hasEnough(Inventory inv, Material mat, int required) {
        int count = 0;
        for (var item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count >= required;
    }
}
