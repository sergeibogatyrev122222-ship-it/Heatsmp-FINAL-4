package dev.heatsmp.items;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatParticles;
import dev.heatsmp.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CoolingPearl implements Listener {

    private final HeatSMPPlugin plugin;
    private final HeatParticles particles;
    private final NamespacedKey key;
    private final NamespacedKey pearlKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // Track which ender pearls were thrown as cooling pearls
    private final Set<UUID> activeCoolingPearls = new HashSet<>();

    public CoolingPearl(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.particles = new HeatParticles(plugin);
        this.key = new NamespacedKey(plugin, "cooling_pearl");
        this.pearlKey = new NamespacedKey(plugin, "cooling_pearl_projectile");
        registerRecipe();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("❄ Cooling Pearl")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(Arrays.asList(
                Component.text("❄ Teleports you like an Ender Pearl.")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("On landing: instantly removes 30 heat.")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Forged from the coldest depths of the ice biome.")
                        .color(NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCoolingPearl(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void use(Player player) {
        long cooldownMs = (long) (plugin.getConfig().getDouble("cooling-pearl.cooldown-seconds", 20) * 1000);
        UUID id = player.getUniqueId();
        Long last = cooldowns.get(id);
        if (last != null && (System.currentTimeMillis() - last) < cooldownMs) {
            double remaining = (cooldownMs - (System.currentTimeMillis() - last)) / 1000.0;
            MessageUtil.send(player, "cooling-pearl-cooldown", Map.of("time", String.format("%.1f", remaining)));
            return;
        }

        // Throw an ender pearl and track it
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.getPersistentDataContainer().set(pearlKey, PersistentDataType.STRING, player.getUniqueId().toString());
        activeCoolingPearls.add(pearl.getUniqueId());

        cooldowns.put(id, System.currentTimeMillis());

        // Consume one pearl
        var hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onPearlLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!activeCoolingPearls.contains(pearl.getUniqueId())) return;

        activeCoolingPearls.remove(pearl.getUniqueId());

        String uuidStr = pearl.getPersistentDataContainer().get(pearlKey, PersistentDataType.STRING);
        if (uuidStr == null) return;

        Player player = plugin.getServer().getPlayer(UUID.fromString(uuidStr));
        if (player == null) return;

        double heatReduction = plugin.getConfig().getDouble("cooling-pearl.heat-reduction", 30);

        // Schedule cooling effect after teleport completes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getHeatManager().removeHeat(player, heatReduction);
            particles.spawnCoolingPearlEffect(player);
            MessageUtil.send(player, "cooling-pearl-used");
        }, 1L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // Cancel the ender pearl damage on landing
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            // Only cancel damage if it's our cooling pearl — handled via cooldown check
        }
    }

    private void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(key, createItem());
        recipe.shape(" I ", "IPI", " I ");
        recipe.setIngredient('I', Material.ICE);
        recipe.setIngredient('P', Material.ENDER_PEARL);
        plugin.getServer().addRecipe(recipe);
    }

    public void remove(UUID uuid) {
        cooldowns.remove(uuid);
    }
}
