package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.auction.AuctionItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CancelAuctionCommand implements CommandExecutor, TabCompleter {
    
    private final CBSystem plugin;
    
    public CancelAuctionCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "auction.cancel-usage");
            return true;
        }
        
        try {
            int auctionId = Integer.parseInt(args[0]);
            
            if (plugin.getAuctionManager().cancelAuction(player, auctionId)) {
                plugin.getSoundManager().playSuccessSound(player);
            } else {
                plugin.getSoundManager().playErrorSound(player);
            }
            
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(player, "auction.invalid-id");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1 && sender instanceof Player player) {
            // Zeige nur die eigenen Auktionen
            List<AuctionItem> myAuctions = plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId());
            for (AuctionItem auction : myAuctions) {
                completions.add(String.valueOf(auction.getId()));
            }
        }
        
        return completions;
    }
}
