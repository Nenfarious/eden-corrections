package dev.lsdmc.edenCorrections.commands;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Map;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public CommandHandler(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void registerCommands() {
        // Register main commands with tab completion
        plugin.getCommand("duty").setExecutor(this);
        plugin.getCommand("duty").setTabCompleter(this);
        
        plugin.getCommand("chase").setExecutor(this);
        plugin.getCommand("chase").setTabCompleter(this);
        
        plugin.getCommand("jail").setExecutor(this);
        plugin.getCommand("jail").setTabCompleter(this);
        
        plugin.getCommand("jailoffline").setExecutor(this);
        plugin.getCommand("jailoffline").setTabCompleter(this);
        
        plugin.getCommand("corrections").setExecutor(this);
        plugin.getCommand("corrections").setTabCompleter(this);
        
        plugin.getCommand("edenreload").setExecutor(this);
        plugin.getCommand("edenreload").setTabCompleter(this);
        
        // Register contraband commands
        plugin.getCommand("sword").setExecutor(this);
        plugin.getCommand("sword").setTabCompleter(this);
        
        plugin.getCommand("bow").setExecutor(this);
        plugin.getCommand("bow").setTabCompleter(this);
        
        plugin.getCommand("armor").setExecutor(this);
        plugin.getCommand("armor").setTabCompleter(this);
        
        plugin.getCommand("drugs").setExecutor(this);
        plugin.getCommand("drugs").setTabCompleter(this);
        
        plugin.getCommand("drugtest").setExecutor(this);
        plugin.getCommand("drugtest").setTabCompleter(this);
        
        // Register banking commands
        plugin.getCommand("dutybank").setExecutor(this);
        plugin.getCommand("dutybank").setTabCompleter(this);
        
        // Register tips command
        plugin.getCommand("tips").setExecutor(this);
        plugin.getCommand("tips").setTabCompleter(this);
        
        logger.info("Commands registered successfully with tab completion!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "duty":
                return handleDutyCommand(sender, args);
            case "chase":
                return handleChaseCommand(sender, args);
            case "jail":
                return handleJailCommand(sender, args);
            case "jailoffline":
                return handleJailOfflineCommand(sender, args);
            case "corrections":
                return handleCorrectionsCommand(sender, args);
            case "edenreload":
                return handleReloadCommand(sender, args);
            // Contraband commands
            case "sword":
                return handleContrabandCommand(sender, "sword", args);
            case "bow":
                return handleContrabandCommand(sender, "bow", args);
            case "armor":
                return handleContrabandCommand(sender, "armor", args);
            case "drugs":
                return handleContrabandCommand(sender, "drugs", args);
            case "drugtest":
                return handleDrugTestCommand(sender, args);
            case "dutybank":
                return handleDutyBankCommand(sender, args);
            case "tips":
                return handleTipsCommand(sender, args);
            default:
                return false;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "duty":
                return new ArrayList<>(); // No arguments
            case "chase":
                return handleChaseTabComplete(sender, args);
            case "jail":
                return handleJailTabComplete(sender, args);
            case "jailoffline":
                return handleJailOfflineTabComplete(sender, args);
            case "corrections":
                return handleCorrectionsTabComplete(sender, args);
            case "edenreload":
                return new ArrayList<>(); // No arguments
            case "sword":
            case "bow":
            case "armor":
            case "drugs":
            case "drugtest":
                return handleContrabandTabComplete(sender, args);
            case "dutybank":
                return handleDutyBankTabComplete(sender, args);
            case "tips":
                return handleTipsTabComplete(sender, args);
            default:
                return new ArrayList<>();
        }
    }
    
    private boolean handleDutyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has any guard permissions
        if (!plugin.getDutyManager().hasGuardPermission(player)) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        // No arguments needed - just toggle duty
        if (args.length > 0) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/duty"));
            return true;
        }
        
        // Enhanced duty toggle with proper validation
        boolean success = plugin.getDutyManager().toggleDuty(player);
        return true;
    }
    
    private boolean handleChaseCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.chase")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase <player|capture|end>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "capture":
                return handleChaseCapture(player, args);
            case "end":
                return handleChaseEnd(player, args);
            default:
                return handleChaseStart(player, args);
        }
    }
    
    private boolean handleChaseStart(Player player, String[] args) {
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase <player>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", args[0]));
            return true;
        }
        
        plugin.getChaseManager().startChase(player, target);
        return true;
    }
    
    private boolean handleChaseCapture(Player player, String[] args) {
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase capture"));
            return true;
        }
        
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(player, "chase.restrictions.not-on-duty");
            return true;
        }
        
        Player target = Bukkit.getPlayer(chase.getTargetId());
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", "target"));
            return true;
        }
        
        plugin.getChaseManager().captureTarget(player, target);
        return true;
    }
    
    private boolean handleChaseEnd(Player player, String[] args) {
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/chase end"));
            return true;
        }
        
        ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(player, "chase.restrictions.not-on-duty");
            return true;
        }
        
        plugin.getChaseManager().endChase(chase.getChaseId(), "Manually ended by guard");
        return true;
    }
    
    private boolean handleJailCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.jail")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/jail <player> [reason]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "universal.player-not-found",
                stringPlaceholder("player", args[0]));
            return true;
        }
        
        // Build reason from remaining arguments
        String reason = "No reason specified";
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        
        plugin.getJailManager().jailPlayer(player, target, reason);
        return true;
    }
    
    private boolean handleJailOfflineCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.guard.admin")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/jailoffline <player> [reason]"));
            return true;
        }
        
        String targetName = args[0];
        String guardName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        
        // Build reason from remaining arguments
        String reason = "No reason specified";
        if (args.length > 1) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }
        
        plugin.getJailManager().jailOfflinePlayer(guardName, targetName, reason);
        plugin.getMessageManager().sendMessage(sender, "jail.arrest.offline-success",
            stringPlaceholder("player", targetName));
        return true;
    }
    
    private boolean handleCorrectionsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edencorrections.admin")) {
            plugin.getMessageManager().sendMessage(sender, "universal.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            return handleCorrectionsHelp(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "wanted":
                return handleWantedCommand(sender, args);
            case "chase":
                return handleChaseAdminCommand(sender, args);
            case "duty":
                return handleDutyAdminCommand(sender, args);
            case "player":
                return handlePlayerAdminCommand(sender, args);
            case "system":
                return handleSystemCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender, args);
            case "help":
            default:
                return handleCorrectionsHelp(sender);
        }
    }
    
    private boolean handleCorrectionsHelp(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.header");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.wanted");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.chase");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.duty");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.player");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.system");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.reload");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.help");
            plugin.getMessageManager().sendRawMessage(player, "help.corrections.footer");
        } else {
            sender.sendMessage("=== EdenCorrections Admin Commands ===");
            sender.sendMessage("/corrections wanted <set|clear|check|list> - Manage wanted levels");
            sender.sendMessage("/corrections chase <list|end|endall> - Manage chase system");
            sender.sendMessage("/corrections duty <list> - Manage duty system");
            sender.sendMessage("/corrections system <stats|debug> - System information");
            sender.sendMessage("/corrections reload - Reload configuration");
            sender.sendMessage("/corrections help - Show this help");
        }
        return true;
    }
    
    private boolean handleWantedCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted <set|clear|check|list> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "set":
                return handleWantedSet(sender, args);
            case "clear":
                return handleWantedClear(sender, args);
            case "check":
                return handleWantedCheck(sender, args);
            case "list":
                return handleWantedList(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleWantedSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted set <player> <level> [reason]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        // Check if target is a guard on duty
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(sender, "wanted.restrictions.guard-on-duty");
            return true;
        }
        
        try {
            int level = Integer.parseInt(args[3]);
            String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "Admin set";
            
            if (plugin.getWantedManager().setWantedLevel(target, level, reason)) {
                plugin.getMessageManager().sendMessage(sender, "admin.wanted.set-success",
                    stringPlaceholder("player", target.getName()),
                    numberPlaceholder("level", level));
            } else {
                plugin.getMessageManager().sendMessage(sender, "universal.failed");
            }
        } catch (NumberFormatException e) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-number",
                stringPlaceholder("value", args[3]));
        }
        return true;
    }
    
    private boolean handleWantedClear(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted clear <player>"));
            return true;
        }
        
        Player clearTarget = Bukkit.getPlayer(args[2]);
        if (clearTarget == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        if (plugin.getWantedManager().clearWantedLevel(clearTarget)) {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.clear-success",
                stringPlaceholder("player", clearTarget.getName()));
        } else {
            plugin.getMessageManager().sendMessage(sender, "universal.failed");
        }
        return true;
    }
    
    private boolean handleWantedCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections wanted check <player>"));
            return true;
        }
        
        Player checkTarget = Bukkit.getPlayer(args[2]);
        if (checkTarget == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        int level = plugin.getWantedManager().getWantedLevel(checkTarget);
        if (level > 0) {
            long remainingTime = plugin.getWantedManager().getRemainingWantedTime(checkTarget);
            String reason = plugin.getWantedManager().getWantedReason(checkTarget);
            
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-result",
                stringPlaceholder("player", checkTarget.getName()),
                numberPlaceholder("level", level),
                starsPlaceholder("stars", level));
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-time",
                timePlaceholder("time", remainingTime / 1000));
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-reason",
                stringPlaceholder("reason", reason));
        } else {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.check-none",
                stringPlaceholder("player", checkTarget.getName()));
        }
        return true;
    }
    
    private boolean handleWantedList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-header");
        
        boolean foundWanted = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getWantedManager().isWanted(player)) {
                int playerLevel = plugin.getWantedManager().getWantedLevel(player);
                long remainingTime = plugin.getWantedManager().getRemainingWantedTime(player);
                
                plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-entry",
                    stringPlaceholder("player", player.getName()),
                    numberPlaceholder("level", playerLevel),
                    starsPlaceholder("stars", playerLevel),
                    timePlaceholder("time", remainingTime / 1000));
                foundWanted = true;
            }
        }
        
        if (!foundWanted) {
            plugin.getMessageManager().sendMessage(sender, "admin.wanted.list-none");
        }
        return true;
    }
    
    private boolean handleChaseAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections chase <list|end|endall> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                return handleChaseList(sender, args);
            case "end":
                return handleChaseEndAdmin(sender, args);
            case "endall":
                return handleChaseEndAll(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleChaseList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.chase.list-header");
        
        boolean foundChases = false;
        for (ChaseData chase : plugin.getDataManager().getAllActiveChases()) {
            if (chase.isActive()) {
                Player guard = Bukkit.getPlayer(chase.getGuardId());
                Player target = Bukkit.getPlayer(chase.getTargetId());
                String guardName = guard != null ? guard.getName() : "Unknown";
                String targetName = target != null ? target.getName() : "Unknown";
                
                plugin.getMessageManager().sendMessage(sender, "admin.chase.list-entry",
                    stringPlaceholder("guard", guardName),
                    stringPlaceholder("target", targetName),
                    timePlaceholder("time", chase.getRemainingTime() / 1000));
                foundChases = true;
            }
        }
        
        if (!foundChases) {
            plugin.getMessageManager().sendMessage(sender, "admin.chase.list-none");
        }
        return true;
    }
    
    private boolean handleChaseEndAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections chase end <guard>"));
            return true;
        }
        
        Player guard = Bukkit.getPlayer(args[2]);
        if (guard == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[2]));
            return true;
        }
        
        ChaseData chase = plugin.getDataManager().getChaseByGuard(guard.getUniqueId());
        if (chase == null) {
            plugin.getMessageManager().sendMessage(sender, "admin.chase.not-in-chase",
                stringPlaceholder("guard", guard.getName()));
            return true;
        }
        
        plugin.getChaseManager().endChase(chase.getChaseId(), "Ended by admin");
        plugin.getMessageManager().sendMessage(sender, "admin.chase.end-success",
            stringPlaceholder("guard", guard.getName()));
        return true;
    }
    
    private boolean handleChaseEndAll(CommandSender sender, String[] args) {
        int endedCount = 0;
        for (ChaseData chaseData : plugin.getDataManager().getAllActiveChases()) {
            if (chaseData.isActive()) {
                plugin.getChaseManager().endChase(chaseData.getChaseId(), "Ended by admin");
                endedCount++;
            }
        }
        
        plugin.getMessageManager().sendMessage(sender, "admin.chase.end-all-success",
            numberPlaceholder("count", endedCount));
        return true;
    }
    
    private boolean handleDutyAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections duty <list> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                return handleDutyList(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleDutyList(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-header");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDutyManager().hasGuardPermission(player)) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (data != null) {
                    if (data.isOnDuty()) {
                        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-on-duty",
                            stringPlaceholder("player", player.getName()));
                    } else {
                        plugin.getMessageManager().sendMessage(sender, "admin.duty.list-off-duty",
                            stringPlaceholder("player", player.getName()));
                    }
                }
            }
        }
        return true;
    }
    
    private boolean handlePlayerAdminCommand(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "universal.failed");
        return true;
    }
    
    private boolean handleSystemCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system <stats|debug> [args...]"));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "stats":
                return handleSystemStats(sender, args);
            case "debug":
                return handleSystemDebug(sender, args);
            default:
                plugin.getMessageManager().sendMessage(sender, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", action));
                return true;
        }
    }
    
    private boolean handleSystemStats(CommandSender sender, String[] args) {
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-header");
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-online",
            numberPlaceholder("online", Bukkit.getOnlinePlayers().size()),
            numberPlaceholder("max", Bukkit.getMaxPlayers()));
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-chases",
            numberPlaceholder("count", plugin.getDataManager().getActiveChaseCount()));
        
        String debugStatus = plugin.getConfigManager().isDebugMode() ? "admin.system.debug-status-enabled" : "admin.system.debug-status-disabled";
        plugin.getMessageManager().sendMessage(sender, "admin.system.stats-debug",
            stringPlaceholder("status", plugin.getMessageManager().getRawMessage(debugStatus)));
        return true;
    }
    
    private boolean handleSystemDebug(CommandSender sender, String[] args) {
        if (args.length < 3) {
            String debugStatus = plugin.getConfigManager().isDebugMode() ? "enabled" : "disabled";
            plugin.getMessageManager().sendMessage(sender, "system.debug-" + debugStatus);
            return true;
        }
        
        String debugValue = args[2].toLowerCase();
        if (debugValue.equals("on") || debugValue.equals("true")) {
            plugin.getConfigManager().setDebugMode(true);
            plugin.getMessageManager().sendMessage(sender, "admin.system.debug-enabled");
        } else if (debugValue.equals("off") || debugValue.equals("false")) {
            plugin.getConfigManager().setDebugMode(false);
            plugin.getMessageManager().sendMessage(sender, "admin.system.debug-disabled");
        } else if (debugValue.equals("rank")) {
            return handleDebugRank(sender, args);
        } else {
            plugin.getMessageManager().sendMessage(sender, "system.debug-invalid-value");
        }
        return true;
    }
    
    private boolean handleDebugRank(CommandSender sender, String[] args) {
        if (args.length < 4) {
            plugin.getMessageManager().sendMessage(sender, "universal.invalid-usage",
                stringPlaceholder("command", "/corrections system debug rank <player>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-not-found",
                stringPlaceholder("player", args[3]));
            return true;
        }
        
        sender.sendMessage("§6=== Debug Rank Information for " + target.getName() + " ===");
        
        // Check basic permission
        boolean hasBasicPerm = target.hasPermission("edencorrections.guard");
        sender.sendMessage("§7Basic Permission (edencorrections.guard): " + (hasBasicPerm ? "§aYES" : "§cNO"));
        
        // Check LuckPerms rank
        String detectedRank = plugin.getDutyManager().getPlayerGuardRank(target);
        sender.sendMessage("§7Detected Guard Rank: " + (detectedRank != null ? "§a" + detectedRank : "§cNone"));
        
        // Show config mappings
        sender.sendMessage("§7Config Rank Mappings:");
        Map<String, String> rankMappings = plugin.getConfigManager().getRankMappings();
        for (Map.Entry<String, String> entry : rankMappings.entrySet()) {
            sender.sendMessage("§7  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        // Show player's LuckPerms groups
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(target.getUniqueId());
                if (user != null) {
                    sender.sendMessage("§7Player's LuckPerms Groups:");
                    user.getInheritedGroups(user.getQueryOptions()).forEach(group -> {
                        sender.sendMessage("§7  - " + group.getName());
                    });
                } else {
                    sender.sendMessage("§cNo LuckPerms user data found!");
                }
            } catch (Exception e) {
                sender.sendMessage("§cError accessing LuckPerms: " + e.getMessage());
            }
        } else {
            sender.sendMessage("§cLuckPerms not available!");
        }
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        try {
            plugin.reload();
            plugin.getMessageManager().sendMessage(sender, "system.reload-success");
        } catch (Exception e) {
            plugin.getMessageManager().sendMessage(sender, "system.reload-failed",
                stringPlaceholder("error", e.getMessage()));
        }
        return true;
    }
    
    // === CONTRABAND COMMAND HANDLERS ===
    
    private boolean handleContrabandCommand(CommandSender sender, String contrabandType, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player guard = (Player) sender;
        
        if (!guard.hasPermission("edencorrections.guard.contraband")) {
            plugin.getMessageManager().sendMessage(guard, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(guard, "universal.player-not-found",
                stringPlaceholder("player", targetName));
            return true;
        }
        
        // Can't target self
        if (target.equals(guard)) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/" + contrabandType + " <player>"));
            return true;
        }
        
        // Can't target guards on duty
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.restrictions.target-on-duty");
            return true;
        }
        
        // Process contraband request
        plugin.getContrabandManager().requestContraband(guard, target, contrabandType);
        return true;
    }
    
    private boolean handleDrugTestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player guard = (Player) sender;
        
        if (!guard.hasPermission("edencorrections.guard.contraband")) {
            plugin.getMessageManager().sendMessage(guard, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/drugtest <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessageManager().sendMessage(guard, "universal.player-not-found",
                stringPlaceholder("player", targetName));
            return true;
        }
        
        // Can't target self
        if (target.equals(guard)) {
            plugin.getMessageManager().sendMessage(guard, "universal.invalid-usage",
                stringPlaceholder("command", "/drugtest <player>"));
            return true;
        }
        
        // Can't target guards on duty
        if (plugin.getDutyManager().isOnDuty(target)) {
            plugin.getMessageManager().sendMessage(guard, "contraband.restrictions.target-on-duty");
            return true;
        }
        
        // Perform drug test
        plugin.getContrabandManager().performDrugTest(guard, target);
        return true;
    }
    
    private boolean handleDutyBankCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard.banking")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        if (args.length != 1) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/dutybank <convert|status>"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "convert":
                plugin.getDutyBankingManager().convertDutyTime(player);
                break;
            case "status":
                plugin.getDutyBankingManager().showBankingStatus(player);
                break;
            default:
                plugin.getMessageManager().sendMessage(player, "universal.unknown-subcommand",
                    stringPlaceholder("subcommand", subCommand));
                break;
        }
        return true;
    }
    
    private boolean handleTipsCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().sendMessage(sender, "universal.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("edencorrections.guard")) {
            plugin.getMessageManager().sendMessage(player, "universal.no-permission");
            return true;
        }
        
        String system = "duty-system"; // Default system
        
        if (args.length == 1) {
            system = args[0].toLowerCase() + "-system";
        }
        
        // Get tips from config
        List<String> tips = plugin.getConfigManager().getConfig().getStringList("messages.tips." + system);
        
        if (tips.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "universal.invalid-usage",
                stringPlaceholder("command", "/tips [duty|contraband|chase|jail|banking]"));
            return true;
        }
        
        // Send tips to player
        for (String tip : tips) {
            plugin.getMessageManager().sendRawMessage(player, tip);
        }
        
        return true;
    }
    
    // === TAB COMPLETION HANDLERS ===
    
    private List<String> handleChaseTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: player name or subcommand
            List<String> subcommands = Arrays.asList("capture", "end");
            completions.addAll(subcommands);
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleJailTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleJailOfflineTabComplete(CommandSender sender, String[] args) {
        // For offline jail, we can't really predict player names
        return new ArrayList<>();
    }
    
    private List<String> handleCorrectionsTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("wanted", "chase", "duty", "player", "system", "reload", "help"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "wanted":
                    completions.addAll(Arrays.asList("set", "clear", "check", "list"));
                    break;
                case "chase":
                    completions.addAll(Arrays.asList("list", "end", "endall"));
                    break;
                case "duty":
                    completions.addAll(Arrays.asList("list"));
                    break;
                case "system":
                    completions.addAll(Arrays.asList("stats", "debug"));
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            
            if (subCommand.equals("wanted") && (action.equals("set") || action.equals("clear") || action.equals("check"))) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("chase") && action.equals("end")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (subCommand.equals("system") && action.equals("debug")) {
                completions.addAll(Arrays.asList("on", "off", "rank"));
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String option = args[2].toLowerCase();
            
            if (subCommand.equals("system") && action.equals("debug") && option.equals("rank")) {
                completions.addAll(getOnlinePlayerNames());
            }
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleContrabandTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(getOnlinePlayerNames());
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleDutyBankTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("convert", "status"));
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> handleTipsTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("duty", "contraband", "chase", "jail", "banking"));
        }
        
        return filterCompletions(completions, args);
    }
    
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    }
    
    private List<String> filterCompletions(List<String> completions, String[] args) {
        if (args.length == 0) return completions;
        
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
} 