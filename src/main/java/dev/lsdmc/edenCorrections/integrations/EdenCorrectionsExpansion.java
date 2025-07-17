package dev.lsdmc.edenCorrections.integrations;

import dev.lsdmc.edenCorrections.EdenCorrections;
import dev.lsdmc.edenCorrections.models.PlayerData;
import dev.lsdmc.edenCorrections.models.ChaseData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class EdenCorrectionsExpansion extends PlaceholderExpansion {

    private final EdenCorrections plugin;

    public EdenCorrectionsExpansion(EdenCorrections plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "edencorrections";
    }

    @Override
    public String getAuthor() {
        return "LSDMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Required for internal expansions to prevent unregistering on reload
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null) {
            return null;
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return null;
        }

        // Parse the placeholder parameter
        String[] parts = params.split("_");
        String category = parts[0].toLowerCase();

        switch (category) {
            case "wanted":
                return handleWantedPlaceholders(data, parts);
            case "duty":
                return handleDutyPlaceholders(data, player, parts);
            case "chase":
                return handleChasePlaceholders(data, player, parts);
            case "jail":
                return handleJailPlaceholders(data, parts);
            case "contraband":
                return handleContrabandPlaceholders(data, player, parts);
            case "banking":
                return handleBankingPlaceholders(data, player, parts);
            case "player":
                return handlePlayerPlaceholders(data, player, parts);
            default:
                return null;
        }
    }

    private String handleWantedPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "level":
                return String.valueOf(data.getWantedLevel());
            case "stars":
                return plugin.getWantedManager().getWantedStars(data.getWantedLevel());
            case "time":
                long remaining = data.getRemainingWantedTime();
                return remaining > 0 ? String.valueOf(remaining / 1000) : "0";
            case "reason":
                return data.getWantedReason();
            case "active":
                return String.valueOf(data.isWanted());
            default:
                return null;
        }
    }

    private String handleDutyPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "status":
                return data.isOnDuty() ? "On Duty" : "Off Duty";
            case "active":
                return String.valueOf(data.isOnDuty());
            case "rank":
                return data.getGuardRank() != null ? data.getGuardRank() : "None";
            case "time":
                if (data.isOnDuty()) {
                    long dutyTime = (System.currentTimeMillis() - data.getDutyStartTime()) / 1000;
                    return String.valueOf(dutyTime);
                }
                return "0";
            case "total":
                return String.valueOf(data.getTotalDutyTime() / 1000);
            default:
                return null;
        }
    }

    private String handleChasePlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "active":
                return String.valueOf(data.isBeingChased());
            case "target":
                if (data.isBeingChased()) {
                    Player chaser = plugin.getServer().getPlayer(data.getChaserGuard());
                    return chaser != null ? chaser.getName() : "Unknown";
                }
                return "None";
            case "guard":
                ChaseData chase = plugin.getDataManager().getChaseByGuard(player.getUniqueId());
                if (chase != null) {
                    Player target = plugin.getServer().getPlayer(chase.getTargetId());
                    return target != null ? target.getName() : "Unknown";
                }
                return "None";
            case "time":
                if (data.isBeingChased()) {
                    long chaseTime = (System.currentTimeMillis() - data.getChaseStartTime()) / 1000;
                    return String.valueOf(chaseTime);
                }
                return "0";
            case "combat":
                return String.valueOf(plugin.getChaseManager().isInCombat(player));
            default:
                return null;
        }
    }

    private String handleJailPlaceholders(PlayerData data, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "countdown":
                return String.valueOf(plugin.getJailManager().isInJailCountdown(plugin.getServer().getPlayer(data.getPlayerId())));
            default:
                return null;
        }
    }

    private String handleContrabandPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "request":
                return String.valueOf(plugin.getContrabandManager().hasActiveRequest(player));
            default:
                return null;
        }
    }

    private String handleBankingPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "tokens":
                return String.valueOf(plugin.getDutyBankingManager().getAvailableTokens(player));
            case "time":
                return String.valueOf(plugin.getDutyBankingManager().getTotalDutyTime(player));
            case "enabled":
                return String.valueOf(plugin.getConfigManager().isDutyBankingEnabled());
            default:
                return null;
        }
    }

    private String handlePlayerPlaceholders(PlayerData data, Player player, String[] parts) {
        if (parts.length < 2) return null;

        switch (parts[1].toLowerCase()) {
            case "arrests":
                return String.valueOf(data.getTotalArrests());
            case "violations":
                return String.valueOf(data.getTotalViolations());
            case "power":
                return String.valueOf(data.getTotalDutyTime()); // Using duty time as power for now
            case "name":
                return player.getName();
            default:
                return null;
        }
    }
} 