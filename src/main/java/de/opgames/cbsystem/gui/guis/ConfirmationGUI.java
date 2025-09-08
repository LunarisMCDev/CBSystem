package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfirmationGUI extends BaseGUI {
    
    private final String confirmText;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    
    public ConfirmationGUI(CBSystem plugin, Player player, String title, String confirmText, 
                          Runnable onConfirm, Runnable onCancel) {
        super(plugin, player, title, 27);
        this.confirmText = confirmText;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }
    
    @Override
    protected void setupGUI() {
        // Bestätigungstext
        setItem(13, Material.PAPER, "&e&lBestätigung erforderlich",
            List.of(
                confirmText,
                "",
                "&7Wähle eine Option:"
            ));
        
        // Bestätigen
        setItem(11, Material.GREEN_CONCRETE, "&a&lBestätigen",
            List.of(
                "&7Klicke hier um die",
                "&7Aktion zu bestätigen.",
                "",
                "&aLinksklick: &7Bestätigen"
            ));
        
        // Abbrechen
        setItem(15, Material.RED_CONCRETE, "&c&lAbbrechen",
            List.of(
                "&7Klicke hier um die",
                "&7Aktion abzubrechen.",
                "",
                "&cLinksklick: &7Abbrechen"
            ));
        
        fillEmptySlots();
    }
    
    @Override
    public void handleClick(int slot, ItemStack clickedItem, boolean isShiftClick, boolean isRightClick) {
        if (isFillItem(clickedItem)) return;
        
        switch (slot) {
            case 11: // Bestätigen
                playSuccessSound();
                closeGUI();
                if (onConfirm != null) {
                    onConfirm.run();
                }
                break;
                
            case 15: // Abbrechen
                playErrorSound();
                closeGUI();
                if (onCancel != null) {
                    onCancel.run();
                }
                break;
        }
    }
}
