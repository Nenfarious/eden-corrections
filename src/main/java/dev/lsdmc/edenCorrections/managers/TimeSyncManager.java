package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Robust Time Synchronization Manager
 * 
 * Handles accurate duty time calculations, sync operations, and time validation
 * to ensure all time-based systems remain consistent and accurate.
 */
public class TimeSyncManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Time tracking
    private final Map<UUID, TimeSession> activeSessions;
    private final Map<UUID, Long> pausedSessions;
    private final Map<UUID, TimeSnapshot> lastKnownStates;
    
    // Sync operations
    private final Map<UUID, BukkitTask> syncTasks;
    private BukkitTask globalSyncTask;
    
    // Configuration
    private long syncInterval;
    private long maxTimeDiscrepancy;
    private boolean autoSyncEnabled;
    private boolean strictTimeValidation;
    
    public TimeSyncManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeSessions = new ConcurrentHashMap<>();
        this.pausedSessions = new ConcurrentHashMap<>();
        this.lastKnownStates = new ConcurrentHashMap<>();
        this.syncTasks = new HashMap<>();
    }
    
    public void initialize() {
        loadConfiguration();
        startGlobalSyncMonitoring();
        logger.info("TimeSyncManager initialized - Auto-sync: " + autoSyncEnabled + 
                   ", Sync interval: " + (syncInterval / 1000) + "s");
    }
    
    private void loadConfiguration() {
        syncInterval = plugin.getConfigManager().getConfig().getLong("time-sync.interval-seconds", 30) * 1000L;
        maxTimeDiscrepancy = plugin.getConfigManager().getConfig().getLong("time-sync.max-discrepancy-seconds", 60) * 1000L;
        autoSyncEnabled = plugin.getConfigManager().getConfig().getBoolean("time-sync.auto-sync-enabled", true);
        strictTimeValidation = plugin.getConfigManager().getConfig().getBoolean("time-sync.strict-validation", true);
    }
    
    private void startGlobalSyncMonitoring() {
        if (!autoSyncEnabled) return;
        
        globalSyncTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            performGlobalTimeSync();
        }, 20L * (syncInterval / 1000), 20L * (syncInterval / 1000));
    }
    
    /**
     * Start tracking time for a player's duty session
     */
    public CompletableFuture<Boolean> startTimeTracking(Player player, String sessionType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                
                // Create new time session
                TimeSession session = new TimeSession(playerId, sessionType, currentTime);
                activeSessions.put(playerId, session);
                
                // Create snapshot
                PlayerData data = plugin.getDataManager().getPlayerData(playerId);
                if (data != null) {
                    TimeSnapshot snapshot = new TimeSnapshot(data, currentTime);
                    lastKnownStates.put(playerId, snapshot);
                }
                
                // Start individual sync task if needed
                if (autoSyncEnabled) {
                    startIndividualSyncTask(player);
                }
                
                logger.fine("Started time tracking for " + player.getName() + " - " + sessionType);
                return true;
            } catch (Exception e) {
                logger.severe("Error starting time tracking for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Stop tracking time for a player's session
     */
    public CompletableFuture<Long> stopTimeTracking(Player player, boolean saveToDatabase) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                TimeSession session = activeSessions.remove(playerId);
                
                if (session == null) {
                    logger.warning("No active time session found for " + player.getName());
                    return 0L;
                }
                
                // Calculate accurate duration
                long duration = calculateAccurateDuration(session);
                
                // Stop individual sync task
                stopIndividualSyncTask(playerId);
                
                // Save to database if requested
                if (saveToDatabase) {
                    updatePlayerDataWithAccurateTime(player, duration);
                }
                
                logger.fine("Stopped time tracking for " + player.getName() + " - Duration: " + (duration / 1000) + "s");
                return duration;
            } catch (Exception e) {
                logger.severe("Error stopping time tracking for " + player.getName() + ": " + e.getMessage());
                return 0L;
            }
        });
    }
    
    /**
     * Pause time tracking (for when player goes offline)
     */
    public CompletableFuture<Boolean> pauseTimeTracking(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                TimeSession session = activeSessions.get(playerId);
                
                if (session == null) {
                    return false;
                }
                
                // Calculate elapsed time and store it
                long elapsedTime = System.currentTimeMillis() - session.startTime;
                pausedSessions.put(playerId, elapsedTime);
                
                // Keep the session but mark it as paused
                session.pauseTime = System.currentTimeMillis();
                session.isPaused = true;
                
                logger.fine("Paused time tracking for " + player.getName() + " - Elapsed: " + (elapsedTime / 1000) + "s");
                return true;
            } catch (Exception e) {
                logger.severe("Error pausing time tracking for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Resume time tracking (for when player comes back online)
     */
    public CompletableFuture<Boolean> resumeTimeTracking(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                TimeSession session = activeSessions.get(playerId);
                Long pausedTime = pausedSessions.remove(playerId);
                
                if (session == null || pausedTime == null) {
                    return false;
                }
                
                // Resume the session with adjusted start time
                long currentTime = System.currentTimeMillis();
                session.startTime = currentTime - pausedTime;
                session.pauseTime = 0L;
                session.isPaused = false;
                
                // Restart individual sync task
                if (autoSyncEnabled) {
                    startIndividualSyncTask(player);
                }
                
                logger.fine("Resumed time tracking for " + player.getName() + " - Previous elapsed: " + (pausedTime / 1000) + "s");
                return true;
            } catch (Exception e) {
                logger.severe("Error resuming time tracking for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Perform comprehensive time synchronization for a player
     */
    public CompletableFuture<SyncResult> syncPlayerTime(Player player, boolean forceSync) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                PlayerData data = plugin.getDataManager().getPlayerData(playerId);
                
                if (data == null) {
                    return new SyncResult(false, "Player data not found", 0L, 0L);
                }
                
                TimeSession session = activeSessions.get(playerId);
                TimeSnapshot lastSnapshot = lastKnownStates.get(playerId);
                
                // Calculate current accurate time
                long calculatedTime = calculateCurrentAccurateTime(session, data);
                long storedTime = data.getTotalDutyTime();
                long discrepancy = Math.abs(calculatedTime - storedTime);
                
                // Check if sync is needed
                if (!forceSync && discrepancy < maxTimeDiscrepancy) {
                    return new SyncResult(true, "Time is synchronized", calculatedTime, discrepancy);
                }
                
                // Perform synchronization
                if (strictTimeValidation) {
                    // Use the most accurate calculation
                    long correctedTime = performStrictTimeCalculation(player, session, data, lastSnapshot);
                    data.setTotalDutyTime(correctedTime);
                } else {
                    // Use calculated time
                    data.setTotalDutyTime(calculatedTime);
                }
                
                // Update last known state
                lastKnownStates.put(playerId, new TimeSnapshot(data, System.currentTimeMillis()));
                
                // Save to database
                plugin.getDataManager().savePlayerData(data);
                
                logger.info("Synchronized time for " + player.getName() + 
                           " - Corrected: " + (calculatedTime / 1000) + "s, Discrepancy: " + (discrepancy / 1000) + "s");
                
                return new SyncResult(true, "Time synchronized successfully", calculatedTime, discrepancy);
                
            } catch (Exception e) {
                logger.severe("Error syncing time for " + player.getName() + ": " + e.getMessage());
                return new SyncResult(false, "Sync failed: " + e.getMessage(), 0L, 0L);
            }
        });
    }
    
    /**
     * Perform global time synchronization for all online players
     */
    public CompletableFuture<Map<UUID, SyncResult>> performGlobalTimeSync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, SyncResult> results = new HashMap<>();
            int syncedCount = 0;
            int errorCount = 0;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (hasActiveTimeSession(player)) {
                    try {
                        SyncResult result = syncPlayerTime(player, false).get();
                        results.put(player.getUniqueId(), result);
                        
                        if (result.success) {
                            syncedCount++;
                        } else {
                            errorCount++;
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to sync time for " + player.getName() + ": " + e.getMessage());
                        errorCount++;
                    }
                }
            }
            
            if (syncedCount > 0 || errorCount > 0) {
                logger.fine("Global time sync completed - Synced: " + syncedCount + ", Errors: " + errorCount);
            }
            
            return results;
        });
    }
    
    private long calculateAccurateDuration(TimeSession session) {
        if (session.isPaused) {
            return session.pauseTime - session.startTime;
        } else {
            return System.currentTimeMillis() - session.startTime;
        }
    }
    
    private long calculateCurrentAccurateTime(TimeSession session, PlayerData data) {
        long baseTime = data.getTotalDutyTime();
        
        if (session != null && data.isOnDuty()) {
            long currentSessionTime = calculateAccurateDuration(session);
            return baseTime + currentSessionTime;
        }
        
        return baseTime;
    }
    
    private long performStrictTimeCalculation(Player player, TimeSession session, PlayerData data, TimeSnapshot lastSnapshot) {
        long calculatedTime = data.getTotalDutyTime();
        
        // Add current session time if on duty
        if (session != null && data.isOnDuty()) {
            long sessionTime = calculateAccurateDuration(session);
            calculatedTime += sessionTime;
        }
        
        // Validate against last known state
        if (lastSnapshot != null) {
            long timeDifference = System.currentTimeMillis() - lastSnapshot.timestamp;
            long expectedMaxIncrease = timeDifference * 1.1; // Allow 10% tolerance
            
            long actualIncrease = calculatedTime - lastSnapshot.totalDutyTime;
            
            if (actualIncrease > expectedMaxIncrease) {
                // Time increase is suspicious, use conservative estimate
                calculatedTime = lastSnapshot.totalDutyTime + (timeDifference / 2);
                logger.warning("Applied conservative time calculation for " + player.getName() + 
                              " due to suspicious increase: " + (actualIncrease / 1000) + "s");
            }
        }
        
        return calculatedTime;
    }
    
    private void updatePlayerDataWithAccurateTime(Player player, long duration) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addDutyTime(duration / 1000L); // Convert to seconds
            plugin.getDataManager().savePlayerData(data);
        }
    }
    
    private void startIndividualSyncTask(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        stopIndividualSyncTask(playerId);
        
        // Start new sync task
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                syncPlayerTime(player, false);
            } else {
                stopIndividualSyncTask(playerId);
            }
        }, 20L * (syncInterval / 1000), 20L * (syncInterval / 1000));
        
        syncTasks.put(playerId, task);
    }
    
    private void stopIndividualSyncTask(UUID playerId) {
        BukkitTask task = syncTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Check if player has an active time session
     */
    public boolean hasActiveTimeSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Check if player has a paused time session
     */
    public boolean hasPausedTimeSession(Player player) {
        return pausedSessions.containsKey(player.getUniqueId());
    }
    
    /**
     * Get current session duration for a player
     */
    public long getCurrentSessionDuration(Player player) {
        TimeSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            return calculateAccurateDuration(session);
        }
        return 0L;
    }
    
    /**
     * Validate time consistency for a player
     */
    public CompletableFuture<ValidationResult> validatePlayerTime(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerId = player.getUniqueId();
                PlayerData data = plugin.getDataManager().getPlayerData(playerId);
                
                if (data == null) {
                    return new ValidationResult(false, "Player data not found", 0L);
                }
                
                TimeSession session = activeSessions.get(playerId);
                TimeSnapshot lastSnapshot = lastKnownStates.get(playerId);
                
                long calculatedTime = calculateCurrentAccurateTime(session, data);
                long storedTime = data.getTotalDutyTime();
                long discrepancy = Math.abs(calculatedTime - storedTime);
                
                boolean isValid = discrepancy < maxTimeDiscrepancy;
                
                return new ValidationResult(isValid, 
                    isValid ? "Time is valid" : "Time discrepancy detected: " + (discrepancy / 1000) + "s",
                    discrepancy);
                
            } catch (Exception e) {
                return new ValidationResult(false, "Validation error: " + e.getMessage(), 0L);
            }
        });
    }
    
    /**
     * Get time synchronization statistics
     */
    public Map<String, Object> getTimeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("pausedSessions", pausedSessions.size());
        stats.put("syncTasks", syncTasks.size());
        stats.put("autoSyncEnabled", autoSyncEnabled);
        stats.put("syncInterval", syncInterval / 1000);
        stats.put("maxDiscrepancy", maxTimeDiscrepancy / 1000);
        return stats;
    }
    
    public void cleanup() {
        // Cancel all sync tasks
        if (globalSyncTask != null) {
            globalSyncTask.cancel();
        }
        
        syncTasks.values().forEach(BukkitTask::cancel);
        syncTasks.clear();
        
        // Clear all tracking data
        activeSessions.clear();
        pausedSessions.clear();
        lastKnownStates.clear();
    }
    
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        activeSessions.remove(playerId);
        pausedSessions.remove(playerId);
        lastKnownStates.remove(playerId);
        stopIndividualSyncTask(playerId);
    }
    
    // === INNER CLASSES ===
    
    public static class TimeSession {
        public final UUID playerId;
        public final String sessionType;
        public long startTime;
        public long pauseTime;
        public boolean isPaused;
        
        public TimeSession(UUID playerId, String sessionType, long startTime) {
            this.playerId = playerId;
            this.sessionType = sessionType;
            this.startTime = startTime;
            this.pauseTime = 0L;
            this.isPaused = false;
        }
    }
    
    public static class TimeSnapshot {
        public final long totalDutyTime;
        public final long earnedOffDutyTime;
        public final boolean isOnDuty;
        public final long timestamp;
        
        public TimeSnapshot(PlayerData data, long timestamp) {
            this.totalDutyTime = data.getTotalDutyTime();
            this.earnedOffDutyTime = data.getEarnedOffDutyTime();
            this.isOnDuty = data.isOnDuty();
            this.timestamp = timestamp;
        }
    }
    
    public static class SyncResult {
        public final boolean success;
        public final String message;
        public final long correctedTime;
        public final long discrepancy;
        
        public SyncResult(boolean success, String message, long correctedTime, long discrepancy) {
            this.success = success;
            this.message = message;
            this.correctedTime = correctedTime;
            this.discrepancy = discrepancy;
        }
    }
    
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final long discrepancy;
        
        public ValidationResult(boolean isValid, String message, long discrepancy) {
            this.isValid = isValid;
            this.message = message;
            this.discrepancy = discrepancy;
        }
    }
}