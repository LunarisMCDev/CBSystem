package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TeleportGUI extends BaseGUI {
    
    public TeleportGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&c&lTeleportation", 27);
    }
    
    @Override
    protected void setupGUI() {
        // TPA senden
        setItem(10, Material.ENDER_PEARL, "&a&lTPA senden",
            List.of(
                "&7Sende eine Teleportations-",
                "&7anfrage an einen Spieler.",
                "",
                "&aLinksklick: &7Spieler auswählen"
            ));
        
        // TPA hier
        setItem(11, Material.ENDER_EYE, "&b&lTPA hier",
            List.of(
                "&7Lade einen Spieler zu",
                "&7dir ein.",
                "",
                "&bLinksklick: &7Spieler auswählen"
            ));
        
        // TPA annehmen/ablehnen
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null && !playerData.getTpaRequests().isEmpty()) {
            setItem(13, Material.GREEN_CONCRETE, "&a&lTPA annehmen",
                List.of(
                    "&7Du hast &e" + playerData.getTpaRequests().size() + 
                    "&7 offene Anfrage" + (playerData.getTpaRequests().size() == 1 ? "" : "n") + ".",
                    "",
                    "&aLinksklick: &7Anfragen verwalten"
                ));
        } else {
            setItem(13, Material.GRAY_CONCRETE, "&7Keine TPA-Anfragen",
                List.of(
                    "&7Du hast derzeit keine",
                    "&7offenen Teleportations-",
                    "&7anfragen."
                ));
        }
        
        // Back-Funktion
        PlayerData.BackLocation lastBack = playerData != null ? playerData.getLastBackLocation() : null;
        if (lastBack != null && lastBack.isWorldLoaded()) {
            setItem(15, Material.COMPASS, "&e&lZurück",
                List.of(
                    "&7Teleportiere zur letzten",
                    "&7Position zurück.",
                    "",
                    "&7Letzte Position:",
                    "&e" + lastBack.getWorld() + " (" + (int)lastBack.getX() + ", " + 
                    (int)lastBack.getY() + ", " + (int)lastBack.getZ() + ")",
                    "&7Grund: &e" + (lastBack.getReason() != null ? lastBack.getReason() : "Unbekannt"),
                    "",
                    "&eLinksklick: &7Zurück teleportieren"
                ));
        } else {
            setItem(15, Material.BARRIER, "&c&lKeine Back-Position",
                List.of(
                    "&7Du hast keine vorherige",
                    "&7Position gespeichert."
                ));
        }
        
        // Zufällige Teleportation
        setItem(16, Material.FIREWORK_ROCKET, "&d&lZufällige Teleportation",
            List.of(
                "&7Teleportiere dich zu einem",
                "&7zufälligen Ort in der Welt.",
                "",
                "&c⚠ Kann gefährlich sein!",
                "",
                "&dLinksklick: &7Zufällig teleportieren"
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
            case 10: // TPA senden
                plugin.getGUIManager().openPlayerSelectionGUI(player, "&aTPA senden - Spieler auswählen",
                    selectedPlayer -> {
                        closeGUI();
                        sendTPARequest(selectedPlayer, false);
                    });
                break;
                
            case 11: // TPA hier
                plugin.getGUIManager().openPlayerSelectionGUI(player, "&bTPA hier - Spieler auswählen",
                    selectedPlayer -> {
                        closeGUI();
                        sendTPARequest(selectedPlayer, true);
                    });
                break;
                
            case 13: // TPA-Anfragen verwalten
                PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
                if (playerData != null && !playerData.getTpaRequests().isEmpty()) {
                    // Hier würde ein TPA-Anfragen-Management-GUI geöffnet
                    plugin.getMessageManager().sendMessage(player, "tpa.manage-requests-coming-soon");
                }
                break;
                
            case 15: // Back
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                if (data != null && data.getLastBackLocation() != null) {
                    closeGUI();
                    plugin.getTeleportManager().teleportToLastLocation(player);
                }
                break;
                
            case 16: // Zufällige Teleportation
                closeGUI();
                performRandomTeleport();
                break;
                
            case 18: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 26: // Schließen
                closeGUI();
                break;
        }
    }
    
    private void sendTPARequest(Player targetPlayer, boolean isTPAHere) {
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "general.player-not-online", 
                "player", targetPlayer != null ? targetPlayer.getName() : "Unbekannt");
            return;
        }
        
        if (targetPlayer.equals(player)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "tpa.cannot-request-self");
            return;
        }
        
        // Überprüfe Cooldown
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData != null && playerData.hasCooldown("tpa")) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "cooldown.active",
                "time", String.valueOf(playerData.getCooldownRemaining("tpa")));
            return;
        }
        
        // Sende TPA-Anfrage
        PlayerData targetData = plugin.getPlayerDataManager().getPlayerData(targetPlayer);
        if (targetData != null) {
            PlayerData.TPARequest.Type requestType = isTPAHere ? 
                PlayerData.TPARequest.Type.TPAHERE : PlayerData.TPARequest.Type.TPA;
            
            targetData.addTPARequest(player.getUniqueId(), requestType);
            
            // Setze Cooldown
            if (playerData != null) {
                playerData.setCooldown("tpa", 30); // 30 Sekunden Cooldown
            }
            
            playSuccessSound();
            plugin.getMessageManager().sendMessage(player, "tpa.request-sent", 
                "player", targetPlayer.getName());
            
            if (isTPAHere) {
                plugin.getMessageManager().sendMessage(targetPlayer, "tpa.request-received-here",
                    "player", player.getName());
            } else {
                plugin.getMessageManager().sendMessage(targetPlayer, "tpa.request-received",
                    "player", player.getName());
            }
            
            plugin.getMessageManager().sendMessage(targetPlayer, "tpa.request-info");
        }
    }
    
    private void performRandomTeleport() {
        // Vereinfachte zufällige Teleportation
        org.bukkit.World world = player.getWorld();
        
        // Zufällige Koordinaten generieren (innerhalb von -1000 bis 1000)
        int x = (int) (Math.random() * 2000) - 1000;
        int z = (int) (Math.random() * 2000) - 1000;
        int y = world.getHighestBlockYAt(x, z) + 1;
        
        org.bukkit.Location randomLocation = new org.bukkit.Location(world, x + 0.5, y, z + 0.5);
        
        // Überprüfe ob die Position sicher ist
        if (!plugin.getTeleportManager().isSafeLocation(randomLocation)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "teleport.unsafe-location");
            return;
        }
        
        playSuccessSound();
        plugin.getTeleportManager().startTeleport(player, randomLocation, "Zufällige Teleportation");
    }
}
