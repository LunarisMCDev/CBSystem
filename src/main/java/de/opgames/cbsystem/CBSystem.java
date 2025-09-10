package de.opgames.cbsystem;

import de.opgames.cbsystem.auction.AuctionManager;
import de.opgames.cbsystem.bank.BankManager;
import de.opgames.cbsystem.commands.*;
import de.opgames.cbsystem.scoreboard.ScoreboardManager;
import de.opgames.cbsystem.database.DatabaseManager;
import de.opgames.cbsystem.database.PlayerDataManager;
import de.opgames.cbsystem.economy.EconomyManager;
import de.opgames.cbsystem.gui.GUIManager;
import de.opgames.cbsystem.listeners.*;
import de.opgames.cbsystem.placeholders.CBSystemPlaceholders;
import de.opgames.cbsystem.plot.PlotManager;
import de.opgames.cbsystem.teleport.TeleportManager;
import de.opgames.cbsystem.utils.ConfigManager;
import de.opgames.cbsystem.utils.MessageManager;
import de.opgames.cbsystem.utils.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class CBSystem extends JavaPlugin {
    
    private static CBSystem instance;
    
    // Manager-Instanzen
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private SoundManager soundManager;
    private GUIManager guiManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private PlotManager plotManager;
    private TeleportManager teleportManager;
    private AuctionManager auctionManager;
    private BankManager bankManager;
    private ScoreboardManager scoreboardManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // ASCII Art für Plugin-Start
        getLogger().info("  ____  ____   ____            _                 ");
        getLogger().info(" / ___|| __ ) / ___|  _   _  __| |_ ___ _ __ ___  ");
        getLogger().info(" \\___ \\|  _ \\ \\___ \\ | | | |/ _` | __/ _ \\ '_ ` _ \\ ");
        getLogger().info("  ___) | |_) | ___) || |_| | (_| | ||  __/ | | | |");
        getLogger().info(" |____/|____/ |____/  \\__, |\\__,_|\\__\\___|_| |_|_|");
        getLogger().info("                      |___/                      ");
        getLogger().info("Version " + getDescription().getVersion() + " für OP-Games.de");
        getLogger().info("Entwickelt mit ❤ für die beste CityBuild-Experience!");
        
        // Manager initialisieren
        if (!initializeManagers()) {
            getLogger().severe("Fehler beim Initialisieren der Manager! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Befehle registrieren
        registerCommands();
        
        // Event-Listener registrieren
        registerListeners();
        
        // Hooks für externe Plugins
        setupHooks();
        
        getLogger().info("CBSystem wurde erfolgreich aktiviert!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("CBSystem wird deaktiviert...");
        
        // Alle Spielerdaten speichern
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        // Datenbankverbindungen schließen
        if (databaseManager != null) {
            databaseManager.closeConnections();
        }
        
        getLogger().info("CBSystem wurde erfolgreich deaktiviert!");
    }
    
    private boolean initializeManagers() {
        try {
            // Config Manager zuerst laden
            configManager = new ConfigManager(this);
            
            // Message Manager initialisieren
            messageManager = new MessageManager(this);
            
            // Sound Manager initialisieren
            soundManager = new SoundManager(this);
            
            // Datenbank-Manager initialisieren
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Datenbankverbindung konnte nicht hergestellt werden!");
                return false;
            }
            
            // GUI Manager initialisieren
            guiManager = new GUIManager(this);
            
            // Player Data Manager initialisieren
            playerDataManager = new PlayerDataManager(this);
            
            // Economy Manager initialisieren
            economyManager = new EconomyManager(this);
            
            // Plot Manager initialisieren
            plotManager = new PlotManager(this);
            
            // Teleport Manager initialisieren
            teleportManager = new TeleportManager(this);
            
            // Auction Manager initialisieren
            auctionManager = new AuctionManager(this);
            
            // Bank Manager initialisieren
            bankManager = new BankManager(this);
            
            // Scoreboard Manager initialisieren
            scoreboardManager = new ScoreboardManager(this);
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fehler beim Initialisieren der Manager:", e);
            return false;
        }
    }
    
    private void registerCommands() {
        // Hauptbefehle
        getCommand("cb").setExecutor(new CBCommand(this));
        getCommand("plot").setExecutor(new PlotCommand(this));
        
        // Home-System
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        
        // Warp-System
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("setwarp").setExecutor(new SetWarpCommand(this));
        getCommand("delwarp").setExecutor(new DelWarpCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        
        // Economy-System
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("eco").setExecutor(new EcoCommand(this));
        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("scoreboard").setExecutor(new ScoreboardCommand(this));
        
        // TPA-System
        getCommand("tpa").setExecutor(new TPACommand(this));
        getCommand("tpaccept").setExecutor(new TPAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TPDenyCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        
        // Auktionshaus-System
        getCommand("auction").setExecutor(new AuctionCommand(this));
        getCommand("ah").setExecutor(new AHCommand(this));
        getCommand("bid").setExecutor(new BidCommand(this));
        getCommand("cancelauction").setExecutor(new CancelAuctionCommand(this));

        //Enderchest
        getCommand("ec").setExecutor(new EnderChestCommand(this));
        getCommand("enderchest").setExecutor(new EnderChestCommand(this));

        // Admin-Befehle
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("gamemode").setExecutor(new GameModeCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlotProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ServerListener(this), this);
    }
    
    private void setupHooks() {
        // PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI gefunden! Hook wird eingerichtet...");
            new CBSystemPlaceholders(this).register();
            getLogger().info("CBSystem Placeholders erfolgreich registriert!");
        }
        
        // Vault Hook
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Vault gefunden! Economy-Hook wird eingerichtet...");
            // Vault Economy Hook würde hier implementiert werden
        }
        
        // WorldEdit Hook
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            getLogger().info("WorldEdit gefunden! Plot-System erweitert...");
        }
        
        // PlotSquared Hook
        if (Bukkit.getPluginManager().getPlugin("PlotSquared") != null) {
            getLogger().info("PlotSquared gefunden! Plot-System wird erweitert...");
        }
    }
    
    // Getter-Methoden für alle Manager
    public static CBSystem getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public SoundManager getSoundManager() {
        return soundManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public PlotManager getPlotManager() {
        return plotManager;
    }
    
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
    
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
    
    public BankManager getBankManager() {
        return bankManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
