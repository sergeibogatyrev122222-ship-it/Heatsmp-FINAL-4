package dev.heatsmp.items;

import dev.heatsmp.HeatSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class HeatEnchant {

    private final HeatSMPPlugin plugin;
    private final NamespacedKey key;

    public HeatEnchant(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "heat_enchant");
    }

    /**
     * Apply the Heat Enchant lore/marker to a weapon.
     * Called via /heatadmin enchant <player>
     */
    public ItemStack applyToItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Add lore marker (checked in HeatManager.hasHeatEnchant)
        java.util.List<net.kyori.adventure.text.Component> lore = meta.lore() != null
                ? new java.util.ArrayList<>(meta.lore())
                : new java.util.ArrayList<>();
        lore.add(Component.text("✦ Heat Enchant")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Gain more heat — overheat faster.")
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Also tag with PDC for safe detection
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean hasHeatEnchant(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
