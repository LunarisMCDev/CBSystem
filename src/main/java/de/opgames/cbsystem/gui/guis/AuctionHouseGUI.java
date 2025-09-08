package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.auction.AuctionItem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI extends BaseGUI {
    
    private final List<AuctionItem> auctions;
    private int currentPage = 0;
    
    public AuctionHouseGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&6&lAuktionshaus", 54);
        // Sicherstellen, dass AuctionManager initialisiert ist
        if (plugin.getAuctionManager() == null) {
            plugin.getLogger().severe("AuctionManager ist null! Das Plugin ist nicht richtig initialisiert.");
            this.auctions = new ArrayList<>();
        } else {
            this.auctions = plugin.getAuctionManager().getActiveAuctions();
        }
    }
    
    @Override
    protected void setupGUI() {
        // Header: Auktionshaus-Info
        setItem(4, Material.GOLD_BLOCK, "&6&lAuktionshaus",
            List.of(
                "&7Aktive Auktionen: &e" + (auctions != null ? auctions.size() : 0),
                "&7Deine aktiven Auktionen: &e" + (plugin.getAuctionManager() != null ? 
                    plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId()).size() : 0),
                "",
                "&7Klicke auf eine Auktion zum Kaufen",
                "&7oder erstelle eine neue Auktion"
            ));
        
        // Auktionen anzeigen (Slots 9-44)
        int startIndex = currentPage * 36;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + 36, auctions != null ? auctions.size() : 0); i++) {
            if (slot > 44) break;
            
            AuctionItem auction = auctions != null ? auctions.get(i) : null;
            if (auction != null) {
                setupAuctionItem(slot, auction);
            }
            slot++;
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) (auctions != null ? auctions.size() : 0) / 36);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Aktions-Buttons
        setItem(46, Material.EMERALD, "&a&lAuktion erstellen",
            List.of(
                "&7Verkaufe deine Items",
                "&7an andere Spieler",
                "",
                "&aLinksklick: &7Item auswählen"
            ));
        
        setItem(47, Material.CHEST, "&e&lMeine Auktionen",
            List.of(
                "&7Verwalte deine aktiven",
                "&7und verkauften Auktionen",
                "",
                "&eLinksklick: &7Auktionen anzeigen"
            ));
        
        setItem(48, Material.HOPPER, "&b&lFilter",
            List.of(
                "&7Filtere Auktionen nach",
                "&7Kategorie oder Preis",
                "",
                "&bLinksklick: &7Filter öffnen"
            ));
        
        setItem(49, createBackButton());
        setItem(50, createCloseButton());
        
        setItem(51, Material.CLOCK, "&d&lSortierung",
            List.of(
                "&7Sortiere nach Zeit,",
                "&7Preis oder Beliebtheit",
                "",
                "&dLinksklick: &7Sortierung ändern"
            ));
        
        setItem(52, Material.BOOK, "&f&lHilfe",
            List.of(
                "&7Wie funktioniert",
                "&7das Auktionshaus?",
                "",
                "&fLinksklick: &7Hilfe anzeigen"
            ));
        
        fillEmptySlots();
    }
    
    private void setupAuctionItem(int slot, AuctionItem auction) {
        ItemStack displayItem = auction.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        
        if (meta != null) {
            // Titel setzen
            String itemName = meta.hasDisplayName() ? meta.getDisplayName() : 
                plugin.getMessageManager().colorize("&e" + getItemDisplayName(displayItem));
            meta.setDisplayName(itemName);
            
            // Lore erstellen
            List<String> lore = new ArrayList<>();
            lore.add("&7Verkäufer: &e" + auction.getSellerName());
            lore.add("&7Preis: &a" + plugin.getEconomyManager().formatBalance(auction.getPrice()));
            lore.add("&7Verbleibende Zeit: &c" + auction.getFormattedTimeRemaining());
            lore.add("&7Anzahl: &f" + displayItem.getAmount() + "x");
            
            if (displayItem.hasItemMeta() && displayItem.getItemMeta().hasLore()) {
                lore.add("");
                lore.add("&6Item-Details:");
                List<String> itemLore = displayItem.getItemMeta().getLore();
                if (itemLore != null) {
                    for (String line : itemLore) {
                        lore.add("&7" + line);
                    }
                }
            }
            
            lore.add("");
            if (auction.getSeller().equals(player.getUniqueId())) {
                lore.add("&c&lDeine Auktion");
                lore.add("&cRechtsklick: &7Auktion abbrechen");
            } else {
                lore.add("&aLinksklick: &7Auktion kaufen");
                lore.add("&eShift+Links: &7Details anzeigen");
            }
            
            // Lore färben
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(plugin.getMessageManager().colorize(line));
            }
            meta.setLore(coloredLore);
            
            displayItem.setItemMeta(meta);
        }
        
        setItem(slot, displayItem);
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 45: // Vorherige Seite
                if (currentPage > 0) {
                    currentPage--;
                    update();
                }
                break;
                
            case 46: // Auktion erstellen
                plugin.getGUIManager().openCreateAuctionGUI(player);
                break;
                
            case 47: // Meine Auktionen
                plugin.getGUIManager().openMyAuctionsGUI(player);
                break;
                
            case 48: // Filter
                plugin.getMessageManager().sendMessage(player, "auction.filter-coming-soon");
                break;
                
            case 49: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 51: // Sortierung
                plugin.getMessageManager().sendMessage(player, "auction.sorting-coming-soon");
                break;
                
            case 52: // Hilfe
                showAuctionHelp();
                break;
                
            case 53: // Nächste Seite
                int totalPages = (int) Math.ceil((double) (auctions != null ? auctions.size() : 0) / 36);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    update();
                }
                break;
                
            default:
                // Auktions-Interaktion
                if (slot >= 9 && slot <= 44) {
                    handleAuctionClick(slot, isShiftClick, isRightClick);
                }
                break;
        }
    }
    
    private void handleAuctionClick(int slot, boolean isShiftClick, boolean isRightClick) {
        int auctionIndex = (currentPage * 36) + (slot - 9);
        
        if (auctions == null || auctionIndex >= auctions.size()) return;
        
        AuctionItem auction = auctions.get(auctionIndex);
        
        if (auction.getSeller().equals(player.getUniqueId())) {
            // Eigene Auktion
            if (isRightClick) {
                // Auktion abbrechen
                if (plugin.getAuctionManager().cancelAuction(player, auction.getId())) {
                    playSuccessSound();
                    update(); // GUI aktualisieren
                } else {
                    playErrorSound();
                }
            }
        } else {
            // Fremde Auktion
            if (isShiftClick) {
                // Details anzeigen
                showAuctionDetails(auction);
            } else {
                // Auktion kaufen
                if (plugin.getAuctionManager().buyAuction(player, auction.getId())) {
                    playSuccessSound();
                    plugin.getSoundManager().playShopBuySound(player);
                    update(); // GUI aktualisieren
                } else {
                    playErrorSound();
                }
            }
        }
    }
    
    private void showAuctionDetails(AuctionItem auction) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Auktions-Details ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Item: &e" + getItemDisplayName(auction.getItem()));
        plugin.getMessageManager().sendRawMessage(player, "&7Anzahl: &f" + auction.getItem().getAmount() + "x");
        plugin.getMessageManager().sendRawMessage(player, "&7Verkäufer: &e" + auction.getSellerName());
        plugin.getMessageManager().sendRawMessage(player, "&7Preis: &a" + plugin.getEconomyManager().formatBalance(auction.getPrice()));
        plugin.getMessageManager().sendRawMessage(player, "&7Erstellt: &f" + new java.util.Date(auction.getCreatedAt()));
        plugin.getMessageManager().sendRawMessage(player, "&7Verbleibend: &c" + auction.getFormattedTimeRemaining());
        
        if (auction.getItem().hasItemMeta() && auction.getItem().getItemMeta().hasEnchants()) {
            plugin.getMessageManager().sendRawMessage(player, "&7Verzauberungen: &d" + 
                auction.getItem().getItemMeta().getEnchants().size());
        }
        
        playClickSound();
    }
    
    private void showAuctionHelp() {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Auktionshaus Hilfe ===");
        plugin.getMessageManager().sendRawMessage(player, "&e• &7Klicke auf Auktionen zum Kaufen");
        plugin.getMessageManager().sendRawMessage(player, "&e• &7Shift+Klick für Details");
        plugin.getMessageManager().sendRawMessage(player, "&e• &7Rechtsklick auf eigene Auktionen zum Abbrechen");
        plugin.getMessageManager().sendRawMessage(player, "&e• &7Auktionen laufen 24h");
        plugin.getMessageManager().sendRawMessage(player, "&e• &75% Steuer beim Erstellen");
        plugin.getMessageManager().sendRawMessage(player, "&e• &7Max. 5 Auktionen pro Spieler");
        
        playClickSound();
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
