package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public SpawnCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        Location spawnLocation = getSpawnLocation();
        
        if (spawnLocation == null) {
            plugin.getMessageManager().sendMessage(player, "spawn.not-set");
            return true;
        }
        
        plugin.getTeleportManager().startTeleport(player, spawnLocation, "Spawn");
        plugin.getMessageManager().sendMessage(player, "spawn.teleported");
        
        return true;
    }
    
    private Location getSpawnLocation() {
        if (!plugin.getConfigManager().isSpawnEnabled()) {
            return null;
        }
        
        // Lade Spawn-Location aus der Config
        String worldName = plugin.getConfigManager().getSpawnWorld();
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("Spawn-Welt '" + worldName + "' nicht gefunden!");
            return null;
        }
        
        return new Location(
            world,
            plugin.getConfigManager().getSpawnX(),
            plugin.getConfigManager().getSpawnY(),
            plugin.getConfigManager().getSpawnZ(),
            plugin.getConfigManager().getSpawnYaw(),
            plugin.getConfigManager().getSpawnPitch()
        );
    }
}
