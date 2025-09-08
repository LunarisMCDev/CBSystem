package de.opgames.cbsystem.gui.guis;

import com.plotsquared.core.plot.Plot;
import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import de.opgames.cbsystem.plot.PlotSquaredIntegration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class PlotInfoGUI extends BaseGUI {
    
    private final Plot plot;
    
    public PlotInfoGUI(CBSystem plugin, Player player, Plot plot) {
        super(plugin, player, "&8Plot-Informationen", 27);
        this.plot = plot;
    }
    
    @Override
    protected void setupGUI() {
        if (plot == null) {
            setItem(13, Material.BARRIER, "&cKein Plot gefunden",
                List.of("&7Du stehst nicht auf einem Plot!"));
            setItem(22, createCloseButton());
            fillEmptySlots();
            return;
        }
        
        // Basis-Informationen
        List<String> basicInfo = new ArrayList<>();
        basicInfo.add("&7Plot-ID: &e" + plot.getId());
        basicInfo.add("&7Welt: &e" + plot.getWorldName());
        basicInfo.add("&7Koordinaten: &e" + plot.getBottomAbs().getX() + ", " + plot.getBottomAbs().getZ());
        
        if (plot.hasOwner()) {
            basicInfo.add("&7Besitzer: &a" + plot.getOwnerAbs());
            basicInfo.add("&7Status: &aBenutzt");
        } else {
            basicInfo.add("&7Status: &cFrei");
        }
        
        setItem(4, Material.PAPER, "&6&lBasis-Informationen", basicInfo);
        
        // Mitglieder-Informationen
        if (plot.hasOwner()) {
            List<String> memberInfo = new ArrayList<>();
            
            // Vertraute Spieler
            if (!plot.getTrusted().isEmpty()) {
                memberInfo.add("&a&lVertraute Spieler:");
                int count = 0;
                for (Object trusted : plot.getTrusted()) {
                    if (count >= 5) {
                        memberInfo.add("&7... und " + (plot.getTrusted().size() - 5) + " weitere");
                        break;
                    }
                    memberInfo.add("&7- &e" + trusted.toString());
                    count++;
                }
            } else {
                memberInfo.add("&7Keine vertrauten Spieler");
            }
            
            memberInfo.add("");
            
            // Mitglieder
            if (!plot.getMembers().isEmpty()) {
                memberInfo.add("&b&lMitglieder:");
                int count = 0;
                for (Object member : plot.getMembers()) {
                    if (count >= 5) {
                        memberInfo.add("&7... und " + (plot.getMembers().size() - 5) + " weitere");
                        break;
                    }
                    memberInfo.add("&7- &e" + member.toString());
                    count++;
                }
            } else {
                memberInfo.add("&7Keine Mitglieder");
            }
            
            setItem(10, Material.PLAYER_HEAD, "&6&lMitglieder", memberInfo);
        }
        
        // Plot-Einstellungen/Flags
        List<String> settingsInfo = new ArrayList<>();
        settingsInfo.add("&6&lPlot-Einstellungen:");
        
        // Hier würden normalerweise die Plot-Flags angezeigt werden
        settingsInfo.add("&7PvP: &c" + (plot.getFlag(com.plotsquared.core.plot.flag.implementations.PvpFlag.class) ? "Aktiviert" : "Deaktiviert"));
        settingsInfo.add("&7Tiere: &a" + (plot.getFlag(com.plotsquared.core.plot.flag.implementations.AnimalAttackFlag.class) ? "Erlaubt" : "Verboten"));
        settingsInfo.add("&7Explosionen: &c" + (plot.getFlag(com.plotsquared.core.plot.flag.implementations.ExplosionFlag.class) ? "Aktiviert" : "Deaktiviert"));
        
        setItem(12, Material.REDSTONE, "&6&lEinstellungen", settingsInfo);
        
        // Erweiterte Informationen
        List<String> extendedInfo = new ArrayList<>();
        
        // Plot-Daten aus der Datenbank
        PlotSquaredIntegration.PlotData plotData = plugin.getPlotManager().getPlotData(plot);
        if (plotData != null) {
            extendedInfo.add("&6&lKauf-Informationen:");
            extendedInfo.add("&7Kaufpreis: &a" + plugin.getEconomyManager().formatBalance(plotData.getPurchasePrice()));
            
            if (plotData.getPurchaseDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                extendedInfo.add("&7Gekauft am: &e" + dateFormat.format(plotData.getPurchaseDate()));
            }
        } else {
            extendedInfo.add("&7Keine Kauf-Informationen");
            extendedInfo.add("&7verfügbar.");
        }
        
        extendedInfo.add("");
        extendedInfo.add("&6&lStatistiken:");
        extendedInfo.add("&7Plot-Größe: &e64x64 Blöcke");
        extendedInfo.add("&7Bauhöhe: &eY 0-256");
        
        setItem(14, Material.BOOK, "&6&lErweiterte Infos", extendedInfo);
        
        // Teleportation
        if (plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
            setItem(16, Material.ENDER_PEARL, "&a&lZum Plot teleportieren",
                List.of(
                    "&7Teleportiere dich zum",
                    "&7Plot-Home dieses Plots.",
                    "",
                    "&aLinksklick: &7Teleportieren"
                ));
        }
        
        // Navigation
        setItem(18, createBackButton());
        setItem(26, createCloseButton());
        
        // Management-Button (nur für Besitzer)
        if (plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
            setItem(22, Material.CHEST, "&6&lPlot verwalten",
                List.of(
                    "&7Öffne das Plot-Management",
                    "&7um dein Plot zu verwalten.",
                    "",
                    "&6Linksklick: &7Management öffnen"
                ));
        }
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        playClickSound();
        
        switch (slot) {
            case 16: // Teleportieren
                if (plot != null && plugin.getPlotManager().hasPlotPermission(plot, player.getUniqueId())) {
                    closeGUI();
                    plugin.getPlotManager().teleportToPlotHome(player, plot);
                }
                break;
                
            case 18: // Zurück
                plugin.getGUIManager().openMainMenu(player);
                break;
                
            case 22: // Plot verwalten
                if (plot != null && plugin.getPlotManager().isPlotOwner(plot, player.getUniqueId())) {
                    plugin.getGUIManager().openPlotManagementGUI(player, plot);
                }
                break;
                
            case 26: // Schließen
                closeGUI();
                break;
        }
    }
}
