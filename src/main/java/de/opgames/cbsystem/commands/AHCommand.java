package de.opgames.cbsystem.commands;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AHCommand implements CommandExecutor {
    
    private final CBSystem plugin;
    
    public AHCommand(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        // Kurzer Alias für /auction - öffnet direkt das Auktionshaus GUI
        plugin.getGUIManager().openAuctionHouse(player);
        plugin.getSoundManager().playOpenSound(player);
        
        return true;
    }
}
