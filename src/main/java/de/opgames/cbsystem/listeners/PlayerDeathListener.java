package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlayerDeathListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Speichere Death-Location für /back
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null) {
            playerData.addBackLocation(player.getLocation(), "Tod");
        }
        
        // Benutzerdefinierte Death-Message
        if (event.getDeathMessage() != null) {
            String deathMessage = formatDeathMessage(event.getDeathMessage(), player);
            event.setDeathMessage(deathMessage);
        }
        
        // Economy-Verlust bei Tod (konfigurierbar)
        if (plugin.getConfigManager().isEconomyEnabled()) {
            handleDeathEconomyLoss(player);
        }
        
        // Teleportations-Cooldowns zurücksetzen
        if (playerData != null) {
            playerData.removeCooldown("home");
            playerData.removeCooldown("warp");
            playerData.removeCooldown("tpa");
        }
        
        // Log
        if (plugin.getConfigManager().isLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s ist gestorben bei %s", 
                player.getName(), locationToString(player.getLocation())));
        }
    }
    
    private String formatDeathMessage(String originalMessage, Player player) {
        // Ersetze Standard-Death-Messages mit benutzerdefinierten
        String message = originalMessage;
        
        // Beispiel-Formatierung
        if (message.contains("fell from a high place")) {
            message = "&e" + player.getName() + " &7ist zu tief gefallen!";
        } else if (message.contains("was slain by")) {
            message = message.replace("was slain by", "&7wurde getötet von");
        } else if (message.contains("drowned")) {
            message = "&e" + player.getName() + " &7ist ertrunken!";
        } else if (message.contains("burned to death")) {
            message = "&e" + player.getName() + " &7ist verbrannt!";
        } else if (message.contains("starved to death")) {
            message = "&e" + player.getName() + " &7ist verhungert!";
        }
        
        return plugin.getMessageManager().colorize(message);
    }
    
    private void handleDeathEconomyLoss(Player player) {
        // Konfigurierbare Geld-Verlust bei Tod
        double lossPercentage = plugin.getConfig().getDouble("economy.death-loss-percentage", 0.0);
        
        if (lossPercentage <= 0) {
            return;
        }
        
        double currentBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        double lossAmount = currentBalance * (lossPercentage / 100.0);
        
        if (lossAmount > 0) {
            plugin.getEconomyManager().withdrawBalance(player.getUniqueId(), lossAmount, "Tod-Verlust");
            
            // Benachrichtige den Spieler beim Respawn
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getMessageManager().sendMessage(player, "economy.death-loss",
                        "amount", plugin.getEconomyManager().formatBalance(lossAmount));
                }
            }, 40L); // 2 Sekunden nach Respawn
        }
    }
    
    private String locationToString(org.bukkit.Location location) {
        return String.format("%s:%.1f,%.1f,%.1f",
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ());
    }
}
