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

public class ScoreboardCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public ScoreboardCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cNur Spieler können Scoreboard-Befehle verwenden!"));
            return true;
        }
        
        if (args.length == 0) {
            // Scoreboard umschalten
            plugin.getScoreboardManager().toggleScoreboard(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "toggle", "on", "off" -> handleToggle(player, subCommand);
            case "reload" -> handleReload(player);
            case "help" -> showHelp(player);
            default -> {
                plugin.getMessageManager().sendMessage(player, "scoreboard.invalid-subcommand", "command", subCommand);
                showHelp(player);
            }
        }
        
        return true;
    }
    
    private void handleToggle(Player player, String subCommand) {
        switch (subCommand) {
            case "toggle" -> {
                plugin.getScoreboardManager().toggleScoreboard(player);
            }
            case "on" -> {
                if (plugin.getScoreboardManager().hasScoreboard(player)) {
                    plugin.getMessageManager().sendMessage(player, "scoreboard.already-enabled");
                } else {
                    plugin.getScoreboardManager().createScoreboard(player);
                    plugin.getMessageManager().sendMessage(player, "scoreboard.enabled");
                }
            }
            case "off" -> {
                if (!plugin.getScoreboardManager().hasScoreboard(player)) {
                    plugin.getMessageManager().sendMessage(player, "scoreboard.already-disabled");
                } else {
                    plugin.getScoreboardManager().removeScoreboard(player);
                    plugin.getMessageManager().sendMessage(player, "scoreboard.disabled");
                }
            }
        }
    }
    
    private void handleReload(Player player) {
        if (!player.hasPermission("cbsystem.admin.scoreboard")) {
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return;
        }
        
        // Alle Scoreboards neu laden
        plugin.getScoreboardManager().updateAllScoreboards();
        plugin.getMessageManager().sendMessage(player, "scoreboard.reloaded");
    }
    
    private void showHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "scoreboard.help-header");
        plugin.getMessageManager().sendMessage(player, "scoreboard.help-toggle");
        plugin.getMessageManager().sendMessage(player, "scoreboard.help-on");
        plugin.getMessageManager().sendMessage(player, "scoreboard.help-off");
        
        if (player.hasPermission("cbsystem.admin.scoreboard")) {
            plugin.getMessageManager().sendMessage(player, "scoreboard.help-reload");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("toggle", "on", "off", "help"));
            
            if (sender.hasPermission("cbsystem.admin.scoreboard")) {
                completions.add("reload");
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
