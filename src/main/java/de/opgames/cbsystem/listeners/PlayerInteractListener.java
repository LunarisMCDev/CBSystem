package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlayerInteractListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Spezielle Items behandeln
        handleSpecialItems(event, player, item);
        
        // Rechtsklick-Aktionen
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleRightClickActions(event, player, item);
        }
    }
    
    private void handleSpecialItems(PlayerInteractEvent event, Player player, ItemStack item) {
        // GUI-Items (falls Spieler welche im Inventar hat)
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            
            // CBSystem Hauptmenü-Item
            if (displayName.contains("CBSystem") || displayName.contains("Hauptmenü")) {
                event.setCancelled(true);
                plugin.getGUIManager().openMainMenu(player);
                plugin.getSoundManager().playOpenSound(player);
                return;
            }
            
            // Shop-Item
            if (displayName.contains("Shop")) {
                event.setCancelled(true);
                plugin.getGUIManager().openShopGUI(player);
                plugin.getSoundManager().playOpenSound(player);
                return;
            }
            
            // Plot-Item
            if (displayName.contains("Plot")) {
                event.setCancelled(true);
                com.plotsquared.core.plot.Plot currentPlot = plugin.getPlotManager().getCurrentPlot(player);
                if (currentPlot != null) {
                    plugin.getGUIManager().openPlotManagementGUI(player, currentPlot);
                } else {
                    plugin.getMessageManager().sendMessage(player, "plot.not-in-plot");
                }
                plugin.getSoundManager().playOpenSound(player);
                return;
            }
        }
    }
    
    private void handleRightClickActions(PlayerInteractEvent event, Player player, ItemStack item) {
        Material material = item.getType();
        
        // Kompass öffnet Hauptmenü
        if (material == Material.COMPASS) {
            if (player.hasPermission("cbsystem.compass.use")) {
                event.setCancelled(true);
                plugin.getGUIManager().openMainMenu(player);
                plugin.getSoundManager().playOpenSound(player);
            }
            return;
        }
        
        // Uhr zeigt Server-Info
        if (material == Material.CLOCK) {
            if (player.hasPermission("cbsystem.clock.use")) {
                event.setCancelled(true);
                showServerInfo(player);
            }
            return;
        }
        
        // Buch öffnet Hilfe
        if (material == Material.BOOK) {
            if (player.hasPermission("cbsystem.help.use")) {
                event.setCancelled(true);
                showHelpInfo(player);
            }
            return;
        }
        
        // Enderperle für schnelle Teleportation
        if (material == Material.ENDER_PEARL) {
            if (!player.hasPermission("cbsystem.enderpearl.use")) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "general.no-permission");
                plugin.getSoundManager().playErrorSound(player);
            }
            return;
        }
        
        // Feuerwerk für Effekte (nur mit Berechtigung)
        if (material == Material.FIREWORK_ROCKET) {
            if (!player.hasPermission("cbsystem.firework.use")) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "general.no-permission");
                plugin.getSoundManager().playErrorSound(player);
            }
            return;
        }
    }
    
    private void showServerInfo(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Server-Informationen ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Online-Spieler: &a" + plugin.getServer().getOnlinePlayers().size());
        plugin.getMessageManager().sendRawMessage(player, "&7Aktuelle Zeit: &e" + java.time.LocalTime.now().toString().substring(0, 8));
        plugin.getMessageManager().sendRawMessage(player, "&7Server-Version: &e" + plugin.getServer().getVersion());
        
        if (plugin.getConfigManager().isEconomyEnabled()) {
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            plugin.getMessageManager().sendRawMessage(player, "&7Dein Guthaben: &a" + 
                plugin.getEconomyManager().formatBalance(balance));
        }
        
        if (plugin.getPlotManager().isEnabled()) {
            int plotCount = plugin.getPlotManager().getPlotCount(player.getUniqueId());
            int maxPlots = plugin.getPlotManager().getMaxPlots(player.getUniqueId());
            plugin.getMessageManager().sendRawMessage(player, "&7Deine Plots: &e" + plotCount + "&7/&e" + maxPlots);
        }
        
        plugin.getSoundManager().playClickSound(player);
    }
    
    private void showHelpInfo(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Schnellhilfe ===");
        plugin.getMessageManager().sendRawMessage(player, "&e/cb &7- Hauptmenü öffnen");
        plugin.getMessageManager().sendRawMessage(player, "&e/plot &7- Plot-Verwaltung");
        plugin.getMessageManager().sendRawMessage(player, "&e/home &7- Zu Home teleportieren");
        plugin.getMessageManager().sendRawMessage(player, "&e/shop &7- Shop öffnen");
        plugin.getMessageManager().sendRawMessage(player, "&e/spawn &7- Zum Spawn");
        plugin.getMessageManager().sendRawMessage(player, "&e/money &7- Guthaben anzeigen");
        plugin.getMessageManager().sendRawMessage(player, "");
        plugin.getMessageManager().sendRawMessage(player, "&7Für mehr Hilfe: &e/cb help");
        
        plugin.getSoundManager().playClickSound(player);
    }
}
