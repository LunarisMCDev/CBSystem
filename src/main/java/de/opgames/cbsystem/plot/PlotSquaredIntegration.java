package de.opgames.cbsystem.plot;

import com.plotsquared.bukkit.player.BukkitPlayer;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.PlotAreaManager;
import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.database.DatabaseManager;
import de.opgames.cbsystem.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlotSquaredIntegration {
    
    private final CBSystem plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    
    public PlotSquaredIntegration(CBSystem plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
    }
    
    /**
     * Überprüft ob PlotSquared verfügbar ist
     */
    public boolean isPlotSquaredAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlotSquared") != null;
    }
    
    /**
     * Holt alle Plots eines Spielers
     */
    public List<Plot> getPlayerPlots(UUID playerUUID) {
        if (!isPlotSquaredAvailable()) {
            return new ArrayList<>();
        }
        
        try {
            PlotAreaManager plotAreaManager = PlotSquared.get().getPlotAreaManager();
            List<Plot> plots = new ArrayList<>();
            
            for (PlotArea plotArea : plotAreaManager.getAllPlotAreas()) {
                plots.addAll(plotArea.getPlotsAbs(playerUUID));
            }
            
            return plots;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Abrufen der Plots für Spieler " + playerUUID, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Holt einen Plot an einer bestimmten Position
     */
    public Plot getPlotAt(org.bukkit.Location location) {
        if (!isPlotSquaredAvailable()) {
            return null;
        }
        
                 try {
             com.plotsquared.core.location.Location plotLocation = 
                 com.plotsquared.core.location.Location.at(
                     location.getWorld().getName(),
                     location.getBlockX(),
                     location.getBlockY(),
                     location.getBlockZ()
                 );
             
             return plotLocation.getPlot();
         } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Abrufen des Plots an Position " + location, e);
            return null;
        }
    }
    
    /**
     * Überprüft ob ein Spieler der Besitzer eines Plots ist
     */
    public boolean isPlotOwner(Plot plot, UUID playerUUID) {
        if (plot == null) return false;
        return plot.isOwner(playerUUID);
    }
    
    /**
     * Überprüft ob ein Spieler zu einem Plot hinzugefügt ist
     */
    public boolean isPlotMember(Plot plot, UUID playerUUID) {
        if (plot == null) return false;
        return plot.isAdded(playerUUID) || plot.isOwner(playerUUID);
    }
    
    /**
     * Fügt einen Spieler zu einem Plot hinzu
     */
    public boolean addPlayerToPlot(Plot plot, UUID playerUUID) {
        if (plot == null) return false;
        
        try {
            plot.addTrusted(playerUUID);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Hinzufügen des Spielers " + playerUUID + " zu Plot " + plot.getId(), e);
            return false;
        }
    }
    
    /**
     * Entfernt einen Spieler von einem Plot
     */
    public boolean removePlayerFromPlot(Plot plot, UUID playerUUID) {
        if (plot == null) return false;
        
        try {
            plot.removeTrusted(playerUUID);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Entfernen des Spielers " + playerUUID + " von Plot " + plot.getId(), e);
            return false;
        }
    }
    
    /**
     * Teleportiert einen Spieler zu seinem Plot-Home
     */
    public boolean teleportToPlotHome(Player player, Plot plot) {
        if (plot == null) return false;
        
        try {
            // PlotSquared getHome() mit Consumer-Parameter
            plot.getHome(plotLocation -> {
                if (plotLocation != null) {
                    // Konvertiere PlotSquared Location zu Bukkit Location
                    org.bukkit.World world = plugin.getServer().getWorld(plotLocation.getWorldName());
                    if (world != null) {
                        org.bukkit.Location bukkitLocation = new org.bukkit.Location(
                            world, 
                            plotLocation.getX(), 
                            plotLocation.getY(), 
                            plotLocation.getZ(),
                            plotLocation.getYaw(),
                            plotLocation.getPitch()
                        );
                        
                        plugin.getTeleportManager().startTeleport(player, bukkitLocation, "Plot-Home");
                        plugin.getSoundManager().playTeleportSuccessSound(player);
                        plugin.getMessageManager().sendMessage(player, "plot.teleported-home");
                    } else {
                        plugin.getSoundManager().playErrorSound(player);
                        plugin.getMessageManager().sendMessage(player, "plot.teleport-failed");
                    }
                } else {
                    plugin.getSoundManager().playErrorSound(player);
                    plugin.getMessageManager().sendMessage(player, "plot.teleport-failed");
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Teleportieren zu Plot " + plot.getId(), e);
            return false;
        }
    }
    
    /**
     * Holt Plot-Informationen für die Datenbank
     */
    public PlotData getPlotData(Plot plot) {
        if (plot == null) return null;
        
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT * FROM cb_plot_data WHERE plot_id = ? AND world = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, plot.getId().toString());
                statement.setString(2, plot.getWorldName());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new PlotData(
                            resultSet.getString("plot_id"),
                            resultSet.getString("world"),
                            resultSet.getString("owner_uuid") != null ? 
                                UUID.fromString(resultSet.getString("owner_uuid")) : null,
                            resultSet.getDouble("purchase_price"),
                            resultSet.getTimestamp("purchase_date"),
                            resultSet.getString("additional_data")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Abrufen der Plot-Daten", e);
        }
        
        return null;
    }
    
    /**
     * Speichert Plot-Kauf-Informationen
     */
    public void savePlotPurchase(Plot plot, UUID buyerUUID, double price) {
        databaseManager.executeAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = """
                    INSERT INTO cb_plot_data (plot_id, world, owner_uuid, purchase_price, purchase_date)
                    VALUES (?, ?, ?, ?, NOW())
                    ON DUPLICATE KEY UPDATE 
                        owner_uuid = VALUES(owner_uuid),
                        purchase_price = VALUES(purchase_price),
                        purchase_date = VALUES(purchase_date)
                """;
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, plot.getId().toString());
                    statement.setString(2, plot.getWorldName());
                    statement.setString(3, buyerUUID.toString());
                    statement.setDouble(4, price);
                    
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern des Plot-Kaufs", e);
            }
        });
    }
    
    /**
     * Berechnet den Preis für einen Plot basierend auf der Anzahl der bereits besessenen Plots
     */
    public double calculatePlotPrice(UUID playerUUID) {
        double basePrice = plugin.getConfigManager().getPlotClaimPrice();
        double multiplier = plugin.getConfigManager().getAdditionalPlotMultiplier();
        int ownedPlots = getPlayerPlots(playerUUID).size();
        
        if (ownedPlots == 0) {
            return basePrice;
        }
        
        return basePrice * Math.pow(multiplier, ownedPlots);
    }
    
    /**
     * Überprüft ob ein Spieler einen Plot kaufen kann
     */
    public boolean canPlayerClaimPlot(UUID playerUUID) {
        String rank = getRankForPlayer(playerUUID);
        int maxPlots = plugin.getConfigManager().getMaxPlotsForRank(rank);
        int currentPlots = getPlayerPlots(playerUUID).size();
        
        return currentPlots < maxPlots;
    }
    
    /**
     * Versucht einen Plot für einen Spieler zu kaufen
     */
    public boolean purchasePlot(Player player, Plot plot) {
        if (plot == null || plot.hasOwner()) {
            plugin.getMessageManager().sendMessage(player, "plot.already-claimed");
            return false;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        if (!canPlayerClaimPlot(playerUUID)) {
            plugin.getMessageManager().sendMessage(player, "plot.max-plots-reached");
            return false;
        }
        
        double price = calculatePlotPrice(playerUUID);
        
        if (!economyManager.hasBalance(playerUUID, price)) {
            plugin.getMessageManager().sendMessage(player, "plot.insufficient-funds", 
                "amount", String.format("%.2f", price));
            return false;
        }
        
        try {
            // Geld abziehen
            economyManager.withdrawBalance(playerUUID, price, "Plot-Kauf: " + plot.getId());
            
            // Plot claimen - vereinfachte Variante
            plot.setOwner(playerUUID);
            
            // Kauf in Datenbank speichern
            savePlotPurchase(plot, playerUUID, price);
            
            // Erfolgsnachricht
            plugin.getMessageManager().sendMessage(player, "plot.purchased", 
                "price", String.format("%.2f", price));
            plugin.getSoundManager().playSuccessSound(player);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Plot-Kauf", e);
            plugin.getMessageManager().sendMessage(player, "plot.purchase-error");
            return false;
        }
    }
    
    private String getRankForPlayer(UUID playerUUID) {
        // Hier würde normalerweise das Permissions-System abgefragt werden
        // Für jetzt geben wir "default" zurück
        return "default";
    }
    
    /**
     * Plot-Daten-Klasse
     */
    public static class PlotData {
        private final String plotId;
        private final String world;
        private final UUID ownerUUID;
        private final double purchasePrice;
        private final java.sql.Timestamp purchaseDate;
        private final String additionalData;
        
        public PlotData(String plotId, String world, UUID ownerUUID, double purchasePrice, 
                       java.sql.Timestamp purchaseDate, String additionalData) {
            this.plotId = plotId;
            this.world = world;
            this.ownerUUID = ownerUUID;
            this.purchasePrice = purchasePrice;
            this.purchaseDate = purchaseDate;
            this.additionalData = additionalData;
        }
        
        // Getter-Methoden
        public String getPlotId() { return plotId; }
        public String getWorld() { return world; }
        public UUID getOwnerUUID() { return ownerUUID; }
        public double getPurchasePrice() { return purchasePrice; }
        public java.sql.Timestamp getPurchaseDate() { return purchaseDate; }
        public String getAdditionalData() { return additionalData; }
    }
}
