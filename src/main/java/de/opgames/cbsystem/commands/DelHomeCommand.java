package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DelHomeCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public DelHomeCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getConfigManager().isHomesEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.home.delete")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "home.delete-usage");
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            plugin.getMessageManager().sendErrorMessage(player, "general.database-error");
            return true;
        }
        
        String homeName = args[0].toLowerCase();
        
        if (!playerData.hasHome(homeName)) {
            plugin.getMessageManager().sendMessage(player, "home.not-found", "name", homeName);
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        
        // Bestätigung bei wichtigen Homes
        if (homeName.equals("home") || homeName.equals("base")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                plugin.getMessageManager().sendMessage(player, "home.delete-confirm", "name", homeName);
                plugin.getMessageManager().sendRawMessage(player, 
                    "&7Verwende &c/delhome " + homeName + " confirm &7zum Bestätigen.");
                plugin.getSoundManager().playWarningSound(player);
                return true;
            }
        }
        
        // Home löschen
        if (playerData.removeHome(homeName)) {
            plugin.getMessageManager().sendSuccessMessage(player, "home.deleted", "name", homeName);
            
            // Log
            if (plugin.getConfigManager().isTeleportLoggingEnabled()) {
                plugin.getLogger().info(String.format("Spieler %s hat Home '%s' gelöscht",
                    player.getName(), homeName));
            }
        } else {
            plugin.getMessageManager().sendErrorMessage(player, "home.delete-failed");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (sender instanceof Player player) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            if (playerData != null) {
                if (args.length == 1) {
                    // Home-Namen vorschlagen
                    for (String homeName : playerData.getHomes().keySet()) {
                        if (homeName.toLowerCase().startsWith(args[0].toLowerCase())) {
                            completions.add(homeName);
                        }
                    }
                } else if (args.length == 2) {
                    // "confirm" vorschlagen für wichtige Homes
                    String homeName = args[0].toLowerCase();
                    if ((homeName.equals("home") || homeName.equals("base")) && 
                        "confirm".startsWith(args[1].toLowerCase())) {
                        completions.add("confirm");
                    }
                }
            }
        }
        
        return completions;
    }
}
