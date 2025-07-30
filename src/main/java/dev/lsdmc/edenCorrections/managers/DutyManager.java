package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.InventorySerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DutyManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Integration dependencies
    private LuckPerms luckPerms;
    
    // Duty transition management
    private final Map<UUID, BukkitTask> dutyTransitions;
    private final Map<UUID, Location> transitionLocations;
    
    // Inventory caching for duty management
    private final Map<UUID, String> inventoryCache;
    private final List<Material> guardKitItems;
    
    public DutyManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dutyTransitions = new HashMap<>();
        this.transitionLocations = new HashMap<>();
        this.inventoryCache = new HashMap<>();
        this.guardKitItems = InventorySerializer.getCommonGuardKitItems();
    }
    
    public void initialize() {
        logger.info("DutyManager initializing...");
        
        // Initialize integrations
        initializeLuckPerms();
        
        // Start duty monitoring task
        startDutyMonitoring();
        
        logger.info("DutyManager initialized successfully!");
    }
    
    private void initializeLuckPerms() {
        RegisteredServiceProvider<LuckPerms> lpProvider = 
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (lpProvider != null) {
            luckPerms = lpProvider.getProvider();
            logger.info("LuckPerms integration enabled - rank detection available");
        } else {
            logger.warning("LuckPerms not found - using permission-based rank detection");
        }
    }
    

    
    private void startDutyMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check for expired off-duty times and other duty-related tasks
                checkDutyStatus();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    private void checkDutyStatus() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (hasGuardPermission(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null) {
                    checkOffDutyTimeEarning(player, data);
                    checkOffDutyTimeConsumption(player, data);
                }
            }
        }
    }
    
    /**
     * NEW: Check if guards have earned base off-duty time or performance bonuses
     */
    private void checkOffDutyTimeEarning(Player player, PlayerData data) {
        if (!data.isOnDuty()) return;
        
        long dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
        int dutyMinutes = (int) (dutyTime / (1000L * 60L));
        
        // Check if they've earned base time
        if (!data.hasEarnedBaseTime() && dutyMinutes >= plugin.getConfigManager().getBaseDutyRequirement()) {
            awardBaseOffDutyTime(player, data);
        }
        
        // Check for performance bonuses
        checkPerformanceBonuses(player, data);
        
        // Check for time-based bonuses (every hour)
        if (dutyMinutes > 0 && dutyMinutes % 60 == 0) {
            awardTimeBasedBonus(player, data);
        }
    }
    
    private void checkOffDutyTimeConsumption(Player player, PlayerData data) {
        if (data.isOnDuty()) {
            // Player went back on duty - clear any penalty tracking
            if (data.isPenaltyTrackingActive()) {
                clearOffDutyPenalties(player, data);
            }
            return;
        }
        
        long timeSinceOffDuty = System.currentTimeMillis() - data.getOffDutyTime();
        long earnedTime = data.getEarnedOffDutyTime();
        
        if (timeSinceOffDuty > earnedTime) {
            // They've used up their earned off-duty time - apply escalating penalties
            
            // Safeguard: If they've been off duty for an extremely long time (more than 30 days),
            // give them a fresh start to prevent permanent penalties
            long maxReasonableOffDutyTime = 30 * 24 * 60 * 60 * 1000L; // 30 days
            if (timeSinceOffDuty > maxReasonableOffDutyTime) {
                logger.warning("Player " + player.getName() + " has been off duty for " + 
                             (timeSinceOffDuty / (24 * 60 * 60 * 1000L)) + " days - giving fresh start");
                data.setOffDutyTime(System.currentTimeMillis());
                data.setEarnedOffDutyTime(earnedTime); // Keep their earned time
                data.clearPenaltyTracking();
                data.setHasBeenNotifiedOfExpiredTime(false);
                plugin.getDataManager().savePlayerData(data);
                return;
            }
            
            if (!data.hasBeenNotifiedOfExpiredTime()) {
                // First time notification
                plugin.getMessageManager().sendMessage(player, "duty.restrictions.off-duty-time-expired");
                plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
                data.setHasBeenNotifiedOfExpiredTime(true);
                
                // Start penalty tracking only if not already active
                if (!data.isPenaltyTrackingActive()) {
                    data.initializePenaltyTracking();
                    // Set the penalty start time to when they actually exceeded their earned time
                    data.setPenaltyStartTime(data.getOffDutyTime() + earnedTime);
                    plugin.getDataManager().savePlayerData(data);
                    logger.info(player.getName() + " has used up their earned off-duty time - penalty tracking initiated");
                }
            }
            
            // Apply escalating penalties if system is enabled
            if (plugin.getConfigManager().isPenaltyEscalationEnabled()) {
                // CRITICAL FIX: Calculate overrun time from when penalties should have started
                // not from when they went off duty
                long penaltyStartTime = data.getPenaltyStartTime();
                long currentTime = System.currentTimeMillis();
                long overrunTime = currentTime - penaltyStartTime;
                
                // Safeguard: If penalty tracking was reset but player has been off duty for too long,
                // cap the overrun time to a reasonable maximum
                if (penaltyStartTime == 0 || overrunTime < 0) {
                    // Recalculate penalty start time
                    penaltyStartTime = data.getOffDutyTime() + earnedTime;
                    overrunTime = currentTime - penaltyStartTime;
                    data.setPenaltyStartTime(penaltyStartTime);
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        logger.info("Recalculated penalty start time for " + player.getName() + 
                                   " to " + penaltyStartTime);
                    }
                }
                
                // Cap overrun time to prevent extreme values
                long maxReasonableOverrun = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
                if (overrunTime > maxReasonableOverrun) {
                    logger.warning("Capping overrun time for " + player.getName() + " from " + 
                                 (overrunTime / (60 * 1000L)) + " minutes to " + 
                                 (maxReasonableOverrun / (60 * 1000L)) + " minutes (7 days)");
                    overrunTime = maxReasonableOverrun;
                }
                
                // Add debugging for extreme values
                if (overrunTime > 24 * 60 * 60 * 1000L) { // More than 24 hours
                    logger.warning("Extreme overrun time detected for " + player.getName() + 
                                 ": " + (overrunTime / (60 * 1000L)) + " minutes (" + 
                                 (overrunTime / (24 * 60 * 60 * 1000L)) + " days)");
                    logger.warning("  penaltyStartTime: " + penaltyStartTime);
                    logger.warning("  currentTime: " + currentTime);
                    logger.warning("  timeSinceOffDuty: " + (timeSinceOffDuty / (60 * 1000L)) + " minutes");
                    logger.warning("  earnedTime: " + (earnedTime / (60 * 1000L)) + " minutes");
                }
                
                applyEscalatingPenalties(player, data, overrunTime);
            }
        }
    }
    
    /**
     * Clear all penalties when player goes back on duty
     */
    private void clearOffDutyPenalties(Player player, PlayerData data) {
        // Remove slowness effect
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        
        // Hide penalty boss bar
        if (data.hasActivePenaltyBossBar()) {
            plugin.getBossBarManager().hideBossBarByType(player, "penalty");
            data.setHasActivePenaltyBossBar(false);
        }
        
        // Clear penalty tracking completely
        data.clearPenaltyTracking();
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        // Save the cleared state
        plugin.getDataManager().savePlayerData(data);
        
        // Send relief message
        plugin.getMessageManager().sendSuccess(player, "duty.penalties.cleared");
        
        logger.info("Cleared off-duty penalties for " + player.getName());
    }
    
    /**
     * Apply escalating penalties based on time overrun
     */
    private void applyEscalatingPenalties(Player player, PlayerData data, long overrunTime) {
        long overrunMinutes = overrunTime / (60 * 1000L);
        
        // Get configuration values
        int gracePeriod = plugin.getConfigManager().getPenaltyGracePeriod();
        int stage1Time = plugin.getConfigManager().getPenaltyStage1Time();
        int stage2Time = plugin.getConfigManager().getPenaltyStage2Time();
        int recurringInterval = plugin.getConfigManager().getPenaltyRecurringInterval();
        
        // Debug logging
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Penalty calculation for " + player.getName() + ":");
            logger.info("  Overrun time: " + overrunMinutes + " minutes");
            logger.info("  Grace period: " + gracePeriod + " minutes");
            logger.info("  Current stage: " + data.getCurrentPenaltyStage());
            logger.info("  Stage 1 time: " + stage1Time + " minutes");
            logger.info("  Stage 2 time: " + stage2Time + " minutes");
            logger.info("  Recurring interval: " + recurringInterval + " minutes");
            logger.info("  Last penalty time: " + data.getLastPenaltyTime());
        }
        
        // Skip if still in grace period
        if (overrunMinutes < gracePeriod) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("  Still in grace period - no penalties applied");
            }
            return;
        }
        
        long effectiveOverrunMinutes = overrunMinutes - gracePeriod;
        
        // CRITICAL FIX: Apply penalties gradually, one stage at a time
        int currentStage = data.getCurrentPenaltyStage();
        int nextStage = currentStage + 1;
        
        // Check if enough time has passed for next penalty stage
        long timeSinceLastPenalty = System.currentTimeMillis() - data.getLastPenaltyTime();
        long minimumIntervalMs = Math.max(recurringInterval * 60 * 1000L, 60000L); // At least 1 minute between penalties
        
        // Determine if we should apply the next penalty stage
        boolean shouldApplyNextStage = false;
        
        if (currentStage == 0) {
            // No penalties yet - check if we should apply stage 1
            shouldApplyNextStage = effectiveOverrunMinutes >= stage1Time;
        } else if (currentStage == 1) {
            // Stage 1 applied - check if we should apply stage 2
            shouldApplyNextStage = (effectiveOverrunMinutes >= stage2Time) && 
                                   (timeSinceLastPenalty >= minimumIntervalMs);
        } else if (currentStage >= 2) {
            // Stage 2 or higher - check if we should apply recurring penalty
            long timeForNextRecurring = stage2Time + ((currentStage - 1) * recurringInterval);
            shouldApplyNextStage = (effectiveOverrunMinutes >= timeForNextRecurring) && 
                                   (timeSinceLastPenalty >= minimumIntervalMs);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("  Effective overrun: " + effectiveOverrunMinutes + " minutes");
            logger.info("  Current stage: " + currentStage + ", next stage: " + nextStage);
            logger.info("  Time since last penalty: " + (timeSinceLastPenalty / 1000L) + " seconds");
            logger.info("  Should apply next stage: " + shouldApplyNextStage);
        }
        
        if (shouldApplyNextStage) {
            // Apply the next penalty stage
            data.setCurrentPenaltyStage(nextStage);
            
            // Apply appropriate penalty for the new stage
            if (nextStage == 1) {
                applyStage1Penalty(player, data);
            } else if (nextStage == 2) {
                applyStage2Penalty(player, data);
            } else if (nextStage >= 3) {
                applyRecurringPenalty(player, data);
            }
            
            data.setLastPenaltyTime(System.currentTimeMillis());
            plugin.getDataManager().savePlayerData(data);
            
            // Update boss bar only when stage changes
            updatePenaltyBossBar(player, data, overrunMinutes);
            data.setHasActivePenaltyBossBar(true);
            
            logger.info("Applied penalty stage " + nextStage + " to " + player.getName() + 
                       " (effective overrun: " + effectiveOverrunMinutes + " minutes)");
        } else if (plugin.getConfigManager().isDebugMode()) {
            logger.info("  No penalty applied - conditions not met");
        }
    }
    
    /**
     * Apply Stage 1 penalty (first warning + light slowness only)
     */
    private void applyStage1Penalty(Player player, PlayerData data) {
        // Apply slowness effect
        int slownessLevel = plugin.getConfigManager().getPenaltyStage1SlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE, // Permanent until cleared
            slownessLevel - 1 // Bukkit uses 0-based levels
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        // Send warning message
        if (plugin.getConfigManager().isPenaltyStage1WarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.stage1-applied", 
                MessageManager.numberPlaceholder("slowness_level", slownessLevel)
            );
        }
        
        logger.info("Applied Stage 1 off-duty penalty to " + player.getName() + 
                   " (Slowness " + slownessLevel + ")");
    }
    
    /**
     * Apply Stage 2 penalty (stronger slowness only)
     */
    private void applyStage2Penalty(Player player, PlayerData data) {
        // Upgrade slowness effect
        int slownessLevel = plugin.getConfigManager().getPenaltyStage2SlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE,
            slownessLevel - 1
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        // Send warning message
        if (plugin.getConfigManager().isPenaltyStage2WarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.stage2-applied",
                MessageManager.numberPlaceholder("slowness_level", slownessLevel)
            );
        }
        
        logger.info("Applied Stage 2 off-duty penalty to " + player.getName() + 
                   " (Slowness " + slownessLevel + ")");
    }
    
    /**
     * Apply recurring penalty (every interval after stage 2 - slowness + economy penalty)
     */
    private void applyRecurringPenalty(Player player, PlayerData data) {
        // Maintain current slowness level
        int slownessLevel = plugin.getConfigManager().getPenaltyRecurringSlownessLevel();
        PotionEffect slowness = new PotionEffect(
            PotionEffectType.SLOWNESS, 
            Integer.MAX_VALUE,
            slownessLevel - 1
        );
        player.addPotionEffect(slowness);
        data.setLastSlownessApplication(System.currentTimeMillis());
        
        // Apply economy penalty
        int economyPenalty = plugin.getConfigManager().getPenaltyRecurringEconomyPenalty();
        applyEconomyPenalty(player, economyPenalty, "recurring off-duty violation");
        
        // Send warning message
        if (plugin.getConfigManager().isPenaltyRecurringWarningEnabled()) {
            plugin.getMessageManager().sendWarning(player, "duty.penalties.recurring-applied",
                MessageManager.numberPlaceholder("penalty", economyPenalty),
                MessageManager.numberPlaceholder("stage", data.getCurrentPenaltyStage())
            );
        }
        
        logger.info("Applied recurring off-duty penalty to " + player.getName() + 
                   " (Stage " + data.getCurrentPenaltyStage() + ", $" + economyPenalty + " deducted)");
    }
    
    /**
     * Apply economy penalty using Vault economy system
     */
    private void applyEconomyPenalty(Player player, int amount, String reason) {
        // Use Vault economy system for penalties
        plugin.getVaultEconomyManager().takeMoney(player, amount, reason)
            .thenAccept(success -> {
                if (success) {
                    logger.info("Successfully deducted $" + amount + " from " + 
                               player.getName() + " (Reason: " + reason + ")");
                } else {
                    logger.warning("Failed to deduct money from " + player.getName() + 
                                 " - Vault economy operation failed or insufficient funds");
                }
            })
            .exceptionally(throwable -> {
                logger.severe("Error executing economy penalty for " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Update or show penalty boss bar using existing BossBarManager
     */
    private void updatePenaltyBossBar(Player player, PlayerData data, long overrunMinutes) {
        if (!plugin.getConfigManager().isPenaltyBossBarEnabled()) {
            return;
        }
        
        // Use the new penalty boss bar method
        plugin.getBossBarManager().showPenaltyBossBar(player, data.getCurrentPenaltyStage(), overrunMinutes);
        data.setHasActivePenaltyBossBar(true);
    }

    // === ENHANCED DUTY MANAGEMENT ===
    
    public boolean toggleDuty(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        if (data.isOnDuty()) {
            return goOffDuty(player, data);
        } else {
            return initiateGuardDuty(player); // Enhanced method
        }
    }
    
    public boolean initiateGuardDuty(Player player) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: " + player.getName() + " attempting to go on duty");
        
        
        }        
        if (data.isOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "duty.activation.already-on");
            return false;
        }
        
        


        // STRICT GUARD RANK VALIDATION - Even OPs must have proper LuckPerms rank
        String guardRank = getPlayerGuardRank(player);
        if (guardRank == null) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.no-rank");
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + player.getName() + " denied duty - no valid guard rank");
                // Show what groups they have for debugging
                debugPlayerGroups(player);
            }
            return false;
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: " + player.getName() + " has valid guard rank: " + guardRank);
        }
        
        // Check if player can go on duty (time restriction)
        if (!canGoOnDuty(data, player)) {
            long remainingTime = getRemainingOffDutyTime(data);
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.insufficient-time",
                timePlaceholder("time", remainingTime));
            return false;
        }
        
        // Check WorldGuard region requirement
        if (!isInDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wrong-region");
            return false;
        }
        
        // Check if player is in combat
        if (plugin.getChaseManager().isInCombat(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.combat-active");
            return false;
        }
        
        // Check if player is wanted (guards cannot go on duty while wanted)
        if (plugin.getWantedManager().isWanted(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wanted-active");
            return false;
        }
        
        // Store detected rank
        data.setGuardRank(guardRank);
        plugin.getDataManager().savePlayerData(data);
        
        plugin.getMessageManager().sendMessage(player, "duty.activation.rank-detected", 
            stringPlaceholder("rank", guardRank));
        
        // Start immobilization countdown
        return startDutyTransition(player, guardRank);
    }

    
    // Debug helper method
    private void debugPlayerGroups(Player player) {
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    logger.info("DEBUG: " + player.getName() + " has LuckPerms groups:");
                    user.getInheritedGroups(user.getQueryOptions()).forEach(group -> {
                        logger.info("DEBUG: - " + group.getName());
                    });
                    
                    Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
                    logger.info("DEBUG: Current rank mappings in config:");
                    for (Map.Entry<String, String> entry : rankMappings.entrySet()) {
                        logger.info("DEBUG: - " + entry.getKey() + " -> " + entry.getValue());
                    }
                } else {
                    logger.info("DEBUG: " + player.getName() + " has no LuckPerms user data");
                }
            } catch (Exception e) {
                logger.warning("DEBUG: Error checking " + player.getName() + " groups: " + e.getMessage());
            }
        } else {
            logger.info("DEBUG: LuckPerms not available for " + player.getName());
        }
    }
    
    private boolean startDutyTransition(Player player, String guardRank) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing transition
        cancelDutyTransition(player, null);
        
        // Store player's current location
        transitionLocations.put(playerId, player.getLocation().clone());
        
        int immobilizationTime = plugin.getConfigManager().getImmobilizationTime();
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Starting duty transition for " + player.getName() + " (" + immobilizationTime + "s)");
        }
        
        // Show countdown boss bar
        plugin.getBossBarManager().showDutyBossBar(player, immobilizationTime, guardRank);
        
        // Start immobilization task
        BukkitTask task = new BukkitRunnable() {
            private int remaining = immobilizationTime;
            
            @Override
            public void run() {
                // Check if player is still online
                if (!player.isOnline()) {
                    dutyTransitions.remove(playerId);
                    transitionLocations.remove(playerId);
                    this.cancel();
                    return;
                }
                
                if (remaining <= 0) {
                    // Complete duty activation
                    completeDutyActivation(player, guardRank);
                    dutyTransitions.remove(playerId);
                    transitionLocations.remove(playerId);
                    this.cancel();
                    return;
                }
                
                // Check if player moved
                Location storedLocation = transitionLocations.get(playerId);
                if (storedLocation != null && player.getLocation().distanceSquared(storedLocation) > 0.25) {
                    // Player moved, cancel transition
                    cancelDutyTransition(player, "duty.restrictions.movement-cancelled");
                    this.cancel();
                    return;
                }
                
                // Check if player is still in duty region
                if (!isInDutyRegion(player)) {
                    cancelDutyTransition(player, "duty.restrictions.left-region");
                    this.cancel();
                    return;
                }
                
                remaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
        
        dutyTransitions.put(playerId, task);
        return true;
    }
    
    public void cancelDutyTransition(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        
        // Cancel task
        BukkitTask task = dutyTransitions.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Remove location tracking
        transitionLocations.remove(playerId);
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(player, "duty");
        
        // Send cancellation message if reason provided
        if (reason != null) {
            plugin.getMessageManager().sendMessage(player, reason);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Duty transition cancelled for " + player.getName() + " - " + reason);
        }
    }
    
    private void completeDutyActivation(Player player, String guardRank) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        // Final validation before activation
        if (!isInDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.left-region");
            return;
        }
        
        // Store player's current inventory before clearing it
        if (!storePlayerInventory(player)) {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return;
        }
        
        // Clear player's inventory to prevent mixing with guard kit
        player.getInventory().clear();
        
        // Set on duty
        data.setOnDuty(true);
        data.setDutyStartTime(System.currentTimeMillis());
        data.setGuardRank(guardRank);
        
        // Reset session stats for new duty session
        data.resetSessionStats();
        
        // Save data
        plugin.getDataManager().savePlayerData(data);
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(player, "duty");
        
        // Send success message
        plugin.getMessageManager().sendMessage(player, "duty.activation.success");
        
        // Give appropriate kit
        giveGuardKit(player, guardRank);
        
        // Add guard tag
        plugin.getGuardTagManager().addGuardTag(player, data);
        
        // Send action bar confirmation
        plugin.getMessageManager().sendActionBar(player, "actionbar.duty-activated");
        
        logger.info(player.getName() + " went on duty as " + guardRank);
    }
    
    private void giveGuardKit(Player player, String guardRank) {
        String kitName = plugin.getConfigManager().getKitForRank(guardRank);
        
        if (kitName == null || kitName.trim().isEmpty()) {
            logger.warning("No kit configured for rank: " + guardRank);
            return;
        }
        
        try {
            // Use CMI integration instead of console commands
            plugin.getCMIIntegration().giveKit(player, kitName)
                .thenAccept(success -> {
                    if (success) {
                        plugin.getMessageManager().sendMessage(player, "duty.activation.kit-given", 
                            MessageManager.stringPlaceholder("kit", kitName));
                        
                        if (plugin.getConfigManager().isDebugMode()) {
                            logger.info("DEBUG: Successfully gave kit " + kitName + " to " + player.getName() + " via CMI integration");
                        }
                    } else {
                        logger.warning("CMI integration failed for kit " + kitName + " - falling back to console command");
                        
                        // Fallback to console command if CMI integration fails
                        try {
            String command = "cmi kit " + kitName + " " + player.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
                            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-given-fallback", 
                                MessageManager.stringPlaceholder("kit", kitName));
            
            if (plugin.getConfigManager().isDebugMode()) {
                                logger.info("DEBUG: Gave kit " + kitName + " to " + player.getName() + " via fallback console command");
                            }
                        } catch (Exception fallbackError) {
                            logger.severe("Both CMI integration and fallback failed for kit " + kitName + ": " + fallbackError.getMessage());
                            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                                MessageManager.stringPlaceholder("kit", kitName));
                        }
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("CMI kit integration error for " + kitName + ": " + throwable.getMessage());
                    plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                        MessageManager.stringPlaceholder("kit", kitName));
                    return null;
                });
                
        } catch (Exception e) {
            logger.warning("Failed to initiate kit giving for " + kitName + " to " + player.getName() + ": " + e.getMessage());
            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-failed", 
                MessageManager.stringPlaceholder("kit", kitName));
        }
    }
    
    public boolean goOffDuty(Player player, PlayerData data) {
        if (!data.isOnDuty()) {
            plugin.getMessageManager().sendMessage(player, "duty.deactivation.already-off");
            return false;
        }
        
        // Check if player is in a chase
        if (data.isBeingChased() || plugin.getDataManager().isGuardChasing(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.cannot-during-chase");
            return false;
        }
        
        // Check if player is in combat
        if (plugin.getChaseManager().isInCombat(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.combat-active");
            return false;
        }
        
        // NEW: Check if they have earned off-duty time
        if (!data.hasAvailableOffDutyTime()) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.no-earned-off-duty-time");
            return false;
        }
        
        // NEW: Check if player is in a valid region for going off duty
        if (!isInValidOffDutyRegion(player)) {
            plugin.getMessageManager().sendMessage(player, "duty.restrictions.wrong-region");
            return false;
        }
        
        // Calculate duty time
        long dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
        data.addDutyTime(dutyTime);
        
        // Set off duty
        data.setOnDuty(false);
        data.setOffDutyTime(System.currentTimeMillis());
        
        // Reset notification flag for this off-duty session
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        // NEW: Restore player's original inventory
        boolean inventoryRestored = restorePlayerInventory(player);
        
        // Save data
        plugin.getDataManager().savePlayerData(data);
        
        // Show available off-duty time
        long availableMinutes = data.getAvailableOffDutyTimeInMinutes();
        plugin.getMessageManager().sendMessage(player, "duty.deactivation.success-with-time",
            timePlaceholder("time", availableMinutes * 60L));
        
        // Provide feedback about inventory restoration
        if (inventoryRestored) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Successfully restored original inventory for " + player.getName() + " when going off duty");
            }
        } else {
            // This could happen if they were on duty before a server restart or if there was no stored inventory
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: No stored inventory found for " + player.getName() + " when going off duty");
            }
        }
        
        // Remove guard tag
        plugin.getGuardTagManager().removeGuardTag(player);
        
        // Send action bar confirmation
        plugin.getMessageManager().sendActionBar(player, "actionbar.duty-deactivated");
        
        // Notify duty banking system if enabled
        if (plugin.getConfigManager().isDutyBankingEnabled()) {
            plugin.getDutyBankingManager().handleDutyEnd(player, dutyTime / 1000L);
        }
        
        logger.info(player.getName() + " went off duty after " + (dutyTime / 1000) + " seconds (has " + availableMinutes + "m off-duty time)");
        
        return true;
    }
    
    /**
     * NEW: Award base off-duty time when guard meets minimum duty requirement
     */
    private void awardBaseOffDutyTime(Player player, PlayerData data) {
        int baseTimeMinutes = plugin.getConfigManager().getBaseOffDutyEarned();
        long baseTimeMillis = baseTimeMinutes * 60L * 1000L;
        
        data.addEarnedOffDutyTime(baseTimeMillis);
        data.setHasEarnedBaseTime(true);
        
        plugin.getDataManager().savePlayerData(data);
        
        // Update guard tag to reflect new earned time
        plugin.getGuardTagManager().updateGuardTag(player, data);
        
        plugin.getMessageManager().sendMessage(player, "duty.earning.base-time-earned",
            timePlaceholder("time", baseTimeMinutes * 60L));
        
        logger.info(player.getName() + " earned " + baseTimeMinutes + " minutes of off-duty time (base)");
    }
    
    /**
     * NEW: Check and award performance bonuses
     */
    private void checkPerformanceBonuses(Player player, PlayerData data) {
        // Check searches bonus
        int searchesPerBonus = plugin.getConfigManager().getSearchesPerBonus();
        if (data.getSessionSearches() >= searchesPerBonus) {
            int bonusTime = plugin.getConfigManager().getSearchBonusTime();
            awardPerformanceBonus(player, data, bonusTime * 60L * 1000L, "searches");
            data.setSessionSearches(data.getSessionSearches() - searchesPerBonus);
        }
        
        // Check kills bonus
        int killsPerBonus = plugin.getConfigManager().getKillsPerBonus();
        if (data.getSessionKills() >= killsPerBonus) {
            int bonusTime = plugin.getConfigManager().getKillBonusTime();
            awardPerformanceBonus(player, data, bonusTime * 60L * 1000L, "kills");
            data.setSessionKills(data.getSessionKills() - killsPerBonus);
        }
    }
    
    /**
     * NEW: Award time-based bonus for continuous duty
     */
    private void awardTimeBasedBonus(Player player, PlayerData data) {
        int bonusMinutes = plugin.getConfigManager().getDutyTimeBonusRate();
        long bonusMillis = bonusMinutes * 60L * 1000L;
        
        awardPerformanceBonus(player, data, bonusMillis, "continuous duty");
        
        // Update guard tag to reflect new earned time
        plugin.getGuardTagManager().updateGuardTag(player, data);
    }
    
    /**
     * NEW: Award performance bonus and notify player
     */
    private void awardPerformanceBonus(Player player, PlayerData data, long bonusMillis, String reason) {
        data.addEarnedOffDutyTime(bonusMillis);
        plugin.getDataManager().savePlayerData(data);
        
        // Update guard tag to reflect new earned time
        plugin.getGuardTagManager().updateGuardTag(player, data);
        
        long bonusSeconds = bonusMillis / 1000L;
        plugin.getMessageManager().sendMessage(player, "duty.earning.performance-bonus",
            timePlaceholder("time", bonusSeconds),
            stringPlaceholder("reason", reason));
        
        logger.info(player.getName() + " earned " + (bonusSeconds / 60L) + " minutes of off-duty time (" + reason + ")");
    }
    
    /**
     * NEW: Require guard to return to duty when they've used up their earned time
     */
    private void requireReturnToDuty(Player player, PlayerData data) {
        plugin.getMessageManager().sendMessage(player, "duty.restrictions.off-duty-time-expired");
        plugin.getMessageManager().sendMessage(player, "duty.restrictions.must-return-to-duty");
        
        // Optional: Add a grace period before forcing them back
        long gracePeriod = 5 * 60 * 1000L; // 5 minutes
        data.addEarnedOffDutyTime(gracePeriod);
        plugin.getDataManager().savePlayerData(data);
        
        logger.info(player.getName() + " has used up their earned off-duty time");
    }

    // === INVENTORY MANAGEMENT METHODS ===
    
    /**
     * Store player's current inventory in cache and database
     * @param player the player whose inventory to store
     * @return true if successful, false otherwise
     */
    private boolean storePlayerInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot store inventory for null or offline player");
            return false;
        }
        
        try {
            // Serialize player's inventory
            String inventoryData = InventorySerializer.serializePlayerInventory(player);
            
            if (inventoryData == null) {
                logger.warning("Failed to serialize inventory for " + player.getName());
                return false;
            }
            
            // Store in memory cache for quick access
            inventoryCache.put(player.getUniqueId(), inventoryData);
            
            // Store in database for persistence
            plugin.getDataManager().savePlayerInventory(player.getUniqueId(), inventoryData);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Stored inventory for " + player.getName());
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to store inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Restore player's original inventory from cache/database
     * @param player the player whose inventory to restore
     * @return true if successful, false otherwise
     */
    private boolean restorePlayerInventory(Player player) {
        if (player == null || !player.isOnline()) {
            logger.warning("Cannot restore inventory for null or offline player");
            return false;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            String inventoryData = null;
            
            // Try to get from memory cache first
            if (inventoryCache.containsKey(playerId)) {
                inventoryData = inventoryCache.get(playerId);
            } else {
                // Try to load from database
                inventoryData = plugin.getDataManager().loadPlayerInventory(playerId);
            }
            
            if (inventoryData == null) {
                logger.warning("No cached inventory found for " + player.getName());
                return false;
            }
            
            // Remove any guard kit items first
            int removedItems = InventorySerializer.removeGuardKitItems(player, guardKitItems);
            
            // Restore original inventory
            boolean success = InventorySerializer.deserializePlayerInventory(player, inventoryData);
            
            if (success) {
                // Clean up cache and database
                inventoryCache.remove(playerId);
                plugin.getDataManager().deletePlayerInventory(playerId);
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Restored inventory for " + player.getName() + " (removed " + removedItems + " guard items)");
                }
            }
            
            return success;
        } catch (Exception e) {
            logger.severe("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if player is in a valid region for going off duty
     * @param player the player to check
     * @return true if in valid region, false otherwise
     */
    private boolean isInValidOffDutyRegion(Player player) {
        // Check if player is in duty region (guards can go off duty in the same region they go on duty)
        if (isInDutyRegion(player)) {
            return true;
        }
        
        // Check if player is in any duty-required zones (these are also valid for going off duty)
        String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, dutyRequiredZones);
    }
    
    /**
     * Clean up old stored inventory data to prevent database bloat
     * This should be called periodically (e.g., daily)
     */
    public void cleanupOldStoredInventories() {
        try {
            // Clean up stored inventories older than 7 days for players who are no longer on duty
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7 days
            
            // Get all stored inventory entries
            List<UUID> storedInventoryPlayers = plugin.getDataManager().getPlayersWithStoredInventory();
            
            int cleanedCount = 0;
            for (UUID playerId : storedInventoryPlayers) {
                PlayerData data = plugin.getDataManager().getPlayerData(playerId);
                
                // Clean up if player is not on duty and inventory is old
                if (data != null && !data.isOnDuty()) {
                    String inventoryData = plugin.getDataManager().loadPlayerInventory(playerId);
                    if (inventoryData != null) {
                        // Check if inventory data contains timestamp
                        try {
                            JsonObject inventoryObj = JsonParser.parseString(inventoryData).getAsJsonObject();
                            if (inventoryObj.has("metadata")) {
                                JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
                                if (metadata.has("timestamp")) {
                                    long timestamp = metadata.get("timestamp").getAsLong();
                                    if (timestamp < cutoffTime) {
                                        plugin.getDataManager().deletePlayerInventory(playerId);
                                        cleanedCount++;
                                        
                                        if (plugin.getConfigManager().isDebugMode()) {
                                            logger.info("DEBUG: Cleaned up old stored inventory for " + playerId);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // If we can't parse the data, it's corrupted - clean it up
                            plugin.getDataManager().deletePlayerInventory(playerId);
                            cleanedCount++;
                        }
                    }
                }
            }
            
            if (cleanedCount > 0) {
                logger.info("Cleaned up " + cleanedCount + " old stored inventories");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to cleanup old stored inventories: " + e.getMessage());
        }
    }
    
    /**
     * Check if player has stored inventory that needs to be restored when going off duty
     * @param player the player to check
     * @return true if they have stored inventory, false otherwise
     */
    public boolean hasStoredInventoryForRestoration(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Check memory cache first
        if (inventoryCache.containsKey(playerId)) {
            return true;
        }
        
        // Check database
        return plugin.getDataManager().hasStoredInventory(playerId);
    }
    
    /**
     * Public method to restore player inventory (for use by event handlers)
     * @param player the player to restore inventory for
     * @return true if successful, false otherwise
     */
    public boolean restorePlayerInventoryPublic(Player player) {
        return restorePlayerInventory(player);
    }
    
    /**
     * Public method to give guard kit to a player (for use by event handlers)
     * @param player the player to give kit to
     * @param guardRank the guard rank to give kit for
     */
    public void giveGuardKitPublic(Player player, String guardRank) {
        giveGuardKit(player, guardRank);
    }
    
    /**
     * Check if player has any guard kit items in their inventory
     * @param player the player to check
     * @return true if they have guard kit items, false otherwise
     */
    public boolean hasGuardKitItems(Player player) {
        return InventorySerializer.hasGuardKitItems(player, guardKitItems);
    }
    
    /**
     * Remove all guard kit items from player's inventory
     * @param player the player to remove items from
     * @return number of items removed
     */
    public int removeGuardKitItems(Player player) {
        return InventorySerializer.removeGuardKitItems(player, guardKitItems);
    }
    
    /**
     * Get the list of guard kit items
     * @return list of materials considered guard kit items
     */
    public List<Material> getGuardKitItems() {
        return guardKitItems;
    }

    // === INTEGRATION METHODS ===
    
    public String getPlayerGuardRank(Player player) {
        // CRITICAL: Only check LuckPerms - ignore OP status and basic permissions
        if (luckPerms != null) {
            String rank = detectRankFromLuckPerms(player);
            if (rank != null) {
                return rank;
            }
        }
        
        // Fallback to permission-based detection ONLY if LuckPerms is not available
        if (luckPerms == null) {
            return detectRankFromPermissions(player);
        }
        
        // If LuckPerms is available but no rank found, deny access
        return null;
    }
    
    private String detectRankFromLuckPerms(Player player) {
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return null;
            }
            
                    Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
        
        // Check each configured rank mapping in order of priority (highest first)
        // Define rank priority order - this can be made configurable in the future
        String[] rankOrder = {"warden", "captain", "sergeant", "officer", "private", "trainee"};
        
        for (String rankKey : rankOrder) {
                String groupName = rankMappings.get(rankKey);
                if (groupName != null) {
                    // Check if user has this group
                    boolean hasGroup = user.getInheritedGroups(user.getQueryOptions()).stream()
                            .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
                    
                    if (hasGroup) {
                        return rankKey;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.warning("Error detecting rank from LuckPerms for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private String detectRankFromPermissions(Player player) {
        // Check permissions in order of hierarchy (highest first)
        if (player.hasPermission("edencorrections.guard.warden")) return "warden";
        if (player.hasPermission("edencorrections.guard.captain")) return "captain";
        if (player.hasPermission("edencorrections.guard.sergeant")) return "sergeant";
        if (player.hasPermission("edencorrections.guard.officer")) return "officer";
        if (player.hasPermission("edencorrections.guard.private")) return "private";
        if (player.hasPermission("edencorrections.guard.trainee")) return "trainee";
        if (player.hasPermission("edencorrections.guard")) return "trainee"; // fallback to lowest rank
        
        return null;
    }
    
    private boolean isInDutyRegion(Player player) {
        return plugin.getWorldGuardUtils().isPlayerInDutyRegion(player);
    }
    
    public boolean isInRegion(Player player, String regionName) {
        return plugin.getWorldGuardUtils().isPlayerInRegion(player, regionName);
    }
    
    public boolean isInAnyRegion(Player player, String[] regionNames) {
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, regionNames);
    }

    // === UTILITY METHODS ===
    
    public boolean canGoOnDuty(PlayerData data) {
        if (data.isOnDuty()) return false;
        
        // Allow new players or those who haven't gone off duty yet to go on duty immediately
        if (data.getOffDutyTime() == 0) {
            return true;
        }
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return offDutyTime >= requiredOffDutyTime;
    }
    
    /**
     * Check if a player can go on duty, with optional bypass for admins
     */
    public boolean canGoOnDuty(PlayerData data, Player player) {
        if (data.isOnDuty()) return false;
        
        // Allow new players or those who haven't gone off duty yet to go on duty immediately
        if (data.getOffDutyTime() == 0) {
            return true;
        }
        
        // Check if player has bypass permission
        if (player.hasPermission("edencorrections.admin.bypass.cooldown")) {
            return true;
        }
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return offDutyTime >= requiredOffDutyTime;
    }
    
    public long getRemainingOffDutyTime(PlayerData data) {
        if (data.getOffDutyTime() == 0) return 0;
        
        long offDutyTime = System.currentTimeMillis() - data.getOffDutyTime();
        long requiredOffDutyTime = getRequiredOffDutyTime();
        
        return Math.max(0, requiredOffDutyTime - offDutyTime) / 1000L; // Return in seconds
    }
    
    public boolean hasGuardPermission(Player player) {
        // For basic guard permission check (used in monitoring tasks), use standard permission
        // This avoids expensive LuckPerms calls every second
        return player.hasPermission("edencorrections.guard");
    }
    
    public boolean hasValidGuardRank(Player player) {
        // For rank validation, use strict LuckPerms check
        return getPlayerGuardRank(player) != null;
    }
    
    public boolean isOnDuty(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data != null && data.isOnDuty();
    }
    
    public boolean isInDutyTransition(Player player) {
        return dutyTransitions.containsKey(player.getUniqueId());
    }
    
    public long getRequiredOffDutyTime() {
        return plugin.getConfigManager().getBaseDutyRequirement() * 60L * 1000L; // Convert minutes to milliseconds
    }
    
    public long getMaxOffDutyTime() {
        return getRequiredOffDutyTime() * 3; // 3x the required time
    }
    
    public long getGracePenalty() {
        return 300000L; // 5 minutes in milliseconds
    }
    
    // === NEW: PERFORMANCE TRACKING METHODS ===
    
    /**
     * Award points for performing a contraband search
     */
    public void awardSearchPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionSearches();
            plugin.getDataManager().savePlayerData(data);
            
            // Update guard tag to reflect new stats
            plugin.getGuardTagManager().updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " performed search (total: " + data.getSessionSearches() + ")");
            }
        }
    }
    
    /**
     * Award points for successful contraband search
     */
    public void awardSuccessfulSearchPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionSuccessfulSearches();
            
            // Award immediate bonus
            int bonusMinutes = plugin.getConfigManager().getSuccessfulSearchBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful search");
            
            // Update guard tag to reflect new stats
            plugin.getGuardTagManager().updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful search (total: " + data.getSessionSuccessfulSearches() + ")");
            }
        }
    }
    
    /**
     * Award points for successful arrest
     */
    public void awardArrestPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionArrests();
            
            // Award immediate bonus
            int bonusMinutes = plugin.getConfigManager().getSuccessfulArrestBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful arrest");
            
            // Update guard tag to reflect new stats
            plugin.getGuardTagManager().updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful arrest (total: " + data.getSessionArrests() + ")");
            }
        }
    }
    
    /**
     * Award points for killing a player (with rank scaling)
     */
    public void awardKillPerformance(Player guard, Player victim) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionKills();
            plugin.getDataManager().savePlayerData(data);
            
            // Update guard tag to reflect new stats
            plugin.getGuardTagManager().updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " killed " + victim.getName() + " (total: " + data.getSessionKills() + ")");
            }
        }
    }
    
    /**
     * Award points for successful detection (drug test, metal detect, etc.)
     */
    public void awardDetectionPerformance(Player guard) {
        if (!isOnDuty(guard)) return;
        
        PlayerData data = plugin.getDataManager().getPlayerData(guard.getUniqueId());
        if (data != null) {
            data.incrementSessionDetections();
            
            // Award immediate bonus
            int bonusMinutes = plugin.getConfigManager().getSuccessfulDetectionBonus();
            long bonusMillis = bonusMinutes * 60L * 1000L;
            awardPerformanceBonus(guard, data, bonusMillis, "successful detection");
            
            // Update guard tag to reflect new stats
            plugin.getGuardTagManager().updateGuardTag(guard, data);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: " + guard.getName() + " successful detection (total: " + data.getSessionDetections() + ")");
            }
        }
    }
    
    // === CLEANUP METHODS ===
    
    public void cleanup() {
        // Cancel all duty transitions
        for (Map.Entry<UUID, BukkitTask> entry : dutyTransitions.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }
            
            // Clean up UI for player
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                plugin.getBossBarManager().hideBossBarByType(player, "duty");
            }
        }
        
        // Clean up inventory cache and restore inventories for online players
        for (Map.Entry<UUID, String> entry : inventoryCache.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                restorePlayerInventory(player);
            }
        }
        
        // Clean up all guard tags
        plugin.getGuardTagManager().cleanupAllGuardTags();
        
        dutyTransitions.clear();
        transitionLocations.clear();
        inventoryCache.clear();
    }
    
    public void cleanupPlayer(Player player) {
        // Cancel any active duty transition
        cancelDutyTransition(player, null);
        
        // Remove guard tag if player is on duty
        if (isOnDuty(player)) {
            plugin.getGuardTagManager().removeGuardTag(player);
            // Do NOT restore inventory if on duty (preserve guard kit)
            // Do NOT delete stored inventory - keep it for when they go off duty later
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Preserved stored inventory for " + player.getName() + " (logged off while on duty)");
            }
        } else {
            // Clean up inventory cache for off-duty players
            UUID playerId = player.getUniqueId();
            if (inventoryCache.containsKey(playerId)) {
                // Just remove from cache
                inventoryCache.remove(playerId);
                plugin.getDataManager().deletePlayerInventory(playerId);
                // Restore inventory if off duty
                restorePlayerInventory(player);
            }
        }
    }
} 