package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CBCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public CBCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (args.length == 0) {
            // Hauptmenü öffnen
            plugin.getGUIManager().openMainMenu(player);
            plugin.getSoundManager().playOpenSound(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help", "hilfe" -> showHelp(player);
            case "info", "version" -> showInfo(player);
            case "reload" -> handleReload(player);
            case "menu", "gui" -> plugin.getGUIManager().openMainMenu(player);
            case "admin" -> handleAdmin(player);
            case "debug" -> handleDebug(player, args);
            default -> {
                plugin.getMessageManager().sendMessage(player, "general.unknown-command");
                showHelp(player);
            }
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "help.header");
        
        List<String> helpCommands = Arrays.asList(
            "/cb - Öffnet das Hauptmenü",
            "/cb help - Zeigt diese Hilfe",
            "/cb info - Plugin-Informationen",
            "/cb menu - Öffnet das Hauptmenü",
            "/cb reload - Lädt das Plugin neu (Admin)",
            "/cb admin - Öffnet das Admin-Menü (Admin)"
        );
        
        for (String helpCommand : helpCommands) {
            plugin.getMessageManager().sendRawMessage(player, "&e" + helpCommand);
        }
        
        plugin.getMessageManager().sendMessage(player, "help.footer");
        plugin.getSoundManager().playClickSound(player);
    }
    
    private void showInfo(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== CBSystem Info ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Version: &e" + plugin.getDescription().getVersion());
        plugin.getMessageManager().sendRawMessage(player, "&7Autor: &e" + plugin.getDescription().getAuthors());
        plugin.getMessageManager().sendRawMessage(player, "&7Website: &e" + plugin.getDescription().getWebsite());
        plugin.getMessageManager().sendRawMessage(player, "&7Entwickelt für: &6OP-Games.de");
        plugin.getMessageManager().sendRawMessage(player, "");
        plugin.getMessageManager().sendRawMessage(player, "&7Online-Spieler: &a" + plugin.getServer().getOnlinePlayers().size());
        plugin.getMessageManager().sendRawMessage(player, "&7Geladene Welten: &a" + plugin.getServer().getWorlds().size());
        
        if (plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getMessageManager().sendRawMessage(player, "&7Economy: &aAktiviert");
        }
        
        if (plugin.getConfigManager().isPlotSquaredEnabled()) {
            plugin.getMessageManager().sendRawMessage(player, "&7PlotSquared: &aIntegriert");
        }
        
        plugin.getSoundManager().playSuccessSound(player);
    }
    
    private void handleReload(Player player) {
        if (!player.hasPermission("cbsystem.admin.reload")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getMessageManager().reloadMessages();
            
            plugin.getMessageManager().sendSuccessMessage(player, "general.plugin-reloaded");
            plugin.getLogger().info("Plugin wurde von " + player.getName() + " neu geladen.");
        } catch (Exception e) {
            plugin.getMessageManager().sendErrorMessage(player, "admin.reload-failed");
            plugin.getLogger().severe("Fehler beim Neu-Laden: " + e.getMessage());
        }
    }
    
    private void handleAdmin(Player player) {
        if (!player.hasPermission("cbsystem.admin.*")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return;
        }
        
        plugin.getGUIManager().openAdminGUI(player);
        plugin.getSoundManager().playOpenSound(player);
    }
    
    private void handleDebug(Player player, String[] args) {
        if (!player.hasPermission("cbsystem.admin.*")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            plugin.getMessageManager().sendRawMessage(player, "&7Debug-Befehle:");
            plugin.getMessageManager().sendRawMessage(player, "&e/cb debug info &7- System-Informationen");
            plugin.getMessageManager().sendRawMessage(player, "&e/cb debug cache &7- Cache-Statistiken");
            plugin.getMessageManager().sendRawMessage(player, "&e/cb debug database &7- Datenbank-Status");
            return;
        }
        
        String debugCommand = args[1].toLowerCase();
        
        switch (debugCommand) {
            case "info" -> showDebugInfo(player);
            case "cache" -> showCacheInfo(player);
            case "database", "db" -> showDatabaseInfo(player);
            default -> plugin.getMessageManager().sendMessage(player, "general.unknown-command");
        }
    }
    
    private void showDebugInfo(Player player) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Debug-Informationen ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Java Version: &e" + System.getProperty("java.version"));
        plugin.getMessageManager().sendRawMessage(player, "&7Speicher verwendet: &e" + (usedMemory / 1024 / 1024) + "MB");
        plugin.getMessageManager().sendRawMessage(player, "&7Speicher gesamt: &e" + (totalMemory / 1024 / 1024) + "MB");
        plugin.getMessageManager().sendRawMessage(player, "&7Threads: &e" + Thread.activeCount());
        plugin.getMessageManager().sendRawMessage(player, "&7Betriebssystem: &e" + System.getProperty("os.name"));
    }
    
    private void showCacheInfo(Player player) {
        int cachedPlayers = plugin.getPlayerDataManager().getCachedPlayerCount();
        int openGUIs = plugin.getGUIManager().getOpenGUICount();
        
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Cache-Statistiken ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Gecachte Spielerdaten: &e" + cachedPlayers);
        plugin.getMessageManager().sendRawMessage(player, "&7Offene GUIs: &e" + openGUIs);
    }
    
    private void showDatabaseInfo(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Datenbank-Status ===");
        
        if (plugin.getDatabaseManager().isDatabaseEnabled()) {
            plugin.getMessageManager().sendRawMessage(player, "&7Status: &aVerbunden");
            plugin.getMessageManager().sendRawMessage(player, "&7Host: &e" + plugin.getConfigManager().getDatabaseHost());
            plugin.getMessageManager().sendRawMessage(player, "&7Datenbank: &e" + plugin.getConfigManager().getDatabaseName());
        } else {
            plugin.getMessageManager().sendRawMessage(player, "&7Status: &cDeaktiviert");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "info", "reload", "menu", "admin", "debug");
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Überprüfe Berechtigungen
                    if (sender instanceof Player player) {
                        if (subCommand.equals("reload") || subCommand.equals("admin") || subCommand.equals("debug")) {
                            if (!player.hasPermission("cbsystem.admin.*")) {
                                continue;
                            }
                        }
                    }
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            List<String> debugCommands = Arrays.asList("info", "cache", "database");
            
            for (String debugCommand : debugCommands) {
                if (debugCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(debugCommand);
                }
            }
        }
        
        return completions;
    }
}
