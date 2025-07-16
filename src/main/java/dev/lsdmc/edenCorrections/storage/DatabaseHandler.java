package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.sql.SQLException;

public interface DatabaseHandler {
    
    // === CONNECTION MANAGEMENT ===
    
    /**
     * Initialize the database connection and create tables if they don't exist
     * @throws SQLException if database initialization fails
     */
    void initialize() throws SQLException;
    
    /**
     * Close the database connection
     */
    void close();
    
    /**
     * Check if the database connection is valid
     * @return true if connection is valid, false otherwise
     */
    boolean isConnected();
    
    /**
     * Test the database connection
     * @return true if connection test passed, false otherwise
     */
    boolean testConnection();
    
    // === PLAYER DATA OPERATIONS ===
    
    /**
     * Save player data to the database
     * @param playerData the player data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerData(PlayerData playerData);
    
    /**
     * Load player data from the database
     * @param playerId the player's UUID
     * @return CompletableFuture containing the player data, or null if not found
     */
    CompletableFuture<PlayerData> loadPlayerData(UUID playerId);
    
    /**
     * Load player data by name (for offline operations)
     * @param playerName the player's name
     * @return CompletableFuture containing the player data, or null if not found
     */
    CompletableFuture<PlayerData> loadPlayerDataByName(String playerName);
    
    /**
     * Load all player data from the database
     * @return CompletableFuture containing list of all player data
     */
    CompletableFuture<List<PlayerData>> loadAllPlayerData();
    
    /**
     * Delete player data from the database
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deletePlayerData(UUID playerId);
    
    // === CHASE DATA OPERATIONS ===
    
    /**
     * Save chase data to the database
     * @param chaseData the chase data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> saveChaseData(ChaseData chaseData);
    
    /**
     * Load chase data from the database
     * @param chaseId the chase ID
     * @return CompletableFuture containing the chase data, or null if not found
     */
    CompletableFuture<ChaseData> loadChaseData(UUID chaseId);
    
    /**
     * Load all active chase data from the database
     * @return CompletableFuture containing list of all active chase data
     */
    CompletableFuture<List<ChaseData>> loadAllActiveChases();
    
    /**
     * Delete chase data from the database
     * @param chaseId the chase ID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deleteChaseData(UUID chaseId);
    
    /**
     * Clean up expired chase data from the database
     * @return CompletableFuture that completes when cleanup is done
     */
    CompletableFuture<Void> cleanupExpiredChases();
    
    // === INVENTORY CACHING OPERATIONS ===
    
    /**
     * Save player's inventory data to the database
     * @param playerId the player's UUID
     * @param inventoryData serialized inventory data
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerInventory(UUID playerId, String inventoryData);
    
    /**
     * Load player's inventory data from the database
     * @param playerId the player's UUID
     * @return CompletableFuture containing the serialized inventory data, or null if not found
     */
    CompletableFuture<String> loadPlayerInventory(UUID playerId);
    
    /**
     * Delete player's inventory data from the database
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deletePlayerInventory(UUID playerId);
    
    // === STATISTICS AND MAINTENANCE ===
    
    /**
     * Clean up old/expired data from the database
     * @return CompletableFuture that completes when cleanup is done
     */
    CompletableFuture<Void> performMaintenance();
    
    /**
     * Get database statistics
     * @return CompletableFuture containing database statistics
     */
    CompletableFuture<DatabaseStats> getStatistics();
    
    /**
     * Create a backup of the database
     * @param backupPath the path to save the backup
     * @return CompletableFuture that completes when backup is done
     */
    CompletableFuture<Void> createBackup(String backupPath);
    
    // === BATCH OPERATIONS ===
    
    /**
     * Save multiple player data entries in a batch
     * @param playerDataList list of player data to save
     * @return CompletableFuture that completes when batch save is done
     */
    CompletableFuture<Void> batchSavePlayerData(List<PlayerData> playerDataList);
    
    /**
     * Load multiple player data entries by UUIDs
     * @param playerIds list of player UUIDs
     * @return CompletableFuture containing list of player data
     */
    CompletableFuture<List<PlayerData>> batchLoadPlayerData(List<UUID> playerIds);
    
    // === DATABASE STATS CLASS ===
    
    class DatabaseStats {
        private final int totalPlayers;
        private final int activeChases;
        private final int cachedInventories;
        private final long databaseSize;
        private final String databaseType;
        private final long lastMaintenance;
        
        public DatabaseStats(int totalPlayers, int activeChases, int cachedInventories, 
                           long databaseSize, String databaseType, long lastMaintenance) {
            this.totalPlayers = totalPlayers;
            this.activeChases = activeChases;
            this.cachedInventories = cachedInventories;
            this.databaseSize = databaseSize;
            this.databaseType = databaseType;
            this.lastMaintenance = lastMaintenance;
        }
        
        public int getTotalPlayers() { return totalPlayers; }
        public int getActiveChases() { return activeChases; }
        public int getCachedInventories() { return cachedInventories; }
        public long getDatabaseSize() { return databaseSize; }
        public String getDatabaseType() { return databaseType; }
        public long getLastMaintenance() { return lastMaintenance; }
    }
} 