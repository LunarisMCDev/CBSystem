package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public SetWarpCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.admin.setwarp")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "warp.setwarp-usage");
            return true;
        }
        
        String warpName = args[0].toLowerCase();
        
        // Hier würde der Warp in die Datenbank gespeichert werden
        plugin.getMessageManager().sendSuccessMessage(player, "warp.set", "name", warpName);
        plugin.getLogger().info("Warp '" + warpName + "' wurde von " + player.getName() + " gesetzt.");
        
        return true;
    }
}
