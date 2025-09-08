package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlayerTeleportListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Speichere vorherige Position für /back (nur bei bestimmten Teleportations-Gründen)
        if (shouldSaveBackLocation(event.getCause())) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            if (playerData != null) {
                String reason = getTeleportReason(event.getCause());
                playerData.addBackLocation(event.getFrom(), reason);
            }
        }
        
        // Breche laufende Teleportationen ab wenn der Spieler sich anderweitig teleportiert
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            plugin.getTeleportManager().cancelTeleport(player);
        }
        
        // Log für bestimmte Teleportations-Typen
        if (plugin.getConfigManager().isTeleportLoggingEnabled() && shouldLogTeleport(event.getCause())) {
            plugin.getLogger().info(String.format("Spieler %s teleportiert von %s nach %s (Grund: %s)",
                player.getName(),
                locationToString(event.getFrom()),
                locationToString(event.getTo()),
                event.getCause().name()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleportDamageCancel(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Breche Teleportation ab wenn Spieler Schaden nimmt (für verzögerte Teleportation)
        if (plugin.getTeleportManager().hasPendingTeleport(player)) {
            // Überprüfe ob der Spieler kürzlich Schaden genommen hat
            if (player.getLastDamageCause() != null && 
                (System.currentTimeMillis() - player.getLastDamageCause().getEntity().getTicksLived() * 50) < 2000) {
                
                plugin.getTeleportManager().cancelTeleport(player, "teleport.cancelled-damaged");
                event.setCancelled(true);
            }
        }
    }
    
    private boolean shouldSaveBackLocation(PlayerTeleportEvent.TeleportCause cause) {
        return switch (cause) {
            case COMMAND, PLUGIN -> true; // Befehle und Plugin-Teleportationen
            case ENDER_PEARL, CHORUS_FRUIT -> true; // Items
            case END_PORTAL, NETHER_PORTAL -> true; // Portale
            default -> false;
        };
    }
    
    private boolean shouldLogTeleport(PlayerTeleportEvent.TeleportCause cause) {
        return switch (cause) {
            case COMMAND, PLUGIN -> true;
            case END_PORTAL, NETHER_PORTAL -> true;
            default -> false;
        };
    }
    
    private String getTeleportReason(PlayerTeleportEvent.TeleportCause cause) {
        return switch (cause) {
            case COMMAND -> "Befehl";
            case PLUGIN -> "Plugin";
            case ENDER_PEARL -> "Enderperle";
            case CHORUS_FRUIT -> "Chorusfrucht";
            case END_PORTAL -> "End-Portal";
            case NETHER_PORTAL -> "Nether-Portal";
            case SPECTATE -> "Spectate";
            default -> cause.name();
        };
    }
    
    private String locationToString(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "Unbekannt";
        }
        
        return String.format("%s:%.1f,%.1f,%.1f",
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ());
    }
}
