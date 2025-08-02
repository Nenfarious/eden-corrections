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
        this.executor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "EdenCorrections-MySQL-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() throws SQLException {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Successfully connected to MySQL database");
            }
            
            // Create tables
            createTables();
            
            // Check and perform migrations if needed
            checkAndMigrate();
            
            initialized = true;
            logger.info("MySQL database handler initialized successfully");
            
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
                accumulated_penalty_time BIGINT NOT NULL DEFAULT 0,
                last_online_time BIGINT NOT NULL DEFAULT 0,
                penalty_tracking_paused BOOLEAN NOT NULL DEFAULT FALSE,
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
            
            // Player inventory cache table
            """
            CREATE TABLE IF NOT EXISTS player_inventory_cache (
                player_id VARCHAR(36) PRIMARY KEY,
                inventory_data LONGTEXT NOT NULL,
                cached_at BIGINT NOT NULL DEFAULT 0,
                FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        };
        
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            for (String sql : tableCreationSql) {
                stmt.execute(sql);
            }
        }
    }
    
    private void checkAndMigrate() throws SQLException {
        // Migration logic would go here if needed
        logger.info("Schema check completed");
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
        initialized = false;
        logger.info("MySQL database connection closed");
    }
    
    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed() && initialized;
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (!isConnected()) return false;
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeQuery("SELECT 1");
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
                INSERT INTO player_data (
                    player_id, player_name, is_on_duty, duty_start_time, off_duty_time, 
                    grace_debt_time, guard_rank, earned_off_duty_time, has_earned_base_time, 
                    has_been_notified_expired, session_searches, session_successful_searches, 
                    session_arrests, session_kills, session_detections, penalty_start_time,
                    current_penalty_stage, last_penalty_time, last_slowness_application,
                    has_active_penalty_boss_bar, accumulated_penalty_time, last_online_time,
                    penalty_tracking_paused, wanted_level, wanted_expire_time, wanted_reason, 
                    being_chased, chaser_guard, chase_start_time, total_arrests, total_violations, 
                    total_duty_time, last_updated
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    accumulated_penalty_time = VALUES(accumulated_penalty_time),
                    last_online_time = VALUES(last_online_time),
                    penalty_tracking_paused = VALUES(penalty_tracking_paused),
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
                stmt.setLong(21, playerData.getAccumulatedPenaltyTime());
                stmt.setLong(22, playerData.getLastOnlineTime());
                stmt.setBoolean(23, playerData.isPenaltyTrackingPaused());
                stmt.setInt(24, playerData.getWantedLevel());
                stmt.setLong(25, playerData.getWantedExpireTime());
                stmt.setString(26, playerData.getWantedReason());
                stmt.setBoolean(27, playerData.isBeingChased());
                stmt.setString(28, playerData.getChaserGuard() != null ? playerData.getChaserGuard().toString() : null);
                stmt.setLong(29, playerData.getChaseStartTime());
                stmt.setInt(30, playerData.getTotalArrests());
                stmt.setInt(31, playerData.getTotalViolations());
                stmt.setLong(32, playerData.getTotalDutyTime());
                stmt.setLong(33, System.currentTimeMillis());
                
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
        
        // Set offline penalty tracking information
        data.setAccumulatedPenaltyTime(rs.getLong("accumulated_penalty_time"));
        data.setLastOnlineTime(rs.getLong("last_online_time"));
        data.setPenaltyTrackingPaused(rs.getBoolean("penalty_tracking_paused"));
        
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
    
    // === REMAINING METHOD STUBS ===
    
    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(), executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerId) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<Void> saveChaseData(ChaseData chaseData) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<ChaseData> loadChaseData(UUID chaseId) {
        return CompletableFuture.supplyAsync(() -> null, executor);
    }
    
    @Override
    public CompletableFuture<List<ChaseData>> loadAllActiveChases() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(), executor);
    }
    
    @Override
    public CompletableFuture<Void> deleteChaseData(UUID chaseId) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<Void> cleanupExpiredChases() {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<Void> savePlayerInventory(UUID playerId, String inventoryData) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<String> loadPlayerInventory(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> null, executor);
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerInventory(UUID playerId) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<List<UUID>> getPlayersWithStoredInventory() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(), executor);
    }
    
    @Override
    public CompletableFuture<Boolean> hasStoredInventory(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> false, executor);
    }
    
    @Override
    public CompletableFuture<Void> performMaintenance() {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<DatabaseStats> getStatistics() {
        return CompletableFuture.supplyAsync(() -> 
            new DatabaseStats(0, 0, 0, 0, "MySQL", System.currentTimeMillis()), executor);
    }
    
    @Override
    public CompletableFuture<Void> createBackup(String backupPath) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<Void> batchSavePlayerData(List<PlayerData> playerDataList) {
        return CompletableFuture.runAsync(() -> {}, executor);
    }
    
    @Override
    public CompletableFuture<List<PlayerData>> batchLoadPlayerData(List<UUID> playerIds) {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(), executor);
    }
}