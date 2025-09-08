package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public HomeCommand(CBSystem plugin) {
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
        
        if (!player.hasPermission("cbsystem.home.use")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            plugin.getMessageManager().sendErrorMessage(player, "general.database-error");
            return true;
        }
        
        if (args.length == 0) {
            // Kein Home-Name angegeben
            if (playerData.getHomeCount() == 0) {
                plugin.getMessageManager().sendMessage(player, "home.list-empty");
                return true;
            }
            
            if (playerData.getHomeCount() == 1) {
                // Nur ein Home vorhanden - direkt teleportieren
                String homeName = playerData.getHomes().keySet().iterator().next();
                teleportToHome(player, homeName);
            } else {
                // Mehrere Homes - GUI öffnen
                plugin.getGUIManager().openHomeManagementGUI(player);
                plugin.getSoundManager().playOpenSound(player);
            }
            return true;
        }
        
        String homeName = args[0].toLowerCase();
        
        // Spezielle Befehle
        if (homeName.equals("list") || homeName.equals("liste")) {
            showHomeList(player, playerData);
            return true;
        }
        
        // Zu spezifischem Home teleportieren
        teleportToHome(player, homeName);
        return true;
    }
    
    private void teleportToHome(Player player, String homeName) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) return;
        
        PlayerData.HomeLocation home = playerData.getHome(homeName);
        if (home == null) {
            plugin.getMessageManager().sendMessage(player, "home.not-found", "name", homeName);
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        if (!home.isWorldLoaded()) {
            plugin.getMessageManager().sendMessage(player, "error.world-not-found", "world", home.getWorld());
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        Location location = home.toBukkitLocation();
        if (location == null) {
            plugin.getMessageManager().sendMessage(player, "home.teleport-failed");
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        // Überprüfe Cooldown
        if (playerData.hasCooldown("home")) {
            plugin.getMessageManager().sendMessage(player, "cooldown.active",
                "time", String.valueOf(playerData.getCooldownRemaining("home")));
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        // Setze Cooldown
        playerData.setCooldown("home", plugin.getConfigManager().getHomeTeleportCooldown());
        
        // Teleportiere
        plugin.getTeleportManager().startTeleport(player, location, "Home: " + homeName);
        plugin.getMessageManager().sendMessage(player, "home.teleporting", "name", homeName);
    }
    
    private void showHomeList(Player player, PlayerData playerData) {
        if (playerData.getHomeCount() == 0) {
            plugin.getMessageManager().sendMessage(player, "home.list-empty");
            return;
        }
        
        plugin.getMessageManager().sendMessage(player, "home.list-header");
        
        for (String homeName : playerData.getHomes().keySet()) {
            PlayerData.HomeLocation home = playerData.getHome(homeName);
            
            String status = home.isWorldLoaded() ? "&a✓" : "&c✗";
            plugin.getMessageManager().sendRawMessage(player,
                status + " &e" + homeName + " &7- &a" + home.getWorld() + 
                " &7(" + (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ() + ")");
        }
        
        plugin.getMessageManager().sendRawMessage(player,
            "&7Gesamt: &e" + playerData.getHomeCount() + "&7/&e" + plugin.getConfigManager().getMaxHomes());
        
        plugin.getSoundManager().playClickSound(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            if (playerData != null) {
                // Home-Namen vorschlagen
                for (String homeName : playerData.getHomes().keySet()) {
                    if (homeName.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(homeName);
                    }
                }
                
                // Spezielle Befehle
                if ("list".startsWith(args[0].toLowerCase())) {
                    completions.add("list");
                }
            }
        }
        
        return completions;
    }
}
