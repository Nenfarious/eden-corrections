package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

public class DataManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Database handler
    private DatabaseHandler databaseHandler;
    
    // In-memory cache for performance (loaded from database)
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, ChaseData> activeChases;
    
    // Cache management
    private final Map<UUID, Long> lastCacheUpdate;
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000L; // 5 minutes
    
    public DataManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerDataCache = new ConcurrentHashMap<>();
        this.activeChases = new ConcurrentHashMap<>();
        this.lastCacheUpdate = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        try {
            // Initialize database handler based on configuration
            initializeDatabase();
            
            // Load existing data from database
            loadExistingData();
            
            // Start periodic cache cleanup
            startCacheCleanup();
            
            logger.info("DataManager initialized successfully with " + 
                       getDatabaseType() + " database!");
            
        } catch (Exception e) {
            logger.severe("Failed to initialize DataManager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DataManager initialization failed", e);
        }
    }
    
    private void initializeDatabase() throws SQLException {
        String databaseType = plugin.getConfigManager().getDatabaseType().toLowerCase();
        
        switch (databaseType) {
            case "sqlite":
                String sqliteFile = plugin.getConfigManager().getSQLiteFile();
                databaseHandler = new SQLiteHandler(plugin, sqliteFile);
                break;
                
            case "mysql":
                // Get MySQL configuration from config
                String host = plugin.getConfigManager().getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfigManager().getConfig().getInt("database.mysql.port", 3306);
                String database = plugin.getConfigManager().getConfig().getString("database.mysql.database", "edencorrections");
                String username = plugin.getConfigManager().getConfig().getString("database.mysql.username", "username");
                String password = plugin.getConfigManager().getConfig().getString("database.mysql.password", "password");
                
                databaseHandler = new MySQLHandler(plugin, host, port, database, username, password);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
        
        // Initialize the database handler
        databaseHandler.initialize();
        
        // Test the connection
        if (!databaseHandler.testConnection()) {
            throw new SQLException("Database connection test failed");
        }
    }
    
    private void loadExistingData() {
        try {
            // Load all player data
            CompletableFuture<List<PlayerData>> playerDataFuture = databaseHandler.loadAllPlayerData();
            List<PlayerData> playerDataList = playerDataFuture.get(30, TimeUnit.SECONDS);
            
            for (PlayerData playerData : playerDataList) {
                playerDataCache.put(playerData.getPlayerId(), playerData);
                lastCacheUpdate.put(playerData.getPlayerId(), System.currentTimeMillis());
            }
            
            // Load all active chases
            CompletableFuture<List<ChaseData>> chaseDataFuture = databaseHandler.loadAllActiveChases();
            List<ChaseData> chaseDataList = chaseDataFuture.get(30, TimeUnit.SECONDS);
            
            for (ChaseData chaseData : chaseDataList) {
                activeChases.put(chaseData.getChaseId(), chaseData);
            }
            
            logger.info("Loaded " + playerDataList.size() + " player records and " + 
                       chaseDataList.size() + " active chases from database");
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.severe("Failed to load existing data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startCacheCleanup() {
        // Schedule periodic cache cleanup
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cleanupExpiredCache();
            performDatabaseMaintenance();
        }, 20L * 60L * 5L, 20L * 60L * 5L); // Run every 5 minutes
    }
    
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        // Remove expired cache entries
        lastCacheUpdate.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > CACHE_EXPIRY_TIME) {
                playerDataCache.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        // Clean up expired chases
        activeChases.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    private void performDatabaseMaintenance() {
        try {
            databaseHandler.performMaintenance();
        } catch (Exception e) {
            logger.warning("Database maintenance failed: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        try {
            // Save all cached data to database
            saveAllCachedData();
            
            // Close database connection
            if (databaseHandler != null) {
                databaseHandler.close();
            }
            
            logger.info("DataManager shutdown successfully!");
            
        } catch (Exception e) {
            logger.severe("Error during DataManager shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveAllCachedData() {
        try {
            // Save all player data
            List<PlayerData> playerDataList = new ArrayList<>(playerDataCache.values());
            if (!playerDataList.isEmpty()) {
                databaseHandler.batchSavePlayerData(playerDataList).get(30, TimeUnit.SECONDS);
                logger.info("Saved " + playerDataList.size() + " player records to database");
            }
            
            // Save all chase data
            for (ChaseData chaseData : activeChases.values()) {
                databaseHandler.saveChaseData(chaseData);
            }
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.severe("Failed to save all cached data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // === PLAYER DATA METHODS ===
    
    public PlayerData getPlayerData(UUID playerId) {
        // Check cache first
        PlayerData cachedData = playerDataCache.get(playerId);
        if (cachedData != null) {
            // Check if cache is still valid
            Long lastUpdate = lastCacheUpdate.get(playerId);
            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < CACHE_EXPIRY_TIME) {
                return cachedData;
            }
        }
        
        // Load from database
        try {
            CompletableFuture<PlayerData> future = databaseHandler.loadPlayerData(playerId);
            PlayerData data = future.get(5, TimeUnit.SECONDS);
            
            if (data != null) {
                playerDataCache.put(playerId, data);
                lastCacheUpdate.put(playerId, System.currentTimeMillis());
            }
            
            return data;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to load player data for " + playerId + ": " + e.getMessage());
            return cachedData; // Return cached data if database fails
        }
    }
    
    public PlayerData getOrCreatePlayerData(UUID playerId, String playerName) {
        PlayerData data = getPlayerData(playerId);
        if (data == null) {
            data = new PlayerData(playerId, playerName);
            // Save to database immediately
            savePlayerData(data);
        }
        return data;
    }
    
    public void savePlayerData(PlayerData playerData) {
        // Update cache
        playerDataCache.put(playerData.getPlayerId(), playerData);
        lastCacheUpdate.put(playerData.getPlayerId(), System.currentTimeMillis());
        
        // Save to database asynchronously
        databaseHandler.savePlayerData(playerData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save player data for " + playerData.getPlayerName() + ": " + throwable.getMessage());
            } else if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Saved player data for " + playerData.getPlayerName());
            }
        });
    }
    
    public PlayerData getPlayerDataByName(String playerName) {
        // Check cache first
        for (PlayerData data : playerDataCache.values()) {
            if (data.getPlayerName().equalsIgnoreCase(playerName)) {
                return data;
            }
        }
        
        // Load from database
        try {
            CompletableFuture<PlayerData> future = databaseHandler.loadPlayerDataByName(playerName);
            PlayerData data = future.get(5, TimeUnit.SECONDS);
            
            if (data != null) {
                playerDataCache.put(data.getPlayerId(), data);
                lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
            }
            
            return data;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to load player data for " + playerName + ": " + e.getMessage());
            return null;
        }
    }
    
    public void deletePlayerData(UUID playerId) {
        // Remove from cache
        playerDataCache.remove(playerId);
        lastCacheUpdate.remove(playerId);
        
        // Delete from database
        databaseHandler.deletePlayerData(playerId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to delete player data for " + playerId + ": " + throwable.getMessage());
            }
        });
    }
    
    // === CHASE DATA METHODS ===
    
    public ChaseData getChaseData(UUID chaseId) {
        return activeChases.get(chaseId);
    }
    
    public ChaseData getChaseByGuard(UUID guardId) {
        return activeChases.values().stream()
                .filter(chase -> chase.getGuardId().equals(guardId) && chase.isActive())
                .findFirst()
                .orElse(null);
    }
    
    public ChaseData getChaseByTarget(UUID targetId) {
        return activeChases.values().stream()
                .filter(chase -> chase.getTargetId().equals(targetId) && chase.isActive())
                .findFirst()
                .orElse(null);
    }
    
    public void addChaseData(ChaseData chaseData) {
        activeChases.put(chaseData.getChaseId(), chaseData);
        
        // Save to database asynchronously
        databaseHandler.saveChaseData(chaseData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save chase data: " + throwable.getMessage());
            }
        });
    }
    
    public void removeChaseData(UUID chaseId) {
        ChaseData chaseData = activeChases.remove(chaseId);
        
        if (chaseData != null) {
            // Update in database (mark as inactive)
            databaseHandler.saveChaseData(chaseData).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Failed to update chase data: " + throwable.getMessage());
                }
            });
        }
    }
    
    public Collection<ChaseData> getAllActiveChases() {
        return activeChases.values();
    }
    
    public void cleanupExpiredChases() {
        List<UUID> expiredChases = new ArrayList<>();
        
        for (ChaseData chase : activeChases.values()) {
            if (chase.isExpired()) {
                expiredChases.add(chase.getChaseId());
            }
        }
        
        for (UUID chaseId : expiredChases) {
            removeChaseData(chaseId);
        }
        
        // Clean up database
        databaseHandler.cleanupExpiredChases().whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.warning("Failed to cleanup expired chases from database: " + throwable.getMessage());
            }
        });
    }
    
    public void cleanupExpiredWantedLevels() {
        List<PlayerData> expiredPlayers = new ArrayList<>();
        
        for (PlayerData playerData : playerDataCache.values()) {
            if (playerData.hasExpiredWanted()) {
                playerData.clearWantedLevel();
                expiredPlayers.add(playerData);
            }
        }
        
        // Save updated data
        for (PlayerData playerData : expiredPlayers) {
            savePlayerData(playerData);
        }
    }
    
    // === INVENTORY CACHING METHODS ===
    
    public void savePlayerInventory(UUID playerId, String inventoryData) {
        databaseHandler.savePlayerInventory(playerId, inventoryData).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to save player inventory for " + playerId + ": " + throwable.getMessage());
            } else if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Saved inventory cache for " + playerId);
            }
        });
    }
    
    public String loadPlayerInventory(UUID playerId) {
        try {
            CompletableFuture<String> future = databaseHandler.loadPlayerInventory(playerId);
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to load player inventory for " + playerId + ": " + e.getMessage());
            return null;
        }
    }
    
    public void deletePlayerInventory(UUID playerId) {
        databaseHandler.deletePlayerInventory(playerId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to delete player inventory for " + playerId + ": " + throwable.getMessage());
            }
        });
    }
    
    // === UTILITY METHODS ===
    
    public boolean isPlayerOnDuty(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isOnDuty();
    }
    
    public boolean isPlayerWanted(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isWanted();
    }
    
    public boolean isPlayerBeingChased(UUID playerId) {
        PlayerData data = getPlayerData(playerId);
        return data != null && data.isBeingChased();
    }
    
    public boolean isGuardChasing(UUID guardId) {
        return getChaseByGuard(guardId) != null;
    }
    
    public int getActiveChaseCount() {
        return (int) activeChases.values().stream()
                .filter(ChaseData::isActive)
                .count();
    }
    
    // === STATISTICS AND DIAGNOSTICS ===
    
    public DatabaseHandler.DatabaseStats getDatabaseStats() {
        try {
            CompletableFuture<DatabaseHandler.DatabaseStats> future = databaseHandler.getStatistics();
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to get database statistics: " + e.getMessage());
            return null;
        }
    }
    
    public String getDatabaseType() {
        if (databaseHandler instanceof SQLiteHandler) {
            return "SQLite";
        } else if (databaseHandler instanceof MySQLHandler) {
            return "MySQL";
        } else {
            return "Unknown";
        }
    }
    
    public boolean isDatabaseConnected() {
        return databaseHandler != null && databaseHandler.isConnected();
    }
    
    public boolean testDatabaseConnection() {
        return databaseHandler != null && databaseHandler.testConnection();
    }
    
    public void createDatabaseBackup(String backupPath) {
        if (databaseHandler != null) {
            databaseHandler.createBackup(backupPath).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Failed to create database backup: " + throwable.getMessage());
                } else {
                    logger.info("Database backup created successfully at: " + backupPath);
                }
            });
        }
    }
    
    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> diagnostics = new ConcurrentHashMap<>();
        
        diagnostics.put("databaseType", getDatabaseType());
        diagnostics.put("databaseConnected", isDatabaseConnected());
        diagnostics.put("cachedPlayerData", playerDataCache.size());
        diagnostics.put("activeChases", activeChases.size());
        diagnostics.put("cacheHitRate", calculateCacheHitRate());
        
        DatabaseHandler.DatabaseStats stats = getDatabaseStats();
        if (stats != null) {
            diagnostics.put("totalPlayersInDB", stats.getTotalPlayers());
            diagnostics.put("activeChasesInDB", stats.getActiveChases());
            diagnostics.put("cachedInventoriesInDB", stats.getCachedInventories());
            diagnostics.put("databaseSizeBytes", stats.getDatabaseSize());
        }
        
        return diagnostics;
    }
    
    private double calculateCacheHitRate() {
        // This is a simplified calculation
        // In a real implementation, you'd track cache hits vs misses
        int totalCacheEntries = playerDataCache.size();
        int recentEntries = (int) lastCacheUpdate.values().stream()
                .filter(time -> System.currentTimeMillis() - time < CACHE_EXPIRY_TIME)
                .count();
        
        return totalCacheEntries > 0 ? (double) recentEntries / totalCacheEntries : 0.0;
    }
    
    // === BATCH OPERATIONS ===
    
    public void batchSavePlayerData(List<PlayerData> playerDataList) {
        if (playerDataList.isEmpty()) return;
        
        // Update cache
        for (PlayerData data : playerDataList) {
            playerDataCache.put(data.getPlayerId(), data);
            lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
        }
        
        // Save to database
        databaseHandler.batchSavePlayerData(playerDataList).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.severe("Failed to batch save player data: " + throwable.getMessage());
            } else {
                logger.info("Batch saved " + playerDataList.size() + " player records");
            }
        });
    }
    
    public List<PlayerData> batchLoadPlayerData(List<UUID> playerIds) {
        if (playerIds.isEmpty()) return new ArrayList<>();
        
        try {
            CompletableFuture<List<PlayerData>> future = databaseHandler.batchLoadPlayerData(playerIds);
            List<PlayerData> playerDataList = future.get(10, TimeUnit.SECONDS);
            
            // Update cache
            for (PlayerData data : playerDataList) {
                playerDataCache.put(data.getPlayerId(), data);
                lastCacheUpdate.put(data.getPlayerId(), System.currentTimeMillis());
            }
            
            return playerDataList;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warning("Failed to batch load player data: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // === ADVANCED QUERY METHODS ===
    
    public List<PlayerData> getOnlineGuards() {
        return playerDataCache.values().stream()
                .filter(data -> data.isOnDuty() && plugin.getServer().getPlayer(data.getPlayerId()) != null)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<PlayerData> getWantedPlayers() {
        return playerDataCache.values().stream()
                .filter(PlayerData::isWanted)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<PlayerData> getPlayersInChase() {
        return playerDataCache.values().stream()
                .filter(PlayerData::isBeingChased)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public int getTotalGuardDutyTime() {
        return playerDataCache.values().stream()
                .mapToInt(data -> (int) (data.getTotalDutyTime() / 1000L))
                .sum();
    }
    
    public int getTotalArrests() {
        return playerDataCache.values().stream()
                .mapToInt(PlayerData::getTotalArrests)
                .sum();
    }
    
    public int getTotalViolations() {
        return playerDataCache.values().stream()
                .mapToInt(PlayerData::getTotalViolations)
                .sum();
    }
} 