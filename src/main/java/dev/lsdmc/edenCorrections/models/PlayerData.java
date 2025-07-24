package dev.lsdmc.edenCorrections.models;

import java.util.UUID;

public class PlayerData {
    
    private final UUID playerId;
    private final String playerName;
    
    // Duty information
    private boolean isOnDuty;
    private long dutyStartTime;
    private long offDutyTime;
    private long graceDebtTime;
    private String guardRank; // LuckPerms detected guard rank
    
    // NEW: Off-duty earning system
    private long earnedOffDutyTime; // Time in milliseconds they've earned and can use
    private boolean hasEarnedBaseTime; // Whether they've earned base time this session
    private boolean hasBeenNotifiedOfExpiredTime; // Prevents spam notifications
    
    // Performance tracking for current duty session
    private int sessionSearches;
    private int sessionSuccessfulSearches;
    private int sessionArrests;
    private int sessionKills;
    private int sessionDetections;
    
    // Wanted level information
    private int wantedLevel;
    private long wantedExpireTime;
    private String wantedReason;
    
    // Chase information
    private boolean beingChased;
    private UUID chaserGuard;
    private long chaseStartTime;
    
    // Statistics
    private int totalArrests;
    private int totalViolations;
    private long totalDutyTime;
    
    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        
        // Initialize defaults
        this.isOnDuty = false;
        this.dutyStartTime = 0;
        this.offDutyTime = 0;
        this.graceDebtTime = 0;
        this.guardRank = null;
        this.earnedOffDutyTime = 0;
        this.hasEarnedBaseTime = false;
        this.hasBeenNotifiedOfExpiredTime = false;
        this.sessionSearches = 0;
        this.sessionSuccessfulSearches = 0;
        this.sessionArrests = 0;
        this.sessionKills = 0;
        this.sessionDetections = 0;
        this.wantedLevel = 0;
        this.wantedExpireTime = 0;
        this.wantedReason = "";
        this.beingChased = false;
        this.chaserGuard = null;
        this.chaseStartTime = 0;
        this.totalArrests = 0;
        this.totalViolations = 0;
        this.totalDutyTime = 0;
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public boolean isOnDuty() { return isOnDuty; }
    public long getDutyStartTime() { return dutyStartTime; }
    public long getOffDutyTime() { return offDutyTime; }
    public long getGraceDebtTime() { return graceDebtTime; }
    public String getGuardRank() { return guardRank; }
    public int getWantedLevel() { return wantedLevel; }
    public long getWantedExpireTime() { return wantedExpireTime; }
    public String getWantedReason() { return wantedReason; }
    public boolean isBeingChased() { return beingChased; }
    public UUID getChaserGuard() { return chaserGuard; }
    public long getChaseStartTime() { return chaseStartTime; }
    public int getTotalArrests() { return totalArrests; }
    public int getTotalViolations() { return totalViolations; }
    public long getTotalDutyTime() { return totalDutyTime; }
    
    // NEW: Off-duty earning system getters
    public long getEarnedOffDutyTime() { return earnedOffDutyTime; }
    public boolean hasEarnedBaseTime() { return hasEarnedBaseTime; }
    public boolean hasBeenNotifiedOfExpiredTime() { return hasBeenNotifiedOfExpiredTime; }
    public int getSessionSearches() { return sessionSearches; }
    public int getSessionSuccessfulSearches() { return sessionSuccessfulSearches; }
    public int getSessionArrests() { return sessionArrests; }
    public int getSessionKills() { return sessionKills; }
    public int getSessionDetections() { return sessionDetections; }
    
    // Setters
    public void setOnDuty(boolean onDuty) { this.isOnDuty = onDuty; }
    public void setDutyStartTime(long dutyStartTime) { this.dutyStartTime = dutyStartTime; }
    public void setOffDutyTime(long offDutyTime) { this.offDutyTime = offDutyTime; }
    public void setGraceDebtTime(long graceDebtTime) { this.graceDebtTime = graceDebtTime; }
    public void setGuardRank(String guardRank) { this.guardRank = guardRank; }
    public void setWantedLevel(int wantedLevel) { this.wantedLevel = wantedLevel; }
    public void setWantedExpireTime(long wantedExpireTime) { this.wantedExpireTime = wantedExpireTime; }
    public void setWantedReason(String wantedReason) { this.wantedReason = wantedReason; }
    public void setBeingChased(boolean beingChased) { this.beingChased = beingChased; }
    public void setChaserGuard(UUID chaserGuard) { this.chaserGuard = chaserGuard; }
    public void setChaseStartTime(long chaseStartTime) { this.chaseStartTime = chaseStartTime; }
    public void setTotalArrests(int totalArrests) { this.totalArrests = totalArrests; }
    public void setTotalViolations(int totalViolations) { this.totalViolations = totalViolations; }
    public void setTotalDutyTime(long totalDutyTime) { this.totalDutyTime = totalDutyTime; }
    
    // NEW: Off-duty earning system setters
    public void setEarnedOffDutyTime(long earnedOffDutyTime) { this.earnedOffDutyTime = earnedOffDutyTime; }
    public void setHasEarnedBaseTime(boolean hasEarnedBaseTime) { this.hasEarnedBaseTime = hasEarnedBaseTime; }
    public void setHasBeenNotifiedOfExpiredTime(boolean hasBeenNotifiedOfExpiredTime) { this.hasBeenNotifiedOfExpiredTime = hasBeenNotifiedOfExpiredTime; }
    public void setSessionSearches(int sessionSearches) { this.sessionSearches = sessionSearches; }
    public void setSessionSuccessfulSearches(int sessionSuccessfulSearches) { this.sessionSuccessfulSearches = sessionSuccessfulSearches; }
    public void setSessionArrests(int sessionArrests) { this.sessionArrests = sessionArrests; }
    public void setSessionKills(int sessionKills) { this.sessionKills = sessionKills; }
    public void setSessionDetections(int sessionDetections) { this.sessionDetections = sessionDetections; }
    
    // Utility methods
    public boolean isWanted() {
        return wantedLevel > 0 && System.currentTimeMillis() < wantedExpireTime;
    }
    
    public boolean hasExpiredWanted() {
        return wantedLevel > 0 && System.currentTimeMillis() >= wantedExpireTime;
    }
    
    public long getRemainingWantedTime() {
        if (!isWanted()) return 0;
        return wantedExpireTime - System.currentTimeMillis();
    }
    
    public void clearWantedLevel() {
        this.wantedLevel = 0;
        this.wantedExpireTime = 0;
        this.wantedReason = "";
    }
    
    public void clearChaseData() {
        this.beingChased = false;
        this.chaserGuard = null;
        this.chaseStartTime = 0;
    }
    
    public void incrementArrests() {
        this.totalArrests++;
    }
    
    public void incrementViolations() {
        this.totalViolations++;
    }
    
    public void addDutyTime(long additionalTime) {
        this.totalDutyTime += additionalTime;
    }
    
    // Guard rank utility methods
    public boolean hasGuardRank() {
        return guardRank != null && !guardRank.isEmpty();
    }
    
    public boolean isGuardRank(String rank) {
        return guardRank != null && guardRank.equalsIgnoreCase(rank);
    }
    
    // NEW: Off-duty earning system utility methods
    public void addEarnedOffDutyTime(long additionalTime) {
        this.earnedOffDutyTime += additionalTime;
    }
    
    public void consumeOffDutyTime(long consumedTime) {
        this.earnedOffDutyTime = Math.max(0, this.earnedOffDutyTime - consumedTime);
    }
    
    public boolean hasAvailableOffDutyTime() {
        return earnedOffDutyTime > 0;
    }
    
    public long getAvailableOffDutyTimeInSeconds() {
        return earnedOffDutyTime / 1000L;
    }
    
    public long getAvailableOffDutyTimeInMinutes() {
        return earnedOffDutyTime / (1000L * 60L);
    }
    
    public void incrementSessionSearches() {
        this.sessionSearches++;
    }
    
    public void incrementSessionSuccessfulSearches() {
        this.sessionSuccessfulSearches++;
    }
    
    public void incrementSessionArrests() {
        this.sessionArrests++;
    }
    
    public void incrementSessionKills() {
        this.sessionKills++;
    }
    
    public void incrementSessionDetections() {
        this.sessionDetections++;
    }
    
    public void resetSessionStats() {
        this.sessionSearches = 0;
        this.sessionSuccessfulSearches = 0;
        this.sessionArrests = 0;
        this.sessionKills = 0;
        this.sessionDetections = 0;
        this.hasEarnedBaseTime = false;
        this.hasBeenNotifiedOfExpiredTime = false;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", isOnDuty=" + isOnDuty +
                ", guardRank='" + guardRank + '\'' +
                ", wantedLevel=" + wantedLevel +
                ", beingChased=" + beingChased +
                '}';
    }
} 