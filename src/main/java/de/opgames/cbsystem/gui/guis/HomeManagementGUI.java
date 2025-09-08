package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.data.PlayerData;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeManagementGUI extends BaseGUI {
    
    private int currentPage = 0;
    private final int homesPerPage = 28; // 4 Reihen à 7 Items
    
    public HomeManagementGUI(CBSystem plugin, Player player) {
        super(plugin, player, "&b&lHome-Verwaltung", 54);
    }
    
    @Override
    protected void setupGUI() {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            setItem(22, Material.BARRIER, "&cFehler",
                List.of("&7Spielerdaten konnten nicht geladen werden!"));
            return;
        }
        
        Map<String, PlayerData.HomeLocation> homes = playerData.getHomes();
        
        // Info-Item
        setItem(4, Material.RED_BED, "&b&lDeine Homes",
            List.of(
                "&7Homes: &e" + homes.size() + "&7/&e" + plugin.getConfigManager().getMaxHomes(),
                "&7Kosten pro Home: &a" + plugin.getEconomyManager().formatBalance(plugin.getConfigManager().getCostPerHome()),
                "",
                "&7Klicke auf ein Home um dich",
                "&7dorthin zu teleportieren!"
            ));
        
        // Home-Liste anzeigen
        List<Map.Entry<String, PlayerData.HomeLocation>> homeList = new ArrayList<>(homes.entrySet());
        int startIndex = currentPage * homesPerPage;
        int slot = 10;
        
        for (int i = startIndex; i < Math.min(startIndex + homesPerPage, homeList.size()); i++) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2; // Überspringe Rand-Slots
            if (slot >= 44) break;
            
            Map.Entry<String, PlayerData.HomeLocation> entry = homeList.get(i);
            String homeName = entry.getKey();
            PlayerData.HomeLocation home = entry.getValue();
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Welt: &e" + home.getWorld());
            lore.add("&7Position: &e" + (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ());
            
            if (!home.isWorldLoaded()) {
                lore.add("&c⚠ Welt nicht geladen!");
            }
            
            lore.add("");
            lore.add("&aLinksklick: &7Teleportieren");
            lore.add("&cRechtsklick: &7Home löschen");
            
            Material bedMaterial = getBedMaterialForHome(i);
            setItem(slot, bedMaterial, "&b" + homeName, lore);
            slot++;
        }
        
        // Neues Home setzen
        if (homes.size() < plugin.getConfigManager().getMaxHomes()) {
            double cost = plugin.getConfigManager().getCostPerHome();
            boolean canAfford = plugin.getEconomyManager().hasBalance(player.getUniqueId(), cost);
            
            setItem(49, Material.GREEN_BED, "&a&lNeues Home setzen",
                List.of(
                    "&7Setze ein neues Home an",
                    "&7deiner aktuellen Position.",
                    "",
                    "&7Kosten: &a" + plugin.getEconomyManager().formatBalance(cost),
                    "&7Dein Guthaben: " + (canAfford ? "&a" : "&c") + 
                        plugin.getEconomyManager().formatBalance(plugin.getEconomyManager().getBalance(player.getUniqueId())),
                    "",
                    canAfford ? "&aLinksklick: &7Home setzen" : "&cNicht genug Geld!"
                ));
        } else {
            setItem(49, Material.RED_BED, "&c&lMaximum erreicht",
                List.of(
                    "&7Du hast bereits die maximale",
                    "&7Anzahl an Homes erreicht!",
                    "",
                    "&7Maximum: &e" + plugin.getConfigManager().getMaxHomes()
                ));
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) homes.size() / homesPerPage);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Zurück und Schließen
        setItem(46, createBackButton());
        setItem(52, createCloseButton());
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) return;
        
        playClickSound();
        
        switch (slot) {
            case 45: // Vorherige Seite
                if (currentPage > 0) {
                    currentPage--;
                    update();
                }
                break;
                
            case 46: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 49: // Neues Home setzen
                handleSetNewHome();
                break;
                
            case 52: // Schließen
                closeGUI();
                break;
                
            case 53: // Nächste Seite
                Map<String, PlayerData.HomeLocation> homes = playerData.getHomes();
                int totalPages = (int) Math.ceil((double) homes.size() / homesPerPage);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    update();
                }
                break;
                
            default:
                // Home-Klick
                if (slot >= 10 && slot <= 43) {
                    handleHomeClick(slot, isRightClick);
                }
                break;
        }
    }
    
    private void handleHomeClick(int slot, boolean isRightClick) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) return;
        
        // Berechne Home-Index basierend auf Slot
        int adjustedSlot = slot - 10;
        if (slot >= 18) adjustedSlot -= 2;
        if (slot >= 27) adjustedSlot -= 2;
        if (slot >= 36) adjustedSlot -= 2;
        
        int homeIndex = (currentPage * homesPerPage) + adjustedSlot;
        
        List<Map.Entry<String, PlayerData.HomeLocation>> homeList = new ArrayList<>(playerData.getHomes().entrySet());
        
        if (homeIndex >= homeList.size()) return;
        
        Map.Entry<String, PlayerData.HomeLocation> entry = homeList.get(homeIndex);
        String homeName = entry.getKey();
        PlayerData.HomeLocation home = entry.getValue();
        
        if (isRightClick) {
            // Home löschen
            plugin.getGUIManager().openConfirmationGUI(player,
                "&cHome löschen",
                "&7Möchtest du das Home '&e" + homeName + "&7' wirklich löschen?",
                () -> {
                    playerData.removeHome(homeName);
                    playSuccessSound();
                    plugin.getMessageManager().sendMessage(player, "home.deleted", "name", homeName);
                    plugin.getGUIManager().openHomeManagementGUI(player);
                },
                () -> plugin.getGUIManager().openHomeManagementGUI(player)
            );
        } else {
            // Zum Home teleportieren
            if (!home.isWorldLoaded()) {
                playErrorSound();
                plugin.getMessageManager().sendMessage(player, "error.world-not-found", "world", home.getWorld());
                return;
            }
            
            org.bukkit.Location location = home.toBukkitLocation();
            if (location == null) {
                playErrorSound();
                plugin.getMessageManager().sendMessage(player, "home.teleport-failed");
                return;
            }
            
            // Überprüfe Cooldown
            if (playerData.hasCooldown("home")) {
                playErrorSound();
                plugin.getMessageManager().sendMessage(player, "cooldown.active",
                    "time", String.valueOf(playerData.getCooldownRemaining("home")));
                return;
            }
            
            closeGUI();
            playerData.setCooldown("home", plugin.getConfigManager().getHomeTeleportCooldown());
            plugin.getTeleportManager().startTeleport(player, location, "Home: " + homeName);
        }
    }
    
    private void handleSetNewHome() {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) return;
        
        if (playerData.getHomeCount() >= plugin.getConfigManager().getMaxHomes()) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "home.max-homes", 
                "max", String.valueOf(plugin.getConfigManager().getMaxHomes()));
            return;
        }
        
        double cost = plugin.getConfigManager().getCostPerHome();
        if (!plugin.getEconomyManager().hasBalance(player.getUniqueId(), cost)) {
            playErrorSound();
            plugin.getMessageManager().sendMessage(player, "home.insufficient-funds",
                "cost", plugin.getEconomyManager().formatBalance(cost));
            return;
        }
        
        // Text-Input für Home-Namen (vereinfacht)
        closeGUI();
        plugin.getMessageManager().sendMessage(player, "home.enter-name");
        
        // Hier würde normalerweise ein Chat-Listener implementiert werden
        // Für die Demo setzen wir ein Standard-Home
        String homeName = "home" + (playerData.getHomeCount() + 1);
        
        // Geld abziehen
        plugin.getEconomyManager().withdrawBalance(player.getUniqueId(), cost, "Home gesetzt: " + homeName);
        
        // Home setzen
        playerData.addHome(homeName, player.getLocation());
        
        playSuccessSound();
        plugin.getMessageManager().sendMessage(player, "home.set", "name", homeName);
        
        // GUI wieder öffnen
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
            plugin.getGUIManager().openHomeManagementGUI(player), 20L);
    }
    
    private Material getBedMaterialForHome(int index) {
        Material[] bedColors = {
            Material.RED_BED, Material.BLUE_BED, Material.GREEN_BED, Material.YELLOW_BED,
            Material.ORANGE_BED, Material.PURPLE_BED, Material.PINK_BED, Material.WHITE_BED,
            Material.LIGHT_BLUE_BED, Material.LIME_BED, Material.CYAN_BED, Material.MAGENTA_BED,
            Material.BROWN_BED, Material.LIGHT_GRAY_BED, Material.GRAY_BED, Material.BLACK_BED
        };
        
        return bedColors[index % bedColors.length];
    }
}
