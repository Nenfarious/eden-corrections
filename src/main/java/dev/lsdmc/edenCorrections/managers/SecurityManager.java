package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

/**
 * Security Manager - Handles guard immunity and security restrictions
 * Prevents certain actions against guards on duty based on configuration
 */
public class SecurityManager implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public SecurityManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        logger.info("SecurityManager initialized successfully!");
        
        // Register event listeners
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Check if a player is protected by guard immunity
     */
    public boolean isPlayerProtected(Player player) {
        if (!plugin.getConfigManager().isGuardImmunityEnabled()) {
            return false;
        }
        
        return plugin.getDutyManager().isOnDuty(player);
    }
    
    /**
     * Check if a player can be set as wanted
     */
    public boolean canPlayerBeWanted(Player player) {
        if (!plugin.getConfigManager().isGuardWantedProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be chased
     */
    public boolean canPlayerBeChased(Player player) {
        if (!plugin.getConfigManager().isGuardChaseProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be targeted for contraband
     */
    public boolean canPlayerBeContrabandTargeted(Player player) {
        if (!plugin.getConfigManager().isGuardContrabandProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be jailed
     */
    public boolean canPlayerBeJailed(Player player) {
        if (!plugin.getConfigManager().isGuardJailProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be attacked (combat protection)
     */
    public boolean canPlayerBeAttacked(Player player) {
        if (!plugin.getConfigManager().isGuardCombatProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be teleported against their will
     */
    public boolean canPlayerBeTeleported(Player player) {
        if (!plugin.getConfigManager().isGuardTeleportProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Handle entity damage events for combat protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Check if victim is protected from combat
        if (!canPlayerBeAttacked(victim)) {
            event.setCancelled(true);
            
            // Notify attacker
            plugin.getMessageManager().sendMessage(attacker, "security.guard-immunity.combat-protected",
                playerPlaceholder("player", victim));
            
            // Notify victim
            plugin.getMessageManager().sendMessage(victim, "security.guard-immunity.attack-blocked",
                playerPlaceholder("player", attacker));
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Combat attack blocked - " + attacker.getName() + " attacked protected guard " + victim.getName());
            }
        }
    }
    
    /**
     * Handle teleport events for teleport protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if teleport is forced (not player-initiated)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || 
            event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            
            if (!canPlayerBeTeleported(player)) {
                event.setCancelled(true);
                
                // Notify player
                plugin.getMessageManager().sendMessage(player, "security.guard-immunity.teleport-blocked");
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Teleport blocked for protected guard " + player.getName());
                }
            }
        }
    }
    
    /**
     * Handle command events for command protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Check for commands that might affect protected players
        if (command.startsWith("/wanted") || command.startsWith("/chase") || 
            command.startsWith("/jail") || command.startsWith("/contraband")) {
            
            // This is handled in the respective managers, but we log it here
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Command executed by " + player.getName() + ": " + command);
            }
        }
    }
    
    /**
     * Get security status for a player
     */
    public String getPlayerSecurityStatus(Player player) {
        if (!isPlayerProtected(player)) {
            return "UNPROTECTED";
        }
        
        StringBuilder status = new StringBuilder("PROTECTED (");
        boolean first = true;
        
        if (plugin.getConfigManager().isGuardWantedProtected()) {
            if (!first) status.append(", ");
            status.append("WANTED");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardChaseProtected()) {
            if (!first) status.append(", ");
            status.append("CHASE");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardContrabandProtected()) {
            if (!first) status.append(", ");
            status.append("CONTRABAND");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardJailProtected()) {
            if (!first) status.append(", ");
            status.append("JAIL");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardCombatProtected()) {
            if (!first) status.append(", ");
            status.append("COMBAT");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardTeleportProtected()) {
            if (!first) status.append(", ");
            status.append("TELEPORT");
        }
        
        status.append(")");
        return status.toString();
    }
    
    /**
     * Log security violation attempt
     */
    public void logSecurityViolation(String action, Player perpetrator, Player target) {
        logger.warning("SECURITY VIOLATION: " + perpetrator.getName() + " attempted " + action + " on protected guard " + target.getName());
        
        // Notify online administrators
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("edencorrections.admin"))
            .forEach(admin -> {
                plugin.getMessageManager().sendMessage(admin, "security.violation-alert",
                    stringPlaceholder("action", action),
                    playerPlaceholder("perpetrator", perpetrator),
                    playerPlaceholder("target", target));
            });
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        // No cleanup needed for this manager
    }
=======
package dev.lsdmc.edenCorrections.managers;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.logging.Logger;

import static dev.lsdmc.edenCorrections.managers.MessageManager.*;

/**
 * Security Manager - Handles guard immunity and security restrictions
 * Prevents certain actions against guards on duty based on configuration
 */
public class SecurityManager implements Listener {
    
    private final EdenCorrections plugin;
    private final Logger logger;
    
    public SecurityManager(EdenCorrections plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void initialize() {
        logger.info("SecurityManager initialized successfully!");
        
        // Register event listeners
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Check if a player is protected by guard immunity
     */
    public boolean isPlayerProtected(Player player) {
        if (!plugin.getConfigManager().isGuardImmunityEnabled()) {
            return false;
        }
        
        return plugin.getDutyManager().isOnDuty(player);
    }
    
    /**
     * Check if a player can be set as wanted
     */
    public boolean canPlayerBeWanted(Player player) {
        if (!plugin.getConfigManager().isGuardWantedProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be chased
     */
    public boolean canPlayerBeChased(Player player) {
        if (!plugin.getConfigManager().isGuardChaseProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be targeted for contraband
     */
    public boolean canPlayerBeContrabandTargeted(Player player) {
        if (!plugin.getConfigManager().isGuardContrabandProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be jailed
     */
    public boolean canPlayerBeJailed(Player player) {
        if (!plugin.getConfigManager().isGuardJailProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be attacked (combat protection)
     */
    public boolean canPlayerBeAttacked(Player player) {
        if (!plugin.getConfigManager().isGuardCombatProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Check if a player can be teleported against their will
     */
    public boolean canPlayerBeTeleported(Player player) {
        if (!plugin.getConfigManager().isGuardTeleportProtected()) {
            return true;
        }
        
        return !isPlayerProtected(player);
    }
    
    /**
     * Handle entity damage events for combat protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Check if victim is protected from combat
        if (!canPlayerBeAttacked(victim)) {
            event.setCancelled(true);
            
            // Notify attacker
            plugin.getMessageManager().sendMessage(attacker, "security.guard-immunity.combat-protected",
                playerPlaceholder("player", victim));
            
            // Notify victim
            plugin.getMessageManager().sendMessage(victim, "security.guard-immunity.attack-blocked",
                playerPlaceholder("player", attacker));
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Combat attack blocked - " + attacker.getName() + " attacked protected guard " + victim.getName());
            }
        }
    }
    
    /**
     * Handle teleport events for teleport protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if teleport is forced (not player-initiated)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || 
            event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            
            if (!canPlayerBeTeleported(player)) {
                event.setCancelled(true);
                
                // Notify player
                plugin.getMessageManager().sendMessage(player, "security.guard-immunity.teleport-blocked");
                
                if (plugin.getConfigManager().isDebugMode()) {
                    logger.info("DEBUG: Teleport blocked for protected guard " + player.getName());
                }
            }
        }
    }
    
    /**
     * Handle command events for command protection
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Check for commands that might affect protected players
        if (command.startsWith("/wanted") || command.startsWith("/chase") || 
            command.startsWith("/jail") || command.startsWith("/contraband")) {
            
            // This is handled in the respective managers, but we log it here
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("DEBUG: Command executed by " + player.getName() + ": " + command);
            }
        }
    }
    
    /**
     * Get security status for a player
     */
    public String getPlayerSecurityStatus(Player player) {
        if (!isPlayerProtected(player)) {
            return "UNPROTECTED";
        }
        
        StringBuilder status = new StringBuilder("PROTECTED (");
        boolean first = true;
        
        if (plugin.getConfigManager().isGuardWantedProtected()) {
            if (!first) status.append(", ");
            status.append("WANTED");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardChaseProtected()) {
            if (!first) status.append(", ");
            status.append("CHASE");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardContrabandProtected()) {
            if (!first) status.append(", ");
            status.append("CONTRABAND");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardJailProtected()) {
            if (!first) status.append(", ");
            status.append("JAIL");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardCombatProtected()) {
            if (!first) status.append(", ");
            status.append("COMBAT");
            first = false;
        }
        
        if (plugin.getConfigManager().isGuardTeleportProtected()) {
            if (!first) status.append(", ");
            status.append("TELEPORT");
        }
        
        status.append(")");
        return status.toString();
    }
    
    /**
     * Log security violation attempt
     */
    public void logSecurityViolation(String action, Player perpetrator, Player target) {
        logger.warning("SECURITY VIOLATION: " + perpetrator.getName() + " attempted " + action + " on protected guard " + target.getName());
        
        // Notify online administrators
        plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("edencorrections.admin"))
            .forEach(admin -> {
                plugin.getMessageManager().sendMessage(admin, "security.violation-alert",
                    stringPlaceholder("action", action),
                    playerPlaceholder("perpetrator", perpetrator),
                    playerPlaceholder("target", target));
            });
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        // No cleanup needed for this manager
    }
>>>>>>> 802b20989bd53e59c06b10b624bd5acdc909227d
} 