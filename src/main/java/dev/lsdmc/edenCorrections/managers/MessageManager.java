package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

// PlaceholderAPI integration (optional dependency)
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public class MessageManager {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    private final MiniMessage miniMessage;
    
    // Message cache for performance
    private final Map<String, String> messageCache;
    
    // Boss bar storage for active boss bars
    private final Map<UUID, BossBar> activeBossBars;
    
    // Boss bar task management for automatic cleanup
    private final Map<UUID, BukkitTask> bossBarTasks;
    
    // Action bar task management
    private final Map<UUID, BukkitTask> actionBarTasks;
    
    // PlaceholderAPI integration
    private boolean placeholderAPIEnabled;
    
    // Placeholder patterns for validation
    private static final Pattern INTERNAL_PLACEHOLDER = Pattern.compile("<([^>]+)>");
    private static final Pattern EXTERNAL_PLACEHOLDER = Pattern.compile("%([^%]+)%");
    private static final Pattern LEGACY_PLACEHOLDER = Pattern.compile("\\{([^}]+)\\}");
    
    // Default color scheme (vibrant Purple & Cyan)
    private static final String PRIMARY_COLOR = "#9D4EDD";      // Vibrant purple
    private static final String SECONDARY_COLOR = "#06FFA5";    // Bright cyan
    private static final String ACCENT_COLOR = "#FFB3C6";       // Soft pink
    private static final String ERROR_COLOR = "#FFA94D";        // Warm orange (less harsh than red)
    private static final String SUCCESS_COLOR = "#51CF66";      // Fresh green
    private static final String NEUTRAL_COLOR = "#ADB5BD";      // Light gray
    private static final String WARNING_COLOR = "#FFE066";      // Soft yellow
    private static final String INFO_COLOR = "#74C0FC";         // Soft blue
    
    // Message validation tracking
    private final Map<String, Integer> messageUsageCount;
    private final List<String> missingMessages;
    private final List<String> invalidMessages;
    
    public MessageManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.miniMessage = MiniMessage.miniMessage();
        this.messageCache = new ConcurrentHashMap<>();
        this.activeBossBars = new ConcurrentHashMap<>();
        this.bossBarTasks = new ConcurrentHashMap<>();
        this.actionBarTasks = new ConcurrentHashMap<>();
        this.placeholderAPIEnabled = false;
        this.messageUsageCount = new ConcurrentHashMap<>();
        this.missingMessages = new ArrayList<>();
        this.invalidMessages = new ArrayList<>();
    }
    
    public void initialize() {
        logger.info("MessageManager initializing...");
        
        // Check if PlaceholderAPI is available
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (plugin.getConfigManager().isPlaceholderAPIEnabled()) {
                placeholderAPIEnabled = true;
                logger.info("PlaceholderAPI integration enabled!");
            } else {
                logger.info("PlaceholderAPI found but disabled in config");
            }
        } else {
            logger.info("PlaceholderAPI not found - external placeholder support disabled");
        }
        
        // Load and validate messages
        loadMessages();
        validateMessages();
        
        logger.info("MessageManager initialized successfully with MiniMessage support!");
    }
    
    public void reload() {
        logger.info("Reloading MessageManager...");
        
        // Clear caches
        messageCache.clear();
        messageUsageCount.clear();
        missingMessages.clear();
        invalidMessages.clear();
        
        // Reload messages
        loadMessages();
        validateMessages();
        
        logger.info("MessageManager reloaded successfully!");
    }
    
    private void loadMessages() {
        try {
            // Load prefix from top-level config first
            String prefix = plugin.getConfig().getString("prefix");
            if (prefix != null) {
                messageCache.put("prefix", prefix);
            }
            
            // Load messages from messages section
        ConfigurationSection messages = plugin.getConfig().getConfigurationSection("messages");
        if (messages != null) {
            loadMessageSection(messages, "");
                logger.info("Loaded " + messageCache.size() + " messages from configuration");
            } else {
                logger.warning("No messages section found in configuration!");
            }
        } catch (Exception e) {
            logger.severe("Error loading messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMessageSection(ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (section.isConfigurationSection(key)) {
                loadMessageSection(section.getConfigurationSection(key), fullKey);
            } else if (section.isString(key)) {
                String message = section.getString(key);
                if (message != null) {
                    messageCache.put(fullKey, message);
                    
                    // Check for legacy placeholder format and warn
                    if (LEGACY_PLACEHOLDER.matcher(message).find()) {
                        logger.warning("Message '" + fullKey + "' uses legacy {placeholder} format. Consider updating to <placeholder> format.");
                    }
                }
            }
        }
    }
    
    // === CORE MESSAGE SENDING METHODS ===
    
    public void sendMessage(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send message to null player: " + messageKey);
            return;
        }
        
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            player.sendMessage(getPrefix().append(message));
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendMessage(CommandSender sender, String messageKey, TagResolver... placeholders) {
        if (sender == null) {
            logger.warning("Attempted to send message to null sender: " + messageKey);
            return;
        }
        
        Player player = sender instanceof Player ? (Player) sender : null;
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            if (sender instanceof ConsoleCommandSender) {
                // For console, convert to plain text
                String plainMessage = PlainTextComponentSerializer.plainText().serialize(getPrefix().append(message));
                sender.sendMessage(plainMessage);
            } else {
                sender.sendMessage(getPrefix().append(message));
            }
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendMessage(Audience audience, String messageKey, TagResolver... placeholders) {
        if (audience == null) {
            logger.warning("Attempted to send message to null audience: " + messageKey);
            return;
        }
        
        Component message = getMessage(null, messageKey, placeholders);
        if (message != null) {
            audience.sendMessage(getPrefix().append(message));
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendRawMessage(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send raw message to null player: " + messageKey);
            return;
        }
        
        Component message = getMessage(player, messageKey, placeholders);
        if (message != null) {
            player.sendMessage(message);
            trackMessageUsage(messageKey);
        }
    }
    
    public void sendGuardAlert(String messageKey, TagResolver... placeholders) {
        try {
            Component alertMessage = getMessage(null, "system.guard-alert", 
                Placeholder.component("message", getMessage(null, messageKey, placeholders)));
        
        if (alertMessage != null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().hasGuardPermission(player) && 
                    plugin.getDutyManager().isOnDuty(player)) {
                    player.sendMessage(alertMessage);
                }
            }
                trackMessageUsage(messageKey);
            }
        } catch (Exception e) {
            logger.severe("Error sending guard alert: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // === ENHANCED BOSS BAR METHODS ===
    
    public void showBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to show boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component title = getMessage(player, messageKey, placeholders);
            if (title == null) {
                logger.warning("Failed to get boss bar title for message: " + messageKey);
                return;
            }
        
        // Remove existing boss bar if present
        hideBossBar(player);
        
        BossBar bossBar = BossBar.bossBar(title, 1.0f, color, overlay);
        activeBossBars.put(player.getUniqueId(), bossBar);
        player.showBossBar(bossBar);
            
            trackMessageUsage(messageKey);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Showed boss bar to " + player.getName() + " - " + messageKey);
            }
        } catch (Exception e) {
            logger.severe("Error showing boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showTimedBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, int durationSeconds, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to show timed boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            showBossBar(player, messageKey, color, overlay, placeholders);
            
            // Cancel existing task if present
            BukkitTask existingTask = bossBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            // Schedule automatic removal
            BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                hideBossBar(player);
            }, durationSeconds * 20L);
            
            bossBarTasks.put(player.getUniqueId(), task);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Showed timed boss bar to " + player.getName() + " for " + durationSeconds + "s");
            }
        } catch (Exception e) {
            logger.severe("Error showing timed boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showCountdownBossBar(Player player, String messageKey, BossBar.Color color, BossBar.Overlay overlay, int durationSeconds, TagResolver... staticPlaceholders) {
        if (player == null) {
            logger.warning("Attempted to show countdown boss bar to null player: " + messageKey);
            return;
        }
        
        try {
            // Create initial boss bar with countdown progress
            Component initialTitle = getMessage(player, messageKey, combineTagResolvers(staticPlaceholders, timePlaceholder("time", durationSeconds)));
            if (initialTitle == null) {
                logger.warning("Failed to get countdown boss bar title for message: " + messageKey);
                return;
            }
            
            // Remove existing boss bar if present
            hideBossBar(player);
            
            // Create boss bar with full progress initially
            BossBar bossBar = BossBar.bossBar(initialTitle, 1.0f, color, overlay);
            activeBossBars.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
            
            // Cancel existing task if present
            BukkitTask existingTask = bossBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            // Create countdown task
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = durationSeconds;
                
                @Override
                public void run() {
                    // Check if player is still online
                    if (!player.isOnline()) {
                        hideBossBar(player);
                        return;
                    }
                    
                    if (remaining <= 0) {
                        // Countdown complete - hide boss bar and cancel task
                        hideBossBar(player);
                        return;
                    }
                    
                    try {
                        // Create combined placeholders with time
                        TagResolver[] combinedPlaceholders = combineTagResolvers(staticPlaceholders, timePlaceholder("time", remaining));
                        
                        // Update boss bar title
                        Component title = getMessage(player, messageKey, combinedPlaceholders);
                        if (title != null) {
                            bossBar.name(title);
                        }
                        
                        // Update progress (remaining time as percentage)
                        float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / durationSeconds));
                        bossBar.progress(progress);
                        
                        remaining--;
                        
                    } catch (Exception e) {
                        logger.warning("Error updating countdown boss bar for " + player.getName() + ": " + e.getMessage());
                        hideBossBar(player);
                    }
                }
            }, 0L, 20L);
            
            bossBarTasks.put(player.getUniqueId(), task);
            trackMessageUsage(messageKey);
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Started countdown boss bar for " + player.getName() + " (" + durationSeconds + "s)");
            }
        } catch (Exception e) {
            logger.severe("Error showing countdown boss bar to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void updateBossBar(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to update boss bar for null player: " + messageKey);
            return;
        }
        
        try {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar != null) {
                Component title = getMessage(player, messageKey, placeholders);
            if (title != null) {
                bossBar.name(title);
                    trackMessageUsage(messageKey);
                }
            }
        } catch (Exception e) {
            logger.warning("Error updating boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void updateBossBarProgress(Player player, float progress) {
        if (player == null) {
            logger.warning("Attempted to update boss bar progress for null player");
            return;
        }
        
        try {
        BossBar bossBar = activeBossBars.get(player.getUniqueId());
        if (bossBar != null) {
                float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
                bossBar.progress(clampedProgress);
            }
        } catch (Exception e) {
            logger.warning("Error updating boss bar progress for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void hideBossBar(Player player) {
        if (player == null) {
            logger.warning("Attempted to hide boss bar for null player");
            return;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            
            // Remove and hide boss bar
            BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
            
            // Cancel and remove associated task
            BukkitTask task = bossBarTasks.remove(playerId);
            if (task != null) {
                task.cancel();
            }
            
            if (plugin.getConfigManager().isDebugMode() && bossBar != null) {
                logger.info("DEBUG: Hid boss bar for " + player.getName());
            }
        } catch (Exception e) {
            logger.warning("Error hiding boss bar for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    // Helper method to combine tag resolvers
    private TagResolver[] combineTagResolvers(TagResolver[] staticPlaceholders, TagResolver... additionalPlaceholders) {
        TagResolver[] combined = new TagResolver[staticPlaceholders.length + additionalPlaceholders.length];
        System.arraycopy(staticPlaceholders, 0, combined, 0, staticPlaceholders.length);
        System.arraycopy(additionalPlaceholders, 0, combined, staticPlaceholders.length, additionalPlaceholders.length);
        return combined;
    }

    // === ACTION BAR METHODS ===
    
    public void sendActionBar(Player player, String messageKey, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send action bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component message = getMessage(player, messageKey, placeholders);
            if (message != null) {
                player.sendActionBar(message);
                trackMessageUsage(messageKey);
            }
        } catch (Exception e) {
            logger.warning("Error sending action bar to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void sendTimedActionBar(Player player, String messageKey, int durationSeconds, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send timed action bar to null player: " + messageKey);
            return;
        }
        
        try {
            Component message = getMessage(player, messageKey, placeholders);
            if (message == null) return;
            
            // Cancel existing action bar task
            BukkitTask existingTask = actionBarTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }
            
            // Send initial action bar
            player.sendActionBar(message);
            trackMessageUsage(messageKey);
            
            // Schedule repeated sending (action bars fade after ~3 seconds)
            BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                private int remaining = durationSeconds;
                
                @Override
                public void run() {
                    if (remaining <= 0 || !player.isOnline()) {
                        actionBarTasks.remove(player.getUniqueId());
                        // Send empty action bar to clear
                        if (player.isOnline()) {
                            player.sendActionBar(Component.empty());
                        }
                        return;
                    }
                    
                    player.sendActionBar(message);
                    remaining--;
                }
            }, 0L, 20L);
            
            actionBarTasks.put(player.getUniqueId(), task);
        } catch (Exception e) {
            logger.warning("Error sending timed action bar to " + player.getName() + ": " + e.getMessage());
        }
    }
    
    public void sendActionBarToGuards(String messageKey, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getDutyManager().hasGuardPermission(player) && 
                    plugin.getDutyManager().isOnDuty(player)) {
                    player.sendActionBar(message);
                }
            }
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.warning("Error sending action bar to guards: " + e.getMessage());
        }
    }
    
    public void sendActionBarToAll(String messageKey, TagResolver... placeholders) {
        try {
            Component message = getMessage(null, messageKey, placeholders);
            if (message == null) return;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendActionBar(message);
            }
            trackMessageUsage(messageKey);
        } catch (Exception e) {
            logger.warning("Error sending action bar to all players: " + e.getMessage());
        }
    }
    
    public void clearActionBar(Player player) {
        if (player == null) {
            logger.warning("Attempted to clear action bar for null player");
            return;
        }
        
        try {
            BukkitTask task = actionBarTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
            }
            player.sendActionBar(Component.empty());
        } catch (Exception e) {
            logger.warning("Error clearing action bar for " + player.getName() + ": " + e.getMessage());
        }
    }

    // === TITLE METHODS ===
    
    public void sendTitle(Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut, TagResolver... placeholders) {
        if (player == null) {
            logger.warning("Attempted to send title to null player");
            return;
        }
        
        try {
            Component title = titleKey != null ? getMessage(player, titleKey, placeholders) : Component.empty();
            Component subtitle = subtitleKey != null ? getMessage(player, subtitleKey, placeholders) : Component.empty();
            
            Title titleObj = Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
                )
            );
            
            player.showTitle(titleObj);
            
            if (titleKey != null) trackMessageUsage(titleKey);
            if (subtitleKey != null) trackMessageUsage(subtitleKey);
        } catch (Exception e) {
            logger.warning("Error sending title to " + player.getName() + ": " + e.getMessage());
        }
    }

    // === MESSAGE RETRIEVAL AND PARSING ===
    
    public Component getMessage(Player player, String messageKey, TagResolver... placeholders) {
        String rawMessage = getRawMessage(messageKey);
        if (rawMessage == null) {
            logger.warning("Message not found: " + messageKey);
            missingMessages.add(messageKey);
            return miniMessage.deserialize("<color:" + ERROR_COLOR + ">Message not found: " + messageKey + "</color>");
        }
        
        // Parse PlaceholderAPI placeholders if enabled and player is provided
        if (placeholderAPIEnabled && player != null) {
            rawMessage = parsePlaceholderAPI(player, rawMessage);
        }
        
        // Debug logging for placeholder issues
        if (plugin.getConfigManager().isDebugMode() && placeholders.length > 0) {
            logger.info("DEBUG: Processing message '" + messageKey + "' with " + placeholders.length + " placeholders");
            logger.info("DEBUG: Raw message: " + rawMessage);
        }
        
        try {
            // Use the proper MiniMessage TagResolver system
            Component result = miniMessage.deserialize(rawMessage, TagResolver.resolver(placeholders));
            
            if (plugin.getConfigManager().isDebugMode() && placeholders.length > 0) {
                logger.info("DEBUG: Successfully processed placeholders for message: " + messageKey);
            }
            
            return result;
        } catch (Exception e) {
            logger.warning("Error parsing message '" + messageKey + "': " + e.getMessage());
            logger.warning("Raw message was: " + rawMessage);
            invalidMessages.add(messageKey + ": " + e.getMessage());
            return miniMessage.deserialize("<color:" + ERROR_COLOR + ">Error parsing message: " + messageKey + "</color>");
        }
    }
    
    private String parsePlaceholderAPI(Player player, String message) {
        try {
            // Use PlaceholderAPI to parse external placeholders
            return PlaceholderAPI.setPlaceholders(player, message);
        } catch (Exception e) {
            logger.warning("Error parsing PlaceholderAPI placeholders: " + e.getMessage());
            return message;
        }
    }
    
    public String getRawMessage(String messageKey) {
        return messageCache.get(messageKey);
    }
    
    public Component getPrefix() {
        String prefixMessage = getRawMessage("prefix");
        if (prefixMessage != null) {
            try {
            return miniMessage.deserialize(prefixMessage);
            } catch (Exception e) {
                logger.warning("Error parsing prefix: " + e.getMessage());
            }
        }
        return miniMessage.deserialize("<gradient:" + PRIMARY_COLOR + ":" + SECONDARY_COLOR + ">[₠]</gradient> ");
    }
    
    // === EXTERNAL PLACEHOLDER METHODS ===
    
    public String getExternalMessage(String messageKey, Map<String, String> placeholders) {
        String rawMessage = getRawMessage(messageKey);
        if (rawMessage == null) return "Message not found: " + messageKey;
        
        // Replace external placeholders (%placeholder%)
        String processed = rawMessage;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                processed = processed.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        // Strip MiniMessage tags for external use
        return stripMiniMessageTags(processed);
    }
    
    private String stripMiniMessageTags(String message) {
        // Basic MiniMessage tag removal for external plugins
        return message.replaceAll("<[^>]*>", "");
    }
    
    public String getPlainTextMessage(String messageKey, TagResolver... placeholders) {
        Component component = getMessage(null, messageKey, placeholders);
        if (component != null) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
        return "Message not found: " + messageKey;
    }
    
    public String getLegacyMessage(String messageKey, TagResolver... placeholders) {
        Component component = getMessage(null, messageKey, placeholders);
        if (component != null) {
            return LegacyComponentSerializer.legacySection().serialize(component);
        }
        return "Message not found: " + messageKey;
    }

    // === CONVENIENCE METHODS ===
    
    public void sendSuccess(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendError(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendWarning(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }
    
    public void sendInfo(Player player, String messageKey, TagResolver... placeholders) {
        sendMessage(player, messageKey, placeholders);
    }

    // === UTILITY METHODS FOR PLACEHOLDERS ===
    
    public static TagResolver playerPlaceholder(String key, Player player) {
        return Placeholder.unparsed(key, player.getName());
    }
    
    public static TagResolver stringPlaceholder(String key, String value) {
        return Placeholder.unparsed(key, value != null ? value : "");
    }
    
    public static TagResolver numberPlaceholder(String key, Number value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
    
    public static TagResolver timePlaceholder(String key, long timeInSeconds) {
        long minutes = timeInSeconds / 60;
        long seconds = timeInSeconds % 60;
        String timeString = minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
        return Placeholder.unparsed(key, timeString);
    }
    
    public static TagResolver distancePlaceholder(String key, double distance) {
        return Placeholder.unparsed(key, String.valueOf((int) distance));
    }
    
    public static TagResolver starsPlaceholder(String key, int level) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < level; i++) {
            stars.append("⭐");
        }
        return Placeholder.unparsed(key, stars.toString());
    }
    
    public static TagResolver booleanPlaceholder(String key, boolean value) {
        return Placeholder.unparsed(key, value ? "true" : "false");
    }
    
    public static TagResolver percentagePlaceholder(String key, double percentage) {
        return Placeholder.unparsed(key, String.format("%.1f%%", percentage * 100));
    }
    
    public static TagResolver componentPlaceholder(String key, Component component) {
        return Placeholder.component(key, component);
    }

    // === VALIDATION METHODS ===
    
    public boolean hasMessage(String messageKey) {
        return messageCache.containsKey(messageKey);
    }
    
    public void validateMessages() {
        logger.info("Validating message configuration...");
        
        String[] requiredMessages = {
            "universal.no-permission",
            "universal.player-not-found",
            "universal.player-only",
            "duty.activation.success",
            "duty.deactivation.success",
            "chase.start.success",
            "chase.end.success",
            "system.startup",
            "system.shutdown",
            "prefix"
        };
        
        int missingCount = 0;
        for (String messageKey : requiredMessages) {
            if (!hasMessage(messageKey)) {
                logger.warning("Missing required message: " + messageKey);
                missingCount++;
            }
        }
        
        if (missingCount == 0) {
            logger.info("All required messages are present!");
        } else {
            logger.warning("Found " + missingCount + " missing required messages");
        }
        
        // Validate message formats
        validateMessageFormats();
    }
    
    private void validateMessageFormats() {
        int invalidCount = 0;
        for (Map.Entry<String, String> entry : messageCache.entrySet()) {
            String key = entry.getKey();
            String message = entry.getValue();
            
            try {
                // Test parsing the message
                miniMessage.deserialize(message);
            } catch (Exception e) {
                logger.warning("Invalid message format '" + key + "': " + e.getMessage());
                invalidCount++;
            }
        }
        
        if (invalidCount > 0) {
            logger.warning("Found " + invalidCount + " messages with invalid format");
        }
    }
    
    private void trackMessageUsage(String messageKey) {
        if (plugin.getConfigManager().isDebugMode()) {
            messageUsageCount.put(messageKey, messageUsageCount.getOrDefault(messageKey, 0) + 1);
        }
    }
    
    public Map<String, Integer> getMessageUsageStats() {
        return new HashMap<>(messageUsageCount);
    }
    
    public List<String> getMissingMessages() {
        return new ArrayList<>(missingMessages);
    }
    
    public List<String> getInvalidMessages() {
        return new ArrayList<>(invalidMessages);
    }
    
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    // === CLEANUP METHODS ===
    
    public void cleanup() {
        logger.info("Cleaning up MessageManager resources...");
        
        try {
        // Hide all active boss bars
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                player.hideBossBar(entry.getValue());
                    } catch (Exception e) {
                        logger.warning("Error hiding boss bar for " + player.getName() + " during cleanup: " + e.getMessage());
                    }
            }
        }
        activeBossBars.clear();
            
            // Cancel all boss bar tasks
            for (Map.Entry<UUID, BukkitTask> entry : bossBarTasks.entrySet()) {
                BukkitTask task = entry.getValue();
                if (task != null) {
                    try {
                        task.cancel();
                    } catch (Exception e) {
                        logger.warning("Error cancelling boss bar task during cleanup: " + e.getMessage());
                    }
                }
            }
            bossBarTasks.clear();
            
            // Cancel all action bar tasks and clear action bars
            for (Map.Entry<UUID, BukkitTask> entry : actionBarTasks.entrySet()) {
                BukkitTask task = entry.getValue();
                if (task != null) {
                    try {
                        task.cancel();
                    } catch (Exception e) {
                        logger.warning("Error cancelling action bar task during cleanup: " + e.getMessage());
                    }
                }
                
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                        player.sendActionBar(Component.empty());
                    } catch (Exception e) {
                        logger.warning("Error clearing action bar for " + player.getName() + " during cleanup: " + e.getMessage());
                    }
                }
            }
            actionBarTasks.clear();
            
            // Clear message cache
        messageCache.clear();
            messageUsageCount.clear();
            missingMessages.clear();
            invalidMessages.clear();
            
            logger.info("MessageManager cleanup completed successfully");
        } catch (Exception e) {
            logger.severe("Error during MessageManager cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // === CLEANUP FOR INDIVIDUAL PLAYERS ===
    
    public void cleanupPlayer(Player player) {
        if (player == null) return;
        
        try {
            UUID playerId = player.getUniqueId();
            
            // Hide boss bar
            BossBar bossBar = activeBossBars.remove(playerId);
            if (bossBar != null) {
                try {
                    player.hideBossBar(bossBar);
                } catch (Exception e) {
                    logger.warning("Error hiding boss bar for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            // Cancel boss bar task
            BukkitTask bossTask = bossBarTasks.remove(playerId);
            if (bossTask != null) {
                try {
                    bossTask.cancel();
                } catch (Exception e) {
                    logger.warning("Error cancelling boss bar task for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            // Cancel action bar task and clear action bar
            BukkitTask actionTask = actionBarTasks.remove(playerId);
            if (actionTask != null) {
                try {
                    actionTask.cancel();
                } catch (Exception e) {
                    logger.warning("Error cancelling action bar task for " + player.getName() + " during player cleanup: " + e.getMessage());
                }
            }
            
            try {
                player.sendActionBar(Component.empty());
            } catch (Exception e) {
                logger.warning("Error clearing action bar for " + player.getName() + " during player cleanup: " + e.getMessage());
            }
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Cleaned up UI elements for " + player.getName());
            }
        } catch (Exception e) {
            logger.warning("Error during player cleanup for " + player.getName() + ": " + e.getMessage());
        }
    }

    // === DIAGNOSTIC METHODS ===
    
    public void generateDiagnosticReport() {
        logger.info("=== MessageManager Diagnostic Report ===");
        logger.info("Total messages loaded: " + messageCache.size());
        logger.info("PlaceholderAPI enabled: " + placeholderAPIEnabled);
        logger.info("Active boss bars: " + activeBossBars.size());
        logger.info("Active boss bar tasks: " + bossBarTasks.size());
        logger.info("Active action bar tasks: " + actionBarTasks.size());
        logger.info("Missing messages: " + missingMessages.size());
        logger.info("Invalid messages: " + invalidMessages.size());
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Most used messages:");
            messageUsageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> logger.info("  " + entry.getKey() + ": " + entry.getValue()));
        }
        
        logger.info("=== End Diagnostic Report ===");
    }
} 