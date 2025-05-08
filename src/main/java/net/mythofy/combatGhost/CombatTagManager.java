package net.mythofy.combatGhost;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatTagManager {
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final long tagDurationMillis;

    public CombatTagManager(Plugin plugin, long tagDurationSeconds) {
        this.plugin = plugin;
        this.tagDurationMillis = tagDurationSeconds * 1000;
    }

    public void tagPlayer(Player player) {
        combatTags.put(player.getUniqueId(), System.currentTimeMillis());
        // Schedule tag expiry
        Bukkit.getScheduler().runTaskLater(plugin, () -> untagIfExpired(player), tagDurationMillis / 50);
    }

    public boolean isTagged(Player player) {
        Long lastCombat = combatTags.get(player.getUniqueId());
        if (lastCombat == null) return false;
        return (System.currentTimeMillis() - lastCombat) < tagDurationMillis;
    }

    public void untagIfExpired(Player player) {
        if (!isTagged(player)) {
            combatTags.remove(player.getUniqueId());
        }
    }

    public void forceUntag(Player player) {
        combatTags.remove(player.getUniqueId());
    }
}