package dev.lsdmc.edenCorrections.config;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    // Configuration validation cache
    private boolean configValid = true;
    private final List<String> validationErrors = new ArrayList<>();
    
    // Hot reload support
    private final AtomicBoolean isReloading = new AtomicBoolean(false);
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private long lastReloadTime = 0;
    
    // Configuration change listeners
    private final List<ConfigChangeListener> changeListeners = new ArrayList<>();
    
    public interface ConfigChangeListener {
        void onConfigReloaded();
        void onConfigValueChanged(String path, Object oldValue, Object newValue);
    }
    
    public ConfigManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.getConfig();
        
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Load configuration
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            isReloading.set(true);
            
            // Backup current config values for change detection
            Map<String, Object> oldValues = new HashMap<>(configCache);
            
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            // Set defaults if not present
            setDefaults();
            
            // Validate configuration
            validateConfiguration();
            
            // Cache current values
            cacheConfigValues();
            
            // Detect and notify changes
            detectConfigChanges(oldValues);
            
            if (configValid) {
                logger.info("Configuration loaded and validated successfully!");
                lastReloadTime = System.currentTimeMillis();
            } else {
                logger.warning("Configuration loaded with " + validationErrors.size() + " validation errors:");
                for (String error : validationErrors) {
                    logger.warning("  - " + error);
                }
            }
            
            // Notify listeners
            notifyConfigReloaded();
            
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isReloading.set(false);
        }
    }
    
    private void cacheConfigValues() {
        configCache.clear();
        cacheSection("", config);
    }
    
    private void cacheSection(String prefix, ConfigurationSection section) {
        for (String key : section.getKeys(true)) {
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            if (!section.isConfigurationSection(key)) {
                configCache.put(fullPath, section.get(key));
            }
        }
    }
    
    private void detectConfigChanges(Map<String, Object> oldValues) {
        for (Map.Entry<String, Object> entry : configCache.entrySet()) {
            String path = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldValues.get(path);
            
            if (!java.util.Objects.equals(oldValue, newValue)) {
                notifyConfigValueChanged(path, oldValue, newValue);
            }
        }
    }
    
    private void notifyConfigReloaded() {
        for (ConfigChangeListener listener : changeListeners) {
            try {
                listener.onConfigReloaded();
            } catch (Exception e) {
                logger.warning("Error notifying config change listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyConfigValueChanged(String path, Object oldValue, Object newValue) {
        for (ConfigChangeListener listener : changeListeners) {
            try {
                listener.onConfigValueChanged(path, oldValue, newValue);
            } catch (Exception e) {
                logger.warning("Error notifying config value change listener: " + e.getMessage());
            }
        }
    }
    
    public void addConfigChangeListener(ConfigChangeListener listener) {
        changeListeners.add(listener);
    }
    
    public void removeConfigChangeListener(ConfigChangeListener listener) {
        changeListeners.remove(listener);
    }
    
    public boolean isReloading() {
        return isReloading.get();
    }
    
    public long getLastReloadTime() {
        return lastReloadTime;
    }
    
    private void setDefaults() {
        // Core settings
        config.addDefault("core.debug", false);
        config.addDefault("core.language", "en");
        
        // Time settings (in seconds)
        config.addDefault("times.wanted-duration", 1800);
        config.addDefault("times.contraband-compliance", 10);
        
        // Guard system settings
        config.addDefault("guard-system.duty-region", "guard");
        config.addDefault("guard-system.immobilization-time", 5);
        
        // Guard restrictions
        config.addDefault("guard-system.restrictions.block-mining", true);
        config.addDefault("guard-system.restrictions.block-crafting", true);
        config.addDefault("guard-system.restrictions.block-storage", true);
        config.addDefault("guard-system.restrictions.block-item-dropping", true);
        
        // Off-duty earning system
        config.addDefault("guard-system.off-duty-earning.base-duty-requirement", 15);
        config.addDefault("guard-system.off-duty-earning.base-off-duty-earned", 30);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.searches-per-bonus", 10);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.search-bonus-time", 5);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.successful-search-bonus", 10);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.successful-arrest-bonus", 8);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.kills-per-bonus", 5);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.kill-bonus-time", 15);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.successful-detection-bonus", 10);
        config.addDefault("guard-system.off-duty-earning.performance-bonuses.duty-time-bonus-rate", 2);
        
        // Penalty escalation system
        config.addDefault("guard-system.penalty-escalation.enabled", true);
        config.addDefault("guard-system.penalty-escalation.grace-period", 5);
        config.addDefault("guard-system.penalty-escalation.stages.stage-1.time-minutes", 5);
        config.addDefault("guard-system.penalty-escalation.stages.stage-1.slowness-level", 1);
        config.addDefault("guard-system.penalty-escalation.stages.stage-1.economy-penalty", 1000);
        config.addDefault("guard-system.penalty-escalation.stages.stage-1.warning-message", true);
        config.addDefault("guard-system.penalty-escalation.stages.stage-2.time-minutes", 10);
        config.addDefault("guard-system.penalty-escalation.stages.stage-2.slowness-level", 2);
        config.addDefault("guard-system.penalty-escalation.stages.stage-2.economy-penalty", 1000);
        config.addDefault("guard-system.penalty-escalation.stages.stage-2.warning-message", true);
        config.addDefault("guard-system.penalty-escalation.stages.recurring.interval-minutes", 5);
        config.addDefault("guard-system.penalty-escalation.stages.recurring.slowness-level", 2);
        config.addDefault("guard-system.penalty-escalation.stages.recurring.economy-penalty", 1000);
        config.addDefault("guard-system.penalty-escalation.stages.recurring.warning-message", true);
        
        // Penalty bypass settings
        config.addDefault("penalty-escalation.bypass.earned-time-bonus", 60);
        config.addDefault("penalty-escalation.bypass.clear-penalty-tracking", true);
        config.addDefault("penalty-escalation.bypass.remove-potion-effects", true);
        
        // Jail system settings
        config.addDefault("jail-system.base-time", 300);
        config.addDefault("jail-system.level-multiplier", 60);
        config.addDefault("jail-system.max-wanted-level", 5);
        config.addDefault("jail-system.countdown", 10);
        config.addDefault("jail-system.countdown-radius", 5.0);
        config.addDefault("jail-system.chase-integration.enabled", true);
        config.addDefault("jail-system.chase-integration.flee-threshold", 2.5);
        
        // Chase system settings
        config.addDefault("chase-system.max-distance", 100);
        config.addDefault("chase-system.warning-distance", 20);
        config.addDefault("chase-system.max-concurrent", 3);
        config.addDefault("chase-system.restrictions.prevent-chase-during-combat", true);
        config.addDefault("chase-system.restrictions.block-restricted-areas", true);
        config.addDefault("chase-system.restrictions.restricted-areas", "");
        config.addDefault("chase-system.restrictions.auto-end-in-restricted-area", true);
        
        // Contraband system settings
        config.addDefault("contraband-system.enabled", true);
        config.addDefault("contraband-system.drug-detection", true);
        config.addDefault("contraband-system.max-request-distance", 5);
        config.addDefault("contraband-system.compliance-grace-period", 3);
        
        // Combat system settings
        config.addDefault("combat-system.timer-duration", 5);
        config.addDefault("combat-system.prevent-capture", true);
        config.addDefault("combat-system.prevent-teleport", true);
        
        // Banking system settings
        config.addDefault("banking-system.enabled", true);
        config.addDefault("banking-system.conversion-rate", 100);
        config.addDefault("banking-system.minimum-conversion", 300);
        config.addDefault("banking-system.auto-convert", false);
        config.addDefault("banking-system.auto-convert-threshold", 3600);
        config.addDefault("banking-system.currency-command", "et give {player} {amount}");
        
        // Region settings
        config.addDefault("regions.no-chase-zones", "safezon");
        config.addDefault("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        
        // Security settings
        config.addDefault("security.guard-immunity.enabled", true);
        config.addDefault("security.guard-immunity.wanted-protection", true);
        config.addDefault("security.guard-immunity.chase-protection", true);
        config.addDefault("security.guard-immunity.contraband-protection", true);
        config.addDefault("security.guard-immunity.jail-protection", true);
        config.addDefault("security.guard-immunity.combat-protection", false);
        config.addDefault("security.guard-immunity.teleport-protection", true);
        
        // User interface settings
        config.addDefault("user-interface.boss-bars.enabled", true);
        config.addDefault("user-interface.boss-bars.wanted.enabled", true);
        config.addDefault("user-interface.boss-bars.wanted.color", "RED");
        config.addDefault("user-interface.boss-bars.wanted.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.chase.enabled", true);
        config.addDefault("user-interface.boss-bars.chase.color", "BLUE");
        config.addDefault("user-interface.boss-bars.chase.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.combat.enabled", true);
        config.addDefault("user-interface.boss-bars.combat.color", "RED");
        config.addDefault("user-interface.boss-bars.combat.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.jail.enabled", true);
        config.addDefault("user-interface.boss-bars.jail.color", "PURPLE");
        config.addDefault("user-interface.boss-bars.jail.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.duty.enabled", true);
        config.addDefault("user-interface.boss-bars.duty.color", "GREEN");
        config.addDefault("user-interface.boss-bars.duty.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.contraband.enabled", true);
        config.addDefault("user-interface.boss-bars.contraband.color", "YELLOW");
        config.addDefault("user-interface.boss-bars.contraband.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.grace-period.enabled", true);
        config.addDefault("user-interface.boss-bars.grace-period.color", "PINK");
        config.addDefault("user-interface.boss-bars.grace-period.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.penalty.enabled", true);
        config.addDefault("user-interface.boss-bars.penalty.color", "RED");
        config.addDefault("user-interface.boss-bars.penalty.overlay", "PROGRESS");
        config.addDefault("user-interface.boss-bars.penalty.duration", 30);
        
        // Guard tags
        config.addDefault("user-interface.guard-tags.enabled", true);
        config.addDefault("user-interface.guard-tags.format", "<gradient:#9D4EDD:#06FFA5>[ON DUTY]</gradient>");
        config.addDefault("user-interface.guard-tags.priority", 1000);
        config.addDefault("user-interface.guard-tags.hover-enabled", true);
        config.addDefault("user-interface.guard-tags.show-session-stats", true);
        config.addDefault("user-interface.guard-tags.show-total-stats", true);
        
        // Integration settings
        config.addDefault("integrations.unlimited-nametags.guard-tags.enabled", false);
        config.addDefault("integrations.unlimited-nametags.guard-tags.format", "<gradient:#9D4EDD:#06FFA5>[ON DUTY]</gradient> <aqua>{rank}</aqua>");
        config.addDefault("integrations.unlimited-nametags.guard-tags.priority", 100);
        config.addDefault("integrations.unlimited-nametags.wanted-indicators.enabled", false);
        config.addDefault("integrations.unlimited-nametags.wanted-indicators.format", "<red><bold>WANTED {stars}</bold></red>");
        config.addDefault("integrations.unlimited-nametags.wanted-indicators.priority", 200);
        
        // Database settings
        config.addDefault("database.type", "sqlite");
        config.addDefault("database.sqlite.file", "edencorrections.db");
        config.addDefault("database.sqlite.maintenance.enabled", true);
        config.addDefault("database.sqlite.maintenance.enable-vacuum", true);
        config.addDefault("database.sqlite.maintenance.vacuum-timeout", 10000);
        config.addDefault("database.sqlite.maintenance.maintenance-interval", 60);
        config.addDefault("database.mysql.host", "localhost");
        config.addDefault("database.mysql.port", 3306);
        config.addDefault("database.mysql.database", "edencorrections");
        config.addDefault("database.mysql.username", "username");
        config.addDefault("database.mysql.password", "password");
        
        // Performance settings
        config.addDefault("performance.spam-control.duty-system.disable-continuous-messages", true);
        config.addDefault("performance.spam-control.duty-system.show-status-changes-only", true);
        config.addDefault("performance.spam-control.duty-system.notification-cooldown", 30);
        config.addDefault("performance.spam-control.duty-system.disable-performance-spam", true);
        config.addDefault("performance.spam-control.duty-system.show-bonuses-once", true);
        config.addDefault("performance.spam-control.chase-system.disable-distance-warnings", false);
        config.addDefault("performance.spam-control.chase-system.distance-warning-cooldown", 10);
        config.addDefault("performance.spam-control.chase-system.disable-status-spam", true);
        config.addDefault("performance.spam-control.combat-system.disable-timer-spam", true);
        config.addDefault("performance.spam-control.combat-system.show-start-end-only", true);
        config.addDefault("performance.spam-control.general.disable-debug-spam", true);
        config.addDefault("performance.spam-control.general.message-cooldown", 5);
        config.addDefault("performance.spam-control.general.disable-error-spam", true);
        config.addDefault("performance.caching.enable-message-cache", true);
        config.addDefault("performance.caching.message-cache-size", 1000);
        config.addDefault("performance.caching.enable-database-cache", true);
        config.addDefault("performance.caching.database-cache-size", 500);
        config.addDefault("performance.caching.cache-cleanup-interval", 300);
        config.addDefault("performance.caching.database-cleanup-interval", 600);
        
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }
    
    private void validateConfiguration() {
        configValid = true;
        validationErrors.clear();
        
        // Validate core settings
        validateCoreSettings();
        
        // Validate time settings
        validateTimeSettings();
        
        // Validate jail settings
        validateJailSettings();
        
        // Validate chase settings
        validateChaseSettings();
        
        // Validate guard system
        validateGuardSystem();
        
        // Validate contraband system
        validateContrabandSystem();
        
        // Validate combat timer
        validateCombatTimer();
        
        // Validate duty banking
        validateDutyBanking();
        
        // Validate region settings
        validateRegionSettings();
        
        // Validate performance settings
        validatePerformanceSettings();
        
        // Validate integration settings
        validateIntegrationSettings();
        
        // Validate security settings
        validateSecuritySettings();
        
        // Validate boss bar settings
        validateBossBarSettings();
    }
    
    private void validateCoreSettings() {
        // Validate language
        String language = config.getString("core.language", "en");
        if (!Arrays.asList("en", "es", "fr", "de").contains(language)) {
            addValidationError("Invalid language: " + language + " (defaulting to 'en')");
        }
    }
    
    private void validateTimeSettings() {
        validatePositiveInt("times.wanted-duration", "Wanted duration");
        validatePositiveInt("times.contraband-compliance", "Contraband compliance time");
    }
    
    private void validateJailSettings() {
        validatePositiveInt("jail-system.base-time", "Base jail time");
        validatePositiveInt("jail-system.level-multiplier", "Jail level multiplier");
        validateRange("jail-system.max-wanted-level", "Max wanted level", 1, 10);
    }
    
    private void validateChaseSettings() {
        validatePositiveInt("chase-system.max-distance", "Max chase distance");
        validatePositiveInt("chase-system.warning-distance", "Chase warning distance");
        validatePositiveInt("chase-system.max-concurrent", "Max concurrent chases");
        
        // Validate logical relationships
        int maxDistance = config.getInt("chase-system.max-distance", 100);
        int warningDistance = config.getInt("chase-system.warning-distance", 20);
        if (warningDistance >= maxDistance) {
            addValidationError("Chase warning distance (" + warningDistance + ") should be less than max distance (" + maxDistance + ")");
        }
    }
    
    private void validateGuardSystem() {
        // Validate immobilization time
        validateNonNegativeInt("guard-system.immobilization-time", "Immobilization time");
        
        // Validate rank mappings
        ConfigurationSection rankMappings = config.getConfigurationSection("guard-system.rank-mappings");
        if (rankMappings == null || rankMappings.getKeys(false).isEmpty()) {
            addValidationError("No guard rank mappings configured");
        }
        
        // Validate kit mappings
        ConfigurationSection kitMappings = config.getConfigurationSection("guard-system.kit-mappings");
        if (kitMappings == null || kitMappings.getKeys(false).isEmpty()) {
            addValidationError("No guard kit mappings configured");
        }
    }
    
    private void validateContrabandSystem() {
        if (!config.getBoolean("contraband-system.enabled", true)) {
            return; // Skip validation if disabled
        }
        
        validatePositiveInt("contraband-system.max-request-distance", "Max contraband request distance");
        validateNonNegativeInt("contraband-system.compliance-grace-period", "Contraband grace period");
        
        // Validate contraband types
        ConfigurationSection contrabandTypes = config.getConfigurationSection("contraband-system.types");
        if (contrabandTypes != null) {
            for (String type : contrabandTypes.getKeys(false)) {
                validateContrabandType(type);
            }
        }
    }
    
    private void validateContrabandType(String type) {
        String itemsPath = "contraband-system.types." + type + ".items"; 
        String descriptionPath = "contraband-system.types." + type + ".description";
        
        if (!config.contains(itemsPath)) {
            addValidationError("Missing contraband type items configuration: " + itemsPath);
            return;
        }
        
        String items = config.getString(itemsPath, "");
        if (items.trim().isEmpty()) {
            addValidationError("Empty contraband items for type: " + type);
        }
        
        // Validate that all items are valid Material names
        String[] materialNames = items.split(",");
        for (String materialName : materialNames) {
            materialName = materialName.trim().toUpperCase();
            try {
                Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                addValidationError("Invalid material in contraband type " + type + ": " + materialName);
            }
        }
        
        String description = config.getString(descriptionPath, "");
        if (description.trim().isEmpty()) {
            addValidationError("Contraband type '" + type + "' has no description");
        }
    }
    
    private void validateCombatTimer() {
        validateNonNegativeInt("combat-system.timer-duration", "Combat timer duration");
    }
    
    private void validateDutyBanking() {
        if (!config.getBoolean("banking-system.enabled", true)) {
            return; // Skip validation if disabled
        }
        
        validatePositiveInt("banking-system.conversion-rate", "Duty banking conversion rate");
        validateNonNegativeInt("banking-system.minimum-conversion", "Minimum conversion time");
        validatePositiveInt("banking-system.auto-convert-threshold", "Auto-convert threshold");
        
        // Validate currency command
        String currencyCommand = config.getString("banking-system.currency-command", "");
        if (currencyCommand.trim().isEmpty()) {
            addValidationError("Duty banking currency command is empty");
        } else {
            if (!currencyCommand.contains("{player}")) {
                addValidationError("Currency command missing {player} placeholder");
            }
            if (!currencyCommand.contains("{amount}")) {
                addValidationError("Currency command missing {amount} placeholder");
            }
        }
    }
    
    private void validateRegionSettings() {
        // Validate that region lists are not empty
        String[] noChaseZones = getNoChaseZones();
        String[] dutyRequiredZones = getDutyRequiredZones();
        
        if (noChaseZones.length == 0) {
            addValidationError("No no-chase zones configured");
        }
        if (dutyRequiredZones.length == 0) {
            addValidationError("No duty-required zones configured");
        }
    }
    
    private void validatePerformanceSettings() {
        // Validate spam control settings
        validateNonNegativeInt("performance.spam-control.duty-system.notification-cooldown", "Duty system notification cooldown");
        validateNonNegativeInt("performance.spam-control.chase-system.distance-warning-cooldown", "Chase system distance warning cooldown");
        validateNonNegativeInt("performance.spam-control.general.message-cooldown", "General message cooldown");
        
        // Validate caching settings
        validatePositiveInt("performance.caching.message-cache-size", "Message cache size");
        validatePositiveInt("performance.caching.database-cache-size", "Database cache size");
        validatePositiveInt("performance.caching.cache-cleanup-interval", "Cache cleanup interval");
        validatePositiveInt("performance.caching.database-cleanup-interval", "Database cleanup interval");
    }
    
    private void validateIntegrationSettings() {
        // Validate PlaceholderAPI settings
        if (config.getBoolean("integrations.placeholderapi.enabled", true)) {
            // Additional validation can be added here
        }
        
        // Validate LuckPerms settings
        if (config.getBoolean("integrations.luckperms.strict-mode", true)) {
            // Additional validation can be added here
        }
    }
    
    private void validateSecuritySettings() {
        // Validate guard immunity settings
        if (config.getBoolean("security.guard-immunity.enabled", true)) {
            // All sub-settings are optional, no validation needed
        }
    }
    
    private void validateBossBarSettings() {
        // Validate boss bar colors
        String[] validColors = {"BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"};
        String[] validOverlays = {"PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20"};
        
        validateBossBarSetting("user-interface.boss-bars.wanted.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.chase.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.combat.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.jail.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.duty.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.contraband.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.grace-period.color", validColors);
        validateBossBarSetting("user-interface.boss-bars.penalty.color", validColors);
        
        validateBossBarSetting("user-interface.boss-bars.wanted.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.chase.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.combat.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.jail.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.duty.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.contraband.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.grace-period.overlay", validOverlays);
        validateBossBarSetting("user-interface.boss-bars.penalty.overlay", validOverlays);
    }
    
    private void validateBossBarSetting(String path, String[] validValues) {
        String value = config.getString(path, "");
        if (!value.isEmpty() && !Arrays.asList(validValues).contains(value.toUpperCase())) {
            addValidationError("Invalid boss bar setting '" + path + "': " + value + " (valid: " + String.join(", ", validValues) + ")");
        }
    }
    
    private void validatePositiveInt(String path, String name) {
        int value = config.getInt(path, 1);
        if (value <= 0) {
            addValidationError(name + " must be positive (current: " + value + ")");
        }
    }
    
    private void validateNonNegativeInt(String path, String name) {
        int value = config.getInt(path, 0);
        if (value < 0) {
            addValidationError(name + " must be non-negative (current: " + value + ")");
        }
    }
    
    private void validateRange(String path, String name, int min, int max) {
        int value = config.getInt(path, min);
        if (value < min || value > max) {
            addValidationError(name + " must be between " + min + " and " + max + " (current: " + value + ")");
        }
    }
    
    private void addValidationError(String error) {
        configValid = false;
        validationErrors.add(error);
    }
    
    public void reload() {
        if (isReloading.compareAndSet(false, true)) {
            try {
                // Clear the config cache
                configCache.clear();
                
                // Load the configuration
                loadConfig();
                
                // Cache the new values
                cacheConfigValues();
                
                // Update last reload time
                lastReloadTime = System.currentTimeMillis();
                
                // Notify listeners
                notifyConfigReloaded();
                
                logger.info("Configuration reloaded successfully");
                
            } catch (Exception e) {
                logger.severe("Failed to reload configuration: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isReloading.set(false);
            }
        } else {
            logger.warning("Configuration reload already in progress");
        }
    }
    
    // === VALIDATION GETTERS ===
    
    public boolean isConfigValid() {
        return configValid;
    }
    
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    // === EXISTING GETTERS ===
    
    public boolean isDebugMode() {
        return config.getBoolean("core.debug", false);
    }
    
    public String getLanguage() {
        return config.getString("core.language", "en");
    }
    

    
    public int getChaseDuration() {
        return config.getInt("times.wanted-duration", 1800); // Use wanted duration for chase duration
    }
    
    public int getWantedDuration() {
        return config.getInt("times.wanted-duration", 1800);
    }
    
    public int getJailCountdown() {
        return config.getInt("jail-system.countdown", 10);
    }
    
    public double getJailCountdownRadius() {
        return config.getDouble("jail-system.countdown-radius", 5.0);
    }
    
    public boolean isJailChaseIntegrationEnabled() {
        return config.getBoolean("jail-system.chase-integration.enabled", true);
    }
    
    public double getJailFleeThreshold() {
        return config.getDouble("jail-system.chase-integration.flee-threshold", 2.5);
    }
    
    public int getContrabandCompliance() {
        return config.getInt("times.contraband-compliance", 10);
    }
    
    public int getBaseJailTime() {
        return config.getInt("jail-system.base-time", 300);
    }
    
    public int getJailLevelMultiplier() {
        return config.getInt("jail-system.level-multiplier", 60);
    }
    
    public int getMaxWantedLevel() {
        return config.getInt("jail-system.max-wanted-level", 5);
    }
    
    public int getMaxChaseDistance() {
        return config.getInt("chase-system.max-distance", 100);
    }
    
    public int getChaseWarningDistance() {
        return config.getInt("chase-system.warning-distance", 20);
    }
    
    public int getMaxConcurrentChases() {
        return config.getInt("chase-system.max-concurrent", 3);
    }
    
    // === ENHANCED CHASE RESTRICTIONS ===
    
    public boolean shouldPreventChaseDuringCombat() {
        return config.getBoolean("chase-system.restrictions.prevent-chase-during-combat", true);
    }
    
    public boolean shouldBlockRestrictedAreas() {
        return config.getBoolean("chase-system.restrictions.block-restricted-areas", true);
    }
    
    public String[] getChaseRestrictedAreas() {
        String areas = config.getString("chase-system.restrictions.restricted-areas", "safezone,spawn,visitor_area,medical_wing,admin_office");
        return areas.split(",");
    }
    
    public boolean shouldAutoEndInRestrictedArea() {
        return config.getBoolean("chase-system.restrictions.auto-end-in-restricted-area", true);
    }
    
    public boolean isContrabandEnabled() {
        return config.getBoolean("contraband-system.enabled", true);
    }
    
    public boolean isDrugDetectionEnabled() {
        return config.getBoolean("contraband-system.drug-detection", true);
    }
    
    public boolean isGuardMiningBlocked() {
        return config.getBoolean("guard-system.restrictions.block-mining", true);
    }
    
    public boolean isGuardCraftingBlocked() {
        return config.getBoolean("guard-system.restrictions.block-crafting", true);
    }
    
    public boolean isGuardStorageBlocked() {
        return config.getBoolean("guard-system.restrictions.block-storage", true);
    }
    
    public boolean isGuardItemDroppingBlocked() {
        return config.getBoolean("guard-system.restrictions.block-item-dropping", true);
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "edencorrections.db");
    }
    
    // MySQL database configuration
    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "edencorrections");
    }
    
    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "username");
    }
    
    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "password");
    }
    
    // Database maintenance configuration
    public boolean isDatabaseMaintenanceEnabled() {
        return config.getBoolean("database.sqlite.maintenance.enabled", true);
    }
    
    public boolean isDatabaseVacuumEnabled() {
        return config.getBoolean("database.sqlite.maintenance.enable-vacuum", true);
    }
    
    public int getDatabaseVacuumTimeout() {
        return config.getInt("database.sqlite.maintenance.vacuum-timeout", 10000);
    }
    
    public int getDatabaseMaintenanceInterval() {
        return config.getInt("database.sqlite.maintenance.maintenance-interval", 60);
    }
    
    // === ENHANCED SYSTEM GETTERS ===
    
    // Guard System Configuration
    public String getDutyRegion() {
        return config.getString("guard-system.duty-region", "guard");
    }
    
    public int getImmobilizationTime() {
        return config.getInt("guard-system.immobilization-time", 5);
    }
    
    public Map<String, String> getRankMappings() {
        Map<String, String> mappings = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("guard-system.rank-mappings");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                mappings.put(key, section.getString(key));
            }
        }
        return mappings;
    }
    
    public Map<String, String> getKitMappings() {
        Map<String, String> mappings = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("guard-system.kit-mappings");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                mappings.put(key, section.getString(key));
            }
        }
        return mappings;
    }
    
    public String getKitForRank(String rank) {
        return config.getString("guard-system.kit-mappings." + rank, "guard_kit");
    }
    
    // Off-duty earning system configuration
    public int getBaseDutyRequirement() {
        return config.getInt("guard-system.off-duty-earning.base-duty-requirement", 15);
    }
    
    public int getBaseOffDutyEarned() {
        return config.getInt("guard-system.off-duty-earning.base-off-duty-earned", 30);
    }
    
    public int getSearchesPerBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.searches-per-bonus", 10);
    }
    
    public int getSearchBonusTime() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.search-bonus-time", 5);
    }
    
    public int getSuccessfulSearchBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-search-bonus", 10);
    }
    
    public int getSuccessfulArrestBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-arrest-bonus", 8);
    }
    
    public int getKillsPerBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.kills-per-bonus", 5);
    }
    
    public int getKillBonusTime() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.kill-bonus-time", 15);
    }
    
    public int getSuccessfulDetectionBonus() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.successful-detection-bonus", 10);
    }
    
    public int getDutyTimeBonusRate() {
        return config.getInt("guard-system.off-duty-earning.performance-bonuses.duty-time-bonus-rate", 2);
    }
    
    // Enhanced Contraband System
    public ConfigurationSection getContrabandTypes() {
        return config.getConfigurationSection("contraband-system.types");
    }
    
    public int getMaxRequestDistance() {
        return config.getInt("contraband-system.max-request-distance", 5);
    }
    
    public int getGracePeriod() {
        return config.getInt("contraband-system.compliance-grace-period", 3);
    }
    
    public String getContrabandItems(String type) {
        return config.getString("contraband-system.types." + type + ".items", "");
    }
    
    public String getContrabandDescription(String type) {
        return config.getString("contraband-system.types." + type + ".description", type);
    }
    
    // Combat Timer Configuration
    public int getCombatTimerDuration() {
        return config.getInt("combat-system.timer-duration", 5);
    }
    
    public boolean shouldPreventCaptureInCombat() {
        return config.getBoolean("combat-system.prevent-capture", true);
    }
    
    public boolean shouldPreventTeleportInCombat() {
        return config.getBoolean("combat-system.prevent-teleport", true);
    }
    
    // Duty Banking Configuration
    public boolean isDutyBankingEnabled() {
        return config.getBoolean("banking-system.enabled", true);
    }
    
    public int getConversionRate() {
        return config.getInt("banking-system.conversion-rate", 100);
    }
    
    public int getMinimumConversion() {
        return config.getInt("banking-system.minimum-conversion", 300);
    }
    
    public boolean isAutoConvert() {
        return config.getBoolean("banking-system.auto-convert", false);
    }
    
    public int getAutoConvertThreshold() {
        return config.getInt("banking-system.auto-convert-threshold", 3600);
    }
    
    public String getCurrencyCommand() {
        return config.getString("banking-system.currency-command", "et give {player} {amount}");
    }
    
    // Region Configuration
    public String[] getNoChaseZones() {
        String zones = config.getString("regions.no-chase-zones", "safezon");
        return zones.split(",");
    }
    
    public String[] getDutyRequiredZones() {
        String zones = config.getString("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        return zones.split(",");
    }
    
    // Performance Configuration
    public boolean isDutySystemContinuousMessagesDisabled() {
        return config.getBoolean("performance.spam-control.duty-system.disable-continuous-messages", true);
    }
    
    public boolean isDutySystemStatusChangesOnly() {
        return config.getBoolean("performance.spam-control.duty-system.show-status-changes-only", true);
    }
    
    public int getDutySystemNotificationCooldown() {
        return config.getInt("performance.spam-control.duty-system.notification-cooldown", 30);
    }
    
    public boolean isDutySystemPerformanceSpamDisabled() {
        return config.getBoolean("performance.spam-control.duty-system.disable-performance-spam", true);
    }
    
    public boolean isDutySystemShowBonusesOnce() {
        return config.getBoolean("performance.spam-control.duty-system.show-bonuses-once", true);
    }
    
    public boolean isChaseSystemDistanceWarningsDisabled() {
        return config.getBoolean("performance.spam-control.chase-system.disable-distance-warnings", false);
    }
    
    public int getChaseSystemDistanceWarningCooldown() {
        return config.getInt("performance.spam-control.chase-system.distance-warning-cooldown", 10);
    }
    
    public boolean isChaseSystemStatusSpamDisabled() {
        return config.getBoolean("performance.spam-control.chase-system.disable-status-spam", true);
    }
    
    public boolean isCombatSystemTimerSpamDisabled() {
        return config.getBoolean("performance.spam-control.combat-system.disable-timer-spam", true);
    }
    
    public boolean isCombatSystemShowStartEndOnly() {
        return config.getBoolean("performance.spam-control.combat-system.show-start-end-only", true);
    }
    
    public boolean isGeneralDebugSpamDisabled() {
        return config.getBoolean("performance.spam-control.general.disable-debug-spam", true);
    }
    
    public int getGeneralMessageCooldown() {
        return config.getInt("performance.spam-control.general.message-cooldown", 5);
    }
    
    public boolean isGeneralErrorSpamDisabled() {
        return config.getBoolean("performance.spam-control.general.disable-error-spam", true);
    }
    
    public boolean isMessageCacheEnabled() {
        return config.getBoolean("performance.caching.enable-message-cache", true);
    }
    
    public int getMessageCacheSize() {
        return config.getInt("performance.caching.message-cache-size", 1000);
    }
    
    public boolean isDatabaseCacheEnabled() {
        return config.getBoolean("performance.caching.enable-database-cache", true);
    }
    
    public int getDatabaseCacheSize() {
        return config.getInt("performance.caching.database-cache-size", 500);
    }
    
    public int getCacheCleanupInterval() {
        return config.getInt("performance.caching.cache-cleanup-interval", 300);
    }
    
    public int getDatabaseCleanupInterval() {
        return config.getInt("performance.caching.database-cleanup-interval", 600);
    }
    
    // Integration Configuration
    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("integrations.placeholderapi.enabled", true);
    }
    
    public boolean isLuckPermsStrictMode() {
        return config.getBoolean("integrations.luckperms.strict-mode", true);
    }
    
    public boolean isWorldGuardRequired() {
        return config.getBoolean("integrations.worldguard.required", false);
    }
    
    public boolean isCMIKitsEnabled() {
        return config.getBoolean("integrations.cmi.kits-enabled", true);
    }
    
    // === SECURITY CONFIGURATION ===
    
    public boolean isGuardImmunityEnabled() {
        return config.getBoolean("security.guard-immunity.enabled", true);
    }
    
    public boolean isGuardWantedProtected() {
        return config.getBoolean("security.guard-immunity.wanted-protection", true);
    }
    
    public boolean isGuardChaseProtected() {
        return config.getBoolean("security.guard-immunity.chase-protection", true);
    }
    
    public boolean isGuardContrabandProtected() {
        return config.getBoolean("security.guard-immunity.contraband-protection", true);
    }
    
    public boolean isGuardJailProtected() {
        return config.getBoolean("security.guard-immunity.jail-protection", true);
    }
    
    public boolean isGuardCombatProtected() {
        return config.getBoolean("security.guard-immunity.combat-protection", false);
    }
    
    public boolean isGuardTeleportProtected() {
        return config.getBoolean("security.guard-immunity.teleport-protection", true);
    }
    
    // === BOSS BAR CONFIGURATION ===
    
    public boolean areBossBarsEnabled() {
        return config.getBoolean("user-interface.boss-bars.enabled", true);
    }
    
    public boolean isWantedBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.wanted.enabled", true);
    }
    
    public String getWantedBossBarColor() {
        return config.getString("user-interface.boss-bars.wanted.color", "RED");
    }
    
    public String getWantedBossBarOverlay() {
        return config.getString("user-interface.boss-bars.wanted.overlay", "PROGRESS");
    }
    
    public boolean isChaseBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.chase.enabled", true);
    }
    
    public String getChaseBossBarColor() {
        return config.getString("user-interface.boss-bars.chase.color", "BLUE");
    }
    
    public String getChaseBossBarOverlay() {
        return config.getString("user-interface.boss-bars.chase.overlay", "PROGRESS");
    }
    
    public boolean isCombatBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.combat.enabled", true);
    }
    
    public String getCombatBossBarColor() {
        return config.getString("user-interface.boss-bars.combat.color", "RED");
    }
    
    public String getCombatBossBarOverlay() {
        return config.getString("user-interface.boss-bars.combat.overlay", "PROGRESS");
    }
    
    public boolean isJailBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.jail.enabled", true);
    }
    
    public String getJailBossBarColor() {
        return config.getString("user-interface.boss-bars.jail.color", "PURPLE");
    }
    
    public String getJailBossBarOverlay() {
        return config.getString("user-interface.boss-bars.jail.overlay", "PROGRESS");
    }
    
    public boolean isDutyBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.duty.enabled", true);
    }
    
    public String getDutyBossBarColor() {
        return config.getString("user-interface.boss-bars.duty.color", "GREEN");
    }
    
    public String getDutyBossBarOverlay() {
        return config.getString("user-interface.boss-bars.duty.overlay", "PROGRESS");
    }
    
    public boolean isContrabandBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.contraband.enabled", true);
    }
    
    public String getContrabandBossBarColor() {
        return config.getString("user-interface.boss-bars.contraband.color", "YELLOW");
    }
    
    public String getContrabandBossBarOverlay() {
        return config.getString("user-interface.boss-bars.contraband.overlay", "PROGRESS");
    }
    
    public boolean isGraceBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.grace-period.enabled", true);
    }
    
    public String getGraceBossBarColor() {
        return config.getString("user-interface.boss-bars.grace-period.color", "PINK");
    }
    
    public String getGraceBossBarOverlay() {
        return config.getString("user-interface.boss-bars.grace-period.overlay", "PROGRESS");
    }
    
    public boolean isPenaltyBossBarEnabled() {
        return config.getBoolean("user-interface.boss-bars.penalty.enabled", true);
    }
    
    public String getPenaltyBossBarColor() {
        return config.getString("user-interface.boss-bars.penalty.color", "RED");
    }
    
    public String getPenaltyBossBarOverlay() {
        return config.getString("user-interface.boss-bars.penalty.overlay", "PROGRESS");
    }
    
    public int getPenaltyBossBarDuration() {
        return config.getInt("user-interface.boss-bars.penalty.duration", 30);
    }

    // === LUCKPERMS META INTEGRATION CONFIGURATION ===
    
    public boolean isLuckPermsGuardTagsEnabled() {
        return config.getBoolean("integrations.luckperms.guard-tags.enabled", true);
    }
    
    public boolean isLuckPermsWantedIndicatorsEnabled() {
        return config.getBoolean("integrations.luckperms.wanted-indicators.enabled", true);
    }
    
    public String getLuckPermsGuardTagFormat() {
        return config.getString("integrations.luckperms.guard-tags.format", 
            "&7[&aGuard&7] &b{rank}");
    }
    
    public String getLuckPermsWantedTagFormat() {
        return config.getString("integrations.luckperms.wanted-indicators.format", 
            "&4[&cWANTED {stars}&4]");
    }
    
    public int getLuckPermsGuardTagPriority() {
        return config.getInt("integrations.luckperms.guard-tags.priority", 100);
    }
    
    public int getLuckPermsWantedTagPriority() {
        return config.getInt("integrations.luckperms.wanted-indicators.priority", 200);
    }
    
    public boolean isLuckPermsUsePrefix() {
        return config.getBoolean("integrations.luckperms.use-prefix", true);
    }
    
    public boolean isLuckPermsUseSuffix() {
        return config.getBoolean("integrations.luckperms.use-suffix", false);
    }
    
    // === GUARD TAG CONFIGURATION ===
    
    public boolean isGuardTagEnabled() {
        return config.getBoolean("user-interface.guard-tags.enabled", true);
    }
    
    public String getGuardTagFormat() {
        return config.getString("user-interface.guard-tags.format", "<gradient:#9D4EDD:#06FFA5>[ON DUTY]</gradient>");
    }
    
    public String getGuardTagHoverFormat() {
        return config.getString("user-interface.guard-tags.hover-format", 
            "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "<gradient:#9D4EDD:#06FFA5><bold>CORRECTIONAL OFFICER</bold></gradient>\n" +
            "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "<gray>Rank: <aqua>%s\n" +
            "<gray>Status: <green><bold>ON DUTY</bold></green>\n" +
            "<gray>Total Arrests: <green>%d\n" +
            "<gray>Total Violations: <red>%d\n" +
            "<gray>Total Duty Time: <yellow>%s\n" +
            "<gray>Session Stats:\n" +
            "<gray>  • Searches: <cyan>%d\n" +
            "<gray>  • Arrests: <green>%d\n" +
            "<gray>  • Detections: <yellow>%d\n" +
            "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    public int getGuardTagPriority() {
        return config.getInt("user-interface.guard-tags.priority", 1000);
    }
    
    public boolean isGuardTagHoverEnabled() {
        return config.getBoolean("user-interface.guard-tags.hover-enabled", true);
    }
    
    public boolean isGuardTagShowSessionStats() {
        return config.getBoolean("user-interface.guard-tags.show-session-stats", true);
    }
    
    public boolean isGuardTagShowTotalStats() {
        return config.getBoolean("user-interface.guard-tags.show-total-stats", true);
    }
    
    // === PENALTY ESCALATION CONFIGURATION ===
    
    public boolean isPenaltyEscalationEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.enabled", true);
    }
    
    public int getPenaltyGracePeriod() {
        return config.getInt("guard-system.penalty-escalation.grace-period", 5);
    }
    
    public int getPenaltyStage1Time() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.time-minutes", 5);
    }
    
    public int getPenaltyStage1SlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.slowness-level", 1);
    }
    
    public int getPenaltyStage1EconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-1.economy-penalty", 1000);
    }
    
    public boolean isPenaltyStage1WarningEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.stages.stage-1.warning-message", true);
    }
    
    public int getPenaltyStage2Time() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.time-minutes", 10);
    }
    
    public int getPenaltyStage2SlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.slowness-level", 2);
    }
    
    public int getPenaltyStage2EconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.stage-2.economy-penalty", 1000);
    }
    
    public boolean isPenaltyStage2WarningEnabled() {
        return config.getBoolean("guard-system.penalty-escalation.stages.stage-2.warning-message", true);
    }
    
    public int getPenaltyRecurringInterval() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.interval-minutes", 5);
    }
    
    public int getPenaltyRecurringSlownessLevel() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.slowness-level", 2);
    }
    
    public int getPenaltyRecurringEconomyPenalty() {
        return config.getInt("guard-system.penalty-escalation.stages.recurring.economy-penalty", 1000);
    }
    
    public boolean isPenaltyRecurringWarningEnabled() {
        return (Boolean) getConfigValue("penalty-escalation.recurring.warning-enabled", true);
    }

    public int getPenaltyBypassEarnedTimeBonus() {
        return (Integer) getConfigValue("penalty-escalation.bypass.earned-time-bonus", 60);
    }
    
    public boolean isPenaltyBypassClearTracking() {
        return (Boolean) getConfigValue("penalty-escalation.bypass.clear-penalty-tracking", true);
    }
    
    public boolean isPenaltyBypassRemovePotionEffects() {
        return (Boolean) getConfigValue("penalty-escalation.bypass.remove-potion-effects", true);
    }

    // === CONFIGURATION SETTERS ===
    
    public void setDebugMode(boolean debug) {
        config.set("core.debug", debug);
        plugin.saveConfig();
    }
    
    public void setConfigValue(String path, Object value) {
        config.set(path, value);
        plugin.saveConfig();
    }
    
    public Object getConfigValue(String path, Object defaultValue) {
        return config.get(path, defaultValue);
    }
    
    public boolean hasConfigValue(String path) {
        return config.contains(path);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    // === BACKUP AND RESTORE ===
    
    public void backupConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            
            if (configFile.exists()) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(configFile);
                backupConfig.save(backupFile);
                logger.info("Configuration backed up to config.yml.backup");
            }
        } catch (Exception e) {
            logger.severe("Failed to backup configuration: " + e.getMessage());
        }
    }
    
    public void restoreConfig() {
        try {
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            
            if (backupFile.exists()) {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backupFile);
                backupConfig.save(configFile);
                loadConfig();
                logger.info("Configuration restored from backup");
            } else {
                logger.warning("No backup configuration found");
            }
        } catch (Exception e) {
            logger.severe("Failed to restore configuration: " + e.getMessage());
        }
    }
} 