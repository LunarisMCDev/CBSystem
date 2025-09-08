package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WarpCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public WarpCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getConfigManager().isWarpsEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.warp.use")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            // Warp-GUI öffnen
            plugin.getGUIManager().openWarpGUI(player);
            return true;
        }
        
        String warpName = args[0].toLowerCase();
        
        // Spezielle Befehle
        if (warpName.equals("list") || warpName.equals("liste")) {
            showWarpList(player);
            return true;
        }
        
        // Zu Warp teleportieren
        teleportToWarp(player, warpName);
        return true;
    }
    
    private void teleportToWarp(Player player, String warpName) {
        // Hier würde normalerweise die Warp-Location aus der Datenbank geladen werden
        org.bukkit.Location warpLocation = getWarpLocation(warpName);
        
        if (warpLocation == null) {
            plugin.getMessageManager().sendMessage(player, "warp.not-found", "name", warpName);
            return;
        }
        
        plugin.getTeleportManager().startTeleport(player, warpLocation, "Warp: " + warpName);
        plugin.getMessageManager().sendMessage(player, "warp.teleporting", "name", warpName);
    }
    
    private org.bukkit.Location getWarpLocation(String warpName) {
        // Standard-Warp-Locations (würden normalerweise aus der Datenbank kommen)
        org.bukkit.World world = plugin.getServer().getWorld("world");
        if (world == null) return null;
        
        return switch (warpName) {
            case "spawn" -> new org.bukkit.Location(world, 0.5, 100, 0.5);
            case "shop" -> new org.bukkit.Location(world, 50.5, 100, 50.5);
            default -> null;
        };
    }
    
    private void showWarpList(Player player) {
        plugin.getMessageManager().sendMessage(player, "warp.list-header");
        
        List<String> warps = Arrays.asList("spawn", "shop");
        
        if (warps.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "warp.list-empty");
            return;
        }
        
        for (String warp : warps) {
            plugin.getMessageManager().sendRawMessage(player, "&e" + warp + " &7- Verfügbar");
        }
        
        plugin.getSoundManager().playClickSound(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> warps = Arrays.asList("spawn", "shop", "list");
            
            for (String warp : warps) {
                if (warp.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(warp);
                }
            }
        }
        
        return completions;
    }
}
