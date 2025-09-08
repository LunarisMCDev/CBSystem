package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FlyCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public FlyCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.fly")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        Player targetPlayer = player;
        
        // Anderen Spieler als Ziel
        if (args.length > 0) {
            if (!player.hasPermission("cbsystem.fly.others")) {
                plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
                return true;
            }
            
            targetPlayer = plugin.getServer().getPlayer(args[0]);
            if (targetPlayer == null) {
                plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                    "player", args[0]);
                return true;
            }
        }
        
        // Flugmodus umschalten
        boolean newFlyState = !targetPlayer.getAllowFlight();
        targetPlayer.setAllowFlight(newFlyState);
        targetPlayer.setFlying(newFlyState);
        
        if (targetPlayer.equals(player)) {
            // Eigener Flugmodus
            if (newFlyState) {
                plugin.getMessageManager().sendSuccessMessage(player, "admin.fly-enabled");
            } else {
                plugin.getMessageManager().sendMessage(player, "admin.fly-disabled");
            }
        } else {
            // Flugmodus für anderen Spieler
            if (newFlyState) {
                plugin.getMessageManager().sendMessage(player, "admin.fly-enabled-other", 
                    "player", targetPlayer.getName());
                plugin.getMessageManager().sendMessage(targetPlayer, "admin.fly-enabled");
            } else {
                plugin.getMessageManager().sendMessage(player, "admin.fly-disabled-other", 
                    "player", targetPlayer.getName());
                plugin.getMessageManager().sendMessage(targetPlayer, "admin.fly-disabled");
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender.hasPermission("cbsystem.fly.others")) {
            // Online-Spieler vorschlagen
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}
