package net.mythofy.combatGhost;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CombatGhostCommand implements CommandExecutor {

    private final CombatGhost plugin;

    public CombatGhostCommand(CombatGhost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "CombatGhost commands: /combatghost reload, /cb skin set <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("combatghost.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin.");
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "CombatGhost config reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("skin") && args.length >= 3 && args[1].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set skins.");
                return true;
            }
            Player player = (Player) sender;
            String targetSkin = args[2];
            // Store the skin preference (could be in a map or config, here just a stub)
            plugin.getCombatGhostNPCManager().setPlayerSkin(player.getUniqueId(), targetSkin);
            sender.sendMessage(ChatColor.GREEN + "Your ghost NPC skin has been set to: " + targetSkin);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command. Use /combatghost reload or /cb skin set <player>");
        return true;
    }
}