package de.opgames.cbsystem.gui;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGUI {
    
    protected final CBSystem plugin;
    protected final Player player;
    protected final Inventory inventory;
    protected final int size;
    protected final String title;
    
    public BaseGUI(CBSystem plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.title = plugin.getMessageManager().colorize(title);
        this.size = size;
        this.inventory = Bukkit.createInventory(null, size, this.title);
        
        setupGUI();
    }
    
    /**
     * Wird beim Erstellen des GUIs aufgerufen
     */
    protected abstract void setupGUI();
    
    /**
     * Wird bei einem Klick aufgerufen
     */
    public abstract void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick);
    
    /**
     * Wird beim Schließen des GUIs aufgerufen
     */
    public void onClose() {
        // Override in subclasses if needed
    }
    
    /**
     * Aktualisiert das GUI
     */
    public void update() {
        inventory.clear();
        setupGUI();
    }
    
    /**
     * Holt das Inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Holt den Spieler
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Holt den Titel
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Erstellt ein ItemStack mit Name und Lore
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(plugin.getMessageManager().colorize(name));
            }
            
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(plugin.getMessageManager().colorize(line));
                }
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Erstellt ein ItemStack mit Name
     */
    protected ItemStack createItem(Material material, String name) {
        List<String> emptyLore = null;
        return createItem(material, name, emptyLore);
    }
    
    /**
     * Erstellt ein ItemStack mit Name und einer Lore-Zeile
     */
    protected ItemStack createItem(Material material, String name, String loreLine) {
        List<String> lore = new ArrayList<>();
        if (loreLine != null) {
            lore.add(loreLine);
        }
        return createItem(material, name, lore);
    }
    
    /**
     * Setzt ein Item an einer bestimmten Position
     */
    protected void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }
    
    /**
     * Setzt ein Item an einer bestimmten Position
     */
    protected void setItem(int slot, Material material, String name, List<String> lore) {
        setItem(slot, createItem(material, name, lore));
    }
    
    /**
     * Setzt ein Item an einer bestimmten Position
     */
    protected void setItem(int slot, Material material, String name) {
        setItem(slot, createItem(material, name));
    }
    
    /**
     * Füllt leere Slots mit einem Füll-Item
     */
    protected void fillEmptySlots() {
        if (!plugin.getConfigManager().shouldFillEmptySlots()) return;
        
        Material fillMaterial = Material.valueOf(plugin.getConfigManager().getFillItem());
        String fillName = plugin.getConfigManager().getFillItemName();
        
        ItemStack fillItem = createItem(fillMaterial, fillName);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }
    
    /**
     * Erstellt einen Zurück-Button
     */
    protected ItemStack createBackButton() {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().getMessage("gui.back.lore.0"));
        return createItem(
            Material.ARROW,
            plugin.getMessageManager().getMessage("gui.back.name"),
            lore
        );
    }
    
    /**
     * Erstellt einen Schließen-Button
     */
    protected ItemStack createCloseButton() {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().getMessage("gui.close.lore.0"));
        return createItem(
            Material.BARRIER,
            plugin.getMessageManager().getMessage("gui.close.name"),
            lore
        );
    }
    
    /**
     * Erstellt einen Nächste-Seite-Button
     */
    protected ItemStack createNextPageButton(int currentPage, int maxPages) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().getMessage("gui.next-page.lore.0",
            "page", String.valueOf(currentPage + 1),
            "maxpage", String.valueOf(maxPages)));
        return createItem(
            Material.ARROW,
            plugin.getMessageManager().getMessage("gui.next-page.name"),
            lore
        );
    }
    
    /**
     * Erstellt einen Vorherige-Seite-Button
     */
    protected ItemStack createPreviousPageButton(int currentPage, int maxPages) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessageManager().getMessage("gui.previous-page.lore.0",
            "page", String.valueOf(currentPage + 1),
            "maxpage", String.valueOf(maxPages)));
        return createItem(
            Material.ARROW,
            plugin.getMessageManager().getMessage("gui.previous-page.name"),
            lore
        );
    }
    
    /**
     * Überprüft ob ein Slot ein Füll-Item enthält
     */
    protected boolean isFillItem(ItemStack item) {
        if (item == null) return false;
        
        try {
            Material fillMaterial = Material.valueOf(plugin.getConfigManager().getFillItem());
            return item.getType() == fillMaterial && 
                   item.hasItemMeta() && 
                   item.getItemMeta().hasDisplayName() &&
                   item.getItemMeta().getDisplayName().equals(
                       plugin.getMessageManager().colorize(plugin.getConfigManager().getFillItemName())
                   );
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Spielt einen Klick-Sound ab
     */
    protected void playClickSound() {
        plugin.getSoundManager().playClickSound(player);
    }
    
    /**
     * Spielt einen Erfolgs-Sound ab
     */
    protected void playSuccessSound() {
        plugin.getSoundManager().playSuccessSound(player);
    }
    
    /**
     * Spielt einen Fehler-Sound ab
     */
    protected void playErrorSound() {
        plugin.getSoundManager().playErrorSound(player);
    }
    
    /**
     * Schließt das GUI
     */
    protected void closeGUI() {
        player.closeInventory();
    }
    
    /**
     * Öffnet ein anderes GUI
     */
    protected void openGUI(BaseGUI newGUI) {
        plugin.getGUIManager().closeGUI(player);
        player.openInventory(newGUI.getInventory());
    }
}
