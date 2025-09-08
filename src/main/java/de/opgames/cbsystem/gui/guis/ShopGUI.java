package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopGUI extends BaseGUI {
    
    public ShopGUI(CBSystem plugin, Player player) {
        super(plugin, player, plugin.getConfigManager().getShopGUITitle(), 
              plugin.getConfigManager().getShopGUISize());
    }
    
    @Override
    protected void setupGUI() {
        // Spieler-Guthaben anzeigen
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        setItem(4, Material.GOLD_INGOT, "&6&lDein Guthaben",
            List.of(
                "&7Aktuelles Guthaben:",
                "&a" + plugin.getEconomyManager().formatBalance(balance),
                "",
                "&7Wähle eine Kategorie aus!"
            ));
        
        // Shop-Kategorien
        setupShopCategories();
        
        // Navigation
        setItem(45, createBackButton());
        setItem(53, createCloseButton());
        
        fillEmptySlots();
    }
    
    private void setupShopCategories() {
        // Baublöcke
        if (plugin.getConfig().getBoolean("shop.categories.blocks.enabled", true)) {
            setItem(10, Material.GRASS_BLOCK, "&a&lBaublöcke",
                List.of(
                    "&7Alle Arten von Blöcken",
                    "&7zum Bauen und Dekorieren.",
                    "",
                    "&7Kategorien: &eStein, Holz, Erde",
                    "",
                    "&aLinksklick: &7Kategorie öffnen"
                ));
        }
        
        // Werkzeuge
        if (plugin.getConfig().getBoolean("shop.categories.tools.enabled", true)) {
            setItem(12, Material.DIAMOND_PICKAXE, "&b&lWerkzeuge",
                List.of(
                    "&7Werkzeuge für alle",
                    "&7deine Bedürfnisse.",
                    "",
                    "&7Kategorien: &eSpitzhacken, Äxte, Schaufeln",
                    "",
                    "&bLinksklick: &7Kategorie öffnen"
                ));
        }
        
        // Essen
        if (plugin.getConfig().getBoolean("shop.categories.food.enabled", true)) {
            setItem(14, Material.BREAD, "&e&lEssen",
                List.of(
                    "&7Nahrung um dich",
                    "&7satt zu halten.",
                    "",
                    "&7Kategorien: &eFleisch, Gemüse, Süßes",
                    "",
                    "&eLinksklick: &7Kategorie öffnen"
                ));
        }
        
        // Redstone
        if (plugin.getConfig().getBoolean("shop.categories.redstone.enabled", true)) {
            setItem(16, Material.REDSTONE, "&c&lRedstone",
                List.of(
                    "&7Redstone-Komponenten",
                    "&7für deine Maschinen.",
                    "",
                    "&7Kategorien: &eRedstone, Repeater, Pistons",
                    "",
                    "&cLinksklick: &7Kategorie öffnen"
                ));
        }
        
        // Dekoration
        if (plugin.getConfig().getBoolean("shop.categories.decoration.enabled", true)) {
            setItem(28, Material.FLOWER_POT, "&d&lDekoration",
                List.of(
                    "&7Dekorative Items für",
                    "&7schöne Builds.",
                    "",
                    "&7Kategorien: &eBlumen, Gemälde, Teppiche",
                    "",
                    "&dLinksklick: &7Kategorie öffnen"
                ));
        }
        
        // Seltene Items
        if (plugin.getConfig().getBoolean("shop.categories.rare.enabled", true)) {
            setItem(30, Material.DIAMOND, "&6&lSeltene Items",
                List.of(
                    "&7Seltene und wertvolle",
                    "&7Items und Materialien.",
                    "",
                    "&7Kategorien: &eEdelsteine, Verzauberungen",
                    "",
                    "&6Linksklick: &7Kategorie öffnen"
                ));
        }
        
        // Spezial-Kategorien
        setItem(32, Material.CHEST, "&f&lSell-All",
            List.of(
                "&7Verkaufe alle Items",
                "&7aus deinem Inventar.",
                "",
                "&fLinksklick: &7Sell-All öffnen"
            ));
        
        setItem(34, Material.HOPPER, "&9&lMassen-Kauf",
            List.of(
                "&7Kaufe Items in",
                "&7größeren Mengen.",
                "",
                "&9Linksklick: &7Massen-Kauf öffnen"
            ));
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 10: // Baublöcke
                plugin.getGUIManager().openShopCategoryGUI(player, "blocks");
                break;
                
            case 12: // Werkzeuge
                plugin.getGUIManager().openShopCategoryGUI(player, "tools");
                break;
                
            case 14: // Essen
                plugin.getGUIManager().openShopCategoryGUI(player, "food");
                break;
                
            case 16: // Redstone
                plugin.getGUIManager().openShopCategoryGUI(player, "redstone");
                break;
                
            case 28: // Dekoration
                plugin.getGUIManager().openShopCategoryGUI(player, "decoration");
                break;
                
            case 30: // Seltene Items
                plugin.getGUIManager().openShopCategoryGUI(player, "rare");
                break;
                
            case 32: // Sell-All
                plugin.getGUIManager().openSellAllGUI(player);
                break;
                
            case 34: // Massen-Kauf
                // Implementiere Massen-Kauf Funktionalität
                plugin.getMessageManager().sendMessage(player, "shop.bulk-buy-coming-soon");
                break;
                
            case 45: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 53: // Schließen
                closeGUI();
                break;
        }
    }
}
