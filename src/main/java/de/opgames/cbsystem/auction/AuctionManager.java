package de.opgames.cbsystem.auction;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AuctionManager {
    
    private final CBSystem plugin;
    private final Map<Integer, AuctionItem> activeAuctions;
    private final AtomicInteger nextId;
    
    // Konfiguration
    private static final long DEFAULT_AUCTION_DURATION = 24 * 60 * 60 * 1000; // 24 Stunden
    private static final double AUCTION_TAX_PERCENTAGE = 0.05; // 5% Steuer
    private static final int MAX_AUCTIONS_PER_PLAYER = 5;
    
    public AuctionManager(CBSystem plugin) {
        this.plugin = plugin;
        this.activeAuctions = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(1);
        
        // Cleanup-Task alle 5 Minuten
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredAuctions, 6000L, 6000L);
    }
    
    public boolean createAuction(Player seller, ItemStack item, double price) {
        return createAuction(seller, item, price, DEFAULT_AUCTION_DURATION);
    }
    
    public boolean createAuction(Player seller, ItemStack item, double price, long duration) {
        // Validierung
        if (item == null || item.getAmount() == 0) {
            plugin.getMessageManager().sendMessage(seller, "auction.invalid-item");
            return false;
        }
        
        if (price <= 0) {
            plugin.getMessageManager().sendMessage(seller, "auction.invalid-price");
            return false;
        }
        
        if (getPlayerActiveAuctions(seller.getUniqueId()).size() >= MAX_AUCTIONS_PER_PLAYER) {
            plugin.getMessageManager().sendMessage(seller, "auction.max-auctions-reached", 
                "max", String.valueOf(MAX_AUCTIONS_PER_PLAYER));
            return false;
        }
        
        // Steuer berechnen
        double tax = price * AUCTION_TAX_PERCENTAGE;
        if (!plugin.getEconomyManager().hasBalance(seller.getUniqueId(), tax)) {
            plugin.getMessageManager().sendMessage(seller, "auction.insufficient-funds-tax",
                "tax", plugin.getEconomyManager().formatBalance(tax));
            return false;
        }
        
        // Item aus Inventar entfernen
        if (!seller.getInventory().containsAtLeast(item, item.getAmount())) {
            plugin.getMessageManager().sendMessage(seller, "auction.item-not-found");
            return false;
        }
        
        seller.getInventory().removeItem(item);
        
        // Steuer abziehen
        plugin.getEconomyManager().withdrawBalance(seller.getUniqueId(), tax, 
            "Auktionshaus-Steuer für: " + item.getType().name());
        
        // Auktion erstellen
        int id = nextId.getAndIncrement();
        AuctionItem auction = new AuctionItem(id, seller.getUniqueId(), seller.getName(), item, price, duration);
        activeAuctions.put(id, auction);
        
        plugin.getMessageManager().sendMessage(seller, "auction.created",
            "item", getItemDisplayName(item),
            "price", plugin.getEconomyManager().formatBalance(price),
            "tax", plugin.getEconomyManager().formatBalance(tax));
        
        return true;
    }
    
    public boolean buyAuction(Player buyer, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        
        if (auction == null) {
            plugin.getMessageManager().sendMessage(buyer, "auction.not-found");
            return false;
        }
        
        if (auction.isSold()) {
            plugin.getMessageManager().sendMessage(buyer, "auction.already-sold");
            return false;
        }
        
        if (auction.isExpired()) {
            plugin.getMessageManager().sendMessage(buyer, "auction.expired");
            return false;
        }
        
        if (auction.getSeller().equals(buyer.getUniqueId())) {
            plugin.getMessageManager().sendMessage(buyer, "auction.cannot-buy-own");
            return false;
        }
        
        // Geld prüfen
        if (!plugin.getEconomyManager().hasBalance(buyer.getUniqueId(), auction.getPrice())) {
            plugin.getMessageManager().sendMessage(buyer, "auction.insufficient-funds",
                "price", plugin.getEconomyManager().formatBalance(auction.getPrice()));
            return false;
        }
        
        // Inventar-Platz prüfen
        if (!hasInventorySpace(buyer, auction.getItem())) {
            plugin.getMessageManager().sendMessage(buyer, "auction.inventory-full");
            return false;
        }
        
        // Transaktion durchführen
        plugin.getEconomyManager().withdrawBalance(buyer.getUniqueId(), auction.getPrice(),
            "Auktionshaus-Kauf: " + auction.getItem().getType().name());
        
        plugin.getEconomyManager().addBalance(auction.getSeller(), auction.getPrice(),
            "Auktionshaus-Verkauf: " + auction.getItem().getType().name());
        
        buyer.getInventory().addItem(auction.getItem());
        auction.markAsSold(buyer.getUniqueId());
        
        // Nachrichten senden
        plugin.getMessageManager().sendMessage(buyer, "auction.purchased",
            "item", getItemDisplayName(auction.getItem()),
            "price", plugin.getEconomyManager().formatBalance(auction.getPrice()),
            "seller", auction.getSellerName());
        
        // Verkäufer benachrichtigen (wenn online)
        Player seller = Bukkit.getPlayer(auction.getSeller());
        if (seller != null && seller.isOnline()) {
            plugin.getMessageManager().sendMessage(seller, "auction.sold",
                "item", getItemDisplayName(auction.getItem()),
                "price", plugin.getEconomyManager().formatBalance(auction.getPrice()),
                "buyer", buyer.getName());
        }
        
        return true;
    }
    
    public boolean cancelAuction(Player seller, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        
        if (auction == null) {
            plugin.getMessageManager().sendMessage(seller, "auction.not-found");
            return false;
        }
        
        if (!auction.getSeller().equals(seller.getUniqueId())) {
            plugin.getMessageManager().sendMessage(seller, "auction.not-your-auction");
            return false;
        }
        
        if (auction.isSold()) {
            plugin.getMessageManager().sendMessage(seller, "auction.already-sold");
            return false;
        }
        
        // Item zurückgeben
        if (hasInventorySpace(seller, auction.getItem())) {
            seller.getInventory().addItem(auction.getItem());
        } else {
            // Item droppen wenn kein Platz
            seller.getWorld().dropItemNaturally(seller.getLocation(), auction.getItem());
            plugin.getMessageManager().sendMessage(seller, "auction.item-dropped");
        }
        
        activeAuctions.remove(auctionId);
        
        plugin.getMessageManager().sendMessage(seller, "auction.cancelled",
            "item", getItemDisplayName(auction.getItem()));
        
        return true;
    }
    
    public List<AuctionItem> getActiveAuctions() {
        return activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    public List<AuctionItem> getPlayerActiveAuctions(UUID playerId) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.getSeller().equals(playerId) && !auction.isSold() && !auction.isExpired())
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    public List<AuctionItem> getPlayerSoldAuctions(UUID playerId) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.getSeller().equals(playerId) && auction.isSold())
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    private void cleanupExpiredAuctions() {
        List<AuctionItem> expired = activeAuctions.values().stream()
            .filter(AuctionItem::isExpired)
            .toList();
        
        for (AuctionItem auction : expired) {
            // Item an Verkäufer zurückgeben (wenn online)
            Player seller = Bukkit.getPlayer(auction.getSeller());
            if (seller != null && seller.isOnline()) {
                if (hasInventorySpace(seller, auction.getItem())) {
                    seller.getInventory().addItem(auction.getItem());
                    plugin.getMessageManager().sendMessage(seller, "auction.expired-returned",
                        "item", getItemDisplayName(auction.getItem()));
                } else {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), auction.getItem());
                    plugin.getMessageManager().sendMessage(seller, "auction.expired-dropped",
                        "item", getItemDisplayName(auction.getItem()));
                }
            }
            
            activeAuctions.remove(auction.getId());
        }
        
        if (!expired.isEmpty()) {
            plugin.getLogger().info("Auktionshaus: " + expired.size() + " abgelaufene Auktionen bereinigt.");
        }
    }
    
    private boolean hasInventorySpace(Player player, ItemStack item) {
        return player.getInventory().firstEmpty() != -1 || 
               canStackInExistingSlots(player, item);
    }
    
    private boolean canStackInExistingSlots(Player player, ItemStack item) {
        int remainingAmount = item.getAmount();
        
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                int canAdd = item.getMaxStackSize() - invItem.getAmount();
                remainingAmount -= canAdd;
                if (remainingAmount <= 0) return true;
            }
        }
        
        return false;
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
            default -> item.getType().name().toLowerCase().replace("_", " ");
        };
    }
    
    public int getActiveAuctionCount() {
        return (int) activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .count();
    }
    
    public AuctionItem getAuction(int id) {
        return activeAuctions.get(id);
    }
    
    // Erweiterte Suchfunktionen
    public List<AuctionItem> searchAuctions(String searchTerm) {
        String term = searchTerm.toLowerCase();
        return activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .filter(auction -> getItemDisplayName(auction.getItem()).toLowerCase().contains(term) ||
                             auction.getSellerName().toLowerCase().contains(term))
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    public List<AuctionItem> getAuctionsByCategory(String category) {
        return activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .filter(auction -> getItemCategory(auction.getItem()).equalsIgnoreCase(category))
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    public List<AuctionItem> getAuctionsByPriceRange(double minPrice, double maxPrice) {
        return activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .filter(auction -> auction.getPrice() >= minPrice && auction.getPrice() <= maxPrice)
            .sorted((a, b) -> Double.compare(a.getPrice(), b.getPrice()))
            .toList();
    }
    
    public List<AuctionItem> getAuctionsBySeller(UUID sellerId) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.getSeller().equals(sellerId) && !auction.isSold() && !auction.isExpired())
            .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
            .toList();
    }
    
    // Kategorien-System
    private String getItemCategory(ItemStack item) {
        return switch (item.getType()) {
            case DIAMOND, EMERALD, GOLD_INGOT, IRON_INGOT, NETHERITE_INGOT -> "Wertvoll";
            case STONE, COBBLESTONE, DIRT, SAND, GRAVEL -> "Baumaterial";
            case WHEAT, CARROT, POTATO, BEETROOT, BREAD -> "Nahrung";
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG -> "Holz";
            case DIAMOND_SWORD, DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SHOVEL, DIAMOND_HOE -> "Diamant-Werkzeuge";
            case IRON_SWORD, IRON_PICKAXE, IRON_AXE, IRON_SHOVEL, IRON_HOE -> "Eisen-Werkzeuge";
            case ENCHANTED_BOOK -> "Verzauberungen";
            case POTION, SPLASH_POTION, LINGERING_POTION -> "Tränke";
            case SPAWNER -> "Spawner";
            default -> "Sonstiges";
        };
    }
    
    // Statistiken
    public Map<String, Integer> getCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (AuctionItem auction : activeAuctions.values()) {
            if (!auction.isSold() && !auction.isExpired()) {
                String category = getItemCategory(auction.getItem());
                stats.put(category, stats.getOrDefault(category, 0) + 1);
            }
        }
        return stats;
    }
    
    public double getTotalValue() {
        return activeAuctions.values().stream()
            .filter(auction -> !auction.isSold() && !auction.isExpired())
            .mapToDouble(AuctionItem::getPrice)
            .sum();
    }
    
    public double getAveragePrice() {
        List<AuctionItem> active = getActiveAuctions();
        if (active.isEmpty()) return 0.0;
        
        return active.stream()
            .mapToDouble(AuctionItem::getPrice)
            .average()
            .orElse(0.0);
    }
    
    // Admin-Funktionen
    public boolean removeAuction(int auctionId) {
        AuctionItem auction = activeAuctions.remove(auctionId);
        if (auction != null) {
            // Item an Verkäufer zurückgeben
            Player seller = Bukkit.getPlayer(auction.getSeller());
            if (seller != null && seller.isOnline()) {
                if (hasInventorySpace(seller, auction.getItem())) {
                    seller.getInventory().addItem(auction.getItem());
                } else {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), auction.getItem());
                }
            }
            return true;
        }
        return false;
    }
    
    public void clearAllAuctions() {
        for (AuctionItem auction : new ArrayList<>(activeAuctions.values())) {
            removeAuction(auction.getId());
        }
    }
    
    // Top-Seller
    public Map<UUID, Integer> getTopSellers(int limit) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.isSold())
            .collect(Collectors.groupingBy(
                AuctionItem::getSeller,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
}
