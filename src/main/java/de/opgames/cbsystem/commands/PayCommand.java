package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PayCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public PayCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getConfigManager().isEconomyEnabled() || !plugin.getConfigManager().isPayEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (!player.hasPermission("cbsystem.money.pay")) {
            plugin.getMessageManager().sendErrorMessage(player, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "money.pay-usage");
            return true;
        }
        
        // Prüfe ob "*" für alle online Spieler verwendet wird
        if (args[0].equals("*")) {
            return handlePayAll(player, args);
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", args[0]);
            return true;
        }
        
        if (targetPlayer.equals(player)) {
            plugin.getMessageManager().sendMessage(player, "money.cannot-pay-self");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-number", 
                "input", args[1]);
            return true;
        }
        
        if (!plugin.getEconomyManager().isValidPayAmount(amount)) {
            plugin.getMessageManager().sendMessage(player, "money.invalid-amount");
            return true;
        }
        
        if (plugin.getEconomyManager().transferMoney(player.getUniqueId(), targetPlayer.getUniqueId(), 
                                                   amount, "Spieler-Transfer")) {
            plugin.getMessageManager().sendSuccessMessage(player, "money.sent",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "player", targetPlayer.getName());
            
            plugin.getMessageManager().sendMessage(targetPlayer, "money.received",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "player", player.getName());
            
            plugin.getSoundManager().playMoneySendSound(player);
            plugin.getSoundManager().playMoneyReceiveSound(targetPlayer);
        } else {
            plugin.getMessageManager().sendErrorMessage(player, "money.insufficient");
        }
        
        return true;
    }
    
    private boolean handlePayAll(Player player, String[] args) {
        // Prüfe spezielle Berechtigung für Pay-All
        if (!player.hasPermission("cbsystem.money.pay.all")) {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-no-permission");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-number", 
                "input", args[1]);
            return true;
        }
        
        if (!plugin.getEconomyManager().isValidPayAmount(amount)) {
            plugin.getMessageManager().sendMessage(player, "money.invalid-amount");
            return true;
        }
        
        // Alle online Spieler außer dem Sender
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        onlinePlayers.removeIf(p -> p.equals(player));
        
        if (onlinePlayers.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-no-players");
            return true;
        }
        
        // Berechne Gesamtbetrag
        double totalAmount = amount * onlinePlayers.size();
        double currentBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        
        if (currentBalance < totalAmount) {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-insufficient",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "total", plugin.getEconomyManager().formatBalance(totalAmount),
                "balance", plugin.getEconomyManager().formatBalance(currentBalance),
                "players", String.valueOf(onlinePlayers.size()));
            return true;
        }
        
        // Bestätigung für große Beträge
        if (totalAmount > 10000) {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-confirmation",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "total", plugin.getEconomyManager().formatBalance(totalAmount),
                "players", String.valueOf(onlinePlayers.size()));
            return true;
        }
        
        // Geld an alle Spieler senden
        int successCount = 0;
        for (Player targetPlayer : onlinePlayers) {
            if (plugin.getEconomyManager().transferMoney(player.getUniqueId(), targetPlayer.getUniqueId(), 
                                                       amount, "Pay-All: " + player.getName())) {
                successCount++;
                
                // Benachrichtige Empfänger
                plugin.getMessageManager().sendMessage(targetPlayer, "money.received-from-all",
                    "amount", plugin.getEconomyManager().formatBalance(amount),
                    "player", player.getName());
                
                plugin.getSoundManager().playMoneyReceiveSound(targetPlayer);
            }
        }
        
        // Benachrichtige Sender
        if (successCount > 0) {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-success",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "count", String.valueOf(successCount),
                "total", plugin.getEconomyManager().formatBalance(amount * successCount));
            
            plugin.getSoundManager().playMoneySendSound(player);
            
            // Log für Admins
            plugin.getLogger().info(player.getName() + " sent " + 
                plugin.getEconomyManager().formatBalance(amount) + " to " + successCount + " players " +
                "(Total: " + plugin.getEconomyManager().formatBalance(amount * successCount) + ")");
        } else {
            plugin.getMessageManager().sendMessage(player, "money.pay-all-failed");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            // "*" für alle Spieler vorschlagen (wenn Berechtigung vorhanden)
            if (player.hasPermission("cbsystem.money.pay.all") && 
                "*".startsWith(args[0].toLowerCase())) {
                completions.add("*");
            }
            
            // Online-Spieler vorschlagen (außer sich selbst)
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (!onlinePlayer.equals(player) && 
                    onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
        } else if (args.length == 2) {
            // Beispiel-Beträge vorschlagen
            List<String> amounts = List.of("100", "500", "1000", "5000", "10000");
            for (String amount : amounts) {
                if (amount.startsWith(args[1])) {
                    completions.add(amount);
                }
            }
        }
        
        return completions;
    }
}
