package de.opgames.cbsystem.placeholders;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CBSystemPlaceholders extends PlaceholderExpansion {
    
    private final CBSystem plugin;
    
    public CBSystemPlaceholders(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "cbsystem";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "OP-Games.de";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Placeholder bleibt nach Plugin-Reload bestehen
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        
        // Economy Placeholders
        if (params.equals("balance")) {
            // Debug: Überprüfe ob EconomyManager funktioniert
            if (plugin.getEconomyManager() == null) {
                plugin.getLogger().warning("EconomyManager ist null in PlaceholderAPI!");
                return "0";
            }
            
            // Debug: Überprüfe ob PlayerDataManager funktioniert
            if (plugin.getPlayerDataManager() == null) {
                plugin.getLogger().warning("PlayerDataManager ist null in PlaceholderAPI!");
                return "0";
            }
            
            // Debug: Überprüfe ob Spielerdaten existieren
            var playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (playerData == null) {
                plugin.getLogger().warning("PlayerData für " + player.getName() + " ist null in PlaceholderAPI!");
                return "0";
            }
            
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            plugin.getLogger().info("PlaceholderAPI Balance für " + player.getName() + ": " + balance);
            return plugin.getEconomyManager().formatBalance(balance);
        }
        
        if (params.equals("balance_raw")) {
            return String.valueOf(plugin.getEconomyManager().getBalance(player.getUniqueId()));
        }
        
        if (params.equals("balance_formatted")) {
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            return formatLargeNumber(balance);
        }
        
        if (params.equals("currency_symbol")) {
            return plugin.getConfigManager().getCurrencySymbol();
        }
        
        // Player Data Placeholders
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData != null) {
            
            if (params.equals("homes_count")) {
                return String.valueOf(playerData.getHomeCount());
            }
            
            if (params.equals("homes_max")) {
                return String.valueOf(plugin.getConfigManager().getMaxHomes());
            }
            
            if (params.equals("playtime")) {
                return formatPlaytime(playerData.getPlayTime());
            }
            
            if (params.equals("playtime_hours")) {
                return String.valueOf(playerData.getPlayTime() / 3600000); // ms zu Stunden
            }
            
            if (params.equals("first_join")) {
                return playerData.getFirstJoin().toString();
            }
            
            if (params.equals("last_seen")) {
                return playerData.getLastSeen().toString();
            }
        }
        
        // Auction House Placeholders
        if (params.equals("auctions_active")) {
            return String.valueOf(plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId()).size());
        }
        
        if (params.equals("auctions_sold")) {
            return String.valueOf(plugin.getAuctionManager().getPlayerSoldAuctions(player.getUniqueId()).size());
        }
        
        if (params.equals("auctions_max")) {
            return "5"; // Max Auktionen pro Spieler
        }
        
        // Server Placeholders
        if (params.equals("server_players_online")) {
            return String.valueOf(plugin.getServer().getOnlinePlayers().size());
        }
        
        if (params.equals("server_players_max")) {
            return String.valueOf(plugin.getServer().getMaxPlayers());
        }
        
        if (params.equals("server_auctions_total")) {
            return String.valueOf(plugin.getAuctionManager().getActiveAuctionCount());
        }
        
        // Plot Placeholders (wenn PlotSquared verfügbar)
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            
            if (params.equals("plot_count")) {
                return String.valueOf(plugin.getPlotManager().getPlayerPlotCount(player.getUniqueId()));
            }
            
            if (params.equals("plot_max")) {
                return String.valueOf(plugin.getPlotManager().getMaxPlotsForPlayer(onlinePlayer));
            }
            
            if (params.equals("plot_current")) {
                if (plugin.getPlotManager().isInPlot(onlinePlayer)) {
                    return plugin.getPlotManager().getCurrentPlotId(onlinePlayer);
                }
                return "Kein Plot";
            }
            
            if (params.equals("plot_owner")) {
                if (plugin.getPlotManager().isInPlot(onlinePlayer)) {
                    return plugin.getPlotManager().getCurrentPlotOwner(onlinePlayer);
                }
                return "N/A";
            }
        }
        
        // Rank/Permission Placeholders
        if (params.equals("rank")) {
            return getRank(player);
        }
        
        if (params.equals("rank_prefix")) {
            return getRankPrefix(player);
        }
        
        if (params.equals("rank_suffix")) {
            return getRankSuffix(player);
        }
        
        // Statistics Placeholders
        if (params.equals("money_spent_total")) {
            // TODO: Implementiere Statistik-Tracking
            return "0";
        }
        
        if (params.equals("money_earned_total")) {
            // TODO: Implementiere Statistik-Tracking
            return "0";
        }
        
        if (params.equals("transactions_count")) {
            // TODO: Implementiere Statistik-Tracking
            return "0";
        }
        
        // Leaderboard Placeholders
        if (params.startsWith("top_balance_")) {
            try {
                int position = Integer.parseInt(params.substring("top_balance_".length()));
                return getTopBalance(position);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        if (params.startsWith("top_balance_name_")) {
            try {
                int position = Integer.parseInt(params.substring("top_balance_name_".length()));
                return getTopBalanceName(position);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        // Shop Placeholders
        if (params.equals("shop_transactions_today")) {
            // TODO: Implementiere Shop-Statistiken
            return "0";
        }
        
        return null; // Unbekannter Placeholder
    }
    
    private String formatLargeNumber(double number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000);
        } else {
            return String.format("%.0f", number);
        }
    }
    
    private String formatPlaytime(long playtimeMs) {
        long hours = playtimeMs / 3600000;
        long minutes = (playtimeMs % 3600000) / 60000;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    private String getRank(OfflinePlayer player) {
        if (!player.isOnline()) return "Offline";
        
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer.hasPermission("cbsystem.rank.admin")) {
            return "Admin";
        } else if (onlinePlayer.hasPermission("cbsystem.rank.moderator")) {
            return "Moderator";
        } else if (onlinePlayer.hasPermission("cbsystem.rank.premium")) {
            return "Premium";
        } else if (onlinePlayer.hasPermission("cbsystem.rank.vip")) {
            return "VIP";
        } else {
            return "Spieler";
        }
    }
    
    private String getRankPrefix(OfflinePlayer player) {
        String rank = getRank(player);
        return switch (rank) {
            case "Admin" -> "&4[Admin] &r";
            case "Moderator" -> "&9[Mod] &r";
            case "Premium" -> "&6[Premium] &r";
            case "VIP" -> "&a[VIP] &r";
            default -> "&7[Spieler] &r";
        };
    }
    
    private String getRankSuffix(OfflinePlayer player) {
        String rank = getRank(player);
        return switch (rank) {
            case "Admin" -> " &4★";
            case "Moderator" -> " &9♦";
            case "Premium" -> " &6◆";
            case "VIP" -> " &a♢";
            default -> "";
        };
    }
    
    private String getTopBalance(int position) {
        // TODO: Implementiere Top-Balance-System mit Datenbank
        return "0";
    }
    
    private String getTopBalanceName(int position) {
        // TODO: Implementiere Top-Balance-System mit Datenbank
        return "N/A";
    }
}
