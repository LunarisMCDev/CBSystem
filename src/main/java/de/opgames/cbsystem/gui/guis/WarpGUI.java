package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WarpGUI extends BaseGUI {
    
    public WarpGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&d&lWarps", 54);
    }
    
    @Override
    protected void setupGUI() {
        // Info-Item
        setItem(4, Material.ENDER_PEARL, "&d&lÖffentliche Warps",
            List.of(
                "&7Teleportiere zu öffentlichen",
                "&7Orten auf dem Server!",
                "",
                "&7Klicke auf einen Warp zum",
                "&7Teleportieren."
            ));
        
        // Standard-Warps
        setupDefaultWarps();
        
        // Navigation
        setItem(45, createBackButton());
        setItem(53, createCloseButton());
        
        fillEmptySlots();
    }
    
    private void setupDefaultWarps() {
        // Spawn
        setItem(10, Material.BEACON, "&a&lSpawn",
            List.of(
                "&7Der Spawn-Punkt des Servers.",
                "&7Hier beginnt dein Abenteuer!",
                "",
                "&aLinksklick: &7Zum Spawn teleportieren"
            ));
        
        // Shop
        setItem(12, Material.EMERALD, "&e&lShop",
            List.of(
                "&7Der zentrale Handelsplatz",
                "&7für alle Items.",
                "",
                "&eLinksklick: &7Zum Shop teleportieren"
            ));
        
        // PvP-Arena (falls vorhanden)
        setItem(14, Material.DIAMOND_SWORD, "&c&lPvP-Arena",
            List.of(
                "&7Kämpfe gegen andere",
                "&7Spieler in der Arena!",
                "",
                "&c⚠ PvP aktiviert!",
                "",
                "&cLinksklick: &7Zur Arena teleportieren"
            ));
        
        // Farmwelt
        setItem(16, Material.WHEAT, "&6&lFarmwelt",
            List.of(
                "&7Eine Welt zum Farmen",
                "&7und Ressourcen sammeln.",
                "",
                "&6Linksklick: &7Zur Farmwelt teleportieren"
            ));
        
        // Events-Bereich
        setItem(28, Material.FIREWORK_ROCKET, "&5&lEvents",
            List.of(
                "&7Der Event-Bereich für",
                "&7besondere Veranstaltungen.",
                "",
                "&5Linksklick: &7Zum Event-Bereich teleportieren"
            ));
        
        // Nether-Portal
        setItem(30, Material.OBSIDIAN, "&4&lNether-Portal",
            List.of(
                "&7Sicherer Zugang zum Nether",
                "&7ohne eigenes Portal.",
                "",
                "&4Linksklick: &7Zum Nether teleportieren"
            ));
        
        // End-Portal
        setItem(32, Material.END_STONE, "&8&lEnd-Portal",
            List.of(
                "&7Direkter Zugang zum End",
                "&7und zum Enderdrachen.",
                "",
                "&8Linksklick: &7Zum End teleportieren"
            ));
        
        // Casino (falls vorhanden)
        setItem(34, Material.GOLD_BLOCK, "&6&lCasino",
            List.of(
                "&7Versuche dein Glück",
                "&7im Server-Casino!",
                "",
                "&6Linksklick: &7Zum Casino teleportieren"
            ));
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        String warpName = null;
        
        switch (slot) {
            case 10: // Spawn
                warpName = "spawn";
                break;
                
            case 12: // Shop
                warpName = "shop";
                break;
                
            case 14: // PvP-Arena
                warpName = "pvp";
                break;
                
            case 16: // Farmwelt
                warpName = "farm";
                break;
                
            case 28: // Events
                warpName = "events";
                break;
                
            case 30: // Nether
                warpName = "nether";
                break;
                
            case 32: // End
                warpName = "end";
                break;
                
            case 34: // Casino
                warpName = "casino";
                break;
                
            case 45: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                return;
                
            case 53: // Schließen
                closeGUI();
                return;
        }
        
        if (warpName != null) {
            teleportToWarp(warpName);
        }
    }
    
    private void teleportToWarp(String warpName) {
        // Hier würde normalerweise die Warp-Location aus der Datenbank geladen werden
        // Für die Demo verwenden wir Standard-Locations
        
        org.bukkit.Location warpLocation = getWarpLocation(warpName);
        
        if (warpLocation == null) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "warp.not-found", "name", warpName);
            return;
        }
        
        // Überprüfe Berechtigung
        if (!hasWarpPermission(warpName)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return;
        }
        
        // Überprüfe Cooldown
        if (hasWarpCooldown()) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "cooldown.active", 
                "time", String.valueOf(getWarpCooldownRemaining()));
            return;
        }
        
        closeGUI();
        setWarpCooldown();
        plugin.getTeleportManager().startTeleport(player, warpLocation, "Warp: " + warpName);
    }
    
    private org.bukkit.Location getWarpLocation(String warpName) {
        // Standard-Warp-Locations (würden normalerweise aus der Datenbank kommen)
        org.bukkit.World world = plugin.getServer().getWorld("world");
        if (world == null) return null;
        
        return switch (warpName) {
            case "spawn" -> new org.bukkit.Location(world, 0.5, 100, 0.5);
            case "shop" -> new org.bukkit.Location(world, 50.5, 100, 50.5);
            case "pvp" -> new org.bukkit.Location(world, -50.5, 100, -50.5);
            case "farm" -> new org.bukkit.Location(world, 100.5, 100, 100.5);
            case "events" -> new org.bukkit.Location(world, -100.5, 100, 100.5);
            default -> null;
        };
    }
    
    private boolean hasWarpPermission(String warpName) {
        // Überprüfe spezifische Warp-Berechtigung
        return player.hasPermission("cbsystem.warp.use") || 
               player.hasPermission("cbsystem.warp." + warpName);
    }
    
    private boolean hasWarpCooldown() {
        // Implementiere Cooldown-Check (vereinfacht)
        return false; // Für Demo deaktiviert
    }
    
    private long getWarpCooldownRemaining() {
        return 0; // Für Demo
    }
    
    private void setWarpCooldown() {
        // Implementiere Cooldown-Setting
        // Für Demo nicht implementiert
    }
}
