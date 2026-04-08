package dev.heatsmp;

import dev.heatsmp.abilities.AbilityManager;
import dev.heatsmp.commands.*;
import dev.heatsmp.heat.HeatManager;
import dev.heatsmp.items.CoolingPearl;
import dev.heatsmp.listeners.*;
import dev.heatsmp.rods.RodManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HeatSMPPlugin extends JavaPlugin {

    private static HeatSMPPlugin instance;
    private HeatManager heatManager;
    private AbilityManager abilityManager;
    private CoolingPearl coolingPearl;
    private RodManager rodManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        heatManager = new HeatManager(this);
        abilityManager = new AbilityManager(this);
        coolingPearl = new CoolingPearl(this); // registers its own listener
        rodManager = new RodManager(this);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new HeatTickListener(this), this);
        getServer().getPluginManager().registerEvents(new CoolingPearlListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new RodListener(this), this);
        getServer().getPluginManager().registerEvents(new MaterialCheckListener(this), this);

        getCommand("heat").setExecutor(new HeatCommand(this));
        getCommand("heatadmin").setExecutor(new HeatAdminCommand(this));
        getCommand("ability").setExecutor(new AbilityCommand(this));
        getCommand("recipe").setExecutor(new RecipeCommand(this));

        // Restore effects for online players after /reload
        for (Player player : getServer().getOnlinePlayers()) {
            double heat = heatManager.getHeat(player);
            heatManager.applyHeatEffects(player, heat);
            if (heat <= 0) player.setFireTicks(0);
        }

        getLogger().info("HeatSMP v1.1 enabled! The fire rises. 🔥");
    }

    @Override
    public void onDisable() {
        if (heatManager != null) heatManager.saveAll();
        getLogger().info("HeatSMP disabled.");
    }

    public static HeatSMPPlugin getInstance() { return instance; }
    public HeatManager getHeatManager() { return heatManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public CoolingPearl getCoolingPearl() { return coolingPearl; }
    public RodManager getRodManager() { return rodManager; }
}
