# 🔌 Developer API Reference

Complete developer guide for integrating with EdenCorrections, including API access, custom events, database integration, and extension development.

---

## 🎯 **Overview**

EdenCorrections provides a comprehensive API for developers to create extensions, integrations, and custom functionality. The API covers guard management, wanted systems, chase mechanics, contraband detection, and more.

### Integration Methods
| Method | Complexity | Best For |
|--------|------------|----------|
| **Plugin Dependency** | Simple | Direct API access, events |
| **PlaceholderAPI** | Easy | Display integration, GUI systems |
| **Database Access** | Advanced | Custom analytics, external systems |
| **Event Listening** | Moderate | Custom behaviors, logging |
| **Command Extensions** | Moderate | Additional functionality |

---

## 🚀 **Quick Start Integration**

### Plugin Dependency Setup

**plugin.yml**:
```yaml
name: YourPlugin
version: 1.0.0
main: com.yourpackage.YourPlugin
depend: [EdenCorrections]
api-version: "1.20"
```

**Basic API Access**:
```java
import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.managers.*;
import dev.lsdmc.edenCorrections.models.*;

public class YourPlugin extends JavaPlugin {
    private EdenCorrections edenCorrections;
    
    @Override
    public void onEnable() {
        // Get EdenCorrections instance
        edenCorrections = (EdenCorrections) Bukkit.getPluginManager().getPlugin("EdenCorrections");
        
        if (edenCorrections == null) {
            getLogger().severe("EdenCorrections not found! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Access managers
        GuardManager guardManager = edenCorrections.getGuardManager();
        WantedManager wantedManager = edenCorrections.getWantedManager();
        ChaseManager chaseManager = edenCorrections.getChaseManager();
        
        getLogger().info("Successfully hooked into EdenCorrections!");
    }
}
```

---

## 📋 **Core API Managers**

### GuardManager

**Access Guard Information**:
```java
import dev.lsdmc.edenCorrections.managers.DutyManager;
import dev.lsdmc.edenCorrections.models.PlayerData;

public class GuardIntegration {
    private final DutyManager dutyManager;
    
    public GuardIntegration(EdenCorrections plugin) {
        this.dutyManager = plugin.getDutyManager();
    }
    
    // Check if player is on duty
    public boolean isPlayerOnDuty(Player player) {
        return dutyManager.isOnDuty(player.getUniqueId());
    }
    
    // Get guard rank
    public String getGuardRank(Player player) {
        return dutyManager.getGuardRank(player);
    }
    
    // Get duty time
    public long getDutyTime(Player player) {
        PlayerData data = dutyManager.getPlayerData(player.getUniqueId());
        return data != null ? data.getDutyTime() : 0;
    }
    
    // Force duty change (admin function)
    public boolean toggleDutyStatus(Player player, boolean forceDuty) {
        if (forceDuty) {
            return dutyManager.setOnDuty(player, true);
        } else {
            return dutyManager.setOnDuty(player, false);
        }
    }
    
    // Get all online guards
    public List<Player> getOnlineGuards() {
        return dutyManager.getOnlineGuards();
    }
    
    // Check if player can go on duty
    public boolean canGoOnDuty(Player player) {
        return dutyManager.canToggleDuty(player, true);
    }
}
```

### WantedManager

**Wanted System Integration**:
```java
import dev.lsdmc.edenCorrections.managers.WantedManager;

public class WantedIntegration {
    private final WantedManager wantedManager;
    
    public WantedIntegration(EdenCorrections plugin) {
        this.wantedManager = plugin.getWantedManager();
    }
    
    // Set wanted level
    public void setWantedLevel(Player player, int level, String reason) {
        wantedManager.setWantedLevel(player.getUniqueId(), level, reason);
    }
    
    // Get wanted level
    public int getWantedLevel(Player player) {
        return wantedManager.getWantedLevel(player.getUniqueId());
    }
    
    // Check if player is wanted
    public boolean isWanted(Player player) {
        return wantedManager.isWanted(player.getUniqueId());
    }
    
    // Clear wanted status
    public void clearWanted(Player player) {
        wantedManager.clearWanted(player.getUniqueId());
    }
    
    // Get all wanted players
    public Map<UUID, Integer> getAllWantedPlayers() {
        return wantedManager.getAllWanted();
    }
    
    // Get wanted reason
    public String getWantedReason(Player player) {
        return wantedManager.getWantedReason(player.getUniqueId());
    }
    
    // Get time remaining
    public long getTimeRemaining(Player player) {
        return wantedManager.getTimeRemaining(player.getUniqueId());
    }
}
```

### ChaseManager

**Chase System Integration**:
```java
import dev.lsdmc.edenCorrections.managers.ChaseManager;
import dev.lsdmc.edenCorrections.models.ChaseData;

public class ChaseIntegration {
    private final ChaseManager chaseManager;
    
    public ChaseIntegration(EdenCorrections plugin) {
        this.chaseManager = plugin.getChaseManager();
    }
    
    // Start a chase
    public boolean startChase(Player guard, Player target) {
        return chaseManager.startChase(guard, target);
    }
    
    // End a chase
    public void endChase(Player guard) {
        chaseManager.endChase(guard.getUniqueId());
    }
    
    // Check if player is in chase
    public boolean isInChase(Player player) {
        return chaseManager.isInChase(player.getUniqueId());
    }
    
    // Get chase data
    public ChaseData getChaseData(Player guard) {
        return chaseManager.getChaseData(guard.getUniqueId());
    }
    
    // Get all active chases
    public Map<UUID, ChaseData> getActiveChases() {
        return chaseManager.getActiveChases();
    }
    
    // Check capture eligibility
    public boolean canCapture(Player guard, Player target) {
        return chaseManager.canCapture(guard, target);
    }
    
    // Perform capture
    public boolean performCapture(Player guard) {
        return chaseManager.performCapture(guard.getUniqueId());
    }
}
```

### ContrabandManager

**Contraband System Integration**:
```java
import dev.lsdmc.edenCorrections.managers.ContrabandManager;

public class ContrabandIntegration {
    private final ContrabandManager contrabandManager;
    
    public ContrabandIntegration(EdenCorrections plugin) {
        this.contrabandManager = plugin.getContrabandManager();
    }
    
    // Check for contraband
    public boolean hasContraband(Player player, String type) {
        return contrabandManager.hasContraband(player, type);
    }
    
    // Initiate contraband search
    public void initiateSearch(Player guard, Player target, String contrabandType) {
        contrabandManager.initiateSearch(guard, target, contrabandType);
    }
    
    // Drug test
    public boolean performDrugTest(Player guard, Player target) {
        return contrabandManager.performDrugTest(guard, target);
    }
    
    // Get contraband types
    public List<String> getContrabandTypes() {
        return contrabandManager.getContrabandTypes();
    }
    
    // Add custom contraband type
    public void addContrabandType(String type, List<Material> items, String description) {
        contrabandManager.addContrabandType(type, items, description);
    }
    
    // Check search cooldown
    public boolean isOnSearchCooldown(Player guard) {
        return contrabandManager.isOnCooldown(guard.getUniqueId());
    }
}
```

### DutyBankingManager

**Banking System Integration**:
```java
import dev.lsdmc.edenCorrections.managers.DutyBankingManager;

public class BankingIntegration {
    private final DutyBankingManager bankingManager;
    
    public BankingIntegration(EdenCorrections plugin) {
        this.bankingManager = plugin.getDutyBankingManager();
    }
    
    // Get available tokens
    public int getAvailableTokens(Player player) {
        return bankingManager.getAvailableTokens(player.getUniqueId());
    }
    
    // Get total duty time
    public long getTotalDutyTime(Player player) {
        return bankingManager.getTotalDutyTime(player.getUniqueId());
    }
    
    // Convert duty time
    public boolean convertDutyTime(Player player) {
        return bankingManager.convertDutyTime(player.getUniqueId());
    }
    
    // Add bonus time
    public void addBonusTime(Player player, long bonusSeconds, String reason) {
        bankingManager.addBonusTime(player.getUniqueId(), bonusSeconds, reason);
    }
    
    // Check conversion eligibility
    public boolean canConvert(Player player) {
        return bankingManager.canConvert(player.getUniqueId());
    }
    
    // Get conversion rate
    public int getConversionRate() {
        return bankingManager.getConversionRate();
    }
}
```

---

## 🎭 **Event System**

### Core Events

**GuardDutyEvent**:
```java
import dev.lsdmc.edenCorrections.events.GuardDutyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GuardDutyListener implements Listener {
    
    @EventHandler
    public void onGuardDutyChange(GuardDutyEvent event) {
        Player player = event.getPlayer();
        boolean isGoingOnDuty = event.isGoingOnDuty();
        String rank = event.getGuardRank();
        
        if (isGoingOnDuty) {
            // Player going on duty
            player.sendMessage("§a[Custom] Welcome back to duty, " + rank + "!");
            
            // Custom logic for duty start
            giveCustomEquipment(player, rank);
            logDutyStart(player);
            
        } else {
            // Player going off duty
            player.sendMessage("§e[Custom] Thank you for your service!");
            
            // Custom logic for duty end
            calculatePerformanceBonus(player);
            logDutyEnd(player);
        }
        
        // Cancel event if needed
        if (shouldPreventDutyChange(player)) {
            event.setCancelled(true);
            player.sendMessage("§cDuty change prevented by custom system!");
        }
    }
}
```

**WantedLevelChangeEvent**:
```java
import dev.lsdmc.edenCorrections.events.WantedLevelChangeEvent;

public class WantedSystemListener implements Listener {
    
    @EventHandler
    public void onWantedLevelChange(WantedLevelChangeEvent event) {
        Player player = event.getPlayer();
        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();
        String reason = event.getReason();
        
        // Log wanted level changes
        logWantedChange(player, oldLevel, newLevel, reason);
        
        // Custom responses based on level
        if (newLevel >= 4) {
            // High-threat response
            broadcastThreatAlert(player, newLevel);
            activateAutomaticResponse(player);
            
        } else if (newLevel == 0 && oldLevel > 0) {
            // Wanted level cleared
            player.sendMessage("§a[System] Your wanted status has been cleared!");
            removeSpecialRestrictions(player);
        }
        
        // Update custom systems
        updateBountySystem(player, newLevel);
        updateSecurityLevel(newLevel);
    }
}
```

**ChaseStartEvent / ChaseEndEvent**:
```java
import dev.lsdmc.edenCorrections.events.ChaseStartEvent;
import dev.lsdmc.edenCorrections.events.ChaseEndEvent;

public class ChaseSystemListener implements Listener {
    
    @EventHandler
    public void onChaseStart(ChaseStartEvent event) {
        Player guard = event.getGuard();
        Player target = event.getTarget();
        
        // Notify nearby guards
        notifyNearbyGuards(guard.getLocation(), target);
        
        // Custom chase mechanics
        applyChaseEffects(guard, target);
        startChaseTimer(guard, target);
        
        // Cancel if custom conditions not met
        if (!customChaseValidation(guard, target)) {
            event.setCancelled(true);
            guard.sendMessage("§cChase cancelled by security protocol!");
        }
    }
    
    @EventHandler
    public void onChaseEnd(ChaseEndEvent event) {
        Player guard = event.getGuard();
        Player target = event.getTarget();
        boolean wasSuccessful = event.wasSuccessful();
        String reason = event.getEndReason();
        
        // Cleanup chase effects
        removeChaseEffects(guard, target);
        
        // Performance tracking
        if (wasSuccessful) {
            awardChaseBonus(guard);
            updateStatistics(guard, "successful_chases", 1);
        }
        
        // Custom logic based on end reason
        switch (reason.toLowerCase()) {
            case "captured":
                handleSuccessfulCapture(guard, target);
                break;
            case "escaped":
                handleEscape(guard, target);
                break;
            case "timeout":
                handleTimeout(guard, target);
                break;
        }
    }
}
```

**ContrabandDetectionEvent**:
```java
import dev.lsdmc.edenCorrections.events.ContrabandDetectionEvent;

public class ContrabandListener implements Listener {
    
    @EventHandler
    public void onContrabandDetection(ContrabandDetectionEvent event) {
        Player guard = event.getGuard();
        Player target = event.getTarget();
        String contrabandType = event.getContrabandType();
        boolean wasVoluntary = event.wasVoluntarySurrender();
        List<ItemStack> contraband = event.getContrabandItems();
        
        // Custom contraband handling
        if (!wasVoluntary) {
            // Resistance to search - increase wanted level
            increaseWantedLevel(target, 1, "Contraband possession: " + contrabandType);
            
            // Performance bonus for guard
            awardDetectionBonus(guard, contrabandType);
        }
        
        // Log contraband for analytics
        logContrabandDetection(guard, target, contrabandType, contraband);
        
        // Custom item handling
        handleConfiscatedItems(contraband, guard, target);
        
        // Update security alerts
        if (isHighRiskContraband(contrabandType)) {
            triggerSecurityAlert(target, contrabandType);
        }
    }
}
```

### Custom Event Creation

**Creating Custom Events**:
```java
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomSecurityEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private final Player player;
    private final String securityLevel;
    private final String reason;
    private boolean cancelled = false;
    
    public CustomSecurityEvent(Player player, String securityLevel, String reason) {
        this.player = player;
        this.securityLevel = securityLevel;
        this.reason = reason;
    }
    
    public Player getPlayer() { return player; }
    public String getSecurityLevel() { return securityLevel; }
    public String getReason() { return reason; }
    
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    @Override
    public HandlerList getHandlers() { return handlers; }
    
    public static HandlerList getHandlerList() { return handlers; }
}

// Fire the event
public void triggerSecurityAlert(Player player, String level, String reason) {
    CustomSecurityEvent event = new CustomSecurityEvent(player, level, reason);
    Bukkit.getPluginManager().callEvent(event);
    
    if (!event.isCancelled()) {
        // Proceed with security response
        executeSecurityProtocol(player, level);
    }
}
```

---

## 🗄️ **Database Integration**

### Direct Database Access

**Database Connection**:
```java
import dev.lsdmc.edenCorrections.storage.DatabaseHandler;
import dev.lsdmc.edenCorrections.storage.DataManager;

public class CustomDatabaseIntegration {
    private final DatabaseHandler dbHandler;
    private final DataManager dataManager;
    
    public CustomDatabaseIntegration(EdenCorrections plugin) {
        this.dataManager = plugin.getDataManager();
        this.dbHandler = dataManager.getDatabaseHandler();
    }
    
    // Custom player data queries
    public Map<String, Object> getCustomPlayerStats(UUID playerUUID) {
        String query = "SELECT * FROM eden_player_data WHERE player_id = ?";
        
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("arrests", rs.getInt("arrests"));
                stats.put("searches", rs.getInt("searches"));
                stats.put("duty_time", rs.getLong("duty_time"));
                stats.put("violations", rs.getInt("violations"));
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return new HashMap<>();
    }
    
    // Custom analytics
    public List<Map<String, Object>> getTopPerformers(int limit) {
        String query = """
            SELECT player_id, arrests, searches, duty_time,
                   (arrests * 10 + searches * 5 + duty_time / 3600) as score
            FROM eden_player_data 
            ORDER BY score DESC 
            LIMIT ?
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> performer = new HashMap<>();
                performer.put("player_id", rs.getString("player_id"));
                performer.put("arrests", rs.getInt("arrests"));
                performer.put("searches", rs.getInt("searches"));
                performer.put("duty_time", rs.getLong("duty_time"));
                performer.put("score", rs.getDouble("score"));
                results.add(performer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return results;
    }
    
    // Async database operations
    public CompletableFuture<Void> updateCustomStatAsync(UUID playerUUID, String stat, int value) {
        return CompletableFuture.runAsync(() -> {
            String query = "UPDATE eden_player_data SET " + stat + " = ? WHERE player_id = ?";
            
            try (Connection conn = dbHandler.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, value);
                stmt.setString(2, playerUUID.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
```

### Custom Table Creation

**Additional Data Storage**:
```java
public class CustomTableManager {
    private final DatabaseHandler dbHandler;
    
    public CustomTableManager(EdenCorrections plugin) {
        this.dbHandler = plugin.getDataManager().getDatabaseHandler();
        createCustomTables();
    }
    
    private void createCustomTables() {
        // Performance metrics table
        String performanceTable = """
            CREATE TABLE IF NOT EXISTS eden_performance_metrics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_id VARCHAR(36) NOT NULL,
                metric_type VARCHAR(50) NOT NULL,
                metric_value DOUBLE NOT NULL,
                timestamp BIGINT NOT NULL,
                INDEX idx_player_metric (player_id, metric_type),
                INDEX idx_timestamp (timestamp)
            )
        """;
        
        // Custom events table
        String eventsTable = """
            CREATE TABLE IF NOT EXISTS eden_custom_events (
                id INT AUTO_INCREMENT PRIMARY KEY,
                event_type VARCHAR(100) NOT NULL,
                player_id VARCHAR(36),
                guard_id VARCHAR(36),
                data JSON,
                timestamp BIGINT NOT NULL,
                INDEX idx_event_type (event_type),
                INDEX idx_timestamp (timestamp)
            )
        """;
        
        executeUpdate(performanceTable);
        executeUpdate(eventsTable);
    }
    
    public void logPerformanceMetric(UUID playerUUID, String metricType, double value) {
        String query = """
            INSERT INTO eden_performance_metrics (player_id, metric_type, metric_value, timestamp)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, metricType);
            stmt.setDouble(3, value);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void logCustomEvent(String eventType, UUID playerUUID, UUID guardUUID, String jsonData) {
        String query = """
            INSERT INTO eden_custom_events (event_type, player_id, guard_id, data, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbHandler.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, eventType);
            stmt.setString(2, playerUUID != null ? playerUUID.toString() : null);
            stmt.setString(3, guardUUID != null ? guardUUID.toString() : null);
            stmt.setString(4, jsonData);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 🎯 **PlaceholderAPI Extensions**

### Custom Placeholder Expansion

**Creating Custom Placeholders**:
```java
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class CustomEdenExpansion extends PlaceholderExpansion {
    private final EdenCorrections plugin;
    
    public CustomEdenExpansion(EdenCorrections plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "customeden";
    }
    
    @Override
    public String getAuthor() {
        return "YourName";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        
        // Custom performance metrics
        if (params.equals("performance_score")) {
            return String.valueOf(calculatePerformanceScore(player));
        }
        
        if (params.equals("efficiency_rating")) {
            return getEfficiencyRating(player);
        }
        
        if (params.equals("guard_level")) {
            return String.valueOf(getGuardLevel(player));
        }
        
        if (params.equals("next_promotion_progress")) {
            return String.valueOf(getPromotionProgress(player));
        }
        
        // Time-based placeholders
        if (params.equals("shift_time_remaining")) {
            return formatTime(getShiftTimeRemaining(player));
        }
        
        if (params.equals("break_time_earned")) {
            return formatTime(getBreakTimeEarned(player));
        }
        
        // Statistics placeholders
        if (params.startsWith("stat_")) {
            String statName = params.substring(5);
            return String.valueOf(getPlayerStat(player, statName));
        }
        
        // Ranking placeholders
        if (params.startsWith("rank_")) {
            String rankType = params.substring(5);
            return String.valueOf(getPlayerRank(player, rankType));
        }
        
        return null;
    }
    
    private double calculatePerformanceScore(Player player) {
        UUID uuid = player.getUniqueId();
        DutyBankingManager banking = plugin.getDutyBankingManager();
        
        // Get base stats
        long dutyTime = banking.getTotalDutyTime(uuid);
        int arrests = getPlayerStat(player, "arrests");
        int searches = getPlayerStat(player, "searches");
        
        // Calculate weighted score
        double score = (dutyTime / 3600.0) * 1.0 +  // 1 point per hour
                      arrests * 10.0 +               // 10 points per arrest
                      searches * 5.0;                // 5 points per search
        
        return Math.round(score * 100.0) / 100.0;
    }
    
    private String getEfficiencyRating(Player player) {
        double score = calculatePerformanceScore(player);
        
        if (score >= 1000) return "S+";
        if (score >= 800) return "S";
        if (score >= 600) return "A";
        if (score >= 400) return "B";
        if (score >= 200) return "C";
        return "D";
    }
}
```

---

## 🛠️ **Advanced Integration Examples**

### Custom Command Extension

**Adding Custom Commands**:
```java
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CustomGuardCommands implements CommandExecutor {
    private final EdenCorrections edenCorrections;
    
    public CustomGuardCommands(EdenCorrections plugin) {
        this.edenCorrections = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        switch (command.getName().toLowerCase()) {
            case "guardstats":
                showGuardStatistics(player, args);
                return true;
                
            case "patrol":
                startPatrolMode(player);
                return true;
                
            case "backup":
                requestBackup(player, args);
                return true;
                
            case "investigate":
                investigatePlayer(player, args);
                return true;
        }
        
        return false;
    }
    
    private void showGuardStatistics(Player player, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        // Check permissions for viewing other players' stats
        if (!target.equals(player) && !player.hasPermission("edencorrections.admin")) {
            player.sendMessage("§cYou don't have permission to view other players' statistics!");
            return;
        }
        
        // Gather statistics
        DutyManager dutyManager = edenCorrections.getDutyManager();
        PlayerData data = dutyManager.getPlayerData(target.getUniqueId());
        
        if (data == null) {
            player.sendMessage("§cNo data found for " + target.getName());
            return;
        }
        
        // Display formatted statistics
        player.sendMessage("§9§l=== Guard Statistics for " + target.getName() + " ===");
        player.sendMessage("§7Rank: §e" + dutyManager.getGuardRank(target));
        player.sendMessage("§7Status: " + (dutyManager.isOnDuty(target.getUniqueId()) ? "§aOn Duty" : "§cOff Duty"));
        player.sendMessage("§7Total Duty Time: §b" + formatDuration(data.getDutyTime()));
        player.sendMessage("§7Arrests: §a" + data.getArrests());
        player.sendMessage("§7Searches: §e" + data.getSearches());
        player.sendMessage("§7Performance Score: §d" + calculatePerformanceScore(data));
    }
    
    private void startPatrolMode(Player player) {
        if (!player.hasPermission("edencorrections.guard")) {
            player.sendMessage("§cYou must be a guard to use patrol mode!");
            return;
        }
        
        DutyManager dutyManager = edenCorrections.getDutyManager();
        if (!dutyManager.isOnDuty(player.getUniqueId())) {
            player.sendMessage("§cYou must be on duty to start patrol mode!");
            return;
        }
        
        // Implement patrol mode logic
        activatePatrolMode(player);
        player.sendMessage("§a[Patrol] Patrol mode activated! Enhanced detection enabled.");
    }
}
```

### Integration with External Plugins

**Discord Integration Example**:
```java
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;

public class DiscordIntegration {
    private final EdenCorrections plugin;
    private final JDA jda;
    private final String channelId;
    
    public DiscordIntegration(EdenCorrections plugin, JDA jda, String channelId) {
        this.plugin = plugin;
        this.jda = jda;
        this.channelId = channelId;
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new DiscordEventListener(), plugin);
    }
    
    private class DiscordEventListener implements Listener {
        
        @EventHandler
        public void onWantedLevelChange(WantedLevelChangeEvent event) {
            if (event.getNewLevel() >= 4) {
                // High-priority wanted alert
                sendDiscordAlert(
                    "🚨 **HIGH PRIORITY ALERT** 🚨\n" +
                    "Player: `" + event.getPlayer().getName() + "`\n" +
                    "Wanted Level: " + event.getNewLevel() + " stars\n" +
                    "Reason: " + event.getReason()
                );
            }
        }
        
        @EventHandler
        public void onChaseStart(ChaseStartEvent event) {
            sendDiscordMessage(
                "🏃 **Chase Started**\n" +
                "Guard: `" + event.getGuard().getName() + "`\n" +
                "Target: `" + event.getTarget().getName() + "`"
            );
        }
        
        @EventHandler
        public void onContrabandDetection(ContrabandDetectionEvent event) {
            if (!event.wasVoluntarySurrender()) {
                sendDiscordMessage(
                    "🔍 **Contraband Detected**\n" +
                    "Guard: `" + event.getGuard().getName() + "`\n" +
                    "Suspect: `" + event.getTarget().getName() + "`\n" +
                    "Type: " + event.getContrabandType() + "\n" +
                    "Items: " + event.getContrabandItems().size()
                );
            }
        }
    }
    
    private void sendDiscordMessage(String message) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
    
    private void sendDiscordAlert(String message) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage("@here " + message).queue();
        }
    }
}
```

### Custom Economy Integration

**TokenManager Integration**:
```java
public class TokenEconomyIntegration {
    private final EdenCorrections plugin;
    private final TokenManager tokenManager;
    
    public TokenEconomyIntegration(EdenCorrections plugin) {
        this.plugin = plugin;
        this.tokenManager = (TokenManager) Bukkit.getPluginManager().getPlugin("TokenManager");
        
        if (tokenManager != null) {
            setupCustomRewards();
        }
    }
    
    private void setupCustomRewards() {
        // Register event listener for custom rewards
        Bukkit.getPluginManager().registerEvents(new EconomyRewardListener(), plugin);
    }
    
    private class EconomyRewardListener implements Listener {
        
        @EventHandler
        public void onSuccessfulArrest(ChaseEndEvent event) {
            if (event.wasSuccessful() && event.getEndReason().equals("captured")) {
                Player guard = event.getGuard();
                Player target = event.getTarget();
                
                // Base reward
                int baseReward = 100;
                
                // Bonus based on wanted level
                WantedManager wantedManager = plugin.getWantedManager();
                int wantedLevel = wantedManager.getWantedLevel(target.getUniqueId());
                int bonus = wantedLevel * 50;
                
                int totalReward = baseReward + bonus;
                
                // Award tokens
                tokenManager.addTokens(guard, totalReward);
                
                guard.sendMessage(String.format(
                    "§a[Economy] Arrest reward: §e%d tokens §a(Base: %d + Wanted bonus: %d)",
                    totalReward, baseReward, bonus
                ));
            }
        }
        
        @EventHandler
        public void onContrabandDetection(ContrabandDetectionEvent event) {
            if (!event.wasVoluntarySurrender()) {
                Player guard = event.getGuard();
                String contrabandType = event.getContrabandType();
                
                // Different rewards for different contraband types
                int reward = switch (contrabandType.toLowerCase()) {
                    case "drugs" -> 150;
                    case "weapons" -> 100;
                    case "armor" -> 75;
                    default -> 50;
                };
                
                tokenManager.addTokens(guard, reward);
                
                guard.sendMessage(String.format(
                    "§a[Economy] Contraband detection reward: §e%d tokens",
                    reward
                ));
            }
        }
    }
}
```

---

## 📊 **Performance Monitoring**

### Custom Metrics Collection

**Performance Tracker**:
```java
public class PerformanceTracker {
    private final EdenCorrections plugin;
    private final Map<String, Long> operationTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> operationCounts = new ConcurrentHashMap<>();
    
    public PerformanceTracker(EdenCorrections plugin) {
        this.plugin = plugin;
        startMetricsCollection();
    }
    
    public void trackOperation(String operation, Runnable task) {
        long startTime = System.nanoTime();
        
        try {
            task.run();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordOperation(operation, duration);
        }
    }
    
    public <T> T trackOperation(String operation, Supplier<T> task) {
        long startTime = System.nanoTime();
        
        try {
            return task.get();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordOperation(operation, duration);
        }
    }
    
    private void recordOperation(String operation, long duration) {
        operationTimes.merge(operation, duration, Long::sum);
        operationCounts.merge(operation, 1, Integer::sum);
    }
    
    public Map<String, Double> getAverageOperationTimes() {
        Map<String, Double> averages = new HashMap<>();
        
        for (String operation : operationTimes.keySet()) {
            long totalTime = operationTimes.get(operation);
            int count = operationCounts.get(operation);
            double average = (double) totalTime / count / 1_000_000.0; // Convert to milliseconds
            averages.put(operation, average);
        }
        
        return averages;
    }
    
    private void startMetricsCollection() {
        // Collect metrics every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            logPerformanceMetrics();
            resetMetrics();
        }, 0L, 6000L); // 5 minutes in ticks
    }
    
    private void logPerformanceMetrics() {
        Map<String, Double> averages = getAverageOperationTimes();
        
        plugin.getLogger().info("=== Performance Metrics ===");
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            plugin.getLogger().info(String.format(
                "%s: %.2fms (count: %d)",
                entry.getKey(),
                entry.getValue(),
                operationCounts.get(entry.getKey())
            ));
        }
    }
    
    private void resetMetrics() {
        operationTimes.clear();
        operationCounts.clear();
    }
}
```

---

## 🔧 **Configuration Extensions**

### Custom Configuration Manager

**Extended Config System**:
```java
public class CustomConfigManager {
    private final EdenCorrections plugin;
    private FileConfiguration config;
    private File configFile;
    
    public CustomConfigManager(EdenCorrections plugin, String configName) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), configName + ".yml");
        
        if (!configFile.exists()) {
            plugin.saveResource(configName + ".yml", false);
        }
        
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config file: " + e.getMessage());
        }
    }
    
    // Type-safe configuration getters
    public <T> T get(String path, Class<T> type, T defaultValue) {
        Object value = config.get(path, defaultValue);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultValue;
    }
    
    public List<String> getStringList(String path, List<String> defaultValue) {
        return config.getStringList(path).isEmpty() ? defaultValue : config.getStringList(path);
    }
    
    public Map<String, Object> getSection(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return new HashMap<>();
        
        return section.getValues(false);
    }
    
    // Custom configuration validation
    public boolean validateConfig() {
        List<String> errors = new ArrayList<>();
        
        // Validate required sections
        String[] requiredSections = {"rewards", "limits", "messages"};
        for (String section : requiredSections) {
            if (!config.contains(section)) {
                errors.add("Missing required section: " + section);
            }
        }
        
        // Validate numeric ranges
        validateRange("limits.max-chases", 1, 50, errors);
        validateRange("limits.max-wanted-level", 1, 10, errors);
        
        if (!errors.isEmpty()) {
            plugin.getLogger().severe("Configuration validation failed:");
            errors.forEach(error -> plugin.getLogger().severe("- " + error));
            return false;
        }
        
        return true;
    }
    
    private void validateRange(String path, int min, int max, List<String> errors) {
        int value = config.getInt(path, min);
        if (value < min || value > max) {
            errors.add(String.format(
                "Value at %s (%d) is outside valid range [%d-%d]",
                path, value, min, max
            ));
        }
    }
}
```

---

## 📋 **API Quick Reference**

### Essential Manager Methods

```java
// DutyManager
dutyManager.isOnDuty(UUID playerUUID)
dutyManager.getGuardRank(Player player)
dutyManager.setOnDuty(Player player, boolean onDuty)
dutyManager.canToggleDuty(Player player, boolean targetState)

// WantedManager  
wantedManager.setWantedLevel(UUID playerUUID, int level, String reason)
wantedManager.getWantedLevel(UUID playerUUID)
wantedManager.isWanted(UUID playerUUID)
wantedManager.clearWanted(UUID playerUUID)

// ChaseManager
chaseManager.startChase(Player guard, Player target)
chaseManager.endChase(UUID guardUUID)
chaseManager.isInChase(UUID playerUUID)
chaseManager.canCapture(Player guard, Player target)

// ContrabandManager
contrabandManager.hasContraband(Player player, String type)
contrabandManager.initiateSearch(Player guard, Player target, String type)
contrabandManager.performDrugTest(Player guard, Player target)

// DutyBankingManager
bankingManager.getAvailableTokens(UUID playerUUID)
bankingManager.convertDutyTime(UUID playerUUID)
bankingManager.addBonusTime(UUID playerUUID, long seconds, String reason)
```

### Essential Events

```java
GuardDutyEvent          // Duty status changes
WantedLevelChangeEvent  // Wanted level modifications
ChaseStartEvent         // Chase initiation
ChaseEndEvent          // Chase completion
ContrabandDetectionEvent // Contraband found
JailEvent              // Player arrests
```

### Database Tables

```sql
eden_player_data        -- Player statistics and data
eden_chase_data         -- Active chase information  
eden_wanted_data        -- Wanted player records
eden_contraband_logs    -- Contraband detection history
```

### Configuration Paths

```yaml
guard-system           # Guard system settings
chase                 # Chase mechanics
contraband            # Contraband detection
duty-banking          # Banking system
database              # Database configuration
```

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete Developer API Reference* 