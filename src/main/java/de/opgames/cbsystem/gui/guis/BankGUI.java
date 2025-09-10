package de.opgames.cbsystem.gui.guis;

import de.opgames.cbsystem.CBSystem;
import de.opgames.cbsystem.bank.BankManager;
import de.opgames.cbsystem.gui.BaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BankGUI extends BaseGUI {
    
    private final BankManager bankManager;
    
    public BankGUI(CBSystem plugin, Player player) {
        super(plugin, player, 54, "&6&lBank &8- &7OP-Games.de");
        this.bankManager = new BankManager(plugin);
        setupGUI();
    }
    
    @Override
    protected void setupGUI() {
        // Bankkonto-Info
        double bankBalance = bankManager.getBankBalance(player.getUniqueId());
        double pocketBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        
        // Bankkonto-Info Item
        ItemStack bankInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankInfoMeta = bankInfo.getItemMeta();
        bankInfoMeta.setDisplayName(plugin.getMessageManager().colorize("&6&lBankkonto"));
        bankInfoMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Bankguthaben: &a" + plugin.getEconomyManager().formatBalance(bankBalance)),
            plugin.getMessageManager().colorize("&7Taschengeld: &e" + plugin.getEconomyManager().formatBalance(pocketBalance)),
            "",
            plugin.getMessageManager().colorize("&7Zinssatz: &a2% pro Tag"),
            plugin.getMessageManager().colorize("&7Letzte Zinsen: &eHeute")
        ));
        bankInfo.setItemMeta(bankInfoMeta);
        inventory.setItem(4, bankInfo);
        
        // Einzahlung
        ItemStack deposit = new ItemStack(Material.EMERALD);
        ItemMeta depositMeta = deposit.getItemMeta();
        depositMeta.setDisplayName(plugin.getMessageManager().colorize("&a&lEinzahlung"));
        depositMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Klicke um Geld einzuzahlen"),
            plugin.getMessageManager().colorize("&7Verwende: &e/bank deposit <betrag>"),
            "",
            plugin.getMessageManager().colorize("&a✓ &7Sichere Aufbewahrung"),
            plugin.getMessageManager().colorize("&a✓ &7Zinsen verdienen")
        ));
        deposit.setItemMeta(depositMeta);
        inventory.setItem(20, deposit);
        
        // Auszahlung
        ItemStack withdraw = new ItemStack(Material.REDSTONE);
        ItemMeta withdrawMeta = withdraw.getItemMeta();
        withdrawMeta.setDisplayName(plugin.getMessageManager().colorize("&c&lAuszahlung"));
        withdrawMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Klicke um Geld abzuheben"),
            plugin.getMessageManager().colorize("&7Verwende: &e/bank withdraw <betrag>"),
            "",
            plugin.getMessageManager().colorize("&c⚠ &7Geld wird sofort verfügbar")
        ));
        withdraw.setItemMeta(withdrawMeta);
        inventory.setItem(22, withdraw);
        
        // Kredit-Info
        BankManager.LoanInfo loanInfo = bankManager.getLoanInfo(player.getUniqueId());
        ItemStack loan = new ItemStack(Material.PAPER);
        ItemMeta loanMeta = loan.getItemMeta();
        
        if (loanInfo != null) {
            loanMeta.setDisplayName(plugin.getMessageManager().colorize("&e&lAktiver Kredit"));
            loanMeta.setLore(Arrays.asList(
                plugin.getMessageManager().colorize("&7Verbleibender Betrag: &c" + plugin.getEconomyManager().formatBalance(loanInfo.getRemainingAmount())),
                plugin.getMessageManager().colorize("&7Originalbetrag: &e" + plugin.getEconomyManager().formatBalance(loanInfo.getOriginalAmount())),
                plugin.getMessageManager().colorize("&7Fälligkeitsdatum: &c" + loanInfo.getDueDate().toString()),
                "",
                plugin.getMessageManager().colorize("&7Verwende: &e/bank payloan <betrag>"),
                plugin.getMessageManager().colorize("&7um den Kredit zurückzuzahlen")
            ));
        } else {
            loanMeta.setDisplayName(plugin.getMessageManager().colorize("&e&lKredit beantragen"));
            loanMeta.setLore(Arrays.asList(
                plugin.getMessageManager().colorize("&7Klicke um einen Kredit zu beantragen"),
                plugin.getMessageManager().colorize("&7Verwende: &e/bank loan <betrag> [tage]"),
                "",
                plugin.getMessageManager().colorize("&e✓ &7Maximal: &a$100,000"),
                plugin.getMessageManager().colorize("&e✓ &7Maximal: &a7 Tage"),
                plugin.getMessageManager().colorize("&e✓ &7Zinssatz: &c5% pro Tag")
            ));
        }
        loan.setItemMeta(loanMeta);
        inventory.setItem(24, loan);
        
        // Transaktionshistorie
        ItemStack history = new ItemStack(Material.BOOK);
        ItemMeta historyMeta = history.getItemMeta();
        historyMeta.setDisplayName(plugin.getMessageManager().colorize("&b&lTransaktionshistorie"));
        historyMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Klicke um deine Transaktionen zu sehen"),
            "",
            plugin.getMessageManager().colorize("&b✓ &7Einzahlungen"),
            plugin.getMessageManager().colorize("&b✓ &7Auszahlungen"),
            plugin.getMessageManager().colorize("&b✓ &7Zinsen"),
            plugin.getMessageManager().colorize("&b✓ &7Kredite")
        ));
        history.setItemMeta(historyMeta);
        inventory.setItem(30, history);
        
        // Bank-Statistiken
        ItemStack stats = new ItemStack(Material.DIAMOND);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.setDisplayName(plugin.getMessageManager().colorize("&d&lBank-Statistiken"));
        statsMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Deine Bank-Statistiken"),
            "",
            plugin.getMessageManager().colorize("&d✓ &7Gesamte Einzahlungen"),
            plugin.getMessageManager().colorize("&d✓ &7Gesamte Auszahlungen"),
            plugin.getMessageManager().colorize("&d✓ &7Verdiente Zinsen"),
            plugin.getMessageManager().colorize("&d✓ &7Kredit-Historie")
        ));
        stats.setItemMeta(statsMeta);
        inventory.setItem(32, history);
        
        // Hilfe
        ItemStack help = new ItemStack(Material.NETHER_STAR);
        ItemMeta helpMeta = help.getItemMeta();
        helpMeta.setDisplayName(plugin.getMessageManager().colorize("&6&lHilfe"));
        helpMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Bank-Befehle:"),
            "",
            plugin.getMessageManager().colorize("&e/bank balance &7- Guthaben anzeigen"),
            plugin.getMessageManager().colorize("&e/bank deposit <betrag> &7- Einzahlen"),
            plugin.getMessageManager().colorize("&e/bank withdraw <betrag> &7- Abheben"),
            plugin.getMessageManager().colorize("&e/bank loan <betrag> [tage] &7- Kredit"),
            plugin.getMessageManager().colorize("&e/bank payloan <betrag> &7- Kredit zahlen"),
            plugin.getMessageManager().colorize("&e/bank info &7- Detaillierte Infos")
        ));
        help.setItemMeta(helpMeta);
        inventory.setItem(40, help);
        
        // Schließen
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(plugin.getMessageManager().colorize("&c&lSchließen"));
        closeMeta.setLore(Arrays.asList(
            plugin.getMessageManager().colorize("&7Klicke um das Bank-Menü zu schließen")
        ));
        close.setItemMeta(closeMeta);
        inventory.setItem(49, close);
        
        // Füllen mit Glas
        fillEmptySlots();
    }
    
    @Override
    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 20 -> { // Einzahlung
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "bank.deposit-help");
            }
            case 22 -> { // Auszahlung
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "bank.withdraw-help");
            }
            case 24 -> { // Kredit
                player.closeInventory();
                if (bankManager.hasActiveLoan(player.getUniqueId())) {
                    plugin.getMessageManager().sendMessage(player, "bank.payloan-help");
                } else {
                    plugin.getMessageManager().sendMessage(player, "bank.loan-help");
                }
            }
            case 30 -> { // Transaktionshistorie
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "bank.history-coming-soon");
            }
            case 32 -> { // Statistiken
                player.closeInventory();
                plugin.getMessageManager().sendMessage(player, "bank.stats-coming-soon");
            }
            case 40 -> { // Hilfe
                player.closeInventory();
                showHelp();
            }
            case 49 -> { // Schließen
                player.closeInventory();
            }
        }
    }
    
    private void showHelp() {
        plugin.getMessageManager().sendMessage(player, "bank.help-header");
        plugin.getMessageManager().sendMessage(player, "bank.help-balance");
        plugin.getMessageManager().sendMessage(player, "bank.help-deposit");
        plugin.getMessageManager().sendMessage(player, "bank.help-withdraw");
        plugin.getMessageManager().sendMessage(player, "bank.help-loan");
        plugin.getMessageManager().sendMessage(player, "bank.help-payloan");
        plugin.getMessageManager().sendMessage(player, "bank.help-info");
    }
}
