package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerActionsGUI extends BaseGUI {
    
    private final Player targetPlayer;
    
    public PlayerActionsGUI(CBSystem plugin, Player player, Player targetPlayer) {
        super(plugin, player, "&c&l" + targetPlayer.getName() + " &8- &7Aktionen", 45);
        this.targetPlayer = targetPlayer;
    }
    
    @Override
    protected void setupGUI() {
        // Header: Spieler-Info
        double balance = plugin.getEconomyManager().getBalance(targetPlayer.getUniqueId());
        setItem(4, Material.PLAYER_HEAD, "&e" + targetPlayer.getName(),
            List.of(
                "&7UUID: &f" + targetPlayer.getUniqueId().toString().substring(0, 8) + "...",
                "&7Welt: &e" + targetPlayer.getWorld().getName(),
                "&7Position: &f" + Math.round(targetPlayer.getLocation().getX()) + ", " + 
                    Math.round(targetPlayer.getLocation().getY()) + ", " + 
                    Math.round(targetPlayer.getLocation().getZ()),
                "&7Guthaben: &a" + plugin.getEconomyManager().formatBalance(balance),
                "&7Health: &c" + Math.round(targetPlayer.getHealth()) + "/20",
                "&7Level: &d" + targetPlayer.getLevel()
            ));
        
        // Teleportation-Aktionen
        setItem(10, Material.ENDER_PEARL, "&a&lTeleportation",
            List.of(
                "&7Teleportiere zu oder von dem Spieler",
                "",
                "&aLinksklick: &7Zu Spieler teleportieren",
                "&eShift+Links: &7Spieler zu dir teleportieren"
            ));
        
        setItem(11, Material.COMPASS, "&b&lPosition Info",
            List.of(
                "&7Zeige detaillierte Position",
                "&7und Welt-Informationen",
                "",
                "&bLinksklick: &7Position anzeigen"
            ));
        
        // Moderation-Aktionen
        setItem(12, Material.IRON_BOOTS, "&c&lKick",
            List.of(
                "&7Kicke den Spieler vom Server",
                "",
                "&cLinksklick: &7Spieler kicken"
            ));
        
        setItem(13, Material.BARRIER, "&4&lBan",
            List.of(
                "&7Banne den Spieler permanent",
                "&c&lVorsicht: Permanente Aktion!",
                "",
                "&4Linksklick: &7Spieler bannen"
            ));
        
        setItem(14, Material.CLOCK, "&6&lTemp Ban",
            List.of(
                "&7Banne den Spieler temporär",
                "&7(1h, 1d, 1w, 1m)",
                "",
                "&6Linksklick: &7Temp-Ban Menü"
            ));
        
        // Utility-Aktionen
        setItem(19, Material.GOLDEN_APPLE, "&a&lHeal",
            List.of(
                "&7Heile den Spieler vollständig",
                "",
                "&aLinksklick: &7Spieler heilen"
            ));
        
        setItem(20, Material.BREAD, "&e&lFeed",
            List.of(
                "&7Sättige den Spieler vollständig",
                "",
                "&eLinksklick: &7Spieler sättigen"
            ));
        
        setItem(21, Material.ELYTRA, "&b&lFly Toggle",
            List.of(
                "&7Aktiviere/Deaktiviere Flugmodus",
                "&7Status: " + (targetPlayer.getAllowFlight() ? "&aAktiv" : "&cInaktiv"),
                "",
                "&bLinksklick: &7Flugmodus umschalten"
            ));
        
        setItem(22, Material.GRASS_BLOCK, "&d&lGamemode",
            List.of(
                "&7Ändere den Spielmodus",
                "&7Aktuell: &e" + targetPlayer.getGameMode().name(),
                "",
                "&dLinksklick: &7Gamemode-Menü"
            ));
        
        setItem(23, Material.EXPERIENCE_BOTTLE, "&5&lClear Inventory",
            List.of(
                "&7Leere das Inventar des Spielers",
                "&c&lVorsicht: Nicht rückgängig machbar!",
                "",
                "&5Linksklick: &7Inventar leeren"
            ));
        
        // Economy-Aktionen
        setItem(28, Material.GOLD_INGOT, "&6&lGeld setzen",
            List.of(
                "&7Setze das Guthaben des Spielers",
                "&7Aktuell: &a" + plugin.getEconomyManager().formatBalance(balance),
                "",
                "&6Linksklick: &7Geld-Menü öffnen"
            ));
        
        setItem(29, Material.EMERALD, "&a&lGeld geben",
            List.of(
                "&7Gib dem Spieler Geld",
                "",
                "&aLinksklick: &7Betrag eingeben"
            ));
        
        setItem(30, Material.REDSTONE, "&c&lGeld entziehen",
            List.of(
                "&7Entziehe dem Spieler Geld",
                "",
                "&cLinksklick: &7Betrag eingeben"
            ));
        
        // Navigation
        setItem(36, createBackButton());
        setItem(40, createCloseButton());
        
        // Spezial-Aktionen
        setItem(44, Material.BOOK, "&f&lSpieler-Logs",
            List.of(
                "&7Zeige Aktions-Logs des Spielers",
                "&7(Befehle, Teleports, etc.)",
                "",
                "&fLinksklick: &7Logs anzeigen"
            ));
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        if (!player.hasPermission("cbsystem.admin.playeractions")) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.no-permission");
            return;
        }
        
        // Prüfe ob Ziel-Spieler noch online ist
        if (!targetPlayer.isOnline()) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", targetPlayer.getName());
            plugin.getGUIManager().openPlayerManagementGUI(player);
            return;
        }
        
        playClickSound();
        
        switch (slot) {
            case 10: // Teleportation
                if (isShiftClick) {
                    // Spieler zu Admin teleportieren
                    targetPlayer.teleport(player.getLocation());
                    plugin.getMessageManager().sendMessage(player, "admin.player-teleported-to-you",
                        "player", targetPlayer.getName());
                    plugin.getMessageManager().sendMessage(targetPlayer, "admin.teleported-to-admin",
                        "admin", player.getName());
                } else {
                    // Admin zu Spieler teleportieren
                    player.teleport(targetPlayer.getLocation());
                    plugin.getMessageManager().sendMessage(player, "admin.teleported-to-player",
                        "player", targetPlayer.getName());
                }
                playSuccessSound();
                break;
                
            case 11: // Position Info
                showPositionInfo();
                break;
                
            case 12: // Kick
                kickPlayer();
                break;
                
            case 13: // Ban
                banPlayer();
                break;
                
            case 14: // Temp Ban
                plugin.getMessageManager().sendMessage(player, "admin.tempban-coming-soon");
                break;
                
            case 19: // Heal
                healPlayer();
                break;
                
            case 20: // Feed
                feedPlayer();
                break;
                
            case 21: // Fly Toggle
                toggleFly();
                break;
                
            case 22: // Gamemode
                openGamemodeMenu();
                break;
                
            case 23: // Clear Inventory
                clearInventory();
                break;
                
            case 28: // Geld setzen
                plugin.getMessageManager().sendMessage(player, "admin.money-set-coming-soon");
                break;
                
            case 29: // Geld geben
                plugin.getMessageManager().sendMessage(player, "admin.money-give-coming-soon");
                break;
                
            case 30: // Geld entziehen
                plugin.getMessageManager().sendMessage(player, "admin.money-take-coming-soon");
                break;
                
            case 36: // Zurück
                plugin.getGUIManager().openPlayerManagementGUI(player);
                break;
                
            case 40: // Schließen
                closeGUI();
                break;
                
            case 44: // Spieler-Logs
                plugin.getMessageManager().sendMessage(player, "admin.player-logs-coming-soon");
                break;
        }
    }
    
    private void showPositionInfo() {
        plugin.getMessageManager().sendRawMessage(player, "&6&l=== Position von " + targetPlayer.getName() + " ===");
        plugin.getMessageManager().sendRawMessage(player, "&7Welt: &e" + targetPlayer.getWorld().getName());
        plugin.getMessageManager().sendRawMessage(player, "&7X: &f" + targetPlayer.getLocation().getX());
        plugin.getMessageManager().sendRawMessage(player, "&7Y: &f" + targetPlayer.getLocation().getY());
        plugin.getMessageManager().sendRawMessage(player, "&7Z: &f" + targetPlayer.getLocation().getZ());
        plugin.getMessageManager().sendRawMessage(player, "&7Yaw: &f" + targetPlayer.getLocation().getYaw());
        plugin.getMessageManager().sendRawMessage(player, "&7Pitch: &f" + targetPlayer.getLocation().getPitch());
        playClickSound();
    }
    
    private void kickPlayer() {
        targetPlayer.kickPlayer(plugin.getMessageManager().colorize("&cDu wurdest von einem Administrator gekickt!"));
        plugin.getMessageManager().sendMessage(player, "admin.player-kicked", "player", targetPlayer.getName());
        playSuccessSound();
        closeGUI();
    }
    
    private void banPlayer() {
        String reason = plugin.getMessageManager().colorize("&cDu wurdest von einem Administrator gebannt!");
        targetPlayer.ban(reason, (java.time.Instant) null, "CBSystem Admin");
        plugin.getMessageManager().sendMessage(player, "admin.player-banned", "player", targetPlayer.getName());
        playSuccessSound();
        closeGUI();
    }
    
    private void healPlayer() {
        targetPlayer.setHealth(20.0);
        targetPlayer.setFoodLevel(20);
        targetPlayer.setSaturation(20.0f);
        
        plugin.getMessageManager().sendMessage(player, "admin.healed-other", "player", targetPlayer.getName());
        plugin.getMessageManager().sendMessage(targetPlayer, "admin.healed");
        playSuccessSound();
        update();
    }
    
    private void feedPlayer() {
        targetPlayer.setFoodLevel(20);
        targetPlayer.setSaturation(20.0f);
        
        plugin.getMessageManager().sendMessage(player, "admin.fed-other", "player", targetPlayer.getName());
        plugin.getMessageManager().sendMessage(targetPlayer, "admin.fed");
        playSuccessSound();
        update();
    }
    
    private void toggleFly() {
        boolean newFlyState = !targetPlayer.getAllowFlight();
        targetPlayer.setAllowFlight(newFlyState);
        
        if (newFlyState) {
            plugin.getMessageManager().sendMessage(player, "admin.fly-enabled-other", "player", targetPlayer.getName());
            plugin.getMessageManager().sendMessage(targetPlayer, "admin.fly-enabled");
        } else {
            targetPlayer.setFlying(false);
            plugin.getMessageManager().sendMessage(player, "admin.fly-disabled-other", "player", targetPlayer.getName());
            plugin.getMessageManager().sendMessage(targetPlayer, "admin.fly-disabled");
        }
        
        playSuccessSound();
        update();
    }
    
    private void openGamemodeMenu() {
        // Zykliere durch die Gamemodes
        GameMode current = targetPlayer.getGameMode();
        GameMode next = switch (current) {
            case SURVIVAL -> GameMode.CREATIVE;
            case CREATIVE -> GameMode.ADVENTURE;
            case ADVENTURE -> GameMode.SPECTATOR;
            case SPECTATOR -> GameMode.SURVIVAL;
        };
        
        targetPlayer.setGameMode(next);
        
        plugin.getMessageManager().sendMessage(player, "admin.gamemode-changed-other", 
            "player", targetPlayer.getName(), "gamemode", next.name());
        plugin.getMessageManager().sendMessage(targetPlayer, "admin.gamemode-changed", 
            "gamemode", next.name());
        
        playSuccessSound();
        update();
    }
    
    private void clearInventory() {
        targetPlayer.getInventory().clear();
        targetPlayer.updateInventory();
        
        plugin.getMessageManager().sendMessage(player, "admin.inventory-cleared-other", 
            "player", targetPlayer.getName());
        plugin.getMessageManager().sendMessage(targetPlayer, "admin.inventory-cleared");
        
        playSuccessSound();
    }
}
