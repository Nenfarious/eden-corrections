package dev.lsdmc.edenCorrections.events;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;
import org.bukkit.Location;

public class GuardEventHandler implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    // Commands blocked during chase
    private final List<String> blockedChaseCommands = Arrays.asList(
        "/spawn", "/home", "/tpa", "/tpaccept", "/warp", "/back", "/rtp"
    );
    
    public GuardEventHandler(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Initialize or load player data
        PlayerData data = plugin.getDataManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
        
        // Check for expired wanted levels
        if (data.hasExpiredWanted()) {
            data.clearWantedLevel();
            plugin.getDataManager().savePlayerData(data);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Player " + player.getName() + " joined - Data loaded/created");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle player leaving during chase
        if (plugin.getDataManager().isPlayerBeingChased(player.getUniqueId())) {
            // End chase if target leaves
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByTarget(player.getUniqueId()).getChaseId(),
                "Target disconnected"
            );
        } else if (plugin.getDataManager().isGuardChasing(player.getUniqueId())) {
            // End chase if guard leaves
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByGuard(player.getUniqueId()).getChaseId(),
                "Guard disconnected"
            );
        }
        
        // Cancel any active jail countdown
        if (plugin.getJailManager().isInJailCountdown(player)) {
            plugin.getJailManager().cancelJailCountdown(player);
        }
        
        // Comprehensive cleanup for all systems
        cleanupPlayerSystems(player);
        
        // Save player data
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            plugin.getDataManager().savePlayerData(data);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            logger.info("Player " + player.getName() + " left - Data saved and systems cleaned up");
        }
    }
    
    private void cleanupPlayerSystems(Player player) {
        // Clean up duty system
        plugin.getDutyManager().cleanupPlayer(player);
        
        // Clean up chase system (combat timers)
        plugin.getChaseManager().cleanupPlayer(player);
        
        // Clean up contraband system
        plugin.getContrabandManager().cleanupPlayer(player);
        
        // Clean up boss bar system
        plugin.getBossBarManager().cleanupPlayer(player);
        
        // Clean up message system (action bars)
        plugin.getMessageManager().cleanupPlayer(player);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer != null && killer != victim) {
            // Enhanced guard protection logic
            handlePlayerKill(killer, victim);
        }
        
        // End any active chases involving the dead player
        if (plugin.getDataManager().isPlayerBeingChased(victim.getUniqueId())) {
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByTarget(victim.getUniqueId()).getChaseId(),
                "Target died"
            );
        } else if (plugin.getDataManager().isGuardChasing(victim.getUniqueId())) {
            plugin.getChaseManager().endChase(
                plugin.getDataManager().getChaseByGuard(victim.getUniqueId()).getChaseId(),
                "Guard died"
            );
        }
        
        // Clean up all active systems for the dead player
        cleanupPlayerSystems(victim);
        
        // Clean up combat timers for both players
        if (killer != null) {
            plugin.getChaseManager().endCombatTimer(victim);
            plugin.getChaseManager().endCombatTimer(killer);
        }
    }
    
    private void handlePlayerKill(Player killer, Player victim) {
        String killerRank = plugin.getDutyManager().getPlayerGuardRank(killer);
        String victimRank = plugin.getDutyManager().getPlayerGuardRank(victim);
        boolean killerIsGuard = killerRank != null;
        boolean victimIsGuard = victimRank != null && plugin.getDutyManager().isOnDuty(victim);
        
        if (victimIsGuard && !killerIsGuard) {
            // Non-guard killed guard - major violation
            plugin.getWantedManager().handlePlayerKillGuard(killer, victim);
            logger.info("NON-GUARD KILLED GUARD: " + killer.getName() + " killed guard " + 
                       victim.getName() + " (" + victimRank + ")");
                       
        } else if (victimIsGuard && killerIsGuard) {
            // Guard vs guard combat - log for administrative review
            logger.warning("GUARD VS GUARD COMBAT: Guard " + killer.getName() + " (" + killerRank + 
                          ") killed guard " + victim.getName() + " (" + victimRank + ")");
            // This may or may not result in wanted level depending on server policy
            
        } else if (!victimIsGuard && killerIsGuard) {
            // Guard killed regular player - this is usually acceptable
            logger.info("Guard " + killer.getName() + " (" + killerRank + ") killed player " + victim.getName());
            
            // Award performance for kill (guards killing criminals is good)
            plugin.getDutyManager().awardKillPerformance(killer, victim);
            
        } else {
            // Regular player vs player combat
            plugin.getWantedManager().handlePlayerKillPlayer(killer, victim);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        // Security check: Can victim be attacked?
        if (!plugin.getSecurityManager().canPlayerBeAttacked(victim)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "security.guard-immunity.combat-protected",
                playerPlaceholder("player", victim));
            plugin.getSecurityManager().logSecurityViolation("attack", attacker, victim);
            return;
        }
        
        // Trigger combat timers for both players
        plugin.getChaseManager().handleCombatEvent(victim);
        plugin.getChaseManager().handleCombatEvent(attacker);
        
        // Check if player is attacking a guard (existing logic)
        if (plugin.getDutyManager().hasGuardPermission(victim) && plugin.getDutyManager().isOnDuty(victim)) {
            // Warn the attacker
            plugin.getMessageManager().sendMessage(attacker, "wanted.warnings.attacking-guard");
            
            // Guard protection logic - potentially increase wanted level for attacking guard
            String attackerRank = plugin.getDutyManager().getPlayerGuardRank(attacker);
            String victimRank = plugin.getDutyManager().getPlayerGuardRank(victim);
            
            if (attackerRank == null) {
                // Non-guard attacking guard - this is a serious violation
                logger.info("Non-guard " + attacker.getName() + " attacked guard " + victim.getName());
                // This could increase wanted level depending on server policy
            } else {
                // Guard vs guard combat - log for administrative review
                logger.info("Guard " + attacker.getName() + " (" + attackerRank + ") attacked guard " + 
                           victim.getName() + " (" + victimRank + ")");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Security check: Can player be teleported?
        if (!plugin.getSecurityManager().canPlayerBeTeleported(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "security.guard-immunity.teleport-blocked");
            plugin.getSecurityManager().logSecurityViolation("teleport", player, null);
            return;
        }
        
        // Block teleportation during combat timer
        if (plugin.getChaseManager().isInCombat(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "combat.teleport-blocked");
            return;
        }
        
        // Block teleportation for wanted players
        if (plugin.getWantedManager().isWanted(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "wanted.blocking.teleport");
            return;
        }
        
        // Block teleportation during chase
        if (plugin.getChaseManager().isPlayerInChase(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "chase.blocking.teleport");
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Block certain commands during chase
        if (plugin.getChaseManager().isPlayerInChase(player)) {
            for (String blockedCommand : blockedChaseCommands) {
                if (command.startsWith(blockedCommand)) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "chase.blocking.command");
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Block mining for guards on duty
        if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardMiningBlocked()) {
                event.setCancelled(true);
                plugin.getMessageManager().sendMessage(player, "restrictions.mining");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Block building for guards on duty (optional restriction)
        if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
            // This could be configurable - for now we'll allow building
            // but this is where you'd add the restriction if needed
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Block storage access for guards on duty
        if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardStorageBlocked()) {
                // Check if it's a container (chest, barrel, etc.)
                String inventoryTitle = event.getView().getTitle().toLowerCase();
                if (inventoryTitle.contains("chest") || inventoryTitle.contains("barrel") || 
                    inventoryTitle.contains("shulker") || inventoryTitle.contains("ender")) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "restrictions.storage");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Block crafting for guards on duty
        if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
            if (plugin.getConfigManager().isGuardCraftingBlocked()) {
                // Check if it's a crafting inventory
                String inventoryTitle = event.getView().getTitle().toLowerCase();
                if (inventoryTitle.contains("crafting") || inventoryTitle.contains("workbench") ||
                    inventoryTitle.contains("enchanting") || inventoryTitle.contains("anvil")) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "restrictions.crafting");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Handle contraband compliance first (before guard restrictions)
        plugin.getContrabandManager().handleItemDrop(player, event.getItemDrop().getItemStack());
        
        // Block item dropping for guards on duty (unless it's for contraband compliance)
        if (plugin.getDutyManager().hasGuardPermission(player) && plugin.getDutyManager().isOnDuty(player)) {
            // Allow dropping if player has an active contraband request
            if (!plugin.getContrabandManager().hasActiveRequest(player)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player, "restrictions.dropping");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check duty transition immobilization
        if (plugin.getDutyManager().isInDutyTransition(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && from.distanceSquared(to) > 0.25) { // Moved more than 0.5 blocks
                event.setCancelled(true);
                plugin.getDutyManager().cancelDutyTransition(player, "duty.restrictions.movement-cancelled");
                return;
            }
        }
        
        // Cancel jail countdown if target moves (if implemented in JailManager)
        if (plugin.getJailManager().isInJailCountdown(player)) {
            // This could be implemented to cancel jail countdown if player moves
            // For now, we'll allow some movement tolerance in the JailManager itself
        }
        
        // Check chase area restrictions for chased players
        if (plugin.getDataManager().isPlayerBeingChased(player.getUniqueId()) && 
            plugin.getConfigManager().shouldBlockRestrictedAreas()) {
            
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && !from.getWorld().equals(to.getWorld())) {
                // World change - check if destination is restricted
                if (plugin.getChaseManager().isPlayerInRestrictedArea(player)) {
                    event.setCancelled(true);
                    plugin.getMessageManager().sendMessage(player, "chase.blocking.area-entry");
                    return;
                }
            } else if (to != null) {
                // Same world movement - check if entering restricted area
                String[] restrictedAreas = plugin.getConfigManager().getChaseRestrictedAreas();
                for (String area : restrictedAreas) {
                    if (plugin.getWorldGuardUtils().isLocationInRegion(to, area) && 
                        !plugin.getWorldGuardUtils().isLocationInRegion(from, area)) {
                        // Entering restricted area
                        event.setCancelled(true);
                        plugin.getMessageManager().sendMessage(player, "chase.blocking.area-entry");
                        return;
                    }
                }
            }
        }
        
        // Update chase distance monitoring is handled by the ChaseManager task
        // No need to check every move event for performance reasons
    }
} 