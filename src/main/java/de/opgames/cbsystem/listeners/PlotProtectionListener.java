package de.opgames.cbsystem.listeners;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlotProtectionListener implements Listener {
    
    private final CBSystem plugin;
    
    public PlotProtectionListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlotManager().isEnabled()) {
            return;
        }
        
        // Admin-Bypass
        if (player.hasPermission("cbsystem.admin.plot.bypass")) {
            return;
        }
        
        Plot plot = plugin.getPlotManager().getPlotAt(event.getBlock().getLocation());
        
        // Außerhalb von Plots - erlaube nur mit Berechtigung
        if (plot == null) {
            if (!player.hasPermission("cbsystem.plot.build.outside")) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "plot.no-build-outside");
                plugin.getSoundManager().playErrorSound(player);
            }
            return;
        }
        
        // Überprüfe Plot-Berechtigung
        if (!plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "plot.no-permission");
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        // Log für Plot-Aktionen
        if (plugin.getConfigManager().isPlotActionLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s hat Block abgebaut in Plot %s", 
                player.getName(), plot.getId()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlotManager().isEnabled()) {
            return;
        }
        
        // Admin-Bypass
        if (player.hasPermission("cbsystem.admin.plot.bypass")) {
            return;
        }
        
        Plot plot = plugin.getPlotManager().getPlotAt(event.getBlock().getLocation());
        
        // Außerhalb von Plots - erlaube nur mit Berechtigung
        if (plot == null) {
            if (!player.hasPermission("cbsystem.plot.build.outside")) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "plot.no-build-outside");
                plugin.getSoundManager().playErrorSound(player);
            }
            return;
        }
        
        // Überprüfe Plot-Berechtigung
        if (!plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "plot.no-permission");
            plugin.getSoundManager().playErrorSound(player);
            return;
        }
        
        // Log für Plot-Aktionen
        if (plugin.getConfigManager().isPlotActionLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s hat Block platziert in Plot %s", 
                player.getName(), plot.getId()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlotManager().isEnabled() || event.getClickedBlock() == null) {
            return;
        }
        
        // Admin-Bypass
        if (player.hasPermission("cbsystem.admin.plot.bypass")) {
            return;
        }
        
        Plot plot = plugin.getPlotManager().getPlotAt(event.getClickedBlock().getLocation());
        
        // Außerhalb von Plots - erlaube nur mit Berechtigung
        if (plot == null) {
            if (!player.hasPermission("cbsystem.plot.interact.outside")) {
                // Nur bestimmte Interaktionen blockieren
                if (isProtectedInteraction(event)) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "plot.no-interact-outside");
                    plugin.getSoundManager().playErrorSound(player);
                }
            }
            return;
        }
        
        // Überprüfe Plot-Berechtigung für geschützte Interaktionen
        if (isProtectedInteraction(event) && 
            !plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
            
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "plot.no-permission");
            plugin.getSoundManager().playErrorSound(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getPlotManager().isEnabled()) {
            return;
        }
        
        // Überprüfe nur bei Chunk-Wechsel
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        Plot fromPlot = plugin.getPlotManager().getPlotAt(event.getFrom());
        Plot toPlot = plugin.getPlotManager().getPlotAt(event.getTo());
        
        // Plot verlassen
        if (fromPlot != null && !fromPlot.equals(toPlot)) {
            onPlotLeave(player, fromPlot);
        }
        
        // Plot betreten
        if (toPlot != null && !toPlot.equals(fromPlot)) {
            onPlotEnter(player, toPlot);
        }
    }
    
    private void onPlotEnter(Player player, Plot plot) {
        // Zeige Plot-Info
        if (plot.hasOwner()) {
            String ownerName = plot.getOwnerAbs().toString();
            plugin.getMessageManager().sendRawMessage(player, 
                "&7Du betrittst das Plot von &e" + ownerName);
        } else {
            plugin.getMessageManager().sendRawMessage(player, 
                "&7Du betrittst ein freies Plot &8(&e/plot buy&8)");
        }
        
        // Sound abspielen
        plugin.getSoundManager().playBellSound(player);
        
        // Log
        if (plugin.getConfigManager().isPlotActionLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s betritt Plot %s", 
                player.getName(), plot.getId()));
        }
    }
    
    private void onPlotLeave(Player player, Plot plot) {
        // Log
        if (plugin.getConfigManager().isPlotActionLoggingEnabled()) {
            plugin.getLogger().info(String.format("Spieler %s verlässt Plot %s", 
                player.getName(), plot.getId()));
        }
    }
    
    private boolean isProtectedInteraction(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return false;
        }
        
        String blockType = event.getClickedBlock().getType().name();
        
        // Geschützte Blöcke/Items
        return blockType.contains("DOOR") ||
               blockType.contains("GATE") ||
               blockType.contains("BUTTON") ||
               blockType.contains("LEVER") ||
               blockType.contains("PRESSURE_PLATE") ||
               blockType.contains("CHEST") ||
               blockType.contains("BARREL") ||
               blockType.contains("SHULKER_BOX") ||
               blockType.contains("FURNACE") ||
               blockType.contains("HOPPER") ||
               blockType.contains("DISPENSER") ||
               blockType.contains("DROPPER") ||
               blockType.equals("CRAFTING_TABLE") ||
               blockType.equals("ENCHANTING_TABLE") ||
               blockType.equals("ANVIL") ||
               blockType.equals("BREWING_STAND") ||
               blockType.equals("BEACON");
    }
}
