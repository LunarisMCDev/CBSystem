package de.opgames.cbsystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.opgames.cbsystem.CBSystem;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final CBSystem plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        if (!plugin.getConfigManager().isDatabaseEnabled()) {
            plugin.getLogger().info("Datenbank ist deaktiviert. Verwende lokale Dateien.");
            return true;
        }
        
        try {
            setupHikariCP();
            createTables();
            startConnectionKeepAlive();
            
            plugin.getLogger().info("MySQL-Datenbankverbindung erfolgreich hergestellt!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Initialisieren der Datenbank:", e);
            return false;
        }
    }
    
    private void setupHikariCP() {
        HikariConfig config = new HikariConfig();
        
        // Basis-Konfiguration
        config.setJdbcUrl("jdbc:mysql://" + 
                         plugin.getConfigManager().getDatabaseHost() + ":" + 
                         plugin.getConfigManager().getDatabasePort() + "/" + 
                         plugin.getConfigManager().getDatabaseName() + 
                         "?useSSL=false&serverTimezone=UTC&autoReconnect=true");
        config.setUsername(plugin.getConfigManager().getDatabaseUsername());
        config.setPassword(plugin.getConfigManager().getDatabasePassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool-Konfiguration
        config.setMaximumPoolSize(plugin.getConfigManager().getMaximumPoolSize());
        config.setMinimumIdle(plugin.getConfigManager().getMinimumIdle());
        config.setMaxLifetime(plugin.getConfigManager().getMaximumLifetime());
        config.setConnectionTimeout(plugin.getConfigManager().getConnectionTimeout());
        config.setLeakDetectionThreshold(plugin.getConfigManager().getLeakDetectionThreshold());
        
        // Verbindungsoptimierung
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Pool-Name
        config.setPoolName("CBSystem-HikariCP");
        
        this.dataSource = new HikariDataSource(config);
    }
    
    private void createTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // Spielerdaten-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    balance DECIMAL(15,2) DEFAULT 1000.00,
                    first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    play_time BIGINT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_last_seen (last_seen)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Homes-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_homes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    name VARCHAR(32) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT DEFAULT 0,
                    pitch FLOAT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_player_home (player_uuid, name),
                    FOREIGN KEY (player_uuid) REFERENCES cb_players(uuid) ON DELETE CASCADE,
                    INDEX idx_player_uuid (player_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Warps-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_warps (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(32) UNIQUE NOT NULL,
                    display_name VARCHAR(64),
                    world VARCHAR(64) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT DEFAULT 0,
                    pitch FLOAT DEFAULT 0,
                    creator_uuid VARCHAR(36),
                    permission VARCHAR(128),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    FOREIGN KEY (creator_uuid) REFERENCES cb_players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Geld-Transaktionen-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    from_uuid VARCHAR(36),
                    to_uuid VARCHAR(36),
                    amount DECIMAL(15,2) NOT NULL,
                    type ENUM('TRANSFER', 'ADMIN_SET', 'ADMIN_ADD', 'ADMIN_REMOVE', 'SHOP_BUY', 'SHOP_SELL', 'PLOT_BUY') NOT NULL,
                    reason VARCHAR(255),
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_from_uuid (from_uuid),
                    INDEX idx_to_uuid (to_uuid),
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_type (type),
                    FOREIGN KEY (from_uuid) REFERENCES cb_players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (to_uuid) REFERENCES cb_players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // TPA-Anfragen-Tabelle (temporäre Daten)
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_tpa_requests (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    requester_uuid VARCHAR(36) NOT NULL,
                    target_uuid VARCHAR(36) NOT NULL,
                    type ENUM('TPA', 'TPAHERE') NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    INDEX idx_requester (requester_uuid),
                    INDEX idx_target (target_uuid),
                    INDEX idx_expires (expires_at),
                    FOREIGN KEY (requester_uuid) REFERENCES cb_players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (target_uuid) REFERENCES cb_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Back-Locations-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_back_locations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT DEFAULT 0,
                    pitch FLOAT DEFAULT 0,
                    reason VARCHAR(64),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_created_at (created_at),
                    FOREIGN KEY (player_uuid) REFERENCES cb_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Shop-Items-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_shop_items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    category VARCHAR(32) NOT NULL,
                    material VARCHAR(64) NOT NULL,
                    display_name VARCHAR(128),
                    lore TEXT,
                    buy_price DECIMAL(15,2),
                    sell_price DECIMAL(15,2),
                    slot_position INT,
                    enabled BOOLEAN DEFAULT TRUE,
                    permission VARCHAR(128),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_category (category),
                    INDEX idx_enabled (enabled)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // PlotSquared-Integration-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_plot_data (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    plot_id VARCHAR(64) NOT NULL,
                    world VARCHAR(64) NOT NULL,
                    owner_uuid VARCHAR(36),
                    purchase_price DECIMAL(15,2),
                    purchase_date TIMESTAMP,
                    additional_data JSON,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_plot (plot_id, world),
                    INDEX idx_owner_uuid (owner_uuid),
                    FOREIGN KEY (owner_uuid) REFERENCES cb_players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            // Auktionshaus-Tabelle
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS cb_auctions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    item_data TEXT NOT NULL,
                    price DECIMAL(15,2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    sold BOOLEAN DEFAULT FALSE,
                    buyer_uuid VARCHAR(36) NULL,
                    buyer_name VARCHAR(16) NULL,
                    sold_at TIMESTAMP NULL,
                    INDEX idx_seller_uuid (seller_uuid),
                    INDEX idx_expires_at (expires_at),
                    INDEX idx_sold (sold),
                    INDEX idx_buyer_uuid (buyer_uuid),
                    FOREIGN KEY (seller_uuid) REFERENCES cb_players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (buyer_uuid) REFERENCES cb_players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            
            plugin.getLogger().info("Datenbanktabellen erfolgreich erstellt/aktualisiert!");
        }
    }
    
    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
    
    private void startConnectionKeepAlive() {
        // Halte die Datenbankverbindung alle 5 Minuten aktiv
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    statement.executeQuery();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.WARNING, "Fehler beim Keep-Alive der Datenbankverbindung:", e);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // 5 Minuten = 6000 Ticks
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Datenbank ist nicht initialisiert!");
        }
        return dataSource.getConnection();
    }
    
    public boolean isDatabaseEnabled() {
        return dataSource != null;
    }
    
    public void closeConnections() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Datenbankverbindungen geschlossen.");
        }
    }
    
    // Utility-Methode für asynchrone Datenbankoperationen
    public void executeAsync(Runnable operation) {
        new BukkitRunnable() {
            @Override
            public void run() {
                operation.run();
            }
        }.runTaskAsynchronously(plugin);
    }
}
