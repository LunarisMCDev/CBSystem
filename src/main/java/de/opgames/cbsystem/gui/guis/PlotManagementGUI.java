package de.opgames.cbsystem.gui.guis;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlotManagementGUI extends BaseGUI {
    
    private final Plot plot;
    
    public PlotManagementGUI(CBSystem plugin, Player player, Plot plot) {
        super(plugin, player, plugin.getConfigManager().getPlotMenuTitle(), 
              plugin.getConfigManager().getPlotMenuSize());
        this.plot = plot;
    }
    
    @Override
    protected void setupGUI() {
        if (plot == null) {
            setItem(13, Material.BARRIER, "&cKein Plot gefunden",
                List.of("&7Du stehst nicht auf einem Plot!"));
            return;
        }
        
        // Plot-Informationen
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Plot-ID: &e" + plot.getId());
        infoLore.add("&7Welt: &e" + plot.getWorldName());
        
        if (plot.hasOwner()) {
            infoLore.add("&7Besitzer: &e" + plot.getOwnerAbs());
            
            // Vertraute Spieler
            if (!plot.getTrusted().isEmpty()) {
                infoLore.add("&7Vertraute: &e" + plot.getTrusted().size());
            }
            
            // Mitglieder
            if (!plot.getMembers().isEmpty()) {
                infoLore.add("&7Mitglieder: &e" + plot.getMembers().size());
            }
        } else {
            infoLore.add("&7Status: &cNicht beansprucht");
        }
        
        infoLore.add("");
        infoLore.add("&aLinksklick: &7Plot-Home");
        
        setItem(4, Material.PAPER, "&6Plot-Informationen", infoLore);
        
        // Nur für Plot-Besitzer
        if (plot.hasOwner() && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
            
            // Spieler vertrauen
            setItem(10, Material.PLAYER_HEAD, "&aSpieler vertrauen",
                List.of(
                    "&7Füge einen Spieler zu",
                    "&7deinem Plot hinzu.",
                    "",
                    "&aLinksklick: &7Spieler hinzufügen"
                ));
            
            // Spieler entfernen
            setItem(12, Material.BARRIER, "&cSpieler entfernen",
                List.of(
                    "&7Entferne einen Spieler",
                    "&7von deinem Plot.",
                    "",
                    "&cLinksklick: &7Spieler entfernen"
                ));
            
            // Plot-Einstellungen
            setItem(14, Material.REDSTONE, "&6Plot-Einstellungen",
                List.of(
                    "&7Verwalte deine",
                    "&7Plot-Einstellungen.",
                    "",
                    "&6Linksklick: &7Einstellungen öffnen"
                ));
            
            // Plot löschen
            setItem(16, Material.TNT, "&cPlot löschen",
                List.of(
                    "&7Lösche dein Plot",
                    "&7unwiderruflich.",
                    "",
                    "&cLinksklick: &7Plot löschen",
                    "&c&lVorsicht: Nicht rückgängig machbar!"
                ));
        }
        
        // Plot kaufen (wenn nicht beansprucht)
        if (!plot.hasOwner() && plugin.getPlotManager().canPlayerClaimPlot(player.getUniqueId())) {
            double price = plugin.getPlotManager().calculatePlotPrice(player.getUniqueId());
            
            setItem(13, Material.EMERALD, "&a&lPlot kaufen",
                List.of(
                    "&7Kaufe diesen Plot für",
                    "&a" + plugin.getEconomyManager().formatBalance(price),
                    "",
                    "&7Plots im Besitz: &e" + plugin.getPlotManager().getPlotCount(player.getUniqueId()),
                    "&7Maximum: &e" + plugin.getPlotManager().getMaxPlots(player.getUniqueId()),
                    "",
                    "&aLinksklick: &7Plot kaufen"
                ));
        }
        
        // Zurück-Button
        setItem(18, createBackButton());
        
        // Schließen-Button
        setItem(26, createCloseButton());
        
        // Fülle leere Slots
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 4: // Plot-Home
                if (plot != null && plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
                    closeGUI();
                    plugin.getPlotManager().teleportToPlotHome(player, plot);
                }
                break;
                
            case 10: // Spieler vertrauen
                if (plot != null && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
                    closeGUI();
                    plugin.getMessageManager().sendMessage(player, "plot.trust-player-instruction");
                    // Hier würde normalerweise ein Spieler-Auswahl-GUI geöffnet
                }
                break;
                
            case 12: // Spieler entfernen
                if (plot != null && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
                    closeGUI();
                    plugin.getMessageManager().sendMessage(player, "plot.untrust-player-instruction");
                    // Hier würde normalerweise ein Spieler-Auswahl-GUI geöffnet
                }
                break;
                
            case 13: // Plot kaufen
                if (plot != null && !plot.hasOwner()) {
                    if (plugin.getPlotManager().purchasePlot(player, plot)) {
                        playSuccessSound();
                        update(); // GUI aktualisieren
                    } else {
                        playErrorSound();
                    }
                }
                break;
                
            case 14: // Plot-Einstellungen
                if (plot != null && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
                    // Hier würde normalerweise ein Plot-Settings-GUI geöffnet
                    plugin.getMessageManager().sendMessage(player, "plot.settings-coming-soon");
                }
                break;
                
            case 16: // Plot löschen
                if (plot != null && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
                    plugin.getGUIManager().openConfirmationGUI(player, 
                        "&cPlot löschen", 
                        "&7Bist du sicher, dass du dein Plot löschen möchtest?",
                        () -> {
                            // Plot löschen
                            closeGUI();
                            plugin.getMessageManager().sendMessage(player, "plot.delete-not-implemented");
                        },
                        () -> {
                            // Abbrechen
                            plugin.getGUIManager().openPlotManagementGUI(player, plot);
                        }
                    );
                }
                break;
                
            case 18: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 26: // Schließen
                closeGUI();
                break;
        }
    }
}
