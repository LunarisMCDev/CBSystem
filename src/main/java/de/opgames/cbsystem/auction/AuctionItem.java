package de.opgames.cbsystem.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {
    
    private final int id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long createdAt;
    private final long expiresAt;
    private boolean sold;
    private UUID buyer;
    
    public AuctionItem(int id, UUID seller, String sellerName, ItemStack item, double price, long duration) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + duration;
        this.sold = false;
        this.buyer = null;
    }
    
    // Getters
    public int getId() { return id; }
    public UUID getSeller() { return seller; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isSold() { return sold; }
    public UUID getBuyer() { return buyer; }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt && !sold;
    }
    
    public long getTimeRemaining() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
    
    public void markAsSold(UUID buyer) {
        this.sold = true;
        this.buyer = buyer;
    }
    
    public String getFormattedTimeRemaining() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) return "Abgelaufen";
        
        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
