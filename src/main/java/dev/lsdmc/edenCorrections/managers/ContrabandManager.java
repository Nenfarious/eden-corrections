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
                stringPlaceholder("command", "/contraband " + contrabandType));
            return false;
        }
        
        // Parse target items
        List<Material> targetItems = parseContrabandItems(itemsConfig);
        if (targetItems.isEmpty()) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/contraband " + contrabandType));
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
        
        // Show countdown boss bar
        plugin.getMessageManager().showCountdownBossBar(
            target,
            "bossbar.contraband-countdown",
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS,
            timeout,
            stringPlaceholder("description", description)
        );
        
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
            plugin.getMessageManager().hideBossBar(target);
            
            if (compliant) {
                // Success
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-success");
                plugin.getMessageManager().sendActionBar(target, "actionbar.contraband-compliance");
            } else {
                // Failed - increase wanted level
                plugin.getMessageManager().sendMessage(target, "contraband.detection.compliance-failed");
                increaseWantedLevel(target, "Contraband possession: " + request.getDescription());
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
                // Player doesn't have items anymore (dropped or consumed)
                handleContrabandCompliance(request, true);
            }
        } else {
            // Player offline - remove request
            activeRequests.remove(request.getTargetId());
        }
    }
    
    private void increaseWantedLevel(Player player, String reason) {
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        int currentLevel = data.getWantedLevel();
        int newLevel = Math.min(currentLevel + 1, plugin.getConfigManager().getMaxWantedLevel());
        
        // Set new wanted level
        plugin.getWantedManager().setWantedLevel(player, newLevel, reason);
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
            plugin.getMessageManager().hideBossBar(player);
            
            logger.info("Contraband request cancelled for " + player.getName());
        }
    }
    
    // === CLEANUP METHODS ===
    
    public void cleanup() {
        for (ContrabandRequest request : activeRequests.values()) {
            if (request.getTimeoutTask() != null) {
                request.getTimeoutTask().cancel();
            }
            
            Player target = Bukkit.getPlayer(request.getTargetId());
            if (target != null) {
                plugin.getMessageManager().hideBossBar(target);
            }
        }
        
        activeRequests.clear();
        logger.info("ContrabandManager cleaned up successfully");
    }
    
    public void cleanupPlayer(Player player) {
        cancelActiveRequest(player);
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
} 