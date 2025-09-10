package de.opgames.cbsystem.gui;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.guis.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {
    
    private final CBSystem plugin;
    private final Map<UUID, BaseGUI> openGUIs;
    
    public GUIManager(CBSystem plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    /**
     * Öffnet das Hauptmenü für einen Spieler
     */
    public void openMainMenu(Player player) {
        MainMenuGUI gui = new MainMenuGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Plot-Management-GUI
     */
    public void openPlotManagementGUI(Player player, Plot plot) {
        PlotManagementGUI gui = new PlotManagementGUI(plugin, player, plot);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Plot-Info-GUI
     */
    public void openPlotInfoGUI(Player player, Plot plot) {
        PlotInfoGUI gui = new PlotInfoGUI(plugin, player, plot);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Shop-GUI
     */
    public void openShopGUI(Player player) {
        ShopGUI gui = new ShopGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet eine Shop-Kategorie
     */
    public void openShopCategoryGUI(Player player, String category) {
        ShopCategoryGUI gui = new ShopCategoryGUI(plugin, player, category);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Home-Management-GUI
     */
    public void openHomeManagementGUI(Player player) {
        HomeManagementGUI gui = new HomeManagementGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Warp-GUI
     */
    public void openWarpGUI(Player player) {
        WarpGUI gui = new WarpGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Sell-All-GUI
     */
    public void openSellAllGUI(Player player) {
        SellAllGUI gui = new SellAllGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Auktionshaus-GUI
     */
    public void openAuctionHouse(Player player) {
        AuctionHouseGUI gui = new AuctionHouseGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Meine-Auktionen-GUI
     */
    public void openMyAuctionsGUI(Player player) {
        MyAuctionsGUI gui = new MyAuctionsGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Auktion-Erstellen-GUI
     */
    public void openCreateAuctionGUI(Player player) {
        CreateAuctionGUI gui = new CreateAuctionGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Bank-GUI
     */
    public void openBankGUI(Player player) {
        BankGUI gui = new BankGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Spielerverwaltungs-GUI
     */
    public void openPlayerManagementGUI(Player player) {
        PlayerManagementGUI gui = new PlayerManagementGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Spieler-Aktionen-GUI
     */
    public void openPlayerActionsGUI(Player player, Player targetPlayer) {
        PlayerActionsGUI gui = new PlayerActionsGUI(plugin, player, targetPlayer);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Server-Statistiken-GUI
     */
    public void openServerStatsGUI(Player player) {
        // TODO: ServerStatsGUI implementieren
        plugin.getMessageManager().sendMessage(player, "admin.server-stats-coming-soon");
    }
    
    /**
     * Öffnet das Notfall-Aktionen-GUI
     */
    public void openEmergencyActionsGUI(Player player) {
        // TODO: EmergencyActionsGUI implementieren
        plugin.getMessageManager().sendMessage(player, "admin.emergency-actions-coming-soon");
    }
    
    /**
     * Öffnet das Admin-GUI
     */
    public void openAdminGUI(Player player) {
        if (!player.hasPermission("cbsystem.admin.*")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return;
        }
        
        AdminGUI gui = new AdminGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Economy-GUI
     */
    public void openEconomyGUI(Player player) {
        EconomyGUI gui = new EconomyGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet das Teleportations-GUI
     */
    public void openTeleportGUI(Player player) {
        TeleportGUI gui = new TeleportGUI(plugin, player);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet ein Bestätigungs-GUI
     */
    public void openConfirmationGUI(Player player, String title, String confirmText, 
                                   Runnable onConfirm, Runnable onCancel) {
        ConfirmationGUI gui = new ConfirmationGUI(plugin, player, title, confirmText, onConfirm, onCancel);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet ein Spieler-Auswahl-GUI
     */
    public void openPlayerSelectionGUI(Player player, String title, 
                                      java.util.function.Consumer<Player> onPlayerSelected) {
        PlayerSelectionGUI gui = new PlayerSelectionGUI(plugin, player, title, onPlayerSelected);
        openGUI(player, gui);
    }
    
    /**
     * Öffnet ein Input-GUI für Text-Eingabe
     */
    public void openTextInputGUI(Player player, String title, String placeholder,
                                java.util.function.Consumer<String> onInputReceived) {
        // Für Text-Input würden wir normalerweise ein Chat-basiertes System verwenden
        // oder ein Anvil-GUI. Hier ist ein einfaches Beispiel:
        player.closeInventory();
        plugin.getMessageManager().sendMessage(player, "gui.text-input.instruction", 
            "placeholder", placeholder);
        
        // TODO: Implementiere Chat-Listener für Text-Input
    }
    
    /**
     * Öffnet ein GUI für einen Spieler
     */
    private void openGUI(Player player, BaseGUI gui) {
        // Schließe vorheriges GUI falls vorhanden
        closeGUI(player);
        
        // Registriere das neue GUI
        openGUIs.put(player.getUniqueId(), gui);
        
        // Öffne das Inventar
        player.openInventory(gui.getInventory());
        
        // Spiele Sound ab
        plugin.getSoundManager().playOpenSound(player);
        
        // Debug-Nachricht
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("GUI '" + gui.getClass().getSimpleName() + 
                                  "' für Spieler " + player.getName() + " geöffnet");
        }
    }
    
    /**
     * Schließt das GUI eines Spielers
     */
    public void closeGUI(Player player) {
        BaseGUI gui = openGUIs.remove(player.getUniqueId());
        if (gui != null) {
            gui.onClose();
            plugin.getSoundManager().playCloseSound(player);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("GUI '" + gui.getClass().getSimpleName() + 
                                      "' für Spieler " + player.getName() + " geschlossen");
            }
        }
    }
    
    /**
     * Holt das aktuell geöffnete GUI eines Spielers
     */
    public BaseGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    /**
     * Überprüft ob ein Spieler ein GUI geöffnet hat
     */
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
    
    /**
     * Aktualisiert das GUI eines Spielers
     */
    public void updateGUI(Player player) {
        BaseGUI gui = getOpenGUI(player);
        if (gui != null) {
            gui.update();
        }
    }
    
    /**
     * Schließt alle GUIs (wird beim Plugin-Disable aufgerufen)
     */
    public void closeAllGUIs() {
        for (UUID playerUUID : openGUIs.keySet()) {
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }
    
    /**
     * Holt die Anzahl der geöffneten GUIs
     */
    public int getOpenGUICount() {
        return openGUIs.size();
    }
    
    /**
     * Räumt verwaiste GUI-Referenzen auf
     */
    public void cleanupGUIs() {
        openGUIs.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
    }
}
