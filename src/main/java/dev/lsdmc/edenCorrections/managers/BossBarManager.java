package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

/**
 * Boss Bar Manager - Handles all boss bar displays with proper aesthetics
 * Provides centralized boss bar management with configuration support
 */
public class BossBarManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Active boss bars tracking
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, BukkitTask> bossBarTasks;
    private final Map<UUID, String> bossBarTypes;
    
    public BossBarManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeBossBars = new HashMap<>();
        this.bossBarTasks = new HashMap<>();
        this.bossBarTypes = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("BossBarManager initialized successfully!");
    }
    
    /**
     * Show wanted boss bar for a player
     */
    public void showWantedBossBar(Player player, int wantedLevel, long remainingTime) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isWantedBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.wanted.active",
                getBossBarColor(plugin.getConfigManager().getWantedBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getWantedBossBarOverlay()),
                player,
                numberPlaceholder("level", wantedLevel),
                starsPlaceholder("stars", wantedLevel),
                timePlaceholder("time", remainingTime)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "wanted");
            
            // Start update task
            startWantedBossBarUpdate(player, bossBar, wantedLevel, remainingTime);
            
        } catch (Exception e) {
            logger.warning("Error showing wanted boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show chase boss bar for target
     */
    public void showChaseTargetBossBar(Player target, Player guard, double distance) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isChaseBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(target);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.chase.target",
                getBossBarColor(plugin.getConfigManager().getChaseBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getChaseBossBarOverlay()),
                target,
                playerPlaceholder("guard", guard),
                distancePlaceholder("distance", distance)
            );
            
            // Show boss bar
            showBossBar(target, bossBar, "chase_target");
            
        } catch (Exception e) {
            logger.warning("Error showing chase target boss bar for " + target.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show chase boss bar for guard
     */
    public void showChaseGuardBossBar(Player guard, Player target, double distance) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isChaseBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(guard);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.chase.guard",
                getBossBarColor(plugin.getConfigManager().getChaseBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getChaseBossBarOverlay()),
                guard,
                playerPlaceholder("target", target),
                distancePlaceholder("distance", distance)
            );
            
            // Show boss bar
            showBossBar(guard, bossBar, "chase_guard");
            
        } catch (Exception e) {
            logger.warning("Error showing chase guard boss bar for " + guard.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show combat timer boss bar
     */
    public void showCombatBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isCombatBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.combat-timer",
                getBossBarColor(plugin.getConfigManager().getCombatBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getCombatBossBarOverlay()),
                player,
                timePlaceholder("time", duration)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "combat");
            
            // Start countdown
            startCountdownBossBar(player, bossBar, duration, "combat");
            
        } catch (Exception e) {
            logger.warning("Error showing combat boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show jail countdown boss bar
     */
    public void showJailBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isJailBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.jail-countdown",
                getBossBarColor(plugin.getConfigManager().getJailBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getJailBossBarOverlay()),
                player,
                timePlaceholder("time", duration)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "jail");
            
            // Start countdown
            startCountdownBossBar(player, bossBar, duration, "jail");
            
        } catch (Exception e) {
            logger.warning("Error showing jail boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show duty transition boss bar
     */
    public void showDutyBossBar(Player player, int duration, String rank) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isDutyBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.duty-transition",
                getBossBarColor(plugin.getConfigManager().getDutyBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getDutyBossBarOverlay()),
                player,
                timePlaceholder("time", duration),
                stringPlaceholder("rank", rank)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "duty");
            
            // Start countdown
            startCountdownBossBar(player, bossBar, duration, "duty");
            
        } catch (Exception e) {
            logger.warning("Error showing duty boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show contraband countdown boss bar
     */
    public void showContrabandBossBar(Player player, int duration, String description) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isContrabandBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.contraband-countdown",
                getBossBarColor(plugin.getConfigManager().getContrabandBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getContrabandBossBarOverlay()),
                player,
                timePlaceholder("time", duration),
                stringPlaceholder("description", description)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "contraband");
            
            // Start countdown
            startCountdownBossBar(player, bossBar, duration, "contraband");
            
        } catch (Exception e) {
            logger.warning("Error showing contraband boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show grace period boss bar
     */
    public void showGraceBossBar(Player player, int duration) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isGraceBossBarEnabled()) {
            return;
        }
        
        try {
            // Hide existing boss bar
            hideBossBar(player);
            
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.grace-period",
                getBossBarColor(plugin.getConfigManager().getGraceBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getGraceBossBarOverlay()),
                player,
                timePlaceholder("time", duration)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "grace");
            
            // Start countdown
            startCountdownBossBar(player, bossBar, duration, "grace");
            
        } catch (Exception e) {
            logger.warning("Error showing grace boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Show penalty boss bar for off-duty violations
     */
    public void showPenaltyBossBar(Player player, int stage, long overrunMinutes) {
        if (!plugin.getConfigManager().areBossBarsEnabled() || 
            !plugin.getConfigManager().isPenaltyBossBarEnabled()) {
            return;
        }
        
        try {
            // Create boss bar
            BossBar bossBar = createBossBar(
                "bossbar.penalty",
                getBossBarColor(plugin.getConfigManager().getPenaltyBossBarColor()),
                getBossBarOverlay(plugin.getConfigManager().getPenaltyBossBarOverlay()),
                player,
                numberPlaceholder("stage", stage),
                numberPlaceholder("overrun_minutes", overrunMinutes)
            );
            
            // Show boss bar
            showBossBar(player, bossBar, "penalty");
            
            // Start countdown if duration is configured
            int duration = plugin.getConfigManager().getPenaltyBossBarDuration();
            if (duration > 0) {
                startCountdownBossBar(player, bossBar, duration, "penalty");
            }
            
        } catch (Exception e) {
            logger.warning("Error showing penalty boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update chase boss bar distance
     */
    public void updateChaseBossBar(Player player, double distance, Player otherPlayer) {
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || (!type.equals("chase_target") && !type.equals("chase_guard"))) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            
            if (type.equals("chase_target")) {
                // Update target boss bar
                bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.chase.target",
                    playerPlaceholder("guard", otherPlayer),
                    distancePlaceholder("distance", distance)));
            } else {
                // Update guard boss bar
                bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.chase.guard",
                    playerPlaceholder("target", otherPlayer),
                    distancePlaceholder("distance", distance)));
            }
            
            // Update progress based on distance (closer = higher progress)
            float progress = Math.max(0.1f, Math.min(1.0f, 1.0f - (float)(distance / 100.0)));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating chase boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update wanted boss bar time
     */
    public void updateWantedBossBar(Player player, int wantedLevel, long remainingTime) {
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            return;
        }
        
        String type = bossBarTypes.get(player.getUniqueId());
        if (type == null || !type.equals("wanted")) {
            return;
        }
        
        try {
            BossBar bossBar = activeBossBars.get(player.getUniqueId());
            
            // Update title
            bossBar.name(plugin.getMessageManager().getMessage(player, "bossbar.wanted.active",
                numberPlaceholder("level", wantedLevel),
                starsPlaceholder("stars", wantedLevel),
                timePlaceholder("time", remainingTime)));
            
            // Update progress based on remaining time
            long totalTime = plugin.getConfigManager().getWantedDuration();
            float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingTime / totalTime));
            bossBar.progress(progress);
            
        } catch (Exception e) {
            logger.warning("Error updating wanted boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Hide boss bar for a player
     */
    public void hideBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        BukkitTask task = bossBarTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Hide boss bar
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        
        // Remove type tracking
        bossBarTypes.remove(playerId);
    }
    
    /**
     * Hide boss bar by type
     */
    public void hideBossBarByType(Player player, String type) {
        String currentType = bossBarTypes.get(player.getUniqueId());
        if (currentType != null && currentType.equals(type)) {
            hideBossBar(player);
        }
    }
    
    /**
     * Check if player has a boss bar
     */
    public boolean hasBossBar(Player player) {
        return activeBossBars.containsKey(player.getUniqueId());
    }
    
    /**
     * Get boss bar type for player
     */
    public String getBossBarType(Player player) {
        return bossBarTypes.get(player.getUniqueId());
    }
    
    // === PRIVATE HELPER METHODS ===
    
    private BossBar createBossBar(String messageKey, BossBar.Color color, BossBar.Overlay overlay, 
                                 Player player, TagResolver... placeholders) {
        return BossBar.bossBar(
            plugin.getMessageManager().getMessage(player, messageKey, placeholders),
            1.0f,
            color,
            overlay
        );
    }
    
    private void showBossBar(Player player, BossBar bossBar, String type) {
        UUID playerId = player.getUniqueId();
        
        // Store boss bar and type
        activeBossBars.put(playerId, bossBar);
        bossBarTypes.put(playerId, type);
        
        // Show to player
        player.showBossBar(bossBar);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Showed " + type + " boss bar to " + player.getName());
        }
    }
    
    private void startCountdownBossBar(Player player, BossBar bossBar, int duration, String type) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        BukkitTask existingTask = bossBarTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start countdown task
        BukkitTask task = new BukkitRunnable() {
            private int remaining = duration;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    hideBossBar(player);
                    return;
                }
                
                if (remaining <= 0) {
                    hideBossBar(player);
                    return;
                }
                
                try {
                    // Update progress
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / duration));
                    bossBar.progress(progress);
                    
                    remaining--;
                    
                } catch (Exception e) {
                    logger.warning("Error updating countdown boss bar for " + player.getName() + ": " + e.getMessage());
                    hideBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        bossBarTasks.put(playerId, task);
    }
    
    private void startWantedBossBarUpdate(Player player, BossBar bossBar, int wantedLevel, long remainingTime) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing task
        BukkitTask existingTask = bossBarTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Start update task
        BukkitTask task = new BukkitRunnable() {
            private long remaining = remainingTime;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    hideBossBar(player);
                    return;
                }
                
                if (remaining <= 0) {
                    hideBossBar(player);
                    return;
                }
                
                try {
                    // Update boss bar
                    updateWantedBossBar(player, wantedLevel, remaining);
                    
                    remaining--;
                    
                } catch (Exception e) {
                    logger.warning("Error updating wanted boss bar for " + player.getName() + ": " + e.getMessage());
                    hideBossBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        bossBarTasks.put(playerId, task);
    }
    
    private BossBar.Color getBossBarColor(String colorName) {
        try {
            return BossBar.Color.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid boss bar color: " + colorName + ", using RED");
            return BossBar.Color.RED;
        }
    }
    
    private BossBar.Overlay getBossBarOverlay(String overlayName) {
        try {
            return BossBar.Overlay.valueOf(overlayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid boss bar overlay: " + overlayName + ", using PROGRESS");
            return BossBar.Overlay.PROGRESS;
        }
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        // Hide all boss bars
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.hideBossBar(entry.getValue());
            }
        }
        
        // Cancel all tasks
        for (BukkitTask task : bossBarTasks.values()) {
            task.cancel();
        }
        
        // Clear maps
        activeBossBars.clear();
        bossBarTasks.clear();
        bossBarTypes.clear();
    }
    
    /**
     * Cleanup for specific player
     */
    public void cleanupPlayer(Player player) {
        hideBossBar(player);
    }
} 