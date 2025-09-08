package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public SetHomeCommand(CBSystem plugin) {
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
        
        if (!player.hasPermission("cbsystem.home.set")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            plugin.getMessageManager().sendErrorMessage(player, "general.database-error");
            return true;
        }
        
        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        
        // Validiere Home-Namen
        if (!isValidHomeName(homeName)) {
            plugin.getMessageManager().sendMessage(player, "home.invalid-name");
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        
        // Überprüfe ob bereits existiert
        boolean homeExists = playerData.hasHome(homeName);
        
        // Überprüfe Home-Limit (nur bei neuen Homes)
        if (!homeExists && playerData.getHomeCount() >= plugin.getConfigManager().getMaxHomes()) {
            plugin.getMessageManager().sendMessage(player, "home.max-homes",
                "max", String.valueOf(plugin.getConfigManager().getMaxHomes()));
            plugin.getSoundManager().playErrorSound(player);
            return true;
        }
        
        // Überprüfe Kosten (nur bei neuen Homes)
        double cost = plugin.getConfigManager().getCostPerHome();
        if (!homeExists && cost > 0) {
            if (!plugin.getEconomyManager().hasBalance(player.getUniqueId(), cost)) {
                plugin.getMessageManager().sendMessage(player, "home.insufficient-funds",
                    "cost", plugin.getEconomyManager().formatBalance(cost));
                plugin.getSoundManager().playErrorSound(player);
                return true;
            }
            
            // Geld abziehen
            plugin.getEconomyManager().withdrawBalance(player.getUniqueId(), cost, 
                "Home gesetzt: " + homeName);
        }
        
        // Home setzen
        playerData.addHome(homeName, player.getLocation());
        
        // Erfolgs-Nachricht
        if (homeExists) {
            plugin.getMessageManager().sendSuccessMessage(player, "home.updated", "name", homeName);
        } else {
            plugin.getMessageManager().sendSuccessMessage(player, "home.set", "name", homeName);
        }
        
        // Log
        if (plugin.getConfigManager().isTeleportLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s hat Home '%s' gesetzt bei %s",
                player.getName(), homeName, locationToString(player.getLocation())));
        }
        
        return true;
    }
    
    private boolean isValidHomeName(String name) {
        // Überprüfe Länge
        if (name.length() < 1 || name.length() > 16) {
            return false;
        }
        
        // Überprüfe Zeichen (nur Buchstaben, Zahlen und Unterstriche)
        return name.matches("^[a-zA-Z0-9_]+$");
    }
    
    private String locationToString(org.bukkit.Location location) {
        return String.format("%s:%.1f,%.1f,%.1f",
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            if (playerData != null) {
                // Existierende Home-Namen vorschlagen (zum Überschreiben)
                for (String homeName : playerData.getHomes().keySet()) {
                    if (homeName.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(homeName);
                    }
                }
            }
            
            // Standard-Namen vorschlagen
            List<String> defaultNames = Arrays.asList("home", "base", "farm", "shop", "mine");
            for (String defaultName : defaultNames) {
                if (defaultName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(defaultName);
                }
            }
        }
        
        return completions;
    }
}
