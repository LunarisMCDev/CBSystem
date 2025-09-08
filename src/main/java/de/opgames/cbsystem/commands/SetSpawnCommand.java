package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public SetSpawnCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.admin.setspawn")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        // Setze Spawn für die aktuelle Welt
        player.getWorld().setSpawnLocation(player.getLocation());
        
        // Speichere Spawn-Location in der Config
        plugin.getConfigManager().setSpawnLocation(
            player.getLocation().getWorld().getName(),
            player.getLocation().getX(),
            player.getLocation().getY(),
            player.getLocation().getZ(),
            player.getLocation().getYaw(),
            player.getLocation().getPitch()
        );
        
        plugin.getMessageManager().sendMessage(player, "spawn.set");
        plugin.getLogger().info("Spawn wurde von " + player.getName() + " gesetzt: " + 
            player.getLocation().getWorld().getName() + " (" + 
            Math.round(player.getLocation().getX()) + ", " +
            Math.round(player.getLocation().getY()) + ", " +
            Math.round(player.getLocation().getZ()) + ")");
        
        return true;
    }
}
