package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Guard Tag Manager
 * 
 * Manages dynamic duty status tags for correctional officers.
 * Integrates with LuckPerms for prefix management and provides
 * configurable tag appearance and behavior.
 * 
 * @author EdenCorrections Team
 * @version 3.0 - Simplified and Fixed
 */
public class GuardTagManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final LuckPerms luckPerms;
    
    // Active guard tags tracking
    private final ConcurrentHashMap<UUID, String> activeGuardTags;
    
    // Configuration constants
    private int guardPrefixPriority;
    
    /**
     * Initialize the Guard Tag Manager
     * 
     * @param plugin The main plugin instance
     */
    public GuardTagManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.luckPerms = LuckPermsProvider.get();
        this.activeGuardTags = new ConcurrentHashMap<>();
        
        logger.info("GuardTagManager initialized with LuckPerms integration");
    }
    
    /**
     * Initialize the tag manager system
     */
    public void initialize() {
        try {
            loadConfiguration();
            
            if (!validateLuckPermsIntegration()) {
                logger.severe("LuckPerms integration validation failed - guard tags may not function properly");
                return;
            }
            
            // Clean up any existing guard tags on startup
            cleanupAllGuardTags();
            
            logger.info("GuardTagManager initialized successfully");
            
        } catch (Exception e) {
            logger.severe("Failed to initialize GuardTagManager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reload configuration from the config manager
     * This should be called when the configuration is reloaded
     */
    public void reloadConfiguration() {
        try {
            loadConfiguration();
            
            // Refresh all active guard tags with new configuration
            refreshAllActiveTags();
            
            logger.info("Guard tag configuration reloaded successfully");
        } catch (Exception e) {
            logger.severe("Failed to reload guard tag configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Refresh all active guard tags with current configuration
     */
    private void refreshAllActiveTags() {
        CompletableFuture.runAsync(() -> {
            try {
                int refreshedCount = 0;
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    if (activeGuardTags.containsKey(playerId)) {
                        // Get current player data
                        PlayerData playerData = plugin.getDataManager().getPlayerData(playerId);
                        if (playerData != null && playerData.isOnDuty()) {
                            // Remove old tag
                            removeGuardTagFromLuckPerms(player);
                            
                            // Create new tag with updated configuration
                            String newTag = createGuardTag(playerData);
                            addGuardTagToLuckPerms(player, newTag);
                            activeGuardTags.put(playerId, newTag);
                            
                            refreshedCount++;
                        }
                    }
                }
                
                final int finalRefreshedCount = refreshedCount;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (finalRefreshedCount > 0) {
                        logger.info("Refreshed " + finalRefreshedCount + " guard tags with new configuration");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("Error refreshing guard tags: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Load configuration values from the config manager
     */
    private void loadConfiguration() {
        guardPrefixPriority = plugin.getConfigManager().getGuardTagPriority();
        
        logger.info("Guard tag configuration loaded - Priority: " + guardPrefixPriority);
    }
    
    /**
     * Add a duty tag to a player
     * 
     * @param player The player going on duty
     * @param playerData The player's data containing guard information
     */
    public void addGuardTag(Player player, PlayerData playerData) {
        if (player == null || playerData == null) {
            logger.warning("Cannot add guard tag: player or playerData is null");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Check if player already has a guard tag and update if necessary
        if (activeGuardTags.containsKey(playerId)) {
            logger.fine("Player " + player.getName() + " already has a guard tag, updating...");
            removeGuardTag(player);
        }
        
        // Try UNT integration first (preferred method)
        if (plugin.getLuckPermsMetaManager() != null && plugin.getLuckPermsMetaManager().isAvailable()) {
            plugin.getLuckPermsMetaManager().setGuardTag(player, playerData)
                .thenAccept(success -> {
                    if (success) {
                        activeGuardTags.put(playerId, "UNT");
                        logger.info("Added UNT guard tag to " + player.getName() + " (Rank: " + playerData.getGuardRank() + ")");
                    } else {
                        logger.warning("UNT guard tag failed for " + player.getName() + " - falling back to LuckPerms");
                        // Fallback to LuckPerms if UNT fails
                        addLuckPermsGuardTag(player, playerData);
                    }
                })
                .exceptionally(throwable -> {
                    logger.warning("UNT guard tag error for " + player.getName() + ": " + throwable.getMessage());
                    // Fallback to LuckPerms on error
                    addLuckPermsGuardTag(player, playerData);
                    return null;
                });
        } else {
            // Fallback to LuckPerms prefix system
            addLuckPermsGuardTag(player, playerData);
        }
    }
    
    /**
     * Add guard tag using LuckPerms prefix system (fallback method)
     */
    private void addLuckPermsGuardTag(Player player, PlayerData playerData) {
        UUID playerId = player.getUniqueId();
        
        // Create the guard tag
        String guardTag = createGuardTag(playerData);
        
        // Add the tag asynchronously to avoid blocking the main thread
        CompletableFuture.runAsync(() -> {
            try {
                addGuardTagToLuckPerms(player, guardTag);
                activeGuardTags.put(playerId, guardTag);
                
                // Log success on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    logger.info("Added LuckPerms guard tag to " + player.getName() + " (Rank: " + playerData.getGuardRank() + ")");
                });
                
            } catch (Exception e) {
                logger.severe("Failed to add LuckPerms guard tag to " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Remove the duty tag from a player
     * 
     * @param player The player going off duty
     */
    public void removeGuardTag(Player player) {
        if (player == null) {
            logger.warning("Cannot remove guard tag: player is null");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Check if player has an active guard tag
        if (!activeGuardTags.containsKey(playerId)) {
            logger.fine("Player " + player.getName() + " does not have an active guard tag");
            return;
        }
        
        String tagType = activeGuardTags.get(playerId);
        
        // Handle UNT tag removal
        if ("LUCKPERMS".equals(tagType) && plugin.getLuckPermsMetaManager() != null && plugin.getLuckPermsMetaManager().isAvailable()) {
            plugin.getLuckPermsMetaManager().removeGuardTag(player)
                .thenAccept(success -> {
                    if (success) {
                        activeGuardTags.remove(playerId);
                        logger.info("Removed UNT guard tag from " + player.getName());
                    } else {
                        logger.warning("Failed to remove UNT guard tag from " + player.getName() + " - attempting LuckPerms cleanup");
                        removeLuckPermsGuardTag(player, playerId);
                    }
                })
                .exceptionally(throwable -> {
                    logger.warning("UNT guard tag removal error for " + player.getName() + ": " + throwable.getMessage());
                    removeLuckPermsGuardTag(player, playerId);
                    return null;
                });
        } else {
            // Handle LuckPerms tag removal (fallback or direct)
            removeLuckPermsGuardTag(player, playerId);
        }
    }
    
    /**
     * Remove guard tag using LuckPerms system
     */
    private void removeLuckPermsGuardTag(Player player, UUID playerId) {
        // Remove the tag asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                removeGuardTagFromLuckPerms(player);
                activeGuardTags.remove(playerId);
                
                // Log success on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    logger.info("Removed LuckPerms guard tag from " + player.getName());
                });
                
            } catch (Exception e) {
                logger.severe("Failed to remove LuckPerms guard tag from " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Update the guard tag for a player when their stats change
     * 
     * @param player The player to update
     * @param playerData The updated player data
     */
    public void updateGuardTag(Player player, PlayerData playerData) {
        if (player == null || playerData == null) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Only update if player has an active guard tag
        if (activeGuardTags.containsKey(playerId)) {
            String tagType = activeGuardTags.get(playerId);
            
            // Handle UNT tag update
                    if ("LUCKPERMS".equals(tagType) && plugin.getLuckPermsMetaManager() != null && plugin.getLuckPermsMetaManager().isAvailable()) {
            plugin.getLuckPermsMetaManager().updateGuardTag(player, playerData)
                    .thenAccept(success -> {
                        if (success) {
                            logger.fine("Updated UNT guard tag for " + player.getName());
                        } else {
                            logger.warning("Failed to update UNT guard tag for " + player.getName() + " - falling back to LuckPerms");
                            updateLuckPermsGuardTag(player, playerData);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.warning("UNT guard tag update error for " + player.getName() + ": " + throwable.getMessage());
                        updateLuckPermsGuardTag(player, playerData);
                        return null;
                    });
            } else {
                // Handle LuckPerms tag update
                updateLuckPermsGuardTag(player, playerData);
            }
        }
    }
    
    /**
     * Update guard tag using LuckPerms system
     */
    private void updateLuckPermsGuardTag(Player player, PlayerData playerData) {
        // Remove old tag and add new one with a small delay
        removeGuardTag(player);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                addLuckPermsGuardTag(player, playerData);
            }
        }.runTaskLater(plugin, 3L); // 3 tick delay for smooth transition
    }
    
    /**
     * Check if a player has an active guard tag
     * 
     * @param player The player to check
     * @return true if the player has an active guard tag
     */
    public boolean hasGuardTag(Player player) {
        return player != null && activeGuardTags.containsKey(player.getUniqueId());
    }
    
    /**
     * Get the current guard tag for a player
     * 
     * @param player The player to get the tag for
     * @return The guard tag string, or null if not found
     */
    public String getGuardTag(Player player) {
        return player != null ? activeGuardTags.get(player.getUniqueId()) : null;
    }
    
    /**
     * Create a guard tag with hover effect
     * 
     * @param playerData The player data containing guard information
     * @return The formatted guard tag string
     */
    private String createGuardTag(PlayerData playerData) {
        String rank = playerData.getGuardRank() != null ? playerData.getGuardRank() : "Officer";
        int arrests = playerData.getTotalArrests();
        int violations = playerData.getTotalViolations();
        long dutyTime = playerData.getTotalDutyTime() / 1000; // Convert to seconds
        
        // Format duty time
        String dutyTimeFormatted = formatDutyTime(dutyTime);
        
        // Get session stats
        int sessionSearches = playerData.getSessionSearches();
        int sessionArrests = playerData.getSessionArrests();
        int sessionDetections = playerData.getSessionDetections();
        
        // Get configuration values
        String tagFormat = plugin.getConfigManager().getGuardTagFormat();
        String hoverFormat = plugin.getConfigManager().getGuardTagHoverFormat();
        boolean showHover = plugin.getConfigManager().isGuardTagHoverEnabled();
        boolean showSessionStats = plugin.getConfigManager().isGuardTagShowSessionStats();
        boolean showTotalStats = plugin.getConfigManager().isGuardTagShowTotalStats();
        
        // Create hover text
        String hoverText = "";
        if (showHover) {
            if (showSessionStats && showTotalStats) {
                hoverText = String.format(hoverFormat,
                    rank, arrests, violations, dutyTimeFormatted,
                    sessionSearches, sessionArrests, sessionDetections
                );
            } else if (showTotalStats) {
                hoverText = String.format(
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "<gradient:#9D4EDD:#06FFA5><bold>CORRECTIONAL OFFICER</bold></gradient>\n" +
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "<gray>Rank: <aqua>%s\n" +
                    "<gray>Status: <green><bold>ON DUTY</bold></green>\n" +
                    "<gray>Total Arrests: <green>%d\n" +
                    "<gray>Total Violations: <red>%d\n" +
                    "<gray>Total Duty Time: <yellow>%s\n" +
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    rank, arrests, violations, dutyTimeFormatted
                );
            } else {
                hoverText = String.format(
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "<gradient:#9D4EDD:#06FFA5><bold>CORRECTIONAL OFFICER</bold></gradient>\n" +
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "<gray>Rank: <aqua>%s\n" +
                    "<gray>Status: <green><bold>ON DUTY</bold></green>\n" +
                    "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    rank
                );
            }
        }
        
        // Create the guard tag with hover effect
        if (showHover && !hoverText.isEmpty()) {
            return String.format(
                "<hover:show_text:'%s'>%s</hover>",
                hoverText, tagFormat
            );
        } else {
            // Return the tag format without extra spaces to prevent gray brackets
            return tagFormat;
        }
    }
    
    /**
     * Format duty time in a human-readable format
     * 
     * @param seconds Total duty time in seconds
     * @return Formatted time string
     */
    private String formatDutyTime(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " minute" + (minutes != 1 ? "s" : "");
            } else {
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " " + remainingSeconds + " second" + (remainingSeconds != 1 ? "s" : "");
            }
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            if (minutes == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "");
            } else {
                return hours + " hour" + (hours != 1 ? "s" : "") + " " + minutes + " minute" + (minutes != 1 ? "s" : "");
            }
        }
    }
    
    /**
     * Add the guard tag to LuckPerms as a prefix node
     * 
     * @param player The player to add the tag to
     * @param guardTag The guard tag string
     */
    private void addGuardTagToLuckPerms(Player player, String guardTag) {
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                logger.warning("Could not find LuckPerms user for " + player.getName());
                return;
            }
            
            // Create prefix node with high priority (no context)
            PrefixNode prefixNode = PrefixNode.builder(guardTag, guardPrefixPriority).build();
            
            // Add the node to the user
            user.data().add(prefixNode);
            
            // Save the user
            luckPerms.getUserManager().saveUser(user);
            
            logger.info("[GuardTagManager] Added guard tag to " + player.getName() + 
                       " with priority " + guardPrefixPriority);
            
        } catch (Exception e) {
            logger.severe("Error adding guard tag to LuckPerms for " + player.getName() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Remove the guard tag from LuckPerms
     * 
     * @param player The player to remove the tag from
     */
    private void removeGuardTagFromLuckPerms(Player player) {
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                logger.warning("Could not find LuckPerms user for " + player.getName());
                return;
            }
            
            // Remove all prefix nodes that match our guard tag patterns
            user.data().clear(node -> {
                if (node instanceof PrefixNode) {
                    PrefixNode prefixNode = (PrefixNode) node;
                    String prefixValue = prefixNode.getKey();
                    if (prefixValue != null) {
                        // Check for various guard tag patterns
                        String lowerValue = prefixValue.toLowerCase();
                        return lowerValue.contains("[on duty]") || 
                               lowerValue.contains("on duty") ||
                               lowerValue.contains("guard") ||
                               lowerValue.contains("officer") ||
                               lowerValue.contains("correctional") ||
                               lowerValue.contains("duty");
                    }
                }
                return false;
            });
            
            // Save the user
            luckPerms.getUserManager().saveUser(user);
            
            logger.fine("Successfully removed guard tag from LuckPerms for " + player.getName());
            
        } catch (Exception e) {
            logger.severe("Error removing guard tag from LuckPerms for " + player.getName() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Clean up all guard tags (useful for plugin reload/disable)
     */
    public void cleanupAllGuardTags() {
        logger.info("Cleaning up all guard tags...");
        
        CompletableFuture.runAsync(() -> {
            try {
                int removedCount = 0;
                
                // Iterate through all online players and remove guard tags
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (activeGuardTags.containsKey(player.getUniqueId())) {
                        removeGuardTagFromLuckPerms(player);
                        removedCount++;
                    }
                }
                
                // Clear the tracking map
                activeGuardTags.clear();
                
                final int finalRemovedCount = removedCount;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    logger.info("Cleaned up " + finalRemovedCount + " guard tags");
                });
                
            } catch (Exception e) {
                logger.severe("Error during guard tag cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Force cleanup of all guard-related prefixes for all online players
     * This is useful for fixing orphaned tags or bracket issues
     */
    public void forceCleanupAllGuardPrefixes() {
        logger.info("Force cleaning up all guard prefixes...");
        
        CompletableFuture.runAsync(() -> {
            try {
                int removedCount = 0;
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    try {
                        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                        if (user != null) {
                            // Remove any prefix that might be a guard tag
                            user.data().clear(node -> {
                                if (node instanceof PrefixNode) {
                                    PrefixNode prefixNode = (PrefixNode) node;
                                    String prefixValue = prefixNode.getKey();
                                    if (prefixValue != null) {
                                        String lowerValue = prefixValue.toLowerCase();
                                        return lowerValue.contains("[on duty]") || 
                                               lowerValue.contains("on duty") ||
                                               lowerValue.contains("guard") ||
                                               lowerValue.contains("officer") ||
                                               lowerValue.contains("correctional") ||
                                               lowerValue.contains("duty");
                                    }
                                }
                                return false;
                            });
                            
                            luckPerms.getUserManager().saveUser(user);
                            removedCount++;
                        }
                    } catch (Exception e) {
                        logger.warning("Error cleaning up prefixes for " + player.getName() + ": " + e.getMessage());
                    }
                }
                
                // Clear our tracking map
                activeGuardTags.clear();
                
                final int finalRemovedCount = removedCount;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    logger.info("Force cleaned up prefixes for " + finalRemovedCount + " players");
                });
                
            } catch (Exception e) {
                logger.severe("Error during force cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Clean up guard tag for a specific player (e.g., on player quit)
     * 
     * @param player The player to clean up
     */
    public void cleanupPlayer(Player player) {
        if (player != null) {
            removeGuardTag(player);
        }
    }
    
    /**
     * Restore guard tag for a player on join if they should have one
     * This fixes the server restart data confusion issue
     * 
     * @param player The player joining
     */
    public void restoreGuardTagOnJoin(Player player) {
        if (player == null) {
            return;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null && data.isOnDuty()) {
            // Player should be on duty, restore their guard tag
            logger.info("Restoring guard tag for " + player.getName() + " (was on duty before restart)");
            addGuardTag(player, data);
        }
    }
    
    /**
     * Validate LuckPerms integration
     * 
     * @return true if integration is working properly
     */
    private boolean validateLuckPermsIntegration() {
        try {
            if (luckPerms == null) {
                logger.severe("LuckPerms is not available");
                return false;
            }
            
            if (luckPerms.getUserManager() == null) {
                logger.severe("LuckPerms UserManager is not available");
                return false;
            }
            
            logger.info("LuckPerms integration validated successfully");
            return true;
            
        } catch (Exception e) {
            logger.severe("LuckPerms integration validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get diagnostic information for debugging
     * 
     * @return Map containing diagnostic information
     */
    public java.util.Map<String, Object> getDiagnosticInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("activeGuardTags", activeGuardTags.size());
        info.put("guardPrefixPriority", guardPrefixPriority);
        info.put("tagFormat", plugin.getConfigManager().getGuardTagFormat());
        info.put("hoverEnabled", plugin.getConfigManager().isGuardTagHoverEnabled());
        info.put("showSessionStats", plugin.getConfigManager().isGuardTagShowSessionStats());
        info.put("showTotalStats", plugin.getConfigManager().isGuardTagShowTotalStats());
        return info;
    }
    
    /**
     * Test LuckPerms integration
     * 
     * @return true if test passed
     */
    public boolean testLuckPermsIntegration() {
        return validateLuckPermsIntegration();
    }
    
    /**
     * Get the number of active guard tags
     * 
     * @return Number of active guard tags
     */
    public int getActiveGuardTagCount() {
        return activeGuardTags.size();
    }
    
    /**
     * Check if a player has a guard tag by UUID
     * 
     * @param playerId The player's UUID
     * @return true if the player has an active guard tag
     */
    public boolean hasGuardTag(UUID playerId) {
        return activeGuardTags.containsKey(playerId);
    }
} 