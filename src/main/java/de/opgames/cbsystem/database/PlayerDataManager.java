package de.opgames.cbsystem.database;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {
    
    private final CBSystem plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    
    public PlayerDataManager(CBSystem plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }
    
    /**
     * Lädt Spielerdaten beim Join
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        databaseManager.executeAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                // Überprüfe ob Spieler existiert
                String checkSql = "SELECT * FROM cb_players WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
                    statement.setString(1, uuid.toString());
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        PlayerData playerData;
                        
                        if (resultSet.next()) {
                            // Spieler existiert - lade Daten
                            playerData = new PlayerData(
                                uuid,
                                name,
                                resultSet.getDouble("balance"),
                                resultSet.getTimestamp("first_join"),
                                resultSet.getTimestamp("last_seen"),
                                resultSet.getLong("play_time")
                            );
                            
                            // Update Name falls geändert
                            if (!name.equals(resultSet.getString("name"))) {
                                updatePlayerName(uuid, name);
                            }
                        } else {
                            // Neuer Spieler - erstelle Eintrag
                            double startingBalance = plugin.getConfigManager().getStartingBalance();
                            playerData = new PlayerData(uuid, name, startingBalance, null, null, 0L);
                            createPlayerData(playerData);
                        }
                        
                        // Cache die Daten
                        playerDataCache.put(uuid, playerData);
                        
                        // Lade zusätzliche Daten
                        loadPlayerHomes(playerData);
                        loadPlayerBackLocations(playerData);
                        
                        plugin.getLogger().info("Spielerdaten für " + name + " geladen. Balance: " + playerData.getBalance());
                        
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden der Spielerdaten für " + name, e);
            }
        });
    }
    
    /**
     * Lädt Spielerdaten synchron (für Commands)
     */
    public boolean loadPlayerDataSync(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        try (Connection connection = databaseManager.getConnection()) {
            // Überprüfe ob Spieler existiert
            String checkSql = "SELECT * FROM cb_players WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    PlayerData playerData;
                    
                    if (resultSet.next()) {
                        // Spieler existiert - lade Daten
                        playerData = new PlayerData(
                            uuid,
                            name,
                            resultSet.getDouble("balance"),
                            resultSet.getTimestamp("first_join"),
                            resultSet.getTimestamp("last_seen"),
                            resultSet.getLong("play_time")
                        );
                        
                        // Update Name falls geändert
                        if (!name.equals(resultSet.getString("name"))) {
                            updatePlayerName(uuid, name);
                        }
                    } else {
                        // Neuer Spieler - erstelle Eintrag
                        double startingBalance = plugin.getConfigManager().getStartingBalance();
                        playerData = new PlayerData(uuid, name, startingBalance, null, null, 0L);
                        createPlayerData(playerData);
                    }
                    
                    // Cache die Daten
                    playerDataCache.put(uuid, playerData);
                    
                    // Lade zusätzliche Daten
                    loadPlayerHomes(playerData);
                    loadPlayerBackLocations(playerData);
                    
                    plugin.getLogger().info("Spielerdaten für " + name + " synchron geladen. Balance: " + playerData.getBalance());
                    return true;
                    
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim synchronen Laden der Spielerdaten für " + name, e);
            return false;
        }
    }
    
    /**
     * Erstellt einen neuen Spielereintrag
     */
    private void createPlayerData(PlayerData playerData) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = """
                INSERT INTO cb_players (uuid, name, balance, first_join, last_seen, play_time)
                VALUES (?, ?, ?, NOW(), NOW(), 0)
            """;
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerData.getUuid().toString());
                statement.setString(2, playerData.getName());
                statement.setDouble(3, playerData.getBalance());
                
                statement.executeUpdate();
            }
        }
    }
    
    /**
     * Aktualisiert den Spielernamen
     */
    private void updatePlayerName(UUID uuid, String newName) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "UPDATE cb_players SET name = ? WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newName);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
        }
    }
    
    /**
     * Lädt die Homes eines Spielers
     */
    private void loadPlayerHomes(PlayerData playerData) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT * FROM cb_homes WHERE player_uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerData.getUuid().toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        playerData.addHome(
                            resultSet.getString("name"),
                            resultSet.getString("world"),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            resultSet.getFloat("yaw"),
                            resultSet.getFloat("pitch")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden der Homes", e);
        }
    }
    
    /**
     * Lädt die Back-Locations eines Spielers
     */
    private void loadPlayerBackLocations(PlayerData playerData) {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = """
                SELECT * FROM cb_back_locations 
                WHERE player_uuid = ? 
                ORDER BY created_at DESC 
                LIMIT ?
            """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerData.getUuid().toString());
                statement.setInt(2, plugin.getConfigManager().getMaxBackLocations());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        playerData.addBackLocation(
                            resultSet.getString("world"),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            resultSet.getFloat("yaw"),
                            resultSet.getFloat("pitch"),
                            resultSet.getString("reason")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden der Back-Locations", e);
        }
    }
    
    /**
     * Speichert Spielerdaten beim Quit
     */
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = playerDataCache.get(uuid);
        
        if (playerData != null) {
            databaseManager.executeAsync(() -> {
                try (Connection connection = databaseManager.getConnection()) {
                    // Update Spielerdaten
                    String sql = """
                        UPDATE cb_players 
                        SET name = ?, balance = ?, last_seen = NOW(), play_time = ?
                        WHERE uuid = ?
                    """;
                    
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, playerData.getName());
                        statement.setDouble(2, playerData.getBalance());
                        statement.setLong(3, playerData.getPlayTime());
                        statement.setString(4, uuid.toString());
                        
                        statement.executeUpdate();
                    }
                    
                    // Speichere Homes
                    savePlayerHomes(playerData);
                    
                    // Speichere Back-Locations
                    savePlayerBackLocations(playerData);
                    
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Fehler beim Speichern der Spielerdaten für " + player.getName(), e);
                }
            });
        }
        
        // Entferne aus Cache
        playerDataCache.remove(uuid);
    }
    
    /**
     * Speichert die Homes eines Spielers
     */
    private void savePlayerHomes(PlayerData playerData) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            // Lösche alte Homes
            String deleteSql = "DELETE FROM cb_homes WHERE player_uuid = ?";
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setString(1, playerData.getUuid().toString());
                deleteStatement.executeUpdate();
            }
            
            // Speichere neue Homes
            if (!playerData.getHomes().isEmpty()) {
                String insertSql = """
                    INSERT INTO cb_homes (player_uuid, name, world, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
                
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    for (Map.Entry<String, PlayerData.HomeLocation> entry : playerData.getHomes().entrySet()) {
                        PlayerData.HomeLocation home = entry.getValue();
                        insertStatement.setString(1, playerData.getUuid().toString());
                        insertStatement.setString(2, entry.getKey());
                        insertStatement.setString(3, home.getWorld());
                        insertStatement.setDouble(4, home.getX());
                        insertStatement.setDouble(5, home.getY());
                        insertStatement.setDouble(6, home.getZ());
                        insertStatement.setFloat(7, home.getYaw());
                        insertStatement.setFloat(8, home.getPitch());
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
            }
        }
    }
    
    /**
     * Speichert die Back-Locations eines Spielers
     */
    private void savePlayerBackLocations(PlayerData playerData) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            // Lösche alte Back-Locations (behalte nur die neuesten)
            String deleteSql = """
                DELETE FROM cb_back_locations 
                WHERE player_uuid = ? 
                AND id NOT IN (
                    SELECT id FROM (
                        SELECT id FROM cb_back_locations 
                        WHERE player_uuid = ? 
                        ORDER BY created_at DESC 
                        LIMIT ?
                    ) AS recent
                )
            """;
            
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                deleteStatement.setString(1, playerData.getUuid().toString());
                deleteStatement.setString(2, playerData.getUuid().toString());
                deleteStatement.setInt(3, plugin.getConfigManager().getMaxBackLocations());
                deleteStatement.executeUpdate();
            }
        }
    }
    
    /**
     * Holt Spielerdaten aus dem Cache
     */
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }
    
    /**
     * Holt Spielerdaten aus dem Cache (mit Player-Objekt)
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Überprüft ob Spielerdaten geladen sind
     */
    public boolean isPlayerDataLoaded(UUID uuid) {
        return playerDataCache.containsKey(uuid);
    }
    
    /**
     * Speichert alle Spielerdaten (wird beim Plugin-Disable aufgerufen)
     */
    public void saveAllPlayerData() {
        plugin.getLogger().info("Speichere alle Spielerdaten...");
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerData(player);
        }
        
        plugin.getLogger().info("Alle Spielerdaten gespeichert!");
    }
    
    /**
     * Lädt Spielerdaten aus der Datenbank (falls nicht im Cache)
     */
    public PlayerData loadPlayerDataSync(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "SELECT * FROM cb_players WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        PlayerData playerData = new PlayerData(
                            uuid,
                            resultSet.getString("name"),
                            resultSet.getDouble("balance"),
                            resultSet.getTimestamp("first_join"),
                            resultSet.getTimestamp("last_seen"),
                            resultSet.getLong("play_time")
                        );
                        
                        playerDataCache.put(uuid, playerData);
                        return playerData;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler beim Laden der Spielerdaten für " + uuid, e);
        }
        
        return null;
    }
    
    /**
     * Räumt den Cache auf (entfernt verwaiste Einträge)
     */
    public void cleanupCache() {
        playerDataCache.entrySet().removeIf(entry -> {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
    }
    
    /**
     * Holt die Anzahl der gecachten Spielerdaten
     */
    public int getCachedPlayerCount() {
        return playerDataCache.size();
    }
}
