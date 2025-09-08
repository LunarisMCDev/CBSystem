package de.opgames.cbsystem.utils;

import de.opgames.cbsystem.CBSystem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SoundManager {
    
    private final CBSystem plugin;
    
    public SoundManager(CBSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Spielt einen Sound für einen Spieler ab
     */
    public void playSound(Player player, String soundName, float volume, float pitch) {
        if (!plugin.getConfigManager().areSoundsEnabled() || player == null) {
            return;
        }
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Unbekannter Sound: " + soundName, e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Fehler beim Abspielen des Sounds: " + soundName, e);
        }
    }
    
    /**
     * Spielt einen Sound mit Standard-Lautstärke und Tonhöhe ab
     */
    public void playSound(Player player, String soundName) {
        playSound(player, soundName, 1.0f, 1.0f);
    }
    
    // GUI-Sounds
    public void playClickSound(Player player) {
        playSound(player, plugin.getConfigManager().getGUIClickSound(), 0.5f, 1.0f);
    }
    
    public void playSuccessSound(Player player) {
        playSound(player, plugin.getConfigManager().getGUISuccessSound(), 0.7f, 1.2f);
    }
    
    public void playErrorSound(Player player) {
        playSound(player, plugin.getConfigManager().getGUIErrorSound(), 0.8f, 0.8f);
    }
    
    public void playOpenSound(Player player) {
        playSound(player, plugin.getConfigManager().getGUIOpenSound(), 0.6f, 1.0f);
    }
    
    public void playCloseSound(Player player) {
        playSound(player, plugin.getConfigManager().getGUICloseSound(), 0.6f, 0.9f);
    }
    
    // Teleportation-Sounds
    public void playTeleportStartSound(Player player) {
        playSound(player, plugin.getConfigManager().getTeleportStartSound(), 0.8f, 1.0f);
    }
    
    public void playTeleportSuccessSound(Player player) {
        playSound(player, plugin.getConfigManager().getTeleportSuccessSound(), 1.0f, 1.0f);
    }
    
    public void playTeleportCancelledSound(Player player) {
        playSound(player, plugin.getConfigManager().getTeleportCancelledSound(), 0.8f, 0.8f);
    }
    
    // Geld-Sounds
    public void playMoneyReceiveSound(Player player) {
        playSound(player, plugin.getConfigManager().getMoneyReceiveSound(), 0.7f, 1.3f);
    }
    
    public void playMoneySendSound(Player player) {
        playSound(player, plugin.getConfigManager().getMoneySendSound(), 0.6f, 1.1f);
    }
    
    public void playMoneyInsufficientSound(Player player) {
        playSound(player, plugin.getConfigManager().getMoneyInsufficientSound(), 0.8f, 0.7f);
    }
    
    // Spezielle Sounds
    public void playLevelUpSound(Player player) {
        playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
    }
    
    public void playOrbPickupSound(Player player) {
        playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 0.7f, 1.5f);
    }
    
    public void playBellSound(Player player) {
        playSound(player, "BLOCK_NOTE_BLOCK_BELL", 0.8f, 1.0f);
    }
    
    public void playDingSound(Player player) {
        playSound(player, "BLOCK_NOTE_BLOCK_PLING", 0.6f, 1.8f);
    }
    
    public void playPopSound(Player player) {
        playSound(player, "ENTITY_CHICKEN_EGG", 0.5f, 1.5f);
    }
    
    public void playWarningSound(Player player) {
        playSound(player, "BLOCK_NOTE_BLOCK_BASS", 1.0f, 0.5f);
    }
    
    /**
     * Spielt einen Sound für alle Spieler in der Nähe ab
     */
    public void playSoundNearby(Player center, String soundName, double radius, float volume, float pitch) {
        if (!plugin.getConfigManager().areSoundsEnabled()) {
            return;
        }
        
        center.getWorld().getNearbyEntities(center.getLocation(), radius, radius, radius)
            .stream()
            .filter(entity -> entity instanceof Player)
            .map(entity -> (Player) entity)
            .forEach(player -> playSound(player, soundName, volume, pitch));
    }
    
    /**
     * Spielt einen Sound für alle Online-Spieler ab
     */
    public void playSoundGlobal(String soundName, float volume, float pitch) {
        if (!plugin.getConfigManager().areSoundsEnabled()) {
            return;
        }
        
        plugin.getServer().getOnlinePlayers().forEach(player -> 
            playSound(player, soundName, volume, pitch));
    }
    
    /**
     * Spielt eine Sound-Sequenz ab (mit Verzögerung zwischen den Sounds)
     */
    public void playSoundSequence(Player player, String[] sounds, long delayTicks) {
        if (!plugin.getConfigManager().areSoundsEnabled() || sounds.length == 0) {
            return;
        }
        
        playSound(player, sounds[0]);
        
        for (int i = 1; i < sounds.length; i++) {
            final String sound = sounds[i];
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
                playSound(player, sound), delayTicks * i);
        }
    }
    
    /**
     * Überprüft ob ein Sound existiert
     */
    public boolean isValidSound(String soundName) {
        try {
            Sound.valueOf(soundName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Holt alle verfügbaren Sounds
     */
    public Sound[] getAllSounds() {
        return Sound.values();
    }
    
    /**
     * Spielt einen zufälligen Sound aus einer Liste ab
     */
    public void playRandomSound(Player player, String[] sounds, float volume, float pitch) {
        if (sounds.length == 0) return;
        
        String randomSound = sounds[(int) (Math.random() * sounds.length)];
        playSound(player, randomSound, volume, pitch);
    }
    
    // Vordefinierte Sound-Kombinationen
    public void playShopBuySound(Player player) {
        playSoundSequence(player, new String[]{
            "ENTITY_EXPERIENCE_ORB_PICKUP",
            "BLOCK_NOTE_BLOCK_PLING"
        }, 5L);
    }
    
    public void playShopSellSound(Player player) {
        playSoundSequence(player, new String[]{
            "ENTITY_ARROW_HIT_PLAYER",
            "ENTITY_EXPERIENCE_ORB_PICKUP"
        }, 3L);
    }
    
    public void playPlotClaimSound(Player player) {
        playSoundSequence(player, new String[]{
            "ENTITY_PLAYER_LEVELUP",
            "BLOCK_NOTE_BLOCK_PLING",
            "ENTITY_EXPERIENCE_ORB_PICKUP"
        }, 8L);
    }
    
    public void playHomeTeleportSound(Player player) {
        playSoundSequence(player, new String[]{
            "ENTITY_ENDERMAN_TELEPORT",
            "ENTITY_PLAYER_LEVELUP"
        }, 10L);
    }
}
