package de.opgames.cbsystem.utils;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final CBSystem plugin;
    private FileConfiguration config;
    
    public ConfigManager(CBSystem plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // Plugin-Einstellungen
    public String getPrefix() {
        return config.getString("plugin.prefix", "&8[&6CB&eSystem&8] &7");
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("plugin.debug", false);
    }
    
    public String getLanguage() {
        return config.getString("plugin.language", "de");
    }
    
    public boolean isUpdateCheckEnabled() {
        return config.getBoolean("plugin.update-check", true);
    }
    
    // Datenbank-Einstellungen
    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enabled", true);
    }
    
    public String getDatabaseHost() {
        return config.getString("database.host", "localhost");
    }
    
    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }
    
    public String getDatabaseName() {
        return config.getString("database.database", "cbsystem");
    }
    
    public String getDatabaseUsername() {
        return config.getString("database.username", "root");
    }
    
    public String getDatabasePassword() {
        return config.getString("database.password", "");
    }
    
    public int getMaximumPoolSize() {
        return config.getInt("database.pool.maximum-pool-size", 10);
    }
    
    public int getMinimumIdle() {
        return config.getInt("database.pool.minimum-idle", 2);
    }
    
    public long getMaximumLifetime() {
        return config.getLong("database.pool.maximum-lifetime", 1800000);
    }
    
    public long getConnectionTimeout() {
        return config.getLong("database.pool.connection-timeout", 5000);
    }
    
    public long getLeakDetectionThreshold() {
        return config.getLong("database.pool.leak-detection-threshold", 60000);
    }
    
    // PlotSquared-Einstellungen
    public boolean isPlotSquaredEnabled() {
        return config.getBoolean("plotsquared.enabled", true);
    }
    
    public boolean isEconomyIntegrationEnabled() {
        return config.getBoolean("plotsquared.economy-integration", true);
    }
    
    public boolean isPlotChatEnabled() {
        return config.getBoolean("plotsquared.features.plot-chat", true);
    }
    
    public boolean isPlotInfoGUIEnabled() {
        return config.getBoolean("plotsquared.features.plot-info-gui", true);
    }
    
    public boolean isPlotManagementGUIEnabled() {
        return config.getBoolean("plotsquared.features.plot-management-gui", true);
    }
    
    public double getPlotClaimPrice() {
        return config.getDouble("plotsquared.prices.claim-plot", 10000.0);
    }
    
    public double getAdditionalPlotMultiplier() {
        return config.getDouble("plotsquared.prices.additional-plot-multiplier", 1.5);
    }
    
    public int getMaxPlotsForRank(String rank) {
        return config.getInt("plotsquared.limits." + rank + ".max-plots", 3);
    }
    
    public int getMaxMembersForRank(String rank) {
        return config.getInt("plotsquared.limits." + rank + ".max-members", 10);
    }
    
    // Home-System-Einstellungen
    public boolean isHomesEnabled() {
        return config.getBoolean("homes.enabled", true);
    }
    
    public int getMaxHomes() {
        return config.getInt("homes.max-homes", 5);
    }
    
    public int getHomeTeleportCooldown() {
        return config.getInt("homes.teleport-cooldown", 3);
    }
    
    public double getCostPerHome() {
        return config.getDouble("homes.cost-per-home", 5000.0);
    }
    
    // Warp-System-Einstellungen
    public boolean isWarpsEnabled() {
        return config.getBoolean("warps.enabled", true);
    }
    
    public int getWarpTeleportCooldown() {
        return config.getInt("warps.teleport-cooldown", 5);
    }
    
    // Economy-Einstellungen
    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", true);
    }
    
    public double getStartingBalance() {
        return config.getDouble("economy.starting-balance", 1000.0);
    }
    
    public String getCurrencySymbol() {
        return config.getString("economy.currency-symbol", "$");
    }
    
    public String getCurrencyName() {
        return config.getString("economy.currency-name", "Dollar");
    }
    
    public String getCurrencyNamePlural() {
        return config.getString("economy.currency-name-plural", "Dollar");
    }
    
    public double getMaxBalance() {
        return config.getDouble("economy.max-balance", 999999999.0);
    }
    
    public boolean isPayEnabled() {
        return config.getBoolean("economy.pay.enabled", true);
    }
    
    public double getMinPayAmount() {
        return config.getDouble("economy.pay.min-amount", 1.0);
    }
    
    public double getMaxPayAmount() {
        return config.getDouble("economy.pay.max-amount", 100000.0);
    }
    
    public double getPayTaxPercentage() {
        return config.getDouble("economy.pay.tax-percentage", 0.0);
    }
    
    // Shop-Einstellungen
    public boolean isShopEnabled() {
        return config.getBoolean("shop.enabled", true);
    }
    
    public String getShopGUITitle() {
        return config.getString("shop.gui.title", "&6&lShop &8- &7OP-Games.de");
    }
    
    public int getShopGUISize() {
        return config.getInt("shop.gui.size", 54);
    }
    
    // TPA-Einstellungen
    public boolean isTPAEnabled() {
        return config.getBoolean("tpa.enabled", true);
    }
    
    public int getTPARequestTimeout() {
        return config.getInt("tpa.request-timeout", 60);
    }
    
    public int getTPATeleportDelay() {
        return config.getInt("tpa.teleport-delay", 3);
    }
    
    public int getMaxTPARequests() {
        return config.getInt("tpa.max-requests", 5);
    }
    
    // Back-System-Einstellungen
    public boolean isBackEnabled() {
        return config.getBoolean("back.enabled", true);
    }
    
    public int getBackCooldown() {
        return config.getInt("back.cooldown", 10);
    }
    
    public int getMaxBackLocations() {
        return config.getInt("back.max-locations", 10);
    }
    
    // Spawn-System-Einstellungen
    public boolean isSpawnEnabled() {
        return config.getBoolean("spawn.enabled", true);
    }
    
    public int getSpawnTeleportCooldown() {
        return config.getInt("spawn.teleport-cooldown", 3);
    }
    
    public String getSpawnWorld() {
        return config.getString("spawn.location.world", "world");
    }
    
    public double getSpawnX() {
        return config.getDouble("spawn.location.x", 0.5);
    }
    
    public double getSpawnY() {
        return config.getDouble("spawn.location.y", 100.0);
    }
    
    public double getSpawnZ() {
        return config.getDouble("spawn.location.z", 0.5);
    }
    
    public float getSpawnYaw() {
        return (float) config.getDouble("spawn.location.yaw", 0.0);
    }
    
    public float getSpawnPitch() {
        return (float) config.getDouble("spawn.location.pitch", 0.0);
    }
    
    public void setSpawnLocation(String world, double x, double y, double z, float yaw, float pitch) {
        config.set("spawn.location.world", world);
        config.set("spawn.location.x", x);
        config.set("spawn.location.y", y);
        config.set("spawn.location.z", z);
        config.set("spawn.location.yaw", yaw);
        config.set("spawn.location.pitch", pitch);
        plugin.saveConfig();
    }
    
    // Sound-Einstellungen
    public boolean areSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }
    
    public String getGUIClickSound() {
        return config.getString("sounds.gui.click", "UI_BUTTON_CLICK");
    }
    
    public String getGUISuccessSound() {
        return config.getString("sounds.gui.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }
    
    public String getGUIErrorSound() {
        return config.getString("sounds.gui.error", "ENTITY_VILLAGER_NO");
    }
    
    public String getGUIOpenSound() {
        return config.getString("sounds.gui.open", "BLOCK_CHEST_OPEN");
    }
    
    public String getGUICloseSound() {
        return config.getString("sounds.gui.close", "BLOCK_CHEST_CLOSE");
    }
    
    public String getTeleportStartSound() {
        return config.getString("sounds.teleport.start", "ENTITY_ENDERMAN_TELEPORT");
    }
    
    public String getTeleportSuccessSound() {
        return config.getString("sounds.teleport.success", "ENTITY_PLAYER_LEVELUP");
    }
    
    public String getTeleportCancelledSound() {
        return config.getString("sounds.teleport.cancelled", "ENTITY_VILLAGER_NO");
    }
    
    public String getMoneyReceiveSound() {
        return config.getString("sounds.money.receive", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }
    
    public String getMoneySendSound() {
        return config.getString("sounds.money.send", "ENTITY_ARROW_HIT_PLAYER");
    }
    
    public String getMoneyInsufficientSound() {
        return config.getString("sounds.money.insufficient", "ENTITY_VILLAGER_NO");
    }
    
    // GUI-Einstellungen
    public boolean shouldFillEmptySlots() {
        return config.getBoolean("gui.fill-empty-slots", true);
    }
    
    public String getFillItem() {
        return config.getString("gui.fill-item", "GRAY_STAINED_GLASS_PANE");
    }
    
    public String getFillItemName() {
        return config.getString("gui.fill-item-name", "&7");
    }
    
    // Plot-GUI-Einstellungen
    public String getPlotMenuTitle() {
        return config.getString("gui.plot-menu.title", "&8Plot-Menü");
    }
    
    public int getPlotMenuSize() {
        return config.getInt("gui.plot-menu.size", 27);
    }
    
    // Performance-Einstellungen
    public boolean isAutoSaveEnabled() {
        return config.getBoolean("performance.auto-save.enabled", true);
    }
    
    public int getAutoSaveInterval() {
        return config.getInt("performance.auto-save.interval", 300);
    }
    
    public int getPlayerDataCacheTime() {
        return config.getInt("performance.cache.player-data-cache-time", 600);
    }
    
    public int getPlotCacheTime() {
        return config.getInt("performance.cache.plot-cache-time", 300);
    }
    
    public boolean isAsyncDatabaseOperations() {
        return config.getBoolean("performance.async.database-operations", true);
    }
    
    public boolean isAsyncFileOperations() {
        return config.getBoolean("performance.async.file-operations", true);
    }
    
    // Admin-Einstellungen
    public boolean isMaintenanceEnabled() {
        return config.getBoolean("admin.maintenance.enabled", false);
    }
    
    public String getMaintenanceMessage() {
        return config.getString("admin.maintenance.message", "&cDer Server befindet sich im Wartungsmodus!");
    }
    
    public boolean isLoggingEnabled() {
        return config.getBoolean("admin.logging.enabled", true);
    }
    
    public boolean isCommandLoggingEnabled() {
        return config.getBoolean("admin.logging.log-commands", true);
    }
    
    public boolean isTeleportLoggingEnabled() {
        return config.getBoolean("admin.logging.log-teleports", true);
    }
    
    public boolean isMoneyTransactionLoggingEnabled() {
        return config.getBoolean("admin.logging.log-money-transactions", true);
    }
    
    public boolean isPlotActionLoggingEnabled() {
        return config.getBoolean("admin.logging.log-plot-actions", true);
    }
    
    // Scoreboard-Einstellungen
    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }
    
    public int getScoreboardUpdateInterval() {
        return config.getInt("scoreboard.update-interval", 20);
    }
    
    public boolean isScoreboardAutoCreate() {
        return config.getBoolean("scoreboard.auto-create", true);
    }
    
    public String getScoreboardTitle() {
        return config.getString("scoreboard.title", "&6&lOP-Games.de");
    }
    
    public boolean isScoreboardShowEconomy() {
        return config.getBoolean("scoreboard.lines.show-economy", true);
    }
    
    public boolean isScoreboardShowPlots() {
        return config.getBoolean("scoreboard.lines.show-plots", true);
    }
    
    public boolean isScoreboardShowServer() {
        return config.getBoolean("scoreboard.lines.show-server", true);
    }
    
    public boolean isScoreboardShowRank() {
        return config.getBoolean("scoreboard.lines.show-rank", true);
    }
    
    // Utility-Methoden
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    public void set(String path, Object value) {
        config.set(path, value);
        plugin.saveConfig();
    }
}
