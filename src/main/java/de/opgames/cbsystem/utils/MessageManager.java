package de.opgames.cbsystem.utils;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MessageManager {
    
    private final CBSystem plugin;
    private FileConfiguration messages;
    private final Map<String, String> messageCache = new HashMap<>();
    
    public MessageManager(CBSystem plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        File messageFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messageFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messageFile);
        
        // Load default messages from jar
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaultMessages);
        }
        
        // Cache alle Nachrichten für bessere Performance
        cacheMessages();
        
        plugin.getLogger().info("Nachrichten geladen! Sprache: " + plugin.getConfigManager().getLanguage());
    }
    
    private void cacheMessages() {
        messageCache.clear();
        for (String key : messages.getKeys(true)) {
            if (!messages.isConfigurationSection(key)) {
                messageCache.put(key, messages.getString(key));
            }
        }
    }
    
    public void reloadMessages() {
        loadMessages();
    }
    
    /**
     * Sendet eine Nachricht an einen CommandSender
     */
    public void sendMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = getMessage(messageKey, replacements);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(plugin.getConfigManager().getPrefix() + message));
        }
    }
    
    /**
     * Sendet eine Nachricht ohne Prefix
     */
    public void sendRawMessage(CommandSender sender, String messageKey, String... replacements) {
        String message = getMessage(messageKey, replacements);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(message));
        }
    }
    
    /**
     * Holt eine Nachricht aus der Konfiguration
     */
    public String getMessage(String messageKey, String... replacements) {
        String message = messageCache.get(messageKey);
        
        if (message == null) {
            // Fallback: Versuche direkt aus der Config zu laden
            message = messages.getString(messageKey);
            if (message == null) {
                plugin.getLogger().log(Level.WARNING, "Nachricht nicht gefunden: " + messageKey);
                return "&cNachricht nicht gefunden: " + messageKey;
            }
        }
        
        // Platzhalter ersetzen
        if (replacements.length > 0) {
            message = replacePlaceholders(message, replacements);
        }
        
        return message;
    }
    
    /**
     * Holt eine kolorierte Nachricht
     */
    public String getColorizedMessage(String messageKey, String... replacements) {
        return colorize(getMessage(messageKey, replacements));
    }
    
    /**
     * Ersetzt Platzhalter in einer Nachricht
     */
    private String replacePlaceholders(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Ungerade Anzahl von Platzhalter-Ersetzungen für Nachricht: " + message);
            return message;
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String replacement = replacements[i + 1];
            message = message.replace(placeholder, replacement);
        }
        
        return message;
    }
    
    /**
     * Koloriert eine Nachricht
     */
    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Sendet eine Erfolgsnachricht
     */
    public void sendSuccessMessage(CommandSender sender, String messageKey, String... replacements) {
        sendMessage(sender, messageKey, replacements);
        if (sender instanceof Player) {
            plugin.getSoundManager().playSuccessSound((Player) sender);
        }
    }
    
    /**
     * Sendet eine Fehlernachricht
     */
    public void sendErrorMessage(CommandSender sender, String messageKey, String... replacements) {
        sendMessage(sender, messageKey, replacements);
        if (sender instanceof Player) {
            plugin.getSoundManager().playErrorSound((Player) sender);
        }
    }
    
    /**
     * Sendet eine Info-Nachricht
     */
    public void sendInfoMessage(CommandSender sender, String messageKey, String... replacements) {
        sendMessage(sender, messageKey, replacements);
        if (sender instanceof Player) {
            plugin.getSoundManager().playClickSound((Player) sender);
        }
    }
    
    /**
     * Broadcast eine Nachricht an alle Spieler
     */
    public void broadcastMessage(String messageKey, String... replacements) {
        String message = getColorizedMessage(messageKey, replacements);
        plugin.getServer().broadcastMessage(colorize(plugin.getConfigManager().getPrefix() + message));
    }
    
    /**
     * Sendet eine Nachricht an alle Spieler mit einer bestimmten Berechtigung
     */
    public void sendMessageToPlayersWithPermission(String permission, String messageKey, String... replacements) {
        String message = getColorizedMessage(messageKey, replacements);
        String fullMessage = colorize(plugin.getConfigManager().getPrefix() + message);
        
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission(permission))
            .forEach(player -> player.sendMessage(fullMessage));
    }
    
    /**
     * Formatiert eine Zahl als Währung
     */
    public String formatCurrency(double amount) {
        return String.format("%.2f%s", amount, plugin.getConfigManager().getCurrencySymbol());
    }
    
    /**
     * Formatiert Zeit in einem lesbaren Format
     */
    public String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " Sekunde" + (seconds != 1 ? "n" : "");
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " Minute" + (minutes != 1 ? "n" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " Stunde" + (hours != 1 ? "n" : "");
        } else {
            long days = seconds / 86400;
            return days + " Tag" + (days != 1 ? "e" : "");
        }
    }
    
    /**
     * Erstellt eine Fortschrittsleiste
     */
    public String createProgressBar(double current, double max, int length, char completeChar, char incompleteChar) {
        double percentage = Math.max(0, Math.min(1, current / max));
        int completed = (int) (length * percentage);
        
        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < completed; i++) {
            bar.append(completeChar);
        }
        bar.append("&7");
        for (int i = completed; i < length; i++) {
            bar.append(incompleteChar);
        }
        
        return colorize(bar.toString());
    }
    
    /**
     * Holt eine String-Liste aus der Konfiguration
     */
    public List<String> getStringList(String path) {
        return messages.getStringList(path);
    }
    
    /**
     * Überprüft ob eine Nachricht existiert
     */
    public boolean hasMessage(String messageKey) {
        return messageCache.containsKey(messageKey) || messages.contains(messageKey);
    }
    
    /**
     * Fügt eine neue Nachricht hinzu (nur zur Laufzeit)
     */
    public void addMessage(String key, String message) {
        messageCache.put(key, message);
    }
}
