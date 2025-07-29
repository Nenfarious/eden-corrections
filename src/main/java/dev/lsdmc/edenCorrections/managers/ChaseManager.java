package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ChaseManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Combat timer tracking
    private final Map<UUID, Long> combatTimers;
    private final Map<UUID, BukkitTask> combatTasks;
    
    public ChaseManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.combatTimers = new HashMap<>();
        this.combatTasks = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("ChaseManager initialized successfully!");
        
        // Start chase monitoring task
        startChaseMonitoring();
    }
    
    public void shutdown() {
        logger.info("ChaseManager shutting down...");
        
        // End all active chases
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.plugin-shutdown"));
        }
        
        // Clean up combat timers
        cleanupAllCombatTimers();
        
        logger.info("ChaseManager shutdown complete");
    }
    
    private void startChaseMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check for expired chases and distance violations
                monitorActiveChases();
            }
        }.runTaskTimer(plugin, 20L * 10L, 20L * 10L); // Run every 10 seconds for better performance
    }
    
    private void monitorActiveChases() {
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            Player guard = plugin.getServer().getPlayer(chase.getGuardId());
            Player target = plugin.getServer().getPlayer(chase.getTargetId());
            
            // Check if players are still online
            if (guard == null || target == null) {
                endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.player-offline"));
                continue;
            }
            
            // Check distance
            double distance = guard.getLocation().distance(target.getLocation());
            if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
                endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.target-too-far"));
                continue;
            }
            
            // Check if target entered restricted area
            if (isPlayerInRestrictedArea(target)) {
                endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.target-restricted-area"));
                continue;
            }
            
            // Update boss bars
            plugin.getBossBarManager().updateChaseBossBar(guard, distance, target);
            plugin.getBossBarManager().updateChaseBossBar(target, distance, guard);
            
            // Send distance warnings
            if (distance > plugin.getConfigManager().getChaseWarningDistance()) {
                plugin.getMessageManager().sendMessage(guard, "chase.warnings.distance",
                    numberPlaceholder("distance", Math.round(distance)));
            }
        }
    }

    // === COMBAT TIMER METHODS ===
    
    public void handleCombatEvent(Player player) {
        if (!plugin.getConfigManager().shouldPreventCaptureInCombat()) {
            return; // Combat timer disabled
        }
        
        UUID playerId = player.getUniqueId();
        int duration = plugin.getConfigManager().getCombatTimerDuration();
        
        // Set combat timer
        combatTimers.put(playerId, System.currentTimeMillis() + (duration * 1000L));
        
        // Cancel existing combat task
        BukkitTask existingTask = combatTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Show combat timer boss bar
        plugin.getBossBarManager().showCombatBossBar(player, duration);
        
        // Send action bar notification
        plugin.getMessageManager().sendActionBar(player, "action-bar.combat-active");
        
        // Send combat message
        plugin.getMessageManager().sendMessage(player, "combat.timer-started");
        
        // Start countdown task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                endCombatTimer(player);
            }
        }.runTaskLater(plugin, duration * 20L);
        
        combatTasks.put(playerId, task);
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer started for " + player.getName() + " (" + duration + "s)");
        }
    }
    
    public void endCombatTimer(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Remove combat timer
        combatTimers.remove(playerId);
        
        // Cancel task
        BukkitTask task = combatTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        
        // Hide boss bar
        plugin.getBossBarManager().hideBossBarByType(player, "combat");
        
        // Send end message
        plugin.getMessageManager().sendMessage(player, "combat.timer-ended");
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("DEBUG: Combat timer ended for " + player.getName());
        }
    }
    
    public boolean isInCombat(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return false;
        
        if (System.currentTimeMillis() >= combatEnd) {
            // Timer expired, clean up
            endCombatTimer(player);
            return false;
        }
        
        return true;
    }
    
    public long getRemainingCombatTime(Player player) {
        Long combatEnd = combatTimers.get(player.getUniqueId());
        if (combatEnd == null) return 0;
        
        long remaining = combatEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000L);
        }

    // === ENHANCED CHASE METHODS ===
    
    public boolean startChase(Player guard, Player target) {
        // Validate chase can start
        if (!canStartChaseWithMessages(guard, target)) {
            return false;
        }
        
        // Check security restrictions
        if (!plugin.getSecurityManager().canPlayerBeChased(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.chase-protected");
            plugin.getSecurityManager().logSecurityViolation("start chase", guard, target);
            return false;
        }
        
        // Check safe zones
        if (plugin.getWorldGuardUtils().isPlayerInSafeZone(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.in-safe-zone");
            return false;
        }
        
        // Create chase data
        ChaseData chase = new ChaseData(guard.getUniqueId(), target.getUniqueId(), null, 0);
        plugin.getDataManager().addChaseData(chase);
        
        // Update player data
        PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
        PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
        
        targetData.setBeingChased(true);
        targetData.setChaserGuard(guard.getUniqueId());
        targetData.setChaseStartTime(System.currentTimeMillis());
        
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
        // Show boss bars
        double distance = guard.getLocation().distance(target.getLocation());
        plugin.getBossBarManager().showChaseGuardBossBar(guard, target, distance);
        plugin.getBossBarManager().showChaseTargetBossBar(target, guard, distance);
        
        // Send messages
        plugin.getMessageManager().sendMessage(guard, "chase.start.success", 
            playerPlaceholder("target", target));
        plugin.getMessageManager().sendMessage(target, "chase.start.target-notification", 
            playerPlaceholder("guard", guard));
        
        // Send guard alert
        plugin.getMessageManager().sendGuardAlert("chase.start.guard-alert",
            playerPlaceholder("guard", guard),
            playerPlaceholder("target", target));
        
        logger.info("Chase started: " + guard.getName() + " -> " + target.getName());
        return true;
    }
    
    public boolean endChase(UUID chaseId, String reason) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(chaseId);
        if (chase == null) return false;
        
        Player guard = plugin.getServer().getPlayer(chase.getGuardId());
        Player target = plugin.getServer().getPlayer(chase.getTargetId());
        
        // Clear player data
        if (guard != null) {
            PlayerData guardData = plugin.getDataManager().getPlayerData(guard.getUniqueId());
            if (guardData != null) {
                plugin.getDataManager().savePlayerData(guardData);
            }
        }
        
        if (target != null) {
            PlayerData targetData = plugin.getDataManager().getPlayerData(target.getUniqueId());
            if (targetData != null) {
                targetData.clearChaseData();
                plugin.getDataManager().savePlayerData(targetData);
            }
        }
        
        // Remove chase from data
        plugin.getDataManager().removeChaseData(chaseId);
        
        // Hide boss bars
        if (guard != null) {
            plugin.getBossBarManager().hideBossBarByType(guard, "chase_guard");
        }
        if (target != null) {
            plugin.getBossBarManager().hideBossBarByType(target, "chase_target");
        }
        
        // Send end messages
        if (guard != null) {
            plugin.getMessageManager().sendMessage(guard, "chase.end.success",
                stringPlaceholder("reason", reason));
        }
        
        if (target != null) {
            plugin.getMessageManager().sendMessage(target, "chase.end.target-notification",
                stringPlaceholder("reason", reason));
        }
        
        logger.info("Chase ended: " + chase.getGuardId() + " -> " + chase.getTargetId() + " (" + reason + ")");
        return true;
    }
    
    /**
     * Check if a chase can start between guard and target
     * @param guard The guard attempting to start the chase
     * @param target The target to be chased
     * @return true if chase can start, false otherwise
     */
    public boolean canStartChase(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return false;
        }
        
        // Check if target is wanted
        if (!plugin.getWantedManager().isWanted(target)) {
            return false;
        }
        
        // Check if target is in combat (enhanced restriction)
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            return false;
        }
        
        // Check if target is in restricted area
        if (isPlayerInRestrictedArea(target)) {
            return false;
        }
        
        // Check max concurrent chases
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            return false;
        }
        
        // Check if guard is already chasing someone
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            return false;
        }
        
        // Check if target is already being chased
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            return false;
        }
        
        // Check if trying to chase themselves
        if (guard.equals(target)) {
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            return false;
        }
        
        return true;
    }
    
    private boolean canStartChaseWithMessages(Player guard, Player target) {
        // Check if guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.not-on-duty");
            return false;
        }
        
        // Check if target is wanted
        if (!plugin.getWantedManager().isWanted(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-not-wanted");
            return false;
        }
        
        // Check if target is in combat (enhanced restriction)
        if (plugin.getConfigManager().shouldPreventChaseDuringCombat() && isInCombat(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-in-combat");
            return false;
        }
        
        // Check if target is in restricted area
        if (isPlayerInRestrictedArea(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.area-restricted");
            return false;
        }
        
        // Check max concurrent chases
        int activeChases = plugin.getDataManager().getAllActiveChases().size();
        if (activeChases >= plugin.getConfigManager().getMaxConcurrentChases()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.max-concurrent");
            return false;
        }
        
        // Check if guard is already chasing someone
        if (plugin.getDataManager().isGuardChasing(guard.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.already-chasing");
            return false;
        }
        
        // Check if target is already being chased
        if (plugin.getDataManager().isPlayerBeingChased(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.target-being-chased");
            return false;
        }
        
        // Check if trying to chase themselves
        if (guard.equals(target)) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.cannot-chase-self");
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxChaseDistance()) {
            plugin.getMessageManager().sendMessage(guard, "chase.restrictions.too-far");
            return false;
        }
        
        return true;
    }
    
    public boolean captureTarget(Player guard, Player target) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
        if (chase == null) {
            return false;
        }
        
        // End the chase
        endChase(chase.getChaseId(), plugin.getMessageManager().getPlainTextMessage("chase.end-reasons.target-captured"));
        
        // Start jail process
        return plugin.getJailManager().startJailCountdown(guard, target);
    }
    

    
    public boolean isPlayerInChase(Player player) {
        return plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) || 
               plugin.getDataManager().isGuardChasing(player.getUniqueId());
    }
    
    /**
     * Check if a player is in a restricted area for chases
     */
    public boolean isPlayerInRestrictedArea(Player player) {
        if (!plugin.getConfigManager().shouldBlockRestrictedAreas()) {
            return false;
        }
        
        String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
        return plugin.getWorldGuardUtils().isPlayerInAnyRegion(player, restrictedAreas);
    }
    
    public ChaseData getChaseByPlayer(Player player) {
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase != null) return chase;
        
        return plugin.getDataManager().getChaseByTarget(player.getUniqueId());
    }
    
    // === CLEANUP METHODS ===
    
    private void cleanupAllCombatTimers() {
        for (Map.Entry<UUID, BukkitTask> entry : combatTasks.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }
            
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                plugin.getBossBarManager().hideBossBarByType(player, "combat");
            }
        }
        
        combatTimers.clear();
        combatTasks.clear();
    }
    
    public void cleanupPlayer(Player player) {
        // End any combat timer
        endCombatTimer(player);
    }
} 
