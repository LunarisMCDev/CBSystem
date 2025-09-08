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

public class MyAuctionsGUI extends BaseGUI {
    
    private final List<AuctionItem> myAuctions;
    private final List<AuctionItem> soldAuctions;
    private int currentPage = 0;
    private boolean showingSold = false;
    
    public MyAuctionsGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&e&lMeine Auktionen", 54);
        if (plugin.getAuctionManager() == null) {
            plugin.getLogger().severe("AuctionManager ist null! Das Plugin ist nicht richtig initialisiert.");
            this.myAuctions = new ArrayList<>();
            this.soldAuctions = new ArrayList<>();
        } else {
            this.myAuctions = plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId());
            this.soldAuctions = plugin.getAuctionManager().getPlayerSoldAuctions(player.getUniqueId());
        }
    }
    
    @Override
    protected void setupGUI() {
        List<AuctionItem> currentList = showingSold ? 
            (soldAuctions != null ? soldAuctions : new ArrayList<>()) : 
            (myAuctions != null ? myAuctions : new ArrayList<>());
        String listType = showingSold ? "Verkauft" : "Aktiv";
        
        // Header: Übersicht
        setItem(4, Material.CHEST, "&e&lMeine Auktionen - " + listType,
            List.of(
                "&7Aktive Auktionen: &e" + (myAuctions != null ? myAuctions.size() : 0),
                "&7Verkaufte Auktionen: &a" + (soldAuctions != null ? soldAuctions.size() : 0),
                "&7Maximale Auktionen: &c5",
                "",
                "&7Aktuell angezeigt: &f" + listType
            ));
        
        // Auktionen anzeigen (Slots 9-44)
        int startIndex = currentPage * 36;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + 36, currentList.size()); i++) {
            if (slot > 44) break;
            
            AuctionItem auction = currentList.get(i);
            setupAuctionItem(slot, auction);
            slot++;
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) currentList.size() / 36);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Toggle-Buttons
        setItem(46, showingSold ? Material.HOPPER : Material.EMERALD, 
            showingSold ? "&a&lAktive Auktionen" : "&e&lAktive Auktionen",
            List.of(
                "&7Zeige deine aktiven Auktionen",
                "&7Anzahl: &e" + (myAuctions != null ? myAuctions.size() : 0),
                "",
                showingSold ? "&aLinksklick: &7Zu aktiven wechseln" : "&e&lAktuell angezeigt"
            ));
        
        setItem(47, showingSold ? Material.GOLD_INGOT : Material.IRON_INGOT,
            showingSold ? "&a&lVerkaufte Auktionen" : "&7&lVerkaufte Auktionen",
            List.of(
                "&7Zeige deine verkauften Auktionen",
                "&7Anzahl: &a" + (soldAuctions != null ? soldAuctions.size() : 0),
                "",
                showingSold ? "&a&lAktuell angezeigt" : "&7Linksklick: &7Zu verkauften wechseln"
            ));
        
        // Aktions-Buttons
        setItem(48, Material.EMERALD, "&a&lNeue Auktion",
            List.of(
                "&7Erstelle eine neue Auktion",
                "&7Freie Slots: &e" + (5 - (myAuctions != null ? myAuctions.size() : 0)) + "/5",
                "",
                "&aLinksklick: &7Auktion erstellen"
            ));
        
        setItem(49, createBackButton());
        setItem(50, createCloseButton());
        
        // Statistiken
        double totalEarnings = (soldAuctions != null ? soldAuctions.stream() : new ArrayList<AuctionItem>().stream())
            .mapToDouble(AuctionItem::getPrice)
            .sum();
        
        setItem(51, Material.GOLD_BLOCK, "&6&lStatistiken",
            List.of(
                "&7Gesamteinnahmen: &a" + plugin.getEconomyManager().formatBalance(totalEarnings),
                "&7Verkaufte Items: &e" + (soldAuctions != null ? soldAuctions.size() : 0),
                "&7Aktive Auktionen: &e" + (myAuctions != null ? myAuctions.size() : 0),
                "&7Erfolgsquote: &a" + calculateSuccessRate() + "%"
            ));
        
        setItem(52, Material.BARRIER, "&c&lAlle abbrechen",
            List.of(
                "&7Brich alle aktiven Auktionen ab",
                "&c&lVorsicht: Nicht rückgängig machbar!",
                "",
                (myAuctions == null || myAuctions.isEmpty()) ? "&7Keine aktiven Auktionen" : "&cLinksklick: &7Alle abbrechen"
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
            lore.add("&7Preis: &a" + plugin.getEconomyManager().formatBalance(auction.getPrice()));
            lore.add("&7Anzahl: &f" + displayItem.getAmount() + "x");
            
            if (auction.isSold()) {
                lore.add("&7Status: &a&lVERKAUFT");
                lore.add("&7Käufer: &e" + (auction.getBuyer() != null ? 
                    plugin.getServer().getOfflinePlayer(auction.getBuyer()).getName() : "Unbekannt"));
                lore.add("&7Verkauft am: &f" + new java.util.Date(auction.getCreatedAt()));
            } else {
                lore.add("&7Status: &e&lAKTIV");
                lore.add("&7Verbleibend: &c" + auction.getFormattedTimeRemaining());
                if (auction.isExpired()) {
                    lore.add("&c&lABGELAUFEN");
                }
            }
            
            lore.add("&7Erstellt: &f" + new java.util.Date(auction.getCreatedAt()));
            
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
            if (!auction.isSold() && !auction.isExpired()) {
                lore.add("&cRechtsklick: &7Auktion abbrechen");
            }
            lore.add("&eShift+Links: &7Details anzeigen");
            
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
                
            case 46: // Aktive Auktionen
                if (showingSold) {
                    showingSold = false;
                    currentPage = 0;
                    update();
                }
                break;
                
            case 47: // Verkaufte Auktionen
                if (!showingSold) {
                    showingSold = true;
                    currentPage = 0;
                    update();
                }
                break;
                
            case 48: // Neue Auktion
                plugin.getGUIManager().openCreateAuctionGUI(player);
                break;
                
            case 49: // Zurück
                plugin.getGUIManager().openAuctionHouse(player);
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 52: // Alle abbrechen
                cancelAllAuctions();
                break;
                
            case 53: // Nächste Seite
                List<AuctionItem> currentList = showingSold ? soldAuctions : myAuctions;
                int totalPages = (int) Math.ceil((double) currentList.size() / 36);
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
        List<AuctionItem> currentList = showingSold ? soldAuctions : myAuctions;
        int auctionIndex = (currentPage * 36) + (slot - 9);
        
        if (auctionIndex >= currentList.size()) return;
        
        AuctionItem auction = currentList.get(auctionIndex);
        
        if (isShiftClick) {
            // Details anzeigen
            showAuctionDetails(auction);
        } else if (isRightClick && !auction.isSold() && !auction.isExpired()) {
            // Auktion abbrechen
            if (plugin.getAuctionManager().cancelAuction(player, auction.getId())) {
                playSuccessSound();
                // Listen aktualisieren
                if (myAuctions != null) {
                    myAuctions.clear();
                    myAuctions.addAll(plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId()));
                }
                update();
            } else {
                playErrorSound();
            }
        }
    }
    
    private void cancelAllAuctions() {
        if (myAuctions == null || myAuctions.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "auction.no-active-auctions");
            return;
        }
        
        int cancelled = 0;
        for (AuctionItem auction : new ArrayList<>(myAuctions)) {
            if (!auction.isSold() && !auction.isExpired()) {
                if (plugin.getAuctionManager().cancelAuction(player, auction.getId())) {
                    cancelled++;
                }
            }
        }
        
        if (cancelled > 0) {
            plugin.getMessageManager().sendMessage(player, "auction.cancelled-all", 
                "count", String.valueOf(cancelled));
            playSuccessSound();
            
            // Liste aktualisieren
            if (myAuctions != null) {
                myAuctions.clear();
                myAuctions.addAll(plugin.getAuctionManager().getPlayerActiveAuctions(player.getUniqueId()));
            }
            update();
        } else {
            plugin.getMessageManager().sendMessage(player, "auction.cancel-all-failed");
            playErrorSound();
        }
    }
    
    private void showAuctionDetails(AuctionItem auction) {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Auktions-Details ===");
        plugin.getMessageManager().sendRawMessage(player, "&7ID: &e#" + auction.getId());
        plugin.getMessageManager().sendRawMessage(player, "&7Item: &e" + getItemDisplayName(auction.getItem()));
        plugin.getMessageManager().sendRawMessage(player, "&7Anzahl: &f" + auction.getItem().getAmount() + "x");
        plugin.getMessageManager().sendRawMessage(player, "&7Preis: &a" + plugin.getEconomyManager().formatBalance(auction.getPrice()));
        plugin.getMessageManager().sendRawMessage(player, "&7Status: " + (auction.isSold() ? "&a&lVERKAUFT" : "&e&lAKTIV"));
        plugin.getMessageManager().sendRawMessage(player, "&7Erstellt: &f" + new java.util.Date(auction.getCreatedAt()));
        
        if (auction.isSold()) {
            plugin.getMessageManager().sendRawMessage(player, "&7Käufer: &e" + 
                (auction.getBuyer() != null ? plugin.getServer().getOfflinePlayer(auction.getBuyer()).getName() : "Unbekannt"));
        } else {
            plugin.getMessageManager().sendRawMessage(player, "&7Läuft ab: &c" + new java.util.Date(auction.getExpiresAt()));
            plugin.getMessageManager().sendRawMessage(player, "&7Verbleibend: &c" + auction.getFormattedTimeRemaining());
        }
        
        playClickSound();
    }
    
    private String calculateSuccessRate() {
        int total = (myAuctions != null ? myAuctions.size() : 0) + (soldAuctions != null ? soldAuctions.size() : 0);
        if (total == 0) return "0";
        
        double rate = ((double) (soldAuctions != null ? soldAuctions.size() : 0) / total) * 100;
        return String.format("%.1f", rate);
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
