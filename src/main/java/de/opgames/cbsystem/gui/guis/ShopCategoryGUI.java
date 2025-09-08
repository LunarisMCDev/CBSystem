package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopCategoryGUI extends BaseGUI {
    
    private final String category;
    private int currentPage = 0;
    private final Map<String, ShopItem[]> categoryItems;
    
    public ShopCategoryGUI(CBSystem plugin, Player player, String category) {
        super(plugin, player, "&6&lShop &8- &7" + getCategoryDisplayName(category), 54);
        this.category = category;
        this.categoryItems = initializeCategoryItems();
        // Re-setup GUI after categoryItems is initialized
        setupGUI();
    }
    
    private static String getCategoryDisplayName(String category) {
        return switch (category) {
            case "blocks" -> "Baublöcke";
            case "tools" -> "Werkzeuge";
            case "food" -> "Essen";
            case "redstone" -> "Redstone";
            case "decoration" -> "Dekoration";
            case "rare" -> "Seltene Items";
            default -> "Unbekannt";
        };
    }
    
    @Override
    protected void setupGUI() {
        // Verhindere Setup wenn categoryItems noch nicht initialisiert ist
        if (categoryItems == null) {
            return;
        }
        
        // Spieler-Guthaben
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        setItem(4, Material.GOLD_INGOT, "&6&lDein Guthaben",
            List.of("&a" + plugin.getEconomyManager().formatBalance(balance)));
        
        // Shop-Items für diese Kategorie
        ShopItem[] items = categoryItems.getOrDefault(category, new ShopItem[0]);
        
        // Items anzeigen (Slots 9-44)
        int startIndex = currentPage * 36;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + 36, items.length); i++) {
            if (slot > 44) break;
            
            ShopItem shopItem = items[i];
            List<String> lore = List.of(
                "&7Kaufpreis: &a" + plugin.getEconomyManager().formatBalance(shopItem.buyPrice),
                "&7Verkaufspreis: &c" + plugin.getEconomyManager().formatBalance(shopItem.sellPrice),
                "",
                "&aLinksklick: &71x kaufen",
                "&aShift+Links: &764x kaufen",
                "&cRechtsklick: &71x verkaufen",
                "&cShift+Rechts: &764x verkaufen"
            );
            
            setItem(slot, shopItem.material, shopItem.displayName, lore);
            slot++;
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) items.length / 36);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Zurück zum Shop
        setItem(49, createBackButton());
        
        // Schließen
        setItem(50, createCloseButton());
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        // Navigation
        switch (slot) {
            case 45: // Vorherige Seite
                if (currentPage > 0) {
                    currentPage--;
                    update();
                }
                break;
                
            case 49: // Zurück zum Shop
                plugin.getGUIManager().openShopGUI(player);
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 53: // Nächste Seite
                if (categoryItems != null) {
                    ShopItem[] items = categoryItems.getOrDefault(category, new ShopItem[0]);
                    int totalPages = (int) Math.ceil((double) items.length / 36);
                    if (currentPage < totalPages - 1) {
                        currentPage++;
                        update();
                    }
                }
                break;
                
            default:
                // Shop-Item-Interaktion
                if (slot >= 9 && slot <= 44) {
                    handleShopItemClick(slot, isShiftClick, isRightClick);
                }
                break;
        }
    }
    
    private void handleShopItemClick(int slot, boolean isShiftClick, boolean isRightClick) {
        if (categoryItems == null) return;
        
        ShopItem[] items = categoryItems.getOrDefault(category, new ShopItem[0]);
        int itemIndex = (currentPage * 36) + (slot - 9);
        
        if (itemIndex >= items.length) return;
        
        ShopItem shopItem = items[itemIndex];
        int amount = isShiftClick ? 64 : 1;
        
        if (isRightClick) {
            // Verkaufen
            handleSellItem(shopItem, amount);
        } else {
            // Kaufen
            handleBuyItem(shopItem, amount);
        }
    }
    
    private void handleBuyItem(ShopItem shopItem, int amount) {
        double totalCost = shopItem.buyPrice * amount;
        
        if (!plugin.getEconomyManager().hasBalance(player.getUniqueId(), totalCost)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.insufficient-funds",
                "price", plugin.getEconomyManager().formatBalance(totalCost));
            return;
        }
        
        // Überprüfe Inventar-Platz
        if (!hasInventorySpace(shopItem.material, amount)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.inventory-full");
            return;
        }
        
        // Führe Kauf durch
        plugin.getEconomyManager().withdrawBalance(player.getUniqueId(), totalCost, 
            "Shop-Kauf: " + amount + "x " + shopItem.displayName);
        
        ItemStack item = new ItemStack(shopItem.material, amount);
        player.getInventory().addItem(item);
        
        playSuccessSound();
        plugin.getSoundManager().playShopBuySound(player);
        plugin.getMessageManager().sendMessage(player, "shop.item-bought",
            "amount", String.valueOf(amount),
            "item", shopItem.displayName,
            "price", plugin.getEconomyManager().formatBalance(totalCost));
        
        update(); // Guthaben aktualisieren
    }
    
    private void handleSellItem(ShopItem shopItem, int amount) {
        if (shopItem.sellPrice <= 0) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.sell-disabled");
            return;
        }
        
        // Überprüfe ob Spieler genug Items hat
        if (!hasItemInInventory(shopItem.material, amount)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "shop.insufficient-items");
            return;
        }
        
        double totalEarnings = shopItem.sellPrice * amount;
        
        // Entferne Items aus Inventar
        removeItemFromInventory(shopItem.material, amount);
        
        // Geld hinzufügen
        plugin.getEconomyManager().addBalance(player.getUniqueId(), totalEarnings,
            "Shop-Verkauf: " + amount + "x " + shopItem.displayName);
        
        playSuccessSound();
        plugin.getSoundManager().playShopSellSound(player);
        plugin.getMessageManager().sendMessage(player, "shop.item-sold",
            "amount", String.valueOf(amount),
            "item", shopItem.displayName,
            "price", plugin.getEconomyManager().formatBalance(totalEarnings));
        
        update(); // Guthaben aktualisieren
    }
    
    private boolean hasInventorySpace(Material material, int amount) {
        return player.getInventory().firstEmpty() != -1 || 
               canStackInExistingSlots(material, amount);
    }
    
    private boolean canStackInExistingSlots(Material material, int amount) {
        int remainingAmount = amount;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int canAdd = material.getMaxStackSize() - item.getAmount();
                remainingAmount -= canAdd;
                if (remainingAmount <= 0) return true;
            }
        }
        
        return false;
    }
    
    private boolean hasItemInInventory(Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }
    
    private void removeItemFromInventory(Material material, int amount) {
        int remaining = amount;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                int removeAmount = Math.min(remaining, item.getAmount());
                
                if (removeAmount == item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - removeAmount);
                }
                
                remaining -= removeAmount;
                if (remaining <= 0) break;
            }
        }
    }
    
    private Map<String, ShopItem[]> initializeCategoryItems() {
        Map<String, ShopItem[]> items = new HashMap<>();
        
        // Baublöcke
        items.put("blocks", new ShopItem[]{
            new ShopItem(Material.STONE, "&7Stein", 1.0, 0.5),
            new ShopItem(Material.COBBLESTONE, "&7Kopfsteinpflaster", 0.8, 0.4),
            new ShopItem(Material.OAK_WOOD, "&eEichenholz", 2.0, 1.0),
            new ShopItem(Material.DIRT, "&6Erde", 0.5, 0.2),
            new ShopItem(Material.GRASS_BLOCK, "&aGrasblock", 1.0, 0.5),
            new ShopItem(Material.SAND, "&eSand", 0.8, 0.4),
            new ShopItem(Material.GRAVEL, "&7Kies", 0.6, 0.3),
            new ShopItem(Material.GLASS, "&fGlas", 2.0, 1.0)
        });
        
        // Werkzeuge
        items.put("tools", new ShopItem[]{
            new ShopItem(Material.WOODEN_PICKAXE, "&6Holzspitzhacke", 10.0, 2.0),
            new ShopItem(Material.STONE_PICKAXE, "&7Steinspitzhacke", 25.0, 5.0),
            new ShopItem(Material.IRON_PICKAXE, "&fEisenspitzhacke", 100.0, 20.0),
            new ShopItem(Material.DIAMOND_PICKAXE, "&bDiamantspitzhacke", 500.0, 100.0),
            new ShopItem(Material.WOODEN_AXE, "&6Holzaxt", 8.0, 1.5),
            new ShopItem(Material.STONE_AXE, "&7Steinaxt", 20.0, 4.0),
            new ShopItem(Material.IRON_AXE, "&fEisenaxt", 80.0, 16.0),
            new ShopItem(Material.DIAMOND_AXE, "&bDiamantaxt", 400.0, 80.0)
        });
        
        // Essen
        items.put("food", new ShopItem[]{
            new ShopItem(Material.BREAD, "&6Brot", 5.0, 1.0),
            new ShopItem(Material.COOKED_BEEF, "&cGebratenes Rindfleisch", 8.0, 2.0),
            new ShopItem(Material.COOKED_PORKCHOP, "&dGebratenes Schweinefleisch", 8.0, 2.0),
            new ShopItem(Material.APPLE, "&cApfel", 3.0, 0.5),
            new ShopItem(Material.GOLDEN_APPLE, "&6Goldapfel", 100.0, 20.0),
            new ShopItem(Material.CAKE, "&eTorte", 25.0, 5.0)
        });
        
        // Redstone
        items.put("redstone", new ShopItem[]{
            new ShopItem(Material.REDSTONE, "&cRedstone", 5.0, 1.0),
            new ShopItem(Material.REPEATER, "&7Verstärker", 15.0, 3.0),
            new ShopItem(Material.COMPARATOR, "&7Komparator", 20.0, 4.0),
            new ShopItem(Material.PISTON, "&7Kolben", 30.0, 6.0),
            new ShopItem(Material.STICKY_PISTON, "&7Klebriger Kolben", 50.0, 10.0),
            new ShopItem(Material.REDSTONE_TORCH, "&cRedstone-Fackel", 8.0, 1.5)
        });
        
        // Dekoration
        items.put("decoration", new ShopItem[]{
            new ShopItem(Material.FLOWER_POT, "&6Blumentopf", 10.0, 2.0),
            new ShopItem(Material.PAINTING, "&eGemälde", 15.0, 3.0),
            new ShopItem(Material.WHITE_CARPET, "&fWeißer Teppich", 5.0, 1.0),
            new ShopItem(Material.TORCH, "&eFackel", 2.0, 0.5),
            new ShopItem(Material.LANTERN, "&6Laterne", 20.0, 4.0)
        });
        
        // Seltene Items
        items.put("rare", new ShopItem[]{
            new ShopItem(Material.DIAMOND, "&bDiamant", 100.0, 50.0),
            new ShopItem(Material.EMERALD, "&aSmaragd", 80.0, 40.0),
            new ShopItem(Material.NETHERITE_INGOT, "&4Netherit-Barren", 1000.0, 500.0),
            new ShopItem(Material.ENCHANTED_BOOK, "&dVerzaubertes Buch", 200.0, 50.0)
        });
        
        return items;
    }
    
    private static class ShopItem {
        final Material material;
        final String displayName;
        final double buyPrice;
        final double sellPrice;
        
        ShopItem(Material material, String displayName, double buyPrice, double sellPrice) {
            this.material = material;
            this.displayName = displayName;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }
}
