package net.mythofy.combatGhost;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombatItemRestrictListener implements Listener {
    private final CombatTagManager tagManager;
    private final Set<Material> restrictedItems;

    public CombatItemRestrictListener(CombatTagManager tagManager, FileConfiguration config) {
        this.tagManager = tagManager;
        this.restrictedItems = new HashSet<>();
        List<String> itemNames = config.getStringList("restricted-items");
        for (String name : itemNames) {
            try {
                restrictedItems.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!tagManager.isTagged(player)) return;
        if (player.getInventory().getItemInMainHand() == null) return;

        Material item = player.getInventory().getItemInMainHand().getType();
        if (restrictedItems.contains(item)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use this item while in combat!");
        }
    }
}