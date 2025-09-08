package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoneyCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public MoneyCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cEconomy-System ist deaktiviert!"));
            return true;
        }
        
        if (args.length == 0) {
            // Eigenes Guthaben anzeigen
            if (sender instanceof Player player) {
                showBalance(player, player);
            } else {
                sender.sendMessage("Bitte gib einen Spielernamen an!");
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance", "bal" -> {
                if (sender instanceof Player player) {
                    showBalance(player, player);
                } else {
                    sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
                }
            }
            case "top", "baltop" -> showTopList(sender);
            case "pay" -> handlePay(sender, args);
            case "set" -> handleSet(sender, args);
            case "add", "give" -> handleAdd(sender, args);
            case "remove", "take" -> handleRemove(sender, args);
            case "help" -> showHelp(sender);
            default -> {
                // Spielername als Argument
                Player targetPlayer = plugin.getServer().getPlayer(subCommand);
                if (targetPlayer != null) {
                    if (sender instanceof Player player) {
                        showBalance(player, targetPlayer);
                    } else {
                        showBalanceConsole(sender, targetPlayer);
                    }
                } else {
                    plugin.getMessageManager().sendMessage(sender, "general.player-not-found", 
                        "player", subCommand);
                }
            }
        }
        
        return true;
    }
    
    private void showBalance(Player viewer, Player target) {
        // Debug: Überprüfe ob PlayerDataManager funktioniert
        if (plugin.getPlayerDataManager() == null) {
            plugin.getLogger().warning("PlayerDataManager ist null!");
            viewer.sendMessage("§cFehler: PlayerDataManager nicht initialisiert!");
            return;
        }
        
        // Debug: Überprüfe ob EconomyManager funktioniert
        if (plugin.getEconomyManager() == null) {
            plugin.getLogger().warning("EconomyManager ist null!");
            viewer.sendMessage("§cFehler: EconomyManager nicht initialisiert!");
            return;
        }
        
        // Debug: Überprüfe ob Spielerdaten existieren
        var playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (playerData == null) {
            plugin.getLogger().warning("PlayerData für " + target.getName() + " ist null!");
            viewer.sendMessage("§cFehler: Spielerdaten nicht gefunden! Versuche neu zu laden...");
            
            // Versuche Spielerdaten synchron neu zu laden
            boolean loaded = plugin.getPlayerDataManager().loadPlayerDataSync(target);
            if (loaded) {
                // Daten erfolgreich geladen, zeige Balance
                double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
                plugin.getMessageManager().sendMessage(viewer, "money.balance", 
                    "balance", plugin.getEconomyManager().formatBalance(balance));
                plugin.getSoundManager().playClickSound(viewer);
                return;
            } else {
                viewer.sendMessage("§cFehler: Spielerdaten konnten nicht geladen werden!");
                return;
            }
        }
        
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        
        // Debug: Zeige zusätzliche Informationen
        plugin.getLogger().info("Balance für " + target.getName() + ": " + balance);
        
        if (viewer.equals(target)) {
            plugin.getMessageManager().sendMessage(viewer, "money.balance", 
                "balance", plugin.getEconomyManager().formatBalance(balance));
        } else {
            if (!viewer.hasPermission("cbsystem.money.others")) {
                plugin.getMessageManager().sendErrorMessage(viewer, "general.no-permission");
                return;
            }
            
            plugin.getMessageManager().sendMessage(viewer, "money.balance-other",
                "player", target.getName(),
                "balance", plugin.getEconomyManager().formatBalance(balance));
        }
        
        plugin.getSoundManager().playClickSound(viewer);
    }
    
    private void showBalanceConsole(CommandSender sender, Player target) {
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        sender.sendMessage("Guthaben von " + target.getName() + ": " + 
                          plugin.getEconomyManager().formatBalance(balance));
    }
    
    private void showTopList(CommandSender sender) {
        // Vereinfachte Top-Liste (normalerweise aus Datenbank)
        sender.sendMessage(plugin.getMessageManager().colorize("&6&l=== Reichste Spieler ==="));
        
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        onlinePlayers.sort((p1, p2) -> Double.compare(
            plugin.getEconomyManager().getBalance(p2.getUniqueId()),
            plugin.getEconomyManager().getBalance(p1.getUniqueId())
        ));
        
        int rank = 1;
        for (Player player : onlinePlayers.subList(0, Math.min(10, onlinePlayers.size()))) {
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            sender.sendMessage(plugin.getMessageManager().colorize(
                "&e" + rank + ". &a" + player.getName() + " &7- &6" + 
                plugin.getEconomyManager().formatBalance(balance)));
            rank++;
        }
        
        if (sender instanceof Player player) {
            plugin.getSoundManager().playClickSound(player);
        }
    }
    
    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return;
        }
        
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(player, "money.pay-usage");
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", args[1]);
            return;
        }
        
        if (targetPlayer.equals(player)) {
            plugin.getMessageManager().sendMessage(player, "money.cannot-pay-self");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-number", 
                "input", args[2]);
            return;
        }
        
        if (!plugin.getEconomyManager().isValidPayAmount(amount)) {
            plugin.getMessageManager().sendMessage(player, "money.invalid-amount");
            return;
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
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.money.set")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cVerwendung: /money set <spieler> <betrag>"));
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", 
                "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "general.invalid-number", 
                "input", args[2]);
            return;
        }
        
        if (plugin.getEconomyManager().setBalance(targetPlayer.getUniqueId(), amount, 
                                                 "Admin-Befehl von " + sender.getName())) {
            plugin.getMessageManager().sendMessage(sender, "money.set",
                "player", targetPlayer.getName(),
                "amount", plugin.getEconomyManager().formatBalance(amount));
            
            plugin.getMessageManager().sendMessage(targetPlayer, "money.balance-set",
                "amount", plugin.getEconomyManager().formatBalance(amount));
        }
    }
    
    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.money.add")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cVerwendung: /money add <spieler> <betrag>"));
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", 
                "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "general.invalid-number", 
                "input", args[2]);
            return;
        }
        
        if (plugin.getEconomyManager().addBalance(targetPlayer.getUniqueId(), amount, 
                                                 "Admin-Befehl von " + sender.getName())) {
            plugin.getMessageManager().sendMessage(sender, "money.added",
                "player", targetPlayer.getName(),
                "amount", plugin.getEconomyManager().formatBalance(amount));
            
            plugin.getMessageManager().sendMessage(targetPlayer, "money.received",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "player", sender.getName());
        }
    }
    
    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.money.remove")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cVerwendung: /money remove <spieler> <betrag>"));
            return;
        }
        
        Player targetPlayer = plugin.getServer().getPlayer(args[1]);
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", 
                "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "general.invalid-number", 
                "input", args[2]);
            return;
        }
        
        if (plugin.getEconomyManager().withdrawBalance(targetPlayer.getUniqueId(), amount, 
                                                      "Admin-Befehl von " + sender.getName())) {
            plugin.getMessageManager().sendMessage(sender, "money.removed",
                "player", targetPlayer.getName(),
                "amount", plugin.getEconomyManager().formatBalance(amount));
        } else {
            plugin.getMessageManager().sendMessage(sender, "money.insufficient-target");
        }
    }
    
    private void showHelp(CommandSender sender) {
        List<String> helpCommands = plugin.getMessageManager().getStringList("help.money");
        if (helpCommands.isEmpty()) {
            helpCommands = Arrays.asList(
                "/money - Zeigt dein Guthaben",
                "/money <spieler> - Zeigt Guthaben eines Spielers",
                "/money pay <spieler> <betrag> - Sendet Geld",
                "/money top - Zeigt die reichsten Spieler"
            );
        }
        
        sender.sendMessage(plugin.getMessageManager().colorize("&6&l=== Geld-Befehle ==="));
        for (String helpCommand : helpCommands) {
            sender.sendMessage(plugin.getMessageManager().colorize("&e" + helpCommand));
        }
        
        if (sender instanceof Player player) {
            plugin.getSoundManager().playClickSound(player);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("balance", "pay", "top", "help");
            
            // Admin-Befehle
            if (sender.hasPermission("cbsystem.admin.money.set")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.addAll(Arrays.asList("set", "add", "remove"));
            }
            
            // Online-Spieler
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(onlinePlayer.getName());
                }
            }
            
            // Subcommands
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("pay") || subCommand.equals("set") || 
                subCommand.equals("add") || subCommand.equals("remove")) {
                // Online-Spieler vorschlagen
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
