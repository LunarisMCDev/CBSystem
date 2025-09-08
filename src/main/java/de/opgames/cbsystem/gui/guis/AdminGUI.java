package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AdminGUI extends BaseGUI {
    
    public AdminGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&4&lAdmin-Menü", 54);
    }
    
    @Override
    protected void setupGUI() {
        // Server-Info
        setItem(4, Material.COMMAND_BLOCK, "&4&lServer-Verwaltung",
            List.of(
                "&7Online-Spieler: &e" + plugin.getServer().getOnlinePlayers().size(),
                "&7TPS: &a" + getTPS(),
                "&7Speicher: &e" + getMemoryUsage(),
                "",
                "&7Admin-Tools für OP-Games.de"
            ));
        
        // Spieler-Verwaltung
        setItem(10, Material.PLAYER_HEAD, "&a&lSpieler-Verwaltung",
            List.of(
                "&7Verwalte Online-Spieler,",
                "&7Berechtigungen und mehr.",
                "",
                "&aLinksklick: &7Spieler-Tools öffnen"
            ));
        
        // Economy-Verwaltung
        setItem(12, Material.GOLD_INGOT, "&6&lEconomy-Verwaltung",
            List.of(
                "&7Verwalte Spieler-Guthaben",
                "&7und Economy-Einstellungen.",
                "",
                "&6Linksklick: &7Economy-Tools öffnen"
            ));
        
        // Plot-Verwaltung
        setItem(14, Material.GRASS_BLOCK, "&2&lPlot-Verwaltung",
            List.of(
                "&7Verwalte Plots, lösche",
                "&7inaktive Plots und mehr.",
                "",
                "&2Linksklick: &7Plot-Tools öffnen"
            ));
        
        // Server-Einstellungen
        setItem(16, Material.REDSTONE, "&c&lServer-Einstellungen",
            List.of(
                "&7Ändere Server-Konfiguration",
                "&7und Plugin-Einstellungen.",
                "",
                "&cLinksklick: &7Einstellungen öffnen"
            ));
        
        // Wartungsmodus
        boolean maintenanceMode = plugin.getConfigManager().isMaintenanceEnabled();
        setItem(28, maintenanceMode ? Material.RED_CONCRETE : Material.GREEN_CONCRETE, 
            maintenanceMode ? "&c&lWartungsmodus AN" : "&a&lWartungsmodus AUS",
            List.of(
                "&7Aktiviere oder deaktiviere",
                "&7den Wartungsmodus.",
                "",
                maintenanceMode ? "&cLinksklick: &7Deaktivieren" : "&aLinksklick: &7Aktivieren"
            ));
        
        // Plugin neu laden
        setItem(30, Material.BOOK, "&b&lPlugin neu laden",
            List.of(
                "&7Lade die Plugin-Konfiguration",
                "&7und Nachrichten neu.",
                "",
                "&bLinksklick: &7Neu laden"
            ));
        
        // Backup erstellen
        setItem(32, Material.CHEST, "&e&lBackup erstellen",
            List.of(
                "&7Erstelle ein Backup der",
                "&7wichtigsten Server-Daten.",
                "",
                "&eLinksklick: &7Backup starten"
            ));
        
        // Server-Statistiken
        setItem(34, Material.WRITABLE_BOOK, "&d&lServer-Statistiken",
            List.of(
                "&7Zeige detaillierte",
                "&7Server-Statistiken an.",
                "",
                "&dLinksklick: &7Statistiken anzeigen"
            ));
        
        // Logs anzeigen
        setItem(37, Material.PAPER, "&7&lLogs anzeigen",
            List.of(
                "&7Zeige die neuesten",
                "&7Server-Logs an.",
                "",
                "&7Linksklick: &7Logs öffnen"
            ));
        
        // Broadcast senden
        setItem(39, Material.BELL, "&f&lBroadcast senden",
            List.of(
                "&7Sende eine Nachricht",
                "&7an alle Spieler.",
                "",
                "&fLinksklick: &7Broadcast senden"
            ));
        
        // Welt-Verwaltung
        setItem(41, Material.GRASS_BLOCK, "&2&lWelt-Verwaltung",
            List.of(
                "&7Verwalte Welten, lade",
                "&7Chunks und mehr.",
                "",
                "&2Linksklick: &7Welt-Tools öffnen"
            ));
        
        // Teleportation-Tools
        setItem(43, Material.ENDER_PEARL, "&5&lTeleportation-Tools",
            List.of(
                "&7Admin-Teleportation,",
                "&7Vanish und mehr.",
                "",
                "&5Linksklick: &7Teleport-Tools öffnen"
            ));
        
        // Zurück und Schließen
        setItem(45, createBackButton());
        setItem(53, createCloseButton());
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        if (!player.hasPermission("cbsystem.admin.*")) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return;
        }
        
        playClickSound();
        
        switch (slot) {
            case 10: // Spieler-Verwaltung
                plugin.getGUIManager().openPlayerManagementGUI(player);
                break;
                
            case 12: // Economy-Verwaltung
                plugin.getMessageManager().sendMessage(player, "admin.economy-management-coming-soon");
                break;
                
            case 14: // Plot-Verwaltung
                plugin.getMessageManager().sendMessage(player, "admin.plot-management-coming-soon");
                break;
                
            case 16: // Server-Einstellungen
                plugin.getMessageManager().sendMessage(player, "admin.server-settings-coming-soon");
                break;
                
            case 28: // Wartungsmodus
                toggleMaintenanceMode();
                break;
                
            case 30: // Plugin neu laden
                reloadPlugin();
                break;
                
            case 32: // Backup erstellen
                plugin.getMessageManager().sendMessage(player, "admin.backup-coming-soon");
                break;
                
            case 34: // Server-Statistiken
                showServerStats();
                break;
                
            case 37: // Logs anzeigen
                plugin.getMessageManager().sendMessage(player, "admin.logs-coming-soon");
                break;
                
            case 39: // Broadcast senden
                plugin.getMessageManager().sendMessage(player, "admin.broadcast-instruction");
                closeGUI();
                break;
                
            case 41: // Welt-Verwaltung
                plugin.getMessageManager().sendMessage(player, "admin.world-management-coming-soon");
                break;
                
            case 43: // Teleportation-Tools
                plugin.getMessageManager().sendMessage(player, "admin.teleport-tools-coming-soon");
                break;
                
            case 45: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 53: // Schließen
                closeGUI();
                break;
        }
    }
    
    private void toggleMaintenanceMode() {
        boolean currentMode = plugin.getConfigManager().isMaintenanceEnabled();
        plugin.getConfigManager().set("admin.maintenance.enabled", !currentMode);
        
        playSuccessSound();
        if (!currentMode) {
            plugin.getMessageManager().sendMessage(player, "admin.maintenance-enabled");
        } else {
            plugin.getMessageManager().sendMessage(player, "admin.maintenance-disabled");
        }
        
        update(); // GUI aktualisieren
    }
    
    private void reloadPlugin() {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getMessageManager().reloadMessages();
            
            playSuccessSound();
            plugin.getMessageManager().sendMessage(player, "general.plugin-reloaded");
            
            plugin.getLogger().info("Plugin wurde von " + player.getName() + " neu geladen.");
        } catch (Exception e) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "admin.reload-failed");
            plugin.getLogger().severe("Fehler beim Neu-Laden des Plugins: " + e.getMessage());
        }
    }
    
    private void showServerStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        plugin.getMessageManager().sendMessage(player, "admin.server-stats-header");
        plugin.getMessageManager().sendRawMessage(player, "&7Online-Spieler: &e" + plugin.getServer().getOnlinePlayers().size());
        plugin.getMessageManager().sendRawMessage(player, "&7Welten: &e" + plugin.getServer().getWorlds().size());
        plugin.getMessageManager().sendRawMessage(player, "&7Speicher: &e" + (usedMemory / 1024 / 1024) + "MB / " + (totalMemory / 1024 / 1024) + "MB");
        plugin.getMessageManager().sendRawMessage(player, "&7Java Version: &e" + System.getProperty("java.version"));
        plugin.getMessageManager().sendRawMessage(player, "&7Server Version: &e" + plugin.getServer().getVersion());
    }
    
    private String getTPS() {
        // Vereinfachte TPS-Berechnung (normalerweise würde man das über eine TPS-Messung machen)
        return "20.0"; // Placeholder
    }
    
    private String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (usedMemory / 1024 / 1024) + "MB / " + (totalMemory / 1024 / 1024) + "MB";
    }
}
