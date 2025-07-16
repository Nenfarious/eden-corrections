package dev.lsdmc.edenCorrections.utils;

import com.google.gson.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class InventorySerializer {
    
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Logger logger = Logger.getLogger(InventorySerializer.class.getName());
    
    /**
     * Serialize a player's entire inventory to JSON
     * @param player the player whose inventory to serialize
     * @return JSON string representation of the inventory
     */
    public static String serializePlayerInventory(Player player) {
        try {
            PlayerInventory inventory = player.getInventory();
            JsonObject inventoryJson = new JsonObject();
            
            // Serialize main inventory (0-35)
            JsonArray mainInventory = new JsonArray();
            for (int i = 0; i < 36; i++) {
                ItemStack item = inventory.getItem(i);
                mainInventory.add(serializeItemStack(item));
            }
            inventoryJson.add("main", mainInventory);
            
            // Serialize armor (36-39)
            JsonArray armorInventory = new JsonArray();
            ItemStack[] armor = inventory.getArmorContents();
            for (ItemStack item : armor) {
                armorInventory.add(serializeItemStack(item));
            }
            inventoryJson.add("armor", armorInventory);
            
            // Serialize off-hand (40)
            inventoryJson.add("offhand", serializeItemStack(inventory.getItemInOffHand()));
            
            // Store additional metadata
            JsonObject metadata = new JsonObject();
            metadata.addProperty("heldItemSlot", inventory.getHeldItemSlot());
            metadata.addProperty("timestamp", System.currentTimeMillis());
            metadata.addProperty("playerName", player.getName());
            metadata.addProperty("playerUuid", player.getUniqueId().toString());
            inventoryJson.add("metadata", metadata);
            
            return gson.toJson(inventoryJson);
            
        } catch (Exception e) {
            logger.severe("Failed to serialize inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Deserialize JSON and restore a player's inventory
     * @param player the player to restore the inventory for
     * @param inventoryJson the JSON string containing the inventory data
     * @return true if successful, false otherwise
     */
    public static boolean deserializePlayerInventory(Player player, String inventoryJson) {
        try {
            if (inventoryJson == null || inventoryJson.trim().isEmpty()) {
                logger.warning("Cannot deserialize null or empty inventory data for " + player.getName());
                return false;
            }
            
            JsonObject inventoryObj = JsonParser.parseString(inventoryJson).getAsJsonObject();
            PlayerInventory inventory = player.getInventory();
            
            // Clear current inventory
            inventory.clear();
            
            // Restore main inventory (0-35)
            if (inventoryObj.has("main")) {
                JsonArray mainInventory = inventoryObj.getAsJsonArray("main");
                for (int i = 0; i < mainInventory.size() && i < 36; i++) {
                    ItemStack item = deserializeItemStack(mainInventory.get(i));
                    if (item != null) {
                        inventory.setItem(i, item);
                    }
                }
            }
            
            // Restore armor (36-39)
            if (inventoryObj.has("armor")) {
                JsonArray armorInventory = inventoryObj.getAsJsonArray("armor");
                ItemStack[] armor = new ItemStack[4];
                for (int i = 0; i < armorInventory.size() && i < 4; i++) {
                    armor[i] = deserializeItemStack(armorInventory.get(i));
                }
                inventory.setArmorContents(armor);
            }
            
            // Restore off-hand
            if (inventoryObj.has("offhand")) {
                ItemStack offhand = deserializeItemStack(inventoryObj.get("offhand"));
                if (offhand != null) {
                    inventory.setItemInOffHand(offhand);
                }
            }
            
            // Restore metadata
            if (inventoryObj.has("metadata")) {
                JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
                if (metadata.has("heldItemSlot")) {
                    inventory.setHeldItemSlot(metadata.get("heldItemSlot").getAsInt());
                }
            }
            
            // Update the player's inventory
            player.updateInventory();
            
            return true;
            
        } catch (Exception e) {
            logger.severe("Failed to deserialize inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Serialize an individual ItemStack to JSON
     * @param item the ItemStack to serialize
     * @return JsonElement representing the ItemStack
     */
    private static JsonElement serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return JsonNull.INSTANCE;
        }
        
        try {
            JsonObject itemJson = new JsonObject();
            
            // Basic item properties
            itemJson.addProperty("type", item.getType().name());
            itemJson.addProperty("amount", item.getAmount());
            itemJson.addProperty("durability", item.getDurability());
            
            // ItemMeta data
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                JsonObject metaJson = new JsonObject();
                
                // Display name
                if (meta.hasDisplayName()) {
                    metaJson.addProperty("displayName", meta.getDisplayName());
                }
                
                // Lore
                if (meta.hasLore()) {
                    JsonArray loreArray = new JsonArray();
                    for (String loreLine : meta.getLore()) {
                        loreArray.add(loreLine);
                    }
                    metaJson.add("lore", loreArray);
                }
                
                // Enchantments
                if (meta.hasEnchants()) {
                    JsonObject enchantmentsJson = new JsonObject();
                    for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                        enchantmentsJson.addProperty(entry.getKey().getKey().toString(), entry.getValue());
                    }
                    metaJson.add("enchantments", enchantmentsJson);
                }
                
                // Custom model data
                if (meta.hasCustomModelData()) {
                    metaJson.addProperty("customModelData", meta.getCustomModelData());
                }
                
                // Unbreakable
                if (meta.isUnbreakable()) {
                    metaJson.addProperty("unbreakable", true);
                }
                
                // Attribute modifiers
                if (meta.hasAttributeModifiers()) {
                    // We'll skip complex attribute modifiers for now
                    // This can be expanded if needed
                    metaJson.addProperty("hasAttributeModifiers", true);
                }
                
                // Persistent data container (basic support)
                if (!meta.getPersistentDataContainer().isEmpty()) {
                    // We'll skip complex persistent data for now
                    // This can be expanded if needed
                    metaJson.addProperty("hasPersistentData", true);
                }
                
                itemJson.add("meta", metaJson);
            }
            
            return itemJson;
            
        } catch (Exception e) {
            logger.warning("Failed to serialize ItemStack: " + e.getMessage());
            return JsonNull.INSTANCE;
        }
    }
    
    /**
     * Deserialize a JsonElement to an ItemStack
     * @param element the JsonElement to deserialize
     * @return the deserialized ItemStack, or null if invalid
     */
    private static ItemStack deserializeItemStack(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        
        try {
            JsonObject itemJson = element.getAsJsonObject();
            
            // Basic item properties
            Material type = Material.valueOf(itemJson.get("type").getAsString());
            int amount = itemJson.get("amount").getAsInt();
            short durability = itemJson.has("durability") ? itemJson.get("durability").getAsShort() : 0;
            
            ItemStack item = new ItemStack(type, amount);
            item.setDurability(durability);
            
            // ItemMeta data
            if (itemJson.has("meta")) {
                JsonObject metaJson = itemJson.getAsJsonObject("meta");
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    // Display name
                    if (metaJson.has("displayName")) {
                        meta.setDisplayName(metaJson.get("displayName").getAsString());
                    }
                    
                    // Lore
                    if (metaJson.has("lore")) {
                        JsonArray loreArray = metaJson.getAsJsonArray("lore");
                        List<String> lore = new ArrayList<>();
                        for (JsonElement loreElement : loreArray) {
                            lore.add(loreElement.getAsString());
                        }
                        meta.setLore(lore);
                    }
                    
                    // Enchantments
                    if (metaJson.has("enchantments")) {
                        JsonObject enchantmentsJson = metaJson.getAsJsonObject("enchantments");
                        for (Map.Entry<String, JsonElement> entry : enchantmentsJson.entrySet()) {
                            try {
                                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(entry.getKey()));
                                if (enchantment != null) {
                                    meta.addEnchant(enchantment, entry.getValue().getAsInt(), true);
                                }
                            } catch (Exception e) {
                                logger.warning("Failed to deserialize enchantment: " + entry.getKey());
                            }
                        }
                    }
                    
                    // Custom model data
                    if (metaJson.has("customModelData")) {
                        meta.setCustomModelData(metaJson.get("customModelData").getAsInt());
                    }
                    
                    // Unbreakable
                    if (metaJson.has("unbreakable") && metaJson.get("unbreakable").getAsBoolean()) {
                        meta.setUnbreakable(true);
                    }
                    
                    item.setItemMeta(meta);
                }
            }
            
            return item;
            
        } catch (Exception e) {
            logger.warning("Failed to deserialize ItemStack: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a snapshot of essential inventory info for comparison
     * @param player the player to snapshot
     * @return a compact JSON string with essential inventory info
     */
    public static String createInventorySnapshot(Player player) {
        try {
            JsonObject snapshot = new JsonObject();
            PlayerInventory inventory = player.getInventory();
            
            // Count items by type
            Map<Material, Integer> itemCounts = new HashMap<>();
            
            // Count main inventory items
            for (int i = 0; i < 36; i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    itemCounts.merge(item.getType(), item.getAmount(), Integer::sum);
                }
            }
            
            // Count armor items
            for (ItemStack armor : inventory.getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR) {
                    itemCounts.merge(armor.getType(), armor.getAmount(), Integer::sum);
                }
            }
            
            // Count off-hand item
            ItemStack offhand = inventory.getItemInOffHand();
            if (offhand != null && offhand.getType() != Material.AIR) {
                itemCounts.merge(offhand.getType(), offhand.getAmount(), Integer::sum);
            }
            
            // Convert to JSON
            JsonObject itemCountsJson = new JsonObject();
            for (Map.Entry<Material, Integer> entry : itemCounts.entrySet()) {
                itemCountsJson.addProperty(entry.getKey().name(), entry.getValue());
            }
            snapshot.add("itemCounts", itemCountsJson);
            
            // Add metadata
            snapshot.addProperty("timestamp", System.currentTimeMillis());
            snapshot.addProperty("totalItems", itemCounts.values().stream().mapToInt(Integer::intValue).sum());
            
            return gson.toJson(snapshot);
            
        } catch (Exception e) {
            logger.warning("Failed to create inventory snapshot for " + player.getName() + ": " + e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Check if player's inventory contains any guard kit items
     * @param player the player to check
     * @param guardKitItems list of materials that are considered guard kit items
     * @return true if player has any guard kit items, false otherwise
     */
    public static boolean hasGuardKitItems(Player player, List<Material> guardKitItems) {
        PlayerInventory inventory = player.getInventory();
        
        // Check main inventory
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && guardKitItems.contains(item.getType())) {
                return true;
            }
        }
        
        // Check armor
        for (ItemStack armor : inventory.getArmorContents()) {
            if (armor != null && guardKitItems.contains(armor.getType())) {
                return true;
            }
        }
        
        // Check off-hand
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && guardKitItems.contains(offhand.getType())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove all guard kit items from player's inventory
     * @param player the player to remove items from
     * @param guardKitItems list of materials that are considered guard kit items
     * @return number of items removed
     */
    public static int removeGuardKitItems(Player player, List<Material> guardKitItems) {
        PlayerInventory inventory = player.getInventory();
        int removedCount = 0;
        
        // Remove from main inventory
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && guardKitItems.contains(item.getType())) {
                removedCount += item.getAmount();
                inventory.setItem(i, null);
            }
        }
        
        // Remove from armor
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && guardKitItems.contains(armor[i].getType())) {
                removedCount += armor[i].getAmount();
                armor[i] = null;
            }
        }
        inventory.setArmorContents(armor);
        
        // Remove from off-hand
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && guardKitItems.contains(offhand.getType())) {
            removedCount += offhand.getAmount();
            inventory.setItemInOffHand(null);
        }
        
        player.updateInventory();
        return removedCount;
    }
    
    /**
     * Get a list of common guard kit items
     * @return list of materials commonly found in guard kits
     */
    public static List<Material> getCommonGuardKitItems() {
        List<Material> guardItems = new ArrayList<>();
        
        // Weapons
        guardItems.add(Material.WOODEN_SWORD);
        guardItems.add(Material.STONE_SWORD);
        guardItems.add(Material.IRON_SWORD);
        guardItems.add(Material.DIAMOND_SWORD);
        guardItems.add(Material.NETHERITE_SWORD);
        guardItems.add(Material.BOW);
        guardItems.add(Material.CROSSBOW);
        guardItems.add(Material.TRIDENT);
        
        // Armor
        guardItems.add(Material.LEATHER_HELMET);
        guardItems.add(Material.LEATHER_CHESTPLATE);
        guardItems.add(Material.LEATHER_LEGGINGS);
        guardItems.add(Material.LEATHER_BOOTS);
        guardItems.add(Material.CHAINMAIL_HELMET);
        guardItems.add(Material.CHAINMAIL_CHESTPLATE);
        guardItems.add(Material.CHAINMAIL_LEGGINGS);
        guardItems.add(Material.CHAINMAIL_BOOTS);
        guardItems.add(Material.IRON_HELMET);
        guardItems.add(Material.IRON_CHESTPLATE);
        guardItems.add(Material.IRON_LEGGINGS);
        guardItems.add(Material.IRON_BOOTS);
        guardItems.add(Material.DIAMOND_HELMET);
        guardItems.add(Material.DIAMOND_CHESTPLATE);
        guardItems.add(Material.DIAMOND_LEGGINGS);
        guardItems.add(Material.DIAMOND_BOOTS);
        guardItems.add(Material.NETHERITE_HELMET);
        guardItems.add(Material.NETHERITE_CHESTPLATE);
        guardItems.add(Material.NETHERITE_LEGGINGS);
        guardItems.add(Material.NETHERITE_BOOTS);
        
        // Tools
        guardItems.add(Material.WOODEN_PICKAXE);
        guardItems.add(Material.STONE_PICKAXE);
        guardItems.add(Material.IRON_PICKAXE);
        guardItems.add(Material.DIAMOND_PICKAXE);
        guardItems.add(Material.NETHERITE_PICKAXE);
        guardItems.add(Material.WOODEN_AXE);
        guardItems.add(Material.STONE_AXE);
        guardItems.add(Material.IRON_AXE);
        guardItems.add(Material.DIAMOND_AXE);
        guardItems.add(Material.NETHERITE_AXE);
        
        // Food and consumables
        guardItems.add(Material.BREAD);
        guardItems.add(Material.COOKED_BEEF);
        guardItems.add(Material.COOKED_PORKCHOP);
        guardItems.add(Material.COOKED_CHICKEN);
        guardItems.add(Material.GOLDEN_APPLE);
        guardItems.add(Material.ENCHANTED_GOLDEN_APPLE);
        
        // Utility items
        guardItems.add(Material.ARROW);
        guardItems.add(Material.SPECTRAL_ARROW);
        guardItems.add(Material.TIPPED_ARROW);
        guardItems.add(Material.SHIELD);
        guardItems.add(Material.TORCH);
        guardItems.add(Material.ENDER_PEARL);
        
        return guardItems;
    }
    
    /**
     * Validate serialized inventory data
     * @param inventoryJson the JSON string to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateInventoryData(String inventoryJson) {
        if (inventoryJson == null || inventoryJson.trim().isEmpty()) {
            return false;
        }
        
        try {
            JsonObject inventoryObj = JsonParser.parseString(inventoryJson).getAsJsonObject();
            
            // Check for required fields
            if (!inventoryObj.has("main") || !inventoryObj.has("armor") || !inventoryObj.has("metadata")) {
                return false;
            }
            
            // Validate main inventory structure
            JsonArray mainInventory = inventoryObj.getAsJsonArray("main");
            if (mainInventory.size() != 36) {
                return false;
            }
            
            // Validate armor inventory structure
            JsonArray armorInventory = inventoryObj.getAsJsonArray("armor");
            if (armorInventory.size() != 4) {
                return false;
            }
            
            // Validate metadata
            JsonObject metadata = inventoryObj.getAsJsonObject("metadata");
            if (!metadata.has("timestamp") || !metadata.has("playerName") || !metadata.has("playerUuid")) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.warning("Invalid inventory data format: " + e.getMessage());
            return false;
        }
    }
} 