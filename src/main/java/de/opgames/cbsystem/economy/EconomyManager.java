package de.opgames.cbsystem.economy;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import de.opgames.cbsystem.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class EconomyManager {
    
    private final CBSystem plugin;
    private final DatabaseManager databaseManager;
    
    public EconomyManager(CBSystem plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }
    
    /**
     * Holt das Guthaben eines Spielers
     */
    public double getBalance(UUID playerUUID) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        return playerData != null ? playerData.getBalance() : 0.0;
    }
    
    /**
     * Überprüft ob ein Spieler genug Geld hat
     */
    public boolean hasBalance(UUID playerUUID, double amount) {
        return getBalance(playerUUID) >= amount;
    }
    
    /**
     * Setzt das Guthaben eines Spielers
     */
    public boolean setBalance(UUID playerUUID, double amount, String reason) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null) return false;
        
        double oldBalance = playerData.getBalance();
        playerData.setBalance(Math.max(0, Math.min(amount, plugin.getConfigManager().getMaxBalance())));
        
        // Transaktion in Datenbank speichern
        logTransaction(null, playerUUID, amount - oldBalance, TransactionType.ADMIN_SET, reason);
        
        return true;
    }
    
    /**
     * Fügt Geld zum Guthaben hinzu
     */
    public boolean addBalance(UUID playerUUID, double amount, String reason) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null) return false;
        
        double newBalance = Math.min(playerData.getBalance() + amount, plugin.getConfigManager().getMaxBalance());
        playerData.setBalance(newBalance);
        
        // Transaktion in Datenbank speichern
        logTransaction(null, playerUUID, amount, TransactionType.ADMIN_ADD, reason);
        
        return true;
    }
    
    /**
     * Zieht Geld vom Guthaben ab
     */
    public boolean withdrawBalance(UUID playerUUID, double amount, String reason) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null || !hasBalance(playerUUID, amount)) return false;
        
        playerData.subtractBalance(amount);
        
        // Transaktion in Datenbank speichern
        logTransaction(playerUUID, null, amount, TransactionType.ADMIN_REMOVE, reason);
        
        return true;
    }
    
    /**
     * Überweist Geld von einem Spieler zu einem anderen
     */
    public boolean transferMoney(UUID fromUUID, UUID toUUID, double amount, String reason) {
        if (!hasBalance(fromUUID, amount)) return false;
        
        PlayerData fromData = plugin.getPlayerDataManager().getPlayerData(fromUUID);
        PlayerData toData = plugin.getPlayerDataManager().getPlayerData(toUUID);
        
        if (fromData == null || toData == null) return false;
        
        // Berechne eventuelle Steuern
        double tax = amount * (plugin.getConfigManager().getPayTaxPercentage() / 100.0);
        double actualAmount = amount - tax;
        
        // Führe die Transaktion durch
        fromData.subtractBalance(amount);
        toData.addBalance(actualAmount);
        
        // Transaktion in Datenbank speichern
        logTransaction(fromUUID, toUUID, actualAmount, TransactionType.TRANSFER, reason);
        
        if (tax > 0) {
            logTransaction(fromUUID, null, tax, TransactionType.TRANSFER, "Steuer für Überweisung");
        }
        
        return true;
    }
    
    /**
     * Formatiert einen Geldbetrag als String
     */
    public String formatBalance(double amount) {
        return String.format("%.2f%s", amount, plugin.getConfigManager().getCurrencySymbol());
    }
    
    /**
     * Holt den Namen der Währung
     */
    public String getCurrencyName(double amount) {
        return amount == 1.0 ? 
            plugin.getConfigManager().getCurrencyName() : 
            plugin.getConfigManager().getCurrencyNamePlural();
    }
    
    /**
     * Loggt eine Transaktion in die Datenbank
     */
    private void logTransaction(UUID fromUUID, UUID toUUID, double amount, TransactionType type, String reason) {
        if (!plugin.getConfigManager().isMoneyTransactionLoggingEnabled()) return;
        
        databaseManager.executeAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = """
                    INSERT INTO cb_transactions (from_uuid, to_uuid, amount, type, reason)
                    VALUES (?, ?, ?, ?, ?)
                """;
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, fromUUID != null ? fromUUID.toString() : null);
                    statement.setString(2, toUUID != null ? toUUID.toString() : null);
                    statement.setDouble(3, amount);
                    statement.setString(4, type.name());
                    statement.setString(5, reason);
                    
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler beim Loggen der Transaktion", e);
            }
        });
    }
    
    /**
     * Überprüft ob der Economy-Service aktiviert ist
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().isEconomyEnabled();
    }
    
    /**
     * Holt das Startguthaben für neue Spieler
     */
    public double getStartingBalance() {
        return plugin.getConfigManager().getStartingBalance();
    }
    
    /**
     * Überprüft ob ein Betrag gültig ist
     */
    public boolean isValidAmount(double amount) {
        return amount > 0 && amount <= plugin.getConfigManager().getMaxBalance();
    }
    
    /**
     * Überprüft ob ein Pay-Betrag gültig ist
     */
    public boolean isValidPayAmount(double amount) {
        return amount >= plugin.getConfigManager().getMinPayAmount() && 
               amount <= plugin.getConfigManager().getMaxPayAmount();
    }
    
    /**
     * Enum für Transaktionstypen
     */
    public enum TransactionType {
        TRANSFER,
        ADMIN_SET,
        ADMIN_ADD,
        ADMIN_REMOVE,
        SHOP_BUY,
        SHOP_SELL,
        PLOT_BUY
    }
}
