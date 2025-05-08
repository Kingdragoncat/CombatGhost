package net.mythofy.combatGhost;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class CombatRegionListener implements Listener {
    private final CombatTagManager tagManager;
    private final List<String> restrictedRegions;

    public CombatRegionListener(CombatTagManager tagManager, FileConfiguration config) {
        this.tagManager = tagManager;
        this.restrictedRegions = config.getStringList("restricted-regions");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!tagManager.isTagged(player)) return;
        if (event.getTo() == null || event.getFrom().getBlock().equals(event.getTo().getBlock())) return;

        Location to = event.getTo();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(to));

        for (String region : restrictedRegions) {
            if (set.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(region))) {
                // Cancel movement and send message
                event.setTo(event.getFrom());
                player.sendMessage("§cYou cannot enter this area while in combat!");
                return;
            }
        }
    }
}