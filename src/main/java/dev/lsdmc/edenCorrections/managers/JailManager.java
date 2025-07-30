package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Jail Manager
 * 
 * Handles jail countdown and integration with chase system when targets flee.
 * Implements automatic chase initiation for fleeing targets during arrest countdown.
 * 
 * @author EdenCorrections Team
 * @version 2.0 - Enhanced with Chase Integration
 */
public class JailManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Active jail countdowns
    private final Map<UUID, BukkitTask> activeCountdowns;
    
    // Store initial positions for flee detection
    private final Map<UUID, Location> initialGuardPositions;
    private final Map<UUID, Location> initialTargetPositions;
    
    public JailManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeCountdowns = new HashMap<>();
        this.initialGuardPositions = new ConcurrentHashMap<>();
        this.initialTargetPositions = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        logger.info("JailManager initialized successfully!");
    }
    
    public boolean startJailCountdown(Player guard, Player target) {
        if (activeCountdowns.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "jail.restrictions.already-active");
            return false;
        }
        
        // Security check: Can target be jailed?
        if (!plugin.getSecurityManager().canPlayerBeJailed(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.jail-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("jail countdown", guard, target);
            return false;
        }
        
        // Store initial positions for flee detection
        UUID targetId = target.getUniqueId();
        initialGuardPositions.put(targetId, guard.getLocation().clone());
        initialTargetPositions.put(targetId, target.getLocation().clone());
        
        // Get jail countdown time and distance settings
        int countdownTime = plugin.getConfigManager().getJailCountdown();
        double jailRadius = plugin.getConfigManager().getJailCountdownRadius();
        boolean chaseIntegrationEnabled = plugin.getConfigManager().isJailChaseIntegrationEnabled();
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.countdown.started",
            playerPlaceholder("player", target),
            numberPlaceholder("seconds", countdownTime),
            numberPlaceholder("radius", Math.round(jailRadius)));
        plugin.getMessageManager().sendMessage(target, "jail.countdown.target-notification",
            numberPlaceholder("seconds", countdownTime),
            numberPlaceholder("radius", Math.round(jailRadius)));
        
        // Show boss bar
        plugin.getBossBarManager().showJailBossBar(target, countdownTime);
        
        // Start enhanced countdown task with flee detection
        BukkitTask countdownTask = new BukkitRunnable() {
            int remaining = countdownTime;
            
            @Override
            public void run() {
                // Check if both players are still online
                if (!guard.isOnline() || !target.isOnline()) {
                    cancelCountdown(targetId, "Player disconnected");
                    return;
                }
                
                // Enhanced distance checking with flee detection
                Location guardPos = guard.getLocation();
                Location targetPos = target.getLocation();
                Location initialGuardPos = initialGuardPositions.get(targetId);
                Location initialTargetPos = initialTargetPositions.get(targetId);
                
                if (initialGuardPos == null || initialTargetPos == null) {
                    cancelCountdown(targetId, "Position tracking error");
                    return;
                }
                
                // Calculate distances
                double currentDistance = guardPos.distance(targetPos);
                double guardMovement = guardPos.distance(initialGuardPos);
                double targetMovement = targetPos.distance(initialTargetPos);
                
                // Get flee threshold from configuration
                double fleeThreshold = plugin.getConfigManager().getJailFleeThreshold();
                
                // Check if distance exceeded jail radius
                if (currentDistance > jailRadius) {
                    // Determine who moved more to identify the "flee" cause
                    if (targetMovement > guardMovement && targetMovement > fleeThreshold) {
                        // Target fled - initiate chase if enabled and possible
                        if (chaseIntegrationEnabled && canInitiateChaseForFlee(guard, target)) {
                            initiateChaseForFlee(guard, target, targetId);
                        } else {
                            // Fall back to standard countdown cancellation
                            cancelCountdown(targetId, "Target fled jail area");
                            plugin.getMessageManager().sendMessage(guard, "jail.countdown.target-fled",
                                playerPlaceholder("target", target),
                                numberPlaceholder("distance", Math.round(currentDistance)));
                        }
                    } else {
                        // Guard moved away - standard cancellation
                        cancelCountdown(targetId, "Guard moved away from target");
                        plugin.getMessageManager().sendMessage(guard, "jail.countdown.cancelled",
                            stringPlaceholder("reason", "You moved too far from the target"));
                    }
                    return;
                }
                
                remaining--;
                
                if (remaining <= 0) {
                    // Complete the jail process
                    completeJail(guard, target);
                    cancel();
                    cleanupCountdownData(targetId);
                } else if (remaining <= 3) {
                    // Show countdown for last 3 seconds
                    plugin.getMessageManager().sendMessage(guard, "jail.countdown.progress",
                        numberPlaceholder("seconds", remaining));
                    plugin.getMessageManager().sendMessage(target, "jail.countdown.progress",
                        numberPlaceholder("seconds", remaining));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
        
        activeCountdowns.put(targetId, countdownTask);
        return true;
    }
    
    /**
     * Check if a chase can be initiated for a fleeing target
     */
    private boolean canInitiateChaseForFlee(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return false;
        }
        
        // Check if target has a wanted level
        PlayerData targetData = plugin.getDataManager().getPlayerData(target.getUniqueId());
        if (targetData == null || !targetData.isWanted()) {
            // Set a wanted level for fleeing from jail
            plugin.getWantedManager().increaseWantedLevel(target, 1, "Fleeing from jail");
        }
        
        // Check if chase manager can start a chase
        return plugin.getChaseManager().canStartChase(guard, target);
    }
    
    /**
     * Initiate a chase for a fleeing jail target
     */
    private void initiateChaseForFlee(Player guard, Player target, UUID targetId) {
        // Cancel the jail countdown first
        cancelCountdown(targetId, "Target fled - chase initiated");
        
        // Start the chase
        boolean chaseStarted = plugin.getChaseManager().startChase(guard, target);
        
        if (chaseStarted) {
            // Notify players about the chase initiation
            plugin.getMessageManager().sendMessage(guard, "jail.chase.initiated",
                playerPlaceholder("target", target));
            plugin.getMessageManager().sendMessage(target, "jail.chase.target-notification",
                playerPlaceholder("guard", guard));
            
            // Send alert to other guards
            plugin.getMessageManager().sendGuardAlert("jail.chase.guard-alert",
                playerPlaceholder("target", target),
                playerPlaceholder("guard", guard));
            
            logger.info("Initiated chase for fleeing jail target: " + target.getName() + " (Guard: " + guard.getName() + ")");
        } else {
            // Chase failed to start, fall back to standard cancellation
            plugin.getMessageManager().sendMessage(guard, "jail.countdown.target-fled",
                playerPlaceholder("target", target));
            logger.warning("Failed to initiate chase for fleeing jail target: " + target.getName());
        }
    }
    
    /**
     * Clean up countdown tracking data
     */
    private void cleanupCountdownData(UUID targetId) {
        activeCountdowns.remove(targetId);
        initialGuardPositions.remove(targetId);
        initialTargetPositions.remove(targetId);
    }
    
    private void cancelCountdown(UUID targetId, String reason) {
        BukkitTask task = activeCountdowns.get(targetId);
        if (task != null) {
            task.cancel();
        }
        
        // Clean up all tracking data
        cleanupCountdownData(targetId);
        
        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null) {
            plugin.getMessageManager().sendMessage(target, "jail.countdown.cancelled",
                stringPlaceholder("reason", reason));
            plugin.getBossBarManager().hideBossBarByType(target, "jail");
        }
    }
    
    private void completeJail(Player guard, Player target) {
        // Calculate jail time based on wanted level
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        // Clear wanted level
        plugin.getWantedManager().clearWantedLevel(target);
        
        // Remove contraband items when player is caught/jailed
        plugin.getContrabandManager().removeContrabandOnCapture(target);
        
        // Update statistics
        PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
        PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
        
        guardData.incrementArrests();
        targetData.incrementViolations();
        
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
        // Execute jail command (this would integrate with your jail plugin)
        executeJailCommand(guard, target, jailTime);
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
            playerPlaceholder("player", target),
            timePlaceholder("time", jailTime));
        plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
            timePlaceholder("time", jailTime));
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(target, "jail");
        
        // Notify other guards
        notifyGuards("jail.arrest.guard-alert",
            playerPlaceholder("guard", guard),
            playerPlaceholder("target", target));
        
        // Award performance for successful arrest
        plugin.getDutyManager().awardArrestPerformance(guard);
        
        logger.info(guard.getName() + " arrested " + target.getName() + " for " + jailTime + " seconds");
    }
    
    private int calculateJailTime(int wantedLevel) {
        int baseTime = plugin.getConfigManager().getBaseJailTime();
        int levelMultiplier = plugin.getConfigManager().getJailLevelMultiplier();
        
        return baseTime + (wantedLevel * levelMultiplier);
    }
    
    private void executeJailCommand(Player guard, Player target, int jailTime) {
        // Use the new CMI integration instead of console commands
        plugin.getCMIIntegration().jailPlayer(guard, target, jailTime, "Arrested by " + guard.getName())
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully jailed " + target.getName() + " via CMI integration");
                    
                    // Send success messages to players
                    plugin.getMessageManager().sendMessage(guard, "jail.success",
                        MessageManager.playerPlaceholder("target", target),
                        MessageManager.numberPlaceholder("time", jailTime / 60));
                    
                    plugin.getMessageManager().sendMessage(target, "jail.target-notification",
                        MessageManager.playerPlaceholder("guard", guard),
                        MessageManager.numberPlaceholder("time", jailTime / 60));
                    
                } else {
                    logger.warning("Failed to jail " + target.getName() + " via CMI integration - falling back to command");
                    
                    // Fallback to traditional command if CMI integration fails
                    try {
                        String jailCommand = "jail " + target.getName() + " " + (jailTime / 60) + " Arrested by " + guard.getName();
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
                        
                        plugin.getMessageManager().sendMessage(guard, "jail.success-fallback",
                            MessageManager.playerPlaceholder("target", target));
                        
                    } catch (Exception e) {
                        logger.severe("Fallback jail command also failed: " + e.getMessage());
                        plugin.getMessageManager().sendMessage(guard, "jail.failed",
                            MessageManager.playerPlaceholder("target", target));
                    }
                }
            })
            .exceptionally(throwable -> {
                logger.severe("CMI jail integration error: " + throwable.getMessage());
                plugin.getMessageManager().sendMessage(guard, "jail.failed",
                    MessageManager.playerPlaceholder("target", target));
                return null;
            });
    }
    
    public boolean jailPlayer(Player guard, Player target, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = plugin.getMessageManager().getPlainTextMessage("jail.no-reason");
        }
        
        // Security check: Can target be jailed?
        if (!plugin.getSecurityManager().canPlayerBeJailed(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.jail-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("jail", guard, target);
            return false;
        }
        
        // Calculate jail time
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        // Clear wanted level
        plugin.getWantedManager().clearWantedLevel(target);
        
        // Remove contraband items when player is jailed
        plugin.getContrabandManager().removeContrabandOnCapture(target);
        
        // Update statistics
        PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
        PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
        
        guardData.incrementArrests();
        targetData.incrementViolations();
        
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
        // Execute jail command
        String jailCommand = "jail " + target.getName() + " " + jailTime + " " + reason;
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
            playerPlaceholder("player", target),
            timePlaceholder("time", jailTime));
        plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
            timePlaceholder("time", jailTime),
            stringPlaceholder("reason", reason));
        
        // Award performance for successful arrest
        plugin.getDutyManager().awardArrestPerformance(guard);
        
        logger.info(guard.getName() + " jailed " + target.getName() + " for " + jailTime + " seconds - Reason: " + reason);
        return true;
    }
    
    public boolean jailOfflinePlayer(String guardName, String targetName, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = plugin.getMessageManager().getPlainTextMessage("jail.no-reason");
        }
        
        // Create final copy for lambda access
        final String finalReason = reason;
        
        // Find the guard player who is executing this command
        Player executorGuard = plugin.getServer().getPlayer(guardName);
        
        // If the executing guard is not online, find any online guard to execute the command
        if (executorGuard == null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
                    executorGuard = player;
                    break;
                }
            }
        }
        
        // Use base jail time for offline players
        int jailTime = plugin.getConfigManager().getBaseJailTime();
        String fullReason = finalReason + " (Offline arrest by " + guardName + ")";
        
        if (executorGuard != null) {
            // Use CMI integration with online guard as executor
            plugin.getCMIIntegration().jailOfflinePlayer(executorGuard, targetName, jailTime, fullReason)
                .thenAccept(success -> {
                    if (success) {
                        logger.info(guardName + " successfully jailed offline player " + targetName + 
                                   " for " + (jailTime / 60) + " minutes via CMI integration - Reason: " + finalReason);
                    } else {
                        logger.warning("CMI integration failed for offline jail - falling back to console command");
                        
                        // Fallback to console command
                        try {
                            String jailCommand = "jail " + targetName + " " + (jailTime / 60) + " " + fullReason;
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
                            logger.info("Fallback: " + guardName + " jailed offline player " + targetName + " via console command");
                        } catch (Exception e) {
                            logger.severe("Both CMI integration and fallback failed for offline jail: " + e.getMessage());
                        }
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("CMI offline jail integration error: " + throwable.getMessage());
                    return null;
                });
        } else {
            // No online guards available - use console command as last resort
            logger.warning("No online guards available for CMI integration - using console command for offline jail");
            try {
                String jailCommand = "jail " + targetName + " " + (jailTime / 60) + " " + fullReason;
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
                logger.info("Console fallback: " + guardName + " jailed offline player " + targetName);
            } catch (Exception e) {
                logger.severe("Console command fallback failed for offline jail: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isInJailCountdown(Player player) {
        return activeCountdowns.containsKey(player.getUniqueId());
    }
    
    public void cancelJailCountdown(Player player) {
        cancelCountdown(player.getUniqueId(), plugin.getMessageManager().getPlainTextMessage("jail.manual-cancellation"));
    }
    
    private void notifyGuards(String messageKey, TagResolver... placeholders) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
                plugin.getMessageManager().sendGuardAlert(messageKey, placeholders);
            }
        }
    }
    
    /**
     * Clean up manager resources
     */
    public void cleanup() {
        logger.info("Cleaning up JailManager resources...");
        
        try {
            // Cancel all active countdown tasks
            int cancelledCount = 0;
            for (Map.Entry<UUID, BukkitTask> entry : activeCountdowns.entrySet()) {
                BukkitTask task = entry.getValue();
                if (task != null && !task.isCancelled()) {
                    try {
                        task.cancel();
                        cancelledCount++;
                        
                        // Clean up boss bars for affected players
                        Player player = plugin.getServer().getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            plugin.getBossBarManager().hideBossBarByType(player, "jail");
                        }
                    } catch (Exception e) {
                        logger.warning("Error cancelling jail countdown task: " + e.getMessage());
                    }
                }
            }
            
            // Clear all tracking maps
            activeCountdowns.clear();
            initialGuardPositions.clear();
            initialTargetPositions.clear();
            
            logger.info("Cancelled " + cancelledCount + " active jail countdown tasks");
            logger.info("JailManager cleanup completed");
            
        } catch (Exception e) {
            logger.severe("Error during JailManager cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 