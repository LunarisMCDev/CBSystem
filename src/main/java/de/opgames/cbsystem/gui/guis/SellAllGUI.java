package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellAllGUI extends BaseGUI {
    
    private final Map<Material, SellableItem> sellableItems;
    private double totalValue = 0.0;
    private final Map<Material, Integer> playerItems;
    
    public SellAllGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&c&lVerkaufe Alles", 54);
        this.sellableItems = initializeSellableItems();
        this.playerItems = scanPlayerInventory();
        calculateTotalValue();
    }
    
    @Override
    protected void setupGUI() {
        // Header: Gesamtwert
        setItem(4, Material.GOLD_INGOT, "&6&lGesamtwert",
            List.of(
                "&7Wert aller verkaufbaren Items:",
                "&a" + plugin.getEconomyManager().formatBalance(totalValue),
                "",
                "&eKlicke auf 'Alles verkaufen' um fortzufahren"
            ));
        
        // Verkaufbare Items anzeigen (Slots 9-44)
        int slot = 9;
        for (Map.Entry<Material, Integer> entry : playerItems.entrySet()) {
            if (slot > 44) break;
            
            Material material = entry.getKey();
            int amount = entry.getValue();
            SellableItem sellableItem = sellableItems.get(material);
            
            if (sellableItem != null && amount > 0) {
                double itemValue = sellableItem.price * amount;
                
                List<String> lore = new ArrayList<>();
                lore.add("&7Anzahl: &e" + amount + "x");
                lore.add("&7Preis pro Stück: &a" + plugin.getEconomyManager().formatBalance(sellableItem.price));
                lore.add("&7Gesamtwert: &a" + plugin.getEconomyManager().formatBalance(itemValue));
                lore.add("");
                lore.add("&aLinksklick: &7Nur dieses Item verkaufen");
                lore.add("&cRechtsklick: &7Von Verkauf ausschließen");
                
                setItem(slot, material, "&e" + getItemDisplayName(material), lore);
                slot++;
            }
        }
        
        // Aktions-Buttons
        setItem(48, Material.EMERALD, "&a&lAlles verkaufen",
            List.of(
                "&7Verkaufe alle markierten Items",
                "&7für insgesamt:",
                "&a" + plugin.getEconomyManager().formatBalance(totalValue),
                "",
                "&aLinksklick: &7Verkaufen bestätigen"
            ));
        
        setItem(49, createBackButton());
        setItem(50, createCloseButton());
        
        // Filter-Buttons
        setItem(45, Material.HOPPER, "&6&lFilter: Alle",
            List.of(
                "&7Zeige alle verkaufbaren Items",
                "",
                "&6Linksklick: &7Filter ändern"
            ));
        
        setItem(53, Material.REDSTONE, "&c&lAlles abwählen",
            List.of(
                "&7Entferne alle Items vom Verkauf",
                "",
                "&cLinksklick: &7Abwählen"
            ));
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 48: // Alles verkaufen
                sellAllItems();
                break;
                
            case 49: // Zurück
                plugin.getGUIManager().openShopGUI(player);
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 45: // Filter
                // TODO: Filter-Funktionalität implementieren
                plugin.getMessageManager().sendMessage(player, "shop.filter-coming-soon");
                break;
                
            case 53: // Alles abwählen
                deselectAllItems();
                break;
                
            default:
                // Einzelnes Item verkaufen oder ausschließen
                if (slot >= 9 && slot <= 44) {
                    handleItemClick(slot, isRightClick);
                }
                break;
        }
    }
    
    private void sellAllItems() {
        if (totalValue <= 0) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.no-items-to-sell");
            return;
        }
        
        int totalItemsSold = 0;
        double totalEarnings = 0.0;
        
        // Verkaufe alle Items
        for (Map.Entry<Material, Integer> entry : playerItems.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            SellableItem sellableItem = sellableItems.get(material);
            
            if (sellableItem != null && amount > 0) {
                // Entferne Items aus Inventar
                int removed = removeItemFromInventory(material, amount);
                if (removed > 0) {
                    double earnings = sellableItem.price * removed;
                    totalEarnings += earnings;
                    totalItemsSold += removed;
                }
            }
        }
        
        if (totalItemsSold > 0) {
            // Geld hinzufügen
            plugin.getEconomyManager().addBalance(player.getUniqueId(), totalEarnings,
                "Sell-All: " + totalItemsSold + " Items verkauft");
            
            playSuccessSound();
            plugin.getSoundManager().playShopSellSound(player);
            plugin.getMessageManager().sendMessage(player, "shop.sell-all-success",
                "amount", String.valueOf(totalItemsSold),
                "earnings", plugin.getEconomyManager().formatBalance(totalEarnings));
            
            closeGUI();
        } else {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.sell-all-failed");
        }
    }
    
    private void handleItemClick(int slot, boolean isRightClick) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        
        Material material = item.getType();
        Integer amount = playerItems.get(material);
        SellableItem sellableItem = sellableItems.get(material);
        
        if (amount == null || sellableItem == null) return;
        
        if (isRightClick) {
            // Item vom Verkauf ausschließen
            playerItems.put(material, 0);
            plugin.getMessageManager().sendMessage(player, "shop.item-excluded",
                "item", getItemDisplayName(material));
        } else {
            // Nur dieses Item verkaufen
            int removed = removeItemFromInventory(material, amount);
            if (removed > 0) {
                double earnings = sellableItem.price * removed;
                
                plugin.getEconomyManager().addBalance(player.getUniqueId(), earnings,
                    "Sell-All (Einzelverkauf): " + removed + "x " + getItemDisplayName(material));
                
                playSuccessSound();
                plugin.getSoundManager().playShopSellSound(player);
                plugin.getMessageManager().sendMessage(player, "shop.item-sold",
                    "amount", String.valueOf(removed),
                    "item", getItemDisplayName(material),
                    "price", plugin.getEconomyManager().formatBalance(earnings));
                
                playerItems.put(material, 0);
            }
        }
        
        calculateTotalValue();
        update();
    }
    
    private void deselectAllItems() {
        for (Material material : playerItems.keySet()) {
            playerItems.put(material, 0);
        }
        calculateTotalValue();
        update();
        
        plugin.getMessageManager().sendMessage(player, "shop.all-items-deselected");
    }
    
    private Map<Material, Integer> scanPlayerInventory() {
        Map<Material, Integer> items = new HashMap<>();
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                Material material = item.getType();
                if (sellableItems.containsKey(material)) {
                    items.put(material, items.getOrDefault(material, 0) + item.getAmount());
                }
            }
        }
        
        return items;
    }
    
    private void calculateTotalValue() {
        totalValue = 0.0;
        
        for (Map.Entry<Material, Integer> entry : playerItems.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            SellableItem sellableItem = sellableItems.get(material);
            
            if (sellableItem != null && amount > 0) {
                totalValue += sellableItem.price * amount;
            }
        }
    }
    
    private int removeItemFromInventory(Material material, int amount) {
        int remaining = amount;
        int removed = 0;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                int removeAmount = Math.min(remaining, item.getAmount());
                
                if (removeAmount == item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - removeAmount);
                }
                
                removed += removeAmount;
                remaining -= removeAmount;
                if (remaining <= 0) break;
            }
        }
        
        return removed;
    }
    
    private String getItemDisplayName(Material material) {
        return switch (material) {
            case STONE -> "Stein";
            case COBBLESTONE -> "Kopfsteinpflaster";
            case OAK_WOOD -> "Eichenholz";
            case DIRT -> "Erde";
            case GRASS_BLOCK -> "Grasblock";
            case SAND -> "Sand";
            case GRAVEL -> "Kies";
            case COAL -> "Kohle";
            case IRON_INGOT -> "Eisenbarren";
            case GOLD_INGOT -> "Goldbarren";
            case DIAMOND -> "Diamant";
            case EMERALD -> "Smaragd";
            case WHEAT -> "Weizen";
            case BREAD -> "Brot";
            case APPLE -> "Apfel";
            case COOKED_BEEF -> "Gebratenes Rindfleisch";
            case REDSTONE -> "Redstone";
            case GLASS -> "Glas";
            default -> material.name().toLowerCase().replace("_", " ");
        };
    }
    
    private Map<Material, SellableItem> initializeSellableItems() {
        Map<Material, SellableItem> items = new HashMap<>();
        
        // Blöcke
        items.put(Material.STONE, new SellableItem(0.5));
        items.put(Material.COBBLESTONE, new SellableItem(0.4));
        items.put(Material.OAK_WOOD, new SellableItem(1.0));
        items.put(Material.DIRT, new SellableItem(0.2));
        items.put(Material.GRASS_BLOCK, new SellableItem(0.5));
        items.put(Material.SAND, new SellableItem(0.4));
        items.put(Material.GRAVEL, new SellableItem(0.3));
        items.put(Material.GLASS, new SellableItem(1.0));
        
        // Erze und Materialien
        items.put(Material.COAL, new SellableItem(2.0));
        items.put(Material.IRON_INGOT, new SellableItem(5.0));
        items.put(Material.GOLD_INGOT, new SellableItem(10.0));
        items.put(Material.DIAMOND, new SellableItem(50.0));
        items.put(Material.EMERALD, new SellableItem(40.0));
        items.put(Material.REDSTONE, new SellableItem(1.0));
        
        // Essen
        items.put(Material.WHEAT, new SellableItem(1.0));
        items.put(Material.BREAD, new SellableItem(1.0));
        items.put(Material.APPLE, new SellableItem(0.5));
        items.put(Material.COOKED_BEEF, new SellableItem(2.0));
        items.put(Material.COOKED_PORKCHOP, new SellableItem(2.0));
        
        return items;
    }
    
    private static class SellableItem {
        final double price;
        
        SellableItem(double price) {
            this.price = price;
        }
    }
}
