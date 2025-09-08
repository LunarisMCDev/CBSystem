package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlayerJoinQuitListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Lade Spielerdaten asynchron
        plugin.getPlayerDataManager().loadPlayerData(player);
        
        // Begrüßungsnachricht
        if (player.hasPlayedBefore()) {
            // Wiederkehrender Spieler
            event.setJoinMessage(plugin.getMessageManager().colorize(
                "&7[&a+&7] &e" + player.getName() + " &7ist dem Server beigetreten!"));
        } else {
            // Neuer Spieler
            event.setJoinMessage(plugin.getMessageManager().colorize(
                "&7[&a+&7] &6Willkommen &e" + player.getName() + " &6auf OP-Games.de!"));
            
            // Willkommensnachrichten für neue Spieler
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    sendWelcomeMessages(player);
                }
            }, 40L); // 2 Sekunden Verzögerung
        }
        
        // Wartungsmodus-Check
        if (plugin.getConfigManager().isMaintenanceEnabled() && 
            !player.hasPermission("cbsystem.maintenance.bypass")) {
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(plugin.getConfigManager().getMaintenanceMessage());
                }
            }, 1L);
            return;
        }
        
        // Teleportiere neue Spieler zum Spawn
        if (!player.hasPlayedBefore()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    org.bukkit.Location spawn = player.getWorld().getSpawnLocation();
                    player.teleport(spawn);
                    plugin.getSoundManager().playLevelUpSound(player);
                }
            }, 20L); // 1 Sekunde Verzögerung
        }
        
        // Update Last-Seen Zeit
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
            if (playerData != null) {
                playerData.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
            }
        }, 60L); // 3 Sekunden Verzögerung
        
        // Log
        if (plugin.getConfigManager().isLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s (%s) ist beigetreten", 
                player.getName(), player.getUniqueId()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Quit-Nachricht
        event.setQuitMessage(plugin.getMessageManager().colorize(
            "&7[&c-&7] &e" + player.getName() + " &7hat den Server verlassen!"));
        
        // Speichere Spielerdaten
        plugin.getPlayerDataManager().savePlayerData(player);
        
        // Schließe offene GUIs
        plugin.getGUIManager().closeGUI(player);
        
        // Breche laufende Teleportationen ab
        plugin.getTeleportManager().cancelTeleport(player);
        
        // Entferne TPA-Anfragen
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null) {
            // Entferne alle TPA-Anfragen von diesem Spieler
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    PlayerData otherPlayerData = plugin.getPlayerDataManager().getPlayerData(onlinePlayer);
                    if (otherPlayerData != null) {
                        otherPlayerData.removeTPARequest(player.getUniqueId());
                    }
                }
            }
        }
        
        // Log
        if (plugin.getConfigManager().isLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s (%s) hat den Server verlassen", 
                player.getName(), player.getUniqueId()));
        }
    }
    
    private void sendWelcomeMessages(Player player) {
        String[] welcomeMessages = {
            "&6&l=== Willkommen auf OP-Games.de! ===",
            "",
            "&7Herzlich willkommen auf unserem CityBuild-Server!",
            "&7Hier sind ein paar Tipps für den Einstieg:",
            "",
            "&a• &e/cb &7- Öffnet das Hauptmenü",
            "&a• &e/plot &7- Verwalte deine Plots",
            "&a• &e/shop &7- Öffnet den Server-Shop", 
            "&a• &e/spawn &7- Teleportiert zum Spawn",
            "",
            "&7Du startest mit &a" + plugin.getEconomyManager().formatBalance(
                plugin.getConfigManager().getStartingBalance()) + " &7Startguthaben!",
            "",
            "&6Viel Spaß beim Bauen und Erkunden!"
        };
        
        for (String message : welcomeMessages) {
            plugin.getMessageManager().sendRawMessage(player, message);
        }
        
        // Zeige Hauptmenü nach kurzer Verzögerung
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getGUIManager().openMainMenu(player);
                plugin.getSoundManager().playSuccessSound(player);
            }
        }, 100L); // 5 Sekunden
    }
}
