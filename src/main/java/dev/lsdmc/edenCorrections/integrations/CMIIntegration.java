package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * CMI Integration Manager
 * 
 * Handles integration with CMI plugin for jail and kit operations.
 * Uses elevated permission approach to bypass console command restrictions.
 * 
 * @author EdenCorrections Team
 * @version 1.0
 */
public class CMIIntegration {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final boolean cmiAvailable;
    
    public CMIIntegration(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cmiAvailable = checkCMIAvailability();
        
        if (cmiAvailable) {
            logger.info("CMI integration initialized successfully");
        } else {
            logger.warning("CMI not available - integration features disabled");
        }
    }
    
    /**
     * Check if CMI is available on the server
     */
    private boolean checkCMIAvailability() {
        Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
        return cmi != null && cmi.isEnabled();
    }
    
    /**
     * Execute a CMI command as a player with elevated permissions
     * This bypasses CMI's console command restrictions
     */
    private CompletableFuture<Boolean> executePlayerCommand(Player executor, String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!cmiAvailable) {
                logger.warning("Cannot execute CMI command - CMI not available");
                return false;
            }
            
            if (executor == null || !executor.isOnline()) {
                logger.warning("Cannot execute CMI command - executor not available");
                return false;
            }
            
            PermissionAttachment attachment = null;
            try {
                // Grant temporary permissions for CMI commands
                attachment = executor.addAttachment(plugin);
                attachment.setPermission("cmi.command.*", true);
                attachment.setPermission("cmi.command.jail", true);
                attachment.setPermission("cmi.command.kit", true);
                attachment.setPermission("cmi.kit.*", true);
                attachment.setPermission("cmi.jail.*", true);
                
                // Execute command as player
                boolean success = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    try {
                        return Bukkit.dispatchCommand(executor, command);
                    } catch (Exception e) {
                        logger.warning("CMI command execution failed: " + e.getMessage());
                        return false;
                    }
                }).get();
                
                if (success) {
                    logger.info("CMI command executed successfully: " + command);
                } else {
                    logger.warning("CMI command failed: " + command);
                }
                
                return success;
                
            } catch (Exception e) {
                logger.severe("Error executing CMI command '" + command + "': " + e.getMessage());
                return false;
            } finally {
                // Remove temporary permissions
                if (attachment != null) {
                    try {
                        executor.removeAttachment(attachment);
                    } catch (Exception e) {
                        logger.warning("Failed to remove permission attachment: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Jail a player using CMI
     * 
     * @param executor The player executing the jail command (usually a guard)
     * @param target The player to jail
     * @param jailTime Jail time in seconds
     * @param reason Reason for jailing
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> jailPlayer(Player executor, Player target, int jailTime, String reason) {
        if (!cmiAvailable) {
            logger.warning("Cannot jail player - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (target == null) {
            logger.warning("Cannot jail player - target is null");
            return CompletableFuture.completedFuture(false);
        }
        
        // Convert seconds to CMI time format (minutes)
        int jailMinutes = Math.max(1, jailTime / 60);
        
        // Build CMI jail command
        String jailCommand = String.format("cmi jail %s %dm %s", 
            target.getName(), 
            jailMinutes, 
            reason != null ? reason : "Arrested by corrections officer"
        );
        
        logger.info("Attempting to jail " + target.getName() + " for " + jailMinutes + " minutes");
        
        return executePlayerCommand(executor, jailCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully jailed " + target.getName() + " for " + jailMinutes + " minutes");
            } else {
                logger.warning("Failed to jail " + target.getName());
            }
            return success;
        });
    }
    
    /**
     * Jail an offline player using CMI
     * 
     * @param executor The player executing the command
     * @param targetName Name of the offline player to jail
     * @param jailTime Jail time in seconds
     * @param reason Reason for jailing
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> jailOfflinePlayer(Player executor, String targetName, int jailTime, String reason) {
        if (!cmiAvailable) {
            logger.warning("Cannot jail offline player - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (targetName == null || targetName.trim().isEmpty()) {
            logger.warning("Cannot jail player - invalid target name");
            return CompletableFuture.completedFuture(false);
        }
        
        // Convert seconds to CMI time format (minutes)
        int jailMinutes = Math.max(1, jailTime / 60);
        
        // Build CMI jail command for offline player
        String jailCommand = String.format("cmi jail %s %dm %s", 
            targetName, 
            jailMinutes, 
            reason != null ? reason : "Arrested by corrections officer"
        );
        
        logger.info("Attempting to jail offline player " + targetName + " for " + jailMinutes + " minutes");
        
        return executePlayerCommand(executor, jailCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully jailed offline player " + targetName + " for " + jailMinutes + " minutes");
            } else {
                logger.warning("Failed to jail offline player " + targetName);
            }
            return success;
        });
    }
    
    /**
     * Give a kit to a player using CMI
     * 
     * @param executor The player executing the command (can be the target themselves)
     * @param target The player to receive the kit
     * @param kitName Name of the CMI kit
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> giveKit(Player executor, Player target, String kitName) {
        if (!cmiAvailable) {
            logger.warning("Cannot give kit - CMI not available");
            return CompletableFuture.completedFuture(false);
        }
        
        if (target == null || kitName == null || kitName.trim().isEmpty()) {
            logger.warning("Cannot give kit - invalid parameters");
            return CompletableFuture.completedFuture(false);
        }
        
        // Build CMI kit command
        String kitCommand = String.format("cmi kit %s %s", kitName, target.getName());
        
        logger.info("Attempting to give kit " + kitName + " to " + target.getName());
        
        return executePlayerCommand(executor, kitCommand).thenApply(success -> {
            if (success) {
                logger.info("Successfully gave kit " + kitName + " to " + target.getName());
            } else {
                logger.warning("Failed to give kit " + kitName + " to " + target.getName());
            }
            return success;
        });
    }
    
    /**
     * Give a kit to a player (simplified version where target executes their own kit command)
     * 
     * @param target The player to receive the kit
     * @param kitName Name of the CMI kit
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> giveKit(Player target, String kitName) {
        return giveKit(target, target, kitName);
    }
    
    /**
     * Check if CMI is available and loaded
     * 
     * @return true if CMI integration is available
     */
    public boolean isAvailable() {
        return cmiAvailable;
    }
    
    /**
     * Test CMI integration by checking plugin availability
     * 
     * @return true if CMI responds correctly
     */
    public boolean testIntegration() {
        try {
            Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
            if (cmi == null) {
                return false;
            }
            
            // Check if CMI is properly loaded
            return cmi.isEnabled() && cmi.getDescription() != null;
            
        } catch (Exception e) {
            logger.warning("CMI integration test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get diagnostic information about CMI integration
     * 
     * @return Diagnostic information string
     */
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("CMI Integration Status:\n");
        info.append("  Available: ").append(cmiAvailable).append("\n");
        
        if (cmiAvailable) {
            Plugin cmi = Bukkit.getPluginManager().getPlugin("CMI");
            info.append("  Version: ").append(cmi.getDescription().getVersion()).append("\n");
            info.append("  Enabled: ").append(cmi.isEnabled()).append("\n");
        } else {
            info.append("  CMI plugin not found or disabled\n");
        }
        
        return info.toString();
    }
}