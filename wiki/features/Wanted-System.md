# ⭐ Wanted Level System

Complete guide to the Wanted Level System, including violations, escalation, and guard protection mechanics.

---

## 🎯 **System Overview**

The Wanted Level System provides a dynamic law enforcement mechanism with:

- **5-Star Wanted Levels** - Escalating threat assessment from 1-5 stars
- **Auto-Expiring Violations** - Configurable timeout periods for redemption
- **Guard Protection** - On-duty guards cannot be set as wanted
- **Violation Tracking** - Detailed reasons and timestamps
- **Integration Points** - Connects with chase, jail, and contraband systems

---

## ⭐ **Wanted Level Structure**

### Star System

| Level | Stars | Severity | Jail Time | Description |
|-------|-------|----------|-----------|-------------|
| **0** | - | None | 0 minutes | Clean record |
| **1** | ⭐ | Minor | 6 minutes | Minor violations |
| **2** | ⭐⭐ | Moderate | 7 minutes | Repeated offenses |
| **3** | ⭐⭐⭐ | Serious | 8 minutes | Major violations |
| **4** | ⭐⭐⭐⭐ | Severe | 9 minutes | Multiple serious crimes |
| **5** | ⭐⭐⭐⭐⭐ | Maximum | 10 minutes | Extreme criminal activity |

### Jail Time Calculation

```
Jail Time = base-time + (wanted-level × level-multiplier)
```

**Default Configuration:**
```yaml
jail:
  base-time: 300        # 5 minutes base
  level-multiplier: 60  # +1 minute per star
  max-wanted-level: 5   # Maximum 5 stars
```

**Examples:**
- **Level 1**: 300 + (1 × 60) = 360 seconds (6 minutes)
- **Level 3**: 300 + (3 × 60) = 480 seconds (8 minutes)
- **Level 5**: 300 + (5 × 60) = 600 seconds (10 minutes)

---

## 🚨 **Violation Categories**

### Automatic Violations

#### Contraband Possession
```yaml
# Triggered by contraband system
Violation: "Contraband possession: [item_type]"
Increase: +1 star
Trigger: Failed contraband compliance
```

**Examples:**
- "Contraband possession: weapons"
- "Contraband possession: drugs"
- "Contraband possession: armor"

#### Player vs Player Combat
```yaml
# Triggered by combat system
Violation: "Killing another player"
Increase: +1 star
Trigger: Player kills another player
```

**Considerations:**
- Only applies to non-guard kills
- Self-defense exceptions may apply
- Guard vs guard combat logged separately

#### Guard Assault
```yaml
# Triggered by guard protection
Violation: "Attacking a guard"
Increase: +2 stars
Trigger: Attacking on-duty guard
```

**High Priority:**
- Immediate +2 star increase
- Severe violation category
- Triggers guard alerts

#### Guard Elimination
```yaml
# Triggered by guard protection
Violation: "Killing a guard"
Increase: +2 stars
Trigger: Killing on-duty guard
```

**Maximum Severity:**
- Immediate +2 star increase
- Highest priority violation
- Server-wide guard alerts

#### Chase Evasion
```yaml
# Triggered by chase system
Violation: "Escaping from chase"
Increase: +1 star
Trigger: Chase timeout or escape
```

**Conditions:**
- Chase must last minimum duration
- Player must successfully evade capture
- Added to existing wanted level

### Manual Violations

#### Administrative Set
```bash
# Admin command
/corrections wanted set PlayerName 3 "Custom violation reason"
```

**Features:**
- Custom wanted level (1-5)
- Custom violation reason
- Immediate application
- Override existing level

#### Escalation Management
```bash
# Increase existing level
/corrections wanted set PlayerName 4 "Escalated violation"

# Clear wanted level
/corrections wanted clear PlayerName
```

---

## ⏰ **Time-Based Mechanics**

### Expiration System

#### Default Duration
```yaml
times:
  wanted-duration: 1800  # 30 minutes
```

**Behavior:**
- Wanted level expires after 30 minutes
- Automatic cleanup on expiration
- Player receives notification when cleared

#### Countdown Display
```yaml
# Placeholder showing remaining time
%edencorrections_wanted_time%  # Seconds remaining
```

**Integration:**
- PlaceholderAPI support
- Boss bar countdowns
- Action bar notifications

### Persistence

#### Database Storage
```sql
-- Player data table
wantedLevel INT DEFAULT 0
wantedExpireTime BIGINT DEFAULT 0
wantedReason VARCHAR(255) DEFAULT ''
```

**Features:**
- Survives server restarts
- Cross-session persistence
- Automatic cleanup on expiration

#### Offline Handling
- Wanted levels persist when offline
- Countdown continues while offline
- Notifications on reconnection

---

## 🛡️ **Guard Protection System**

### On-Duty Protection

#### Wanted Level Immunity
```java
// Guards on duty cannot be set as wanted
if (plugin.getDutyManager().isOnDuty(target)) {
    return false; // Prevent wanted level setting
}
```

**Protection Covers:**
- Manual admin commands
- Automatic system triggers
- Contraband violations
- Combat-based increases

#### Debug Logging
```yaml
# Debug output when protection triggers
DEBUG: Attempted to set wanted level on guard on duty: PlayerName
```

### Guard vs Guard Combat

#### Special Handling
```java
// Guard vs guard combat logging
if (killerIsGuard && victimIsGuard) {
    logger.warning("GUARD VS GUARD COMBAT: " + killerName + " vs " + victimName);
    // May or may not result in wanted level based on server policy
}
```

**Considerations:**
- Administrative review required
- Server policy dependent
- Detailed logging for investigations

---

## 🔄 **System Integration**

### Chase System Integration

#### Chase Prerequisites
```java
// Chase can only target wanted players
if (!plugin.getWantedManager().isWanted(target)) {
    sendMessage(guard, "chase.restrictions.target-not-wanted");
    return false;
}
```

**Requirements:**
- Target must have wanted level > 0
- Wanted level must not be expired
- Chase duration affects wanted level

#### Chase Outcomes
```java
// Successful capture clears wanted level
if (captureSuccessful) {
    plugin.getWantedManager().clearWantedLevel(target);
}

// Escape increases wanted level
if (chaseTimeout) {
    plugin.getWantedManager().increaseWantedLevel(target, 1, "Escaping from chase");
}
```

### Jail System Integration

#### Arrest Processing
```java
// Jail time calculation based on wanted level
int jailTime = baseTime + (wantedLevel * levelMultiplier);

// Clear wanted level after successful arrest
plugin.getWantedManager().clearWantedLevel(target);
```

**Process:**
1. Calculate jail time from wanted level
2. Process arrest with calculated time
3. Clear wanted level upon successful jail
4. Update statistics

### Contraband System Integration

#### Violation Processing
```java
// Contraband compliance failure
if (!compliant) {
    plugin.getWantedManager().increaseWantedLevel(target, 1, 
        "Contraband possession: " + contrabandType);
}
```

**Automatic Triggers:**
- Failed contraband surrender
- Positive drug test results
- Possession of prohibited items

---

## 📊 **Notification System**

### Player Notifications

#### Wanted Level Set
```yaml
messages:
  wanted:
    level:
      set: "<color:#FF6B6B>Your wanted level is now <level> <stars>!</color>"
      reason: "<color:#FFE066>Reason: <reason></color>"
```

#### Wanted Level Cleared
```yaml
messages:
  wanted:
    level:
      cleared: "<color:#51CF66>Your wanted level has been cleared!</color>"
```

#### Time Warnings
```yaml
messages:
  wanted:
    warnings:
      expiring: "<color:#FFE066>Your wanted level expires in <time>!</color>"
```

### Guard Notifications

#### Guard Alerts
```yaml
messages:
  wanted:
    alerts:
      player-wanted: "<color:#FF6B6B>ALERT: <player> is now wanted (<level> stars)</color>"
      guard-killed: "<color:#FF6B6B>GUARD DOWN: <player> killed guard <guard>!</color>"
```

#### Alert Distribution
```java
// Send to all on-duty guards
for (Player guard : onDutyGuards) {
    plugin.getMessageManager().sendGuardAlert(alertMessage, placeholders);
}
```

---

## 🔧 **Administrative Tools**

### Wanted Level Management

#### Set Wanted Level
```bash
/corrections wanted set <player> <level> [reason]
```

**Examples:**
```bash
# Set specific level
/corrections wanted set PlayerName 3 "Multiple violations"

# Maximum level
/corrections wanted set PlayerName 5 "Extreme criminal activity"

# Custom reason
/corrections wanted set PlayerName 2 "Assault on guard"
```

#### Clear Wanted Level
```bash
/corrections wanted clear <player>
```

**Usage:**
```bash
# Clear specific player
/corrections wanted clear PlayerName

# Result: Player's wanted level reset to 0
```

#### Check Wanted Status
```bash
/corrections wanted check <player>
```

**Information Displayed:**
- Current wanted level
- Remaining time
- Violation reason
- Star visualization

#### List Wanted Players
```bash
/corrections wanted list
```

**Output Format:**
```
=== Wanted Players ===
PlayerName - Level 3 (⭐⭐⭐) - 15m remaining
AnotherPlayer - Level 1 (⭐) - 28m remaining
```

---

## 🎮 **Player Experience**

### Wanted Player Mechanics

#### Movement Restrictions
```java
// Wanted players cannot teleport
if (plugin.getWantedManager().isWanted(player)) {
    event.setCancelled(true);
    sendMessage(player, "wanted.blocking.teleport");
}
```

**Blocked Actions:**
- Teleportation commands
- Home commands
- Spawn commands
- Warp commands

#### Combat Implications
```java
// Wanted players are chase targets
if (wantedLevel > 0) {
    // Can be chased by guards
    // Higher priority for law enforcement
    // Increased guard attention
}
```

### Redemption Mechanics

#### Natural Expiration
```java
// Automatic cleanup after duration
if (System.currentTimeMillis() >= expireTime) {
    clearWantedLevel(player);
    sendMessage(player, "wanted.level.expired");
}
```

#### Arrest Redemption
```java
// Cleared upon successful arrest
if (arrestSuccessful) {
    clearWantedLevel(player);
    sendMessage(player, "wanted.level.cleared-arrest");
}
```

---

## 📈 **Statistics & Analytics**

### Wanted Level Statistics

#### Individual Tracking
```sql
-- Player violation history
SELECT playerName, wantedLevel, wantedReason, wantedExpireTime
FROM edencorrections_players
WHERE wantedLevel > 0;
```

#### Server-wide Metrics
```sql
-- Average wanted level
SELECT AVG(wantedLevel) as avg_wanted_level
FROM edencorrections_players
WHERE wantedLevel > 0;

-- Most common violations
SELECT wantedReason, COUNT(*) as frequency
FROM edencorrections_players
WHERE wantedLevel > 0
GROUP BY wantedReason
ORDER BY frequency DESC;
```

### Performance Monitoring

#### System Efficiency
```java
// Monitor wanted level processing
- Average time from violation to wanted level
- Expiration cleanup efficiency
- Guard response times
```

#### Violation Trends
```java
// Analyze violation patterns
- Peak violation times
- Most common violation types
- Repeat offender identification
```

---

## 🔍 **PlaceholderAPI Integration**

### Available Placeholders

#### Wanted Level Information
```yaml
# Player's current wanted level
%edencorrections_wanted_level%

# Star visualization
%edencorrections_wanted_stars%

# Remaining time in seconds
%edencorrections_wanted_time%

# Violation reason
%edencorrections_wanted_reason%

# Active status (true/false)
%edencorrections_wanted_active%
```

#### Usage Examples
```yaml
# Scoreboard display
wanted_level: "Wanted: %edencorrections_wanted_stars%"
wanted_time: "Expires: %edencorrections_wanted_time%s"

# Chat prefix
wanted_prefix: "%edencorrections_wanted_active% ? '[WANTED]' : ''"
```

### Integration Examples

#### Scoreboard Integration
```yaml
# Using PlaceholderAPI with scoreboards
lines:
  - "Wanted Level: %edencorrections_wanted_level%"
  - "Stars: %edencorrections_wanted_stars%"
  - "Time Left: %edencorrections_wanted_time%s"
```

#### Chat Integration
```yaml
# Chat format with wanted status
format: "%edencorrections_wanted_active% ? '[⭐WANTED⭐] ' : ''%player%: %message%"
```

---

## 🛠️ **Configuration Options**

### Core Settings

#### Time Configuration
```yaml
times:
  wanted-duration: 1800  # 30 minutes default
```

#### Jail Integration
```yaml
jail:
  base-time: 300        # 5 minutes base
  level-multiplier: 60  # +1 minute per star
  max-wanted-level: 5   # Maximum stars
```

#### Escalation Settings
```yaml
wanted-system:
  auto-escalation: true    # Enable automatic increases
  max-escalation: 5        # Maximum automatic level
  escalation-cooldown: 300 # 5 minutes between auto-increases
```

### Advanced Configuration

#### Violation Weights
```yaml
violations:
  contraband: 1           # +1 star
  player-kill: 1          # +1 star
  guard-attack: 2         # +2 stars
  guard-kill: 3           # +3 stars
  chase-escape: 1         # +1 star
```

#### Protection Settings
```yaml
protection:
  guard-immunity: true    # Guards on duty cannot be wanted
  admin-immunity: false   # Admins can be wanted
  op-immunity: false      # OPs can be wanted
```

---

## 🔧 **Troubleshooting**

### Common Issues

#### Wanted Level Not Setting
**Symptoms**: Commands execute but no wanted level appears
**Solutions**:
1. Check guard protection (target on duty?)
2. Verify wanted level limits
3. Check database connectivity
4. Review debug logs

#### Wanted Level Not Expiring
**Symptoms**: Wanted levels persist beyond duration
**Solutions**:
1. Check system time synchronization
2. Verify cleanup task is running
3. Check database timestamps
4. Manual cleanup with admin commands

#### Guard Protection Not Working
**Symptoms**: Guards can be set as wanted
**Solutions**:
1. Verify guard is on duty
2. Check duty system functionality
3. Review guard rank detection
4. Check configuration settings

### Debug Tools

#### Debug Commands
```bash
# Check wanted status
/corrections wanted check PlayerName

# List all wanted players
/corrections wanted list

# Enable debug logging
/corrections system debug on

# Check system stats
/corrections system stats
```

#### Debug Output
```yaml
# Example debug messages
DEBUG: Setting wanted level for PlayerName to 3
DEBUG: Attempted to set wanted level on guard on duty: GuardName
DEBUG: Wanted level expired for PlayerName
DEBUG: Cleanup removed 5 expired wanted levels
```

---

## 📚 **Best Practices**

### For Administrators

#### Wanted Level Management
1. **Regular Monitoring** - Check wanted player lists regularly
2. **Balanced Escalation** - Don't make penalties too harsh
3. **Clear Policies** - Document violation consequences
4. **Guard Training** - Train guards on wanted system

#### Configuration Tips
1. **Reasonable Durations** - 30-60 minutes is typical
2. **Balanced Jail Times** - Scale with server activity
3. **Clear Messaging** - Customize violation messages
4. **Regular Cleanup** - Monitor database performance

### For Guards

#### Wanted Player Handling
1. **Priority Response** - Higher wanted levels = priority
2. **Proper Procedures** - Follow arrest protocols
3. **Team Coordination** - Coordinate with other guards
4. **Documentation** - Note violation reasons

#### Best Practices
1. **Check Wanted Lists** - Regular `/corrections wanted list`
2. **Understand Violations** - Know what triggers wanted levels
3. **Proper Arrests** - Follow full arrest procedures
4. **Guard Protection** - Stay on duty for immunity

---

## 🔗 **Related Systems**

### Integration Points

#### Chase System
- **Prerequisite**: Target must be wanted
- **Outcome**: Escape increases wanted level
- **Resolution**: Capture clears wanted level

#### Jail System
- **Calculation**: Jail time based on wanted level
- **Resolution**: Arrest clears wanted level
- **Statistics**: Tracks violation counts

#### Contraband System
- **Trigger**: Failed compliance increases wanted level
- **Detection**: Positive tests increase wanted level
- **Integration**: Automatic violation processing

### External Plugin Integration

#### PlaceholderAPI
- Real-time wanted status display
- Scoreboard integration
- Chat prefix support

#### WorldGuard
- Region-based wanted restrictions
- Safe zone protections
- Area-specific penalties

---

## 📚 **Related Documentation**

- [Configuration Guide](../Configuration.md#wanted-system) - Wanted system settings
- [Commands Reference](../Commands.md#administrative-commands) - Wanted management commands
- [Chase System](Chase-System.md) - Chase integration details
- [Jail System](Jail-System.md) - Arrest and jail mechanics

---

*The Wanted Level System provides the legal framework for EdenCorrections, creating meaningful consequences for violations while maintaining balance and redemption opportunities.* 