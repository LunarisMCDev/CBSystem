package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MainMenuGUI extends BaseGUI {
    
    public MainMenuGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&6&lCBSystem &8- &7Hauptmenü", 27);
    }
    
    @Override
    protected void setupGUI() {
        // Plot-System
        if (plugin.getConfigManager().isPlotSquaredEnabled()) {
            setItem(10, Material.GRASS_BLOCK, "&a&lPlots",
                List.of(
                    "&7Verwalte deine Plots",
                    "&7und erkunde die Welt!",
                    "",
                    "&aLinksklick: &7Plot-Menü öffnen"
                ));
        }
        
        // Home-System
        if (plugin.getConfigManager().isHomesEnabled()) {
            setItem(11, Material.RED_BED, "&b&lHomes",
                List.of(
                    "&7Setze und verwalte",
                    "&7deine Homes.",
                    "",
                    "&bLinksklick: &7Home-Menü öffnen"
                ));
        }
        
        // Warp-System
        if (plugin.getConfigManager().isWarpsEnabled()) {
            setItem(12, Material.ENDER_PEARL, "&d&lWarps",
                List.of(
                    "&7Teleportiere zu",
                    "&7öffentlichen Orten.",
                    "",
                    "&dLinksklick: &7Warp-Menü öffnen"
                ));
        }
        
        // Shop-System
        if (plugin.getConfigManager().isShopEnabled()) {
            setItem(13, Material.EMERALD, "&e&lShop",
                List.of(
                    "&7Kaufe und verkaufe",
                    "&7Items im Shop.",
                    "",
                    "&eLinksklick: &7Shop öffnen"
                ));
        }
        
        // Economy-System
        if (plugin.getConfigManager().isEconomyEnabled()) {
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            setItem(14, Material.GOLD_INGOT, "&6&lEconomy",
                List.of(
                    "&7Verwalte dein Geld",
                    "&7und Transaktionen.",
                    "",
                    "&7Dein Guthaben: &a" + plugin.getEconomyManager().formatBalance(balance),
                    "",
                    "&6Linksklick: &7Economy-Menü öffnen"
                ));
        }
        
        // Teleportation-System
        if (plugin.getConfigManager().isTPAEnabled()) {
            setItem(15, Material.COMPASS, "&c&lTeleportation",
                List.of(
                    "&7TPA, Back und mehr",
                    "&7Teleportations-Features.",
                    "",
                    "&cLinksklick: &7Teleport-Menü öffnen"
                ));
        }
        
        // Admin-Menü (nur für Admins)
        if (player.hasPermission("cbsystem.admin.*")) {
            setItem(16, Material.COMMAND_BLOCK, "&4&lAdmin",
                List.of(
                    "&7Administrations-Tools",
                    "&7und Einstellungen.",
                    "",
                    "&4Linksklick: &7Admin-Menü öffnen"
                ));
        }
        
        // Info-Item
        setItem(4, Material.BOOK, "&f&lCBSystem Info",
            List.of(
                "&7Willkommen bei &6OP-Games.de&7!",
                "",
                "&7Version: &e" + plugin.getDescription().getVersion(),
                "&7Entwickelt mit &c❤ &7für die",
                "&7beste CityBuild-Experience!",
                "",
                "&7Spieler online: &a" + plugin.getServer().getOnlinePlayers().size()
            ));
        
        // Schließen-Button
        setItem(22, createCloseButton());
        
        // Fülle leere Slots
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 10: // Plots
                if (plugin.getConfigManager().isPlotSquaredEnabled()) {
                    // Öffne Plot-Menü - hier würde normalerweise das aktuelle Plot ermittelt werden
                    closeGUI();
                    plugin.getMessageManager().sendMessage(player, "plot.menu-opened");
                }
                break;
                
            case 11: // Homes
                if (plugin.getConfigManager().isHomesEnabled()) {
                    plugin.getGUIManager().openHomeManagementGUI(player);
                }
                break;
                
            case 12: // Warps
                if (plugin.getConfigManager().isWarpsEnabled()) {
                    plugin.getGUIManager().openWarpGUI(player);
                }
                break;
                
            case 13: // Shop
                if (plugin.getConfigManager().isShopEnabled()) {
                    plugin.getGUIManager().openShopGUI(player);
                }
                break;
                
            case 14: // Economy
                if (plugin.getConfigManager().isEconomyEnabled()) {
                    plugin.getGUIManager().openEconomyGUI(player);
                }
                break;
                
            case 15: // Teleportation
                if (plugin.getConfigManager().isTPAEnabled()) {
                    plugin.getGUIManager().openTeleportGUI(player);
                }
                break;
                
            case 16: // Admin
                if (player.hasPermission("cbsystem.admin.*")) {
                    plugin.getGUIManager().openAdminGUI(player);
                }
                break;
                
            case 22: // Schließen
                closeGUI();
                break;
                
            case 4: // Info
                playSuccessSound();
                plugin.getMessageManager().sendMessage(player, "general.plugin-info");
                break;
        }
    }
}
