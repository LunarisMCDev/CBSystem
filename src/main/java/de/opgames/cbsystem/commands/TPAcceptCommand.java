package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPAcceptCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public TPAcceptCommand(CBSystem plugin) {
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
        
        if (requester == null || !requester.isOnline()) {
            playerData.removeTPARequest(request.getRequester());
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", "Antragsteller");
            return true;
        }
        
        // Entferne die Anfrage
        playerData.removeTPARequest(request.getRequester());
        
        // Führe Teleportation durch
        org.bukkit.Location destination;
        String teleportReason;
        
        if (request.getType() == PlayerData.TPARequest.Type.TPA) {
            // Requester kommt zum Player
            destination = player.getLocation();
            teleportReason = "TPA zu " + player.getName();
        } else {
            // Player geht zum Requester
            destination = requester.getLocation();
            teleportReason = "TPAHERE von " + requester.getName();
        }
        
        // Bestätigungs-Nachrichten
        plugin.getMessageManager().sendSuccessMessage(player, "tpa.accepted");
        plugin.getMessageManager().sendMessage(requester, "tpa.request-accepted", 
            "player", player.getName());
        
        // Teleportation starten
        Player teleportTarget = (request.getType() == PlayerData.TPARequest.Type.TPA) ? requester : player;
        plugin.getTeleportManager().startTeleport(teleportTarget, destination, teleportReason);
        
        return true;
    }
}
