package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EconomyGUI extends BaseGUI {
    
    public EconomyGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&6&lEconomy-Menü", 27);
    }
    
    @Override
    protected void setupGUI() {
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        
        // Guthaben anzeigen
        setItem(4, Material.GOLD_INGOT, "&6&lDein Guthaben",
            List.of(
                "&7Aktuelles Guthaben:",
                "&a" + plugin.getEconomyManager().formatBalance(balance),
                "",
                "&7Währung: &e" + plugin.getConfigManager().getCurrencyName()
            ));
        
        // Geld senden
        setItem(10, Material.PAPER, "&a&lGeld senden",
            List.of(
                "&7Sende Geld an einen",
                "&7anderen Spieler.",
                "",
                "&aLinksklick: &7Spieler auswählen"
            ));
        
        // Transaktions-Verlauf
        setItem(12, Material.BOOK, "&b&lTransaktions-Verlauf",
            List.of(
                "&7Zeige deine letzten",
                "&7Geld-Transaktionen an.",
                "",
                "&bLinksklick: &7Verlauf anzeigen"
            ));
        
        // Top-Liste
        setItem(14, Material.DIAMOND, "&e&lReichsten Spieler",
            List.of(
                "&7Zeige die reichsten",
                "&7Spieler des Servers.",
                "",
                "&eLinksklick: &7Top-Liste anzeigen"
            ));
        
        // Shop-Zugang
        setItem(16, Material.EMERALD, "&2&lShop",
            List.of(
                "&7Öffne den Server-Shop",
                "&7zum Kaufen und Verkaufen.",
                "",
                "&2Linksklick: &7Shop öffnen"
            ));
        
        // Zurück und Schließen
        setItem(18, createBackButton());
        setItem(26, createCloseButton());
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 10: // Geld senden
                plugin.getGUIManager().openPlayerSelectionGUI(player, "&aGeld senden - Spieler auswählen",
                    selectedPlayer -> {
                        // Hier würde normalerweise ein Betrag-Eingabe-GUI geöffnet
                        plugin.getMessageManager().sendMessage(player, "economy.pay-amount-instruction");
                        closeGUI();
                    });
                break;
                
            case 12: // Transaktions-Verlauf
                plugin.getMessageManager().sendMessage(player, "economy.transaction-history-coming-soon");
                break;
                
            case 14: // Top-Liste
                plugin.getMessageManager().sendMessage(player, "economy.top-list-coming-soon");
                break;
                
            case 16: // Shop
                plugin.getGUIManager().openShopGUI(player);
                break;
                
            case 18: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 26: // Schließen
                closeGUI();
                break;
        }
    }
}
