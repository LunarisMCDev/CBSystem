package de.opgames.cbsystem.commands;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlotCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public PlotCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (!plugin.getPlotManager().isEnabled()) {
            plugin.getMessageManager().sendErrorMessage(player, "general.feature-disabled");
            return true;
        }
        
        if (args.length == 0) {
            // Plot-Management-GUI öffnen
            Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
            if (currentPlot != null) {
                plugin.getGUIManager().openPlotManagementGUI(player, currentPlot);
            } else {
                plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "info", "i" -> handleInfo(player);
            case "home", "h" -> handleHome(player);
            case "trust", "add" -> handleTrust(player, args);
            case "untrust", "remove", "deny" -> handleUntrust(player, args);
            case "buy", "claim" -> handleBuy(player);
            case "delete", "del" -> handleDelete(player);
            case "list", "l" -> handleList(player);
            case "help" -> showHelp(player);
            default -> {
                plugin.getMessageManager().sendMessage(player, "general.unknown-command");
                showHelp(player);
            }
        }
        
        return true;
    }
    
    private void handleInfo(Player player) {
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null) {
            plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            return;
        }
        
        plugin.getGUIManager().openPlotInfoGUI(player, currentPlot);
        plugin.getSoundManager().playOpenSound(player);
    }
    
    private void handleHome(Player player) {
        List<Plot> playerPlots = plugin.getPlotManager().getPlayerPlots(player.getUniqueId());
        
        if (playerPlots.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "plot.no-plots");
            return;
        }
        
        // Teleportiere zum ersten Plot (oder öffne Auswahl-GUI bei mehreren)
        Plot firstPlot = playerPlots.get(0);
        plugin.getPlotManager().teleportToPlotHome(player, firstPlot);
    }
    
    private void handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "plot.trust-usage");
            return;
        }
        
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null) {
            plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            return;
        }
        
        if (!plugin.getPlotManager().isPlotOwner(currentPlot, player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "plot.not-owner");
            return;
        }
        
        String targetPlayerName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        
        if (targetPlayer == null) {
            plugin.getMessageManager().sendMessage(player, "general.player-not-found", 
                "player", targetPlayerName);
            return;
        }
        
        if (targetPlayer.equals(player)) {
            plugin.getMessageManager().sendMessage(player, "plot.cannot-trust-self");
            return;
        }
        
        if (plugin.getPlotManager().addPlayerToPlot(currentPlot, targetPlayer.getUniqueId())) {
            plugin.getMessageManager().sendSuccessMessage(player, "plot.player-trusted", 
                "player", targetPlayer.getName());
            plugin.getMessageManager().sendMessage(targetPlayer, "plot.added-to-plot", 
                "owner", player.getName());
        } else {
            plugin.getMessageManager().sendErrorMessage(player, "plot.already-trusted");
        }
    }
    
    private void handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "plot.untrust-usage");
            return;
        }
        
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null) {
            plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            return;
        }
        
        if (!plugin.getPlotManager().isPlotOwner(currentPlot, player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "plot.not-owner");
            return;
        }
        
        String targetPlayerName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        
        if (targetPlayer == null) {
            // Versuche offline Spieler zu finden
            plugin.getMessageManager().sendMessage(player, "general.player-not-found", 
                "player", targetPlayerName);
            return;
        }
        
        if (plugin.getPlotManager().removePlayerFromPlot(currentPlot, targetPlayer.getUniqueId())) {
            plugin.getMessageManager().sendSuccessMessage(player, "plot.player-untrusted", 
                "player", targetPlayer.getName());
            plugin.getMessageManager().sendMessage(targetPlayer, "plot.removed-from-plot", 
                "owner", player.getName());
        } else {
            plugin.getMessageManager().sendErrorMessage(player, "plot.not-trusted-player");
        }
    }
    
    private void handleBuy(Player player) {
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null) {
            plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            return;
        }
        
        if (currentPlot.hasOwner()) {
            plugin.getMessageManager().sendMessage(player, "plot.already-claimed");
            return;
        }
        
        if (!plugin.getPlotManager().canPlayerClaimPlot(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "plot.max-plots-reached");
            return;
        }
        
        double price = plugin.getPlotManager().calculatePlotPrice(player.getUniqueId());
        
        // Bestätigungs-GUI öffnen
        plugin.getGUIManager().openConfirmationGUI(player,
            "&aPlot kaufen",
            "&7Möchtest du diesen Plot für &a" + plugin.getEconomyManager().formatBalance(price) + " &7kaufen?",
            () -> {
                if (plugin.getPlotManager().purchasePlot(player, currentPlot)) {
                    plugin.getSoundManager().playPlotClaimSound(player);
                }
            },
            () -> plugin.getMessageManager().sendMessage(player, "plot.purchase-cancelled")
        );
    }
    
    private void handleDelete(Player player) {
        Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
        
        if (currentPlot == null) {
            plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
            return;
        }
        
        if (!plugin.getPlotManager().isPlotOwner(currentPlot, player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "plot.not-owner");
            return;
        }
        
        // Bestätigungs-GUI öffnen
        plugin.getGUIManager().openConfirmationGUI(player,
            "&cPlot löschen",
            "&7Bist du sicher, dass du dein Plot unwiderruflich löschen möchtest?",
            () -> {
                // Hier würde das Plot gelöscht werden
                plugin.getMessageManager().sendMessage(player, "plot.delete-not-implemented");
            },
            () -> plugin.getMessageManager().sendMessage(player, "plot.delete-cancelled")
        );
    }
    
    private void handleList(Player player) {
        List<Plot> playerPlots = plugin.getPlotManager().getPlayerPlots(player.getUniqueId());
        
        if (playerPlots.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "plot.no-plots");
            return;
        }
        
        plugin.getMessageManager().sendMessage(player, "plot.list-header");
        
        for (int i = 0; i < playerPlots.size(); i++) {
            Plot plot = playerPlots.get(i);
            plugin.getMessageManager().sendRawMessage(player, 
                "&e" + (i + 1) + ". &7Plot &e" + plot.getId() + " &7in &a" + plot.getWorldName());
        }
        
        plugin.getMessageManager().sendRawMessage(player, 
            "&7Gesamt: &e" + playerPlots.size() + "&7/&e" + plugin.getPlotManager().getMaxPlots(player.getUniqueId()));
        
        plugin.getSoundManager().playClickSound(player);
    }
    
    private void showHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "help.header");
        
        List<String> helpCommands = plugin.getMessageManager().getStringList("help.plot");
        if (helpCommands.isEmpty()) {
            helpCommands = Arrays.asList(
                "/plot - Öffnet das Plot-Menü",
                "/plot info - Zeigt Plot-Informationen",
                "/plot home - Teleportiert zu deinem Plot",
                "/plot trust <spieler> - Vertraut einem Spieler",
                "/plot untrust <spieler> - Entfernt Vertrauen",
                "/plot buy - Kauft den aktuellen Plot",
                "/plot list - Zeigt deine Plots an"
            );
        }
        
        for (String helpCommand : helpCommands) {
            plugin.getMessageManager().sendRawMessage(player, "&e" + helpCommand);
        }
        
        plugin.getSoundManager().playClickSound(player);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "info", "home", "trust", "untrust", "buy", "delete", "list", "help"
            );
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("trust") || subCommand.equals("untrust") || 
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
