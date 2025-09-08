package de.opgames.cbsystem.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    
    private final UUID uuid;
    private String name;
    private double balance;
    private final Timestamp firstJoin;
    private Timestamp lastSeen;
    private long playTime;
    
    // Homes
    private final Map<String, HomeLocation> homes = new HashMap<>();
    
    // Back-Locations
    private final List<BackLocation> backLocations = new ArrayList<>();
    
    // TPA-Anfragen
    private final List<TPARequest> tpaRequests = new ArrayList<>();
    
    // Cooldowns
    private final Map<String, Long> cooldowns = new HashMap<>();
    
    public PlayerData(UUID uuid, String name, double balance, Timestamp firstJoin, Timestamp lastSeen, long playTime) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
        this.firstJoin = firstJoin != null ? firstJoin : new Timestamp(System.currentTimeMillis());
        this.lastSeen = lastSeen != null ? lastSeen : new Timestamp(System.currentTimeMillis());
        this.playTime = playTime;
    }
    
    // Getter und Setter
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = Math.max(0, balance); }
    public void addBalance(double amount) { setBalance(balance + amount); }
    public void subtractBalance(double amount) { setBalance(balance - amount); }
    public boolean hasBalance(double amount) { return balance >= amount; }
    
    public Timestamp getFirstJoin() { return firstJoin; }
    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }
    
    public long getPlayTime() { return playTime; }
    public void setPlayTime(long playTime) { this.playTime = playTime; }
    public void addPlayTime(long time) { this.playTime += time; }
    
    // Home-Management
    public Map<String, HomeLocation> getHomes() { return new HashMap<>(homes); }
    
    public boolean hasHome(String name) {
        return homes.containsKey(name.toLowerCase());
    }
    
    public HomeLocation getHome(String name) {
        return homes.get(name.toLowerCase());
    }
    
    public void addHome(String name, String world, double x, double y, double z, float yaw, float pitch) {
        homes.put(name.toLowerCase(), new HomeLocation(name, world, x, y, z, yaw, pitch));
    }
    
    public void addHome(String name, Location location) {
        addHome(name, location.getWorld().getName(), location.getX(), location.getY(), 
               location.getZ(), location.getYaw(), location.getPitch());
    }
    
    public boolean removeHome(String name) {
        return homes.remove(name.toLowerCase()) != null;
    }
    
    public int getHomeCount() {
        return homes.size();
    }
    
    // Back-Location-Management
    public List<BackLocation> getBackLocations() { return new ArrayList<>(backLocations); }
    
    public BackLocation getLastBackLocation() {
        return backLocations.isEmpty() ? null : backLocations.get(0);
    }
    
    public void addBackLocation(String world, double x, double y, double z, float yaw, float pitch, String reason) {
        backLocations.add(0, new BackLocation(world, x, y, z, yaw, pitch, reason, System.currentTimeMillis()));
        
        // Begrenze die Anzahl der Back-Locations
        if (backLocations.size() > 10) {
            backLocations.subList(10, backLocations.size()).clear();
        }
    }
    
    public void addBackLocation(Location location, String reason) {
        addBackLocation(location.getWorld().getName(), location.getX(), location.getY(),
                       location.getZ(), location.getYaw(), location.getPitch(), reason);
    }
    
    // TPA-Request-Management
    public List<TPARequest> getTpaRequests() { return new ArrayList<>(tpaRequests); }
    
    public void addTPARequest(UUID requester, TPARequest.Type type) {
        // Entferne alte Anfrage von diesem Spieler
        tpaRequests.removeIf(request -> request.getRequester().equals(requester));
        
        // Füge neue Anfrage hinzu
        tpaRequests.add(new TPARequest(requester, type, System.currentTimeMillis()));
    }
    
    public TPARequest getTPARequest(UUID requester) {
        return tpaRequests.stream()
            .filter(request -> request.getRequester().equals(requester))
            .findFirst()
            .orElse(null);
    }
    
    public boolean removeTPARequest(UUID requester) {
        return tpaRequests.removeIf(request -> request.getRequester().equals(requester));
    }
    
    public void clearExpiredTPARequests(long timeoutMs) {
        long now = System.currentTimeMillis();
        tpaRequests.removeIf(request -> (now - request.getTimestamp()) > timeoutMs);
    }
    
    // Cooldown-Management
    public boolean hasCooldown(String type) {
        Long cooldownEnd = cooldowns.get(type);
        if (cooldownEnd == null) return false;
        
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(type);
            return false;
        }
        return true;
    }
    
    public long getCooldownRemaining(String type) {
        Long cooldownEnd = cooldowns.get(type);
        if (cooldownEnd == null) return 0;
        
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000); // Sekunden
    }
    
    public void setCooldown(String type, long seconds) {
        cooldowns.put(type, System.currentTimeMillis() + (seconds * 1000));
    }
    
    public void removeCooldown(String type) {
        cooldowns.remove(type);
    }
    
    // Utility-Methoden
    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }
    
    // Innere Klassen
    public static class HomeLocation {
        private final String name;
        private final String world;
        private final double x, y, z;
        private final float yaw, pitch;
        private final long created;
        
        public HomeLocation(String name, String world, double x, double y, double z, float yaw, float pitch) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.created = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public long getCreated() { return created; }
        
        public Location toBukkitLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld == null) return null;
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
        
        public boolean isWorldLoaded() {
            return Bukkit.getWorld(world) != null;
        }
    }
    
    public static class BackLocation {
        private final String world;
        private final double x, y, z;
        private final float yaw, pitch;
        private final String reason;
        private final long timestamp;
        
        public BackLocation(String world, double x, double y, double z, float yaw, float pitch, String reason, long timestamp) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.reason = reason;
            this.timestamp = timestamp;
        }
        
        public String getWorld() { return world; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public float getYaw() { return yaw; }
        public float getPitch() { return pitch; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
        
        public Location toBukkitLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld == null) return null;
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        }
        
        public boolean isWorldLoaded() {
            return Bukkit.getWorld(world) != null;
        }
    }
    
    public static class TPARequest {
        private final UUID requester;
        private final Type type;
        private final long timestamp;
        
        public enum Type {
            TPA,      // Requester möchte zum Target
            TPAHERE   // Requester möchte Target zu sich
        }
        
        public TPARequest(UUID requester, Type type, long timestamp) {
            this.requester = requester;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public UUID getRequester() { return requester; }
        public Type getType() { return type; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - timestamp) > timeoutMs;
        }
    }
}
