# 👮‍♂️ Guard Duty System

Complete guide to the Guard Duty System, including ranks, performance tracking, and off-duty earning mechanics.

---

## 🎯 **System Overview**

The Guard Duty System is the core of EdenCorrections, providing a realistic guard experience with:

- **Rank-based Progression** - Six-tier hierarchy from Trainee to Warden
- **Performance Tracking** - Earn off-duty time through job performance
- **Inventory Management** - Automatic kit swapping and inventory protection
- **Region-based Controls** - Location-specific duty requirements
- **Combat Integration** - Seamless interaction with chase and arrest systems

---

## 📊 **Guard Rank Hierarchy**

### Rank Structure

| Rank | Level | Symbol | Description |
|------|-------|--------|-------------|
| **Trainee** | 1 | 🔰 | Entry-level guard position |
| **Private** | 2 | 🛡️ | Basic contraband detection |
| **Officer** | 3 | ⚔️ | Active law enforcement |
| **Sergeant** | 4 | 🎖️ | Team coordination |
| **Captain** | 5 | 👑 | Administrative oversight |
| **Warden** | 6 | 🏛️ | Full system control |

### Rank Progression

```
Trainee → Private → Officer → Sergeant → Captain → Warden
```

**Progression Requirements**:
- Manual promotion by administrators
- LuckPerms group assignment
- Performance-based recommendations

---

## 🔧 **Duty Toggle System**

### Going On Duty

#### Process Flow
1. **Location Check** - Must be in guard station region
2. **Rank Detection** - LuckPerms group validation
3. **Requirement Verification** - Combat, wanted status checks
4. **Immobilization Period** - 5-second countdown with movement restriction
5. **Activation** - Inventory stored, kit given, duty status activated

#### Requirements
- Valid guard rank in LuckPerms
- Must be in guard station region
- Cannot be in combat
- Cannot be wanted
- Cannot already be on duty

#### Command
```bash
/duty
```

### Going Off Duty

#### Process Flow
1. **Restriction Check** - Cannot be in chase or combat
2. **Time Validation** - Must have earned off-duty time
3. **Region Check** - Must be in valid off-duty region
4. **Deactivation** - Inventory restored, duty time calculated
5. **Statistics Update** - Performance metrics saved

#### Requirements
- Must be on duty
- Cannot be in active chase
- Cannot be in combat
- Must have earned off-duty time
- Must be in valid off-duty region

---

## 🎮 **Performance Tracking System**

### Performance Metrics

| Metric | Description | Bonus Trigger |
|--------|-------------|---------------|
| **Searches** | Contraband requests initiated | Every 10 searches |
| **Successful Searches** | Contraband found | Immediate bonus |
| **Arrests** | Completed jail processes | Immediate bonus |
| **Kills** | Combat eliminations | Every 5 kills |
| **Detections** | Positive drug tests | Immediate bonus |
| **Duty Time** | Continuous service | Hourly bonus |

### Performance Bonuses

#### Base Off-Duty Time
```yaml
# Default configuration
base-duty-requirement: 15    # minutes
base-off-duty-earned: 30     # minutes
```

**Earning Process**:
1. Guard goes on duty
2. Serves minimum 15 minutes
3. Automatically earns 30 minutes of off-duty time
4. Can go off duty and use earned time

#### Search Performance
```yaml
searches-per-bonus: 10       # searches needed
search-bonus-time: 5         # minutes earned
successful-search-bonus: 10  # minutes for positive result
```

**Example**:
- 10 contraband searches = 5 minutes bonus
- 1 successful search = 10 minutes bonus
- Total: 15 minutes additional off-duty time

#### Arrest Performance
```yaml
successful-arrest-bonus: 8   # minutes per arrest
```

**Triggers**:
- Completing jail countdown
- Successfully arresting wanted player
- Proper use of arrest procedures

#### Combat Performance
```yaml
kills-per-bonus: 5          # kills needed
kill-bonus-time: 15         # minutes earned
```

**Considerations**:
- Only counts legitimate guard vs criminal combat
- Guard vs guard combat doesn't count
- Must be on duty when kill occurs

#### Detection Performance
```yaml
successful-detection-bonus: 10  # minutes per detection
```

**Triggers**:
- Positive drug test results
- Successful contraband detection
- Proper use of detection tools

#### Time-Based Bonuses
```yaml
duty-time-bonus-rate: 2     # minutes per hour
```

**Automatic Earning**:
- 2 minutes of off-duty time per hour on duty
- Encourages longer duty sessions
- Stacks with performance bonuses

---

## 🛡️ **Inventory Management**

### Inventory Storage System

#### On Duty Activation
```java
// Automatic process when going on duty
1. Serialize current inventory
2. Store in database cache
3. Clear player inventory
4. Give appropriate guard kit
5. Apply duty restrictions
```

#### Off Duty Restoration
```java
// Automatic process when going off duty
1. Remove all guard kit items
2. Restore original inventory
3. Clear database cache
4. Remove duty restrictions
```

### Guard Kit System

#### Kit Configuration
```yaml
guard-system:
  kit-mappings:
    trainee: "trainee_kit"
    private: "private_kit"
    officer: "officer_kit"
    sergeant: "sergeant_kit"
    captain: "captain_kit"
    warden: "warden_kit"
```

#### Kit Integration
- **CMI Integration** - Automatic kit dispensing
- **Rank-based Kits** - Different equipment per rank
- **Automatic Cleanup** - Removes guard items when off duty

### Common Guard Kit Items
```yaml
# Automatically detected and managed
- DIAMOND_SWORD
- DIAMOND_HELMET
- DIAMOND_CHESTPLATE
- DIAMOND_LEGGINGS
- DIAMOND_BOOTS
- BOW
- ARROW
- GOLDEN_APPLE
- ENDER_PEARL
- POTION (various types)
```

---

## 🚫 **Duty Restrictions**

### On-Duty Restrictions

#### Block Interaction
```yaml
guard-restrictions:
  block-mining: true      # Cannot mine blocks
  block-crafting: true    # Cannot craft items
  block-storage: true     # Cannot access chests
```

#### Item Management
- **No Item Dropping** - Prevents kit loss (except contraband compliance)
- **No Trading** - Cannot trade with other players
- **No Inventory Sharing** - Cannot share items

#### Movement Restrictions
- **Duty Transition** - 5-second immobilization during activation
- **Region Requirements** - Must activate/deactivate in specific areas

### Combat Restrictions

#### Combat Timer Integration
```yaml
combat-timer:
  duration: 5                   # seconds
  prevent-capture: true         # cannot capture during combat
  prevent-teleport: true        # cannot teleport during combat
```

#### During Combat
- Cannot change duty status
- Cannot teleport
- Cannot use certain commands
- Visual indicators (boss bar, action bar)

---

## 🌍 **Region Integration**

### Required Regions

#### Guard Station
```yaml
guard-system:
  duty-region: "guard_station"
```

**Usage**:
- Required for duty activation
- Must be properly configured in WorldGuard
- Access controlled by guard permissions

#### Duty-Required Zones
```yaml
regions:
  duty-required-zones: "guard_lockers,guard_lockers2,guardplotstairs"
```

**Behavior**:
- Only on-duty guards can access
- Automatic restriction enforcement
- Configurable region list

#### Off-Duty Regions
**Valid Locations**:
- Guard station (same as on-duty)
- Any duty-required zone
- Configurable safe zones

### Region Configuration

#### WorldGuard Setup
```bash
# Create guard station
/rg define guard_station

# Set permissions
/rg flag guard_station entry -g guards allow
/rg flag guard_station entry deny

# Set additional flags
/rg flag guard_station pvp allow
/rg flag guard_station use allow
```

#### Integration Benefits
- **Automatic Enforcement** - Plugin handles region checking
- **Flexible Configuration** - Multiple valid regions
- **Security** - Prevents unauthorized access

---

## 📈 **Off-Duty Earning System**

### Time Management

#### Earning Mechanics
1. **Base Time** - Earned after minimum duty period
2. **Performance Bonuses** - Additional time for good performance
3. **Continuous Duty** - Hourly bonuses for extended service
4. **Accumulation** - Times stack and accumulate

#### Time Consumption
1. **Usage Rate** - 1:1 ratio (1 minute earned = 1 minute off duty)
2. **Automatic Tracking** - System monitors off-duty time usage
3. **Expiration Warnings** - Notifications when time runs low
4. **Forced Return** - Must return to duty when time expires

### Advanced Features

#### Auto-Notification System
```yaml
# Automatic notifications
- Base time earned: "You earned 30 minutes of off-duty time!"
- Performance bonus: "Performance bonus: +10 minutes (successful search)"
- Time expiring: "Your off-duty time has expired!"
- Must return: "You must return to duty to continue playing as a guard."
```

#### Grace Period
- **5-minute grace** - Extra time when off-duty expires
- **Prevents immediate kick** - Allows time to return to duty
- **Warning system** - Clear notifications about status

---

## 🔄 **Duty Workflows**

### Standard Duty Session

#### Starting Duty
```bash
1. /tips duty                    # Check duty tips
2. Go to guard station          # Enter duty region
3. /duty                        # Activate duty
4. Wait for countdown           # 5-second immobilization
5. Receive kit and confirmation # Ready for duty
```

#### During Duty
```bash
1. /corrections wanted list     # Check wanted players
2. Patrol and search           # Perform guard duties
3. Monitor performance         # Track bonuses earned
4. Respond to alerts           # Handle incidents
```

#### Ending Duty
```bash
1. Complete active duties      # Finish chases/arrests
2. Return to valid region      # Guard station or duty zone
3. /duty                       # Deactivate duty
4. Inventory restored          # Original items returned
5. /dutybank status            # Check banking options
```

### Performance Optimization

#### Maximizing Off-Duty Time
1. **Complete Minimum Duty** - Earn base 30 minutes
2. **Perform Searches** - 10 searches = 5 minutes
3. **Find Contraband** - Each success = 10 minutes
4. **Make Arrests** - Each arrest = 8 minutes
5. **Stay On Duty** - 2 minutes per hour

#### Example Session
```yaml
# 2-hour duty session
Base Time: 30 minutes          # Minimum duty completed
Search Bonus: 10 minutes       # 20 searches (2 bonuses)
Success Bonus: 30 minutes      # 3 successful searches
Arrest Bonus: 16 minutes       # 2 arrests
Time Bonus: 4 minutes          # 2 hours continuous duty
Total Earned: 90 minutes       # 1.5 hours off-duty time
```

---

## 🎯 **Best Practices**

### For Guards

#### Duty Management
1. **Regular Duty** - Maintain consistent duty schedules
2. **Performance Focus** - Actively search and arrest
3. **Team Coordination** - Work with other guards
4. **Region Awareness** - Stay in appropriate areas

#### Performance Tips
1. **Search Actively** - Look for contraband opportunities
2. **Arrest Properly** - Use full arrest procedures
3. **Stay Alert** - Monitor wanted player lists
4. **Continuous Service** - Longer sessions = more bonuses

### For Administrators

#### Setup Best Practices
1. **Proper Regions** - Configure all required regions
2. **Balanced Kits** - Appropriate equipment per rank
3. **Reasonable Timers** - Don't make transitions too long
4. **Clear Messaging** - Customize messages for your server

#### Performance Tuning
```yaml
# Recommended settings for different server sizes
Small Server (1-50):
  base-duty-requirement: 10
  base-off-duty-earned: 20
  
Medium Server (51-100):
  base-duty-requirement: 15
  base-off-duty-earned: 30
  
Large Server (100+):
  base-duty-requirement: 20
  base-off-duty-earned: 45
```

---

## 🔍 **Troubleshooting**

### Common Issues

#### Cannot Go On Duty
**Symptoms**: `/duty` command fails
**Solutions**:
1. Check guard rank in LuckPerms
2. Verify region configuration
3. Ensure not in combat/wanted
4. Check debug output

#### Inventory Issues
**Symptoms**: Items lost or duplicated
**Solutions**:
1. Check inventory serialization
2. Verify database connectivity
3. Ensure clean plugin shutdown
4. Use backup/restore features

#### Performance Not Tracking
**Symptoms**: No bonuses earned
**Solutions**:
1. Verify on-duty status
2. Check performance thresholds
3. Ensure proper command usage
4. Review debug logs

### Debug Commands
```bash
# Check duty status
/corrections duty list

# Debug specific player
/corrections system debug rank PlayerName

# System statistics
/corrections system stats

# Enable debug logging
/corrections system debug on
```

---

## 📊 **Statistics & Analytics**

### Performance Metrics

#### Individual Statistics
- Total duty time
- Off-duty time earned
- Searches performed
- Arrests made
- Performance bonuses earned

#### Server-wide Statistics
- Average duty time per guard
- Most active guards
- Performance trends
- System efficiency

### Monitoring Tools

#### Built-in Commands
```bash
/corrections duty list          # Active duty status
/corrections system stats       # System overview
/dutybank status               # Banking information
```

#### Database Queries
```sql
-- Most active guards
SELECT playerName, totalDutyTime 
FROM edencorrections_players 
ORDER BY totalDutyTime DESC;

-- Performance trends
SELECT AVG(sessionSearches) as avg_searches 
FROM edencorrections_players 
WHERE isOnDuty = 1;
```

---

## 🔗 **Integration Points**

### Other EdenCorrections Systems

#### Chase System
- Duty requirement for chase initiation
- Performance bonuses for successful captures
- Automatic duty validation

#### Contraband System
- Duty requirement for searches
- Performance tracking for searches
- Automatic statistics updates

#### Jail System
- Duty requirement for arrests
- Performance bonuses for successful arrests
- Automatic violation tracking

### External Plugins

#### LuckPerms Integration
- Rank detection and validation
- Permission-based restrictions
- Group-based kit assignment

#### WorldGuard Integration
- Region-based duty controls
- Automatic area restrictions
- Location validation

#### CMI Integration
- Automatic kit dispensing
- Inventory management
- Command integration

---

## 📚 **Related Documentation**

- [Configuration Guide](../Configuration.md#guard-system) - Guard system settings
- [Commands Reference](../Commands.md#guard-duty-commands) - Duty commands
- [Permissions System](../Permissions.md#guard-permissions) - Guard permissions
- [Troubleshooting](../Troubleshooting.md) - Common issues and solutions

---

*The Guard Duty System is the foundation of EdenCorrections, providing realistic guard mechanics with performance-based rewards and comprehensive inventory management.* 