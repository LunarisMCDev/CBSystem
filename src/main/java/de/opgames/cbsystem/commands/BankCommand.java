package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.bank.BankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    private final BankManager bankManager;
    
    public BankCommand(CBSystem plugin) {
        this.plugin = plugin;
        this.bankManager = new BankManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().colorize("&cNur Spieler können Bank-Befehle verwenden!"));
            return true;
        }
        
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getMessageManager().sendMessage(player, "general.economy-disabled");
            return true;
        }
        
        if (args.length == 0) {
            // Bank-GUI öffnen
            plugin.getGUIManager().openBankGUI(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance", "bal" -> handleBalance(player);
            case "deposit", "dep" -> handleDeposit(player, args);
            case "withdraw", "with" -> handleWithdraw(player, args);
            case "loan" -> handleLoan(player, args);
            case "payloan" -> handlePayLoan(player, args);
            case "info" -> handleInfo(player);
            case "help" -> showHelp(player);
            default -> {
                plugin.getMessageManager().sendMessage(player, "bank.invalid-subcommand", "command", subCommand);
                showHelp(player);
            }
        }
        
        return true;
    }
    
    private void handleBalance(Player player) {
        double bankBalance = bankManager.getBankBalance(player.getUniqueId());
        double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        
        plugin.getMessageManager().sendMessage(player, "bank.balance-info",
            "bank-balance", plugin.getEconomyManager().formatBalance(bankBalance),
            "pocket-balance", plugin.getEconomyManager().formatBalance(pocketBalance));
    }
    
    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "bank.deposit-usage");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        if (amount <= 0) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (pocketBalance < amount) {
            plugin.getMessageManager().sendMessage(player, "bank.insufficient-pocket-balance",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "balance", plugin.getEconomyManager().formatBalance(pocketBalance));
            return;
        }
        
        if (bankManager.deposit(player.getUniqueId(), amount)) {
            plugin.getMessageManager().sendMessage(player, "bank.deposit-success",
                "amount", plugin.getEconomyManager().formatBalance(amount));
            plugin.getSoundManager().playSound(player, "money.receive");
        } else {
            plugin.getMessageManager().sendMessage(player, "bank.deposit-failed");
        }
    }
    
    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "bank.withdraw-usage");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        if (amount <= 0) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        double bankBalance = bankManager.getBankBalance(player.getUniqueId());
        if (bankBalance < amount) {
            plugin.getMessageManager().sendMessage(player, "bank.insufficient-bank-balance",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "balance", plugin.getEconomyManager().formatBalance(bankBalance));
            return;
        }
        
        if (bankManager.withdraw(player.getUniqueId(), amount)) {
            plugin.getMessageManager().sendMessage(player, "bank.withdraw-success",
                "amount", plugin.getEconomyManager().formatBalance(amount));
            plugin.getSoundManager().playSound(player, "money.receive");
        } else {
            plugin.getMessageManager().sendMessage(player, "bank.withdraw-failed");
        }
    }
    
    private void handleLoan(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "bank.loan-usage");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        if (amount <= 0) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        int days = 7; // Standard: 7 Tage
        if (args.length >= 3) {
            try {
                days = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendMessage(player, "general.invalid-number");
                return;
            }
        }
        
        if (bankManager.hasActiveLoan(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "bank.already-has-loan");
            return;
        }
        
        if (amount > 100000.0) {
            plugin.getMessageManager().sendMessage(player, "bank.loan-too-large",
                "max-amount", plugin.getEconomyManager().formatBalance(100000.0));
            return;
        }
        
        if (days > 7) {
            plugin.getMessageManager().sendMessage(player, "bank.loan-too-long",
                "max-days", "7");
            return;
        }
        
        if (bankManager.grantLoan(player.getUniqueId(), amount, days)) {
            plugin.getMessageManager().sendMessage(player, "bank.loan-granted",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "days", String.valueOf(days));
            plugin.getSoundManager().playSound(player, "money.receive");
        } else {
            plugin.getMessageManager().sendMessage(player, "bank.loan-failed");
        }
    }
    
    private void handlePayLoan(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "bank.payloan-usage");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        if (amount <= 0) {
            plugin.getMessageManager().sendMessage(player, "general.invalid-amount");
            return;
        }
        
        if (!bankManager.hasActiveLoan(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "bank.no-active-loan");
            return;
        }
        
        double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (pocketBalance < amount) {
            plugin.getMessageManager().sendMessage(player, "bank.insufficient-pocket-balance",
                "amount", plugin.getEconomyManager().formatBalance(amount),
                "balance", plugin.getEconomyManager().formatBalance(pocketBalance));
            return;
        }
        
        if (bankManager.payLoan(player.getUniqueId(), amount)) {
            plugin.getMessageManager().sendMessage(player, "bank.payloan-success",
                "amount", plugin.getEconomyManager().formatBalance(amount));
            plugin.getSoundManager().playSound(player, "money.send");
        } else {
            plugin.getMessageManager().sendMessage(player, "bank.payloan-failed");
        }
    }
    
    private void handleInfo(Player player) {
        double bankBalance = bankManager.getBankBalance(player.getUniqueId());
        double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        
        plugin.getMessageManager().sendMessage(player, "bank.info-header");
        plugin.getMessageManager().sendMessage(player, "bank.balance-info",
            "bank-balance", plugin.getEconomyManager().formatBalance(bankBalance),
            "pocket-balance", plugin.getEconomyManager().formatBalance(pocketBalance));
        
        BankManager.LoanInfo loanInfo = bankManager.getLoanInfo(player.getUniqueId());
        if (loanInfo != null) {
            plugin.getMessageManager().sendMessage(player, "bank.loan-info",
                "remaining", plugin.getEconomyManager().formatBalance(loanInfo.getRemainingAmount()),
                "original", plugin.getEconomyManager().formatBalance(loanInfo.getOriginalAmount()),
                "due-date", loanInfo.getDueDate().toString());
        } else {
            plugin.getMessageManager().sendMessage(player, "bank.no-loan");
        }
    }
    
    private void showHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "bank.help-header");
        plugin.getMessageManager().sendMessage(player, "bank.help-balance");
        plugin.getMessageManager().sendMessage(player, "bank.help-deposit");
        plugin.getMessageManager().sendMessage(player, "bank.help-withdraw");
        plugin.getMessageManager().sendMessage(player, "bank.help-loan");
        plugin.getMessageManager().sendMessage(player, "bank.help-payloan");
        plugin.getMessageManager().sendMessage(player, "bank.help-info");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("balance", "deposit", "withdraw", "loan", "payloan", "info", "help");
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("deposit") || subCommand.equals("withdraw") || 
                subCommand.equals("loan") || subCommand.equals("payloan")) {
                return Arrays.asList("100", "1000", "10000", "50000", "100000");
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("loan")) {
            return Arrays.asList("1", "3", "5", "7");
        }
        
        return new ArrayList<>();
    }
}
