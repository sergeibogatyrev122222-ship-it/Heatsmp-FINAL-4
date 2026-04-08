package dev.heatsmp.rods;

import dev.heatsmp.HeatSMPPlugin;
import dev.heatsmp.heat.HeatManager;
import dev.heatsmp.heat.HeatParticles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RodManager {

    public enum RodType {
        EMBER, SCORCH, INFERNO, MOLTEN, ASSASSIN
    }

    private final HeatSMPPlugin plugin;
    private final HeatParticles particles;
    private final NamespacedKey rodKey;

    private final Map<UUID, Long> emberRightCd  = new HashMap<>();
    private final Map<UUID, Long> emberLeftCd   = new HashMap<>();
    private final Map<UUID, Long> scorchRightCd = new HashMap<>();
    private final Map<UUID, Long> scorchLeftCd  = new HashMap<>();
    private final Map<UUID, Long> infernRightCd = new HashMap<>();
    private final Map<UUID, Long> infernLeftCd  = new HashMap<>();
    private final Map<UUID, Long> moltenRightCd = new HashMap<>();
    private final Map<UUID, Long> moltenLeftCd  = new HashMap<>();
    private final Map<UUID, Long> assassinRightCd = new HashMap<>();
    private final Map<UUID, Long> assassinLeftCd  = new HashMap<>();

    private final Set<UUID> moltenArmorActive = new HashSet<>();
    private final Set<UUID> assassinInvisActive = new HashSet<>();

    // Global damage cap in half-hearts (5 hearts = 10)
    private static final double MAX_DAMAGE = 10.0;

    public RodManager(HeatSMPPlugin plugin) {
        this.plugin = plugin;
        this.particles = new HeatParticles(plugin);
        this.rodKey = new NamespacedKey(plugin, "rod_type");
        registerRecipes();
    }

    // ─── Item Creation ────────────────────────────────────────────────────────

    public ItemStack createRod(RodType type) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        switch (type) {
            case EMBER -> {
                meta.displayName(Component.text("🔥 Ember Rod").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: Pull nearby players toward you").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Left Click:  Blast players away from you").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Heat Cost: 15 | Tier I").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case SCORCH -> {
                meta.displayName(Component.text("💥 Scorch Rod").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: Ground Slam — crash down (9 block AOE)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Left Click:  Launch yourself into the air").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Heat Cost: 25 | Tier II").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case INFERNO -> {
                meta.displayName(Component.text("🌋 Inferno Rod").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: Steal 20 heat from nearest player").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Left Click:  Dump 15 heat onto nearest player").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Heat Cost: 35 | Tier III").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
            }
            case MOLTEN -> {
                meta.displayName(Component.text("☄ Molten Rod").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: Molten Armor — absorb hits as heat").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Left Click:  Heat Nova — detonate all your heat").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Requires 50 heat | Tier IV ⭐").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            case ASSASSIN -> {
                meta.displayName(Component.text("🗡 Assassin's Rod").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
                meta.lore(List.of(
                    Component.text("Right Click: Full invisibility — hides armor & nametag").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Left Click:  Blind the player you hit (35s cooldown)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Tier V | Stealth").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(rodKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public RodType getRodType(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String val = meta.getPersistentDataContainer().get(rodKey, PersistentDataType.STRING);
        if (val == null) return null;
        try { return RodType.valueOf(val); } catch (Exception e) { return null; }
    }

    // ─── Recipes ──────────────────────────────────────────────────────────────

    private void registerRecipes() {
        // EMBER ROD
        ShapedRecipe ember = new ShapedRecipe(new NamespacedKey(plugin, "ember_rod"), createRod(RodType.EMBER));
        ember.shape(" B ", " S ", " S ");
        ember.setIngredient('B', Material.BLAZE_ROD);
        ember.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(ember);

        // SCORCH ROD
        ShapedRecipe scorch = new ShapedRecipe(new NamespacedKey(plugin, "scorch_rod"), createRod(RodType.SCORCH));
        scorch.shape("FBF", " B ", " B ");
        scorch.setIngredient('B', Material.BLAZE_ROD);
        scorch.setIngredient('F', Material.FIRE_CHARGE);
        plugin.getServer().addRecipe(scorch);

        // INFERNO ROD
        ShapedRecipe inferno = new ShapedRecipe(new NamespacedKey(plugin, "inferno_rod"), createRod(RodType.INFERNO));
        inferno.shape("MBM", "MBM", " B ");
        inferno.setIngredient('B', Material.BLAZE_ROD);
        inferno.setIngredient('M', Material.MAGMA_BLOCK);
        plugin.getServer().addRecipe(inferno);

        // MOLTEN ROD
        ShapedRecipe molten = new ShapedRecipe(new NamespacedKey(plugin, "molten_rod"), createRod(RodType.MOLTEN));
        molten.shape("NLN", "MBM", "MBM");
        molten.setIngredient('B', Material.BLAZE_ROD);
        molten.setIngredient('M', Material.MAGMA_BLOCK);
        molten.setIngredient('N', Material.NETHERITE_INGOT);
        molten.setIngredient('L', Material.LAVA_BUCKET);
        plugin.getServer().addRecipe(molten);

        // ASSASSIN ROD
        ShapedRecipe assassin = new ShapedRecipe(new NamespacedKey(plugin, "assassin_rod"), createRod(RodType.ASSASSIN));
        assassin.shape("WDW", "RBR", "   ");
        assassin.setIngredient('W', Material.GLASS_BOTTLE);
        assassin.setIngredient('D', Material.DIAMOND_BLOCK);
        assassin.setIngredient('R', Material.BLAZE_ROD);
        assassin.setIngredient('B', Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        plugin.getServer().addRecipe(assassin);
    }

    // ─── Ability Dispatch ─────────────────────────────────────────────────────

    public void useRightClick(Player player, RodType type) {
        HeatManager hm = plugin.getHeatManager();
        switch (type) {
            case EMBER    -> emberPull(player, hm);
            case SCORCH   -> scorchSlam(player, hm);
            case INFERNO  -> infernoSteal(player, hm);
            case MOLTEN   -> moltenArmor(player, hm);
            case ASSASSIN -> assassinInvis(player);
        }
    }

    public void useLeftClick(Player player, RodType type) {
        HeatManager hm = plugin.getHeatManager();
        switch (type) {
            case EMBER    -> emberBlast(player, hm);
            case SCORCH   -> scorchLaunch(player, hm);
            case INFERNO  -> infernoDump(player, hm);
            case MOLTEN   -> heatNova(player, hm);
            case ASSASSIN -> {} // blind is handled on hit in RodListener
        }
    }

    // ─── EMBER ROD ────────────────────────────────────────────────────────────

    private void emberPull(Player player, HeatManager hm) {
        if (onCooldown(emberRightCd, player, 6000)) { sendCd(player, emberRightCd, player.getUniqueId(), 6000); return; }
        if (hm.getHeat(player) < 15) { noHeat(player); return; }

        Location center = player.getLocation();
        int pulled = 0;
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player target)) continue;
            Vector dir = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.8);
            dir.setY(0.3);
            target.setVelocity(dir);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.05);
            pulled++;
        }
        if (pulled == 0) { player.sendMessage(Component.text("No players nearby!").color(NamedTextColor.GRAY)); return; }
        emberRightCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
        player.sendMessage(Component.text("🔥 Ember Pull! Drew in " + pulled + " player(s).").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    private void emberBlast(Player player, HeatManager hm) {
        if (onCooldown(emberLeftCd, player, 5000)) { sendCd(player, emberLeftCd, player.getUniqueId(), 5000); return; }
        if (hm.getHeat(player) < 15) { noHeat(player); return; }

        // Blast all nearby players away from player
        Location center = player.getLocation();
        int blasted = 0;
        for (Entity e : player.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof Player target)) continue;
            Vector dir = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.2);
            dir.setY(0.5);
            target.setVelocity(dir);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 10, 0.4, 0.3, 0.4, 0.06);
            blasted++;
        }
        if (blasted == 0) { player.sendMessage(Component.text("No players nearby!").color(NamedTextColor.GRAY)); return; }

        player.getWorld().playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
        emberLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🔥 Ember Blast! Pushed " + blasted + " player(s) away.").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    // ─── SCORCH ROD ───────────────────────────────────────────────────────────

    private void scorchSlam(Player player, HeatManager hm) {
        if (onCooldown(scorchRightCd, player, 8000)) { sendCd(player, scorchRightCd, player.getUniqueId(), 8000); return; }
        if (hm.getHeat(player) < 25) { noHeat(player); return; }

        // Launch up first
        player.setVelocity(new Vector(0, 2.5, 0));
        scorchRightCd.put(player.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            int ticks = 0;
            boolean crashed = false;
            @Override public void run() {
                ticks++;
                if (!player.isOnline()) { cancel(); return; }

                if (!crashed && ticks > 15 && player.getVelocity().getY() < 0) {
                    crashed = true;
                    player.setVelocity(new Vector(0, -3.5, 0));
                    cancel();

                    // Schedule impact check
                    new BukkitRunnable() {
                        int wait = 0;
                        @Override public void run() {
                            wait++;
                            if (!player.isOnline()) { cancel(); return; }
                            // Detect landing (low Y velocity or on ground)
                            if (player.isOnGround() || wait > 40) {
                                cancel();
                                Location impact = player.getLocation();
                                spawnSlamSpiral(impact);
                                player.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);

                                // Damage nearby players with distance falloff
                                for (Entity e : player.getNearbyEntities(9, 5, 9)) {
                                    if (!(e instanceof Player target)) continue;
                                    double dist = target.getLocation().distance(impact);
                                    double dmg = getSlamDamage(dist);
                                    if (dmg > 0) {
                                        dealDamage(target, dmg * 2, player); // *2 because damage is in half-hearts internally
                                        hm.addHeat(target, 10);
                                        Vector kb = target.getLocation().subtract(impact).toVector().normalize().multiply(1.5);
                                        kb.setY(0.6);
                                        target.setVelocity(kb);
                                    }
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 5L, 1L);
                }
                if (ticks > 80) cancel();
            }
        }.runTaskTimer(plugin, 5L, 1L);

        // No fall damage for this ability
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // handled in RodListener via NoDamageFall tag
        }, 1L);

        player.sendMessage(Component.text("💥 Ground Slam!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private double getSlamDamage(double dist) {
        // Returns damage in hearts (will be doubled when passed to .damage())
        if (dist <= 1) return 7;
        if (dist <= 2) return 6;
        if (dist <= 3) return 5;
        if (dist <= 4) return 4;
        if (dist <= 5) return 3;
        if (dist <= 6) return 2;
        if (dist <= 7) return 1.5;
        if (dist <= 8) return 1;
        if (dist <= 9) return 0.5;
        return 0;
    }

    private void spawnSlamSpiral(Location center) {
        // Outward burst
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick > 8) { cancel(); startSlamSpiralIn(center, 5); return; }
                double r = (tick / 8.0) * 5;
                for (int i = 0; i < 16; i++) {
                    double angle = (i / 16.0) * 2 * Math.PI;
                    Location p = center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                    Particle part = (i % 3 == 0) ? Particle.FLAME : (i % 3 == 1) ? Particle.LAVA : Particle.LARGE_SMOKE;
                    center.getWorld().spawnParticle(part, p, 2, 0.1, 0.1, 0.1, 0.02);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startSlamSpiralIn(Location center, double maxRadius) {
        new BukkitRunnable() {
            int tick = 0;
            double angle = 0;
            @Override public void run() {
                if (tick >= 25) { spawnImplosion(center); cancel(); return; }
                double progress = 1.0 - ((double) tick / 25);
                double r = maxRadius * progress;
                angle += 22;
                for (int arm = 0; arm < 3; arm++) {
                    double a = Math.toRadians(angle + (arm * 120));
                    double y = 0.3 + (1 - progress) * 1.2;
                    Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    Particle color = tick < 10 ? Particle.FLAME : Particle.LAVA;
                    center.getWorld().spawnParticle(color, p, 3, 0.05, 0.05, 0.05, 0.01);
                }
                if (tick % 7 == 0) {
                    float pitch = 0.5f + (tick / 25f) * 1.5f;
                    center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.4f, pitch);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 8L, 1L);
    }

    private void scorchLaunch(Player player, HeatManager hm) {
        if (onCooldown(scorchLeftCd, player, 7000)) { sendCd(player, scorchLeftCd, player.getUniqueId(), 7000); return; }
        if (hm.getHeat(player) < 25) { noHeat(player); return; }

        player.setVelocity(new Vector(0, 3.2, 0));
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 20, 0.5, 0.1, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        scorchLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("💥 Scorch Launch!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    // ─── INFERNO ROD ──────────────────────────────────────────────────────────

    private void infernoSteal(Player player, HeatManager hm) {
        if (onCooldown(infernRightCd, player, 10000)) { sendCd(player, infernRightCd, player.getUniqueId(), 10000); return; }
        if (hm.getHeat(player) < 35) { noHeat(player); return; }

        Player target = getNearestPlayer(player, 10);
        if (target == null) { player.sendMessage(Component.text("No player in range!").color(NamedTextColor.GRAY)); return; }

        double steal = Math.min(20, hm.getHeat(target));
        hm.removeHeat(target, steal);
        hm.addHeat(player, steal + 35);

        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.6f);

        infernRightCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🌋 Heat Stolen from " + target.getName() + "! +" + (int) steal + " heat.").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        target.sendMessage(Component.text("🌋 " + player.getName() + " stole " + (int) steal + " heat from you!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    private void infernoDump(Player player, HeatManager hm) {
        if (onCooldown(infernLeftCd, player, 8000)) { sendCd(player, infernLeftCd, player.getUniqueId(), 8000); return; }
        if (hm.getHeat(player) < 35) { noHeat(player); return; }

        Player target = getNearestPlayer(player, 10);
        if (target == null) { player.sendMessage(Component.text("No player in range!").color(NamedTextColor.GRAY)); return; }

        hm.removeHeat(player, 15 + 35);
        hm.addHeat(target, 15);

        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1.5f);

        infernLeftCd.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(Component.text("🌋 Heat Dumped onto " + target.getName() + "!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        target.sendMessage(Component.text("🌋 " + player.getName() + " dumped heat onto you! +15 heat!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    // ─── MOLTEN ROD ───────────────────────────────────────────────────────────

    private void moltenArmor(Player player, HeatManager hm) {
        if (onCooldown(moltenRightCd, player, 20000)) { sendCd(player, moltenRightCd, player.getUniqueId(), 20000); return; }
        if (hm.getHeat(player) < 50) { noHeat(player); return; }

        double heat = hm.getHeat(player);
        int duration = heat >= 90 ? 160 : 100;

        moltenArmorActive.add(player.getUniqueId());
        moltenRightCd.put(player.getUniqueId(), System.currentTimeMillis());

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!player.isOnline() || tick >= duration) {
                    moltenArmorActive.remove(player.getUniqueId());
                    player.sendMessage(Component.text("☄ Molten Armor faded.").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                    cancel(); return;
                }
                if (tick % 3 == 0) {
                    double a = Math.toRadians(tick * 15);
                    Location loc = player.getLocation().add(Math.cos(a) * 1.2, 1, Math.sin(a) * 1.2);
                    player.getWorld().spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.sendMessage(Component.text("☄ Molten Armor active!").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
    }

    public boolean isMoltenArmorActive(Player player) {
        return moltenArmorActive.contains(player.getUniqueId());
    }

    public void absorbHitAsMoltenArmor(Player player, Player attacker) {
        plugin.getHeatManager().addHeat(attacker, 18);
        attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.06);
        attacker.sendMessage(Component.text("☄ Your attack was absorbed by Molten Armor!").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    private void heatNova(Player player, HeatManager hm) {
        if (onCooldown(moltenLeftCd, player, 25000)) { sendCd(player, moltenLeftCd, player.getUniqueId(), 25000); return; }
        if (hm.getHeat(player) < 50) {
            player.sendMessage(Component.text("Heat Nova requires at least 50 heat!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            return;
        }

        double heat = hm.getHeat(player);
        // 50 heat = 6 hearts, every 10 more = +1.5 hearts, cap at 7 hearts (14 half-hearts)
        double extraHearts = Math.floor((heat - 50) / 10.0) * 1.5;
        double damageHearts = Math.min(5.0, 6.0 + extraHearts); // capped at 5 hearts
        double damageRaw = damageHearts * 2; // convert to Minecraft damage units

        Location center = player.getLocation().clone();

        for (Entity e : player.getNearbyEntities(6, 4, 6)) {
            if (!(e instanceof Player t)) continue;
            dealDamage(t, damageRaw, player);
            hm.addHeat(t, (int) (heat * 0.3));
            Vector kb = t.getLocation().subtract(center).toVector().normalize().multiply(2.5);
            kb.setY(0.8);
            t.setVelocity(kb);
        }

        hm.resetHeat(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true));
        moltenLeftCd.put(player.getUniqueId(), System.currentTimeMillis());

        spawnHeatNovaSpiral(center, heat);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
        player.sendMessage(Component.text("☄ HEAT NOVA! " + String.format("%.1f", damageHearts) + " hearts of damage!").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
    }

    private void spawnHeatNovaSpiral(Location center, double heat) {
        double radius = Math.min(heat / 10.0, 6);
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (tick > 10) { cancel(); startNovaSpiral(center, radius); return; }
                double progress = (double) tick / 10;
                double r = radius * progress;
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * 2 * Math.PI;
                    Location p = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                    Particle part = (i % 3 == 0) ? Particle.FLAME : (i % 3 == 1) ? Particle.LAVA : Particle.LARGE_SMOKE;
                    center.getWorld().spawnParticle(part, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startNovaSpiral(Location center, double maxRadius) {
        new BukkitRunnable() {
            int tick = 0;
            double spiralAngle = 0;
            @Override public void run() {
                if (tick >= 30) { spawnImplosion(center); cancel(); return; }
                double progress = 1.0 - ((double) tick / 30);
                double r = maxRadius * progress;
                spiralAngle += 25;
                for (int arm = 0; arm < 3; arm++) {
                    double a = Math.toRadians(spiralAngle + (arm * 120));
                    double y = 0.3 + (1 - progress) * 1.5;
                    Location p = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    Particle color = tick < 10 ? Particle.FLAME : tick < 20 ? Particle.LAVA : Particle.FLAME;
                    center.getWorld().spawnParticle(color, p, 3, 0.05, 0.05, 0.05, 0.01);
                }
                if (tick % 8 == 0) {
                    float pitch = 0.5f + (tick / 30f) * 1.5f;
                    center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5f, pitch);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 10L, 1L);
    }

    private void spawnImplosion(Location center) {
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.05);
        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.02);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1.5f);
    }

    // ─── ASSASSIN ROD ─────────────────────────────────────────────────────────

    private void assassinInvis(Player player) {
        if (onCooldown(assassinRightCd, player, 15000)) { sendCd(player, assassinRightCd, player.getUniqueId(), 15000); return; }

        assassinInvisActive.add(player.getUniqueId());
        assassinRightCd.put(player.getUniqueId(), System.currentTimeMillis());

        // Full invisibility — hide armor and nametag for nearby players
        for (Player other : player.getWorld().getPlayers()) {
            if (!other.equals(player)) other.hidePlayer(plugin, player);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, false));
        player.sendMessage(Component.text("🗡 Assassin's Veil active! 5 seconds...").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));

        // Show ghost particles only to the player themselves so they know where they are
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!player.isOnline() || tick >= 100) {
                    cancel();
                    assassinInvisActive.remove(player.getUniqueId());
                    // Reveal player again
                    for (Player other : player.getWorld().getPlayers()) {
                        if (!other.equals(player)) other.showPlayer(plugin, player);
                    }
                    player.sendMessage(Component.text("🗡 You reappear from the shadows.").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
                    return;
                }
                // Subtle smoke so the invis player can see themselves
                player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 1, 0.2, 0.3, 0.2, 0.01);
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public boolean isAssassinInvisActive(Player player) {
        return assassinInvisActive.contains(player.getUniqueId());
    }

    /** Called from RodListener when assassin rod holder hits a player */
    public void applyAssassinBlind(Player attacker, Player victim) {
        if (onCooldown(assassinLeftCd, attacker, 35000)) {
            sendCd(attacker, assassinLeftCd, attacker.getUniqueId(), 35000);
            return;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, true, true)); // 3 seconds
        assassinLeftCd.put(attacker.getUniqueId(), System.currentTimeMillis());
        attacker.sendMessage(Component.text("🗡 " + victim.getName() + " is blinded!").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
        victim.sendMessage(Component.text("🗡 You have been blinded!").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────


    // ─── Damage Helper (ignores armor, bypasses immunity ticks) ──────────────

    private void dealDamage(Player target, double rawDamage, Player source) {
        double capped = Math.min(rawDamage, MAX_DAMAGE);
        // Bypass armor by using generic damage then attribute damage
        target.damage(0.001, source); // trigger combat tag
        target.setNoDamageTicks(0);
        target.setHealth(Math.max(0, target.getHealth() - capped));
        target.setNoDamageTicks(0);
        // Visual feedback
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
            target.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.1);
    }

    private double cap(double damage) {
        return Math.min(damage, MAX_DAMAGE);
    }

    private Player getNearestPlayer(Player player, double range) {
        Player nearest = null;
        double closest = range * range;
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Player t)) continue;
            double dist = t.getLocation().distanceSquared(player.getLocation());
            if (dist < closest) { closest = dist; nearest = t; }
        }
        return nearest;
    }

    private boolean onCooldown(Map<UUID, Long> map, Player player, long ms) {
        Long last = map.get(player.getUniqueId());
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < ms;
    }

    private void sendCd(Player player, Map<UUID, Long> map, UUID id, long ms) {
        Long last = map.get(id);
        if (last == null) return;
        double remaining = (ms - (System.currentTimeMillis() - last)) / 1000.0;
        player.sendMessage(Component.text("Ability on cooldown for " + String.format("%.1f", remaining) + "s!").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
    }

    private void noHeat(Player player) {
        player.sendMessage(Component.text("Not enough heat!").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    }

    public void clearCooldowns(UUID uuid) {
        emberRightCd.remove(uuid); emberLeftCd.remove(uuid);
        scorchRightCd.remove(uuid); scorchLeftCd.remove(uuid);
        infernRightCd.remove(uuid); infernLeftCd.remove(uuid);
        moltenRightCd.remove(uuid); moltenLeftCd.remove(uuid);
        assassinRightCd.remove(uuid); assassinLeftCd.remove(uuid);
        moltenArmorActive.remove(uuid);
        assassinInvisActive.remove(uuid);
    }

    public RodType[] getAllTypes() {
        return RodType.values();
    }
}
