# 🏃‍♂️ Chase & Combat System

Complete guide to the Chase System, including pursuit mechanics, combat timers, and capture procedures.

---

## 🎯 **System Overview**

The Chase System provides dynamic pursuit mechanics with:

- **Distance-based Tracking** - Automatic monitoring and warnings
- **Combat Timer Integration** - Fair capture prevention during combat
- **Multi-chase Support** - Handle multiple simultaneous pursuits
- **Safe Zone Integration** - WorldGuard region respect
- **Performance Tracking** - Bonuses for successful captures

---

## 🏃‍♂️ **Chase Mechanics**

### Chase Initiation

#### Prerequisites
```java
// Requirements for starting a chase
1. Guard must be on duty
2. Target must be wanted (level > 0)
3. Guard must be within max chase distance
4. Cannot exceed max concurrent chases
5. Guard cannot already be chasing someone
6. Target cannot already be chased
```

#### Command Usage
```bash
# Start chase with wanted player
/chase PlayerName

# Must be within initial distance
# Automatic validation of requirements
```

#### Process Flow
1. **Validation** - Check all prerequisites
2. **Initialization** - Create chase data entry
3. **Player Updates** - Set chase status for both players
4. **Notifications** - Alert guard and target
5. **Monitoring** - Start distance tracking

### Chase Monitoring

#### Distance Tracking
```yaml
chase:
  max-distance: 100      # Maximum chase distance
  warning-distance: 20   # Warning threshold
  max-concurrent: 3      # Server-wide chase limit
```

#### Automatic Monitoring
```java
// Every 5 seconds (configurable)
1. Check player locations
2. Calculate distance between guard and target
3. Send warnings if approaching limit
4. End chase if distance exceeded
5. Update chase statistics
```

#### Warning System
```java
// Distance warnings to guard
if (distance > warningDistance) {
    sendMessage(guard, "chase.warnings.distance", distancePlaceholder("distance", distance));
}
```

### Chase Termination

#### Automatic Termination
```java
// Chase ends automatically when:
1. Distance exceeds maximum limit
2. Chase duration expires
3. Either player disconnects
4. Target enters safe zone
5. Guard goes off duty
```

#### Manual Termination
```bash
# Guard can end chase manually
/chase end

# Admin can end specific chase
/corrections chase end GuardName

# Admin can end all chases
/corrections chase endall
```

---

## ⚔️ **Combat Timer System**

### Combat Detection

#### Trigger Events
```java
// Combat timer starts when:
1. Player damages another player
2. Player takes damage from another player
3. Both players get combat timer
4. Duration resets on new combat
```

#### Configuration
```yaml
combat-timer:
  duration: 5                   # seconds
  prevent-capture: true         # block capture during combat
  prevent-teleport: true        # block teleportation
```

### Combat Restrictions

#### Capture Prevention
```java
// Cannot capture during combat
if (isInCombat(target) || isInCombat(guard)) {
    sendMessage(guard, "chase.restrictions.combat-timer-active");
    return false;
}
```

#### Teleportation Blocking
```java
// Block teleportation during combat
if (isInCombat(player)) {
    event.setCancelled(true);
    sendMessage(player, "combat.teleport-blocked");
}
```

#### Visual Indicators
```java
// Boss bar shows combat timer
showCountdownBossBar(player, "bossbar.combat-timer", 
    BossBar.Color.RED, BossBar.Overlay.PROGRESS, duration);

// Action bar notification
sendActionBar(player, "actionbar.combat-active");
```

### Combat Resolution

#### Timer Expiration
```java
// Combat timer ends automatically
1. Countdown reaches zero
2. Player disconnects
3. Player dies
4. Manual cancellation
```

#### Cleanup Process
```java
// When combat ends:
1. Remove combat timer data
2. Cancel countdown task
3. Hide boss bar
4. Send end message
5. Allow restricted actions
```

---

## 🎯 **Capture Mechanics**

### Capture Requirements

#### Distance Validation
```java
// Must be within capture range
if (distance > 3.0) {
    sendMessage(guard, "chase.restrictions.capture-too-far");
    return false;
}
```

#### Combat Check
```java
// Cannot capture during combat
if (isInCombat(target) || isInCombat(guard)) {
    sendMessage(guard, "chase.restrictions.combat-timer-active");
    return false;
}
```

#### Chase Validation
```java
// Must be actively chasing target
ChaseData chase = getChaseByGuard(guard.getUniqueId());
if (chase == null || !chase.getTargetId().equals(target.getUniqueId())) {
    sendMessage(guard, "chase.restrictions.not-chasing-target");
    return false;
}
```

### Capture Process

#### Command Usage
```bash
# Capture during active chase
/chase capture

# Must be within 3 blocks of target
# Automatic validation and processing
```

#### Process Flow
1. **Validation** - Check all capture requirements
2. **Chase End** - Terminate active chase
3. **Jail Start** - Begin jail countdown process
4. **Notifications** - Alert both players
5. **Statistics** - Update performance metrics

#### Success Handling
```java
// Successful capture results
1. End active chase
2. Start jail countdown
3. Award performance bonus
4. Clear wanted level (after jail)
5. Update statistics
```

---

## 🌍 **Region Integration**

### Safe Zone Protection

#### No-Chase Zones
```yaml
regions:
  no-chase-zones: "safezone,visitor_area,medical_bay"
```

#### Behavior
```java
// Chase ends when target enters safe zone
if (isPlayerInSafeZone(target)) {
    endChase(chaseId, "Target entered safe zone");
    sendMessage(guard, "chase.end.safe-zone");
}
```

#### Zone Types
- **Safe Zones** - Complete chase immunity
- **Visitor Areas** - Civilian protection zones
- **Medical Bays** - Emergency protection areas
- **Admin Zones** - Staff-only areas

### Region Configuration

#### WorldGuard Setup
```bash
# Create safe zone
/rg define safezone

# Set chase immunity
/rg flag safezone custom-flag chase-immunity allow

# Configure region priorities
/rg priority safezone 10
```

#### Integration Benefits
- **Automatic Detection** - Plugin checks regions continuously
- **Flexible Configuration** - Multiple safe zone types
- **Fair Gameplay** - Balanced safe areas

---

## 📊 **Performance Tracking**

### Chase Statistics

#### Individual Metrics
```java
// Per-player tracking
- Total chases initiated
- Successful captures
- Failed chases
- Average chase duration
- Capture efficiency rating
```

#### Server-wide Metrics
```java
// System-wide statistics
- Active chases count
- Average chase duration
- Success rate
- Most chased players
- Top performing guards
```

### Performance Bonuses

#### Successful Capture
```yaml
# Configuration
successful-arrest-bonus: 8  # minutes off-duty time
```

#### Bonus Triggers
```java
// Performance bonus awarded for:
1. Successful chase completion
2. Proper capture procedure
3. Timely arrest processing
4. Following protocols
```

---

## 🔧 **Administrative Tools**

### Chase Management

#### List Active Chases
```bash
/corrections chase list
```

**Output Format:**
```
=== Active Chases ===
GuardName chasing TargetName (Distance: 45m, Time: 2m30s)
OtherGuard chasing Criminal (Distance: 12m, Time: 1m15s)
```

#### End Specific Chase
```bash
/corrections chase end GuardName
```

**Usage:**
- Terminates chase by specific guard
- Provides end reason to participants
- Updates statistics

#### End All Chases
```bash
/corrections chase endall
```

**Emergency Usage:**
- Terminates all active chases
- Useful for server events
- Provides mass notifications

### Debug Tools

#### Chase Debugging
```bash
# Enable debug mode
/corrections system debug on

# Check specific chase
/corrections chase list

# Monitor distance tracking
# Debug output shows real-time chase data
```

---

## 🎮 **Player Experience**

### Guard Perspective

#### Chase Initiation
```java
// Guard workflow
1. Check wanted player list
2. Locate wanted player
3. Get within chase range
4. Execute /chase command
5. Monitor distance warnings
6. Pursue target
7. Capture when close enough
```

#### Visual Feedback
```java
// Guard receives:
- Distance warnings
- Chase status updates
- Capture opportunity alerts
- Performance notifications
```

### Target Perspective

#### Chase Awareness
```java
// Target receives:
- Chase start notification
- Periodic status updates
- Safe zone information
- Combat timer alerts
```

#### Evasion Strategies
```java
// Target can:
- Flee to safe zones
- Use combat timer strategically
- Coordinate with other players
- Attempt to lose pursuit
```

---

## 🔄 **System Workflows**

### Standard Chase Workflow

#### Guard Initiation
```bash
1. /corrections wanted list      # Check wanted players
2. Locate wanted player         # Find target
3. Approach within range        # Get close enough
4. /chase PlayerName           # Start chase
5. Monitor warnings            # Watch distance
6. Pursue target               # Active pursuit
7. /chase capture              # When close enough
8. /jail PlayerName "reason"   # Complete arrest
```

#### Target Response
```bash
1. Receive chase notification   # Alert of pursuit
2. Assess situation            # Check options
3. Flee or fight              # Choose strategy
4. Use safe zones             # Seek protection
5. Manage combat timer        # Avoid capture
```

### Emergency Procedures

#### Chase Cleanup
```bash
# If chase system issues occur
1. /corrections chase list      # Check active chases
2. /corrections chase endall    # End all chases
3. /corrections system debug on # Enable debugging
4. /edenreload                 # Reload if needed
```

---

## ⚙️ **Configuration Options**

### Core Settings

#### Distance Configuration
```yaml
chase:
  max-distance: 100      # Maximum chase distance (blocks)
  warning-distance: 20   # Warning threshold (blocks)
  max-concurrent: 3      # Maximum simultaneous chases
```

#### Time Configuration
```yaml
times:
  chase-duration: 300    # Maximum chase time (seconds)
```

#### Combat Timer
```yaml
combat-timer:
  duration: 5                   # Combat timer duration
  prevent-capture: true         # Block capture during combat
  prevent-teleport: true        # Block teleportation
```

### Advanced Settings

#### Performance Monitoring
```yaml
performance:
  chase-check-interval: 5       # Distance check frequency
```

#### Regional Settings
```yaml
regions:
  no-chase-zones: "safezone,visitor_area,medical_bay"
```

---

## 🔍 **Troubleshooting**

### Common Issues

#### Chase Won't Start
**Symptoms**: `/chase` command fails
**Solutions**:
1. Check guard is on duty
2. Verify target is wanted
3. Check distance to target
4. Verify not at concurrent limit
5. Check target not already chased

#### Chase Ends Immediately
**Symptoms**: Chase terminates right after start
**Solutions**:
1. Check max distance setting
2. Verify both players online
3. Check safe zone configuration
4. Review debug logs

#### Combat Timer Issues
**Symptoms**: Combat timer not working correctly
**Solutions**:
1. Check combat timer configuration
2. Verify damage events triggering
3. Check boss bar display
4. Review combat restrictions

### Debug Commands

#### Chase Debugging
```bash
# Monitor chase system
/corrections chase list

# Debug specific issues
/corrections system debug on

# Check system statistics
/corrections system stats
```

#### Debug Output
```yaml
# Example debug messages
DEBUG: Chase started: GuardName -> TargetName
DEBUG: Chase distance warning: 85m (limit: 100m)
DEBUG: Chase ended: Target too far away
DEBUG: Combat timer started for PlayerName (5s)
```

---

## 📈 **Performance Optimization**

### Server Performance

#### Monitoring Frequency
```yaml
# Optimize based on server size
Small Server:
  chase-check-interval: 10    # Check every 10 seconds
  
Medium Server:
  chase-check-interval: 5     # Check every 5 seconds
  
Large Server:
  chase-check-interval: 2     # Check every 2 seconds
```

#### Concurrent Limits
```yaml
# Adjust based on server capacity
Small Server:
  max-concurrent: 2          # 2 simultaneous chases
  
Medium Server:
  max-concurrent: 5          # 5 simultaneous chases
  
Large Server:
  max-concurrent: 10         # 10 simultaneous chases
```

### Memory Management

#### Automatic Cleanup
```java
// System automatically cleans up:
1. Expired chase data
2. Disconnected player chases
3. Invalid chase states
4. Combat timer data
```

---

## 🔗 **Integration Points**

### Internal Systems

#### Wanted System
- **Prerequisite**: Target must be wanted
- **Outcome**: Escape increases wanted level
- **Resolution**: Capture clears wanted level

#### Jail System
- **Transition**: Successful capture starts jail process
- **Integration**: Seamless capture-to-jail workflow
- **Statistics**: Tracks arrest performance

#### Guard System
- **Requirement**: Must be on duty to chase
- **Performance**: Bonuses for successful captures
- **Restrictions**: Duty-based access control

### External Plugins

#### WorldGuard
- Region-based chase restrictions
- Safe zone protections
- Area-specific rules

#### PlaceholderAPI
- Real-time chase status
- Distance displays
- Combat timer integration

---

## 📚 **Related Documentation**

- [Configuration Guide](../Configuration.md#chase-system) - Chase system settings
- [Commands Reference](../Commands.md#chase-system-commands) - Chase commands
- [Wanted System](Wanted-System.md) - Wanted level integration
- [Jail System](Jail-System.md) - Arrest procedures

---

*The Chase System provides dynamic pursuit mechanics that create engaging gameplay while maintaining fair and balanced law enforcement procedures.* 