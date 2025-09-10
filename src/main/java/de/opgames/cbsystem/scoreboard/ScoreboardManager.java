package de.opgames.cbsystem.scoreboard;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    
    private final CBSystem plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Map<UUID, Objective> playerObjectives;
    
    // Scoreboard-Konfiguration
    private static final String SCOREBOARD_TITLE = "&6&lOP-Games.de";
    private static final int UPDATE_INTERVAL = 20; // 1 Sekunde
    
    public ScoreboardManager(CBSystem plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.playerObjectives = new HashMap<>();
        
        // Scoreboard-Update-Task starten
        startUpdateTask();
    }
    
    /**
     * Erstellt ein Scoreboard für einen Spieler
     */
    public void createScoreboard(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            return;
        }
        
        // Scoreboard erstellen
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("cbsystem", "dummy", 
            plugin.getMessageManager().colorize(SCOREBOARD_TITLE));
        
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Scoreboard speichern
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        playerObjectives.put(player.getUniqueId(), objective);
        
        // Scoreboard dem Spieler zuweisen
        player.setScoreboard(scoreboard);
        
        // Scoreboard aktualisieren
        updateScoreboard(player);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Scoreboard für Spieler " + player.getName() + " erstellt");
        }
    }
    
    /**
     * Entfernt das Scoreboard eines Spielers
     */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        playerObjectives.remove(player.getUniqueId());
        
        // Standard-Scoreboard wiederherstellen
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Scoreboard für Spieler " + player.getName() + " entfernt");
        }
    }
    
    /**
     * Aktualisiert das Scoreboard eines Spielers
     */
    public void updateScoreboard(Player player) {
        if (!playerScoreboards.containsKey(player.getUniqueId())) {
            return;
        }
        
        Objective objective = playerObjectives.get(player.getUniqueId());
        if (objective == null) {
            return;
        }
        
        // Alle Scores löschen
        for (String entry : objective.getScoreboard().getEntries()) {
            objective.getScoreboard().resetScores(entry);
        }
        
        // Neue Scores hinzufügen
        addScoreboardLines(player, objective);
    }
    
    /**
     * Fügt die Scoreboard-Zeilen hinzu
     */
    private void addScoreboardLines(Player player, Objective objective) {
        int score = 15; // Start-Score (wird nach unten gezählt)
        
        // Leerzeile
        addScore(objective, "", score--);
        
        // Spieler-Info
        addScore(objective, "&7Spieler: &e" + player.getName(), score--);
        addScore(objective, "&7Rang: &a" + getPlayerRank(player), score--);
        
        // Leerzeile
        addScore(objective, "", score--);
        
        // Economy-Info
        if (plugin.getConfigManager().isEconomyEnabled()) {
            addScore(objective, "&6&lEconomy", score--);
            
            // Taschengeld
            double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            addScore(objective, "&7Taschengeld: &a" + plugin.getEconomyManager().formatBalance(pocketBalance), score--);
            
            // Bankguthaben
            if (plugin.getBankManager() != null) {
                double bankBalance = plugin.getBankManager().getBankBalance(player.getUniqueId());
                addScore(objective, "&7Bankguthaben: &e" + plugin.getEconomyManager().formatBalance(bankBalance), score--);
            }
        }
        
        // Leerzeile
        addScore(objective, "", score--);
        
        // Plot-Info
        if (plugin.getConfigManager().isPlotSquaredEnabled()) {
            addScore(objective, "&6&lPlots", score--);
            
            int plotCount = plugin.getPlotManager().getPlayerPlotCount(player.getUniqueId());
            int maxPlots = plugin.getPlotManager().getMaxPlotsForPlayer(player.getUniqueId());
            addScore(objective, "&7Plots: &a" + plotCount + "&7/&e" + maxPlots, score--);
            
            // Aktueller Plot
            if (plugin.getPlotManager().isInPlot(player)) {
                String plotId = plugin.getPlotManager().getCurrentPlotId(player);
                String plotOwner = plugin.getPlotManager().getCurrentPlotOwner(player);
                addScore(objective, "&7Aktueller Plot: &e" + plotId, score--);
                addScore(objective, "&7Besitzer: &a" + plotOwner, score--);
            }
        }
        
        // Leerzeile
        addScore(objective, "", score--);
        
        // Server-Info
        addScore(objective, "&6&lServer", score--);
        addScore(objective, "&7Online: &a" + Bukkit.getOnlinePlayers().size() + "&7/&e" + Bukkit.getMaxPlayers(), score--);
        addScore(objective, "&7TPS: &a" + getServerTPS(), score--);
        
        // Leerzeile
        addScore(objective, "", score--);
        
        // Footer
        addScore(objective, "&7op-games.de", score--);
    }
    
    /**
     * Fügt eine Scoreboard-Zeile hinzu
     */
    private void addScore(Objective objective, String text, int score) {
        String coloredText = plugin.getMessageManager().colorize(text);
        objective.getScore(coloredText).setScore(score);
    }
    
    /**
     * Holt den Spieler-Rang
     */
    private String getPlayerRank(Player player) {
        if (player.hasPermission("cbsystem.admin.*")) {
            return "&cAdmin";
        } else if (player.hasPermission("cbsystem.mod.*")) {
            return "&6Moderator";
        } else if (player.hasPermission("cbsystem.vip.*")) {
            return "&dVIP";
        } else if (player.hasPermission("cbsystem.premium.*")) {
            return "&ePremium";
        } else {
            return "&7Spieler";
        }
    }
    
    /**
     * Holt die Server-TPS (vereinfacht)
     */
    private String getServerTPS() {
        // Vereinfachte TPS-Berechnung
        double tps = 20.0; // Standard-TPS
        if (Bukkit.getServer().getTps()[0] > 0) {
            tps = Math.min(20.0, Bukkit.getServer().getTps()[0]);
        }
        
        if (tps >= 18.0) {
            return "&a" + String.format("%.1f", tps);
        } else if (tps >= 15.0) {
            return "&e" + String.format("%.1f", tps);
        } else {
            return "&c" + String.format("%.1f", tps);
        }
    }
    
    /**
     * Startet den Scoreboard-Update-Task
     */
    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerScoreboards.containsKey(player.getUniqueId())) {
                    // Scoreboard-Update im Hauptthread ausführen
                    Bukkit.getScheduler().runTask(plugin, () -> updateScoreboard(player));
                }
            }
        }, 0L, UPDATE_INTERVAL);
    }
    
    /**
     * Aktualisiert alle Scoreboards
     */
    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerScoreboards.containsKey(player.getUniqueId())) {
                updateScoreboard(player);
            }
        }
    }
    
    /**
     * Überprüft ob ein Spieler ein Scoreboard hat
     */
    public boolean hasScoreboard(Player player) {
        return playerScoreboards.containsKey(player.getUniqueId());
    }
    
    /**
     * Schaltet das Scoreboard für einen Spieler um
     */
    public void toggleScoreboard(Player player) {
        if (hasScoreboard(player)) {
            removeScoreboard(player);
            plugin.getMessageManager().sendMessage(player, "scoreboard.disabled");
        } else {
            createScoreboard(player);
            plugin.getMessageManager().sendMessage(player, "scoreboard.enabled");
        }
    }
    
    /**
     * Räumt alle Scoreboards auf
     */
    public void cleanup() {
        for (UUID playerUUID : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                removeScoreboard(player);
            }
        }
        playerScoreboards.clear();
        playerObjectives.clear();
    }
}
