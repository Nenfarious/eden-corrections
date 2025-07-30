<<<<<<< HEAD
package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SQLiteHandler implements DatabaseHandler {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final String databasePath;
    private final ExecutorService executor;
    
    private Connection connection;
    private boolean initialized = false;
    
    // Prepared statement cache for performance
    private final Map<String, PreparedStatement> statementCache = new ConcurrentHashMap<>();
    
    // Database schema version for migrations
    private static final int SCHEMA_VERSION = 1;
    
    public SQLiteHandler(EdenCorrections plugin, String databasePath) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.databasePath = databasePath;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "EdenCorrections-DB-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() throws SQLException {
        try {
            // Ensure database directory exists
            File dbFile = new File(plugin.getDataFolder(), databasePath);
            File parentDir = dbFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new SQLException("Failed to create database directory: " + parentDir.getPath());
            }
            
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Configure connection
            connection.setAutoCommit(true);
            
            // Enable foreign keys and performance optimizations
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = 10000");
                stmt.execute("PRAGMA temp_store = MEMORY");
            }
            
            // Create tables
            createTables();
            
            // Check and perform migrations if needed
            checkAndMigrate();
            
            initialized = true;
            logger.info("SQLite database initialized successfully at: " + dbFile.getPath());
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            throw e;
        }
    }
    
    private void createTables() throws SQLException {
        String[] tableCreationSql = {
            // Player data table
            """
            CREATE TABLE IF NOT EXISTS player_data (
                player_id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                is_on_duty INTEGER NOT NULL DEFAULT 0,
                duty_start_time INTEGER NOT NULL DEFAULT 0,
                off_duty_time INTEGER NOT NULL DEFAULT 0,
                grace_debt_time INTEGER NOT NULL DEFAULT 0,
                guard_rank TEXT,
                earned_off_duty_time INTEGER NOT NULL DEFAULT 0,
                has_earned_base_time INTEGER NOT NULL DEFAULT 0,
                has_been_notified_expired INTEGER NOT NULL DEFAULT 0,
                session_searches INTEGER NOT NULL DEFAULT 0,
                session_successful_searches INTEGER NOT NULL DEFAULT 0,
                session_arrests INTEGER NOT NULL DEFAULT 0,
                session_kills INTEGER NOT NULL DEFAULT 0,
                session_detections INTEGER NOT NULL DEFAULT 0,
                penalty_start_time INTEGER NOT NULL DEFAULT 0,
                current_penalty_stage INTEGER NOT NULL DEFAULT 0,
                last_penalty_time INTEGER NOT NULL DEFAULT 0,
                last_slowness_application INTEGER NOT NULL DEFAULT 0,
                has_active_penalty_boss_bar INTEGER NOT NULL DEFAULT 0,
                wanted_level INTEGER NOT NULL DEFAULT 0,
                wanted_expire_time INTEGER NOT NULL DEFAULT 0,
                wanted_reason TEXT,
                being_chased INTEGER NOT NULL DEFAULT 0,
                chaser_guard TEXT,
                chase_start_time INTEGER NOT NULL DEFAULT 0,
                total_arrests INTEGER NOT NULL DEFAULT 0,
                total_violations INTEGER NOT NULL DEFAULT 0,
                total_duty_time INTEGER NOT NULL DEFAULT 0,
                last_updated INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Chase data table
            """
            CREATE TABLE IF NOT EXISTS chase_data (
                chase_id TEXT PRIMARY KEY,
                guard_id TEXT NOT NULL,
                target_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                duration INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                end_reason TEXT,
                end_time INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Player inventory cache table
            """
            CREATE TABLE IF NOT EXISTS player_inventory_cache (
                player_id TEXT PRIMARY KEY,
                inventory_data TEXT NOT NULL,
                cached_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            )
            """,
            
            // Database metadata table
            """
            CREATE TABLE IF NOT EXISTS database_metadata (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Performance statistics table
            """
            CREATE TABLE IF NOT EXISTS performance_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id TEXT NOT NULL,
                stat_type TEXT NOT NULL,
                stat_value INTEGER NOT NULL,
                recorded_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            )
            """
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : tableCreationSql) {
                stmt.execute(sql);
            }
        }
        
        // Create indexes for performance
        createIndexes();
    }
    
    private void createIndexes() throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_player_name ON player_data(player_name)",
            "CREATE INDEX IF NOT EXISTS idx_player_duty ON player_data(is_on_duty)",
            "CREATE INDEX IF NOT EXISTS idx_player_wanted ON player_data(wanted_level)",
            "CREATE INDEX IF NOT EXISTS idx_chase_guard ON chase_data(guard_id)",
            "CREATE INDEX IF NOT EXISTS idx_chase_target ON chase_data(target_id)",
            "CREATE INDEX IF NOT EXISTS idx_chase_active ON chase_data(is_active)",
            "CREATE INDEX IF NOT EXISTS idx_chase_cleanup ON chase_data(is_active, end_time)",
            "CREATE INDEX IF NOT EXISTS idx_performance_player ON performance_stats(player_id)",
            "CREATE INDEX IF NOT EXISTS idx_performance_type ON performance_stats(stat_type)",
            "CREATE INDEX IF NOT EXISTS idx_performance_time ON performance_stats(recorded_at)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }
    
    private void checkAndMigrate() throws SQLException {
        // Check current schema version
        int currentVersion = getSchemaVersion();
        
        if (currentVersion < SCHEMA_VERSION) {
            logger.info("Database schema outdated. Performing migration from version " + 
                       currentVersion + " to " + SCHEMA_VERSION);
            performMigration(currentVersion);
            setSchemaVersion(SCHEMA_VERSION);
        }
    }
    
    private int getSchemaVersion() throws SQLException {
        String sql = "SELECT value FROM database_metadata WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "schema_version");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (SQLException e) {
            // Table might not exist yet
        }
        return 0;
    }
    
    private void setSchemaVersion(int version) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO database_metadata (key, value, updated_at) 
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "schema_version");
            stmt.setString(2, String.valueOf(version));
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    private void performMigration(int fromVersion) throws SQLException {
        // Future migrations would go here
        logger.info("No migrations needed from version " + fromVersion);
    }
    
    // Get cached prepared statement or create new one
    private PreparedStatement getPreparedStatement(String sql) throws SQLException {
        return statementCache.computeIfAbsent(sql, k -> {
            try {
                return connection.prepareStatement(k);
            } catch (SQLException e) {
                logger.warning("Failed to create prepared statement: " + e.getMessage());
                return null;
            }
        });
    }
    
    @Override
    public void close() {
        try {
            // Close all prepared statements
            for (PreparedStatement stmt : statementCache.values()) {
                if (stmt != null && !stmt.isClosed()) {
                    stmt.close();
                }
            }
            statementCache.clear();
            
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            executor.shutdown();
            logger.info("SQLite database connection closed");
        } catch (SQLException e) {
            logger.warning("Error closing SQLite database: " + e.getMessage());
        }
        initialized = false;
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && initialized;
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!isConnected()) return false;
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (SQLException e) {
            logger.warning("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    // === PLAYER DATA OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, penalty_start_time,
                    current_penalty_stage, last_penalty_time, last_slowness_application,
                    has_active_penalty_boss_bar, wanted_level, wanted_expire_time, wanted_reason, 
                    being_chased, chaser_guard, chase_start_time, total_arrests, total_violations, 
                    total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try {
                PreparedStatement stmt = getPreparedStatement(sql);
                synchronized (stmt) {
                    stmt.setString(1, playerData.getPlayerId().toString());
                    stmt.setString(2, playerData.getPlayerName());
                    stmt.setInt(3, playerData.isOnDuty() ? 1 : 0);
                    stmt.setLong(4, playerData.getDutyStartTime());
                    stmt.setLong(5, playerData.getOffDutyTime());
                    stmt.setLong(6, playerData.getGraceDebtTime());
                    stmt.setString(7, playerData.getGuardRank());
                    stmt.setLong(8, playerData.getEarnedOffDutyTime());
                    stmt.setInt(9, playerData.hasEarnedBaseTime() ? 1 : 0);
                    stmt.setInt(10, playerData.hasBeenNotifiedOfExpiredTime() ? 1 : 0);
                    stmt.setInt(11, playerData.getSessionSearches());
                    stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                    stmt.setInt(13, playerData.getSessionArrests());
                    stmt.setInt(14, playerData.getSessionKills());
                    stmt.setInt(15, playerData.getSessionDetections());
                    stmt.setLong(16, playerData.getPenaltyStartTime());
                    stmt.setInt(17, playerData.getCurrentPenaltyStage());
                    stmt.setLong(18, playerData.getLastPenaltyTime());
                    stmt.setLong(19, playerData.getLastSlownessApplication());
                    stmt.setInt(20, playerData.hasActivePenaltyBossBar() ? 1 : 0);
                    stmt.setInt(21, playerData.getWantedLevel());
                    stmt.setLong(22, playerData.getWantedExpireTime());
                    stmt.setString(23, playerData.getWantedReason());
                    stmt.setInt(24, playerData.isBeingChased() ? 1 : 0);
                    stmt.setString(25, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                    stmt.setLong(26, playerData.getChaseStartTime());
                    stmt.setInt(27, playerData.getTotalArrests());
                    stmt.setInt(28, playerData.getTotalViolations());
                    stmt.setLong(29, playerData.getTotalDutyTime());
                    stmt.setLong(30, System.currentTimeMillis());
                    
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                logger.severe("Failed to save player data for " + playerData.getPlayerName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE player_id = ?";
            
            try {
                PreparedStatement stmt = getPreparedStatement(sql);
                synchronized (stmt) {
                    stmt.setString(1, playerId.toString());
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        return mapResultSetToPlayerData(rs);
                    }
                    return null;
                }
            } catch (SQLException e) {
                logger.severe("Failed to load player data for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<PlayerData> loadPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE player_name = ? COLLATE NOCASE";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return mapResultSetToPlayerData(rs);
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load player data for " + playerName + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("player_id"));
        String playerName = rs.getString("player_name");
        
        PlayerData data = new PlayerData(playerId, playerName);
        
        // Set duty information
        data.setOnDuty(rs.getInt("is_on_duty") == 1);
        data.setDutyStartTime(rs.getLong("duty_start_time"));
        data.setOffDutyTime(rs.getLong("off_duty_time"));
        data.setGraceDebtTime(rs.getLong("grace_debt_time"));
        data.setGuardRank(rs.getString("guard_rank"));
        
        // Set off-duty earning information
        data.setEarnedOffDutyTime(rs.getLong("earned_off_duty_time"));
        data.setHasEarnedBaseTime(rs.getInt("has_earned_base_time") == 1);
        data.setHasBeenNotifiedOfExpiredTime(rs.getInt("has_been_notified_expired") == 1);
        
        // Set session performance
        data.setSessionSearches(rs.getInt("session_searches"));
        data.setSessionSuccessfulSearches(rs.getInt("session_successful_searches"));
        data.setSessionArrests(rs.getInt("session_arrests"));
        data.setSessionKills(rs.getInt("session_kills"));
        data.setSessionDetections(rs.getInt("session_detections"));
        
        // Set penalty tracking information
        data.setPenaltyStartTime(rs.getLong("penalty_start_time"));
        data.setCurrentPenaltyStage(rs.getInt("current_penalty_stage"));
        data.setLastPenaltyTime(rs.getLong("last_penalty_time"));
        data.setLastSlownessApplication(rs.getLong("last_slowness_application"));
        data.setHasActivePenaltyBossBar(rs.getInt("has_active_penalty_boss_bar") == 1);
        
        // Set wanted information
        data.setWantedLevel(rs.getInt("wanted_level"));
        data.setWantedExpireTime(rs.getLong("wanted_expire_time"));
        data.setWantedReason(rs.getString("wanted_reason"));
        
        // Set chase information
        data.setBeingChased(rs.getInt("being_chased") == 1);
        String chaserGuardStr = rs.getString("chaser_guard");
        if (chaserGuardStr != null) {
            data.setChaserGuard(UUID.fromString(chaserGuardStr));
        }
        data.setChaseStartTime(rs.getLong("chase_start_time"));
        
        // Set statistics
        data.setTotalArrests(rs.getInt("total_arrests"));
        data.setTotalViolations(rs.getInt("total_violations"));
        data.setTotalDutyTime(rs.getLong("total_duty_time"));
        
        return data;
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> playerDataList = new ArrayList<>();
            String sql = "SELECT * FROM player_data ORDER BY player_name";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    playerDataList.add(mapResultSetToPlayerData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to load all player data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return playerDataList;
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_data WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete player data for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === CHASE DATA OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> saveChaseData(ChaseData chaseData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO chase_data (
                    chase_id, guard_id, target_id, start_time, duration, is_active, 
                    end_reason, end_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseData.getChaseId().toString());
                stmt.setString(2, chaseData.getGuardId().toString());
                stmt.setString(3, chaseData.getTargetId().toString());
                stmt.setLong(4, chaseData.getStartTime());
                stmt.setLong(5, chaseData.getDuration());
                stmt.setInt(6, chaseData.isActive() ? 1 : 0);
                stmt.setString(7, chaseData.getEndReason());
                stmt.setLong(8, chaseData.getEndTime());
                stmt.setLong(9, System.currentTimeMillis());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to save chase data: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<ChaseData> loadChaseData(UUID chaseId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM chase_data WHERE chase_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return mapResultSetToChaseData(rs);
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load chase data for " + chaseId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private ChaseData mapResultSetToChaseData(ResultSet rs) throws SQLException {
        UUID chaseId = UUID.fromString(rs.getString("chase_id"));
        UUID guardId = UUID.fromString(rs.getString("guard_id"));
        UUID targetId = UUID.fromString(rs.getString("target_id"));
        long startTime = rs.getLong("start_time");
        long duration = rs.getLong("duration");
        
        ChaseData chaseData = new ChaseData(chaseId, guardId, targetId, duration);
        
        if (rs.getInt("is_active") == 0) {
            chaseData.endChase(rs.getString("end_reason"));
        }
        
        return chaseData;
    }
    
    @Override
    public CompletableFuture<List<ChaseData>> loadAllActiveChases() {
        return CompletableFuture.supplyAsync(() -> {
            List<ChaseData> chaseDataList = new ArrayList<>();
            String sql = "SELECT * FROM chase_data WHERE is_active = 1 ORDER BY start_time DESC";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    chaseDataList.add(mapResultSetToChaseData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to load active chase data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return chaseDataList;
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deleteChaseData(UUID chaseId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM chase_data WHERE chase_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete chase data for " + chaseId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> cleanupExpiredChases() {
        return CompletableFuture.runAsync(() -> {
            // Delete chases older than 24 hours
            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            String sql = "DELETE FROM chase_data WHERE is_active = 0 AND end_time < ?";
            
            try {
                PreparedStatement stmt = getPreparedStatement(sql);
                synchronized (stmt) {
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " expired chase records");
                    }
                }
            } catch (SQLException e) {
                logger.severe("Failed to cleanup expired chases: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === INVENTORY OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> savePlayerInventory(UUID playerId, String inventoryData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_inventory_cache (player_id, inventory_data, cached_at) 
                VALUES (?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, inventoryData);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to save player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<String> loadPlayerInventory(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT inventory_data FROM player_inventory_cache WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getString("inventory_data");
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerInventory(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_inventory_cache WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<UUID>> getPlayersWithStoredInventory() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_id FROM player_inventory_cache";
            List<UUID> playerIds = new ArrayList<>();
            
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    try {
                        UUID playerId = UUID.fromString(rs.getString("player_id"));
                        playerIds.add(playerId);
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid UUID in inventory cache: " + rs.getString("player_id"));
                    }
                }
            } catch (SQLException e) {
                logger.severe("Failed to get players with stored inventory: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return playerIds;
        }, executor);
    }
    
    // === MAINTENANCE AND STATISTICS ===
    
    @Override
    public CompletableFuture<Void> performMaintenance() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Clean up expired chases
                cleanupExpiredChases().get();
                
                // Clean up old inventory cache (older than 7 days)
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                String sql = "DELETE FROM player_inventory_cache WHERE cached_at < ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old inventory cache entries");
                    }
                }
                
                // Attempt to vacuum database for performance (with safety checks)
                attemptVacuum();
                
                // Update maintenance timestamp
                setSchemaVersion(SCHEMA_VERSION);
                
                logger.info("Database maintenance completed successfully");
                
            } catch (Exception e) {
                logger.severe("Database maintenance failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Attempts to vacuum the database with safety checks to prevent conflicts
     */
    private void attemptVacuum() {
        if (!plugin.getConfigManager().isDatabaseVacuumEnabled()) {
            logger.warning("VACUUM operation is disabled in configuration");
            return;
        }
        
        logger.info("Starting VACUUM operation on SQLite database...");
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            // Set a timeout for the vacuum operation
            int timeout = plugin.getConfigManager().getDatabaseVacuumTimeout();
            
            // Set busy timeout
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = " + timeout);
            }
            
            // Check if database is busy by attempting to get a lock
            try {
                // Try to get an exclusive lock
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("BEGIN EXCLUSIVE");
                    stmt.execute("ROLLBACK");
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("database is locked") || 
                    e.getMessage().contains("busy")) {
                    logger.warning("Database is busy, skipping VACUUM operation");
                    return;
                }
            }
            
            // Try to vacuum with the configured timeout
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("VACUUM");
                logger.info("Database VACUUM completed successfully");
            }
            
        } catch (SQLException e) {
            if (e.getMessage().contains("database is locked") || 
                e.getMessage().contains("cannot VACUUM") ||
                e.getMessage().contains("SQL statements in progress") ||
                e.getMessage().contains("busy")) {
                logger.warning("Database is busy, skipping VACUUM operation: " + e.getMessage());
            } else {
                logger.warning("VACUUM operation failed: " + e.getMessage());
            }
        }
    }
    
    @Override
    public CompletableFuture<DatabaseStats> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalPlayers = 0;
                int activeChases = 0;
                int cachedInventories = 0;
                long databaseSize = 0;
                
                // Count total players
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_data");
                    if (rs.next()) {
                        totalPlayers = rs.getInt(1);
                    }
                }
                
                // Count active chases
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM chase_data WHERE is_active = 1");
                    if (rs.next()) {
                        activeChases = rs.getInt(1);
                    }
                }
                
                // Count cached inventories
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_inventory_cache");
                    if (rs.next()) {
                        cachedInventories = rs.getInt(1);
                    }
                }
                
                // Get database size
                File dbFile = new File(plugin.getDataFolder(), databasePath);
                if (dbFile.exists()) {
                    databaseSize = dbFile.length();
                }
                
                return new DatabaseStats(totalPlayers, activeChases, cachedInventories, 
                                       databaseSize, "SQLite", System.currentTimeMillis());
                
            } catch (SQLException e) {
                logger.severe("Failed to get database statistics: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> createBackup(String backupPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                File dbFile = new File(plugin.getDataFolder(), databasePath);
                Path backupFilePath = Paths.get(backupPath);
                
                // Ensure backup directory exists
                Files.createDirectories(backupFilePath.getParent());
                
                // Copy database file
                Files.copy(dbFile.toPath(), backupFilePath);
                
                logger.info("Database backup created at: " + backupPath);
                
            } catch (IOException e) {
                logger.severe("Failed to create database backup: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === BATCH OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> batchSavePlayerData(List<PlayerData> playerDataList) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (PlayerData playerData : playerDataList) {
                        stmt.setString(1, playerData.getPlayerId().toString());
                        stmt.setString(2, playerData.getPlayerName());
                        stmt.setInt(3, playerData.isOnDuty() ? 1 : 0);
                        stmt.setLong(4, playerData.getDutyStartTime());
                        stmt.setLong(5, playerData.getOffDutyTime());
                        stmt.setLong(6, playerData.getGraceDebtTime());
                        stmt.setString(7, playerData.getGuardRank());
                        stmt.setLong(8, playerData.getEarnedOffDutyTime());
                        stmt.setInt(9, playerData.hasEarnedBaseTime() ? 1 : 0);
                        stmt.setInt(10, playerData.hasBeenNotifiedOfExpiredTime() ? 1 : 0);
                        stmt.setInt(11, playerData.getSessionSearches());
                        stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                        stmt.setInt(13, playerData.getSessionArrests());
                        stmt.setInt(14, playerData.getSessionKills());
                        stmt.setInt(15, playerData.getSessionDetections());
                        stmt.setInt(16, playerData.getWantedLevel());
                        stmt.setLong(17, playerData.getWantedExpireTime());
                        stmt.setString(18, playerData.getWantedReason());
                        stmt.setInt(19, playerData.isBeingChased() ? 1 : 0);
                        stmt.setString(20, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                        stmt.setLong(21, playerData.getChaseStartTime());
                        stmt.setInt(22, playerData.getTotalArrests());
                        stmt.setInt(23, playerData.getTotalViolations());
                        stmt.setLong(24, playerData.getTotalDutyTime());
                        stmt.setLong(25, System.currentTimeMillis());
                        
                        stmt.addBatch();
                    }
                    
                    stmt.executeBatch();
                    connection.commit();
                }
                
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback batch save: " + rollbackEx.getMessage());
                }
                logger.severe("Failed to batch save player data: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.warning("Failed to reset auto-commit: " + e.getMessage());
                }
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> batchLoadPlayerData(List<UUID> playerIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> playerDataList = new ArrayList<>();
            
            if (playerIds.isEmpty()) {
                return playerDataList;
            }
            
            // Create placeholders for IN clause
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < playerIds.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            
            String sql = "SELECT * FROM player_data WHERE player_id IN (" + placeholders + ")";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < playerIds.size(); i++) {
                    stmt.setString(i + 1, playerIds.get(i).toString());
                }
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    playerDataList.add(mapResultSetToPlayerData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to batch load player data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return playerDataList;
        }, executor);
    }
=======
package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SQLiteHandler implements DatabaseHandler {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final String databasePath;
    private final ExecutorService executor;
    
    private Connection connection;
    private boolean initialized = false;
    
    // Database schema version for migrations
    private static final int SCHEMA_VERSION = 1;
    
    public SQLiteHandler(EdenCorrections plugin, String databasePath) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.databasePath = databasePath;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "EdenCorrections-DB-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() throws SQLException {
        try {
            // Ensure database directory exists
            File dbFile = new File(plugin.getDataFolder(), databasePath);
            File parentDir = dbFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new SQLException("Failed to create database directory: " + parentDir.getPath());
            }
            
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            // Configure connection
            connection.setAutoCommit(true);
            
            // Enable foreign keys and performance optimizations
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = 10000");
                stmt.execute("PRAGMA temp_store = MEMORY");
            }
            
            // Create tables
            createTables();
            
            // Check and perform migrations if needed
            checkAndMigrate();
            
            initialized = true;
            logger.info("SQLite database initialized successfully at: " + dbFile.getPath());
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            throw e;
        }
    }
    
    private void createTables() throws SQLException {
        String[] tableCreationSql = {
            // Player data table
            """
            CREATE TABLE IF NOT EXISTS player_data (
                player_id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                is_on_duty INTEGER NOT NULL DEFAULT 0,
                duty_start_time INTEGER NOT NULL DEFAULT 0,
                off_duty_time INTEGER NOT NULL DEFAULT 0,
                grace_debt_time INTEGER NOT NULL DEFAULT 0,
                guard_rank TEXT,
                earned_off_duty_time INTEGER NOT NULL DEFAULT 0,
                has_earned_base_time INTEGER NOT NULL DEFAULT 0,
                has_been_notified_expired INTEGER NOT NULL DEFAULT 0,
                session_searches INTEGER NOT NULL DEFAULT 0,
                session_successful_searches INTEGER NOT NULL DEFAULT 0,
                session_arrests INTEGER NOT NULL DEFAULT 0,
                session_kills INTEGER NOT NULL DEFAULT 0,
                session_detections INTEGER NOT NULL DEFAULT 0,
                wanted_level INTEGER NOT NULL DEFAULT 0,
                wanted_expire_time INTEGER NOT NULL DEFAULT 0,
                wanted_reason TEXT,
                being_chased INTEGER NOT NULL DEFAULT 0,
                chaser_guard TEXT,
                chase_start_time INTEGER NOT NULL DEFAULT 0,
                total_arrests INTEGER NOT NULL DEFAULT 0,
                total_violations INTEGER NOT NULL DEFAULT 0,
                total_duty_time INTEGER NOT NULL DEFAULT 0,
                last_updated INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Chase data table
            """
            CREATE TABLE IF NOT EXISTS chase_data (
                chase_id TEXT PRIMARY KEY,
                guard_id TEXT NOT NULL,
                target_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                duration INTEGER NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                end_reason TEXT,
                end_time INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Player inventory cache table
            """
            CREATE TABLE IF NOT EXISTS player_inventory_cache (
                player_id TEXT PRIMARY KEY,
                inventory_data TEXT NOT NULL,
                cached_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            )
            """,
            
            // Database metadata table
            """
            CREATE TABLE IF NOT EXISTS database_metadata (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """,
            
            // Performance statistics table
            """
            CREATE TABLE IF NOT EXISTS performance_stats (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id TEXT NOT NULL,
                stat_type TEXT NOT NULL,
                stat_value INTEGER NOT NULL,
                recorded_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            )
            """
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String sql : tableCreationSql) {
                stmt.execute(sql);
            }
        }
        
        // Create indexes for performance
        createIndexes();
    }
    
    private void createIndexes() throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_player_name ON player_data(player_name)",
            "CREATE INDEX IF NOT EXISTS idx_player_duty ON player_data(is_on_duty)",
            "CREATE INDEX IF NOT EXISTS idx_player_wanted ON player_data(wanted_level)",
            "CREATE INDEX IF NOT EXISTS idx_chase_guard ON chase_data(guard_id)",
            "CREATE INDEX IF NOT EXISTS idx_chase_target ON chase_data(target_id)",
            "CREATE INDEX IF NOT EXISTS idx_chase_active ON chase_data(is_active)",
            "CREATE INDEX IF NOT EXISTS idx_performance_player ON performance_stats(player_id)",
            "CREATE INDEX IF NOT EXISTS idx_performance_type ON performance_stats(stat_type)",
            "CREATE INDEX IF NOT EXISTS idx_performance_time ON performance_stats(recorded_at)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }
    
    private void checkAndMigrate() throws SQLException {
        // Check current schema version
        int currentVersion = getSchemaVersion();
        
        if (currentVersion < SCHEMA_VERSION) {
            logger.info("Database schema outdated. Performing migration from version " + 
                       currentVersion + " to " + SCHEMA_VERSION);
            performMigration(currentVersion);
            setSchemaVersion(SCHEMA_VERSION);
        }
    }
    
    private int getSchemaVersion() throws SQLException {
        String sql = "SELECT value FROM database_metadata WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "schema_version");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (SQLException e) {
            // Table might not exist yet
        }
        return 0;
    }
    
    private void setSchemaVersion(int version) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO database_metadata (key, value, updated_at) 
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "schema_version");
            stmt.setString(2, String.valueOf(version));
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    private void performMigration(int fromVersion) throws SQLException {
        // Future migrations would go here
        logger.info("No migrations needed from version " + fromVersion);
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            executor.shutdown();
            logger.info("SQLite database connection closed");
        } catch (SQLException e) {
            logger.warning("Error closing SQLite database: " + e.getMessage());
        }
        initialized = false;
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && initialized;
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!isConnected()) return false;
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
                return true;
            }
        } catch (SQLException e) {
            logger.warning("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    // === PLAYER DATA OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getPlayerId().toString());
                stmt.setString(2, playerData.getPlayerName());
                stmt.setInt(3, playerData.isOnDuty() ? 1 : 0);
                stmt.setLong(4, playerData.getDutyStartTime());
                stmt.setLong(5, playerData.getOffDutyTime());
                stmt.setLong(6, playerData.getGraceDebtTime());
                stmt.setString(7, playerData.getGuardRank());
                stmt.setLong(8, playerData.getEarnedOffDutyTime());
                stmt.setInt(9, playerData.hasEarnedBaseTime() ? 1 : 0);
                stmt.setInt(10, playerData.hasBeenNotifiedOfExpiredTime() ? 1 : 0);
                stmt.setInt(11, playerData.getSessionSearches());
                stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                stmt.setInt(13, playerData.getSessionArrests());
                stmt.setInt(14, playerData.getSessionKills());
                stmt.setInt(15, playerData.getSessionDetections());
                stmt.setInt(16, playerData.getWantedLevel());
                stmt.setLong(17, playerData.getWantedExpireTime());
                stmt.setString(18, playerData.getWantedReason());
                stmt.setInt(19, playerData.isBeingChased() ? 1 : 0);
                stmt.setString(20, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                stmt.setLong(21, playerData.getChaseStartTime());
                stmt.setInt(22, playerData.getTotalArrests());
                stmt.setInt(23, playerData.getTotalViolations());
                stmt.setLong(24, playerData.getTotalDutyTime());
                stmt.setLong(25, System.currentTimeMillis());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to save player data for " + playerData.getPlayerName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return mapResultSetToPlayerData(rs);
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load player data for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<PlayerData> loadPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE player_name = ? COLLATE NOCASE";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return mapResultSetToPlayerData(rs);
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load player data for " + playerName + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        UUID playerId = UUID.fromString(rs.getString("player_id"));
        String playerName = rs.getString("player_name");
        
        PlayerData data = new PlayerData(playerId, playerName);
        
        // Set duty information
        data.setOnDuty(rs.getInt("is_on_duty") == 1);
        data.setDutyStartTime(rs.getLong("duty_start_time"));
        data.setOffDutyTime(rs.getLong("off_duty_time"));
        data.setGraceDebtTime(rs.getLong("grace_debt_time"));
        data.setGuardRank(rs.getString("guard_rank"));
        
        // Set off-duty earning information
        data.setEarnedOffDutyTime(rs.getLong("earned_off_duty_time"));
        data.setHasEarnedBaseTime(rs.getInt("has_earned_base_time") == 1);
        data.setHasBeenNotifiedOfExpiredTime(rs.getInt("has_been_notified_expired") == 1);
        
        // Set session performance
        data.setSessionSearches(rs.getInt("session_searches"));
        data.setSessionSuccessfulSearches(rs.getInt("session_successful_searches"));
        data.setSessionArrests(rs.getInt("session_arrests"));
        data.setSessionKills(rs.getInt("session_kills"));
        data.setSessionDetections(rs.getInt("session_detections"));
        
        // Set wanted information
        data.setWantedLevel(rs.getInt("wanted_level"));
        data.setWantedExpireTime(rs.getLong("wanted_expire_time"));
        data.setWantedReason(rs.getString("wanted_reason"));
        
        // Set chase information
        data.setBeingChased(rs.getInt("being_chased") == 1);
        String chaserGuardStr = rs.getString("chaser_guard");
        if (chaserGuardStr != null) {
            data.setChaserGuard(UUID.fromString(chaserGuardStr));
        }
        data.setChaseStartTime(rs.getLong("chase_start_time"));
        
        // Set statistics
        data.setTotalArrests(rs.getInt("total_arrests"));
        data.setTotalViolations(rs.getInt("total_violations"));
        data.setTotalDutyTime(rs.getLong("total_duty_time"));
        
        return data;
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> playerDataList = new ArrayList<>();
            String sql = "SELECT * FROM player_data ORDER BY player_name";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    playerDataList.add(mapResultSetToPlayerData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to load all player data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return playerDataList;
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_data WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete player data for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === CHASE DATA OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> saveChaseData(ChaseData chaseData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO chase_data (
                    chase_id, guard_id, target_id, start_time, duration, is_active, 
                    end_reason, end_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseData.getChaseId().toString());
                stmt.setString(2, chaseData.getGuardId().toString());
                stmt.setString(3, chaseData.getTargetId().toString());
                stmt.setLong(4, chaseData.getStartTime());
                stmt.setLong(5, chaseData.getDuration());
                stmt.setInt(6, chaseData.isActive() ? 1 : 0);
                stmt.setString(7, chaseData.getEndReason());
                stmt.setLong(8, chaseData.getEndTime());
                stmt.setLong(9, System.currentTimeMillis());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to save chase data: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<ChaseData> loadChaseData(UUID chaseId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM chase_data WHERE chase_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return mapResultSetToChaseData(rs);
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load chase data for " + chaseId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private ChaseData mapResultSetToChaseData(ResultSet rs) throws SQLException {
        UUID chaseId = UUID.fromString(rs.getString("chase_id"));
        UUID guardId = UUID.fromString(rs.getString("guard_id"));
        UUID targetId = UUID.fromString(rs.getString("target_id"));
        long startTime = rs.getLong("start_time");
        long duration = rs.getLong("duration");
        
        ChaseData chaseData = new ChaseData(chaseId, guardId, targetId, duration);
        
        if (rs.getInt("is_active") == 0) {
            chaseData.endChase(rs.getString("end_reason"));
        }
        
        return chaseData;
    }
    
    @Override
    public CompletableFuture<List<ChaseData>> loadAllActiveChases() {
        return CompletableFuture.supplyAsync(() -> {
            List<ChaseData> chaseDataList = new ArrayList<>();
            String sql = "SELECT * FROM chase_data WHERE is_active = 1 ORDER BY start_time DESC";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    chaseDataList.add(mapResultSetToChaseData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to load active chase data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return chaseDataList;
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deleteChaseData(UUID chaseId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM chase_data WHERE chase_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chaseId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete chase data for " + chaseId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> cleanupExpiredChases() {
        return CompletableFuture.runAsync(() -> {
            // Delete chases older than 24 hours
            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            String sql = "DELETE FROM chase_data WHERE is_active = 0 AND end_time < ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, cutoffTime);
                int deleted = stmt.executeUpdate();
                
                if (deleted > 0) {
                    logger.info("Cleaned up " + deleted + " expired chase records");
                }
            } catch (SQLException e) {
                logger.severe("Failed to cleanup expired chases: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === INVENTORY OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> savePlayerInventory(UUID playerId, String inventoryData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_inventory_cache (player_id, inventory_data, cached_at) 
                VALUES (?, ?, ?)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, inventoryData);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to save player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<String> loadPlayerInventory(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT inventory_data FROM player_inventory_cache WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getString("inventory_data");
                }
                return null;
            } catch (SQLException e) {
                logger.severe("Failed to load player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerInventory(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_inventory_cache WHERE player_id = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Failed to delete player inventory for " + playerId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === MAINTENANCE AND STATISTICS ===
    
    @Override
    public CompletableFuture<Void> performMaintenance() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Clean up expired chases
                cleanupExpiredChases().get();
                
                // Clean up old inventory cache (older than 7 days)
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                String sql = "DELETE FROM player_inventory_cache WHERE cached_at < ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old inventory cache entries");
                    }
                }
                
                // Vacuum database for performance
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("VACUUM");
                }
                
                // Update maintenance timestamp
                setSchemaVersion(SCHEMA_VERSION);
                
                logger.info("Database maintenance completed successfully");
                
            } catch (Exception e) {
                logger.severe("Database maintenance failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<DatabaseStats> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalPlayers = 0;
                int activeChases = 0;
                int cachedInventories = 0;
                long databaseSize = 0;
                
                // Count total players
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_data");
                    if (rs.next()) {
                        totalPlayers = rs.getInt(1);
                    }
                }
                
                // Count active chases
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM chase_data WHERE is_active = 1");
                    if (rs.next()) {
                        activeChases = rs.getInt(1);
                    }
                }
                
                // Count cached inventories
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_inventory_cache");
                    if (rs.next()) {
                        cachedInventories = rs.getInt(1);
                    }
                }
                
                // Get database size
                File dbFile = new File(plugin.getDataFolder(), databasePath);
                if (dbFile.exists()) {
                    databaseSize = dbFile.length();
                }
                
                return new DatabaseStats(totalPlayers, activeChases, cachedInventories, 
                                       databaseSize, "SQLite", System.currentTimeMillis());
                
            } catch (SQLException e) {
                logger.severe("Failed to get database statistics: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> createBackup(String backupPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                File dbFile = new File(plugin.getDataFolder(), databasePath);
                Path backupFilePath = Paths.get(backupPath);
                
                // Ensure backup directory exists
                Files.createDirectories(backupFilePath.getParent());
                
                // Copy database file
                Files.copy(dbFile.toPath(), backupFilePath);
                
                logger.info("Database backup created at: " + backupPath);
                
            } catch (IOException e) {
                logger.severe("Failed to create database backup: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    // === BATCH OPERATIONS ===
    
    @Override
    public CompletableFuture<Void> batchSavePlayerData(List<PlayerData> playerDataList) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (PlayerData playerData : playerDataList) {
                        stmt.setString(1, playerData.getPlayerId().toString());
                        stmt.setString(2, playerData.getPlayerName());
                        stmt.setInt(3, playerData.isOnDuty() ? 1 : 0);
                        stmt.setLong(4, playerData.getDutyStartTime());
                        stmt.setLong(5, playerData.getOffDutyTime());
                        stmt.setLong(6, playerData.getGraceDebtTime());
                        stmt.setString(7, playerData.getGuardRank());
                        stmt.setLong(8, playerData.getEarnedOffDutyTime());
                        stmt.setInt(9, playerData.hasEarnedBaseTime() ? 1 : 0);
                        stmt.setInt(10, playerData.hasBeenNotifiedOfExpiredTime() ? 1 : 0);
                        stmt.setInt(11, playerData.getSessionSearches());
                        stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                        stmt.setInt(13, playerData.getSessionArrests());
                        stmt.setInt(14, playerData.getSessionKills());
                        stmt.setInt(15, playerData.getSessionDetections());
                        stmt.setInt(16, playerData.getWantedLevel());
                        stmt.setLong(17, playerData.getWantedExpireTime());
                        stmt.setString(18, playerData.getWantedReason());
                        stmt.setInt(19, playerData.isBeingChased() ? 1 : 0);
                        stmt.setString(20, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                        stmt.setLong(21, playerData.getChaseStartTime());
                        stmt.setInt(22, playerData.getTotalArrests());
                        stmt.setInt(23, playerData.getTotalViolations());
                        stmt.setLong(24, playerData.getTotalDutyTime());
                        stmt.setLong(25, System.currentTimeMillis());
                        
                        stmt.addBatch();
                    }
                    
                    stmt.executeBatch();
                    connection.commit();
                }
                
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback batch save: " + rollbackEx.getMessage());
                }
                logger.severe("Failed to batch save player data: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.warning("Failed to reset auto-commit: " + e.getMessage());
                }
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> batchLoadPlayerData(List<UUID> playerIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> playerDataList = new ArrayList<>();
            
            if (playerIds.isEmpty()) {
                return playerDataList;
            }
            
            // Create placeholders for IN clause
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < playerIds.size(); i++) {
                if (i > 0) placeholders.append(", ");
                placeholders.append("?");
            }
            
            String sql = "SELECT * FROM player_data WHERE player_id IN (" + placeholders + ")";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < playerIds.size(); i++) {
                    stmt.setString(i + 1, playerIds.get(i).toString());
                }
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    playerDataList.add(mapResultSetToPlayerData(rs));
                }
            } catch (SQLException e) {
                logger.severe("Failed to batch load player data: " + e.getMessage());
                throw new RuntimeException(e);
            }
            
            return playerDataList;
        }, executor);
    }
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
} 