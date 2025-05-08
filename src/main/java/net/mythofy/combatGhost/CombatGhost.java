package net.mythofy.combatGhost;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatGhost extends JavaPlugin {

    private CombatTagManager combatTagManager;
    private CombatGhostNPCManager combatGhostNPCManager;
    private String npcDefaultSkin;

    @Override
    public void onEnable() {
        // Dependency check
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe("Citizens plugin not found! Disabling CombatGhost.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard plugin not found! Disabling CombatGhost.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load config
        saveDefaultConfig();
        reloadPluginConfig();
        
        getServer().getPluginManager().registerEvents(new CombatListener(combatTagManager), this);
        getServer().getPluginManager().registerEvents(new LogoutListener(combatTagManager, combatGhostNPCManager), this);
        getServer().getPluginManager().registerEvents(new CombatRegionListener(combatTagManager, getConfig()), this);
        getServer().getPluginManager().registerEvents(new CombatItemRestrictListener(combatTagManager, getConfig()), this);
        
        // Register commands with null checks
        PluginCommand combatghostCmd = getCommand("combatghost");
        if (combatghostCmd != null) {
            combatghostCmd.setExecutor(new CombatGhostCommand(this));
        } else {
            getLogger().warning("Command 'combatghost' not found in plugin.yml!");
        }
        
        PluginCommand cbCmd = getCommand("cb");
        if (cbCmd != null) {
            cbCmd.setExecutor(new CombatGhostCommand(this));
        } else {
            getLogger().warning("Command 'cb' not found in plugin.yml!");
        }
    }
    
    public void reloadPluginConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        int tagDuration = config.getInt("combat-tag-duration", 15);
        int ghostDuration = config.getInt("ghost-duration", 30);
        npcDefaultSkin = config.getString("npc-default-skin", "Steve");

        combatTagManager = new CombatTagManager(this, tagDuration);
        combatGhostNPCManager = new CombatGhostNPCManager(this, ghostDuration, npcDefaultSkin);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    
    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }
    
    public CombatGhostNPCManager getCombatGhostNPCManager() {
        return combatGhostNPCManager;
    }
    
    public String getNpcDefaultSkin() {
        return npcDefaultSkin;
    }
}

