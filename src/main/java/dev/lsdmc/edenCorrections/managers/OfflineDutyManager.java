package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.utils.InventorySerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Offline Duty Management System
 * 
 * Handles duty state transitions when players go offline/online,
 * manages inventory restoration, and ensures proper time tracking
 * without continuous calculation while offline.
 */
public class OfflineDutyManager implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Offline duty tracking
    private final Map<UUID, OfflineDutyState> offlineDutyStates;
    private final Map<UUID, String> storedInventories;
    private final Map<UUID, Long> logoffTimes;
    
    // Delayed processing
    private final Map<UUID, BukkitTask> delayedOffDutyTasks;
    
    // Configuration
    private long offDutyProcessingDelay;
    private boolean autoRestoreInventory;
    private boolean preserveOffDutyTime;
    private long maxOfflineTime;
    
    public OfflineDutyManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.offlineDutyStates = new ConcurrentHashMap<>();
        this.storedInventories = new ConcurrentHashMap<>();
        this.logoffTimes = new ConcurrentHashMap<>();
        this.delayedOffDutyTasks = new HashMap<>();
    }
    
    public void initialize() {
        loadConfiguration();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadOfflineDutyStates();
        logger.info("OfflineDutyManager initialized - Auto-restore inventory: " + autoRestoreInventory);
    }
    
    private void loadConfiguration() {
        offDutyProcessingDelay = plugin.getConfigManager().getConfig().getLong("offline-duty.processing-delay-seconds", 5) * 1000L;
        autoRestoreInventory = plugin.getConfigManager().getConfig().getBoolean("offline-duty.auto-restore-inventory", true);
        preserveOffDutyTime = plugin.getConfigManager().getConfig().getBoolean("offline-duty.preserve-off-duty-time", true);
        maxOfflineTime = plugin.getConfigManager().getConfig().getLong("offline-duty.max-offline-hours", 24) * 3600000L;
    }
    
    private void loadOfflineDutyStates() {
        // Load any existing offline duty states from database/config
        // This handles server restarts where players were on duty
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check for players who were on duty when server shut down
                plugin.getDataManager().loadAllPlayerData().thenAccept(allPlayerData -> {
                    for (PlayerData data : allPlayerData) {
                        if (data.isOnDuty()) {
                            // Player was on duty when server shut down
                            UUID playerId = data.getPlayerId();
                            OfflineDutyState state = new OfflineDutyState(
                                playerId, 
                                data.getDutyStartTime(), 
                                System.currentTimeMillis(),
                                data.getGuardRank()
                            );
                            offlineDutyStates.put(playerId, state);
                            
                            logger.info("Restored offline duty state for player: " + data.getPlayerName());
                        }
                    }
                });
            } catch (Exception e) {
                logger.severe("Error loading offline duty states: " + e.getMessage());
            }
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        handlePlayerLogoff(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        handlePlayerLogin(player);
    }
    
    /**
     * Handle player logging off - pause duty and store state
     */
    private void handlePlayerLogoff(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        
        if (data == null) return;
        
        try {
            // Record logoff time
            logoffTimes.put(playerId, System.currentTimeMillis());
            
            if (data.isOnDuty()) {
                // Player is on duty - handle transition
                handleOnDutyLogoff(player, data);
            } else {
                // Player is off duty - just clean up
                handleOffDutyLogoff(player, data);
            }
            
            // Pause time tracking
            plugin.getTimeSyncManager().pauseTimeTracking(player);
            
            logger.fine("Handled logoff for " + player.getName() + " - On duty: " + data.isOnDuty());
            
        } catch (Exception e) {
            logger.severe("Error handling logoff for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private void handleOnDutyLogoff(Player player, PlayerData data) {
        UUID playerId = player.getUniqueId();
        
        // Calculate current duty session time
        long sessionTime = System.currentTimeMillis() - data.getDutyStartTime();
        
        // Store current duty state
        OfflineDutyState state = new OfflineDutyState(
            playerId,
            data.getDutyStartTime(),
            System.currentTimeMillis(),
            data.getGuardRank()
        );
        state.setSessionTime(sessionTime);
        offlineDutyStates.put(playerId, state);
        
        // Store guard inventory if they have one
        if (plugin.getDutyManager().hasGuardKitItems(player)) {
            String inventoryData = InventorySerializer.serializePlayerInventory(player);
            if (inventoryData != null) {
                storedInventories.put(playerId, inventoryData);
                plugin.getDataManager().savePlayerInventory(playerId, inventoryData);
            }
        }
        
        // Schedule delayed off-duty processing
        scheduleDelayedOffDutyProcessing(player, data, sessionTime);
    }
    
    private void handleOffDutyLogoff(Player player, PlayerData data) {
        // Just clean up any existing stored inventories
        cleanupStoredInventory(player.getUniqueId());
    }
    
    private void scheduleDelayedOffDutyProcessing(Player player, PlayerData data, long sessionTime) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing task
        BukkitTask existingTask = delayedOffDutyTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Schedule the off-duty processing
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Only process if player is still offline
            if (!plugin.getServer().getPlayer(playerId).isOnline()) {
                processOfflineOffDuty(playerId, data, sessionTime);
            }
            delayedOffDutyTasks.remove(playerId);
        }, offDutyProcessingDelay / 50L); // Convert to ticks
        
        delayedOffDutyTasks.put(playerId, task);
    }
    
    private void processOfflineOffDuty(UUID playerId, PlayerData data, long sessionTime) {
        try {
            // Update duty time
            data.addDutyTime(sessionTime / 1000L); // Convert to seconds
            
            // Process off-duty earning (same logic as normal off-duty)
            long dutyMinutes = sessionTime / (1000L * 60L);
            if (dutyMinutes >= plugin.getConfigManager().getBaseDutyRequirement() && !data.hasEarnedBaseTime()) {
                // Award base off-duty time
                long baseOffDutyTime = plugin.getConfigManager().getBaseOffDutyEarned() * 60L * 1000L; // Convert to milliseconds
                data.addEarnedOffDutyTime(baseOffDutyTime);
                data.setHasEarnedBaseTime(true);
            }
            
            // Set player as off duty
            data.setOnDuty(false);
            data.setOffDutyTime(System.currentTimeMillis());
            
            // Save the updated data
            plugin.getDataManager().savePlayerData(data);
            
            logger.info("Processed offline off-duty for player " + playerId + " - Session time: " + (sessionTime / 1000) + "s");
            
        } catch (Exception e) {
            logger.severe("Error processing offline off-duty for " + playerId + ": " + e.getMessage());
        }
    }
    
    /**
     * Handle player logging in - restore state and inventory
     */
    private void handlePlayerLogin(Player player) {
        UUID playerId = player.getUniqueId();
        
        try {
            // Resume time tracking if needed
            plugin.getTimeSyncManager().resumeTimeTracking(player);
            
            // Check for offline duty state
            OfflineDutyState offlineState = offlineDutyStates.remove(playerId);
            Long logoffTime = logoffTimes.remove(playerId);
            
            if (offlineState != null) {
                handleOfflineDutyReturn(player, offlineState, logoffTime);
            } else {
                handleNormalLogin(player);
            }
            
            // Cancel any pending delayed tasks
            BukkitTask delayedTask = delayedOffDutyTasks.remove(playerId);
            if (delayedTask != null) {
                delayedTask.cancel();
            }
            
            logger.fine("Handled login for " + player.getName());
            
        } catch (Exception e) {
            logger.severe("Error handling login for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private void handleOfflineDutyReturn(Player player, OfflineDutyState offlineState, Long logoffTime) {
        UUID playerId = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        
        if (data == null) return;
        
        long offlineTime = logoffTime != null ? System.currentTimeMillis() - logoffTime : 0L;
        
        // Check if offline time was too long
        if (offlineTime > maxOfflineTime) {
            // Force off duty due to extended absence
            forceOffDutyDueToAbsence(player, data, offlineState, offlineTime);
            return;
        }
        
        // Check current duty status
        if (data.isOnDuty()) {
            // Player is still marked as on duty - they were processed while offline
            handleStillOnDutyReturn(player, data, offlineState);
        } else {
            // Player was processed off duty while offline
            handleOffDutyReturn(player, data, offlineState);
        }
    }
    
    private void handleStillOnDutyReturn(Player player, PlayerData data, OfflineDutyState offlineState) {
        // Restore guard inventory if stored
        restoreStoredInventory(player);
        
        // Update duty start time to account for offline period
        long adjustedStartTime = System.currentTimeMillis() - offlineState.getSessionTime();
        data.setDutyStartTime(adjustedStartTime);
        
        // Notify player
        plugin.getMessageManager().sendMessage(player, "offline-duty.resumed-on-duty",
            plugin.getMessageManager().timePlaceholder("offline_time", 
                (System.currentTimeMillis() - offlineState.getLogoffTime()) / 1000L));
    }
    
    private void handleOffDutyReturn(Player player, PlayerData data, OfflineDutyState offlineState) {
        // Restore original inventory
        restoreOriginalInventory(player);
        
        // Notify player they went off duty while offline
        plugin.getMessageManager().sendMessage(player, "offline-duty.went-off-duty-offline",
            plugin.getMessageManager().timePlaceholder("session_time", offlineState.getSessionTime() / 1000L));
        
        // Show earned off-duty time if any
        if (data.getEarnedOffDutyTime() > 0) {
            plugin.getMessageManager().sendMessage(player, "offline-duty.earned-time-offline",
                plugin.getMessageManager().timePlaceholder("earned_time", data.getEarnedOffDutyTime() / 1000L));
        }
    }
    
    private void forceOffDutyDueToAbsence(Player player, PlayerData data, OfflineDutyState offlineState, long offlineTime) {
        // Force off duty
        data.setOnDuty(false);
        data.setOffDutyTime(System.currentTimeMillis());
        
        // Add session time to total
        data.addDutyTime(offlineState.getSessionTime() / 1000L);
        
        // Restore original inventory
        restoreOriginalInventory(player);
        
        // Reset earned off-duty time due to extended absence
        if (!preserveOffDutyTime) {
            data.setEarnedOffDutyTime(0L);
        }
        
        // Save data
        plugin.getDataManager().savePlayerData(data);
        
        // Notify player
        plugin.getMessageManager().sendMessage(player, "offline-duty.forced-off-duty-absence",
            plugin.getMessageManager().timePlaceholder("offline_time", offlineTime / 1000L),
            plugin.getMessageManager().timePlaceholder("max_time", maxOfflineTime / 1000L));
    }
    
    private void handleNormalLogin(Player player) {
        // Check if player has stored inventory that needs restoration
        String storedInventory = storedInventories.remove(player.getUniqueId());
        if (storedInventory != null) {
            // Restore the inventory
            if (autoRestoreInventory) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    InventorySerializer.deserializePlayerInventory(player, storedInventory);
                    plugin.getMessageManager().sendMessage(player, "offline-duty.inventory-restored");
                }, 20L); // 1 second delay
            }
        }
    }
    
    private void restoreStoredInventory(Player player) {
        String storedInventory = storedInventories.remove(player.getUniqueId());
        if (storedInventory != null && autoRestoreInventory) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                InventorySerializer.deserializePlayerInventory(player, storedInventory);
            }, 10L); // 0.5 second delay
        }
    }
    
    private void restoreOriginalInventory(Player player) {
        // Load and restore the player's original inventory from database
        plugin.getDataManager().loadPlayerInventory(player.getUniqueId()).thenAccept(inventoryData -> {
            if (inventoryData != null && autoRestoreInventory) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    InventorySerializer.deserializePlayerInventory(player, inventoryData);
                    plugin.getDataManager().deletePlayerInventory(player.getUniqueId());
                }, 10L); // 0.5 second delay
            }
        });
    }
    
    private void cleanupStoredInventory(UUID playerId) {
        storedInventories.remove(playerId);
        plugin.getDataManager().deletePlayerInventory(playerId);
    }
    
    /**
     * Check if player has an offline duty state
     */
    public boolean hasOfflineDutyState(Player player) {
        return offlineDutyStates.containsKey(player.getUniqueId());
    }
    
    /**
     * Get offline duty state for a player
     */
    public OfflineDutyState getOfflineDutyState(Player player) {
        return offlineDutyStates.get(player.getUniqueId());
    }
    
    /**
     * Manually process offline duty for a player (admin command)
     */
    public CompletableFuture<Boolean> manuallyProcessOfflineDuty(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflineDutyState state = offlineDutyStates.get(playerId);
                if (state == null) {
                    return false;
                }
                
                PlayerData data = plugin.getDataManager().getPlayerData(playerId).join();
                if (data == null) {
                    return false;
                }
                
                processOfflineOffDuty(playerId, data, state.getSessionTime());
                offlineDutyStates.remove(playerId);
                
                return true;
            } catch (Exception e) {
                logger.severe("Error manually processing offline duty for " + playerId + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Get offline duty statistics
     */
    public Map<String, Object> getOfflineDutyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("offlineDutyStates", offlineDutyStates.size());
        stats.put("storedInventories", storedInventories.size());
        stats.put("delayedTasks", delayedOffDutyTasks.size());
        stats.put("autoRestoreInventory", autoRestoreInventory);
        stats.put("preserveOffDutyTime", preserveOffDutyTime);
        return stats;
    }
    
    public void cleanup() {
        // Cancel all delayed tasks
        delayedOffDutyTasks.values().forEach(BukkitTask::cancel);
        delayedOffDutyTasks.clear();
        
        // Clear all tracking data
        offlineDutyStates.clear();
        storedInventories.clear();
        logoffTimes.clear();
    }
    
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        offlineDutyStates.remove(playerId);
        storedInventories.remove(playerId);
        logoffTimes.remove(playerId);
        
        BukkitTask task = delayedOffDutyTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    // === INNER CLASSES ===
    
    public static class OfflineDutyState {
        private final UUID playerId;
        private final long dutyStartTime;
        private final long logoffTime;
        private final String guardRank;
        private long sessionTime;
        
        public OfflineDutyState(UUID playerId, long dutyStartTime, long logoffTime, String guardRank) {
            this.playerId = playerId;
            this.dutyStartTime = dutyStartTime;
            this.logoffTime = logoffTime;
            this.guardRank = guardRank;
            this.sessionTime = 0L;
        }
        
        public UUID getPlayerId() { return playerId; }
        public long getDutyStartTime() { return dutyStartTime; }
        public long getLogoffTime() { return logoffTime; }
        public String getGuardRank() { return guardRank; }
        public long getSessionTime() { return sessionTime; }
        
        public void setSessionTime(long sessionTime) { this.sessionTime = sessionTime; }
        
        public long getOfflineTime() {
            return System.currentTimeMillis() - logoffTime;
        }
    }
}