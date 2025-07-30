<<<<<<< HEAD
package dev.lsdmc.edenCorrections.storage;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class MySQLHandler implements DatabaseHandler {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final ExecutorService executor;
    
    private HikariDataSource dataSource;
    private boolean initialized = false;
    
    // Database configuration
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    // Database schema version for migrations
    private static final int SCHEMA_VERSION = 1;
    
    public MySQLHandler(EdenCorrections plugin, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "EdenCorrections-MySQL-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() throws SQLException {
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8");
            config.setUsername(username);
            config.setPassword(password);
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // Performance settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException("Database connection validation failed");
                }
            }
            
            // Create tables
            createTables();
            
            // Check and perform migrations if needed
            checkAndMigrate();
            
            initialized = true;
            logger.info("MySQL database initialized successfully at: " + host + ":" + port + "/" + database);
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        } catch (SQLException e) {
            logger.severe("Failed to initialize MySQL database: " + e.getMessage());
            throw e;
        }
    }
    
    private void createTables() throws SQLException {
        String[] tableCreationSql = {
            // Player data table
            """
            CREATE TABLE IF NOT EXISTS player_data (
                player_id VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                is_on_duty BOOLEAN NOT NULL DEFAULT FALSE,
                duty_start_time BIGINT NOT NULL DEFAULT 0,
                off_duty_time BIGINT NOT NULL DEFAULT 0,
                grace_debt_time BIGINT NOT NULL DEFAULT 0,
                guard_rank VARCHAR(32),
                earned_off_duty_time BIGINT NOT NULL DEFAULT 0,
                has_earned_base_time BOOLEAN NOT NULL DEFAULT FALSE,
                has_been_notified_expired BOOLEAN NOT NULL DEFAULT FALSE,
                session_searches INT NOT NULL DEFAULT 0,
                session_successful_searches INT NOT NULL DEFAULT 0,
                session_arrests INT NOT NULL DEFAULT 0,
                session_kills INT NOT NULL DEFAULT 0,
                session_detections INT NOT NULL DEFAULT 0,
                penalty_start_time BIGINT NOT NULL DEFAULT 0,
                current_penalty_stage INT NOT NULL DEFAULT 0,
                last_penalty_time BIGINT NOT NULL DEFAULT 0,
                last_slowness_application BIGINT NOT NULL DEFAULT 0,
                has_active_penalty_boss_bar BOOLEAN NOT NULL DEFAULT FALSE,
                wanted_level INT NOT NULL DEFAULT 0,
                wanted_expire_time BIGINT NOT NULL DEFAULT 0,
                wanted_reason TEXT,
                being_chased BOOLEAN NOT NULL DEFAULT FALSE,
                chaser_guard VARCHAR(36),
                chase_start_time BIGINT NOT NULL DEFAULT 0,
                total_arrests INT NOT NULL DEFAULT 0,
                total_violations INT NOT NULL DEFAULT 0,
                total_duty_time BIGINT NOT NULL DEFAULT 0,
                last_updated BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_player_name (player_name),
                INDEX idx_player_duty (is_on_duty),
                INDEX idx_player_wanted (wanted_level),
                INDEX idx_last_updated (last_updated)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Chase data table
            """
            CREATE TABLE IF NOT EXISTS chase_data (
                chase_id VARCHAR(36) PRIMARY KEY,
                guard_id VARCHAR(36) NOT NULL,
                target_id VARCHAR(36) NOT NULL,
                start_time BIGINT NOT NULL,
                duration BIGINT NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                end_reason TEXT,
                end_time BIGINT NOT NULL DEFAULT 0,
                created_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_chase_guard (guard_id),
                INDEX idx_chase_target (target_id),
                INDEX idx_chase_active (is_active),
                INDEX idx_chase_cleanup (is_active, end_time),
                INDEX idx_chase_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Player inventory cache table
            """
            CREATE TABLE IF NOT EXISTS player_inventory_cache (
                player_id VARCHAR(36) PRIMARY KEY,
                inventory_data LONGTEXT NOT NULL,
                cached_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_inventory_cached (cached_at),
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Database metadata table
            """
            CREATE TABLE IF NOT EXISTS database_metadata (
                meta_key VARCHAR(64) PRIMARY KEY,
                meta_value TEXT NOT NULL,
                updated_at BIGINT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Performance statistics table
            """
            CREATE TABLE IF NOT EXISTS performance_stats (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_id VARCHAR(36) NOT NULL,
                stat_type VARCHAR(32) NOT NULL,
                stat_value BIGINT NOT NULL,
                recorded_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_performance_player (player_id),
                INDEX idx_performance_type (stat_type),
                INDEX idx_performance_time (recorded_at),
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };
        
        try (Connection connection = dataSource.getConnection()) {
            for (String sql : tableCreationSql) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
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
        String sql = "SELECT meta_value FROM database_metadata WHERE meta_key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, "schema_version");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("meta_value"));
            }
        } catch (SQLException e) {
            // Table might not exist yet
        }
        return 0;
    }
    
    private void setSchemaVersion(int version) throws SQLException {
        String sql = """
            INSERT INTO database_metadata (meta_key, meta_value, updated_at) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value), updated_at = VALUES(updated_at)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
        logger.info("MySQL database connection pool closed");
        initialized = false;
    }
    
    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed() && initialized;
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!isConnected()) return false;
            
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(5);
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
                INSERT INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, penalty_start_time,
                    current_penalty_stage, last_penalty_time, last_slowness_application,
                    has_active_penalty_boss_bar, wanted_level, wanted_expire_time, wanted_reason, 
                    being_chased, chaser_guard, chase_start_time, total_arrests, total_violations, 
                    total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    is_on_duty = VALUES(is_on_duty),
                    duty_start_time = VALUES(duty_start_time),
                    off_duty_time = VALUES(off_duty_time),
                    grace_debt_time = VALUES(grace_debt_time),
                    guard_rank = VALUES(guard_rank),
                    earned_off_duty_time = VALUES(earned_off_duty_time),
                    has_earned_base_time = VALUES(has_earned_base_time),
                    has_been_notified_expired = VALUES(has_been_notified_expired),
                    session_searches = VALUES(session_searches),
                    session_successful_searches = VALUES(session_successful_searches),
                    session_arrests = VALUES(session_arrests),
                    session_kills = VALUES(session_kills),
                    session_detections = VALUES(session_detections),
                    penalty_start_time = VALUES(penalty_start_time),
                    current_penalty_stage = VALUES(current_penalty_stage),
                    last_penalty_time = VALUES(last_penalty_time),
                    last_slowness_application = VALUES(last_slowness_application),
                    has_active_penalty_boss_bar = VALUES(has_active_penalty_boss_bar),
                    wanted_level = VALUES(wanted_level),
                    wanted_expire_time = VALUES(wanted_expire_time),
                    wanted_reason = VALUES(wanted_reason),
                    being_chased = VALUES(being_chased),
                    chaser_guard = VALUES(chaser_guard),
                    chase_start_time = VALUES(chase_start_time),
                    total_arrests = VALUES(total_arrests),
                    total_violations = VALUES(total_violations),
                    total_duty_time = VALUES(total_duty_time),
                    last_updated = VALUES(last_updated)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerData.getPlayerId().toString());
                stmt.setString(2, playerData.getPlayerName());
                stmt.setBoolean(3, playerData.isOnDuty());
                stmt.setLong(4, playerData.getDutyStartTime());
                stmt.setLong(5, playerData.getOffDutyTime());
                stmt.setLong(6, playerData.getGraceDebtTime());
                stmt.setString(7, playerData.getGuardRank());
                stmt.setLong(8, playerData.getEarnedOffDutyTime());
                stmt.setBoolean(9, playerData.hasEarnedBaseTime());
                stmt.setBoolean(10, playerData.hasBeenNotifiedOfExpiredTime());
                stmt.setInt(11, playerData.getSessionSearches());
                stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                stmt.setInt(13, playerData.getSessionArrests());
                stmt.setInt(14, playerData.getSessionKills());
                stmt.setInt(15, playerData.getSessionDetections());
                stmt.setLong(16, playerData.getPenaltyStartTime());
                stmt.setInt(17, playerData.getCurrentPenaltyStage());
                stmt.setLong(18, playerData.getLastPenaltyTime());
                stmt.setLong(19, playerData.getLastSlownessApplication());
                stmt.setBoolean(20, playerData.hasActivePenaltyBossBar());
                stmt.setInt(21, playerData.getWantedLevel());
                stmt.setLong(22, playerData.getWantedExpireTime());
                stmt.setString(23, playerData.getWantedReason());
                stmt.setBoolean(24, playerData.isBeingChased());
                stmt.setString(25, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                stmt.setLong(26, playerData.getChaseStartTime());
                stmt.setInt(27, playerData.getTotalArrests());
                stmt.setInt(28, playerData.getTotalViolations());
                stmt.setLong(29, playerData.getTotalDutyTime());
                stmt.setLong(30, System.currentTimeMillis());
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            String sql = "SELECT * FROM player_data WHERE player_name = ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
        data.setOnDuty(rs.getBoolean("is_on_duty"));
        data.setDutyStartTime(rs.getLong("duty_start_time"));
        data.setOffDutyTime(rs.getLong("off_duty_time"));
        data.setGraceDebtTime(rs.getLong("grace_debt_time"));
        data.setGuardRank(rs.getString("guard_rank"));
        
        // Set off-duty earning information
        data.setEarnedOffDutyTime(rs.getLong("earned_off_duty_time"));
        data.setHasEarnedBaseTime(rs.getBoolean("has_earned_base_time"));
        data.setHasBeenNotifiedOfExpiredTime(rs.getBoolean("has_been_notified_expired"));
        
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
        data.setHasActivePenaltyBossBar(rs.getBoolean("has_active_penalty_boss_bar"));
        
        // Set wanted information
        data.setWantedLevel(rs.getInt("wanted_level"));
        data.setWantedExpireTime(rs.getLong("wanted_expire_time"));
        data.setWantedReason(rs.getString("wanted_reason"));
        
        // Set chase information
        data.setBeingChased(rs.getBoolean("being_chased"));
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
                INSERT INTO chase_data (
                    chase_id, guard_id, target_id, start_time, duration, is_active, 
                    end_reason, end_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    guard_id = VALUES(guard_id),
                    target_id = VALUES(target_id),
                    start_time = VALUES(start_time),
                    duration = VALUES(duration),
                    is_active = VALUES(is_active),
                    end_reason = VALUES(end_reason),
                    end_time = VALUES(end_time)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, chaseData.getChaseId().toString());
                stmt.setString(2, chaseData.getGuardId().toString());
                stmt.setString(3, chaseData.getTargetId().toString());
                stmt.setLong(4, chaseData.getStartTime());
                stmt.setLong(5, chaseData.getDuration());
                stmt.setBoolean(6, chaseData.isActive());
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
        
        if (!rs.getBoolean("is_active")) {
            chaseData.endChase(rs.getString("end_reason"));
        }
        
        return chaseData;
    }
    
    @Override
    public CompletableFuture<List<ChaseData>> loadAllActiveChases() {
        return CompletableFuture.supplyAsync(() -> {
            List<ChaseData> chaseDataList = new ArrayList<>();
            String sql = "SELECT * FROM chase_data WHERE is_active = TRUE ORDER BY start_time DESC";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            String sql = "DELETE FROM chase_data WHERE is_active = FALSE AND end_time < ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
                INSERT INTO player_inventory_cache (player_id, inventory_data, cached_at) 
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                    inventory_data = VALUES(inventory_data), 
                    cached_at = VALUES(cached_at)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement();
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
                
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql)) {
                    
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old inventory cache entries");
                    }
                }
                
                // Clean up old performance stats (older than 30 days)
                cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000);
                sql = "DELETE FROM performance_stats WHERE recorded_at < ?";
                
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql)) {
                    
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old performance statistics");
                    }
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
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_data");
                    if (rs.next()) {
                        totalPlayers = rs.getInt(1);
                    }
                }
                
                // Count active chases
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM chase_data WHERE is_active = TRUE");
                    if (rs.next()) {
                        activeChases = rs.getInt(1);
                    }
                }
                
                // Count cached inventories
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_inventory_cache");
                    if (rs.next()) {
                        cachedInventories = rs.getInt(1);
                    }
                }
                
                // Get database size (approximate)
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT SUM(data_length + index_length) FROM information_schema.tables WHERE table_schema = '" + database + "'");
                    if (rs.next()) {
                        databaseSize = rs.getLong(1);
                    }
                }
                
                return new DatabaseStats(totalPlayers, activeChases, cachedInventories, 
                                       databaseSize, "MySQL", System.currentTimeMillis());
                
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
                // MySQL backup would typically use mysqldump
                // For now, we'll just log that backup is not implemented for MySQL
                logger.warning("MySQL backup not implemented. Use mysqldump manually: " +
                             "mysqldump -u " + username + " -p " + database + " > " + backupPath);
                
            } catch (Exception e) {
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
                INSERT INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    is_on_duty = VALUES(is_on_duty),
                    duty_start_time = VALUES(duty_start_time),
                    off_duty_time = VALUES(off_duty_time),
                    grace_debt_time = VALUES(grace_debt_time),
                    guard_rank = VALUES(guard_rank),
                    earned_off_duty_time = VALUES(earned_off_duty_time),
                    has_earned_base_time = VALUES(has_earned_base_time),
                    has_been_notified_expired = VALUES(has_been_notified_expired),
                    session_searches = VALUES(session_searches),
                    session_successful_searches = VALUES(session_successful_searches),
                    session_arrests = VALUES(session_arrests),
                    session_kills = VALUES(session_kills),
                    session_detections = VALUES(session_detections),
                    wanted_level = VALUES(wanted_level),
                    wanted_expire_time = VALUES(wanted_expire_time),
                    wanted_reason = VALUES(wanted_reason),
                    being_chased = VALUES(being_chased),
                    chaser_guard = VALUES(chaser_guard),
                    chase_start_time = VALUES(chase_start_time),
                    total_arrests = VALUES(total_arrests),
                    total_violations = VALUES(total_violations),
                    total_duty_time = VALUES(total_duty_time),
                    last_updated = VALUES(last_updated)
                """;
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (PlayerData playerData : playerDataList) {
                        stmt.setString(1, playerData.getPlayerId().toString());
                        stmt.setString(2, playerData.getPlayerName());
                        stmt.setBoolean(3, playerData.isOnDuty());
                        stmt.setLong(4, playerData.getDutyStartTime());
                        stmt.setLong(5, playerData.getOffDutyTime());
                        stmt.setLong(6, playerData.getGraceDebtTime());
                        stmt.setString(7, playerData.getGuardRank());
                        stmt.setLong(8, playerData.getEarnedOffDutyTime());
                        stmt.setBoolean(9, playerData.hasEarnedBaseTime());
                        stmt.setBoolean(10, playerData.hasBeenNotifiedOfExpiredTime());
                        stmt.setInt(11, playerData.getSessionSearches());
                        stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                        stmt.setInt(13, playerData.getSessionArrests());
                        stmt.setInt(14, playerData.getSessionKills());
                        stmt.setInt(15, playerData.getSessionDetections());
                        stmt.setInt(16, playerData.getWantedLevel());
                        stmt.setLong(17, playerData.getWantedExpireTime());
                        stmt.setString(18, playerData.getWantedReason());
                        stmt.setBoolean(19, playerData.isBeingChased());
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
                try (Connection connection = dataSource.getConnection()) {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback batch save: " + rollbackEx.getMessage());
                }
                logger.severe("Failed to batch save player data: " + e.getMessage());
                throw new RuntimeException(e);
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class MySQLHandler implements DatabaseHandler {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final ExecutorService executor;
    
    private HikariDataSource dataSource;
    private boolean initialized = false;
    
    // Database configuration
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    // Database schema version for migrations
    private static final int SCHEMA_VERSION = 1;
    
    public MySQLHandler(EdenCorrections plugin, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "EdenCorrections-MySQL-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() throws SQLException {
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8");
            config.setUsername(username);
            config.setPassword(password);
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // Performance settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException("Database connection validation failed");
                }
            }
            
            // Create tables
            createTables();
            
            // Check and perform migrations if needed
            checkAndMigrate();
            
            initialized = true;
            logger.info("MySQL database initialized successfully at: " + host + ":" + port + "/" + database);
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        } catch (SQLException e) {
            logger.severe("Failed to initialize MySQL database: " + e.getMessage());
            throw e;
        }
    }
    
    private void createTables() throws SQLException {
        String[] tableCreationSql = {
            // Player data table
            """
            CREATE TABLE IF NOT EXISTS player_data (
                player_id VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                is_on_duty BOOLEAN NOT NULL DEFAULT FALSE,
                duty_start_time BIGINT NOT NULL DEFAULT 0,
                off_duty_time BIGINT NOT NULL DEFAULT 0,
                grace_debt_time BIGINT NOT NULL DEFAULT 0,
                guard_rank VARCHAR(32),
                earned_off_duty_time BIGINT NOT NULL DEFAULT 0,
                has_earned_base_time BOOLEAN NOT NULL DEFAULT FALSE,
                has_been_notified_expired BOOLEAN NOT NULL DEFAULT FALSE,
                session_searches INT NOT NULL DEFAULT 0,
                session_successful_searches INT NOT NULL DEFAULT 0,
                session_arrests INT NOT NULL DEFAULT 0,
                session_kills INT NOT NULL DEFAULT 0,
                session_detections INT NOT NULL DEFAULT 0,
                wanted_level INT NOT NULL DEFAULT 0,
                wanted_expire_time BIGINT NOT NULL DEFAULT 0,
                wanted_reason TEXT,
                being_chased BOOLEAN NOT NULL DEFAULT FALSE,
                chaser_guard VARCHAR(36),
                chase_start_time BIGINT NOT NULL DEFAULT 0,
                total_arrests INT NOT NULL DEFAULT 0,
                total_violations INT NOT NULL DEFAULT 0,
                total_duty_time BIGINT NOT NULL DEFAULT 0,
                last_updated BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_player_name (player_name),
                INDEX idx_player_duty (is_on_duty),
                INDEX idx_player_wanted (wanted_level),
                INDEX idx_last_updated (last_updated)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Chase data table
            """
            CREATE TABLE IF NOT EXISTS chase_data (
                chase_id VARCHAR(36) PRIMARY KEY,
                guard_id VARCHAR(36) NOT NULL,
                target_id VARCHAR(36) NOT NULL,
                start_time BIGINT NOT NULL,
                duration BIGINT NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                end_reason TEXT,
                end_time BIGINT NOT NULL DEFAULT 0,
                created_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_chase_guard (guard_id),
                INDEX idx_chase_target (target_id),
                INDEX idx_chase_active (is_active),
                INDEX idx_chase_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Player inventory cache table
            """
            CREATE TABLE IF NOT EXISTS player_inventory_cache (
                player_id VARCHAR(36) PRIMARY KEY,
                inventory_data LONGTEXT NOT NULL,
                cached_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_inventory_cached (cached_at),
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Database metadata table
            """
            CREATE TABLE IF NOT EXISTS database_metadata (
                meta_key VARCHAR(64) PRIMARY KEY,
                meta_value TEXT NOT NULL,
                updated_at BIGINT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            
            // Performance statistics table
            """
            CREATE TABLE IF NOT EXISTS performance_stats (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_id VARCHAR(36) NOT NULL,
                stat_type VARCHAR(32) NOT NULL,
                stat_value BIGINT NOT NULL,
                recorded_at BIGINT NOT NULL DEFAULT 0,
                
                INDEX idx_performance_player (player_id),
                INDEX idx_performance_type (stat_type),
                INDEX idx_performance_time (recorded_at),
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };
        
        try (Connection connection = dataSource.getConnection()) {
            for (String sql : tableCreationSql) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
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
        String sql = "SELECT meta_value FROM database_metadata WHERE meta_key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, "schema_version");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("meta_value"));
            }
        } catch (SQLException e) {
            // Table might not exist yet
        }
        return 0;
    }
    
    private void setSchemaVersion(int version) throws SQLException {
        String sql = """
            INSERT INTO database_metadata (meta_key, meta_value, updated_at) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value), updated_at = VALUES(updated_at)
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
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
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
        logger.info("MySQL database connection pool closed");
        initialized = false;
    }
    
    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed() && initialized;
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!isConnected()) return false;
            
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(5);
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
                INSERT INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    is_on_duty = VALUES(is_on_duty),
                    duty_start_time = VALUES(duty_start_time),
                    off_duty_time = VALUES(off_duty_time),
                    grace_debt_time = VALUES(grace_debt_time),
                    guard_rank = VALUES(guard_rank),
                    earned_off_duty_time = VALUES(earned_off_duty_time),
                    has_earned_base_time = VALUES(has_earned_base_time),
                    has_been_notified_expired = VALUES(has_been_notified_expired),
                    session_searches = VALUES(session_searches),
                    session_successful_searches = VALUES(session_successful_searches),
                    session_arrests = VALUES(session_arrests),
                    session_kills = VALUES(session_kills),
                    session_detections = VALUES(session_detections),
                    wanted_level = VALUES(wanted_level),
                    wanted_expire_time = VALUES(wanted_expire_time),
                    wanted_reason = VALUES(wanted_reason),
                    being_chased = VALUES(being_chased),
                    chaser_guard = VALUES(chaser_guard),
                    chase_start_time = VALUES(chase_start_time),
                    total_arrests = VALUES(total_arrests),
                    total_violations = VALUES(total_violations),
                    total_duty_time = VALUES(total_duty_time),
                    last_updated = VALUES(last_updated)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerData.getPlayerId().toString());
                stmt.setString(2, playerData.getPlayerName());
                stmt.setBoolean(3, playerData.isOnDuty());
                stmt.setLong(4, playerData.getDutyStartTime());
                stmt.setLong(5, playerData.getOffDutyTime());
                stmt.setLong(6, playerData.getGraceDebtTime());
                stmt.setString(7, playerData.getGuardRank());
                stmt.setLong(8, playerData.getEarnedOffDutyTime());
                stmt.setBoolean(9, playerData.hasEarnedBaseTime());
                stmt.setBoolean(10, playerData.hasBeenNotifiedOfExpiredTime());
                stmt.setInt(11, playerData.getSessionSearches());
                stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                stmt.setInt(13, playerData.getSessionArrests());
                stmt.setInt(14, playerData.getSessionKills());
                stmt.setInt(15, playerData.getSessionDetections());
                stmt.setInt(16, playerData.getWantedLevel());
                stmt.setLong(17, playerData.getWantedExpireTime());
                stmt.setString(18, playerData.getWantedReason());
                stmt.setBoolean(19, playerData.isBeingChased());
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            String sql = "SELECT * FROM player_data WHERE player_name = ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
        data.setOnDuty(rs.getBoolean("is_on_duty"));
        data.setDutyStartTime(rs.getLong("duty_start_time"));
        data.setOffDutyTime(rs.getLong("off_duty_time"));
        data.setGraceDebtTime(rs.getLong("grace_debt_time"));
        data.setGuardRank(rs.getString("guard_rank"));
        
        // Set off-duty earning information
        data.setEarnedOffDutyTime(rs.getLong("earned_off_duty_time"));
        data.setHasEarnedBaseTime(rs.getBoolean("has_earned_base_time"));
        data.setHasBeenNotifiedOfExpiredTime(rs.getBoolean("has_been_notified_expired"));
        
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
        data.setBeingChased(rs.getBoolean("being_chased"));
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
                INSERT INTO chase_data (
                    chase_id, guard_id, target_id, start_time, duration, is_active, 
                    end_reason, end_time, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    guard_id = VALUES(guard_id),
                    target_id = VALUES(target_id),
                    start_time = VALUES(start_time),
                    duration = VALUES(duration),
                    is_active = VALUES(is_active),
                    end_reason = VALUES(end_reason),
                    end_time = VALUES(end_time)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, chaseData.getChaseId().toString());
                stmt.setString(2, chaseData.getGuardId().toString());
                stmt.setString(3, chaseData.getTargetId().toString());
                stmt.setLong(4, chaseData.getStartTime());
                stmt.setLong(5, chaseData.getDuration());
                stmt.setBoolean(6, chaseData.isActive());
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
        
        if (!rs.getBoolean("is_active")) {
            chaseData.endChase(rs.getString("end_reason"));
        }
        
        return chaseData;
    }
    
    @Override
    public CompletableFuture<List<ChaseData>> loadAllActiveChases() {
        return CompletableFuture.supplyAsync(() -> {
            List<ChaseData> chaseDataList = new ArrayList<>();
            String sql = "SELECT * FROM chase_data WHERE is_active = TRUE ORDER BY start_time DESC";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            String sql = "DELETE FROM chase_data WHERE is_active = FALSE AND end_time < ?";
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
                INSERT INTO player_inventory_cache (player_id, inventory_data, cached_at) 
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                    inventory_data = VALUES(inventory_data), 
                    cached_at = VALUES(cached_at)
                """;
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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
                
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql)) {
                    
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old inventory cache entries");
                    }
                }
                
                // Clean up old performance stats (older than 30 days)
                cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000);
                sql = "DELETE FROM performance_stats WHERE recorded_at < ?";
                
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql)) {
                    
                    stmt.setLong(1, cutoffTime);
                    int deleted = stmt.executeUpdate();
                    
                    if (deleted > 0) {
                        logger.info("Cleaned up " + deleted + " old performance statistics");
                    }
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
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_data");
                    if (rs.next()) {
                        totalPlayers = rs.getInt(1);
                    }
                }
                
                // Count active chases
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM chase_data WHERE is_active = TRUE");
                    if (rs.next()) {
                        activeChases = rs.getInt(1);
                    }
                }
                
                // Count cached inventories
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_inventory_cache");
                    if (rs.next()) {
                        cachedInventories = rs.getInt(1);
                    }
                }
                
                // Get database size (approximate)
                try (Connection connection = dataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT SUM(data_length + index_length) FROM information_schema.tables WHERE table_schema = '" + database + "'");
                    if (rs.next()) {
                        databaseSize = rs.getLong(1);
                    }
                }
                
                return new DatabaseStats(totalPlayers, activeChases, cachedInventories, 
                                       databaseSize, "MySQL", System.currentTimeMillis());
                
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
                // MySQL backup would typically use mysqldump
                // For now, we'll just log that backup is not implemented for MySQL
                logger.warning("MySQL backup not implemented. Use mysqldump manually: " +
                             "mysqldump -u " + username + " -p " + database + " > " + backupPath);
                
            } catch (Exception e) {
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
                INSERT INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, wanted_level, 
                    wanted_expire_time, wanted_reason, being_chased, chaser_guard, 
                    chase_start_time, total_arrests, total_violations, total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    is_on_duty = VALUES(is_on_duty),
                    duty_start_time = VALUES(duty_start_time),
                    off_duty_time = VALUES(off_duty_time),
                    grace_debt_time = VALUES(grace_debt_time),
                    guard_rank = VALUES(guard_rank),
                    earned_off_duty_time = VALUES(earned_off_duty_time),
                    has_earned_base_time = VALUES(has_earned_base_time),
                    has_been_notified_expired = VALUES(has_been_notified_expired),
                    session_searches = VALUES(session_searches),
                    session_successful_searches = VALUES(session_successful_searches),
                    session_arrests = VALUES(session_arrests),
                    session_kills = VALUES(session_kills),
                    session_detections = VALUES(session_detections),
                    wanted_level = VALUES(wanted_level),
                    wanted_expire_time = VALUES(wanted_expire_time),
                    wanted_reason = VALUES(wanted_reason),
                    being_chased = VALUES(being_chased),
                    chaser_guard = VALUES(chaser_guard),
                    chase_start_time = VALUES(chase_start_time),
                    total_arrests = VALUES(total_arrests),
                    total_violations = VALUES(total_violations),
                    total_duty_time = VALUES(total_duty_time),
                    last_updated = VALUES(last_updated)
                """;
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    for (PlayerData playerData : playerDataList) {
                        stmt.setString(1, playerData.getPlayerId().toString());
                        stmt.setString(2, playerData.getPlayerName());
                        stmt.setBoolean(3, playerData.isOnDuty());
                        stmt.setLong(4, playerData.getDutyStartTime());
                        stmt.setLong(5, playerData.getOffDutyTime());
                        stmt.setLong(6, playerData.getGraceDebtTime());
                        stmt.setString(7, playerData.getGuardRank());
                        stmt.setLong(8, playerData.getEarnedOffDutyTime());
                        stmt.setBoolean(9, playerData.hasEarnedBaseTime());
                        stmt.setBoolean(10, playerData.hasBeenNotifiedOfExpiredTime());
                        stmt.setInt(11, playerData.getSessionSearches());
                        stmt.setInt(12, playerData.getSessionSuccessfulSearches());
                        stmt.setInt(13, playerData.getSessionArrests());
                        stmt.setInt(14, playerData.getSessionKills());
                        stmt.setInt(15, playerData.getSessionDetections());
                        stmt.setInt(16, playerData.getWantedLevel());
                        stmt.setLong(17, playerData.getWantedExpireTime());
                        stmt.setString(18, playerData.getWantedReason());
                        stmt.setBoolean(19, playerData.isBeingChased());
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
                try (Connection connection = dataSource.getConnection()) {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.severe("Failed to rollback batch save: " + rollbackEx.getMessage());
                }
                logger.severe("Failed to batch save player data: " + e.getMessage());
                throw new RuntimeException(e);
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
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
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