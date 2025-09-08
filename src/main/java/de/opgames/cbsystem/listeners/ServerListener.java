package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class ServerListener implements Listener {
    
    private final CBSystem plugin;
    
    public ServerListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            plugin.getLogger().info("Server vollständig geladen - CBSystem bereit!");
            
            // Führe Startup-Tasks aus
            performStartupTasks();
        } else if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            plugin.getLogger().info("Server neu geladen - CBSystem aktualisiert!");
            
            // Führe Reload-Tasks aus
            performReloadTasks();
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();
        
        // Reagiere auf wichtige Plugin-Aktivierungen
        switch (pluginName) {
            case "PlotSquared" -> {
                plugin.getLogger().info("PlotSquared erkannt - Integration aktiviert!");
                // PlotSquared-Integration neu initialisieren falls nötig
            }
            case "Vault" -> {
                plugin.getLogger().info("Vault erkannt - Economy-Integration verfügbar!");
                // Vault-Hook einrichten
            }
            case "PlaceholderAPI" -> {
                plugin.getLogger().info("PlaceholderAPI erkannt - Platzhalter verfügbar!");
                // PlaceholderAPI-Expansion registrieren
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        String pluginName = event.getPlugin().getName();
        
        // Reagiere auf wichtige Plugin-Deaktivierungen
        switch (pluginName) {
            case "PlotSquared" -> {
                plugin.getLogger().warning("PlotSquared wurde deaktiviert - Plot-Features nicht verfügbar!");
            }
            case "Vault" -> {
                plugin.getLogger().warning("Vault wurde deaktiviert - Economy-Integration eingeschränkt!");
            }
            case "PlaceholderAPI" -> {
                plugin.getLogger().info("PlaceholderAPI wurde deaktiviert - Platzhalter nicht verfügbar!");
            }
        }
    }
    
    private void performStartupTasks() {
        // Cleanup-Tasks
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Räume verwaiste Daten auf
            plugin.getPlayerDataManager().cleanupCache();
            plugin.getGUIManager().cleanupGUIs();
            plugin.getTeleportManager().cleanup();
            
            plugin.getLogger().info("Startup-Cleanup abgeschlossen!");
        }, 100L); // 5 Sekunden nach Server-Start
        
        // Auto-Save Task starten
        if (plugin.getConfigManager().isAutoSaveEnabled()) {
            long interval = plugin.getConfigManager().getAutoSaveInterval() * 20L; // Sekunden zu Ticks
            
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                plugin.getPlayerDataManager().saveAllPlayerData();
                plugin.getLogger().info("Auto-Save durchgeführt!");
            }, interval, interval);
            
            plugin.getLogger().info("Auto-Save aktiviert (Interval: " + plugin.getConfigManager().getAutoSaveInterval() + "s)");
        }
        
        // Regelmäßige Cleanup-Tasks
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getPlayerDataManager().cleanupCache();
            plugin.getGUIManager().cleanupGUIs();
            plugin.getTeleportManager().cleanup();
        }, 6000L, 6000L); // Alle 5 Minuten
        
        // Server-Statistiken (falls Debug aktiviert)
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                
                plugin.getLogger().info(String.format("Stats - Spieler: %d, Speicher: %dMB, GUIs: %d, Cache: %d",
                    plugin.getServer().getOnlinePlayers().size(),
                    usedMemory,
                    plugin.getGUIManager().getOpenGUICount(),
                    plugin.getPlayerDataManager().getCachedPlayerCount()));
            }, 12000L, 12000L); // Alle 10 Minuten
        }
    }
    
    private void performReloadTasks() {
        // Lade Konfiguration neu
        plugin.getConfigManager().reloadConfig();
        plugin.getMessageManager().reloadMessages();
        
        // Benachrichtige Online-Admins
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("cbsystem.admin.notifications"))
            .forEach(player -> {
                plugin.getMessageManager().sendMessage(player, "admin.server-reloaded");
                plugin.getSoundManager().playClickSound(player);
            });
        
        plugin.getLogger().info("Reload-Tasks abgeschlossen!");
    }
}
