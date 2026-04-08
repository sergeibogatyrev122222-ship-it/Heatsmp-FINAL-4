package dev.heatsmp.heat;

import dev.heatsmp.HeatSMPPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HeatManager {

    private final HeatSMPPlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    // Runtime maps
    private final Map<UUID, Double> heatMap = new HashMap<>();
    private final Map<UUID, Long> combatTagMap = new HashMap<>();
    private final Map<UUID, Integer> comboMap = new HashMap<>();
    private final Map<UUID, Long> lastHitMap = new HashMap<>();

    // Cached config values
    private double maxHeat;
    private double hitDealt;
    private double hitReceived;
    private double sprintPerSecond;
    private int combatTagSeconds;
    private double idleCooldown;
    private double sneakCooldown;
    private double waterCooldown;
    private double overheatDamagePerSecond;
    private double maxHeatExtraDamage;
    private int tier1Min, tier2Min, tier3Min, overheatMin;
    private boolean streaksEnabled;
    private int comboThreshold;
    private double bonusHeat;
    private int comboResetSeconds;

    public HeatManager(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "heat_data.yml");
        loadConfig();
        loadFromDisk();
    }

    public void loadConfig() {
        var cfg = plugin.getConfig();
        maxHeat = cfg.getDouble("heat.max", 100);
        hitDealt = cfg.getDouble("heat.hit-dealt", 8);
        hitReceived = cfg.getDouble("heat.hit-received", 4);
        sprintPerSecond = cfg.getDouble("heat.sprint-per-second", 2);
        combatTagSeconds = cfg.getInt("heat.combat-tag-seconds", 5);
        idleCooldown = cfg.getDouble("heat.idle-cooldown-per-second", 3);
        sneakCooldown = cfg.getDouble("heat.sneak-cooldown-per-second", 6);
        waterCooldown = cfg.getDouble("heat.water-cooldown-per-second", 15);
        overheatDamagePerSecond = cfg.getDouble("overheat.damage-per-second", 1.0);
        maxHeatExtraDamage = cfg.getDouble("overheat.max-heat-extra-damage", 0.5);
        tier1Min = cfg.getInt("buffs.tier1-min", 25);
        tier2Min = cfg.getInt("buffs.tier2-min", 50);
        tier3Min = cfg.getInt("buffs.tier3-min", 75);
        overheatMin = cfg.getInt("buffs.overheat-min", 90);
        streaksEnabled = cfg.getBoolean("streaks.enabled", true);
        comboThreshold = cfg.getInt("streaks.combo-threshold", 3);
        bonusHeat = cfg.getDouble("streaks.bonus-heat", 4);
        comboResetSeconds = cfg.getInt("streaks.combo-reset-seconds", 3);
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    /** Load all saved heat values from heat_data.yml into the runtime map. */
    public void loadFromDisk() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.isConfigurationSection("heat")) return;

        var section = dataConfig.getConfigurationSection("heat");
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double heat = section.getDouble(key);
                if (heat > 0) {
                    heatMap.put(uuid, Math.min(heat, maxHeat));
                }
            } catch (IllegalArgumentException ignored) {
                // Corrupt key — skip it
            }
        }
        plugin.getLogger().info("Loaded heat data for " + heatMap.size() + " player(s).");
    }

    /**
     * Save a single player's heat to disk immediately.
     * Called on quit and when heat changes significantly.
     */
    public void savePlayer(UUID uuid) {
        if (dataConfig == null) dataConfig = new YamlConfiguration();
        double heat = heatMap.getOrDefault(uuid, 0.0);
        if (heat > 0) {
            dataConfig.set("heat." + uuid.toString(), heat);
        } else {
            dataConfig.set("heat." + uuid.toString(), null); // remove entry when heat is 0
        }
        writeToDisk();
    }

    /** Save all online players' heat values to disk. Called on plugin disable. */
    public void saveAll() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();
        // Write every tracked UUID
        for (Map.Entry<UUID, Double> entry : heatMap.entrySet()) {
            if (entry.getValue() > 0) {
                dataConfig.set("heat." + entry.getKey().toString(), entry.getValue());
            } else {
                dataConfig.set("heat." + entry.getKey().toString(), null);
            }
        }
        writeToDisk();
        plugin.getLogger().info("Saved heat data for " + heatMap.size() + " player(s).");
    }

    private void writeToDisk() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save heat_data.yml", e);
        }
    }

    // ─── Heat Getters / Setters ───────────────────────────────────────────────

    public double getHeat(Player player) {
        return heatMap.getOrDefault(player.getUniqueId(), 0.0);
    }

    public void setHeat(Player player, double value) {
        double clamped = Math.max(0, Math.min(maxHeat, value));
        heatMap.put(player.getUniqueId(), clamped);
        applyHeatEffects(player, clamped);
    }

    public void addHeat(Player player, double amount) {
        setHeat(player, getHeat(player) + amount);
    }

    public void removeHeat(Player player, double amount) {
        setHeat(player, getHeat(player) - amount);
    }

    public void resetHeat(Player player) {
        heatMap.put(player.getUniqueId(), 0.0);
        removeAllHeatEffects(player);
        player.setFireTicks(0);
        // Clear from disk too
        if (dataConfig != null) {
            dataConfig.set("heat." + player.getUniqueId().toString(), null);
            writeToDisk();
        }
    }

    public double getMaxHeat() { return maxHeat; }
    public int getOverheatMin() { return overheatMin; }

    // ─── Combat Tag ──────────────────────────────────────────────────────────

    public void tagCombat(Player player) {
        combatTagMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isInCombat(Player player) {
        Long last = combatTagMap.get(player.getUniqueId());
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < (combatTagSeconds * 1000L);
    }

    // ─── Combo / Streaks ─────────────────────────────────────────────────────

    public void registerHit(Player attacker) {
        if (!streaksEnabled) return;
        UUID id = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastHit = lastHitMap.get(id);

        if (lastHit == null || (now - lastHit) > (comboResetSeconds * 1000L)) {
            comboMap.put(id, 1);
        } else {
            comboMap.merge(id, 1, Integer::sum);
        }
        lastHitMap.put(id, now);
    }

    public double getHitHeatGain(Player attacker) {
        double base = hitDealt;
        if (streaksEnabled) {
            int combo = comboMap.getOrDefault(attacker.getUniqueId(), 0);
            if (combo >= comboThreshold) base += bonusHeat;
        }
        if (hasHeatEnchant(attacker)) {
            base *= plugin.getConfig().getDouble("heat-enchant.heat-gain-multiplier", 1.3);
        }
        return base;
    }

    public boolean hasHeatEnchant(Player player) {
        if (!plugin.getConfig().getBoolean("heat-enchant.enabled", true)) return false;
        var item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return false;
        var meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getLore() != null && meta.getLore().stream()
                .anyMatch(l -> l.contains("Heat Enchant"));
    }

    public double getHitReceivedGain() { return hitReceived; }
    public double getSprintHeatPerSecond() { return sprintPerSecond; }

    // ─── Cooling ─────────────────────────────────────────────────────────────

    public void tickCool(Player player) {
        if (isInCombat(player)) return;
        double current = getHeat(player);
        if (current <= 0) return;

        double loss;
        if (player.isInWater()) {
            loss = waterCooldown;
        } else if (player.isSneaking()) {
            loss = sneakCooldown;
        } else {
            loss = idleCooldown;
        }
        removeHeat(player, loss);
    }

    // ─── Overheat Damage ─────────────────────────────────────────────────────

    public void tickOverheatDamage(Player player) {
        double heat = getHeat(player);
        if (heat < overheatMin) return;
        double damage = overheatDamagePerSecond;
        if (heat >= maxHeat) damage += maxHeatExtraDamage;
        // Bypass armor and damage immunity ticks
        player.setNoDamageTicks(0);
        player.setHealth(Math.max(0, player.getHealth() - damage * 2));
        player.setNoDamageTicks(0);
    }

    // ─── Buff Application ────────────────────────────────────────────────────

    public void applyHeatEffects(Player player, double heat) {
        removeAllHeatEffects(player);
        int dur = Integer.MAX_VALUE;
        if (heat >= overheatMin) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 0, true, false, true));

        } else if (heat >= tier3Min) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1, true, false, true));
        } else if (heat >= tier2Min) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 0, true, false, true));
        } else if (heat >= tier1Min) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 0, true, false, true));
        }
    }

    private void removeAllHeatEffects(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    // ─── Cleanup ─────────────────────────────────────────────────────────────

    public void remove(UUID uuid) {
        // Save before removing from memory
        double heat = heatMap.getOrDefault(uuid, 0.0);
        if (heat > 0 && dataConfig != null) {
            dataConfig.set("heat." + uuid.toString(), heat);
            writeToDisk();
        }
        heatMap.remove(uuid);
        combatTagMap.remove(uuid);
        comboMap.remove(uuid);
        lastHitMap.remove(uuid);
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    public HeatTier getTier(double heat) {
        if (heat >= overheatMin) return HeatTier.OVERHEAT;
        if (heat >= tier3Min) return HeatTier.BLAZING;
        if (heat >= tier2Min) return HeatTier.HOT;
        if (heat >= tier1Min) return HeatTier.WARM;
        return HeatTier.COLD;
    }

    public enum HeatTier {
        COLD, WARM, HOT, BLAZING, OVERHEAT
    }
}
