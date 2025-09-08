package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EcoCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public EcoCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.economy")) {
            plugin.getMessageManager().sendMessage(sender, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give", "add" -> handleGive(sender, args);
            case "take", "remove" -> handleTake(sender, args);
            case "set" -> handleSet(sender, args);
            case "reset", "clear" -> handleReset(sender, args);
            case "balance", "bal" -> handleBalance(sender, args);
            case "top", "baltop" -> handleTop(sender, args);
            case "reload" -> handleReload(sender);
            case "help" -> showHelp(sender);
            default -> {
                plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-subcommand", "command", subCommand);
                showHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.give-usage");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
                return;
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
            return;
        }
        
        // Geld hinzufügen
        plugin.getEconomyManager().addBalance(target.getUniqueId(), amount, 
            "Admin-Befehl: Geld hinzugefügt von " + sender.getName());
        
        double newBalance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        
        plugin.getMessageManager().sendMessage(sender, "money.eco.gave-money",
            "player", target.getName(),
            "amount", plugin.getEconomyManager().formatBalance(amount),
            "balance", plugin.getEconomyManager().formatBalance(newBalance));
        
        // Ziel-Spieler benachrichtigen (wenn online)
        if (target.isOnline()) {
            plugin.getMessageManager().sendMessage((Player) target, "money.eco.received-money",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "admin", sender.getName());
        }
        
        plugin.getLogger().info(sender.getName() + " gab " + target.getName() + " " + 
            plugin.getEconomyManager().formatBalance(amount) + " (Neuer Kontostand: " + 
            plugin.getEconomyManager().formatBalance(newBalance) + ")");
    }
    
    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.take-usage");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
                return;
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
            return;
        }
        
        double currentBalance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        if (currentBalance < amount) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.insufficient-funds",
                "player", target.getName(),
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "balance", plugin.getEconomyManager().formatBalance(currentBalance));
            return;
        }
        
        // Geld entziehen
        plugin.getEconomyManager().withdrawBalance(target.getUniqueId(), amount, 
            "Admin-Befehl: Geld entzogen von " + sender.getName());
        
        double newBalance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        
        plugin.getMessageManager().sendMessage(sender, "money.eco.took-money",
            "player", target.getName(),
            "amount", plugin.getEconomyManager().formatBalance(amount),
            "balance", plugin.getEconomyManager().formatBalance(newBalance));
        
        // Ziel-Spieler benachrichtigen (wenn online)
        if (target.isOnline()) {
            plugin.getMessageManager().sendMessage((Player) target, "money.eco.money-taken",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "admin", sender.getName());
        }
        
        plugin.getLogger().info(sender.getName() + " nahm " + target.getName() + " " + 
            plugin.getEconomyManager().formatBalance(amount) + " (Neuer Kontostand: " + 
            plugin.getEconomyManager().formatBalance(newBalance) + ")");
    }
    
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.set-usage");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[1]);
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
                return;
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.invalid-amount", "amount", args[2]);
            return;
        }
        
        // Geld setzen
        plugin.getEconomyManager().setBalance(target.getUniqueId(), amount, 
            "Admin-Befehl: Kontostand gesetzt von " + sender.getName());
        
        plugin.getMessageManager().sendMessage(sender, "money.set-money",
            "player", target.getName(),
            "amount", plugin.getEconomyManager().formatBalance(amount));
        
        // Ziel-Spieler benachrichtigen (wenn online)
        if (target.isOnline()) {
            plugin.getMessageManager().sendMessage((Player) target, "money.eco.balance-set",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "admin", sender.getName());
        }
        
        plugin.getLogger().info(sender.getName() + " setzte " + target.getName() + "s Kontostand auf " + 
            plugin.getEconomyManager().formatBalance(amount));
    }
    
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "money.eco.reset-usage");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[1]);
            return;
        }
        
        double startingBalance = plugin.getConfigManager().getStartingBalance();
        
        // Kontostand zurücksetzen
        plugin.getEconomyManager().setBalance(target.getUniqueId(), startingBalance, 
            "Admin-Befehl: Kontostand zurückgesetzt von " + sender.getName());
        
        plugin.getMessageManager().sendMessage(sender, "money.eco.reset-money",
            "player", target.getName(),
            "amount", plugin.getEconomyManager().formatBalance(startingBalance));
        
        // Ziel-Spieler benachrichtigen (wenn online)
        if (target.isOnline()) {
            plugin.getMessageManager().sendMessage((Player) target, "money.eco.balance-reset",
                "amount", plugin.getEconomyManager().formatBalance(startingBalance),
                "admin", sender.getName());
        }
        
        plugin.getLogger().info(sender.getName() + " setzte " + target.getName() + "s Kontostand zurück auf " + 
            plugin.getEconomyManager().formatBalance(startingBalance));
    }
    
    private void handleBalance(CommandSender sender, String[] args) {
        OfflinePlayer target;
        
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                plugin.getMessageManager().sendMessage(sender, "general.player-not-found", "player", args[1]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            plugin.getMessageManager().sendMessage(sender, "money.eco.balance-usage");
            return;
        }
        
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId());
        
        if (target.getUniqueId().equals(((Player) sender).getUniqueId())) {
            plugin.getMessageManager().sendMessage(sender, "money.balance",
                "balance", plugin.getEconomyManager().formatBalance(balance));
        } else {
            plugin.getMessageManager().sendMessage(sender, "money.balance-other",
                "player", target.getName(),
                "balance", plugin.getEconomyManager().formatBalance(balance));
        }
    }
    
    private void handleTop(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "money.eco.top-coming-soon");
        // TODO: Implementiere Top-Balance Liste aus der Datenbank
    }
    
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfig();
        plugin.getMessageManager().sendMessage(sender, "money.eco.config-reloaded");
    }
    
    private void showHelp(CommandSender sender) {
        plugin.getMessageManager().sendRawMessage(sender, "&6&l=== Economy Befehle ===");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco give <spieler> <betrag> &7- Gib Spieler Geld");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco take <spieler> <betrag> &7- Entziehe Spieler Geld");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco set <spieler> <betrag> &7- Setze Spieler Guthaben");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco reset <spieler> &7- Setze Guthaben zurück");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco balance [spieler] &7- Zeige Guthaben");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco top &7- Zeige reichste Spieler");
        plugin.getMessageManager().sendRawMessage(sender, "&e/eco reload &7- Config neu laden");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cbsystem.admin.economy")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("give", "take", "set", "reset", "balance", "top", "reload", "help");
            return subCommands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("help") && !args[0].equalsIgnoreCase("top")) {
            // Spielernamen vorschlagen
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take") || args[0].equalsIgnoreCase("set"))) {
            // Betrags-Vorschläge
            return Arrays.asList("100", "1000", "10000", "100000");
        }
        
        return new ArrayList<>();
    }
}
