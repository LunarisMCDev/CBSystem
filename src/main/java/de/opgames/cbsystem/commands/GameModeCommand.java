package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameModeCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public GameModeCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.gamemode")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "admin.gamemode-usage");
            return true;
        }
        
        GameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            plugin.getMessageManager().sendMessage(player, "admin.invalid-gamemode", "mode", args[0]);
            return true;
        }
        
        Player targetPlayer = player;
        
        // Anderen Spieler als Ziel
        if (args.length > 1) {
            if (!player.hasPermission("cbsystem.gamemode.others")) {
                plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
                return true;
            }
            
            targetPlayer = plugin.getServer().getPlayer(args[1]);
            if (targetPlayer == null) {
                plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                    "player", args[1]);
                return true;
            }
        }
        
        // Spielmodus setzen
        targetPlayer.setGameMode(gameMode);
        
        String gameModeName = getGameModeName(gameMode);
        
        if (targetPlayer.equals(player)) {
            plugin.getMessageManager().sendSuccessMessage(player, "admin.gamemode-changed", 
                "gamemode", gameModeName);
        } else {
            plugin.getMessageManager().sendMessage(player, "admin.gamemode-changed-other",
                "player", targetPlayer.getName(),
                "gamemode", gameModeName);
            plugin.getMessageManager().sendMessage(targetPlayer, "admin.gamemode-changed", 
                "gamemode", gameModeName);
        }
        
        return true;
    }
    
    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "survival", "s" -> GameMode.SURVIVAL;
            case "1", "creative", "c" -> GameMode.CREATIVE;
            case "2", "adventure", "a" -> GameMode.ADVENTURE;
            case "3", "spectator", "sp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
    
    private String getGameModeName(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> gameModes = Arrays.asList("survival", "creative", "adventure", "spectator", "0", "1", "2", "3");
            for (String gameMode : gameModes) {
                if (gameMode.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(gameMode);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("cbsystem.gamemode.others")) {
            // Online-Spieler vorschlagen
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }
        
        return completions;
    }
}
