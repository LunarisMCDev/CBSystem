package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerSelectionGUI extends BaseGUI {
    
    private final Consumer<Player> onPlayerSelected;
    private int currentPage = 0;
    private final int playersPerPage = 36;
    
    public PlayerSelectionGUI(CBSystem plugin, Player player, String title, Consumer<Player> onPlayerSelected) {
        super(plugin, player, title, 54);
        this.onPlayerSelected = onPlayerSelected;
    }
    
    @Override
    protected void setupGUI() {
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        onlinePlayers.remove(player); // Entferne den Spieler selbst
        
        // Info-Item
        setItem(4, Material.PLAYER_HEAD, "&6&lSpieler auswählen",
            List.of(
                "&7Online-Spieler: &e" + onlinePlayers.size(),
                "",
                "&7Klicke auf einen Spieler",
                "&7um ihn auszuwählen."
            ));
        
        // Spieler anzeigen
        int startIndex = currentPage * playersPerPage;
        int slot = 9;
        
        for (int i = startIndex; i < Math.min(startIndex + playersPerPage, onlinePlayers.size()); i++) {
            if (slot > 44) break;
            
            Player onlinePlayer = onlinePlayers.get(i);
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            
            if (meta != null) {
                meta.setOwningPlayer(onlinePlayer);
                meta.setDisplayName(plugin.getMessageManager().colorize("&e" + onlinePlayer.getName()));
                
                List<String> lore = new ArrayList<>();
                lore.add("&7Level: &e" + onlinePlayer.getLevel());
                lore.add("&7Gesundheit: &c" + (int) onlinePlayer.getHealth() + "/20");
                
                // Guthaben anzeigen (falls Economy aktiviert)
                if (plugin.getConfigManager().isEconomyEnabled()) {
                    double balance = plugin.getEconomyManager().getBalance(onlinePlayer.getUniqueId());
                    lore.add("&7Guthaben: &a" + plugin.getEconomyManager().formatBalance(balance));
                }
                
                lore.add("");
                lore.add("&aLinksklick: &7Spieler auswählen");
                
                meta.setLore(lore.stream()
                    .map(line -> plugin.getMessageManager().colorize(line))
                    .toList());
                
                playerHead.setItemMeta(meta);
            }
            
            setItem(slot, playerHead);
            slot++;
        }
        
        // Navigation
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);
        
        if (currentPage > 0) {
            setItem(45, createPreviousPageButton(currentPage, totalPages));
        }
        
        if (currentPage < totalPages - 1) {
            setItem(53, createNextPageButton(currentPage, totalPages));
        }
        
        // Zurück und Schließen
        setItem(49, createBackButton());
        setItem(50, createCloseButton());
        
        fillEmptySlots();
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
                
            case 49: // Zurück
                closeGUI();
                break;
                
            case 50: // Schließen
                closeGUI();
                break;
                
            case 53: // Nächste Seite
                List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
                onlinePlayers.remove(player);
                int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    update();
                }
                break;
                
            default:
                // Spieler-Auswahl
                if (slot >= 9 && slot <= 44) {
                    handlePlayerSelection(slot);
                }
                break;
        }
    }
    
    private void handlePlayerSelection(int slot) {
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        onlinePlayers.remove(player);
        
        int playerIndex = (currentPage * playersPerPage) + (slot - 9);
        
        if (playerIndex >= onlinePlayers.size()) return;
        
        Player selectedPlayer = onlinePlayers.get(playerIndex);
        
        playSuccessSound();
        closeGUI();
        
        if (onPlayerSelected != null) {
            onPlayerSelected.accept(selectedPlayer);
        }
    }
}
