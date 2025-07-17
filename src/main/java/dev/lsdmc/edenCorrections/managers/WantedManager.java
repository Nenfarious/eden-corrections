package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class WantedManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public WantedManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        logger.info("WantedManager initialized successfully!");
        
        // Start wanted level monitoring task
        startWantedMonitoring();
    }
    
    private void startWantedMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check for expired wanted levels
                plugin.getDataManager().cleanupExpiredWantedLevels();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Run every minute
    }
    
    public boolean setWantedLevel(Player target, int level, String reason) {
        // Guard protection: Guards on duty cannot be set as wanted
        if (plugin.getDutyManager().isOnDuty(target)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to set wanted level on guard on duty: " + target.getName());
            }
            return false;
        }
        
        return setWantedLevel(target.getUniqueId(), target.getName(), level, reason);
    }
    
    public boolean setWantedLevel(UUID targetId, String targetName, int level, String reason) {
        if (level < 0 || level > plugin.getConfigManager().getMaxWantedLevel()) {
            return false;
        }
        
        // Check if target is online and on duty
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null && plugin.getDutyManager().isOnDuty(targetPlayer)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to set wanted level on guard on duty: " + targetName);
            }
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(targetId, targetName);
        
        if (level == 0) {
            clearWantedLevel(targetId);
            return true;
        }
        
        data.setWantedLevel(level);
        data.setWantedExpireTime(System.currentTimeMillis() + plugin.getConfigManager().getWantedDuration() * 1000L);
        data.setWantedReason(reason);
        
        plugin.getDataManager().savePlayerData(data);
        
        // Notify online players
        if (targetPlayer != null) {
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.set",
                numberPlaceholder("level", level),
                starsPlaceholder("stars", level));
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.reason",
                stringPlaceholder("reason", reason));
        }
        
        logger.info(targetName + "'s wanted level set to " + level + " - Reason: " + reason);
        return true;
    }
    
    public boolean increaseWantedLevel(Player target, int amount, String reason) {
        // Guard protection: Guards on duty cannot have their wanted level increased
        if (plugin.getDutyManager().isOnDuty(target)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to increase wanted level on guard on duty: " + target.getName());
            }
            return false;
        }
        
        return increaseWantedLevel(target.getUniqueId(), target.getName(), amount, reason);
    }
    
    public boolean increaseWantedLevel(UUID targetId, String targetName, int amount, String reason) {
        // Check if target is online and on duty
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null && plugin.getDutyManager().isOnDuty(targetPlayer)) {
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Attempted to increase wanted level on guard on duty: " + targetName);
            }
            return false;
        }
        
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(targetId, targetName);
        
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + amount, plugin.getConfigManager().getMaxWantedLevel());
        
        if (newLevel == currentLevel) {
            return false; // No change
        }
        
        return setWantedLevel(targetId, targetName, newLevel, reason);
    }
    
    public boolean clearWantedLevel(Player target) {
        return clearWantedLevel(target.getUniqueId());
    }
    
    public boolean clearWantedLevel(UUID targetId) {
        PlayerData data = plugin.getDataManager().getPlayerData(targetId);
        if (data == null || !data.isWanted()) {
            return false;
        }
        
        data.clearWantedLevel();
        plugin.getDataManager().savePlayerData(data);
        
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null) {
            plugin.getMessageManager().sendMessage(targetPlayer, "wanted.level.cleared");
        }
        
        logger.info(data.getPlayerName() + "'s wanted level has been cleared");
        return true;
    }
    
    public int getWantedLevel(Player player) {
        return getWantedLevel(player.getUniqueId());
    }
    
    public int getWantedLevel(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getWantedLevel() : 0;
    }
    
    public boolean isWanted(Player player) {
        return isWanted(player.getUniqueId());
    }
    
    public boolean isWanted(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null && data.isWanted();
    }
    
    public long getRemainingWantedTime(Player player) {
        return getRemainingWantedTime(player.getUniqueId());
    }
    
    public long getRemainingWantedTime(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getRemainingWantedTime() : 0;
    }
    
    public String getWantedReason(Player player) {
        return getWantedReason(player.getUniqueId());
    }
    
    public String getWantedReason(UUID playerId) {
        PlayerData data = plugin.getDataManager().getPlayerData(playerId);
        return data != null ? data.getWantedReason() : "";
    }
    
    public void handlePlayerKillGuard(Player player, Player guard) {
        // Increase wanted level for killing a guard
        increaseWantedLevel(player, 2, "Killing a guard");
        
        // Notify other guards
        notifyGuards("wanted.alerts.guard-killed",
            playerPlaceholder("player", player),
            playerPlaceholder("guard", guard));
    }
    
    public void handlePlayerKillPlayer(Player player, Player victim) {
        // Increase wanted level for killing another player
        increaseWantedLevel(player, 1, "Killing another player");
    }
    
    public void handleContrabandViolation(Player player, String contrabandType) {
        // Increase wanted level for contraband violation
        increaseWantedLevel(player, 1, "Contraband violation: " + contrabandType);
    }
    
    public void handleChaseEscape(Player player) {
        // Increase wanted level for escaping a chase
        increaseWantedLevel(player, 1, "Escaping from chase");
    }
    
    public String getWantedStars(int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }
    
    private void notifyGuards(String messageKey, TagResolver... placeholders) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
                plugin.getMessageManager().sendGuardAlert(messageKey, placeholders);
            }
        }
    }
} 