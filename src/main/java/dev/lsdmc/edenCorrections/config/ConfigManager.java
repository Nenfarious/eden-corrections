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

public class ConfigManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private FileConfiguration config;
    
    // Configuration validation cache
    private boolean configValid = true;
    private final List<String> validationErrors = new ArrayList<>();
    
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
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            // Set defaults if not present
            setDefaults();
            
            // Validate configuration
            validateConfiguration();
            
            if (configValid) {
                logger.info("Configuration loaded and validated successfully!");
            } else {
                logger.warning("Configuration loaded with " + validationErrors.size() + " validation errors:");
                for (String error : validationErrors) {
                    logger.warning("  - " + error);
                }
            }
            
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setDefaults() {
        // Core settings
        config.addDefault("debug", false);
        config.addDefault("language", "en");
        
        // Time settings (in seconds)
        config.addDefault("times.duty-transition", 10);
        config.addDefault("times.chase-duration", 300);
        config.addDefault("times.wanted-duration", 1800);
        config.addDefault("times.jail-countdown", 10);
        config.addDefault("times.contraband-compliance", 10);
        
        // Jail settings
        config.addDefault("jail.base-time", 300);
        config.addDefault("jail.level-multiplier", 60);
        config.addDefault("jail.max-wanted-level", 5);
        
        // Chase settings
        config.addDefault("chase.max-distance", 100);
        config.addDefault("chase.warning-distance", 20);
        config.addDefault("chase.max-concurrent", 3);
        
        // Contraband settings
        config.addDefault("contraband.enabled", true);
        config.addDefault("contraband.drug-detection", true);
        
        // Guard restrictions
        config.addDefault("guard-restrictions.block-mining", true);
        config.addDefault("guard-restrictions.block-crafting", true);
        config.addDefault("guard-restrictions.block-storage", true);
        
        // Database settings
        config.addDefault("database.type", "sqlite");
        config.addDefault("database.sqlite.file", "edencorrections.db");
        
        // === ENHANCED SYSTEM DEFAULTS ===
        
        // Guard System
        config.addDefault("guard-system.duty-region", "guard_station");
        config.addDefault("guard-system.immobilization-time", 5);
        config.addDefault("guard-system.rank-mappings.trainee", "trainee");
        config.addDefault("guard-system.rank-mappings.private", "private");
        config.addDefault("guard-system.rank-mappings.officer", "officer");
        config.addDefault("guard-system.rank-mappings.sergeant", "sergeant");
        config.addDefault("guard-system.rank-mappings.captain", "captain");
        config.addDefault("guard-system.rank-mappings.warden", "warden");
        config.addDefault("guard-system.kit-mappings.trainee", "trainee");
        config.addDefault("guard-system.kit-mappings.private", "private");
        config.addDefault("guard-system.kit-mappings.officer", "officer");
        config.addDefault("guard-system.kit-mappings.sergeant", "sergeant");
        config.addDefault("guard-system.kit-mappings.captain", "captain");
        config.addDefault("guard-system.kit-mappings.warden", "warden");
        
        // Enhanced Contraband System
        config.addDefault("contraband.max-request-distance", 5);
        config.addDefault("contraband.grace-period", 3);
        config.addDefault("contraband.types.sword.items", "WOODEN_SWORD,STONE_SWORD,IRON_SWORD,GOLDEN_SWORD,DIAMOND_SWORD,NETHERITE_SWORD");
        config.addDefault("contraband.types.sword.description", "All swords and bladed weapons");
        config.addDefault("contraband.types.bow.items", "BOW,CROSSBOW");
        config.addDefault("contraband.types.bow.description", "All ranged weapons");
        config.addDefault("contraband.types.armor.items", "LEATHER_HELMET,LEATHER_CHESTPLATE,LEATHER_LEGGINGS,LEATHER_BOOTS,CHAINMAIL_HELMET,CHAINMAIL_CHESTPLATE,CHAINMAIL_LEGGINGS,CHAINMAIL_BOOTS,IRON_HELMET,IRON_CHESTPLATE,IRON_LEGGINGS,IRON_BOOTS,GOLDEN_HELMET,GOLDEN_CHESTPLATE,GOLDEN_LEGGINGS,GOLDEN_BOOTS,DIAMOND_HELMET,DIAMOND_CHESTPLATE,DIAMOND_LEGGINGS,DIAMOND_BOOTS,NETHERITE_HELMET,NETHERITE_CHESTPLATE,NETHERITE_LEGGINGS,NETHERITE_BOOTS");
        config.addDefault("contraband.types.armor.description", "All armor pieces");
        config.addDefault("contraband.types.drugs.items", "SUGAR,NETHER_WART,SPIDER_EYE,FERMENTED_SPIDER_EYE,BLAZE_POWDER");
        config.addDefault("contraband.types.drugs.description", "Illegal substances and drugs");
        
        // Combat Timer
        config.addDefault("combat-timer.duration", 5);
        config.addDefault("combat-timer.prevent-capture", true);
        config.addDefault("combat-timer.prevent-teleport", true);
        
        // Duty Banking
        config.addDefault("duty-banking.enabled", true);
        config.addDefault("duty-banking.conversion-rate", 100);
        config.addDefault("duty-banking.minimum-conversion", 300);
        config.addDefault("duty-banking.auto-convert", false);
        config.addDefault("duty-banking.auto-convert-threshold", 3600);
        config.addDefault("duty-banking.currency-command", "et give {player} {amount}");
        
        // Region System
        config.addDefault("regions.no-chase-zones", "safezone");
        config.addDefault("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        
        // Performance Settings
        config.addDefault("performance.chase-check-interval", 5);
        config.addDefault("performance.wanted-check-interval", 60);
        config.addDefault("performance.cleanup-interval", 300);
        
        // Integration Settings
        config.addDefault("integrations.placeholderapi.enabled", true);
        config.addDefault("integrations.luckperms.strict-mode", true);
        config.addDefault("integrations.worldguard.required", false);
        config.addDefault("integrations.cmi.kits-enabled", true);
        
        // Save defaults
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
    }
    
    private void validateCoreSettings() {
        // Validate language
        String language = config.getString("language", "en");
        if (!Arrays.asList("en", "es", "fr", "de").contains(language)) {
            addValidationError("Invalid language: " + language + " (defaulting to 'en')");
        }
    }
    
    private void validateTimeSettings() {
        validatePositiveInt("times.duty-transition", "Duty transition time");
        validatePositiveInt("times.chase-duration", "Chase duration");
        validatePositiveInt("times.wanted-duration", "Wanted duration");
        validatePositiveInt("times.jail-countdown", "Jail countdown");
        validatePositiveInt("times.contraband-compliance", "Contraband compliance time");
    }
    
    private void validateJailSettings() {
        validatePositiveInt("jail.base-time", "Base jail time");
        validatePositiveInt("jail.level-multiplier", "Jail level multiplier");
        validateRange("jail.max-wanted-level", "Max wanted level", 1, 10);
    }
    
    private void validateChaseSettings() {
        validatePositiveInt("chase.max-distance", "Max chase distance");
        validatePositiveInt("chase.warning-distance", "Chase warning distance");
        validatePositiveInt("chase.max-concurrent", "Max concurrent chases");
        
        // Validate logical relationships
        int maxDistance = config.getInt("chase.max-distance", 100);
        int warningDistance = config.getInt("chase.warning-distance", 20);
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
        if (!config.getBoolean("contraband.enabled", true)) {
            return; // Skip validation if disabled
        }
        
        validatePositiveInt("contraband.max-request-distance", "Max contraband request distance");
        validateNonNegativeInt("contraband.grace-period", "Contraband grace period");
        
        // Validate contraband types
        ConfigurationSection contrabandTypes = config.getConfigurationSection("contraband.types");
        if (contrabandTypes != null) {
            for (String type : contrabandTypes.getKeys(false)) {
                validateContrabandType(type);
            }
        }
    }
    
    private void validateContrabandType(String type) {
        String itemsPath = "contraband.types." + type + ".items";
        String descriptionPath = "contraband.types." + type + ".description";
        
        String items = config.getString(itemsPath, "");
        if (items.trim().isEmpty()) {
            addValidationError("Contraband type '" + type + "' has no items configured");
            return;
        }
        
        // Validate that items are valid materials
        String[] itemArray = items.split(",");
        for (String item : itemArray) {
            try {
                Material.valueOf(item.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                addValidationError("Invalid material in contraband type '" + type + "': " + item.trim());
            }
        }
        
        String description = config.getString(descriptionPath, "");
        if (description.trim().isEmpty()) {
            addValidationError("Contraband type '" + type + "' has no description");
        }
    }
    
    private void validateCombatTimer() {
        validateNonNegativeInt("combat-timer.duration", "Combat timer duration");
    }
    
    private void validateDutyBanking() {
        if (!config.getBoolean("duty-banking.enabled", true)) {
            return; // Skip validation if disabled
        }
        
        validatePositiveInt("duty-banking.conversion-rate", "Duty banking conversion rate");
        validateNonNegativeInt("duty-banking.minimum-conversion", "Minimum conversion time");
        validatePositiveInt("duty-banking.auto-convert-threshold", "Auto-convert threshold");
        
        // Validate currency command
        String currencyCommand = config.getString("duty-banking.currency-command", "");
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
        validatePositiveInt("performance.chase-check-interval", "Chase check interval");
        validatePositiveInt("performance.wanted-check-interval", "Wanted check interval");
        validatePositiveInt("performance.cleanup-interval", "Cleanup interval");
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
        loadConfig();
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
        return config.getBoolean("debug", false);
    }
    
    public String getLanguage() {
        return config.getString("language", "en");
    }
    
    public int getDutyTransitionTime() {
        return config.getInt("times.duty-transition", 10);
    }
    
    public int getChaseDuration() {
        return config.getInt("times.chase-duration", 300);
    }
    
    public int getWantedDuration() {
        return config.getInt("times.wanted-duration", 1800);
    }
    
    public int getJailCountdown() {
        return config.getInt("times.jail-countdown", 10);
    }
    
    public int getContrabandCompliance() {
        return config.getInt("times.contraband-compliance", 10);
    }
    
    public int getBaseJailTime() {
        return config.getInt("jail.base-time", 300);
    }
    
    public int getJailLevelMultiplier() {
        return config.getInt("jail.level-multiplier", 60);
    }
    
    public int getMaxWantedLevel() {
        return config.getInt("jail.max-wanted-level", 5);
    }
    
    public int getMaxChaseDistance() {
        return config.getInt("chase.max-distance", 100);
    }
    
    public int getChaseWarningDistance() {
        return config.getInt("chase.warning-distance", 20);
    }
    
    public int getMaxConcurrentChases() {
        return config.getInt("chase.max-concurrent", 3);
    }
    
    public boolean isContrabandEnabled() {
        return config.getBoolean("contraband.enabled", true);
    }
    
    public boolean isDrugDetectionEnabled() {
        return config.getBoolean("contraband.drug-detection", true);
    }
    
    public boolean isGuardMiningBlocked() {
        return config.getBoolean("guard-restrictions.block-mining", true);
    }
    
    public boolean isGuardCraftingBlocked() {
        return config.getBoolean("guard-restrictions.block-crafting", true);
    }
    
    public boolean isGuardStorageBlocked() {
        return config.getBoolean("guard-restrictions.block-storage", true);
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "edencorrections.db");
    }
    
    // === ENHANCED SYSTEM GETTERS ===
    
    // Guard System Configuration
    public String getDutyRegion() {
        return config.getString("guard-system.duty-region", "guard_station");
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
        return config.getConfigurationSection("contraband.types");
    }
    
    public int getMaxRequestDistance() {
        return config.getInt("contraband.max-request-distance", 5);
    }
    
    public int getGracePeriod() {
        return config.getInt("contraband.grace-period", 3);
    }
    
    public String getContrabandItems(String type) {
        return config.getString("contraband.types." + type + ".items", "");
    }
    
    public String getContrabandDescription(String type) {
        return config.getString("contraband.types." + type + ".description", type);
    }
    
    // Combat Timer Configuration
    public int getCombatTimerDuration() {
        return config.getInt("combat-timer.duration", 5);
    }
    
    public boolean shouldPreventCaptureInCombat() {
        return config.getBoolean("combat-timer.prevent-capture", true);
    }
    
    public boolean shouldPreventTeleportInCombat() {
        return config.getBoolean("combat-timer.prevent-teleport", true);
    }
    
    // Duty Banking Configuration
    public boolean isDutyBankingEnabled() {
        return config.getBoolean("duty-banking.enabled", true);
    }
    
    public int getConversionRate() {
        return config.getInt("duty-banking.conversion-rate", 100);
    }
    
    public int getMinimumConversion() {
        return config.getInt("duty-banking.minimum-conversion", 300);
    }
    
    public boolean isAutoConvert() {
        return config.getBoolean("duty-banking.auto-convert", false);
    }
    
    public int getAutoConvertThreshold() {
        return config.getInt("duty-banking.auto-convert-threshold", 3600);
    }
    
    public String getCurrencyCommand() {
        return config.getString("duty-banking.currency-command", "et give {player} {amount}");
    }
    
    // Region Configuration
    public String[] getNoChaseZones() {
        String zones = config.getString("regions.no-chase-zones", "safezone");
        return zones.split(",");
    }
    
    public String[] getDutyRequiredZones() {
        String zones = config.getString("regions.duty-required-zones", "guard_lockers,guard_lockers2,guardplotstairs");
        return zones.split(",");
    }
    
    // Performance Configuration
    public int getChaseCheckInterval() {
        return config.getInt("performance.chase-check-interval", 5);
    }
    
    public int getWantedCheckInterval() {
        return config.getInt("performance.wanted-check-interval", 60);
    }
    
    public int getCleanupInterval() {
        return config.getInt("performance.cleanup-interval", 300);
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
    
    // === UTILITY METHODS ===
    
    public void setDebugMode(boolean debug) {
        config.set("debug", debug);
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