package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * LuckPerms Meta Manager
 * 
 * Handles dynamic guard duty tags and wanted level indicators using LuckPerms prefix/suffix system.
 * Provides visual feedback without chat prefix pollution by using temporary meta nodes.
 * 
 * @author EdenCorrections Team
 * @version 1.0
 */
public class LuckPermsMetaManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final LuckPerms luckPerms;
    
    // Active tag tracking
    private final ConcurrentHashMap<UUID, String> activeGuardTags;
    private final ConcurrentHashMap<UUID, String> activeWantedTags;
    
    // Configuration cache
    private boolean guardTagsEnabled;
    private boolean wantedIndicatorsEnabled;
    private String guardTagFormat;
    private String wantedTagFormat;
    private int guardTagPriority;
    private int wantedTagPriority;
    private boolean usePrefix;
    private boolean useSuffix;
    
    public LuckPermsMetaManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeGuardTags = new ConcurrentHashMap<>();
        this.activeWantedTags = new ConcurrentHashMap<>();
        
        // Initialize LuckPerms API
        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("LuckPerms meta integration initialized successfully");
        } catch (IllegalStateException e) {
            logger.severe("LuckPerms not available - meta features disabled: " + e.getMessage());
            throw new RuntimeException("LuckPerms is required for meta features", e);
        }
    }
    
    public void initialize() {
        loadConfiguration();
        logger.info("LuckPerms meta manager initialized");
    }
    
    public void loadConfiguration() {
        guardTagsEnabled = plugin.getConfigManager().getConfig().getBoolean("integrations.luckperms.guard-tags.enabled", true);
        wantedIndicatorsEnabled = plugin.getConfigManager().getConfig().getBoolean("integrations.luckperms.wanted-indicators.enabled", true);
        guardTagFormat = plugin.getConfigManager().getConfig().getString("integrations.luckperms.guard-tags.format", "&7[&aGuard&7]");
        wantedTagFormat = plugin.getConfigManager().getConfig().getString("integrations.luckperms.wanted-indicators.format", "&4[WANTED]");
        guardTagPriority = plugin.getConfigManager().getConfig().getInt("integrations.luckperms.guard-tags.priority", 100);
        wantedTagPriority = plugin.getConfigManager().getConfig().getInt("integrations.luckperms.wanted-indicators.priority", 150);
        usePrefix = plugin.getConfigManager().getConfig().getBoolean("integrations.luckperms.use-prefix", true);
        useSuffix = plugin.getConfigManager().getConfig().getBoolean("integrations.luckperms.use-suffix", false);
        
        logger.info("LuckPerms meta configuration loaded - Guard tags: " + guardTagsEnabled + ", Wanted indicators: " + wantedIndicatorsEnabled);
    }
    
    /**
     * Set guard duty tag for a player
     */
    public CompletableFuture<Boolean> setGuardTag(Player player, PlayerData playerData) {
        if (!guardTagsEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null) {
                    return false;
                }
                
                String guardTag = createGuardTag(playerData);
                String cleanTag = plugin.getMessageManager().getPlainTextMessage("temp", 
                    plugin.getMessageManager().stringPlaceholder("temp", guardTag)).replace("temp", "");
                
                // Remove any existing guard tag first
                removeGuardTagSync(user);
                
                // Add new guard tag
                if (usePrefix) {
                    PrefixNode prefixNode = PrefixNode.builder(cleanTag, guardTagPriority).build();
                    user.data().add(prefixNode);
                } else if (useSuffix) {
                    SuffixNode suffixNode = SuffixNode.builder(cleanTag, guardTagPriority).build();
                    user.data().add(suffixNode);
                }
                
                // Save changes
                luckPerms.getUserManager().saveUser(user);
                
                // Track active tag
                activeGuardTags.put(player.getUniqueId(), guardTag);
                
                return true;
            } catch (Exception e) {
                logger.warning("Failed to set guard tag for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Set wanted tag for a player
     */
    public CompletableFuture<Boolean> setWantedTag(Player player, int wantedLevel, String reason) {
        if (!wantedIndicatorsEnabled || wantedLevel <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null) {
                    return false;
                }
                
                String wantedTag = createWantedTag(wantedLevel, reason);
                String cleanTag = plugin.getMessageManager().getPlainTextMessage("temp", 
                    plugin.getMessageManager().stringPlaceholder("temp", wantedTag)).replace("temp", "");
                
                // Remove any existing wanted tag first
                removeWantedTagSync(user);
                
                // Add new wanted tag
                if (usePrefix) {
                    PrefixNode prefixNode = PrefixNode.builder(cleanTag, wantedTagPriority).build();
                    user.data().add(prefixNode);
                } else if (useSuffix) {
                    SuffixNode suffixNode = SuffixNode.builder(cleanTag, wantedTagPriority).build();
                    user.data().add(suffixNode);
                }
                
                // Save changes
                luckPerms.getUserManager().saveUser(user);
                
                // Track active tag
                activeWantedTags.put(player.getUniqueId(), wantedTag);
                
                return true;
            } catch (Exception e) {
                logger.warning("Failed to set wanted tag for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Remove guard tag from a player
     */
    public CompletableFuture<Boolean> removeGuardTag(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null) {
                    return false;
                }
                
                removeGuardTagSync(user);
                luckPerms.getUserManager().saveUser(user);
                
                activeGuardTags.remove(player.getUniqueId());
                return true;
            } catch (Exception e) {
                logger.warning("Failed to remove guard tag for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Remove wanted tag from a player
     */
    public CompletableFuture<Boolean> removeWantedTag(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user == null) {
                    return false;
                }
                
                removeWantedTagSync(user);
                luckPerms.getUserManager().saveUser(user);
                
                activeWantedTags.remove(player.getUniqueId());
                return true;
            } catch (Exception e) {
                logger.warning("Failed to remove wanted tag for " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Update guard tag with new information
     */
    public CompletableFuture<Boolean> updateGuardTag(Player player, PlayerData playerData) {
        if (!hasGuardTag(player)) {
            return setGuardTag(player, playerData);
        }
        
        return setGuardTag(player, playerData); // Just overwrite with new data
    }
    
    /**
     * Update wanted tag with new level/reason
     */
    public CompletableFuture<Boolean> updateWantedTag(Player player, int wantedLevel, String reason) {
        if (wantedLevel <= 0) {
            return removeWantedTag(player);
        }
        
        return setWantedTag(player, wantedLevel, reason);
    }
    
    /**
     * Check if player has active guard tag
     */
    public boolean hasGuardTag(Player player) {
        return activeGuardTags.containsKey(player.getUniqueId());
    }
    
    /**
     * Check if player has active wanted tag
     */
    public boolean hasWantedTag(Player player) {
        return activeWantedTags.containsKey(player.getUniqueId());
    }
    
    /**
     * Clean up all tags for a player
     */
    public CompletableFuture<Void> cleanupPlayer(Player player) {
        return CompletableFuture.allOf(
            removeGuardTag(player).thenApply(result -> null),
            removeWantedTag(player).thenApply(result -> null)
        );
    }
    
    /**
     * Clean up all active tags
     */
    public CompletableFuture<Void> cleanupAllTags() {
        return CompletableFuture.runAsync(() -> {
            // Clean up all active guard tags
            for (UUID playerId : activeGuardTags.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    removeGuardTag(player);
                } else {
                    // Player is offline, clean up directly
                    User user = luckPerms.getUserManager().getUser(playerId);
                    if (user != null) {
                        removeGuardTagSync(user);
                        luckPerms.getUserManager().saveUser(user);
                    }
                }
            }
            
            // Clean up all active wanted tags
            for (UUID playerId : activeWantedTags.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    removeWantedTag(player);
                } else {
                    // Player is offline, clean up directly
                    User user = luckPerms.getUserManager().getUser(playerId);
                    if (user != null) {
                        removeWantedTagSync(user);
                        luckPerms.getUserManager().saveUser(user);
                    }
                }
            }
            
            activeGuardTags.clear();
            activeWantedTags.clear();
            logger.info("Cleaned up all LuckPerms meta tags");
        });
    }
    
    private void removeGuardTagSync(User user) {
        // Remove any prefix/suffix nodes that match our guard tag pattern
        user.data().clear(node -> {
            if (node instanceof PrefixNode && usePrefix) {
                return ((PrefixNode) node).getPriority() == guardTagPriority;
            } else if (node instanceof SuffixNode && useSuffix) {
                return ((SuffixNode) node).getPriority() == guardTagPriority;
            }
            return false;
        });
    }
    
    private void removeWantedTagSync(User user) {
        // Remove any prefix/suffix nodes that match our wanted tag pattern
        user.data().clear(node -> {
            if (node instanceof PrefixNode && usePrefix) {
                return ((PrefixNode) node).getPriority() == wantedTagPriority;
            } else if (node instanceof SuffixNode && useSuffix) {
                return ((SuffixNode) node).getPriority() == wantedTagPriority;
            }
            return false;
        });
    }
    
    private String createGuardTag(PlayerData playerData) {
        String format = guardTagFormat
            .replace("{rank}", playerData.getGuardRank() != null ? playerData.getGuardRank() : "Guard")
            .replace("{duty_time}", formatDutyTime(playerData.getTotalDutyTime()))
            .replace("{arrests}", String.valueOf(playerData.getTotalArrests()));
        
        return format;
    }
    
    private String createWantedTag(int wantedLevel, String reason) {
        String stars = "★".repeat(Math.min(wantedLevel, 5));
        return wantedTagFormat
            .replace("{level}", String.valueOf(wantedLevel))
            .replace("{stars}", stars)
            .replace("{reason}", reason != null ? reason : "Unknown");
    }
    
    private String formatDutyTime(long seconds) {
        if (seconds <= 0) return "0m";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h" + (minutes > 0 ? minutes + "m" : "");
        } else {
            return minutes + "m";
        }
    }
    
    public boolean isAvailable() {
        return luckPerms != null;
    }
    
    public boolean testIntegration() {
        try {
            // Test basic LuckPerms functionality
            return luckPerms.getUserManager() != null;
        } catch (Exception e) {
            logger.warning("LuckPerms integration test failed: " + e.getMessage());
            return false;
        }
    }
    
    public String getDiagnosticInfo() {
        return String.format(
            "LuckPerms Meta Integration:\n" +
            "  Available: %s\n" +
            "  Guard Tags Enabled: %s\n" +
            "  Wanted Indicators Enabled: %s\n" +
            "  Active Guard Tags: %d\n" +
            "  Active Wanted Tags: %d\n" +
            "  Use Prefix: %s\n" +
            "  Use Suffix: %s",
            isAvailable(),
            guardTagsEnabled,
            wantedIndicatorsEnabled,
            activeGuardTags.size(),
            activeWantedTags.size(),
            usePrefix,
            useSuffix
        );
    }
    
    public int getActiveGuardTagCount() {
        return activeGuardTags.size();
    }
    
    public int getActiveWantedTagCount() {
        return activeWantedTags.size();
    }
    
    public void cleanup() {
        if (isAvailable()) {
            cleanupAllTags();
        }
    }
}