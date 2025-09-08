package de.opgames.cbsystem.listeners;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    
    private final CBSystem plugin;
    
    public GUIListener(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        BaseGUI gui = plugin.getGUIManager().getOpenGUI(player);
        if (gui == null) {
            return;
        }
        
        // Event abbrechen für GUI-Inventare
        event.setCancelled(true);
        
        // Überprüfe ob das geklickte Inventar das GUI-Inventar ist
        if (!event.getInventory().equals(gui.getInventory())) {
            return;
        }
        
        // Hol das geklickte Item
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }
        
        // Bestimme Klick-Typ
        boolean isShiftClick = event.isShiftClick();
        boolean isRightClick = event.isRightClick();
        int slot = event.getSlot();
        
        try {
            // Delegiere an das GUI
            gui.handleClick(slot, clickedItem, isShiftClick, isRightClick);
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Behandeln des GUI-Klicks: " + e.getMessage());
            e.printStackTrace();
            
            // Schließe GUI bei Fehlern
            plugin.getGUIManager().closeGUI(player);
            plugin.getMessageManager().sendErrorMessage(player, "error.unexpected");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        BaseGUI gui = plugin.getGUIManager().getOpenGUI(player);
        if (gui == null) {
            return;
        }
        
        // Überprüfe ob das geschlossene Inventar das GUI-Inventar ist
        if (!event.getInventory().equals(gui.getInventory())) {
            return;
        }
        
        try {
            // Rufe onClose des GUIs auf
            gui.onClose();
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Schließen des GUIs: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Entferne GUI aus dem Manager
            plugin.getGUIManager().closeGUI(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        BaseGUI gui = plugin.getGUIManager().getOpenGUI(player);
        if (gui == null) {
            return;
        }
        
        // Verhindere Drag-Operationen in GUI-Inventaren
        if (event.getInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
        }
    }
}
