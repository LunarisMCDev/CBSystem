package de.opgames.cbsystem.plot;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class PlotManager {
    
    private final CBSystem plugin;
    private final PlotSquaredIntegration plotSquaredIntegration;
    
    public PlotManager(CBSystem plugin) {
        this.plugin = plugin;
        this.plotSquaredIntegration = new PlotSquaredIntegration(plugin);
    }
    
    /**
     * Überprüft ob PlotSquared verfügbar ist
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().isPlotSquaredEnabled() && 
               plotSquaredIntegration.isPlotSquaredAvailable();
    }
    
    /**
     * Holt alle Plots eines Spielers
     */
    public List<Plot> getPlayerPlots(UUID playerUUID) {
        return plotSquaredIntegration.getPlayerPlots(playerUUID);
    }
    
    /**
     * Holt den Plot an der aktuellen Position des Spielers
     */
    public Plot getCurrentPlot(Player player) {
        return plotSquaredIntegration.getPlotAt(player.getLocation());
    }
    
    /**
     * Holt einen Plot an einer bestimmten Position
     */
    public Plot getPlotAt(Location location) {
        return plotSquaredIntegration.getPlotAt(location);
    }
    
    /**
     * Überprüft ob ein Spieler der Besitzer eines Plots ist
     */
    public boolean isPlotOwner(Plot plot, UUID playerUUID) {
        return plotSquaredIntegration.isPlotOwner(plot, playerUUID);
    }
    
    /**
     * Überprüft ob ein Spieler zu einem Plot hinzugefügt ist
     */
    public boolean isPlotMember(Plot plot, UUID playerUUID) {
        return plotSquaredIntegration.isPlotMember(plot, playerUUID);
    }
    
    /**
     * Überprüft ob ein Spieler Berechtigungen auf einem Plot hat
     */
    public boolean hasPlotPermission(Plot plot, UUID playerUUID) {
        if (plot == null) return false;
        return isPlotOwner(plot, playerUUID) || isPlotMember(plot, playerUUID);
    }
    
    /**
     * Fügt einen Spieler zu einem Plot hinzu
     */
    public boolean addPlayerToPlot(Plot plot, UUID playerUUID) {
        return plotSquaredIntegration.addPlayerToPlot(plot, playerUUID);
    }
    
    /**
     * Entfernt einen Spieler von einem Plot
     */
    public boolean removePlayerFromPlot(Plot plot, UUID playerUUID) {
        return plotSquaredIntegration.removePlayerFromPlot(plot, playerUUID);
    }
    
    /**
     * Teleportiert einen Spieler zu seinem Plot-Home
     */
    public boolean teleportToPlotHome(Player player, Plot plot) {
        return plotSquaredIntegration.teleportToPlotHome(player, plot);
    }
    
    /**
     * Versucht einen Plot für einen Spieler zu kaufen
     */
    public boolean purchasePlot(Player player, Plot plot) {
        return plotSquaredIntegration.purchasePlot(player, plot);
    }
    
    /**
     * Berechnet den Preis für einen Plot
     */
    public double calculatePlotPrice(UUID playerUUID) {
        return plotSquaredIntegration.calculatePlotPrice(playerUUID);
    }
    
    /**
     * Überprüft ob ein Spieler einen Plot kaufen kann
     */
    public boolean canPlayerClaimPlot(UUID playerUUID) {
        return plotSquaredIntegration.canPlayerClaimPlot(playerUUID);
    }
    
    /**
     * Holt Plot-Daten aus der Datenbank
     */
    public PlotSquaredIntegration.PlotData getPlotData(Plot plot) {
        return plotSquaredIntegration.getPlotData(plot);
    }
    
    /**
     * Öffnet das Plot-Management-GUI für einen Spieler
     */
    public void openPlotManagementGUI(Player player, Plot plot) {
        if (!hasPlotPermission(plot, player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "plot.no-permission");
            return;
        }
        
        plugin.getGUIManager().openPlotManagementGUI(player, plot);
    }
    
    /**
     * Öffnet das Plot-Info-GUI für einen Spieler
     */
    public void openPlotInfoGUI(Player player, Plot plot) {
        plugin.getGUIManager().openPlotInfoGUI(player, plot);
    }
    
    /**
     * Holt die Anzahl der Plots eines Spielers
     */
    public int getPlotCount(UUID playerUUID) {
        return getPlayerPlots(playerUUID).size();
    }
    
    /**
     * Holt das maximale Anzahl an Plots für einen Spieler basierend auf seinem Rang
     */
    public int getMaxPlots(UUID playerUUID) {
        // Hier würde normalerweise das Permissions-System abgefragt werden
        String rank = "default"; // Placeholder
        return plugin.getConfigManager().getMaxPlotsForRank(rank);
    }
    
    // Methoden für PlaceholderAPI
    /**
     * Holt die Anzahl der Plots eines Spielers (für Placeholders)
     */
    public int getPlayerPlotCount(UUID playerUUID) {
        return getPlotCount(playerUUID);
    }
    
    /**
     * Holt die maximale Anzahl an Plots für einen Spieler (für Placeholders)
     */
    public int getMaxPlotsForPlayer(Player player) {
        return getMaxPlots(player.getUniqueId());
    }
    
    /**
     * Überprüft ob ein Spieler in einem Plot steht (für Placeholders)
     */
    public boolean isInPlot(Player player) {
        return getCurrentPlot(player) != null;
    }
    
    /**
     * Holt die aktuelle Plot-ID (für Placeholders)
     */
    public String getCurrentPlotId(Player player) {
        Plot plot = getCurrentPlot(player);
        if (plot != null) {
            return plot.getId().toString();
        }
        return "N/A";
    }
    
    /**
     * Holt den Besitzer des aktuellen Plots (für Placeholders)
     */
    public String getCurrentPlotOwner(Player player) {
        Plot plot = getCurrentPlot(player);
        if (plot != null && plot.getOwners().size() > 0) {
            UUID ownerUUID = plot.getOwners().iterator().next();
            return plugin.getServer().getOfflinePlayer(ownerUUID).getName();
        }
        return "N/A";
    }
}
