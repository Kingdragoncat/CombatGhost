package net.mythofy.combatGhost;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the spawning, tracking, and removal of ghost NPCs for combat logging prevention.
 * Uses the Citizens API to create player-like NPCs with configurable skins.
 * 
 * Integration points:
 * 1. Call recordLastLocation(player) in your PlayerQuitEvent handler
 * 2. Call spawnGhost(player) when a player in combat logs out
 * 3. Call removeGhost(playerId) when a player returns or the ghost should be removed
 */
public class CombatGhostNPCManager {
    private final Plugin plugin;
    private final Map<UUID, NPC> ghostNpcs = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final long ghostDurationTicks;
    private final String defaultSkin;
    private final Map<UUID, String> playerSkins = new ConcurrentHashMap<>();

    // Dependency flags
    private final boolean citizensAvailable;
    private final boolean worldGuardAvailable;

    /**
     * Constructs a CombatGhostNPCManager with a default skin.
     *
     * @param plugin              The plugin instance.
     * @param ghostDurationSeconds Duration in seconds for which the ghost NPC should remain.
     */
    public CombatGhostNPCManager(Plugin plugin, long ghostDurationSeconds) {
        this(plugin, ghostDurationSeconds, null);
    }
    
    /**
     * Constructs a CombatGhostNPCManager with a specified default skin.
     *
     * @param plugin              The plugin instance.
     * @param ghostDurationSeconds Duration in seconds for which the ghost NPC should remain.
     * @param defaultSkin         The default skin name for ghost NPCs.
     */
    public CombatGhostNPCManager(Plugin plugin, long ghostDurationSeconds, String defaultSkin) {
        this.plugin = plugin;
        this.ghostDurationTicks = ghostDurationSeconds * 20; // 20 ticks per second
        this.defaultSkin = defaultSkin;

        // Dependency checks
        PluginManager pm = Bukkit.getPluginManager();
        Plugin citizens = pm.getPlugin("Citizens");
        Plugin worldGuard = pm.getPlugin("WorldGuard");

        this.citizensAvailable = citizens != null && citizens.isEnabled();
        this.worldGuardAvailable = worldGuard != null && worldGuard.isEnabled();

        if (!citizensAvailable) {
            plugin.getLogger().severe("[CombatGhostNPCManager] Citizens plugin not found or not enabled! Combat ghost features will not work.");
        }
        if (!worldGuardAvailable) {
            plugin.getLogger().warning("[CombatGhostNPCManager] WorldGuard plugin not found or not enabled! Region protection features may not work.");
        }
    }

    /**
     * Returns true if Citizens is available and enabled.
     */
    public boolean isCitizensAvailable() {
        return citizensAvailable;
    }

    /**
     * Returns true if WorldGuard is available and enabled.
     */
    public boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }
    
    /**
     * Sets a custom skin for a player's ghost NPC.
     *
     * @param playerId The player's UUID.
     * @param skinName The skin name to use.
     */
    public void setPlayerSkin(UUID playerId, String skinName) {
        if (playerId != null && skinName != null && !skinName.isEmpty()) {
            playerSkins.put(playerId, skinName);
        }
    }
    
    /**
     * Gets the skin name for a player's ghost NPC, falling back to the default if not set.
     *
     * @param playerId The player's UUID.
     * @return The skin name.
     */
    public String getPlayerSkin(UUID playerId) {
        if (playerId == null) return defaultSkin;
        return playerSkins.getOrDefault(playerId, defaultSkin);
    }

    /**
     * Call this on PlayerQuitEvent to store the last location for offline NPC spawning.
     * This should be called BEFORE the player object becomes invalid.
     *
     * @param player The player who is about to quit
     */
    public void recordLastLocation(Player player) {
        if (player != null && player.getLocation() != null) {
            lastLocations.put(player.getUniqueId(), player.getLocation().clone());
        }
    }

    /**
     * Spawns a ghost NPC at the player's last known location with the appropriate skin.
     * If the player is online, uses their current location; otherwise, uses the last recorded location.
     *
     * @param player The player who logged out in combat.
     */
    public void spawnGhost(Player player) {
        if (!citizensAvailable) {
            plugin.getLogger().severe("Cannot spawn ghost NPC: Citizens plugin is not available!");
            return;
        }
        if (player == null) return;
        
        // Use last known location if player is offline
        Location loc = player.isOnline() ? player.getLocation() : lastLocations.get(player.getUniqueId());
        if (loc == null) {
            plugin.getLogger().warning("No location found for ghost NPC of " + player.getName() + ". Using world spawn.");
            loc = player.getWorld().getSpawnLocation();
        }
        String npcName = player.getName() + "_Ghost";
        
        // Prevent duplicate ghosts
        if (ghostNpcs.containsKey(player.getUniqueId())) {
            removeGhost(player.getUniqueId());
        }
        
        if (CitizensAPI.getNPCRegistry() == null) {
            plugin.getLogger().warning("Citizens NPC registry not available!");
            return;
        }
        
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, npcName);

        // Set skin using Citizens SkinTrait (reflection fallback)
        String skin = getPlayerSkin(player.getUniqueId());
        if (skin == null || skin.isEmpty()) {
            skin = player.getName(); // fallback to player's own skin
        }
        try {
            // Try to get SkinTrait class via reflection
            Class<?> rawSkinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
            @SuppressWarnings("unchecked")
            Class<? extends net.citizensnpcs.api.trait.Trait> skinTraitClass =
                (Class<? extends net.citizensnpcs.api.trait.Trait>) rawSkinTraitClass;
            Object skinTrait = npc.getOrAddTrait(skinTraitClass);
            Method setSkinName = skinTraitClass.getMethod("setSkinName", String.class);
            setSkinName.invoke(skinTrait, skin);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("SkinTrait class not found. Skins will not be set for ghost NPCs.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set skin for ghost NPC: " + e.getMessage());
        }

        npc.spawn(loc);

        // Note: Citizens does not support direct inventory copying to NPCs

        ghostNpcs.put(player.getUniqueId(), npc);

        // Schedule NPC removal after the configured duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeGhost(player.getUniqueId()), ghostDurationTicks);
        
        plugin.getLogger().info("Spawned combat ghost NPC for " + player.getName() + " that will remain for " + 
                                (ghostDurationTicks / 20) + " seconds");
    }

    /**
     * Removes the ghost NPC for the given player, if it exists.
     *
     * @param playerId The player's UUID.
     * @return true if a ghost was removed, false otherwise
     */
    public boolean removeGhost(UUID playerId) {
        if (!citizensAvailable) {
            plugin.getLogger().severe("Cannot remove ghost NPC: Citizens plugin is not available!");
            return false;
        }
        if (playerId == null) return false;
        
        NPC npc = ghostNpcs.remove(playerId);
        if (npc != null) {
            if (npc.isSpawned()) {
                npc.despawn();
            }
            // Deregister to prevent memory leaks
            CitizensAPI.getNPCRegistry().deregister(npc);
            plugin.getLogger().info("Removed combat ghost NPC for player ID: " + playerId);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a player has an active ghost NPC.
     *
     * @param playerId The player's UUID.
     * @return true if the player has an active ghost NPC
     */
    public boolean hasActiveGhost(UUID playerId) {
        return playerId != null && ghostNpcs.containsKey(playerId);
    }
    
    /**
     * Cleans up all ghost NPCs. Should be called when the plugin is disabled.
     */
    public void cleanup() {
        if (!citizensAvailable) {
            plugin.getLogger().severe("Cannot cleanup ghost NPCs: Citizens plugin is not available!");
            return;
        }
        for (UUID playerId : ghostNpcs.keySet()) {
            removeGhost(playerId);
        }
        ghostNpcs.clear();
        lastLocations.clear();
        plugin.getLogger().info("Cleaned up all combat ghost NPCs");
    }
}
