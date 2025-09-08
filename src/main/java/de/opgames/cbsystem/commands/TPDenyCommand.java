package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPDenyCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public TPDenyCommand(CBSystem plugin) {
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
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            plugin.getMessageManager().sendErrorMessage(player, "general.database-error");
            return true;
        }
        
        // Räume abgelaufene Anfragen auf
        long timeout = plugin.getConfigManager().getTPARequestTimeout() * 1000L;
        playerData.clearExpiredTPARequests(timeout);
        
        if (playerData.getTpaRequests().isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "tpa.no-requests");
            return true;
        }
        
        // Nimm die neueste Anfrage
        PlayerData.TPARequest request = playerData.getTpaRequests().get(0);
        Player requester = plugin.getServer().getPlayer(request.getRequester());
        
        // Entferne die Anfrage
        playerData.removeTPARequest(request.getRequester());
        
        // Bestätigungs-Nachrichten
        plugin.getMessageManager().sendMessage(player, "tpa.denied");
        
        if (requester != null && requester.isOnline()) {
            plugin.getMessageManager().sendMessage(requester, "tpa.request-denied", 
                "player", player.getName());
        }
        
        plugin.getSoundManager().playErrorSound(player);
        
        return true;
    }
}
