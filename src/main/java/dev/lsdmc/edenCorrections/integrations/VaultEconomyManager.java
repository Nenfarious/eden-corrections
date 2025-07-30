package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Handles Vault Economy integration for EdenCorrections
 * Used specifically for penalty deductions and economy operations
 */
public class VaultEconomyManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private Economy economy;
    private boolean vaultAvailable;
    
    public VaultEconomyManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.vaultAvailable = false;
    }
    
    public void initialize() {
        if (!setupEconomy()) {
            logger.warning("Vault Economy not found - penalty economy features will be disabled");
            vaultAvailable = false;
        } else {
            logger.info("Vault Economy integration enabled - penalty system ready");
            vaultAvailable = true;
        }
    }
    
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager()
            .getRegistration(Economy.class);
        
        if (economyProvider == null) {
            return false;
        }
        
        economy = economyProvider.getProvider();
        return economy != null;
    }
    
    /**
     * Take money from a player's account for penalty purposes
     * @param player the player to take money from
     * @param amount the amount to take
     * @param reason the reason for the penalty (for logging)
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> takeMoney(Player player, double amount, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultAvailable || economy == null) {
                logger.warning("Cannot take money from " + player.getName() + " - Vault economy not available");
                return false;
            }
            
            try {
                // Check if player has enough money
                double balance = economy.getBalance(player);
                if (balance < amount) {
                    logger.warning("Cannot take $" + amount + " from " + player.getName() + 
                                 " - insufficient funds (has $" + balance + ")");
                    return false;
                }
                
                // Withdraw money
                net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount);
                
                if (response.transactionSuccess()) {
                    logger.info("Successfully deducted $" + amount + " from " + player.getName() + 
                               " (Reason: " + reason + ") - New balance: $" + response.balance);
                    return true;
                } else {
                    logger.warning("Failed to deduct money from " + player.getName() + 
                                 ": " + response.errorMessage);
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Error taking money from " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Give money to a player's account
     * @param player the player to give money to
     * @param amount the amount to give
     * @param reason the reason for giving money (for logging)
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> giveMoney(Player player, double amount, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultAvailable || economy == null) {
                logger.warning("Cannot give money to " + player.getName() + " - Vault economy not available");
                return false;
            }
            
            try {
                net.milkbowl.vault.economy.EconomyResponse response = economy.depositPlayer(player, amount);
                
                if (response.transactionSuccess()) {
                    logger.info("Successfully gave $" + amount + " to " + player.getName() + 
                               " (Reason: " + reason + ") - New balance: $" + response.balance);
                    return true;
                } else {
                    logger.warning("Failed to give money to " + player.getName() + 
                                 ": " + response.errorMessage);
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Error giving money to " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Get a player's balance
     * @param player the player to check
     * @return CompletableFuture with the balance amount
     */
    public CompletableFuture<Double> getBalance(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultAvailable || economy == null) {
                return 0.0;
            }
            
            try {
                return economy.getBalance(player);
            } catch (Exception e) {
                logger.warning("Error getting balance for " + player.getName() + ": " + e.getMessage());
                return 0.0;
            }
        });
    }
    
    /**
     * Check if a player has at least the specified amount
     * @param player the player to check
     * @param amount the minimum amount to check for
     * @return CompletableFuture with whether they have enough
     */
    public CompletableFuture<Boolean> hasEnough(Player player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (!vaultAvailable || economy == null) {
                return false;
            }
            
            try {
                return economy.getBalance(player) >= amount;
            } catch (Exception e) {
                logger.warning("Error checking balance for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Format currency amount according to Vault economy settings
     * @param amount the amount to format
     * @return formatted currency string
     */
    public String formatAmount(double amount) {
        if (!vaultAvailable || economy == null) {
            return "$" + amount;
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return "$" + amount;
        }
    }
    
    /**
     * Get the currency name (singular)
     * @return currency name
     */
    public String getCurrencyNameSingular() {
        if (!vaultAvailable || economy == null) {
            return "dollar";
        }
        
        try {
            return economy.currencyNameSingular();
        } catch (Exception e) {
            return "dollar";
        }
    }
    
    /**
     * Get the currency name (plural)
     * @return currency name plural
     */
    public String getCurrencyNamePlural() {
        if (!vaultAvailable || economy == null) {
            return "dollars";
        }
        
        try {
            return economy.currencyNamePlural();
        } catch (Exception e) {
            return "dollars";
        }
    }
    
    /**
     * Check if Vault economy is available
     * @return true if available, false otherwise
     */
    public boolean isAvailable() {
        return vaultAvailable && economy != null;
    }
    
    /**
     * Test the Vault integration
     * @return true if working properly, false otherwise
     */
    public boolean testIntegration() {
        if (!vaultAvailable || economy == null) {
            return false;
        }
        
        try {
            // Test basic functionality
            String currencyName = economy.currencyNamePlural();
            return currencyName != null && !currencyName.isEmpty();
        } catch (Exception e) {
            logger.warning("Vault integration test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get diagnostic information about the Vault integration
     * @return diagnostic info string
     */
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Vault Economy Integration:\n");
        info.append("  Available: ").append(vaultAvailable).append("\n");
        
        if (vaultAvailable && economy != null) {
            try {
                info.append("  Economy Plugin: ").append(economy.getName()).append("\n");
                info.append("  Currency (Singular): ").append(economy.currencyNameSingular()).append("\n");
                info.append("  Currency (Plural): ").append(economy.currencyNamePlural()).append("\n");
                info.append("  Fractional Digits: ").append(economy.fractionalDigits()).append("\n");
                info.append("  Test Status: ").append(testIntegration() ? "PASS" : "FAIL").append("\n");
            } catch (Exception e) {
                info.append("  Error getting info: ").append(e.getMessage()).append("\n");
            }
        } else {
            info.append("  Reason: Economy service not found or Vault plugin missing\n");
        }
        
        return info.toString();
    }
}