package dev.lsdmc.edenCorrections.utils;

import dev.lsdmc.edenCorrections.EdenCorrections;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Centralized WorldGuard operations manager for EdenCorrections
 * Handles all region-related functionality and WorldGuard integration
 */
public class WorldGuardUtils {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // WorldGuard components
    private WorldGuardPlugin worldGuardPlugin;
    private RegionContainer regionContainer;
    private boolean worldGuardEnabled;
    
    public WorldGuardUtils(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.worldGuardEnabled = false;
        
        initializeWorldGuard();
    }
    
    /**
     * Initialize WorldGuard integration
     */
    private void initializeWorldGuard() {
        try {
            // Check if WorldGuard is available
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                logger.info("WorldGuard not found - region restrictions will be disabled");
                return;
            }
            
            // Initialize WorldGuard plugin instance
            worldGuardPlugin = WorldGuardPlugin.inst();
            if (worldGuardPlugin == null) {
                logger.warning("Failed to get WorldGuard plugin instance");
                return;
            }
            
            // Initialize region container
            regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (regionContainer == null) {
                logger.warning("Failed to get WorldGuard region container");
                return;
            }
            
            worldGuardEnabled = true;
            logger.info("WorldGuard integration initialized successfully");
            
            // Validate configured regions
            validateConfiguredRegions();
            
        } catch (Exception e) {
            logger.severe("Failed to initialize WorldGuard integration: " + e.getMessage());
            e.printStackTrace();
            worldGuardEnabled = false;
        }
    }
    
    /**
     * Validate that all configured regions exist
     */
    private void validateConfiguredRegions() {
        if (!worldGuardEnabled) return;
        
        try {
            // Check duty region
            String dutyRegion = plugin.getConfigManager().getDutyRegion();
            if (!regionExists(dutyRegion)) {
                logger.warning("Configured duty region '" + dutyRegion + "' does not exist in any world");
            }
            
            // Check no-chase zones (safe zones)
            String[] noChaseZones = plugin.getConfigManager().getNoChaseZones();
            for (String zone : noChaseZones) {
                if (!regionExists(zone.trim())) {
                    logger.warning("Configured no-chase zone '" + zone + "' does not exist in any world");
                }
            }
            
            // Check duty-required zones
            String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
            for (String zone : dutyRequiredZones) {
                if (!regionExists(zone.trim())) {
                    logger.warning("Configured duty-required zone '" + zone + "' does not exist in any world");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error validating configured regions: " + e.getMessage());
        }
    }
    
    // === CORE REGION CHECKING METHODS ===
    
    /**
     * Check if a player is in a specific region
     */
    public boolean isPlayerInRegion(Player player, String regionName) {
        if (!worldGuardEnabled || player == null || regionName == null) {
            return false;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName.trim())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Error checking if player " + player.getName() + " is in region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a player is in any of the specified regions
     */
    public boolean isPlayerInAnyRegion(Player player, String[] regionNames) {
        if (!worldGuardEnabled || player == null || regionNames == null) {
            return false;
        }
        
        for (String regionName : regionNames) {
            if (isPlayerInRegion(player, regionName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a location is in a specific region
     */
    public boolean isLocationInRegion(Location location, String regionName) {
        if (!worldGuardEnabled || location == null || regionName == null) {
            return false;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName.trim())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Error checking if location is in region " + regionName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all regions at a player's location
     */
    public Set<String> getRegionsAtPlayer(Player player) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || player == null) {
            return regionNames;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions at player " + player.getName() + ": " + e.getMessage());
        }
        
        return regionNames;
    }
    
    /**
     * Get all regions at a location
     */
    public Set<String> getRegionsAtLocation(Location location) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || location == null) {
            return regionNames;
        }
        
        try {
            RegionQuery query = regionContainer.createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : regions) {
                regionNames.add(region.getId());
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions at location: " + e.getMessage());
        }
        
        return regionNames;
    }
    
    // === EDENCORRECTIONS-SPECIFIC REGION METHODS ===
    
    /**
     * Check if a player is in the duty region
     */
    public boolean isPlayerInDutyRegion(Player player) {
        if (!worldGuardEnabled) {
            return true; // Allow duty activation anywhere if WorldGuard not available
        }
        
        String dutyRegion = plugin.getConfigManager().getDutyRegion();
        return isPlayerInRegion(player, dutyRegion);
    }
    
    /**
     * Check if a player is in a safe zone (no-chase zone)
     */
    public boolean isPlayerInSafeZone(Player player) {
        if (!worldGuardEnabled) {
            return false; // No safe zones if WorldGuard not available
        }
        
        String[] safeZones = plugin.getConfigManager().getNoChaseZones();
        return isPlayerInAnyRegion(player, safeZones);
    }
    
    /**
     * Check if a player is in a no-chase zone (alias for isPlayerInSafeZone)
     */
    public boolean isPlayerInNoChaseZone(Player player) {
        return isPlayerInSafeZone(player);
    }
    
    /**
     * Check if a player is in a duty-required zone
     */
    public boolean isPlayerInDutyRequiredZone(Player player) {
        if (!worldGuardEnabled) {
            return false; // No requirements if WorldGuard not available
        }
        
        String[] dutyRequiredZones = plugin.getConfigManager().getDutyRequiredZones();
        return isPlayerInAnyRegion(player, dutyRequiredZones);
    }
    
    /**
     * Check if a location is in a safe zone
     */
    public boolean isLocationInSafeZone(Location location) {
        if (!worldGuardEnabled) {
            return false;
        }
        
        String[] safeZones = plugin.getConfigManager().getNoChaseZones();
        for (String zone : safeZones) {
            if (isLocationInRegion(location, zone)) {
                return true;
            }
        }
        
        return false;
    }
    
    // === REGION VALIDATION METHODS ===
    
    /**
     * Check if a region exists in any world
     */
    public boolean regionExists(String regionName) {
        if (!worldGuardEnabled || regionName == null) {
            return false;
        }
        
        try {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                World adaptedWorld = BukkitAdapter.adapt(world);
                RegionManager regionManager = regionContainer.get(adaptedWorld);
                
                if (regionManager != null && regionManager.getRegion(regionName) != null) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Error checking if region " + regionName + " exists: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all regions in a world
     */
    public Set<String> getAllRegionsInWorld(org.bukkit.World world) {
        Set<String> regionNames = new HashSet<>();
        
        if (!worldGuardEnabled || world == null) {
            return regionNames;
        }
        
        try {
            World adaptedWorld = BukkitAdapter.adapt(world);
            RegionManager regionManager = regionContainer.get(adaptedWorld);
            
            if (regionManager != null) {
                for (String regionName : regionManager.getRegions().keySet()) {
                    regionNames.add(regionName);
                }
            }
            
        } catch (Exception e) {
            logger.warning("Error getting regions in world " + world.getName() + ": " + e.getMessage());
        }
        
        return regionNames;
    }
    
    /**
     * Get all regions across all worlds
     */
    public Set<String> getAllRegions() {
        Set<String> allRegions = new HashSet<>();
        
        if (!worldGuardEnabled) {
            return allRegions;
        }
        
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            allRegions.addAll(getAllRegionsInWorld(world));
        }
        
        return allRegions;
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Check if WorldGuard is enabled and working
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    /**
     * Get the WorldGuard plugin instance
     */
    public WorldGuardPlugin getWorldGuardPlugin() {
        return worldGuardPlugin;
    }
    
    /**
     * Get the region container
     */
    public RegionContainer getRegionContainer() {
        return regionContainer;
    }
    
    /**
     * Force re-initialization of WorldGuard (for debugging)
     */
    public void reinitialize() {
        logger.info("Reinitializing WorldGuard integration...");
        initializeWorldGuard();
    }
    
    /**
     * Generate a diagnostic report for WorldGuard integration
     */
    public void generateDiagnosticReport() {
        logger.info("=== WorldGuard Integration Diagnostic Report ===");
        logger.info("WorldGuard Enabled: " + worldGuardEnabled);
        
        if (worldGuardEnabled) {
            logger.info("WorldGuard Plugin: " + (worldGuardPlugin != null ? "Available" : "Not Available"));
            logger.info("Region Container: " + (regionContainer != null ? "Available" : "Not Available"));
            
            // Log configured regions
            logger.info("Configured Duty Region: " + plugin.getConfigManager().getDutyRegion());
            logger.info("Configured No-Chase Zones: " + Arrays.toString(plugin.getConfigManager().getNoChaseZones()));
            logger.info("Configured Duty-Required Zones: " + Arrays.toString(plugin.getConfigManager().getDutyRequiredZones()));
            
            // Log total regions
            Set<String> allRegions = getAllRegions();
            logger.info("Total Regions Found: " + allRegions.size());
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("All Regions: " + allRegions);
            }
        }
        
        logger.info("=== End WorldGuard Diagnostic Report ===");
    }
    
    // === INTEGRATION HELPER METHODS ===
    
    /**
     * Check if a player can be chased at their current location
     */
    public boolean canPlayerBeChased(Player player) {
        if (!worldGuardEnabled) {
            return true; // Allow chasing anywhere if WorldGuard not available
        }
        
        // Cannot chase in safe zones (no-chase zones)
        return !isPlayerInSafeZone(player);
    }
    
    /**
     * Check if a player should be forced on duty at their current location
     */
    public boolean shouldForcePlayerOnDuty(Player player) {
        if (!worldGuardEnabled) {
            return false; // No forced duty if WorldGuard not available
        }
        
        // Force duty in duty-required zones
        return isPlayerInDutyRequiredZone(player);
    }
    
    /**
     * Get a user-friendly description of the player's current region context
     */
    public String getPlayerRegionContext(Player player) {
        if (!worldGuardEnabled) {
            return "WorldGuard not available";
        }
        
        Set<String> regions = getRegionsAtPlayer(player);
        if (regions.isEmpty()) {
            return "No regions (wilderness)";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Regions: ").append(String.join(", ", regions));
        
        // Add special zone indicators
        if (isPlayerInSafeZone(player)) {
            context.append(" [SAFE ZONE/NO CHASE]");
        }
        if (isPlayerInDutyRequiredZone(player)) {
            context.append(" [DUTY REQUIRED]");
        }
        if (isPlayerInDutyRegion(player)) {
            context.append(" [DUTY REGION]");
        }
        
        return context.toString();
    }
}
