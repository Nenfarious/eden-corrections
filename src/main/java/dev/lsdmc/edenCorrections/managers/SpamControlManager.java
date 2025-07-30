package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages spam control for commands and messages
 */
public class SpamControlManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Command cooldown tracking
    private final Map<UUID, Map<String, Long>> commandCooldowns;
    private final Map<UUID, Integer> commandSpamCount;
    
    // Message spam tracking
    private final Map<UUID, Long> lastMessageTime;
    private final Map<UUID, Integer> messageSpamCount;
    
    // Configuration
    private int commandCooldownTime = 1000; // 1 second default
    private int messageCooldownTime = 500;  // 0.5 seconds default
    private int maxSpamCount = 5;
    private int spamTimeout = 10000; // 10 seconds
    
    public SpamControlManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.commandCooldowns = new ConcurrentHashMap<>();
        this.commandSpamCount = new ConcurrentHashMap<>();
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.messageSpamCount = new ConcurrentHashMap<>();
    }
    
    public void initialize() {
        // Load configuration
        loadConfiguration();
        
        // Start cleanup task
        startCleanupTask();
        
        logger.info("SpamControlManager initialized");
    }
    
    private void loadConfiguration() {
        // Load from config if available
        commandCooldownTime = (Integer) plugin.getConfigManager().getConfigValue("spam-control.command-cooldown", 1000);
        messageCooldownTime = (Integer) plugin.getConfigManager().getConfigValue("spam-control.message-cooldown", 500);
        maxSpamCount = (Integer) plugin.getConfigManager().getConfigValue("spam-control.max-spam-count", 5);
        spamTimeout = (Integer) plugin.getConfigManager().getConfigValue("spam-control.spam-timeout", 10000);
    }
    
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // Clean up expired cooldowns
            commandCooldowns.entrySet().removeIf(entry -> {
                entry.getValue().entrySet().removeIf(cmdEntry -> 
                    currentTime - cmdEntry.getValue() > commandCooldownTime);
                return entry.getValue().isEmpty();
            });
            
            // Clean up spam counters
            commandSpamCount.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > spamTimeout);
            
            messageSpamCount.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > spamTimeout);
            
        }, 20L * 60L, 20L * 60L); // Run every minute
    }
    
    /**
     * Check if a player can execute a command
     */
    public boolean canExecuteCommand(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Get player's command cooldowns
        Map<String, Long> playerCooldowns = commandCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        
        // Check if command is on cooldown
        Long lastExecution = playerCooldowns.get(command);
        if (lastExecution != null && currentTime - lastExecution < commandCooldownTime) {
            // Increment spam counter
            int spamCount = commandSpamCount.getOrDefault(playerId, 0) + 1;
            commandSpamCount.put(playerId, spamCount);
            
            if (spamCount >= maxSpamCount) {
                plugin.getMessageManager().sendMessage(player, "spam-control.command-spam-warning");
                return false;
            }
            
            plugin.getMessageManager().sendMessage(player, "spam-control.command-cooldown",
                MessageManager.stringPlaceholder("command", command));
            return false;
        }
        
        // Reset spam counter if command is allowed
        commandSpamCount.remove(playerId);
        
        // Update cooldown
        playerCooldowns.put(command, currentTime);
        return true;
    }
    
    /**
     * Check if a player can send a message
     */
    public boolean canSendMessage(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check message cooldown
        Long lastMessage = lastMessageTime.get(playerId);
        if (lastMessage != null && currentTime - lastMessage < messageCooldownTime) {
            // Increment spam counter
            int spamCount = messageSpamCount.getOrDefault(playerId, 0) + 1;
            messageSpamCount.put(playerId, spamCount);
            
            if (spamCount >= maxSpamCount) {
                plugin.getMessageManager().sendMessage(player, "spam-control.message-spam-warning");
                return false;
            }
            
            plugin.getMessageManager().sendMessage(player, "spam-control.message-cooldown");
            return false;
        }
        
        // Reset spam counter if message is allowed
        messageSpamCount.remove(playerId);
        
        // Update last message time
        lastMessageTime.put(playerId, currentTime);
        return true;
    }
    
    /**
     * Force allow a command (bypass spam control)
     */
    public void forceAllowCommand(Player player, String command) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = commandCooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(command);
        }
        commandSpamCount.remove(playerId);
    }
    
    /**
     * Force allow a message (bypass spam control)
     */
    public void forceAllowMessage(Player player) {
        UUID playerId = player.getUniqueId();
        lastMessageTime.remove(playerId);
        messageSpamCount.remove(playerId);
    }
    
    /**
     * Get player's spam statistics
     */
    public Map<String, Object> getPlayerSpamStats(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("commandSpamCount", commandSpamCount.getOrDefault(playerId, 0));
        stats.put("messageSpamCount", messageSpamCount.getOrDefault(playerId, 0));
        stats.put("activeCooldowns", commandCooldowns.getOrDefault(playerId, new HashMap<>()).size());
        
        return stats;
    }
    
    /**
     * Reset all spam data for a player
     */
    public void resetPlayerSpamData(Player player) {
        UUID playerId = player.getUniqueId();
        commandCooldowns.remove(playerId);
        commandSpamCount.remove(playerId);
        lastMessageTime.remove(playerId);
        messageSpamCount.remove(playerId);
    }
    
    /**
     * Clean up all data
     */
    public void cleanup() {
        commandCooldowns.clear();
        commandSpamCount.clear();
        lastMessageTime.clear();
        messageSpamCount.clear();
    }
    
    /**
     * Clean up data for a specific player
     */
    public void cleanupPlayer(Player player) {
        resetPlayerSpamData(player);
    }
} 