package de.opgames.cbsystem.teleport;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {
    
    private final CBSystem plugin;
    private final Map<UUID, TeleportRequest> pendingTeleports = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
    
    public TeleportManager(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Startet eine Teleportation mit Verzögerung
     */
    public void startTeleport(Player player, Location destination, String reason) {
        startTeleport(player, destination, reason, getTeleportDelay(player));
    }
    
    /**
     * Startet eine Teleportation mit benutzerdefinierter Verzögerung
     */
    public void startTeleport(Player player, Location destination, String reason, int delay) {
        UUID playerUUID = player.getUniqueId();
        
        // Abbrechen wenn bereits eine Teleportation läuft
        cancelTeleport(player);
        
        if (delay <= 0) {
            // Sofortige Teleportation
            executeTeleport(player, destination, reason);
            return;
        }
        
        // Erstelle Teleportationsanfrage
        TeleportRequest request = new TeleportRequest(player.getLocation(), destination, reason);
        pendingTeleports.put(playerUUID, request);
        
        // Sound abspielen
        plugin.getSoundManager().playTeleportStartSound(player);
        
        // Nachricht senden
        plugin.getMessageManager().sendMessage(player, "teleport.starting", 
            "delay", String.valueOf(delay));
        
        // Teleportations-Task starten
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = delay;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                TeleportRequest currentRequest = pendingTeleports.get(playerUUID);
                if (currentRequest == null) {
                    cancel();
                    return;
                }
                
                // Überprüfe ob sich der Spieler bewegt hat
                if (hasPlayerMoved(player.getLocation(), currentRequest.getStartLocation())) {
                    cancelTeleport(player, "teleport.cancelled-moved");
                    cancel();
                    return;
                }
                
                timeLeft--;
                
                if (timeLeft <= 0) {
                    // Teleportation ausführen
                    executeTeleport(player, currentRequest.getDestination(), currentRequest.getReason());
                    pendingTeleports.remove(playerUUID);
                    teleportTasks.remove(playerUUID);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Jede Sekunde
        
        teleportTasks.put(playerUUID, task);
    }
    
    /**
     * Führt die Teleportation aus
     */
    private void executeTeleport(Player player, Location destination, String reason) {
        // Sichere die aktuelle Position für /back
        addBackLocation(player, reason);
        
        // Teleportiere den Spieler
        player.teleport(destination);
        
        // Sound und Nachricht
        plugin.getSoundManager().playTeleportSuccessSound(player);
        plugin.getMessageManager().sendMessage(player, "teleport.success");
        
        // Logging
        if (plugin.getConfigManager().isTeleportLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s teleportiert nach %s (Grund: %s)",
                player.getName(), locationToString(destination), reason));
        }
    }
    
    /**
     * Bricht eine laufende Teleportation ab
     */
    public void cancelTeleport(Player player) {
        cancelTeleport(player, null);
    }
    
    /**
     * Bricht eine laufende Teleportation mit Nachricht ab
     */
    public void cancelTeleport(Player player, String messageKey) {
        UUID playerUUID = player.getUniqueId();
        
        // Task abbrechen
        BukkitTask task = teleportTasks.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }
        
        // Request entfernen
        pendingTeleports.remove(playerUUID);
        
        if (messageKey != null) {
            plugin.getSoundManager().playTeleportCancelledSound(player);
            plugin.getMessageManager().sendMessage(player, messageKey);
        }
    }
    
    /**
     * Überprüft ob ein Spieler eine laufende Teleportation hat
     */
    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }
    
    /**
     * Fügt eine Back-Location hinzu
     */
    public void addBackLocation(Player player, String reason) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null) {
            playerData.addBackLocation(player.getLocation(), reason);
        }
    }
    
    /**
     * Teleportiert einen Spieler zur letzten Back-Location
     */
    public boolean teleportToLastLocation(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) return false;
        
        PlayerData.BackLocation backLocation = playerData.getLastBackLocation();
        if (backLocation == null) {
            plugin.getMessageManager().sendMessage(player, "back.no-location");
            return false;
        }
        
        Location location = backLocation.toBukkitLocation();
        if (location == null || !backLocation.isWorldLoaded()) {
            plugin.getMessageManager().sendMessage(player, "error.world-not-found", 
                "world", backLocation.getWorld());
            return false;
        }
        
        // Überprüfe ob die Position sicher ist
        if (!isSafeLocation(location)) {
            plugin.getMessageManager().sendMessage(player, "back.location-unsafe");
            return false;
        }
        
        startTeleport(player, location, "Back-Befehl");
        return true;
    }
    
    /**
     * Holt die Teleportationsverzögerung für einen Spieler
     */
    private int getTeleportDelay(Player player) {
        // Admin haben keine Verzögerung
        if (player.hasPermission("cbsystem.teleport.nodelay")) {
            return 0;
        }
        
        return 3; // Standard 3 Sekunden
    }
    
    /**
     * Überprüft ob sich ein Spieler bewegt hat
     */
    private boolean hasPlayerMoved(Location current, Location start) {
        if (!current.getWorld().equals(start.getWorld())) return true;
        
        double distance = current.distance(start);
        return distance > 0.5; // Toleranz von 0.5 Blöcken
    }
    
    /**
     * Überprüft ob eine Position sicher ist
     */
    public boolean isSafeLocation(Location location) {
        if (location.getWorld() == null) return false;
        
        // Überprüfe ob der Spieler in Lava oder im Void spawnen würde
        if (location.getY() < 0 || location.getY() > 256) return false;
        
        // Überprüfe die Blöcke an der Position
        Location feet = location.clone();
        Location head = location.clone().add(0, 1, 0);
        Location ground = location.clone().subtract(0, 1, 0);
        
        // Spieler darf nicht in Blöcken spawnen
        if (feet.getBlock().getType().isSolid() || head.getBlock().getType().isSolid()) {
            return false;
        }
        
        // Boden sollte solide sein (außer bei Flugmodus)
        return ground.getBlock().getType().isSolid();
    }
    
    /**
     * Konvertiert eine Location zu einem String
     */
    private String locationToString(Location location) {
        return String.format("%s:%.1f,%.1f,%.1f", 
            location.getWorld().getName(), 
            location.getX(), 
            location.getY(), 
            location.getZ());
    }
    
    /**
     * Räumt abgelaufene Teleportationen auf
     */
    public void cleanup() {
        pendingTeleports.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
        
        teleportTasks.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Teleportationsanfrage-Klasse
     */
    private static class TeleportRequest {
        private final Location startLocation;
        private final Location destination;
        private final String reason;
        private final long timestamp;
        
        public TeleportRequest(Location startLocation, Location destination, String reason) {
            this.startLocation = startLocation.clone();
            this.destination = destination.clone();
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Location getStartLocation() { return startLocation; }
        public Location getDestination() { return destination; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
    }
}
