package net.mythofy.combatGhost;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LogoutListener implements Listener {
    private final CombatTagManager tagManager;
    private final CombatGhostNPCManager ghostNPCManager;

    public LogoutListener(CombatTagManager tagManager, CombatGhostNPCManager ghostNPCManager) {
        this.tagManager = tagManager;
        this.ghostNPCManager = ghostNPCManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (tagManager.isTagged(player)) {
            ghostNPCManager.spawnGhost(player);
            Bukkit.getLogger().info("[CombatGhost] " + player.getName() + " logged out while in combat! Spawned ghost NPC.");
            // Optionally: tagManager.forceUntag(player); // If you want to clear the tag now
        }
    }
}
