package de.opgames.cbsystem.listeners;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlayerChatListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Chat-Format erweitern
        String formattedMessage = formatChatMessage(player, message);
        event.setFormat(formattedMessage);
        
        // Plot-Chat (falls aktiviert und Spieler ist auf einem Plot)
        if (plugin.getConfigManager().isPlotChatEnabled() && 
            plugin.getPlotManager().isEnabled()) {
            
            handlePlotChat(event, player);
        }
        
        // Chat-Filter (Schimpfwörter, Spam, etc.)
        if (shouldFilterMessage(message)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "chat.message-filtered");
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        // Anti-Spam
        if (isSpamming(player, message)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "chat.anti-spam");
            plugin.getSoundManager().playWarningSound(player);
            return;
        }
        
        // Log Chat (falls aktiviert)
        if (plugin.getConfigManager().isLoggingEnabled()) {
            plugin.getLogger().info(String.format("[CHAT] %s: %s", player.getName(), message));
        }
    }
    
    private String formatChatMessage(Player player, String message) {
        StringBuilder format = new StringBuilder();
        
        // Prefix für Ränge (falls Permissions-Plugin vorhanden)
        String prefix = getPlayerPrefix(player);
        if (!prefix.isEmpty()) {
            format.append(prefix).append(" ");
        }
        
        // Spielername mit Farbe basierend auf Rang
        String nameColor = getPlayerNameColor(player);
        format.append(nameColor).append("%s");
        
        // Suffix für Ränge
        String suffix = getPlayerSuffix(player);
        if (!suffix.isEmpty()) {
            format.append(" ").append(suffix);
        }
        
        // Plot-Info (falls auf einem Plot)
        if (plugin.getPlotManager().isEnabled()) {
            Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
            if (currentPlot != null && currentPlot.hasOwner()) {
                format.append(" &8[&6Plot: ").append(currentPlot.getId()).append("&8]");
            }
        }
        
        // Separator und Nachricht
        format.append("&8: &f%s");
        
        return plugin.getMessageManager().colorize(format.toString());
    }
    
    private void handlePlotChat(AsyncPlayerChatEvent event, Player player) {
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null || !currentPlot.hasOwner()) {
            return;
        }
        
        // Überprüfe ob Spieler Plot-Chat verwenden möchte (z.B. mit Prefix)
        String message = event.getMessage();
        if (!message.startsWith("@plot ") && !message.startsWith("@p ")) {
            return;
        }
        
        // Entferne Plot-Chat-Prefix
        message = message.substring(message.indexOf(" ") + 1);
        event.setMessage(message);
        
        // Beschränke Chat auf Plot-Mitglieder
        event.getRecipients().clear();
        
        // Füge Plot-Owner hinzu
        Player owner = plugin.getServer().getPlayer(currentPlot.getOwnerAbs());
        if (owner != null && owner.isOnline()) {
            event.getRecipients().add(owner);
        }
        
        // Füge vertraute Spieler hinzu
        for (Object trustedUUID : currentPlot.getTrusted()) {
            Player trustedPlayer = plugin.getServer().getPlayer(java.util.UUID.fromString(trustedUUID.toString()));
            if (trustedPlayer != null && trustedPlayer.isOnline()) {
                event.getRecipients().add(trustedPlayer);
            }
        }
        
        // Füge Mitglieder hinzu
        for (Object memberUUID : currentPlot.getMembers()) {
            Player memberPlayer = plugin.getServer().getPlayer(java.util.UUID.fromString(memberUUID.toString()));
            if (memberPlayer != null && memberPlayer.isOnline()) {
                event.getRecipients().add(memberPlayer);
            }
        }
        
        // Ändere Format für Plot-Chat
        event.setFormat(plugin.getMessageManager().colorize(
            "&8[&6Plot-Chat&8] " + getPlayerNameColor(player) + "%s&8: &f%s"));
    }
    
    private boolean shouldFilterMessage(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Einfache Schimpfwort-Filter
        String[] bannedWords = {
            "idiot", "dumm", "blöd", "scheiß", "verdammt"
            // Weitere Wörter können hier hinzugefügt werden
        };
        
        for (String bannedWord : bannedWords) {
            if (lowerMessage.contains(bannedWord)) {
                return true;
            }
        }
        
        // Übermäßige Großschreibung
        long upperCaseCount = message.chars().filter(Character::isUpperCase).count();
        if (message.length() > 10 && upperCaseCount > message.length() * 0.7) {
            return true;
        }
        
        // Übermäßige Wiederholung von Zeichen
        if (message.matches(".*([a-zA-Z])\\1{4,}.*")) {
            return true;
        }
        
        return false;
    }
    
    private boolean isSpamming(Player player, String message) {
        // Vereinfachtes Anti-Spam (sollte in einem echten System erweitert werden)
        long currentTime = System.currentTimeMillis();
        
        // Überprüfe letzte Nachricht-Zeit (gespeichert in Player-Metadaten)
        if (player.hasMetadata("lastChatTime")) {
            long lastChatTime = player.getMetadata("lastChatTime").get(0).asLong();
            
            if (currentTime - lastChatTime < 2000) { // 2 Sekunden Cooldown
                return true;
            }
        }
        
        // Setze neue Chat-Zeit
        player.setMetadata("lastChatTime", new org.bukkit.metadata.FixedMetadataValue(plugin, currentTime));
        
        return false;
    }
    
    private String getPlayerPrefix(Player player) {
        // Hier würde normalerweise ein Permissions-Plugin (wie LuckPerms) abgefragt werden
        if (player.hasPermission("cbsystem.admin.*")) {
            return "&4[Admin]";
        } else if (player.hasPermission("cbsystem.moderator")) {
            return "&c[Mod]";
        } else if (player.hasPermission("cbsystem.vip")) {
            return "&6[VIP]";
        }
        return "";
    }
    
    private String getPlayerSuffix(Player player) {
        // Hier könnten Suffixe basierend auf Achievements, Rängen etc. hinzugefügt werden
        return "";
    }
    
    private String getPlayerNameColor(Player player) {
        // Farbe basierend auf Rang
        if (player.hasPermission("cbsystem.admin.*")) {
            return "&4";
        } else if (player.hasPermission("cbsystem.moderator")) {
            return "&c";
        } else if (player.hasPermission("cbsystem.vip")) {
            return "&6";
        }
        return "&7"; // Standard-Farbe
    }
}
