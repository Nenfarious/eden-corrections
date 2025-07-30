package dev.lsdmc.edenCorrections;

import dev.lsdmc.edenCorrections.config.ConfigManager;
import dev.lsdmc.edenCorrections.managers.DutyManager;
import dev.lsdmc.edenCorrections.managers.WantedManager;
import dev.lsdmc.edenCorrections.managers.ChaseManager;
import dev.lsdmc.edenCorrections.managers.JailManager;
import dev.lsdmc.edenCorrections.managers.MessageManager;
import dev.lsdmc.edenCorrections.managers.ContrabandManager;
import dev.lsdmc.edenCorrections.managers.DutyBankingManager;
import dev.lsdmc.edenCorrections.managers.SecurityManager;
import dev.lsdmc.edenCorrections.managers.BossBarManager;
import dev.lsdmc.edenCorrections.storage.DataManager;
import dev.lsdmc.edenCorrections.events.GuardEventHandler;
import dev.lsdmc.edenCorrections.commands.CommandHandler;
import dev.lsdmc.edenCorrections.integrations.EdenCorrectionsExpansion;
import dev.lsdmc.edenCorrections.utils.WorldGuardUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.util.logging.Logger;
import java.util.Map;

public class EdenCorrections extends JavaPlugin {
    
    private static EdenCorrections instance;
    private Logger logger;
    
    // Core components
    private ConfigManager configManager;
    private DataManager dataManager;
    private MessageManager messageManager;
    private WorldGuardUtils worldGuardUtils;
    
    // Feature managers
    private DutyManager dutyManager;
    private WantedManager wantedManager;
    private ChaseManager chaseManager;
    private JailManager jailManager;
    private ContrabandManager contrabandManager;
    private DutyBankingManager dutyBankingManager;
    
    // Security and UI managers
    private SecurityManager securityManager;
    private BossBarManager bossBarManager;
    private TimeSyncManager timeSyncManager;
    private OfflineDutyManager offlineDutyManager;
    private SpamControlManager spamControlManager;
    private GuardLootManager guardLootManager;
    private LuckPermsMetaManager luckPermsMetaManager;
    private VaultEconomyManager vaultEconomyManager;
    private CMIIntegration cmiIntegration;
    
    // Event handler
    private GuardEventHandler eventHandler;
    
    // Command handler
    private CommandHandler commandHandler;
    
    // PlaceholderAPI integration
    private EdenCorrectionsExpansion placeholderExpansion;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        try {
            logger.info("Starting EdenCorrections v" + getDescription().getVersion());
            
            // Initialize core components
            initializeCore();
            
            // Initialize managers
            initializeManagers();
            
            // Register events and commands
            registerEventsAndCommands();
            
            // Register PlaceholderAPI integration
            registerPlaceholderAPI();
            
            // Post-initialization validation
            performStartupValidation();
            
            // Log system statistics
            logSystemStats();
            
            // Send startup message
            messageManager.sendMessage(getServer().getConsoleSender(), "system.startup");
            
            logger.info("EdenCorrections enabled successfully!");
            
        } catch (Exception e) {
            logger.severe("Failed to enable EdenCorrections: " + e.getMessage());
            e.printStackTrace();
            
            // Attempt graceful shutdown
            try {
                cleanupAllManagers();
            } catch (Exception cleanupException) {
                logger.severe("Error during emergency cleanup: " + cleanupException.getMessage());
            }
            
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        logger.info("Disabling EdenCorrections...");
        
        try {
            // Clean shutdown of managers in reverse order
            cleanupAllManagers();
            
            logger.info("EdenCorrections disabled successfully!");
            
        } catch (Exception e) {
            logger.severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupAllManagers() {
        // Clean up managers in reverse initialization order
        if (commandHandler != null) {
            logger.info("Cleaning up command handler...");
        }
        
        if (eventHandler != null) {
            logger.info("Cleaning up event handler...");
        }
        
        // Clean up new managers
        if (offlineDutyManager != null) {
            try {
                offlineDutyManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up OfflineDutyManager: " + e.getMessage());
            }
        }
        

        
        if (timeSyncManager != null) {
            try {
                timeSyncManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up TimeSyncManager: " + e.getMessage());
            }
        }
        
        if (spamControlManager != null) {
            try {
                spamControlManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up SpamControlManager: " + e.getMessage());
            }
        }
        
        if (luckPermsMetaManager != null) {
            try {
                luckPermsMetaManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up LuckPermsMetaManager: " + e.getMessage());
            }
        }
        
        if (dutyBankingManager != null) {
            try {
                dutyBankingManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up DutyBankingManager: " + e.getMessage());
            }
        }
        
        if (bossBarManager != null) {
            try {
                bossBarManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up BossBarManager: " + e.getMessage());
            }
        }
        
        if (securityManager != null) {
            try {
                securityManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up SecurityManager: " + e.getMessage());
            }
        }
        
        if (contrabandManager != null) {
            try {
                contrabandManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up ContrabandManager: " + e.getMessage());
            }
        }
        
        if (jailManager != null) {
            logger.info("Cleaning up JailManager...");
            // JailManager cleanup can be added in future if needed
        }
        
        if (chaseManager != null) {
            try {
                chaseManager.shutdown();
            } catch (Exception e) {
                logger.warning("Error cleaning up ChaseManager: " + e.getMessage());
            }
        }
        
        if (wantedManager != null) {
            logger.info("Cleaning up WantedManager...");
            // WantedManager cleanup can be added in future if needed
        }
        
        if (dutyManager != null) {
            try {
                dutyManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up DutyManager: " + e.getMessage());
            }
        }
        
        if (messageManager != null) {
            try {
                messageManager.cleanup();
            } catch (Exception e) {
                logger.warning("Error cleaning up MessageManager: " + e.getMessage());
            }
        }
        
        if (dataManager != null) {
            try {
                dataManager.shutdown();
            } catch (Exception e) {
                logger.warning("Error cleaning up DataManager: " + e.getMessage());
            }
        }
        
        if (worldGuardUtils != null) {
            logger.info("Cleaning up WorldGuardUtils...");
            // WorldGuardUtils doesn't need explicit cleanup, but we log it for completeness
        }
    }
    
    private void initializeCore() {
        // Initialize configuration
        configManager = new ConfigManager(this);
        
        // Validate configuration after loading
        if (!configManager.isConfigValid()) {
            logger.warning("Configuration validation failed - some features may not work correctly");
            logger.warning("Validation errors found:");
            for (String error : configManager.getValidationErrors()) {
                logger.warning("  - " + error);
            }
        }
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        messageManager.initialize();
        
        // Initialize data storage
        dataManager = new DataManager(this);
        dataManager.initialize();
        
        // Initialize WorldGuard utility
        worldGuardUtils = new WorldGuardUtils(this);
    }
    
    private void initializeManagers() {
        // Initialize integrations first
        vaultEconomyManager = new VaultEconomyManager(this);
        cmiIntegration = new CMIIntegration(this);
        
        // Initialize utility managers
        timeSyncManager = new TimeSyncManager(this);
        spamControlManager = new SpamControlManager(this);
        guardLootManager = new GuardLootManager(this);
        offlineDutyManager = new OfflineDutyManager(this);
        
        // Initialize LuckPerms meta manager if available
        try {
            luckPermsMetaManager = new LuckPermsMetaManager(this);
        } catch (Exception e) {
            logger.warning("LuckPerms not available - meta features disabled: " + e.getMessage());
        }
        
        // Initialize feature managers
        dutyManager = new DutyManager(this);
        wantedManager = new WantedManager(this);
        chaseManager = new ChaseManager(this);
        jailManager = new JailManager(this);
        contrabandManager = new ContrabandManager(this);
        dutyBankingManager = new DutyBankingManager(this);
        
        // Initialize security and UI managers
        securityManager = new SecurityManager(this);
        bossBarManager = new BossBarManager(this);
        
        // Initialize integrations
        vaultEconomyManager.initialize();
        
        // Initialize utility managers
        timeSyncManager.initialize();
        spamControlManager.initialize();
        guardLootManager.initialize();
        offlineDutyManager.initialize();
        
        // Initialize LuckPerms meta manager if available
        if (luckPermsMetaManager != null) {
            luckPermsMetaManager.initialize();
        }
        
        // Initialize feature managers
        dutyManager.initialize();
        wantedManager.initialize();
        chaseManager.initialize();
        jailManager.initialize();
        contrabandManager.initialize();
        dutyBankingManager.initialize();
        securityManager.initialize();
        bossBarManager.initialize();
    }
    
    private void registerEventsAndCommands() {
        // Register event handler
        eventHandler = new GuardEventHandler(this);
        getServer().getPluginManager().registerEvents(eventHandler, this);
        
        // Register command handler
        commandHandler = new CommandHandler(this);
        commandHandler.registerCommands();
    }
    
    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderExpansion = new EdenCorrectionsExpansion(this);
                placeholderExpansion.register();
                logger.info("PlaceholderAPI integration registered successfully!");
            } catch (Exception e) {
                logger.warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
            }
        } else {
            logger.info("PlaceholderAPI not found - placeholder integration disabled");
        }
    }
    
    public void reload() {
        try {
            logger.info("Reloading EdenCorrections configuration...");
            
            configManager.reload();
            messageManager.reload();
            
            // Report configuration validation results
            if (!configManager.isConfigValid()) {
                logger.warning("Configuration validation failed after reload - some features may not work correctly");
                logger.warning("Validation errors found:");
                for (String error : configManager.getValidationErrors()) {
                    logger.warning("  - " + error);
                }
            }
            
            logger.info("Configuration and messages reloaded successfully!");
            
        } catch (Exception e) {
            logger.severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Configuration reload failed", e);
        }
    }
    
    public boolean validateConfiguration() {
        // Configuration validation is now handled by ConfigManager
        return configManager.isConfigValid();
    }
    
    private void performStartupValidation() {
        try {
            logger.info("Performing startup validation...");
            
            boolean hasErrors = false;
            
            // Validate manager initialization
            if (configManager == null) {
                logger.severe("ConfigManager failed to initialize!");
                hasErrors = true;
            }
            
            if (dataManager == null) {
                logger.severe("DataManager failed to initialize!");
                hasErrors = true;
            }
            
            if (messageManager == null) {
                logger.severe("MessageManager failed to initialize!");
                hasErrors = true;
            }
            
            // Validate message configuration
            try {
                messageManager.validateMessages();
            } catch (Exception e) {
                logger.warning("Message validation issues detected: " + e.getMessage());
            }
            
            // Test plugin integrations
            validatePluginIntegrations();
            
            if (hasErrors) {
                throw new RuntimeException("Critical startup validation failures detected");
            }
            
            logger.info("Startup validation completed successfully");
            
        } catch (Exception e) {
            logger.severe("Startup validation failed: " + e.getMessage());
            throw new RuntimeException("Startup validation failed", e);
        }
    }
    
    private void validatePluginIntegrations() {
        // Test LuckPerms integration if available
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                if (dutyManager != null) {
                    // Test rank detection with a dummy check
                    logger.info("LuckPerms integration verified");
                }
            } catch (Exception e) {
                logger.warning("LuckPerms integration test failed: " + e.getMessage());
            }
        }
        
        // Test WorldGuard integration if available
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                if (worldGuardUtils != null) {
                    logger.info("WorldGuard integration verified - " + (worldGuardUtils.isWorldGuardEnabled() ? "Enabled" : "Disabled"));
                }
            } catch (Exception e) {
                logger.warning("WorldGuard integration test failed: " + e.getMessage());
            }
        }
        
        // Test CoinsEngine integration if available
        if (getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            if (configManager.isDutyBankingEnabled()) {
                String currencyCommand = configManager.getCurrencyCommand();
                if (currencyCommand != null && !currencyCommand.trim().isEmpty()) {
                    logger.info("CoinsEngine integration configured for duty banking");
                } else {
                    logger.warning("CoinsEngine available but currency command not configured");
                }
            }
        } else if (configManager.isDutyBankingEnabled()) {
            logger.warning("Duty banking enabled but CoinsEngine not found");
        }
        
        // Test CMI integration if available
        if (getServer().getPluginManager().getPlugin("CMI") != null) {
            logger.info("CMI integration available for kit distribution");
        } else {
            logger.warning("CMI not found - guard kits will not be given");
        }
    }
    
    public void logSystemStats() {
        try {
            logger.info("=== EdenCorrections System Statistics ===");
            logger.info("Version: " + getDescription().getVersion());
            logger.info("Debug Mode: " + configManager.isDebugMode());
            logger.info("Online Players: " + getServer().getOnlinePlayers().size() + "/" + getServer().getMaxPlayers());
            
            if (dataManager != null) {
                logger.info("Active Chases: " + dataManager.getAllActiveChases().size());
            }
            
            // Log integration status
            logger.info("LuckPerms Integration: " + (getServer().getPluginManager().getPlugin("LuckPerms") != null ? "Available" : "Not Found"));
            logger.info("WorldGuard Integration: " + (getServer().getPluginManager().getPlugin("WorldGuard") != null ? "Available" : "Not Found"));
            logger.info("CoinsEngine Integration: " + (getServer().getPluginManager().getPlugin("CoinsEngine") != null ? "Available" : "Not Found"));
            logger.info("CMI Integration: " + (getServer().getPluginManager().getPlugin("CMI") != null ? "Available" : "Not Found"));
            
            logger.info("=== End System Statistics ===");
            
        } catch (Exception e) {
            logger.warning("Error logging system statistics: " + e.getMessage());
        }
    }
    
    public void generateDiagnosticReport() {
        logger.info("=== EdenCorrections Comprehensive Diagnostic Report ===");
        
        try {
            // System Information
            logger.info("Plugin Version: " + getDescription().getVersion());
            logger.info("Server Version: " + getServer().getVersion());
            logger.info("Bukkit Version: " + getServer().getBukkitVersion());
            
            // Configuration Status
            logger.info("Configuration Valid: " + configManager.isConfigValid());
            if (!configManager.isConfigValid()) {
                logger.info("Configuration Errors:");
                for (String error : configManager.getValidationErrors()) {
                    logger.info("  - " + error);
                }
            }
            
            // Manager Status
            logger.info("DutyManager: " + (dutyManager != null ? "Initialized" : "Not Initialized"));
            logger.info("WantedManager: " + (wantedManager != null ? "Initialized" : "Not Initialized"));
            logger.info("ChaseManager: " + (chaseManager != null ? "Initialized" : "Not Initialized"));
            logger.info("JailManager: " + (jailManager != null ? "Initialized" : "Not Initialized"));
            logger.info("ContrabandManager: " + (contrabandManager != null ? "Initialized" : "Not Initialized"));
            logger.info("DutyBankingManager: " + (dutyBankingManager != null ? "Initialized" : "Not Initialized"));
            
            // Message Manager Status
            if (messageManager != null) {
                logger.info("MessageManager PlaceholderAPI: " + messageManager.isPlaceholderAPIEnabled());
                logger.info("Missing Messages: " + messageManager.getMissingMessages().size());
                logger.info("Invalid Messages: " + messageManager.getInvalidMessages().size());
                
                if (configManager.isDebugMode()) {
                    logger.info("Message Usage Statistics:");
                    messageManager.getMessageUsageStats().entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(10)
                        .forEach(entry -> logger.info("  " + entry.getKey() + ": " + entry.getValue()));
                }
            }
            
            // Integration Status
            logger.info("=== Integration Status ===");
            logger.info("LuckPerms: " + (getServer().getPluginManager().getPlugin("LuckPerms") != null ? "Available" : "Not Found"));
            logger.info("WorldGuard: " + (getServer().getPluginManager().getPlugin("WorldGuard") != null ? "Available" : "Not Found"));
            logger.info("PlaceholderAPI: " + (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null ? "Available" : "Not Found"));
            logger.info("CoinsEngine: " + (getServer().getPluginManager().getPlugin("CoinsEngine") != null ? "Available" : "Not Found"));
            logger.info("CMI: " + (getServer().getPluginManager().getPlugin("CMI") != null ? "Available" : "Not Found"));
            
            // WorldGuard detailed status
            if (worldGuardUtils != null) {
                worldGuardUtils.generateDiagnosticReport();
            }
            
            // Runtime Statistics
            if (dataManager != null) {
                logger.info("=== Runtime Statistics ===");
                logger.info("Active Chases: " + dataManager.getAllActiveChases().size());
                logger.info("Online Players: " + getServer().getOnlinePlayers().size());
                
                int guardsOnDuty = 0;
                int wantedPlayers = 0;
                for (Player player : getServer().getOnlinePlayers()) {
                    if (dutyManager != null && dutyManager.isOnDuty(player)) {
                        guardsOnDuty++;
                    }
                    if (wantedManager != null && wantedManager.isWanted(player)) {
                        wantedPlayers++;
                    }
                }
                logger.info("Guards on Duty: " + guardsOnDuty);
                logger.info("Wanted Players: " + wantedPlayers);
            }
            
            // Configuration Settings Summary
            logger.info("=== Configuration Summary ===");
            logger.info("Debug Mode: " + configManager.isDebugMode());
            logger.info("Contraband Enabled: " + configManager.isContrabandEnabled());
            logger.info("Duty Banking Enabled: " + configManager.isDutyBankingEnabled());
            logger.info("Max Wanted Level: " + configManager.getMaxWantedLevel());
            logger.info("Max Chase Distance: " + configManager.getMaxChaseDistance());
            logger.info("Max Concurrent Chases: " + configManager.getMaxConcurrentChases());
            
            logger.info("=== End Diagnostic Report ===");
            
        } catch (Exception e) {
            logger.severe("Error generating diagnostic report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Getters
    public static EdenCorrections getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DutyManager getDutyManager() {
        return dutyManager;
    }
    
    public WantedManager getWantedManager() {
        return wantedManager;
    }
    
    public ChaseManager getChaseManager() {
        return chaseManager;
    }
    
    public JailManager getJailManager() {
        return jailManager;
    }
    
    public ContrabandManager getContrabandManager() {
        return contrabandManager;
    }
    
    public DutyBankingManager getDutyBankingManager() {
        return dutyBankingManager;
    }
    
    public GuardEventHandler getEventHandler() {
        return eventHandler;
    }
    
    public CommandHandler getCommandHandler() {
        return commandHandler;
    }
    
    public WorldGuardUtils getWorldGuardUtils() {
        return worldGuardUtils;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    

    
    public TimeSyncManager getTimeSyncManager() {
        return timeSyncManager;
    }
    
    public OfflineDutyManager getOfflineDutyManager() {
        return offlineDutyManager;
    }
    
    public SpamControlManager getSpamControlManager() {
        return spamControlManager;
    }
    
    public GuardLootManager getGuardLootManager() {
        return guardLootManager;
    }
    
    public LuckPermsMetaManager getLuckPermsMetaManager() {
        return luckPermsMetaManager;
    }
    
    public VaultEconomyManager getVaultEconomyManager() {
        return vaultEconomyManager;
    }
    
    public CMIIntegration getCMIIntegration() {
        return cmiIntegration;
    }
} 