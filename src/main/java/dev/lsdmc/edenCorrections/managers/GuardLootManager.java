package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

/**
 * Manages guard loot drops when guards die on duty
 * Instead of dropping their actual inventory, drops configured loot pools
 */
public class GuardLootManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final Random random;
    
    private FileConfiguration lootConfig;
    private File lootFile;
    
    // Cached loot data for performance
    private final Map<String, LootPool> lootPools;
    private boolean announceDealths;
    private boolean dropExperience;
    private int baseExperienceAmount;
    private boolean allowInventoryRetrieval;
    private int retrievalCost;
    private long retrievalTimeLimit;
    private boolean debugMode;
    
    public GuardLootManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.random = new Random();
        this.lootPools = new HashMap<>();
    }
    
    public void initialize() {
        loadLootConfiguration();
        logger.info("GuardLootManager initialized with " + lootPools.size() + " loot pools");
    }
    
    private void loadLootConfiguration() {
        // Create loot.yml if it doesn't exist
        lootFile = new File(plugin.getDataFolder(), "loot.yml");
        if (!lootFile.exists()) {
            try {
                // Copy default loot.yml from resources
                InputStream defaultLoot = plugin.getResource("loot.yml");
                if (defaultLoot != null) {
                    Files.copy(defaultLoot, lootFile.toPath());
                    logger.info("Created default loot.yml configuration");
                } else {
                    // Create a basic config if resource doesn't exist
                    lootFile.createNewFile();
                    logger.warning("Created empty loot.yml - please configure guard loot pools");
                }
            } catch (IOException e) {
                logger.severe("Failed to create loot.yml: " + e.getMessage());
                return;
            }
        }
        
        // Load the configuration
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        
        // Load global settings
        ConfigurationSection settings = lootConfig.getConfigurationSection("settings");
        if (settings != null) {
            announceDealths = settings.getBoolean("announce-guard-deaths", true);
            dropExperience = settings.getBoolean("drop-experience", true);
            baseExperienceAmount = settings.getInt("experience-amount", 50);
            allowInventoryRetrieval = settings.getBoolean("allow-inventory-retrieval", true);
            retrievalCost = settings.getInt("retrieval-cost", 1000);
            retrievalTimeLimit = settings.getLong("retrieval-time-limit", 1800);
            debugMode = settings.getBoolean("debug-mode", false);
        }
        
        // Load loot pools
        ConfigurationSection guardLoot = lootConfig.getConfigurationSection("guard-loot");
        if (guardLoot != null) {
            for (String rank : guardLoot.getKeys(false)) {
                ConfigurationSection rankSection = guardLoot.getConfigurationSection(rank);
                if (rankSection != null && rankSection.getBoolean("enabled", true)) {
                    LootPool pool = loadLootPool(rankSection);
                    if (pool != null) {
                        lootPools.put(rank.toLowerCase(), pool);
                        if (debugMode) {
                            logger.info("Loaded loot pool for rank: " + rank + " with " + pool.items.size() + " items");
                        }
                    }
                }
            }
        }
        
        logger.info("Loaded guard loot configuration with " + lootPools.size() + " loot pools");
    }
    
    private LootPool loadLootPool(ConfigurationSection section) {
        try {
            int minItems = section.getInt("min-items", 2);
            int maxItems = section.getInt("max-items", 4);
            
            List<LootItem> items = new ArrayList<>();
            List<String> itemStrings = section.getStringList("items");
            
            for (String itemString : itemStrings) {
                LootItem item = parseLootItem(itemString);
                if (item != null) {
                    items.add(item);
                }
            }
            
            if (items.isEmpty()) {
                logger.warning("Loot pool has no valid items: " + section.getCurrentPath());
                return null;
            }
            
            return new LootPool(minItems, maxItems, items);
        } catch (Exception e) {
            logger.warning("Failed to load loot pool " + section.getCurrentPath() + ": " + e.getMessage());
            return null;
        }
    }
    
    private LootItem parseLootItem(String itemString) {
        try {
            String[] parts = itemString.split(":");
            if (parts.length < 3) {
                logger.warning("Invalid loot item format: " + itemString);
                return null;
            }
            
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);
            int weight = Integer.parseInt(parts[2]);
            
            return new LootItem(material, amount, weight);
        } catch (Exception e) {
            logger.warning("Failed to parse loot item: " + itemString + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Handle guard death by dropping appropriate loot instead of inventory
     */
    public void handleGuardDeath(Player guard) {
        if (!plugin.getDutyManager().isOnDuty(guard)) {
            return; // Only handle deaths of guards who are on duty
        }
        
        String guardRank = plugin.getDutyManager().getPlayerGuardRank(guard);
        if (guardRank == null) {
            return;
        }
        
        // Get appropriate loot pool
        LootPool lootPool = getLootPoolForRank(guardRank);
        if (lootPool == null) {
            logger.warning("No loot pool configured for guard rank: " + guardRank);
            return;
        }
        
        // Generate and drop loot
        List<ItemStack> lootItems = generateLoot(lootPool);
        Location deathLocation = guard.getLocation();
        
        for (ItemStack item : lootItems) {
            deathLocation.getWorld().dropItemNaturally(deathLocation, item);
        }
        
        // Drop experience if configured
        if (dropExperience) {
            int expAmount = calculateExperienceAmount(guardRank);
            ExperienceOrb orb = deathLocation.getWorld().spawn(deathLocation, ExperienceOrb.class);
            orb.setExperience(expAmount);
        }
        
        // Announce death if configured
        if (announceDealths) {
            plugin.getMessageManager().sendGuardAlert("guard.death.announcement",
                playerPlaceholder("guard", guard),
                stringPlaceholder("rank", guardRank),
                stringPlaceholder("location", deathLocation.getBlockX() + "," + deathLocation.getBlockY() + "," + deathLocation.getBlockZ()));
        }
        
        if (debugMode) {
            logger.info("Generated " + lootItems.size() + " loot items for guard " + guard.getName() + " (rank: " + guardRank + ")");
        }
    }
    
    private LootPool getLootPoolForRank(String rank) {
        if (rank == null) {
            return lootPools.get("default");
        }
        
        // Try exact rank match first
        LootPool pool = lootPools.get(rank.toLowerCase());
        if (pool != null) {
            return pool;
        }
        
        // Fall back to default
        return lootPools.get("default");
    }
    
    private List<ItemStack> generateLoot(LootPool pool) {
        List<ItemStack> loot = new ArrayList<>();
        
        // Determine number of items to drop
        int itemCount = random.nextInt(pool.maxItems - pool.minItems + 1) + pool.minItems;
        
        // Calculate total weight
        int totalWeight = pool.items.stream().mapToInt(item -> item.weight).sum();
        
        // Generate items
        for (int i = 0; i < itemCount; i++) {
            LootItem selectedItem = selectRandomItem(pool.items, totalWeight);
            if (selectedItem != null) {
                loot.add(new ItemStack(selectedItem.material, selectedItem.amount));
            }
        }
        
        return loot;
    }
    
    private LootItem selectRandomItem(List<LootItem> items, int totalWeight) {
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (LootItem item : items) {
            currentWeight += item.weight;
            if (randomValue < currentWeight) {
                return item;
            }
        }
        
        // Fallback to first item if something goes wrong
        return items.isEmpty() ? null : items.get(0);
    }
    
    private int calculateExperienceAmount(String rank) {
        // Base experience, potentially modified by rank
        int multiplier = 1;
        
        switch (rank.toLowerCase()) {
            case "officer":
                multiplier = 2;
                break;
            case "sergeant":
                multiplier = 3;
                break;
            case "lieutenant":
                multiplier = 4;
                break;
            case "captain":
                multiplier = 5;
                break;
        }
        
        return baseExperienceAmount * multiplier;
    }
    
    public void reloadConfiguration() {
        lootPools.clear();
        loadLootConfiguration();
        logger.info("Reloaded guard loot configuration");
    }
    
    // Data classes
    private static class LootPool {
        final int minItems;
        final int maxItems;
        final List<LootItem> items;
        
        LootPool(int minItems, int maxItems, List<LootItem> items) {
            this.minItems = minItems;
            this.maxItems = maxItems;
            this.items = items;
        }
    }
    
    private static class LootItem {
        final Material material;
        final int amount;
        final int weight;
        
        LootItem(Material material, int amount, int weight) {
            this.material = material;
            this.amount = amount;
            this.weight = weight;
        }
    }
}