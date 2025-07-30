package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Collection;
import org.bukkit.Location;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ChaseManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Combat timer tracking
    private final Map<UUID, Long> combatTimers;
    private final Map<UUID, BukkitTask> combatTasks;
    
    public ChaseManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.combatTimers = new HashMap<>();
        this.combatTasks = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("ChaseManager initialized successfully!");
        
        // Clean up any existing chases with null UUIDs
        cleanupInvalidChases();
        
        // Start chase monitoring task
        startChaseMonitoring();
    }
    
    public void shutdown() {
        logger.info("ChaseManager shutting down...");
        
        // End all active chases
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.plugin-shutdown"));
        }
        
        // Clean up combat timers
        cleanupAllCombatTimers();
        
        logger.info("ChaseManager shutdown complete");
    }
    
    private void startChaseMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check for expired chases and distance violations
                monitorActiveChases();
            }
        }.runTaskTimer(plugin, 20L * 10L, 20L * 10L); // Run every 10 seconds for better performance
    }
    
    private void monitorActiveChases() {
        try {
            Collection<ChaseData> activeChases = plugin.getDataManager().getAllActiveChases();
            if (activeChases == null || activeChases.isEmpty()) {
                return;
            }
            
            // Create a copy to avoid concurrent modification issues
            List<ChaseData> chasesToMonitor = new ArrayList<>(activeChases);
            
            for (ChaseData chase : chasesToMonitor) {
                try {
                    // Comprehensive null checking
                    if (chase == null) {
                        logger.warning("Found null chase data in active chases - skipping");
                        continue;
                    }
                    
                    UUID chaseId = chase.getChaseId();
                    UUID guardId = chase.getGuardId();
                    UUID targetId = chase.getTargetId();
                    
                    // Check for null UUIDs
                    if (chaseId == null || guardId == null || targetId == null) {
                        logger.warning("Found chase with null UUIDs: chaseId=" + chaseId + 
                                     ", guardId=" + guardId + ", targetId=" + targetId + " - ending chase");
                        if (chaseId != null) {
                            endChase(chaseId, "Invalid chase data (null UUIDs)");
                        } else {
                            // If chaseId is null, we need to remove from data manager directly
                            plugin.getDataManager().removeChaseData(chase.getChaseId());
                        }
                        continue;
                    }
                    
                    Player guard = plugin.getServer().getPlayer(guardId);
                    Player target = plugin.getServer().getPlayer(targetId);
            
            // Check if players are still online
            if (guard == null || target == null) {
                        String offlinePlayer = guard == null ? "guard" : "target";
                        endChase(chaseId, "Player offline (" + offlinePlayer + ")");
                        continue;
                    }
                    
                    // Cross-world distance checking with error handling
                    try {
                        Location guardLoc = guard.getLocation();
                        Location targetLoc = target.getLocation();
                        
                        if (guardLoc == null || targetLoc == null) {
                            logger.warning("Null location for chase " + chaseId + " - ending chase");
                            endChase(chaseId, "Invalid player location");
                            continue;
                        }
                        
                        // Check if players are in the same world
                        if (!guardLoc.getWorld().equals(targetLoc.getWorld())) {
                            endChase(chaseId, "Players in different worlds");
                            continue;
                        }
                        
                        double distance;
                        try {
                            distance = guardLoc.distance(targetLoc);
                        } catch (IllegalArgumentException e) {
                            logger.warning("Distance calculation failed for chase " + chaseId + ": " + e.getMessage());
                            endChase(chaseId, "Distance calculation error");
                continue;
            }
            
                        // Check distance limits
                        int maxDistance = plugin.getConfigManager().getMaxChaseDistance();
                        if (distance > maxDistance) {
                            endChase(chaseId, "Target too far (" + Math.round(distance) + " > " + maxDistance + ")");
                continue;
            }
            
            // Check if target entered restricted area
            if (isPlayerInRestrictedArea(target)) {
                            endChase(chaseId, "Target entered restricted area");
                continue;
            }
            
                        // Safe boss bar updates with error handling
                        try {
            plugin.getBossBarManager().updateChaseBossBar(guard, distance, target);
            plugin.getBossBarManager().updateChaseBossBar(target, distance, guard);
                        } catch (Exception e) {
                            logger.warning("Boss bar update failed for chase " + chaseId + ": " + e.getMessage());
                            // Continue chase even if boss bar update fails
                        }
            
            // Send distance warnings
                        int warningDistance = plugin.getConfigManager().getChaseWarningDistance();
                        if (distance > warningDistance) {
                            try {
                plugin.getMessageManager().sendMessage(guard, "chase.warnings.distance",
                                    MessageManager.numberPlaceholder("distance", Math.round(distance)));
                            } catch (Exception e) {
                                logger.warning("Warning message failed for chase " + chaseId + ": " + e.getMessage());
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.severe("Location processing failed for chase " + chaseId + ": " + e.getMessage());
                        endChase(chaseId, "Location processing error");
                        continue;
                    }
                    
                } catch (Exception e) {
                    logger.severe("Chase monitoring failed for chase data: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Attempt emergency cleanup if possible
                    if (chase != null && chase.getChaseId() != null) {
                        try {
                            endChase(chase.getChaseId(), "Monitor error - emergency cleanup");
                        } catch (Exception cleanupError) {
                            logger.severe("Emergency cleanup failed: " + cleanupError.getMessage());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.severe("Critical error in chase monitoring: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === COMBAT TIMER METHODS ===
    
    public void handleCombatEvent(Player player) {
        if (!plugin.getConfigManager().shouldPreventCaptureInCombat()) {
            return; // Combat timer disabled
        }
        
        UUID playerId = player.getUniqueId();
        int duration = plugin.getConfigManager().getCombatTimerDuration();
        
        // Set combat timer
        combatTimers.put(playerId, System.currentTimeMillis() + (duration * 1000L));
        
        // Cancel existing combat task
        BukkitTask existingTask = combatTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Show combat timer boss bar
        plugin.getBossBarManager().showCombatBossBar(player, duration);
        
        // Send action bar notification
        plugin.getMessageManager().sendActionBar(player, "actionbar.combat-active");
        
        // Send combat message
        plugin.getMessageManager().sendMessage(player, "combat.timer-started");
        
        // Start countdown task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                endCombatTimer(player);
            }
        }.runTaskLater(plugin, duration * 20L);
        
        combatTasks.put(playerId, task);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer started for " + player.getName() + " (" + duration + "s)");
        }
    }
    
    public void endCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Remove combat timer
        combatTimers.remove(playerId);
        
        // Cancel task
        BukkitTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(player, "combat");
        
        // Send end message
        plugin.getMessageManager().sendMessage(player, "combat.timer-ended");
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer ended for " + player.getName());
        }
    }
    
    public boolean isInCombat(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return false;
        
        if (System.currentTimeMillis() >= combatEnd) {
            // Timer expired, clean up
            endCombatTimer(player);
            return false;
        }
        
        return true;
    }
    
    public long getRemainingCombatTime(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return 0;
        
        long remaining = combatEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000L);
        }

    // === ENHANCED CHASE METHODS ===
    
    public boolean startChase(Player guard, Player target) {
<<<<<<< HEAD
        // Input validation
        if (guard == null || target == null) {
            logger.warning("Cannot start chase: null player provided (guard=" + guard + ", target=" + target + ")");
=======
        // Validate chase can start
        if (!canStartChaseWithMessages(guard, target)) {
            return false;
        }
        
        // Check security restrictions
        if (!plugin.getSecurityManager().canPlayerBeChased(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.chase-protected");
            plugin.getSecurityManager().logSecurityViolation("start chase", guard, target);
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
            return false;
        }
        
        UUID guardId = guard.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        if (guardId == null || targetId == null) {
            logger.warning("Cannot start chase: null UUID (guard=" + guardId + ", target=" + targetId + ")");
            return false;
        }
        
        try {
        // Validate chase can start
        if (!canStartChaseWithMessages(guard, target)) {
            return false;
        }
        
        // Check security restrictions
        if (!plugin.getSecurityManager().canPlayerBeChased(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.chase-protected");
            plugin.getSecurityManager().logSecurityViolation("start chase", guard, target);
            return false;
        }
        
        // Check safe zones with error handling
        try {
            if (plugin.getWorldGuardUtils().isPlayerInSafeZone(target)) {
                plugin.getMessageManager().sendMessage(guard, "chase.restrictions.in-safe-zone");
                return false;
            }
        } catch (Exception e) {
            logger.warning("Safe zone check failed for chase start: " + e.getMessage());
            plugin.getMessageManager().sendMessage(guard, "chase.errors.region-check-failed");
            return false;
        }
        
        // Cross-world location validation
        try {
            Location guardLoc = guard.getLocation();
            Location targetLoc = target.getLocation();
                
                if (guardLoc == null || targetLoc == null) {
                    logger.warning("Cannot start chase: null location (guard=" + guardLoc + ", target=" + targetLoc + ")");
                    plugin.getMessageManager().sendMessage(guard, "chase.errors.invalid-location");
                    return false;
                }
                
                if (!guardLoc.getWorld().equals(targetLoc.getWorld())) {
                    plugin.getMessageManager().sendMessage(guard, "chase.restrictions.different-worlds");
                    return false;
                }
                
                // Initial distance check
                double initialDistance = guardLoc.distance(targetLoc);
                if (initialDistance > plugin.getConfigManager().getMaxChaseDistance()) {
                    plugin.getMessageManager().sendMessage(guard, "chase.restrictions.too-far-to-start",
                        MessageManager.numberPlaceholder("distance", Math.round(initialDistance)),
                        MessageManager.numberPlaceholder("max_distance", plugin.getConfigManager().getMaxChaseDistance()));
                    return false;
                }
                
            } catch (Exception e) {
                logger.warning("Location validation failed for chase start: " + e.getMessage());
                plugin.getMessageManager().sendMessage(guard, "chase.errors.location-validation-failed");
            return false;
        }
        
        // Create chase data
        UUID chaseId = UUID.randomUUID();
            long duration = plugin.getConfigManager().getChaseDuration() * 1000L;
            ChaseData chase = new ChaseData(chaseId, guardId, targetId, duration);
            
            // Update player data with rollback capability
            PlayerData guardData = null;
            PlayerData targetData = null;
            boolean dataUpdated = false;
            
            try {
                guardData = plugin.getDataManager().getOrCreatePlayerData(guardId, guard.getName());
                targetData = plugin.getDataManager().getOrCreatePlayerData(targetId, target.getName());
                
                if (guardData == null || targetData == null) {
                    logger.warning("Failed to create/load player data for chase");
                    plugin.getMessageManager().sendMessage(guard, "chase.errors.data-load-failed");
                    return false;
                }
                
                // Store original chase states for rollback
                boolean originalTargetChased = targetData.isBeingChased();
                UUID originalChaser = targetData.getChaserGuard();
                long originalChaseStart = targetData.getChaseStartTime();
                
                // Update target data
        targetData.setBeingChased(true);
                targetData.setChaserGuard(guardId);
        targetData.setChaseStartTime(System.currentTimeMillis());
        
                // Save player data
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
<<<<<<< HEAD
                // Add chase data to manager
                plugin.getDataManager().addChaseData(chase);
                dataUpdated = true;
                
                // Show boss bars with error handling
                try {
        double distance = guard.getLocation().distance(target.getLocation());
        plugin.getBossBarManager().showChaseGuardBossBar(guard, target, distance);
        plugin.getBossBarManager().showChaseTargetBossBar(target, guard, distance);
                } catch (Exception e) {
                    logger.warning("Boss bar creation failed for chase " + chaseId + ": " + e.getMessage());
                    // Continue without boss bars - not critical
                }
        
                // Send messages with error handling
                try {
=======
        // Show boss bars
        double distance = guard.getLocation().distance(target.getLocation());
        plugin.getBossBarManager().showChaseGuardBossBar(guard, target, distance);
        plugin.getBossBarManager().showChaseTargetBossBar(target, guard, distance);
        
        // Send messages
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
        plugin.getMessageManager().sendMessage(guard, "chase.start.success", 
            playerPlaceholder("target", target));
        plugin.getMessageManager().sendMessage(target, "chase.start.target-notification", 
            playerPlaceholder("guard", guard));
        
        // Send guard alert
        plugin.getMessageManager().sendGuardAlert("chase.start.guard-alert",
            playerPlaceholder("guard", guard),
            playerPlaceholder("target", target));
                } catch (Exception e) {
                    logger.warning("Message sending failed for chase " + chaseId + ": " + e.getMessage());
                    // Continue - messages are not critical for chase functionality
                }
        
                logger.info("Chase started successfully: " + guard.getName() + " -> " + target.getName() + " (ID: " + chaseId + ")");
        return true;
                
            } catch (Exception e) {
                logger.severe("Failed to create chase data: " + e.getMessage());
                e.printStackTrace();
                
                // Rollback on failure
                if (dataUpdated) {
                    try {
                        logger.info("Attempting rollback for failed chase " + chaseId);
                        
                        // Remove chase data
                        plugin.getDataManager().removeChaseData(chaseId);
                        
                        // Restore original player data states
                        if (targetData != null) {
                            targetData.setBeingChased(false);
                            targetData.setChaserGuard(null);
                            targetData.setChaseStartTime(0);
                            plugin.getDataManager().savePlayerData(targetData);
                        }
                        
                        // Hide any boss bars that might have been created
                        plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
                        plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
                        
                        logger.info("Rollback completed for failed chase " + chaseId);
                    } catch (Exception rollbackError) {
                        logger.severe("Rollback failed for chase " + chaseId + ": " + rollbackError.getMessage());
                    }
                }
                
                plugin.getMessageManager().sendMessage(guard, "chase.errors.creation-failed");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("Critical error in startChase: " + e.getMessage());
            e.printStackTrace();
            
            try {
                plugin.getMessageManager().sendMessage(guard, "chase.errors.critical-error");
            } catch (Exception msgError) {
                logger.severe("Failed to send error message: " + msgError.getMessage());
            }
            
            return false;
        }
    }
    
    public boolean endChase(UUID chaseId, String reason) {
        // Input validation
        if (chaseId == null) {
            logger.warning("Cannot end chase: null chaseId provided");
            return false;
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Unknown reason";
        }
        
        try {
            ChaseData chase = plugin.getDataManager().getChaseData(chaseId);
            if (chase == null) {
                logger.fine("Chase " + chaseId + " not found - may have already been ended");
                return false;
            }
            
            UUID guardId = chase.getGuardId();
            UUID targetId = chase.getTargetId();
            Player guard = null;
            Player target = null;
            
            // Safe player retrieval with null checking
            try {
                if (guardId != null) {
                    guard = plugin.getServer().getPlayer(guardId);
                }
                if (targetId != null) {
                    target = plugin.getServer().getPlayer(targetId);
                }
            } catch (Exception e) {
                logger.warning("Error retrieving players for chase " + chaseId + ": " + e.getMessage());
            }
            
            // Clear player data with error handling
            boolean guardDataCleared = false;
            boolean targetDataCleared = false;
            
            if (guard != null && guardId != null) {
                try {
                    PlayerData guardData = plugin.getDataManager().getPlayerData(guardId);
                    if (guardData != null) {
                        // Guard data doesn't have chase state, just save to ensure consistency
                        plugin.getDataManager().savePlayerData(guardData);
                        guardDataCleared = true;
                    }
                } catch (Exception e) {
                    logger.warning("Error clearing guard data for chase " + chaseId + ": " + e.getMessage());
                }
            }
            
            if (target != null && targetId != null) {
                try {
                    PlayerData targetData = plugin.getDataManager().getPlayerData(targetId);
            if (targetData != null) {
                targetData.clearChaseData();
                plugin.getDataManager().savePlayerData(targetData);
                        targetDataCleared = true;
                    }
                } catch (Exception e) {
                    logger.warning("Error clearing target data for chase " + chaseId + ": " + e.getMessage());
                }
            }
            
            // Remove chase from data manager
            boolean chaseDataRemoved = false;
            try {
                plugin.getDataManager().removeChaseData(chaseId);
                chaseDataRemoved = true;
            } catch (Exception e) {
                logger.severe("Error removing chase data for " + chaseId + ": " + e.getMessage());
            }
            
            // Hide boss bars with error handling
        if (guard != null) {
                try {
            plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
                } catch (Exception e) {
                    logger.warning("Error hiding guard boss bar for chase " + chaseId + ": " + e.getMessage());
                }
        }
            
        if (target != null) {
                try {
            plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
                } catch (Exception e) {
                    logger.warning("Error hiding target boss bar for chase " + chaseId + ": " + e.getMessage());
                }
        }
        
<<<<<<< HEAD
            // Send end messages with error handling
=======
        // Remove chase from data
        plugin.getDataManager().removeChaseData(chaseId);
        
        // Hide boss bars
        if (guard != null) {
            plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
        }
        if (target != null) {
            plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
        }
        
        // Send end messages
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
        if (guard != null) {
                try {
            plugin.getMessageManager().sendMessage(guard, "chase.end.success",
                stringPlaceholder("reason", reason));
                } catch (Exception e) {
                    logger.warning("Error sending end message to guard for chase " + chaseId + ": " + e.getMessage());
                }
        }
        
        if (target != null) {
                try {
            plugin.getMessageManager().sendMessage(target, "chase.end.target-notification",
                stringPlaceholder("reason", reason));
                } catch (Exception e) {
                    logger.warning("Error sending end message to target for chase " + chaseId + ": " + e.getMessage());
                }
            }
            
            // Clear any remaining combat timers
            try {
                if (guard != null) {
                    endCombatTimer(guard);
                }
                if (target != null) {
                    endCombatTimer(target);
                }
            } catch (Exception e) {
                logger.warning("Error clearing combat timers for chase " + chaseId + ": " + e.getMessage());
            }
            
            // Log the completion with status
            String guardName = guard != null ? guard.getName() : (guardId != null ? guardId.toString() : "unknown");
            String targetName = target != null ? target.getName() : (targetId != null ? targetId.toString() : "unknown");
            
            logger.info("Chase ended: " + guardName + " -> " + targetName + " (" + reason + ")" +
                       " [Data removed: " + chaseDataRemoved + ", Guard cleared: " + guardDataCleared + 
                       ", Target cleared: " + targetDataCleared + "]");
            
            return chaseDataRemoved; // Success if we at least removed the core chase data
            
        } catch (Exception e) {
            logger.severe("Critical error ending chase " + chaseId + ": " + e.getMessage());
            e.printStackTrace();
            
            // Emergency cleanup attempt
            try {
                plugin.getDataManager().removeChaseData(chaseId);
                logger.info("Emergency cleanup completed for chase " + chaseId);
            } catch (Exception emergencyError) {
                logger.severe("Emergency cleanup failed for chase " + chaseId + ": " + emergencyError.getMessage());
            }
            
            return false;
        }
    }
    
    /**
     * Check if a chase can start between guard and target
     * @param guard The guard attempting to start the chase
     * @param target The target to be chased
     * @return true if chase can start, false otherwise
     */
    public boolean canStartChase(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return false;
        }
        
        // Check if target is wanted
        if (!plugin.getWantedManager().isWanted(target)) {
            return false;
        }
        
        // Check if target is in combat (enhanced restriction)
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            return false;
        }
        
        // Check if target is in restricted area
        if (isPlayerInRestrictedArea(target)) {
            return false;
        }
        
        // Check max concurrent chases
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            return false;
        }
        
        // Check if guard is already chasing someone
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            return false;
        }
        
        // Check if target is already being chased
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            return false;
        }
        
        // Check if trying to chase themselves
        if (guard.equals(target)) {
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            return false;
        }
        
        return true;
    }
    
<<<<<<< HEAD
=======
    /**
     * Check if a chase can start between guard and target
     * @param guard The guard attempting to start the chase
     * @param target The target to be chased
     * @return true if chase can start, false otherwise
     */
    public boolean canStartChase(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return false;
        }
        
        // Check if target is wanted
        if (!plugin.getWantedManager().isWanted(target)) {
            return false;
        }
        
        // Check if target is in combat (enhanced restriction)
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            return false;
        }
        
        // Check if target is in restricted area
        if (isPlayerInRestrictedArea(target)) {
            return false;
        }
        
        // Check max concurrent chases
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            return false;
        }
        
        // Check if guard is already chasing someone
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            return false;
        }
        
        // Check if target is already being chased
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            return false;
        }
        
        // Check if trying to chase themselves
        if (guard.equals(target)) {
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            return false;
        }
        
        return true;
    }
    
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
    private boolean canStartChaseWithMessages(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.not-on-duty");
            return false;
        }
        
        // Check if target is wanted
        if (!plugin.getWantedManager().isWanted(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-not-wanted");
            return false;
        }
        
        // Check if target is in combat (enhanced restriction)
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-in-combat");
            return false;
        }
        
        // Check if target is in restricted area
        if (isPlayerInRestrictedArea(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.area-restricted");
            return false;
        }
        
        // Check max concurrent chases
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.max-concurrent");
            return false;
        }
        
        // Check if guard is already chasing someone
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.already-chasing");
            return false;
        }
        
        // Check if target is already being chased
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-being-chased");
            return false;
        }
        
        // Check if trying to chase themselves
        if (guard.equals(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.cannot-chase-self");
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.too-far");
            return false;
        }
        
        return true;
    }
    
    public boolean captureTarget(Player guard, Player target) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
        if (chase == null) {
            return false;
        }
        
        // End the chase
        endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.target-captured"));
        
        // Start jail process
        return plugin.getJailManager().startJailCountdown(guard, target);
    }
    

    
    public boolean isPlayerInChase(Player player) {
        return plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) || 
               plugin.getDataManager().isGuardChasing(player.getUniqueId());
    }
    
    /**
     * Check if a player is in a restricted area for chases
     */
    public boolean isPlayerInRestrictedArea(Player player) {
        if (!plugin.getConfigManager().shouldBlockRestrictedAreas()) {
            return false;
        }
        
        String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, restrictedAreas);
    }
    
    public ChaseData getChaseByPlayer(Player player) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase != null) return chase;
        
        return plugin.getDataManager().getChaseByTarget(player.getUniqueId());
    }
    
    // === CLEANUP METHODS ===
    
    private void cleanupInvalidChases() {
        List<ChaseData> invalidChases = new ArrayList<>();
        
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            if (chase.getGuardId() == null || chase.getTargetId() == null) {
                invalidChases.add(chase);
            }
        }
        
        for (ChaseData chase : invalidChases) {
            logger.warning("Cleaning up invalid chase with null UUIDs: " + chase.getChaseId());
            plugin.getDataManager().removeChaseData(chase.getChaseId());
        }
        
        if (!invalidChases.isEmpty()) {
            logger.info("Cleaned up " + invalidChases.size() + " invalid chases with null UUIDs");
        }
    }
    
    private void cleanupAllCombatTimers() {
        for (Map.Entry<UUID, BukkitTask> entry : combatTasks.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }
            
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                plugin.getBossBarManager().hideBossBarByType(player, "combat");
            }
        }
        
        combatTimers.clear();
        combatTasks.clear();
    }
    
    public void cleanupPlayer(Player player) {
        // End any combat timer
        endCombatTimer(player);
    }
} 
