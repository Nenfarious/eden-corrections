package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class JailManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Active jail countdowns
    private final Map<UUID, BukkitTask> activeCountdowns;
    
    public JailManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeCountdowns = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("JailManager initialized successfully!");
    }
    
    public boolean startJailCountdown(Player guard, Player target) {
        if (activeCountdowns.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "jail.restrictions.already-active");
            return false;
        }
        
        // Get jail countdown time
        int countdownTime = plugin.getConfigManager().getJailCountdown();
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.countdown.started",
            playerPlaceholder("player", target),
            numberPlaceholder("seconds", countdownTime));
        plugin.getMessageManager().sendMessage(target, "jail.countdown.target-notification",
            numberPlaceholder("seconds", countdownTime));
        
        // Start countdown task
        BukkitTask countdownTask = new BukkitRunnable() {
            int remaining = countdownTime;
            
            @Override
            public void run() {
                // Check if both players are still online
                if (!guard.isOnline() || !target.isOnline()) {
                    cancelCountdown(target.getUniqueId(), "Player disconnected");
                    return;
                }
                
                // Check if guard is still close enough
                double distance = guard.getLocation().distance(target.getLocation());
                if (distance > 5.0) { // 5 blocks maximum distance
                    cancelCountdown(target.getUniqueId(), "Guard moved too far away");
                    plugin.getMessageManager().sendMessage(guard, "jail.countdown.cancelled",
                        stringPlaceholder("reason", "Guard moved too far away"));
                    return;
                }
                
                // Check if target moved (optional - depends on implementation)
                // This could be implemented by storing initial location and checking movement
                
                remaining--;
                
                if (remaining <= 0) {
                    // Complete the jail process
                    completeJail(guard, target);
                    cancel();
                    activeCountdowns.remove(target.getUniqueId());
                } else if (remaining <= 3) {
                    // Show countdown for last 3 seconds
                    plugin.getMessageManager().sendMessage(guard, "jail.countdown.progress",
                        numberPlaceholder("seconds", remaining));
                    plugin.getMessageManager().sendMessage(target, "jail.countdown.progress",
                        numberPlaceholder("seconds", remaining));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
        
        activeCountdowns.put(target.getUniqueId(), countdownTask);
        return true;
    }
    
    private void cancelCountdown(UUID targetId, String reason) {
        BukkitTask task = activeCountdowns.remove(targetId);
        if (task != null) {
            task.cancel();
        }
        
        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null) {
            plugin.getMessageManager().sendMessage(target, "jail.countdown.cancelled",
                stringPlaceholder("reason", reason));
        }
    }
    
    private void completeJail(Player guard, Player target) {
        // Calculate jail time based on wanted level
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        // Clear wanted level
        plugin.getWantedManager().clearWantedLevel(target);
        
        // Update statistics
        PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
        PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
        
        guardData.incrementArrests();
        targetData.incrementViolations();
        
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
        // Execute jail command (this would integrate with your jail plugin)
        executeJailCommand(guard, target, jailTime);
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
            playerPlaceholder("player", target),
            timePlaceholder("time", jailTime));
        plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
            timePlaceholder("time", jailTime));
        
        // Notify other guards
        notifyGuards("jail.arrest.guard-alert",
            playerPlaceholder("guard", guard),
            playerPlaceholder("target", target));
        
        // Award performance for successful arrest
        plugin.getDutyManager().awardArrestPerformance(guard);
        
        logger.info(guard.getName() + " arrested " + target.getName() + " for " + jailTime + " seconds");
    }
    
    private int calculateJailTime(int wantedLevel) {
        int baseTime = plugin.getConfigManager().getBaseJailTime();
        int levelMultiplier = plugin.getConfigManager().getJailLevelMultiplier();
        
        return baseTime + (wantedLevel * levelMultiplier);
    }
    
    private void executeJailCommand(Player guard, Player target, int jailTime) {
        // This would integrate with your jail plugin
        // For now, we'll use a placeholder command
        String jailCommand = "jail " + target.getName() + " " + jailTime + " Arrested by " + guard.getName();
        
        // Execute as console command
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
    }
    
    public boolean jailPlayer(Player guard, Player target, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = "No reason specified";
        }
        
        // Calculate jail time
        int wantedLevel = plugin.getWantedManager().getWantedLevel(target);
        int jailTime = calculateJailTime(wantedLevel);
        
        // Clear wanted level
        plugin.getWantedManager().clearWantedLevel(target);
        
        // Update statistics
        PlayerData guardData = plugin.getDataManager().getOrCreatePlayerData(guard.getUniqueId(), guard.getName());
        PlayerData targetData = plugin.getDataManager().getOrCreatePlayerData(target.getUniqueId(), target.getName());
        
        guardData.incrementArrests();
        targetData.incrementViolations();
        
        plugin.getDataManager().savePlayerData(guardData);
        plugin.getDataManager().savePlayerData(targetData);
        
        // Execute jail command
        String jailCommand = "jail " + target.getName() + " " + jailTime + " " + reason;
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
        
        // Notify players
        plugin.getMessageManager().sendMessage(guard, "jail.arrest.success",
            playerPlaceholder("player", target),
            timePlaceholder("time", jailTime));
        plugin.getMessageManager().sendMessage(target, "jail.arrest.target-notification",
            timePlaceholder("time", jailTime),
            stringPlaceholder("reason", reason));
        
        // Award performance for successful arrest
        plugin.getDutyManager().awardArrestPerformance(guard);
        
        logger.info(guard.getName() + " jailed " + target.getName() + " for " + jailTime + " seconds - Reason: " + reason);
        return true;
    }
    
    public boolean jailOfflinePlayer(String guardName, String targetName, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = "No reason specified";
        }
        
        // Use base jail time for offline players
        int jailTime = plugin.getConfigManager().getBaseJailTime();
        
        // Execute jail command
        String jailCommand = "jail " + targetName + " " + jailTime + " " + reason + " (Offline arrest by " + guardName + ")";
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), jailCommand);
        
        logger.info(guardName + " jailed offline player " + targetName + " for " + jailTime + " seconds - Reason: " + reason);
        return true;
    }
    
    public boolean isInJailCountdown(Player player) {
        return activeCountdowns.containsKey(player.getUniqueId());
    }
    
    public void cancelJailCountdown(Player player) {
        cancelCountdown(player.getUniqueId(), "Manually cancelled");
    }
    
    private void notifyGuards(String messageKey, TagResolver... placeholders) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
                plugin.getMessageManager().sendGuardAlert(messageKey, placeholders);
            }
        }
    }
} 