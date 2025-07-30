package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.integrations.VaultEconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Comprehensive Penalty Management System
 * 
 * Handles violations, fines, punishments, and enforcement actions
 * with proper tracking and escalation mechanisms.
 */
public class PenaltyManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Penalty tracking
    private final Map<UUID, PenaltyRecord> activePenalties;
    private final Map<UUID, BukkitTask> penaltyTasks;
    
    // Configuration
    private boolean penaltiesEnabled;
    private boolean useEconomicFines;
    private boolean escalatingPenalties;
    private int maxViolationsBeforeJail;
    private long penaltyResetTime;
    
    public PenaltyManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activePenalties = new HashMap<>();
        this.penaltyTasks = new HashMap<>();
    }
    
    public void initialize() {
        loadConfiguration();
        startPenaltyMonitoring();
        logger.info("PenaltyManager initialized - Penalties enabled: " + penaltiesEnabled);
    }
    
    private void loadConfiguration() {
        penaltiesEnabled = plugin.getConfigManager().getConfig().getBoolean("penalties.enabled", true);
        useEconomicFines = plugin.getConfigManager().getConfig().getBoolean("penalties.economic-fines.enabled", true);
        escalatingPenalties = plugin.getConfigManager().getConfig().getBoolean("penalties.escalating-penalties", true);
        maxViolationsBeforeJail = plugin.getConfigManager().getConfig().getInt("penalties.max-violations-before-jail", 5);
        penaltyResetTime = plugin.getConfigManager().getConfig().getLong("penalties.reset-time-hours", 24) * 3600000L; // Convert to milliseconds
    }
    
    private void startPenaltyMonitoring() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            cleanupExpiredPenalties();
            checkPenaltyEscalation();
        }, 20L * 60L, 20L * 60L); // Every minute
    }
    
    /**
     * Apply penalty for a violation
     */
    public CompletableFuture<Boolean> applyPenalty(Player player, ViolationType violationType, String reason) {
        if (!penaltiesEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data == null) {
                    return false;
                }
                
                // Get or create penalty record
                PenaltyRecord record = activePenalties.computeIfAbsent(player.getUniqueId(), 
                    k -> new PenaltyRecord(player.getUniqueId()));
                
                // Calculate penalty severity
                PenaltySeverity severity = calculatePenaltySeverity(record, violationType);
                
                // Apply the penalty
                boolean success = applyPenaltyBySeverity(player, data, severity, violationType, reason);
                
                if (success) {
                    // Record the violation
                    record.addViolation(violationType, severity, reason);
                    data.incrementViolations();
                    
                    // Save data
                    plugin.getDataManager().savePlayerData(data);
                    
                    // Notify relevant parties
                    notifyPenaltyApplied(player, severity, violationType, reason);
                    
                    // Check for escalation
                    if (escalatingPenalties) {
                        checkForEscalation(player, record);
                    }
                }
                
                return success;
            } catch (Exception e) {
                logger.severe("Error applying penalty to " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    private PenaltySeverity calculatePenaltySeverity(PenaltyRecord record, ViolationType violationType) {
        int recentViolations = record.getRecentViolationCount(System.currentTimeMillis() - penaltyResetTime);
        
        // Base severity from violation type
        PenaltySeverity baseSeverity = violationType.getBaseSeverity();
        
        if (!escalatingPenalties) {
            return baseSeverity;
        }
        
        // Escalate based on recent violations
        if (recentViolations >= 5) {
            return PenaltySeverity.SEVERE;
        } else if (recentViolations >= 3) {
            return PenaltySeverity.MAJOR;
        } else if (recentViolations >= 1) {
            return PenaltySeverity.MODERATE;
        }
        
        return baseSeverity;
    }
    
    private boolean applyPenaltyBySeverity(Player player, PlayerData data, PenaltySeverity severity, 
                                         ViolationType violationType, String reason) {
        switch (severity) {
            case MINOR:
                return applyMinorPenalty(player, data, violationType, reason);
            case MODERATE:
                return applyModeratePenalty(player, data, violationType, reason);
            case MAJOR:
                return applyMajorPenalty(player, data, violationType, reason);
            case SEVERE:
                return applySeverePenalty(player, data, violationType, reason);
            default:
                return false;
        }
    }
    
    private boolean applyMinorPenalty(Player player, PlayerData data, ViolationType violationType, String reason) {
        // Warning + small fine
        plugin.getMessageManager().sendMessage(player, "penalties.minor.warning",
            plugin.getMessageManager().stringPlaceholder("violation", violationType.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("reason", reason));
        
        if (useEconomicFines) {
            double fine = plugin.getConfigManager().getConfig().getDouble("penalties.fines.minor", 100.0);
            return applyEconomicFine(player, fine, "Minor violation: " + violationType.getDisplayName());
        }
        
        return true;
    }
    
    private boolean applyModeratePenalty(Player player, PlayerData data, ViolationType violationType, String reason) {
        // Increased wanted level + fine
        plugin.getWantedManager().increaseWantedLevel(player, 1, "Penalty: " + reason);
        
        plugin.getMessageManager().sendMessage(player, "penalties.moderate.applied",
            plugin.getMessageManager().stringPlaceholder("violation", violationType.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("reason", reason));
        
        if (useEconomicFines) {
            double fine = plugin.getConfigManager().getConfig().getDouble("penalties.fines.moderate", 250.0);
            return applyEconomicFine(player, fine, "Moderate violation: " + violationType.getDisplayName());
        }
        
        return true;
    }
    
    private boolean applyMajorPenalty(Player player, PlayerData data, ViolationType violationType, String reason) {
        // Significant wanted level increase + larger fine
        plugin.getWantedManager().increaseWantedLevel(player, 2, "Major penalty: " + reason);
        
        plugin.getMessageManager().sendMessage(player, "penalties.major.applied",
            plugin.getMessageManager().stringPlaceholder("violation", violationType.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("reason", reason));
        
        if (useEconomicFines) {
            double fine = plugin.getConfigManager().getConfig().getDouble("penalties.fines.major", 500.0);
            applyEconomicFine(player, fine, "Major violation: " + violationType.getDisplayName());
        }
        
        // Consider temporary restrictions
        applyTemporaryRestrictions(player, 300); // 5 minutes
        
        return true;
    }
    
    private boolean applySeverePenalty(Player player, PlayerData data, ViolationType violationType, String reason) {
        // Maximum wanted level + immediate jail consideration
        plugin.getWantedManager().setWantedLevel(player, plugin.getConfigManager().getMaxWantedLevel(), 
            "Severe penalty: " + reason);
        
        plugin.getMessageManager().sendMessage(player, "penalties.severe.applied",
            plugin.getMessageManager().stringPlaceholder("violation", violationType.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("reason", reason));
        
        if (useEconomicFines) {
            double fine = plugin.getConfigManager().getConfig().getDouble("penalties.fines.severe", 1000.0);
            applyEconomicFine(player, fine, "Severe violation: " + violationType.getDisplayName());
        }
        
        // Apply extended restrictions
        applyTemporaryRestrictions(player, 900); // 15 minutes
        
        // Consider automatic jail
        if (data.getTotalViolations() >= maxViolationsBeforeJail) {
            scheduleAutomaticJail(player, reason);
        }
        
        return true;
    }
    
    private boolean applyEconomicFine(Player player, double amount, String reason) {
        VaultEconomyManager economy = plugin.getVaultEconomyManager();
        if (economy != null && economy.isAvailable()) {
            economy.takeMoney(player, amount, reason).thenAccept(success -> {
                if (success) {
                    plugin.getMessageManager().sendMessage(player, "penalties.fine.applied",
                        plugin.getMessageManager().stringPlaceholder("amount", economy.formatAmount(amount)),
                        plugin.getMessageManager().stringPlaceholder("reason", reason));
                } else {
                    plugin.getMessageManager().sendMessage(player, "penalties.fine.insufficient-funds",
                        plugin.getMessageManager().stringPlaceholder("amount", economy.formatAmount(amount)));
                }
            });
            return true;
        }
        return false;
    }
    
    private void applyTemporaryRestrictions(Player player, int durationSeconds) {
        // Add player to temporary restriction list
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing restriction task
        BukkitTask existingTask = penaltyTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start new restriction task
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            penaltyTasks.remove(playerId);
            plugin.getMessageManager().sendMessage(player, "penalties.restrictions.lifted");
        }, 20L * durationSeconds);
        
        penaltyTasks.put(playerId, task);
        
        plugin.getMessageManager().sendMessage(player, "penalties.restrictions.applied",
            plugin.getMessageManager().numberPlaceholder("duration", durationSeconds / 60));
    }
    
    private void scheduleAutomaticJail(Player player, String reason) {
        // Schedule jail with a slight delay to allow other systems to process
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                int jailTime = plugin.getConfigManager().getConfig().getInt("penalties.automatic-jail-time", 600); // 10 minutes
                plugin.getCMIIntegration().jailPlayer(null, player, jailTime, "Automatic jail: " + reason);
                
                plugin.getMessageManager().sendMessage(player, "penalties.automatic-jail",
                    plugin.getMessageManager().stringPlaceholder("reason", reason),
                    plugin.getMessageManager().numberPlaceholder("time", jailTime / 60));
            }
        }, 20L); // 1 second delay
    }
    
    private void checkForEscalation(Player player, PenaltyRecord record) {
        int recentViolations = record.getRecentViolationCount(System.currentTimeMillis() - penaltyResetTime);
        
        if (recentViolations >= maxViolationsBeforeJail) {
            // Notify administrators
            plugin.getMessageManager().sendGuardAlert("penalties.escalation.alert",
                plugin.getMessageManager().playerPlaceholder("player", player),
                plugin.getMessageManager().numberPlaceholder("violations", recentViolations));
        }
    }
    
    private void notifyPenaltyApplied(Player player, PenaltySeverity severity, ViolationType violationType, String reason) {
        // Notify guards about the penalty
        plugin.getMessageManager().sendGuardAlert("penalties.applied.alert",
            plugin.getMessageManager().playerPlaceholder("player", player),
            plugin.getMessageManager().stringPlaceholder("severity", severity.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("violation", violationType.getDisplayName()),
            plugin.getMessageManager().stringPlaceholder("reason", reason));
        
        // Log for administrators
        logger.info("Penalty applied: " + player.getName() + " - " + severity + " - " + violationType + " - " + reason);
    }
    
    private void cleanupExpiredPenalties() {
        long currentTime = System.currentTimeMillis();
        activePenalties.entrySet().removeIf(entry -> {
            PenaltyRecord record = entry.getValue();
            return record.isExpired(currentTime, penaltyResetTime);
        });
    }
    
    private void checkPenaltyEscalation() {
        // Check for players who need escalated penalties
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PenaltyRecord record = activePenalties.get(player.getUniqueId());
            if (record != null) {
                int recentViolations = record.getRecentViolationCount(System.currentTimeMillis() - penaltyResetTime);
                if (recentViolations >= maxViolationsBeforeJail * 2) {
                    // Extreme escalation - consider server-wide alert
                    plugin.getMessageManager().sendMessage(plugin.getServer().getConsoleSender(), 
                        "penalties.extreme-escalation",
                        plugin.getMessageManager().playerPlaceholder("player", player),
                        plugin.getMessageManager().numberPlaceholder("violations", recentViolations));
                }
            }
        }
    }
    
    /**
     * Check if player has temporary restrictions
     */
    public boolean hasTemporaryRestrictions(Player player) {
        return penaltyTasks.containsKey(player.getUniqueId());
    }
    
    /**
     * Get player's penalty record
     */
    public PenaltyRecord getPenaltyRecord(Player player) {
        return activePenalties.get(player.getUniqueId());
    }
    
    /**
     * Clear all penalties for a player (admin command)
     */
    public void clearPenalties(Player player) {
        UUID playerId = player.getUniqueId();
        activePenalties.remove(playerId);
        
        BukkitTask task = penaltyTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        plugin.getMessageManager().sendMessage(player, "penalties.cleared");
    }
    
    /**
     * Get penalty statistics
     */
    public Map<String, Object> getPenaltyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activePenaltyRecords", activePenalties.size());
        stats.put("activeRestrictions", penaltyTasks.size());
        stats.put("penaltiesEnabled", penaltiesEnabled);
        stats.put("economicFinesEnabled", useEconomicFines);
        return stats;
    }
    
    public void cleanup() {
        // Cancel all active penalty tasks
        penaltyTasks.values().forEach(BukkitTask::cancel);
        penaltyTasks.clear();
        activePenalties.clear();
    }
    
    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = penaltyTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
    
    // === ENUMS AND INNER CLASSES ===
    
    public enum ViolationType {
        CONTRABAND_POSSESSION("Contraband Possession", PenaltySeverity.MODERATE),
        GUARD_ASSAULT("Assaulting Guard", PenaltySeverity.MAJOR),
        GUARD_MURDER("Murdering Guard", PenaltySeverity.SEVERE),
        ESCAPE_ATTEMPT("Escape Attempt", PenaltySeverity.MAJOR),
        RESTRICTED_AREA("Restricted Area Access", PenaltySeverity.MINOR),
        COMMAND_ABUSE("Command Abuse", PenaltySeverity.MINOR),
        DUTY_VIOLATION("Duty Violation", PenaltySeverity.MODERATE),
        SECURITY_BREACH("Security Breach", PenaltySeverity.MAJOR);
        
        private final String displayName;
        private final PenaltySeverity baseSeverity;
        
        ViolationType(String displayName, PenaltySeverity baseSeverity) {
            this.displayName = displayName;
            this.baseSeverity = baseSeverity;
        }
        
        public String getDisplayName() { return displayName; }
        public PenaltySeverity getBaseSeverity() { return baseSeverity; }
    }
    
    public enum PenaltySeverity {
        MINOR("Minor", 1),
        MODERATE("Moderate", 2),
        MAJOR("Major", 3),
        SEVERE("Severe", 4);
        
        private final String displayName;
        private final int level;
        
        PenaltySeverity(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }
        
        public String getDisplayName() { return displayName; }
        public int getLevel() { return level; }
    }
    
    public static class PenaltyRecord {
        private final UUID playerId;
        private final Map<Long, ViolationEntry> violations;
        
        public PenaltyRecord(UUID playerId) {
            this.playerId = playerId;
            this.violations = new HashMap<>();
        }
        
        public void addViolation(ViolationType type, PenaltySeverity severity, String reason) {
            long timestamp = System.currentTimeMillis();
            violations.put(timestamp, new ViolationEntry(type, severity, reason, timestamp));
        }
        
        public int getRecentViolationCount(long since) {
            return (int) violations.values().stream()
                .filter(entry -> entry.timestamp >= since)
                .count();
        }
        
        public boolean isExpired(long currentTime, long expiryTime) {
            return violations.isEmpty() || 
                   violations.values().stream().allMatch(entry -> currentTime - entry.timestamp > expiryTime);
        }
        
        public UUID getPlayerId() { return playerId; }
        public Map<Long, ViolationEntry> getViolations() { return violations; }
    }
    
    public static class ViolationEntry {
        public final ViolationType type;
        public final PenaltySeverity severity;
        public final String reason;
        public final long timestamp;
        
        public ViolationEntry(ViolationType type, PenaltySeverity severity, String reason, long timestamp) {
            this.type = type;
            this.severity = severity;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}