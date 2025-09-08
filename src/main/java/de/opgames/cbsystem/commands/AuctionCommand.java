package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.auction.AuctionItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuctionCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public AuctionCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (args.length == 0) {
            // Auktionshaus GUI öffnen
            plugin.getGUIManager().openAuctionHouse(player);
            plugin.getSoundManager().playOpenSound(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create", "sell" -> handleCreate(player, args);
            case "buy" -> handleBuy(player, args);
            case "cancel" -> handleCancel(player, args);
            case "list", "browse" -> handleList(player, args);
            case "my", "mine" -> handleMyAuctions(player);
            case "search" -> handleSearch(player, args);
            case "info" -> handleInfo(player, args);
            case "help" -> showHelp(player);
            default -> {
                plugin.getMessageManager().sendMessage(player, "auction.unknown-subcommand", "command", subCommand);
                showHelp(player);
            }
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(player, "auction.create-usage");
            return;
        }
        
        try {
            double price = Double.parseDouble(args[2]);
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (item.getType().isAir()) {
                plugin.getMessageManager().sendMessage(player, "auction.no-item-in-hand");
                return;
            }
            
            // Optional: Dauer als 4. Argument
            long duration = 24 * 60 * 60 * 1000; // 24 Stunden Standard
            if (args.length > 3) {
                try {
                    int hours = Integer.parseInt(args[3]);
                    duration = hours * 60 * 60 * 1000;
                } catch (NumberFormatException e) {
                    plugin.getMessageManager().sendMessage(player, "auction.invalid-duration");
                    return;
                }
            }
            
            if (plugin.getAuctionManager().createAuction(player, item, price, duration)) {
                plugin.getSoundManager().playSuccessSound(player);
            } else {
                plugin.getSoundManager().playErrorSound(player);
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-price");
        }
    }
    
    private void handleBuy(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "auction.buy-usage");
            return;
        }
        
        try {
            int auctionId = Integer.parseInt(args[1]);
            
            if (plugin.getAuctionManager().buyAuction(player, auctionId)) {
                plugin.getSoundManager().playSuccessSound(player);
            } else {
                plugin.getSoundManager().playErrorSound(player);
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-id");
        }
    }
    
    private void handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "auction.cancel-usage");
            return;
        }
        
        try {
            int auctionId = Integer.parseInt(args[1]);
            
            if (plugin.getAuctionManager().cancelAuction(player, auctionId)) {
                plugin.getSoundManager().playSuccessSound(player);
            } else {
                plugin.getSoundManager().playErrorSound(player);
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-id");
        }
    }
    
    private void handleList(Player player, String[] args) {
        List<AuctionItem> auctions = plugin.getAuctionManager().getActiveAuctions();
        
        if (auctions.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "auction.no-auctions");
            return;
        }
        
        // Zeige erste 10 Auktionen
        int limit = Math.min(10, auctions.size());
        plugin.getMessageManager().sendMessage(player, "auction.list-header", "count", String.valueOf(auctions.size()));
        
        for (int i = 0; i < limit; i++) {
            AuctionItem auction = auctions.get(i);
            plugin.getMessageManager().sendMessage(player, "auction.list-item",
                "id", String.valueOf(auction.getId()),
                "item", getItemDisplayName(auction.getItem()),
                "price", plugin.getEconomyManager().formatBalance(auction.getPrice()),
                "seller", auction.getSellerName(),
                "time", auction.getFormattedTimeRemaining());
        }
        
        if (auctions.size() > 10) {
            plugin.getMessageManager().sendMessage(player, "auction.list-more", "remaining", String.valueOf(auctions.size() - 10));
        }
    }
    
    private void handleMyAuctions(Player player) {
        List<AuctionItem> myAuctions = plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId());
        
        if (myAuctions.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "auction.no-my-auctions");
            return;
        }
        
        plugin.getMessageManager().sendMessage(player, "auction.my-auctions-header", "count", String.valueOf(myAuctions.size()));
        
        for (AuctionItem auction : myAuctions) {
            plugin.getMessageManager().sendMessage(player, "auction.my-auction-item",
                "id", String.valueOf(auction.getId()),
                "item", getItemDisplayName(auction.getItem()),
                "price", plugin.getEconomyManager().formatBalance(auction.getPrice()),
                "time", auction.getFormattedTimeRemaining());
        }
    }
    
    private void handleSearch(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "auction.search-usage");
            return;
        }
        
        String searchTerm = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
        List<AuctionItem> auctions = plugin.getAuctionManager().getActiveAuctions();
        
        List<AuctionItem> filtered = auctions.stream()
            .filter(auction -> getItemDisplayName(auction.getItem()).toLowerCase().contains(searchTerm) ||
                             auction.getSellerName().toLowerCase().contains(searchTerm))
            .toList();
        
        if (filtered.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "auction.search-no-results", "term", searchTerm);
            return;
        }
        
        plugin.getMessageManager().sendMessage(player, "auction.search-results", 
            "term", searchTerm, "count", String.valueOf(filtered.size()));
        
        int limit = Math.min(5, filtered.size());
        for (int i = 0; i < limit; i++) {
            AuctionItem auction = filtered.get(i);
            plugin.getMessageManager().sendMessage(player, "auction.list-item",
                "id", String.valueOf(auction.getId()),
                "item", getItemDisplayName(auction.getItem()),
                "price", plugin.getEconomyManager().formatBalance(auction.getPrice()),
                "seller", auction.getSellerName(),
                "time", auction.getFormattedTimeRemaining());
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "auction.info-usage");
            return;
        }
        
        try {
            int auctionId = Integer.parseInt(args[1]);
            AuctionItem auction = plugin.getAuctionManager().getAuction(auctionId);
            
            if (auction == null) {
                plugin.getMessageManager().sendMessage(player, "auction.not-found");
                return;
            }
            
            plugin.getMessageManager().sendMessage(player, "auction.info-header", "id", String.valueOf(auctionId));
            plugin.getMessageManager().sendMessage(player, "auction.info-item", "item", getItemDisplayName(auction.getItem()));
            plugin.getMessageManager().sendMessage(player, "auction.info-price", "price", plugin.getEconomyManager().formatBalance(auction.getPrice()));
            plugin.getMessageManager().sendMessage(player, "auction.info-seller", "seller", auction.getSellerName());
            plugin.getMessageManager().sendMessage(player, "auction.info-time", "time", auction.getFormattedTimeRemaining());
            
            if (auction.isSold()) {
                plugin.getMessageManager().sendMessage(player, "auction.info-sold");
            } else if (auction.isExpired()) {
                plugin.getMessageManager().sendMessage(player, "auction.info-expired");
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-id");
        }
    }
    
    private void showHelp(Player player) {
        plugin.getMessageManager().sendMessage(player, "auction.help-header");
        plugin.getMessageManager().sendMessage(player, "auction.help-create");
        plugin.getMessageManager().sendMessage(player, "auction.help-buy");
        plugin.getMessageManager().sendMessage(player, "auction.help-cancel");
        plugin.getMessageManager().sendMessage(player, "auction.help-list");
        plugin.getMessageManager().sendMessage(player, "auction.help-my");
        plugin.getMessageManager().sendMessage(player, "auction.help-search");
        plugin.getMessageManager().sendMessage(player, "auction.help-info");
    }
    
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        return switch (item.getType()) {
            case STONE -> "Stein";
            case DIRT -> "Erde";
            case GRASS_BLOCK -> "Grasblock";
            case DIAMOND -> "Diamant";
            case IRON_INGOT -> "Eisenbarren";
            case GOLD_INGOT -> "Goldbarren";
            default -> item.getType().name().toLowerCase().replace("_", " ");
        };
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "buy", "cancel", "list", "my", "search", "info", "help");
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "buy", "cancel", "info" -> {
                    // Zeige verfügbare Auktions-IDs
                    List<AuctionItem> auctions = plugin.getAuctionManager().getActiveAuctions();
                    for (AuctionItem auction : auctions) {
                        completions.add(String.valueOf(auction.getId()));
                    }
                }
                case "create" -> {
                    // Zeige Preis-Vorschläge
                    completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            // Zeige Dauer-Vorschläge
            completions.addAll(Arrays.asList("1", "6", "12", "24", "48", "72"));
        }
        
        return completions;
    }
}
