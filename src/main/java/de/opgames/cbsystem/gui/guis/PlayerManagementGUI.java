package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerManagementGUI extends BaseGUI {
    
    private final List<Player> onlinePlayers;
    private int currentPage = 0;
    
    public PlayerManagementGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&c&lSpielerverwaltung", 54);
        this.onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        this.onlinePlayers.remove(player); // Admin nicht in der Liste anzeigen
    }
    
    @Override
    protected void setupGUI() {
        // Header: Server-Info
        setItem(4, Material.PLAYER_HEAD, "&6&lServer-Übersicht",
            List.of(
                "&7Online-Spieler: &e" + Bukkit.getOnlinePlayers().size(),
                "&7Maximale Spieler: &e" + Bukkit.getMaxPlayers(),
                "&7Server-TPS: &a" + getServerTPS(),
                "",
                "&7Wähle einen Spieler zur Verwaltung"
            ));
        
        // Online-Spieler anzeigen (Slots 9-44)
        int startIndex = currentPage * 36;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + 36, onlinePlayers.size()); i++) {
            if (slot > 44) break;
            
            Player target = onlinePlayers.get(i);
            if (target != null && target.isOnline()) {
                setupPlayerItem(slot, target);
                slot++;
            }
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / 36);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Aktions-Buttons
        setItem(47, Material.COMPASS, "&e&lOffline-Spieler",
            List.of(
                "&7Verwalte offline Spieler",
                "&7durch Namenseingabe",
                "",
                "&eLinksklick: &7Spieler suchen"
            ));
        
        setItem(48, Material.BOOK, "&a&lServer-Statistiken",
            List.of(
                "&7Detaillierte Server-Infos",
                "&7und Spieler-Statistiken",
                "",
                "&aLinksklick: &7Statistiken anzeigen"
            ));
        
        setItem(49, createBackButton());
        setItem(50, createCloseButton());
        
        setItem(51, Material.BARRIER, "&c&lNotfall-Aktionen",
            List.of(
                "&7Schnelle Notfall-Befehle",
                "&cKick alle, Stop Server, etc.",
                "",
                "&cLinksklick: &7Notfall-Menü"
            ));
        
        fillEmptySlots();
    }
    
    private void setupPlayerItem(int slot, Player target) {
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        String world = target.getWorld().getName();
        
        List<String> lore = new ArrayList<>();
        lore.add("&7UUID: &f" + target.getUniqueId().toString().substring(0, 8) + "...");
        lore.add("&7Welt: &e" + world);
        lore.add("&7Guthaben: &a" + plugin.getEconomyManager().formatBalance(balance));
        lore.add("&7Gamemode: &b" + target.getGameMode().name());
        lore.add("&7Health: &c" + Math.round(target.getHealth()) + "/20");
        lore.add("&7Level: &d" + target.getLevel());
        lore.add("");
        lore.add("&aLinksklick: &7Spieler-Aktionen");
        lore.add("&cRechtsklick: &7Zu Spieler teleportieren");
        
        // Spielerkopf verwenden
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        setItem(slot, playerHead.getType(), "&e" + target.getName(), lore);
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        if (!player.hasPermission("cbsystem.admin.playermanagement")) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return;
        }
        
        playClickSound();
        
        switch (slot) {
            case 45: // Vorherige Seite
                if (currentPage > 0) {
                    currentPage--;
                    update();
                }
                break;
                
            case 47: // Offline-Spieler
                handleOfflinePlayerSearch();
                break;
                
            case 48: // Server-Statistiken
                plugin.getGUIManager().openServerStatsGUI(player);
                break;
                
            case 49: // Zurück
                plugin.getGUIManager().openAdminGUI(player);
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 51: // Notfall-Aktionen
                plugin.getGUIManager().openEmergencyActionsGUI(player);
                break;
                
            case 53: // Nächste Seite
                int totalPages = (int) Math.ceil((double) onlinePlayers.size() / 36);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    update();
                }
                break;
                
            default:
                // Spieler-Interaktion
                if (slot >= 9 && slot <= 44) {
                    handlePlayerClick(slot, isRightClick);
                }
                break;
        }
    }
    
    private void handlePlayerClick(int slot, boolean isRightClick) {
        int playerIndex = (currentPage * 36) + (slot - 9);
        
        if (playerIndex >= onlinePlayers.size()) return;
        
        Player target = onlinePlayers.get(playerIndex);
        if (target == null || !target.isOnline()) {
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", "Unbekannt");
            update(); // GUI aktualisieren
            return;
        }
        
        if (isRightClick) {
            // Zu Spieler teleportieren
            player.teleport(target.getLocation());
            plugin.getMessageManager().sendMessage(player, "admin.teleported-to-player",
                "player", target.getName());
            playSuccessSound();
            closeGUI();
        } else {
            // Spieler-Aktionen-Menü öffnen
            plugin.getGUIManager().openPlayerActionsGUI(player, target);
        }
    }
    
    private void handleOfflinePlayerSearch() {
        closeGUI();
        plugin.getMessageManager().sendMessage(player, "admin.enter-player-name");
        
        // TODO: Chat-Input-System implementieren
        plugin.getMessageManager().sendMessage(player, "admin.offline-player-search-coming-soon");
    }
    
    private String getServerTPS() {
        try {
            // Vereinfachte TPS-Berechnung
            return "20.0"; // Placeholder - echte TPS würden über Reflection geholt
        } catch (Exception e) {
            return "N/A";
        }
    }
}
