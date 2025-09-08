package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CreateAuctionGUI extends BaseGUI {
    
    private ItemStack selectedItem = null;
    private double selectedPrice = 0.0;
    private final double[] pricePresets = {100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0};
    
    public CreateAuctionGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&a&lAuktion erstellen", 54);
    }
    
    @Override
    protected void setupGUI() {
        // Header: Anleitung
        setItem(4, Material.EMERALD, "&a&lAuktion erstellen",
            List.of(
                "&7Schritt 1: Item aus Inventar auswählen",
                "&7Schritt 2: Preis festlegen",
                "&7Schritt 3: Auktion erstellen",
                "",
                "&7Steuer: &c5% &7vom Verkaufspreis",
                "&7Laufzeit: &e24 Stunden"
            ));
        
        // Inventar-Items anzeigen (Slots 9-35)
        int slot = 9;
        for (int i = 0; i < 27; i++) { // Zeige nur die ersten 27 Slots des Spieler-Inventars
            if (slot > 35) break;
            
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                setupInventoryItem(slot, item, i);
            } else {
                setItem(slot, Material.GRAY_STAINED_GLASS_PANE, "&7Leer", List.of("&7Kein Item vorhanden"));
            }
            slot++;
        }
        
        // Ausgewähltes Item anzeigen
        if (selectedItem != null) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Ausgewähltes Item:");
            lore.add("&e" + getItemDisplayName(selectedItem));
            lore.add("&7Anzahl: &f" + selectedItem.getAmount() + "x");
            lore.add("");
            lore.add("&aLinksklick: &7Item abwählen");
            
            setItem(40, selectedItem.getType(), "&a&lAusgewähltes Item", lore);
        } else {
            setItem(40, Material.BARRIER, "&c&lKein Item ausgewählt",
                List.of("&7Wähle ein Item aus deinem", "&7Inventar aus"));
        }
        
        // Preis-Presets (Slots 45-50)
        for (int i = 0; i < Math.min(pricePresets.length, 6); i++) {
            double price = pricePresets[i];
            boolean selected = Math.abs(selectedPrice - price) < 0.01;
            
            Material material = selected ? Material.EMERALD : Material.GOLD_NUGGET;
            String name = (selected ? "&a&l" : "&e") + plugin.getEconomyManager().formatBalance(price);
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Setze den Preis auf:");
            lore.add("&a" + plugin.getEconomyManager().formatBalance(price));
            lore.add("&7Steuer: &c" + plugin.getEconomyManager().formatBalance(price * 0.05));
            lore.add("");
            if (selected) {
                lore.add("&a&lAusgewählt");
            } else {
                lore.add("&eLinksklick: &7Preis auswählen");
            }
            
            setItem(45 + i, material, name, lore);
        }
        
        // Aktions-Buttons
        setItem(51, Material.BOOK, "&6&lEigenen Preis",
            List.of(
                "&7Gib einen eigenen Preis ein",
                "&7Aktueller Preis: &a" + plugin.getEconomyManager().formatBalance(selectedPrice),
                "",
                "&6Linksklick: &7Preis eingeben"
            ));
        
        setItem(52, Material.CLOCK, "&d&lLaufzeit",
            List.of(
                "&7Auktionslaufzeit: &e24 Stunden",
                "&7(Automatisch festgelegt)",
                "",
                "&7Zukünftig: Andere Laufzeiten"
            ));
        
        setItem(53, createCloseButton());
        
        // Auktion erstellen Button
        boolean canCreate = selectedItem != null && selectedPrice > 0;
        if (canCreate) {
            double tax = selectedPrice * 0.05;
            setItem(49, Material.EMERALD_BLOCK, "&a&lAuktion erstellen",
                List.of(
                    "&7Item: &e" + getItemDisplayName(selectedItem),
                    "&7Preis: &a" + plugin.getEconomyManager().formatBalance(selectedPrice),
                    "&7Steuer: &c" + plugin.getEconomyManager().formatBalance(tax),
                    "",
                    "&aLinksklick: &7Auktion erstellen!"
                ));
        } else {
            setItem(49, Material.BARRIER, "&c&lAuktion erstellen",
                List.of(
                    "&7Wähle zuerst ein Item",
                    "&7und einen Preis aus!",
                    "",
                    "&cNicht verfügbar"
                ));
        }
        
        // Zurück Button
        setItem(36, createBackButton());
        
        fillEmptySlots();
    }
    
    private void setupInventoryItem(int slot, ItemStack item, int inventoryIndex) {
        boolean selected = selectedItem != null && selectedItem.isSimilar(item) && 
                          selectedItem.getAmount() == item.getAmount();
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Inventar-Slot: &e" + inventoryIndex);
        lore.add("&7Anzahl: &f" + item.getAmount() + "x");
        
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            lore.add("&7Verzauberungen: &d" + item.getItemMeta().getEnchants().size());
        }
        
        lore.add("");
        if (selected) {
            lore.add("&a&lAusgewählt");
            lore.add("&aLinksklick: &7Abwählen");
        } else {
            lore.add("&eLinksklick: &7Auswählen");
        }
        
        ItemStack displayItem = item.clone();
        if (displayItem.hasItemMeta()) {
            displayItem.getItemMeta().setLore(lore.stream()
                .map(line -> plugin.getMessageManager().colorize(line))
                .toList());
        }
        
        setItem(slot, displayItem);
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        // Inventar-Items (Slots 9-35)
        if (slot >= 9 && slot <= 35) {
            int inventoryIndex = slot - 9;
            ItemStack item = player.getInventory().getItem(inventoryIndex);
            
            if (item != null && item.getType() != Material.AIR) {
                if (selectedItem != null && selectedItem.isSimilar(item) && 
                    selectedItem.getAmount() == item.getAmount()) {
                    // Item abwählen
                    selectedItem = null;
                } else {
                    // Item auswählen
                    selectedItem = item.clone();
                }
                update();
            }
            return;
        }
        
        switch (slot) {
            case 36: // Zurück
                plugin.getGUIManager().openAuctionHouse(player);
                break;
                
            case 40: // Ausgewähltes Item
                if (selectedItem != null) {
                    selectedItem = null;
                    update();
                }
                break;
                
            case 45: case 46: case 47: case 48: case 50: // Preis-Presets
                int presetIndex = slot - 45;
                if (presetIndex < pricePresets.length) {
                    selectedPrice = pricePresets[presetIndex];
                    update();
                }
                break;
                
            case 49: // Auktion erstellen
                createAuction();
                break;
                
            case 51: // Eigener Preis
                handleCustomPrice();
                break;
                
            case 53: // Schließen
                closeGUI();
                break;
        }
    }
    
    private void createAuction() {
        if (selectedItem == null) {
            plugin.getMessageManager().sendMessage(player, "auction.no-item-selected");
            playErrorSound();
            return;
        }
        
        if (selectedPrice <= 0) {
            plugin.getMessageManager().sendMessage(player, "auction.no-price-selected");
            playErrorSound();
            return;
        }
        
        // Auktion erstellen
        if (plugin.getAuctionManager().createAuction(player, selectedItem, selectedPrice)) {
            playSuccessSound();
            closeGUI();
            plugin.getGUIManager().openAuctionHouse(player);
        } else {
            playErrorSound();
        }
    }
    
    private void handleCustomPrice() {
        closeGUI();
        plugin.getMessageManager().sendMessage(player, "auction.enter-custom-price");
        
        // TODO: Chat-Input-System implementieren
        plugin.getMessageManager().sendMessage(player, "auction.custom-price-coming-soon");
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
            case DIAMOND_SWORD -> "Diamantschwert";
            case IRON_PICKAXE -> "Eisenspitzhacke";
            default -> item.getType().name().toLowerCase().replace("_", " ");
        };
    }
}
