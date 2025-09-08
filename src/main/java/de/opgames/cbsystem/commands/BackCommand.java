package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public BackCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getConfigManager().isBackEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.back.use")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            plugin.getMessageManager().sendErrorMessage(player, "general.database-error");
            return true;
        }
        
        // Überprüfe Cooldown
        if (playerData.hasCooldown("back")) {
            plugin.getMessageManager().sendMessage(player, "cooldown.active",
                "time", String.valueOf(playerData.getCooldownRemaining("back")));
            return true;
        }
        
        // Teleportiere zur letzten Position
        if (plugin.getTeleportManager().teleportToLastLocation(player)) {
            playerData.setCooldown("back", plugin.getConfigManager().getBackCooldown());
            plugin.getMessageManager().sendMessage(player, "back.teleported");
        }
        
        return true;
    }
}
