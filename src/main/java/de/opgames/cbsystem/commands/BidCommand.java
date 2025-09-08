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
import java.util.List;

public class BidCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public BidCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "auction.bid-usage");
            return true;
        }
        
        try {
            int auctionId = Integer.parseInt(args[0]);
            double bidAmount = Double.parseDouble(args[1]);
            
            AuctionItem auction = plugin.getAuctionManager().getAuction(auctionId);
            
            if (auction == null) {
                plugin.getMessageManager().sendMessage(player, "auction.not-found");
                return true;
            }
            
            if (auction.isSold()) {
                plugin.getMessageManager().sendMessage(player, "auction.already-sold");
                return true;
            }
            
            if (auction.isExpired()) {
                plugin.getMessageManager().sendMessage(player, "auction.expired");
                return true;
            }
            
            if (auction.getSeller().equals(player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "auction.cannot-bid-own");
                return true;
            }
            
            // Prüfe ob Gebot höher ist als aktueller Preis
            if (bidAmount <= auction.getPrice()) {
                plugin.getMessageManager().sendMessage(player, "auction.bid-too-low", 
                    "current", plugin.getEconomyManager().formatBalance(auction.getPrice()));
                return true;
            }
            
            // Prüfe ob Spieler genug Geld hat
            if (!plugin.getEconomyManager().hasBalance(player.getUniqueId(), bidAmount)) {
                plugin.getMessageManager().sendMessage(player, "auction.insufficient-funds",
                    "amount", plugin.getEconomyManager().formatBalance(bidAmount));
                return true;
            }
            
            // Gebot abgeben (vereinfacht - direkt kaufen)
            if (plugin.getAuctionManager().buyAuction(player, auctionId)) {
                plugin.getSoundManager().playSuccessSound(player);
                plugin.getMessageManager().sendMessage(player, "auction.bid-success",
                    "item", getItemDisplayName(auction.getItem()),
                    "amount", plugin.getEconomyManager().formatBalance(bidAmount));
            } else {
                plugin.getSoundManager().playErrorSound(player);
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-amount");
        }
        
        return true;
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
            // Zeige verfügbare Auktions-IDs
            List<AuctionItem> auctions = plugin.getAuctionManager().getActiveAuctions();
            for (AuctionItem auction : auctions) {
                completions.add(String.valueOf(auction.getId()));
            }
        } else if (args.length == 2) {
            // Zeige Gebots-Vorschläge
            try {
                int auctionId = Integer.parseInt(args[0]);
                AuctionItem auction = plugin.getAuctionManager().getAuction(auctionId);
                if (auction != null) {
                    double currentPrice = auction.getPrice();
                    completions.add(String.valueOf((int)(currentPrice * 1.1))); // 10% mehr
                    completions.add(String.valueOf((int)(currentPrice * 1.2))); // 20% mehr
                    completions.add(String.valueOf((int)(currentPrice * 1.5))); // 50% mehr
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return completions;
    }
}
