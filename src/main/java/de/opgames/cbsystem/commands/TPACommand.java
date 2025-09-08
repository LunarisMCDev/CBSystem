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

public class TPACommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public TPACommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getConfigManager().isTPAEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.tpa.use")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "tpa.usage");
            return true;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", args[0]);
            return true;
        }
        
        if (targetPlayer.equals(player)) {
            plugin.getMessageManager().sendMessage(player, "tpa.cannot-request-self");
            return true;
        }
        
        // Überprüfe Cooldown
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null && playerData.hasCooldown("tpa")) {
            plugin.getMessageManager().sendMessage(player, "cooldown.active",
                "time", String.valueOf(playerData.getCooldownRemaining("tpa")));
            return true;
        }
        
        // Sende TPA-Anfrage
        PlayerData targetData = plugin.getPlayerDataManager().getPlayerData(targetPlayer);
        if (targetData != null) {
            targetData.addTPARequest(player.getUniqueId(), PlayerData.TPARequest.Type.TPA);
            
            // Setze Cooldown
            if (playerData != null) {
                playerData.setCooldown("tpa", 30); // 30 Sekunden Cooldown
            }
            
            plugin.getMessageManager().sendSuccessMessage(player, "tpa.request-sent", 
                "player", targetPlayer.getName());
            
            plugin.getMessageManager().sendMessage(targetPlayer, "tpa.request-received",
                "player", player.getName());
            plugin.getMessageManager().sendMessage(targetPlayer, "tpa.request-info");
            
            plugin.getSoundManager().playClickSound(targetPlayer);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            // Online-Spieler vorschlagen (außer sich selbst)
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (!onlinePlayer.equals(player) && 
                    onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}
