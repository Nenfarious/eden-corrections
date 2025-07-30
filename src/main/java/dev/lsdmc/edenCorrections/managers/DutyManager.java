package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.InventorySerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

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
    
    /**
     * NEW: Check if off-duty guards have consumed their earned time
     */
    private void checkOffDutyTimeConsumption(Player player, PlayerData data) {
        if (data.isOnDuty()) return;
        
        long timeSinceOffDuty = System.currentTimeMillis() - data.getOffDutyTime();
        long earnedTime = data.getEarnedOffDutyTime();
        
        if (timeSinceOffDuty > earnedTime) {
            // They've used up their earned off-duty time
            // Only notify once to prevent spam
            if (!data.hasBeenNotifiedOfExpiredTime()) {
                requireReturnToDuty(player, data);
                data.setHasBeenNotifiedOfExpiredTime(true);
                plugin.getDataManager().savePlayerData(data);
            }
        }
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
        if (!canGoOnDuty(data)) {
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
        
        // NEW: Store player's current inventory before giving guard kit
        if (!storePlayerInventory(player)) {
            plugin.getMessageManager().sendMessage(player, "universal.failed");
            return;
        }
        
        // Set on duty
        data.setOnDuty(true);
        data.setDutyStartTime(System.currentTimeMillis());
        data.setGuardRank(guardRank);
        
        // Reset session stats for new duty session
        data.resetSessionStats();
        
        // Save data
        plugin.getDataManager().savePlayerData(data);
        
        // Start time tracking
        plugin.getTimeSyncManager().startTimeTracking(player, "duty");
        
        // Set guard tag if available
        if (plugin.getLuckPermsMetaManager() != null) {
            plugin.getLuckPermsMetaManager().setGuardTag(player, data);
        }
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(player, "duty");
        
        // Send success message
        plugin.getMessageManager().sendMessage(player, "duty.activation.success");
        
        // Give appropriate kit
        giveGuardKit(player, guardRank);
        
        // Send action bar confirmation
        plugin.getMessageManager().sendActionBar(player, "actionbar.duty-activated");
        
        logger.info(player.getName() + " went on duty as " + guardRank);
    }
    
    private void giveGuardKit(Player player, String guardRank) {
        String kitName = plugin.getConfigManager().getKitForRank(guardRank);
        
        try {
            // Execute CMI kit command
            String command = "cmi kit " + kitName + " " + player.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            plugin.getMessageManager().sendMessage(player, "duty.activation.kit-given", 
                stringPlaceholder("kit", kitName));
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Gave kit " + kitName + " to " + player.getName());
            }
        } catch (Exception e) {
            logger.warning("Failed to give kit " + kitName + " to " + player.getName() + ": " + e.getMessage());
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
        
        // Stop time tracking and get accurate duration
        long dutyTime = plugin.getTimeSyncManager().stopTimeTracking(player, true).join();
        if (dutyTime == 0) {
            // Fallback to manual calculation if time sync failed
            dutyTime = System.currentTimeMillis() - data.getDutyStartTime();
            data.addDutyTime(dutyTime / 1000L);
        }
        
        // Set off duty
        data.setOnDuty(false);
        data.setOffDutyTime(System.currentTimeMillis());
        
        // Reset notification flag for this off-duty session
        data.setHasBeenNotifiedOfExpiredTime(false);
        
        // Remove guard tag if available
        if (plugin.getLuckPermsMetaManager() != null) {
            plugin.getLuckPermsMetaManager().removeGuardTag(player);
        }
        
        // NEW: Restore player's original inventory
        restorePlayerInventory(player);
        
        // Save data
        plugin.getDataManager().savePlayerData(data);
        
        // Show available off-duty time
        long availableMinutes = data.getAvailableOffDutyTimeInMinutes();
        plugin.getMessageManager().sendMessage(player, "duty.deactivation.success-with-time",
            timePlaceholder("time", availableMinutes * 60L));
        
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
    }
    
    /**
     * NEW: Award performance bonus and notify player
     */
    private void awardPerformanceBonus(Player player, PlayerData data, long bonusMillis, String reason) {
        data.addEarnedOffDutyTime(bonusMillis);
        plugin.getDataManager().savePlayerData(data);
        
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
            return false;
        }
    }
    
    /**
     * Restore player's original inventory from cache/database
     * @param player the player whose inventory to restore
     * @return true if successful, false otherwise
     */
    private boolean restorePlayerInventory(Player player) {
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
        return plugin.getConfigManager().getDutyTransitionTime() * 1000L;
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
        
        dutyTransitions.clear();
        transitionLocations.clear();
        inventoryCache.clear();
    }
    
    public void cleanupPlayer(Player player) {
        // Cancel any active duty transition
        cancelDutyTransition(player, null);
        
        // Clean up inventory cache
        UUID playerId = player.getUniqueId();
        if (inventoryCache.containsKey(playerId)) {
            // Try to restore inventory if player is going off duty
            if (isOnDuty(player)) {
                restorePlayerInventory(player);
            } else {
                // Just remove from cache
                inventoryCache.remove(playerId);
                plugin.getDataManager().deletePlayerInventory(playerId);
            }
        }
    }
} 