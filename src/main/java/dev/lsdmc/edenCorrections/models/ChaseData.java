package dev.lsdmc.edenCorrections.models;

import java.util.UUID;

public class ChaseData {
    
    private final UUID chaseId;
    private final UUID guardId;
    private final UUID targetId;
    private final long startTime;
    private final long duration;
    
    private boolean isActive;
    private String endReason;
    private long endTime;
    
    public ChaseData(UUID chaseId, UUID guardId, UUID targetId, long duration) {
        this.chaseId = chaseId;
        this.guardId = guardId;
        this.targetId = targetId;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.isActive = true;
        this.endReason = "";
        this.endTime = 0;
    }
    
    // Getters
    public UUID getChaseId() { return chaseId; }
    public UUID getGuardId() { return guardId; }
    public UUID getTargetId() { return targetId; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return duration; }
    public boolean isActive() { return isActive; }
    public String getEndReason() { return endReason; }
    public long getEndTime() { return endTime; }
    
    // Utility methods
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    public long getRemainingTime() {
        long elapsed = getElapsedTime();
        return Math.max(0, duration - elapsed);
    }
    
    public boolean isExpired() {
        return getElapsedTime() >= duration;
    }
    
    public void endChase(String reason) {
        this.isActive = false;
        this.endReason = reason;
        this.endTime = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "ChaseData{" +
                "chaseId=" + chaseId +
                ", guardId=" + guardId +
                ", targetId=" + targetId +
                ", isActive=" + isActive +
                ", remainingTime=" + getRemainingTime() +
                '}';
    }
} 