package de.opgames.cbsystem.bank;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import de.opgames.cbsystem.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class BankManager {
    
    private final CBSystem plugin;
    private final DatabaseManager databaseManager;
    
    // Bank-Konfiguration
    private static final double INTEREST_RATE = 0.02; // 2% Zinsen pro Tag
    private static final double MAX_LOAN_AMOUNT = 100000.0; // Maximaler Kreditbetrag
    private static final double LOAN_INTEREST_RATE = 0.05; // 5% Kreditzinsen pro Tag
    private static final int MAX_LOAN_DAYS = 7; // Maximal 7 Tage Kreditlaufzeit
    
    public BankManager(CBSystem plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        
        // Bank-Tabellen erstellen
        createBankTables();
        
        // Zinsen-Task alle 24 Stunden
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::processInterest, 0L, 1728000L); // 24 Stunden
    }
    
    private void createBankTables() {
        try (Connection connection = databaseManager.getConnection()) {
            // Bank-Konten Tabelle
            String createBankAccounts = """
                CREATE TABLE IF NOT EXISTS bank_accounts (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    balance DECIMAL(15,2) DEFAULT 0.00,
                    interest_rate DECIMAL(5,4) DEFAULT 0.0200,
                    last_interest TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            
            // Kredite Tabelle
            String createLoans = """
                CREATE TABLE IF NOT EXISTS bank_loans (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    interest_rate DECIMAL(5,4) NOT NULL,
                    remaining_amount DECIMAL(15,2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    due_date TIMESTAMP NOT NULL,
                    status ENUM('ACTIVE', 'PAID', 'DEFAULTED') DEFAULT 'ACTIVE',
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
            """;
            
            // Transaktionen Tabelle
            String createTransactions = """
                CREATE TABLE IF NOT EXISTS bank_transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    type ENUM('DEPOSIT', 'WITHDRAW', 'INTEREST', 'LOAN_GRANTED', 'LOAN_PAYMENT') NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    balance_after DECIMAL(15,2) NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
            """;
            
            try (PreparedStatement stmt1 = connection.prepareStatement(createBankAccounts);
                 PreparedStatement stmt2 = connection.prepareStatement(createLoans);
                 PreparedStatement stmt3 = connection.prepareStatement(createTransactions)) {
                
                stmt1.executeUpdate();
                stmt2.executeUpdate();
                stmt3.executeUpdate();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Erstellen der Bank-Tabellen:", e);
        }
    }
    
    /**
     * Erstellt ein Bankkonto für einen Spieler
     */
    public boolean createBankAccount(UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "INSERT IGNORE INTO bank_accounts (player_uuid, balance, interest_rate) VALUES (?, 0.00, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setDouble(2, INTEREST_RATE);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Erstellen des Bankkontos:", e);
            return false;
        }
    }
    
    /**
     * Holt das Bankguthaben eines Spielers
     */
    public double getBankBalance(UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT balance FROM bank_accounts WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getDouble("balance") : 0.0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Abrufen des Bankguthabens:", e);
            return 0.0;
        }
    }
    
    /**
     * Einzahlung auf das Bankkonto
     */
    public boolean deposit(UUID playerUUID, double amount) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null) return false;
        
        double currentBalance = playerData.getBalance();
        if (currentBalance < amount) return false;
        
        // Geld vom Spieler nehmen
        if (!plugin.getEconomyManager().removeBalance(playerUUID, amount, "Bank-Einzahlung")) {
            return false;
        }
        
        // Geld auf Bankkonto einzahlen
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Bankkonto erstellen falls nicht vorhanden
                createBankAccount(playerUUID);
                
                // Einzahlung durchführen
                String sql = "UPDATE bank_accounts SET balance = balance + ? WHERE player_uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setDouble(1, amount);
                    stmt.setString(2, playerUUID.toString());
                    stmt.executeUpdate();
                }
                
                // Transaktion loggen
                logTransaction(playerUUID, "DEPOSIT", amount, getBankBalance(playerUUID), "Einzahlung");
                
                connection.commit();
                return true;
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler bei der Bank-Einzahlung:", e);
            // Geld zurückgeben
            plugin.getEconomyManager().addBalance(playerUUID, amount, "Bank-Einzahlung Fehler");
            return false;
        }
    }
    
    /**
     * Auszahlung vom Bankkonto
     */
    public boolean withdraw(UUID playerUUID, double amount) {
        double bankBalance = getBankBalance(playerUUID);
        if (bankBalance < amount) return false;
        
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Auszahlung durchführen
                String sql = "UPDATE bank_accounts SET balance = balance - ? WHERE player_uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setDouble(1, amount);
                    stmt.setString(2, playerUUID.toString());
                    stmt.executeUpdate();
                }
                
                // Geld an Spieler geben
                if (!plugin.getEconomyManager().addBalance(playerUUID, amount, "Bank-Auszahlung")) {
                    throw new SQLException("Fehler beim Hinzufügen des Geldes");
                }
                
                // Transaktion loggen
                logTransaction(playerUUID, "WITHDRAW", amount, getBankBalance(playerUUID), "Auszahlung");
                
                connection.commit();
                return true;
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler bei der Bank-Auszahlung:", e);
            return false;
        }
    }
    
    /**
     * Kredit gewähren
     */
    public boolean grantLoan(UUID playerUUID, double amount, int days) {
        if (amount > MAX_LOAN_AMOUNT || days > MAX_LOAN_DAYS) return false;
        
        // Prüfen ob Spieler bereits einen aktiven Kredit hat
        if (hasActiveLoan(playerUUID)) return false;
        
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Kredit in Datenbank eintragen
                String sql = "INSERT INTO bank_loans (player_uuid, amount, interest_rate, remaining_amount, due_date) VALUES (?, ?, ?, ?, DATE_ADD(NOW(), INTERVAL ? DAY))";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.setDouble(2, amount);
                    stmt.setDouble(3, LOAN_INTEREST_RATE);
                    stmt.setDouble(4, amount);
                    stmt.setInt(5, days);
                    stmt.executeUpdate();
                }
                
                // Geld an Spieler geben
                if (!plugin.getEconomyManager().addBalance(playerUUID, amount, "Bank-Kredit")) {
                    throw new SQLException("Fehler beim Hinzufügen des Kredits");
                }
                
                // Transaktion loggen
                logTransaction(playerUUID, "LOAN_GRANTED", amount, plugin.getEconomyManager().getBalance(playerUUID), "Kredit gewährt");
                
                connection.commit();
                return true;
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Gewähren des Kredits:", e);
            return false;
        }
    }
    
    /**
     * Kredit zurückzahlen
     */
    public boolean payLoan(UUID playerUUID, double amount) {
        if (!hasActiveLoan(playerUUID)) return false;
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null) return false;
        
        double currentBalance = playerData.getBalance();
        if (currentBalance < amount) return false;
        
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            
            try {
                // Aktiven Kredit holen
                String getLoanSql = "SELECT id, remaining_amount FROM bank_loans WHERE player_uuid = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
                double remainingAmount;
                int loanId;
                
                try (PreparedStatement stmt = connection.prepareStatement(getLoanSql)) {
                    stmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Kein aktiver Kredit gefunden");
                        }
                        loanId = rs.getInt("id");
                        remainingAmount = rs.getDouble("remaining_amount");
                    }
                }
                
                double paymentAmount = Math.min(amount, remainingAmount);
                
                // Kredit reduzieren
                String updateLoanSql = "UPDATE bank_loans SET remaining_amount = remaining_amount - ? WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(updateLoanSql)) {
                    stmt.setDouble(1, paymentAmount);
                    stmt.setInt(2, loanId);
                    stmt.executeUpdate();
                }
                
                // Kredit als bezahlt markieren falls vollständig zurückgezahlt
                if (remainingAmount - paymentAmount <= 0) {
                    String markPaidSql = "UPDATE bank_loans SET status = 'PAID' WHERE id = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(markPaidSql)) {
                        stmt.setInt(1, loanId);
                        stmt.executeUpdate();
                    }
                }
                
                // Geld vom Spieler nehmen
                if (!plugin.getEconomyManager().removeBalance(playerUUID, paymentAmount, "Kredit-Rückzahlung")) {
                    throw new SQLException("Fehler beim Abziehen des Geldes");
                }
                
                // Transaktion loggen
                logTransaction(playerUUID, "LOAN_PAYMENT", paymentAmount, plugin.getEconomyManager().getBalance(playerUUID), "Kredit-Rückzahlung");
                
                connection.commit();
                return true;
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler bei der Kredit-Rückzahlung:", e);
            return false;
        }
    }
    
    /**
     * Prüft ob Spieler einen aktiven Kredit hat
     */
    public boolean hasActiveLoan(UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM bank_loans WHERE player_uuid = ? AND status = 'ACTIVE'";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Prüfen des Kredits:", e);
            return false;
        }
    }
    
    /**
     * Holt Kredit-Informationen
     */
    public LoanInfo getLoanInfo(UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT amount, remaining_amount, interest_rate, due_date FROM bank_loans WHERE player_uuid = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new LoanInfo(
                            rs.getDouble("amount"),
                            rs.getDouble("remaining_amount"),
                            rs.getDouble("interest_rate"),
                            rs.getTimestamp("due_date").toLocalDateTime()
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Abrufen der Kredit-Informationen:", e);
        }
        return null;
    }
    
    /**
     * Verarbeitet Zinsen für alle Bankkonten
     */
    private void processInterest() {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "UPDATE bank_accounts SET balance = balance + (balance * interest_rate), last_interest = NOW() WHERE balance > 0";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int affectedRows = stmt.executeUpdate();
                plugin.getLogger().info("Zinsen für " + affectedRows + " Bankkonten verarbeitet.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Verarbeiten der Zinsen:", e);
        }
    }
    
    /**
     * Loggt eine Bank-Transaktion
     */
    private void logTransaction(UUID playerUUID, String type, double amount, double balanceAfter, String description) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "INSERT INTO bank_transactions (player_uuid, type, amount, balance_after, description) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, type);
                stmt.setDouble(3, amount);
                stmt.setDouble(4, balanceAfter);
                stmt.setString(5, description);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Loggen der Bank-Transaktion:", e);
        }
    }
    
    /**
     * Kredit-Informationen Klasse
     */
    public static class LoanInfo {
        private final double originalAmount;
        private final double remainingAmount;
        private final double interestRate;
        private final java.time.LocalDateTime dueDate;
        
        public LoanInfo(double originalAmount, double remainingAmount, double interestRate, java.time.LocalDateTime dueDate) {
            this.originalAmount = originalAmount;
            this.remainingAmount = remainingAmount;
            this.interestRate = interestRate;
            this.dueDate = dueDate;
        }
        
        public double getOriginalAmount() { return originalAmount; }
        public double getRemainingAmount() { return remainingAmount; }
        public double getInterestRate() { return interestRate; }
        public java.time.LocalDateTime getDueDate() { return dueDate; }
    }
}
