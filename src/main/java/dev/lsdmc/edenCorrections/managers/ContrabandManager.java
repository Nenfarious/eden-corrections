package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ContrabandManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Active contraband requests
    private final Map<UUID, ContrabandRequest> activeRequests;
    
    public ContrabandManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeRequests = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("ContrabandManager initialized successfully!");
    }
    
    // === CONTRABAND REQUEST METHODS ===
    
    public boolean requestContraband(Player guard, Player target, String contrabandType) {
        // Validate guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return false;
        }
        
        // Check if contraband system is enabled
        if (!plugin.getConfigManager().isContrabandEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return false;
        }
        
        // Security check: Can target be contraband targeted?
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("contraband request", guard, target);
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxRequestDistance()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
            return false;
        }
        
        // Check if target already has active request
        if (activeRequests.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.already-active");
            return false;
        }
        
        // Get contraband configuration
        String itemsConfig = plugin.getConfigManager().getContrabandItems(contrabandType);
        String description = plugin.getConfigManager().getContrabandDescription(contrabandType);
        
        if (itemsConfig == null || itemsConfig.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return false;
        }
        
        // Parse target items
        List<Material> targetItems = parseContrabandItems(itemsConfig);
        if (targetItems.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return false;
        }
        
        // Award performance for initiating search
        plugin.getDutyManager().awardSearchPerformance(guard);
        
        // Start contraband request
        return startContrabandRequest(guard, target, contrabandType, description, targetItems);
    }
    
    private boolean startContrabandRequest(Player guard, Player target, String type, String description, List<Material> targetItems) {
        int timeout = plugin.getConfigManager().getContrabandCompliance();
        
        ContrabandRequest request = new ContrabandRequest(
            guard.getUniqueId(),
            target.getUniqueId(),
            type,
            description,
            targetItems,
            System.currentTimeMillis(),
            timeout
        );
        
        activeRequests.put(target.getUniqueId(), request);
        
        // Send messages
        plugin.getMessageManager().sendMessage(guard, "contraband.request.success",
            playerPlaceholder("player", target),
            stringPlaceholder("description", description));
        
        plugin.getMessageManager().sendMessage(target, "contraband.request.target-notification",
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        // Show enhanced countdown boss bar with better styling
        plugin.getBossBarManager().showContrabandBossBar(target, timeout, description);
        
        // Send action bar notification
        plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-request",
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        // Start timeout task
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            handleContrabandTimeout(request);
        }, timeout * 20L);
        
        request.setTimeoutTask(timeoutTask);
        
        logger.info("Contraband request started: " + guard.getName() + " -> " + target.getName() + " (" + type + ")");
        return true;
    }
    
    private List<Material> parseContrabandItems(String itemsConfig) {
        List<Material> materials = new ArrayList<>();
        String[] items = itemsConfig.split(",");
        
        for (String item : items) {
            try {
                Material material = Material.valueOf(item.trim().toUpperCase());
                materials.add(material);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid material in contraband config: " + item);
            }
        }
        
        return materials;
    }
    
    // === CONTRABAND COMPLIANCE HANDLING ===
    
    public void handleItemDrop(Player player, ItemStack droppedItem) {
        ContrabandRequest request = activeRequests.get(player.getUniqueId());
        if (request == null) return;
        
        Material droppedMaterial = droppedItem.getType();
        
        // Check if dropped item matches contraband request
        if (request.getTargetItems().contains(droppedMaterial)) {
            request.addDroppedItem(droppedMaterial);
            
            // Send confirmation
            plugin.getMessageManager().sendMessage(player, "contraband.detection.item-dropped",
                stringPlaceholder("item", droppedMaterial.name()));
            
            // Check if all required items have been dropped
            if (request.isCompliant()) {
                handleContrabandCompliance(request, true);
            }
        } else {
            // Wrong item dropped - send warning but don't fail yet
            plugin.getMessageManager().sendMessage(player, "contraband.detection.wrong-item",
                stringPlaceholder("item", droppedMaterial.name()));
        }
    }
    
    private void handleContrabandCompliance(ContrabandRequest request, boolean compliant) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        // Remove request
        activeRequests.remove(request.getTargetId());
        
        // Cancel timeout task
        if (request.getTimeoutTask() != null) {
            request.getTimeoutTask().cancel();
        }
        
        if (target != null) {
            // Hide boss bar
            plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            
            if (compliant) {
                // Success - player complied and dropped contraband
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-success");
                plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-compliance");
                
                // Player is free to go - no wanted level increase
                logger.info("Contraband compliance successful: " + target.getName() + " dropped " + request.getDescription());
            } else {
                // Failed - player refused to drop contraband or timeout occurred
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-failed");
                
                // Increase wanted level for contraband possession
                increaseWantedLevel(target, "Contraband possession: " + request.getDescription());
                
                // Immediately start chase
                if (guard != null) {
                    startChaseAfterContrabandViolation(guard, target, request);
                }
            }
        }
        
        if (guard != null) {
            if (compliant) {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.request-completed");
                // Award performance for successful search
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            } else {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.timeout");
                // Award performance for finding contraband violation
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            }
        }
        
        logger.info("Contraband request completed: " + (compliant ? "SUCCESS" : "FAILED") + 
                   " - " + request.getType());
    }
    
    private void handleContrabandTimeout(ContrabandRequest request) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        if (target != null) {
            // Check if player still has contraband items
            boolean hasContraband = false;
            for (Material material : request.getTargetItems()) {
                if (target.getInventory().contains(material)) {
                    hasContraband = true;
                    break;
                }
            }
            
            if (hasContraband) {
                // Player still has contraband - violation
                handleContrabandCompliance(request, false);
            } else {
                // Player doesn't have items anymore (dropped or consumed) - compliance
                handleContrabandCompliance(request, true);
            }
        } else {
            // Player offline - remove request
            activeRequests.remove(request.getTargetId());
        }
    }
    
    private void startChaseAfterContrabandViolation(Player guard, Player target, ContrabandRequest request) {
        // Check if chase can start
        if (!plugin.getChaseManager().canStartChase(guard, target)) {
            // If chase can't start, just increase wanted level
            logger.info("Chase could not start after contraband violation for " + target.getName());
            return;
        }
        
        // Start chase immediately
        boolean chaseStarted = plugin.getChaseManager().startChase(guard, target);
        
        if (chaseStarted) {
            plugin.getMessageManager().sendMessage(guard, "contraband.chase.started",
                playerPlaceholder("target", target),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            plugin.getMessageManager().sendMessage(target, "contraband.chase.target-notification",
                playerPlaceholder("guard", guard),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            logger.info("Chase started after contraband violation: " + guard.getName() + " -> " + target.getName());
        }
    }
    
    private void increaseWantedLevel(Player player, String reason) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + 1, plugin.getConfigManager().getMaxWantedLevel());
        
        // Set new wanted level
        plugin.getWantedManager().setWantedLevel(player, newLevel, reason);
        
        // Apply glow effect for wanted level 3+
        if (newLevel >= 3) {
            applyWantedGlowEffect(player, true);
        }
    }
    
    private void applyWantedGlowEffect(Player player, boolean shouldGlow) {
        if (shouldGlow) {
            // Make player glow red for guards on duty
            player.setGlowing(true);
            
            // Send notification to guards
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().isOnDuty(onlinePlayer) && 
                    plugin.getDutyManager().hasGuardPermission(onlinePlayer)) {
                    plugin.getMessageManager().sendMessage(onlinePlayer, "wanted.glow.notification",
                        playerPlaceholder("player", player),
                        numberPlaceholder("level", plugin.getWantedManager().getWantedLevel(player)));
                }
            }
        } else {
            player.setGlowing(false);
        }
    }
    
    // === CONTRABAND DETECTION METHODS ===
    
    public void performDrugTest(Player guard, Player target) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return;
        }
        
        if (!plugin.getConfigManager().isDrugDetectionEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return;
        }
        
        // Security check: Can target be contraband targeted?
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("drug test", guard, target);
            return;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxRequestDistance()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
            return;
        }
        
        // Check for drug items in inventory
        String[] drugItems = plugin.getConfigManager().getContrabandItems("drugs").split(",");
        boolean foundDrugs = false;
        String foundDrug = "";
        
        for (String drugItem : drugItems) {
            try {
                Material drugMaterial = Material.valueOf(drugItem.trim().toUpperCase());
                if (target.getInventory().contains(drugMaterial)) {
                    foundDrugs = true;
                    foundDrug = drugMaterial.name();
                    break;
                }
            } catch (IllegalArgumentException e) {
                // Invalid material, skip
            }
        }
        
        // Send results
        plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.kit-used",
            playerPlaceholder("player", target));
        
        if (foundDrugs) {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            
            // Increase wanted level
            increaseWantedLevel(target, "Positive drug test: " + foundDrug);
            
            // Award performance for successful detection
            plugin.getDutyManager().awardDetectionPerformance(guard);
        } else {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.negative");
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.negative");
        }
        
        logger.info("Drug test performed: " + guard.getName() + " -> " + target.getName() + 
                   " (Result: " + (foundDrugs ? "POSITIVE" : "NEGATIVE") + ")");
    }
    
    // === CONTRABAND REMOVAL METHODS ===
    
    /**
     * Remove contraband items from player when they are caught/jailed
     * This is the only time contraband should be automatically taken
     */
    public void removeContrabandOnCapture(Player target) {
        // Get all contraband types from config
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs"};
        
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig != null && !itemsConfig.isEmpty()) {
                List<Material> contrabandItems = parseContrabandItems(itemsConfig);
                
                for (Material material : contrabandItems) {
                    target.getInventory().remove(material);
                }
            }
        }
        
        plugin.getMessageManager().sendMessage(target, "contraband.removal.captured");
        logger.info("Contraband removed from " + target.getName() + " upon capture");
    }
    
    // === UTILITY METHODS ===
    
    public boolean hasActiveRequest(Player player) {
        return activeRequests.containsKey(player.getUniqueId());
    }
    
    public ContrabandRequest getActiveRequest(Player player) {
        return activeRequests.get(player.getUniqueId());
    }
    
    public void cancelActiveRequest(Player player) {
        ContrabandRequest request = activeRequests.remove(player.getUniqueId());
        if (request != null) {
            // Cancel timeout task
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            // Hide boss bar
            plugin.getBossBarManager().hideBossBarByType(player, "contraband");
            
            logger.info("Contraband request cancelled for " + player.getName());
        }
    }
    
    /**
     * Check if a player has any contraband items
     */
    public boolean hasContrabandItems(Player player) {
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs"};
        
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig != null && !itemsConfig.isEmpty()) {
                List<Material> contrabandItems = parseContrabandItems(itemsConfig);
                
                for (Material material : contrabandItems) {
                    if (player.getInventory().contains(material)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    // === CLEANUP METHODS ===
    
    public void cleanup() {
        for (ContrabandRequest request : activeRequests.values()) {
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            Player target = Bukkit.getPlayer(request.getTargetId());
            if (target != null) {
                plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            }
        }
        
        activeRequests.clear();
        logger.info("ContrabandManager cleaned up successfully");
    }
    
    public void cleanupPlayer(Player player) {
        cancelActiveRequest(player);
        
        // Remove glow effect if player was wanted
        if (plugin.getWantedManager().getWantedLevel(player) >= 3) {
            applyWantedGlowEffect(player, false);
        }
    }
    
    // === CONTRABAND REQUEST CLASS ===
    
    public static class ContrabandRequest {
        private final UUID guardId;
        private final UUID targetId;
        private final String type;
        private final String description;
        private final List<Material> targetItems;
        private final long startTime;
        private final int timeout;
        private final List<Material> droppedItems;
        private BukkitTask timeoutTask;
        
        public ContrabandRequest(UUID guardId, UUID targetId, String type, String description,
                               List<Material> targetItems, long startTime, int timeout) {
            this.guardId = guardId;
            this.targetId = targetId;
            this.type = type;
            this.description = description;
            this.targetItems = new ArrayList<>(targetItems);
            this.startTime = startTime;
            this.timeout = timeout;
            this.droppedItems = new ArrayList<>();
        }
        
        // Getters
        public UUID getGuardId() { return guardId; }
        public UUID getTargetId() { return targetId; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public List<Material> getTargetItems() { return targetItems; }
        public long getStartTime() { return startTime; }
        public int getTimeout() { return timeout; }
        public List<Material> getDroppedItems() { return droppedItems; }
        public BukkitTask getTimeoutTask() { return timeoutTask; }
        
        // Setters
        public void setTimeoutTask(BukkitTask timeoutTask) { this.timeoutTask = timeoutTask; }
        
        // Utility methods
        public void addDroppedItem(Material material) {
            if (!droppedItems.contains(material)) {
                droppedItems.add(material);
            }
        }
        
        public boolean isCompliant() {
            // Check if all required item types have been dropped
            for (Material requiredItem : targetItems) {
                if (!droppedItems.contains(requiredItem)) {
                    return false;
                }
            }
            return true;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public int getRemainingTime() {
            long elapsed = getElapsedTime() / 1000L;
            return Math.max(0, timeout - (int) elapsed);
        }
        
        @Override
        public String toString() {
            return "ContrabandRequest{" +
                    "guardId=" + guardId +
                    ", targetId=" + targetId +
                    ", type='" + type + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
=======
package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class ContrabandManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Active contraband requests
    private final Map<UUID, ContrabandRequest> activeRequests;
    
    public ContrabandManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.activeRequests = new HashMap<>();
    }
    
    public void initialize() {
        logger.info("ContrabandManager initialized successfully!");
    }
    
    // === CONTRABAND REQUEST METHODS ===
    
    public boolean requestContraband(Player guard, Player target, String contrabandType) {
        // Validate guard is on duty
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return false;
        }
        
        // Check if contraband system is enabled
        if (!plugin.getConfigManager().isContrabandEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return false;
        }
        
        // Security check: Can target be contraband targeted?
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("contraband request", guard, target);
            return false;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxRequestDistance()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
            return false;
        }
        
        // Check if target already has active request
        if (activeRequests.containsKey(target.getUniqueId())) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.already-active");
            return false;
        }
        
        // Get contraband configuration
        String itemsConfig = plugin.getConfigManager().getContrabandItems(contrabandType);
        String description = plugin.getConfigManager().getContrabandDescription(contrabandType);
        
        if (itemsConfig == null || itemsConfig.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return false;
        }
        
        // Parse target items
        List<Material> targetItems = parseContrabandItems(itemsConfig);
        if (targetItems.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return false;
        }
        
        // Award performance for initiating search
        plugin.getDutyManager().awardSearchPerformance(guard);
        
        // Start contraband request
        return startContrabandRequest(guard, target, contrabandType, description, targetItems);
    }
    
    private boolean startContrabandRequest(Player guard, Player target, String type, String description, List<Material> targetItems) {
        int timeout = plugin.getConfigManager().getContrabandCompliance();
        
        ContrabandRequest request = new ContrabandRequest(
            guard.getUniqueId(),
            target.getUniqueId(),
            type,
            description,
            targetItems,
            System.currentTimeMillis(),
            timeout
        );
        
        activeRequests.put(target.getUniqueId(), request);
        
        // Send messages
        plugin.getMessageManager().sendMessage(guard, "contraband.request.success",
            playerPlaceholder("player", target),
            stringPlaceholder("description", description));
        
        plugin.getMessageManager().sendMessage(target, "contraband.request.target-notification",
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        // Show enhanced countdown boss bar with better styling
        plugin.getBossBarManager().showContrabandBossBar(target, timeout, description);
        
        // Send action bar notification
        plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-request",
            stringPlaceholder("description", description),
            numberPlaceholder("seconds", timeout));
        
        // Start timeout task
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            handleContrabandTimeout(request);
        }, timeout * 20L);
        
        request.setTimeoutTask(timeoutTask);
        
        logger.info("Contraband request started: " + guard.getName() + " -> " + target.getName() + " (" + type + ")");
        return true;
    }
    
    private List<Material> parseContrabandItems(String itemsConfig) {
        List<Material> materials = new ArrayList<>();
        String[] items = itemsConfig.split(",");
        
        for (String item : items) {
            try {
                Material material = Material.valueOf(item.trim().toUpperCase());
                materials.add(material);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid material in contraband config: " + item);
            }
        }
        
        return materials;
    }
    
    // === CONTRABAND COMPLIANCE HANDLING ===
    
    public void handleItemDrop(Player player, ItemStack droppedItem) {
        ContrabandRequest request = activeRequests.get(player.getUniqueId());
        if (request == null) return;
        
        Material droppedMaterial = droppedItem.getType();
        
        // Check if dropped item matches contraband request
        if (request.getTargetItems().contains(droppedMaterial)) {
            request.addDroppedItem(droppedMaterial);
            
            // Send confirmation
            plugin.getMessageManager().sendMessage(player, "contraband.detection.item-dropped",
                stringPlaceholder("item", droppedMaterial.name()));
            
            // Check if all required items have been dropped
            if (request.isCompliant()) {
                handleContrabandCompliance(request, true);
            }
        } else {
            // Wrong item dropped - send warning but don't fail yet
            plugin.getMessageManager().sendMessage(player, "contraband.detection.wrong-item",
                stringPlaceholder("item", droppedMaterial.name()));
        }
    }
    
    private void handleContrabandCompliance(ContrabandRequest request, boolean compliant) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        // Remove request
        activeRequests.remove(request.getTargetId());
        
        // Cancel timeout task
        if (request.getTimeoutTask() != null) {
            request.getTimeoutTask().cancel();
        }
        
        if (target != null) {
            // Hide boss bar
            plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            
            if (compliant) {
                // Success - player complied and dropped contraband
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-success");
                plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-compliance");
                
                // Player is free to go - no wanted level increase
                logger.info("Contraband compliance successful: " + target.getName() + " dropped " + request.getDescription());
            } else {
                // Failed - player refused to drop contraband or timeout occurred
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-failed");
                
                // Increase wanted level for contraband possession
                increaseWantedLevel(target, "Contraband possession: " + request.getDescription());
                
                // Immediately start chase
                if (guard != null) {
                    startChaseAfterContrabandViolation(guard, target, request);
                }
            }
        }
        
        if (guard != null) {
            if (compliant) {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.request-completed");
                // Award performance for successful search
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            } else {
                plugin.getMessageManager().sendMessage(guard, "contraband.detection.timeout");
                // Award performance for finding contraband violation
                plugin.getDutyManager().awardSuccessfulSearchPerformance(guard);
            }
        }
        
        logger.info("Contraband request completed: " + (compliant ? "SUCCESS" : "FAILED") + 
                   " - " + request.getType());
    }
    
    private void handleContrabandTimeout(ContrabandRequest request) {
        Player target = Bukkit.getPlayer(request.getTargetId());
        Player guard = Bukkit.getPlayer(request.getGuardId());
        
        if (target != null) {
            // Check if player still has contraband items
            boolean hasContraband = false;
            for (Material material : request.getTargetItems()) {
                if (target.getInventory().contains(material)) {
                    hasContraband = true;
                    break;
                }
            }
            
            if (hasContraband) {
                // Player still has contraband - violation
                handleContrabandCompliance(request, false);
            } else {
                // Player doesn't have items anymore (dropped or consumed) - compliance
                handleContrabandCompliance(request, true);
            }
        } else {
            // Player offline - remove request
            activeRequests.remove(request.getTargetId());
        }
    }
    
    private void startChaseAfterContrabandViolation(Player guard, Player target, ContrabandRequest request) {
        // Check if chase can start
        if (!plugin.getChaseManager().canStartChase(guard, target)) {
            // If chase can't start, just increase wanted level
            logger.info("Chase could not start after contraband violation for " + target.getName());
            return;
        }
        
        // Start chase immediately
        boolean chaseStarted = plugin.getChaseManager().startChase(guard, target);
        
        if (chaseStarted) {
            plugin.getMessageManager().sendMessage(guard, "contraband.chase.started",
                playerPlaceholder("target", target),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            plugin.getMessageManager().sendMessage(target, "contraband.chase.target-notification",
                playerPlaceholder("guard", guard),
                stringPlaceholder("reason", "Contraband possession: " + request.getDescription()));
            
            logger.info("Chase started after contraband violation: " + guard.getName() + " -> " + target.getName());
        }
    }
    
    private void increaseWantedLevel(Player player, String reason) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + 1, plugin.getConfigManager().getMaxWantedLevel());
        
        // Set new wanted level
        plugin.getWantedManager().setWantedLevel(player, newLevel, reason);
        
        // Apply glow effect for wanted level 3+
        if (newLevel >= 3) {
            applyWantedGlowEffect(player, true);
        }
    }
    
    private void applyWantedGlowEffect(Player player, boolean shouldGlow) {
        if (shouldGlow) {
            // Make player glow red for guards on duty
            player.setGlowing(true);
            
            // Send notification to guards
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().isOnDuty(onlinePlayer) && 
                    plugin.getDutyManager().hasGuardPermission(onlinePlayer)) {
                    plugin.getMessageManager().sendMessage(onlinePlayer, "wanted.glow.notification",
                        playerPlaceholder("player", player),
                        numberPlaceholder("level", plugin.getWantedManager().getWantedLevel(player)));
                }
            }
        } else {
            player.setGlowing(false);
        }
    }
    
    // === CONTRABAND DETECTION METHODS ===
    
    public void performDrugTest(Player guard, Player target) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.not-on-duty");
            return;
        }
        
        if (!plugin.getConfigManager().isDrugDetectionEnabled()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.disabled");
            return;
        }
        
        // Security check: Can target be contraband targeted?
        if (!plugin.getSecurityManager().canPlayerBeContrabandTargeted(target)) {
            plugin.getMessageManager().sendMessage(guard, "security.guard-immunity.contraband-protected",
                playerPlaceholder("player", target));
            plugin.getSecurityManager().logSecurityViolation("drug test", guard, target);
            return;
        }
        
        // Check distance
        double distance = guard.getLocation().distance(target.getLocation());
        if (distance > plugin.getConfigManager().getMaxRequestDistance()) {
            plugin.getMessageManager().sendMessage(guard, "contraband.request.too-far");
            return;
        }
        
        // Check for drug items in inventory
        String[] drugItems = plugin.getConfigManager().getContrabandItems("drugs").split(",");
        boolean foundDrugs = false;
        String foundDrug = "";
        
        for (String drugItem : drugItems) {
            try {
                Material drugMaterial = Material.valueOf(drugItem.trim().toUpperCase());
                if (target.getInventory().contains(drugMaterial)) {
                    foundDrugs = true;
                    foundDrug = drugMaterial.name();
                    break;
                }
            } catch (IllegalArgumentException e) {
                // Invalid material, skip
            }
        }
        
        // Send results
        plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.kit-used",
            playerPlaceholder("player", target));
        
        if (foundDrugs) {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.positive",
                stringPlaceholder("drug", foundDrug));
            
            // Increase wanted level
            increaseWantedLevel(target, "Positive drug test: " + foundDrug);
            
            // Award performance for successful detection
            plugin.getDutyManager().awardDetectionPerformance(guard);
        } else {
            plugin.getMessageManager().sendMessage(guard, "contraband.drug-test.negative");
            plugin.getMessageManager().sendMessage(target, "contraband.drug-test.negative");
        }
        
        logger.info("Drug test performed: " + guard.getName() + " -> " + target.getName() + 
                   " (Result: " + (foundDrugs ? "POSITIVE" : "NEGATIVE") + ")");
    }
    
    // === CONTRABAND REMOVAL METHODS ===
    
    /**
     * Remove contraband items from player when they are caught/jailed
     * This is the only time contraband should be automatically taken
     */
    public void removeContrabandOnCapture(Player target) {
        // Get all contraband types from config
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs"};
        
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig != null && !itemsConfig.isEmpty()) {
                List<Material> contrabandItems = parseContrabandItems(itemsConfig);
                
                for (Material material : contrabandItems) {
                    target.getInventory().remove(material);
                }
            }
        }
        
        plugin.getMessageManager().sendMessage(target, "contraband.removal.captured");
        logger.info("Contraband removed from " + target.getName() + " upon capture");
    }
    
    // === UTILITY METHODS ===
    
    public boolean hasActiveRequest(Player player) {
        return activeRequests.containsKey(player.getUniqueId());
    }
    
    public ContrabandRequest getActiveRequest(Player player) {
        return activeRequests.get(player.getUniqueId());
    }
    
    public void cancelActiveRequest(Player player) {
        ContrabandRequest request = activeRequests.remove(player.getUniqueId());
        if (request != null) {
            // Cancel timeout task
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            // Hide boss bar
            plugin.getBossBarManager().hideBossBarByType(player, "contraband");
            
            logger.info("Contraband request cancelled for " + player.getName());
        }
    }
    
    /**
     * Check if a player has any contraband items
     */
    public boolean hasContrabandItems(Player player) {
        String[] contrabandTypes = {"sword", "bow", "armor", "drugs"};
        
        for (String type : contrabandTypes) {
            String itemsConfig = plugin.getConfigManager().getContrabandItems(type);
            if (itemsConfig != null && !itemsConfig.isEmpty()) {
                List<Material> contrabandItems = parseContrabandItems(itemsConfig);
                
                for (Material material : contrabandItems) {
                    if (player.getInventory().contains(material)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    // === CLEANUP METHODS ===
    
    public void cleanup() {
        for (ContrabandRequest request : activeRequests.values()) {
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            Player target = Bukkit.getPlayer(request.getTargetId());
            if (target != null) {
                plugin.getBossBarManager().hideBossBarByType(target, "contraband");
            }
        }
        
        activeRequests.clear();
        logger.info("ContrabandManager cleaned up successfully");
    }
    
    public void cleanupPlayer(Player player) {
        cancelActiveRequest(player);
        
        // Remove glow effect if player was wanted
        if (plugin.getWantedManager().getWantedLevel(player) >= 3) {
            applyWantedGlowEffect(player, false);
        }
    }
    
    // === CONTRABAND REQUEST CLASS ===
    
    public static class ContrabandRequest {
        private final UUID guardId;
        private final UUID targetId;
        private final String type;
        private final String description;
        private final List<Material> targetItems;
        private final long startTime;
        private final int timeout;
        private final List<Material> droppedItems;
        private BukkitTask timeoutTask;
        
        public ContrabandRequest(UUID guardId, UUID targetId, String type, String description,
                               List<Material> targetItems, long startTime, int timeout) {
            this.guardId = guardId;
            this.targetId = targetId;
            this.type = type;
            this.description = description;
            this.targetItems = new ArrayList<>(targetItems);
            this.startTime = startTime;
            this.timeout = timeout;
            this.droppedItems = new ArrayList<>();
        }
        
        // Getters
        public UUID getGuardId() { return guardId; }
        public UUID getTargetId() { return targetId; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public List<Material> getTargetItems() { return targetItems; }
        public long getStartTime() { return startTime; }
        public int getTimeout() { return timeout; }
        public List<Material> getDroppedItems() { return droppedItems; }
        public BukkitTask getTimeoutTask() { return timeoutTask; }
        
        // Setters
        public void setTimeoutTask(BukkitTask timeoutTask) { this.timeoutTask = timeoutTask; }
        
        // Utility methods
        public void addDroppedItem(Material material) {
            if (!droppedItems.contains(material)) {
                droppedItems.add(material);
            }
        }
        
        public boolean isCompliant() {
            // Check if all required item types have been dropped
            for (Material requiredItem : targetItems) {
                if (!droppedItems.contains(requiredItem)) {
                    return false;
                }
            }
            return true;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public int getRemainingTime() {
            long elapsed = getElapsedTime() / 1000L;
            return Math.max(0, timeout - (int) elapsed);
        }
        
        @Override
        public String toString() {
            return "ContrabandRequest{" +
                    "guardId=" + guardId +
                    ", targetId=" + targetId +
                    ", type='" + type + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
} 