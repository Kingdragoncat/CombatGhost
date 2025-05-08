package net.mythofy.combatGhost;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatGhostNPCManager {
    private final Plugin plugin;
    private final Map<UUID, NPC> ghostNpcs = new HashMap<>();
    private final long ghostDurationTicks;
    private final String defaultSkin;
    private final Map<UUID, String> playerSkins = new HashMap<>();

    public CombatGhostNPCManager(Plugin plugin, long ghostDurationSeconds) {
        this(plugin, ghostDurationSeconds, null);
    }
    
    public CombatGhostNPCManager(Plugin plugin, long ghostDurationSeconds, String defaultSkin) {
        this.plugin = plugin;
        this.ghostDurationTicks = ghostDurationSeconds * 20; // 20 ticks per second
        this.defaultSkin = defaultSkin;
    }
    
    public void setPlayerSkin(UUID playerId, String skinName) {
        playerSkins.put(playerId, skinName);
    }
    
    public String getPlayerSkin(UUID playerId) {
        return playerSkins.getOrDefault(playerId, defaultSkin);
    }

    public void spawnGhost(Player player) {
        Location loc = player.getLocation();
        String npcName = player.getName() + "_Ghost";
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, npcName);

        // Set skin using Citizens API
        String skin = getPlayerSkin(player.getUniqueId());
        if (skin != null) {
            npc.data().setPersistent("player-skin-name", skin);
        } else {
            // Use player's own skin if no custom skin is set
            npc.data().setPersistent("player-skin-name", player.getName());
        }

        npc.spawn(loc);

        // Copy inventory (optional: you can expand this to armor, effects, etc.)
        if (npc.getEntity() instanceof org.bukkit.entity.Player) {
            org.bukkit.entity.Player npcPlayer = (org.bukkit.entity.Player) npc.getEntity();
            npcPlayer.getInventory().setContents(player.getInventory().getContents());
            npcPlayer.getInventory().setArmorContents(player.getInventory().getArmorContents());
        }

        ghostNpcs.put(player.getUniqueId(), npc);

        // Schedule NPC removal
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeGhost(player.getUniqueId()), ghostDurationTicks);
    }

    public void removeGhost(UUID playerId) {
        NPC npc = ghostNpcs.remove(playerId);
        if (npc != null && npc.isSpawned()) {
            npc.despawn();
            CitizensAPI.getNPCRegistry().deregister(npc);
        }
    }
}
