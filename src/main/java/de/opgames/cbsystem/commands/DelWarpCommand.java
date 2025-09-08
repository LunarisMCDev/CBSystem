package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelWarpCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public DelWarpCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.delwarp")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(sender, "warp.delwarp-usage");
            return true;
        }
        
        String warpName = args[0].toLowerCase();
        
        // Hier würde der Warp aus der Datenbank gelöscht werden
        plugin.getMessageManager().sendMessage(sender, "warp.deleted", "name", warpName);
        plugin.getLogger().info("Warp '" + warpName + "' wurde von " + sender.getName() + " gelöscht.");
        
        return true;
    }
}
