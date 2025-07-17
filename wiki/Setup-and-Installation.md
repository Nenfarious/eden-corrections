# 📦 Setup & Installation

This comprehensive guide will walk you through setting up EdenCorrections on your Minecraft server, from basic installation to advanced configuration and integration with other plugins.

---

## 🔧 **Prerequisites & System Requirements**

### Minimum Requirements
| Component | Requirement | Recommended |
|-----------|-------------|-------------|
| **Minecraft Server** | 1.19.4+ | 1.20.4+ |
| **Java Version** | 17+ | 21+ |
| **RAM** | 512MB | 2GB+ |
| **CPU** | 1 core | 2+ cores |
| **Storage** | 50MB | 100MB+ |

### Required Dependencies
These plugins **must** be installed for EdenCorrections to function:

#### 🔐 **LuckPerms** (Required)
- **Purpose**: Guard rank management and permission handling
- **Download**: [LuckPerms Official](https://luckperms.net/download)
- **Why Required**: EdenCorrections uses LuckPerms groups to detect guard ranks
- **Installation**: Place in plugins folder, restart server

#### 🛡️ **WorldGuard** (Required)
- **Purpose**: Region-based restrictions and safe zones
- **Download**: [WorldGuard Official](https://dev.bukkit.org/projects/worldguard)
- **Why Required**: Duty regions, safe zones, and chase restrictions
- **Dependencies**: Also requires WorldEdit

### Optional Dependencies
These plugins enhance EdenCorrections functionality:

#### 🎯 **PlaceholderAPI** (Highly Recommended)
- **Purpose**: 50+ placeholders for integration with other plugins
- **Download**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
- **Benefits**: GUI integration, scoreboard display, chat formatting
- **Installation**: Install plugin + `/papi ecloud download EdenCorrections`

#### 🎒 **CMI** (Optional)
- **Purpose**: Guard kit management
- **Alternative**: Any kit plugin that supports console commands
- **Integration**: Automatic kit giving based on guard rank

#### 💰 **Economy Plugin** (Optional)
- **Purpose**: Duty banking currency integration
- **Supported**: Any plugin that accepts console commands
- **Examples**: EssentialsX Economy, TokenManager, CoinsEngine

---

## 💾 **Step-by-Step Installation**

### Step 1: Prepare Your Server

```bash
# 1. Stop your server completely
screen -r minecraft  # or however you access your server
/stop

# 2. Backup your server (IMPORTANT!)
cd /path/to/server
tar -czf backup-$(date +%Y%m%d).tar.gz world/ plugins/ *.yml *.properties

# 3. Update Java if needed (Ubuntu/Debian example)
sudo apt update
sudo apt install openjdk-21-jre-headless
```

### Step 2: Install Dependencies

#### Download Required Plugins
```bash
# Create a temporary download directory
mkdir temp-downloads
cd temp-downloads

# Download URLs (get latest versions from official sources):
# LuckPerms: https://luckperms.net/download
# WorldGuard: https://dev.bukkit.org/projects/worldguard
# WorldEdit: https://dev.bukkit.org/projects/worldedit
# PlaceholderAPI: https://www.spigotmc.org/resources/placeholderapi.6245/
```

#### Install Core Dependencies
```bash
# Move to your plugins directory
cd ../plugins/

# Place the following files:
# - LuckPerms-Bukkit-5.x.x.jar
# - worldguard-bukkit-7.x.x.jar
# - worldedit-bukkit-7.x.x.jar
# - PlaceholderAPI-2.x.x.jar (optional but recommended)
```

### Step 3: First-Time Server Start

```bash
# Start your server to generate default configs
cd ..
java -Xmx2G -Xms1G -jar server.jar nogui

# Wait for "Done! For help, type "help"" message
# Then stop the server:
/stop
```

### Step 4: Install EdenCorrections

```bash
# Place EdenCorrections-2.0.0.jar in plugins/ folder
# Start server again
java -Xmx2G -Xms1G -jar server.jar nogui
```

---

## ⚙️ **Initial Configuration**

### LuckPerms Guard Ranks Setup

```bash
# Connect to your server and run these LuckPerms commands:

# Create guard groups (in order of hierarchy)
/lp creategroup trainee
/lp creategroup private
/lp creategroup officer
/lp creategroup sergeant
/lp creategroup captain
/lp creategroup warden

# Set guard permissions (each rank inherits from lower ranks)
/lp group trainee permission set edencorrections.guard.trainee true
/lp group private permission set edencorrections.guard.private true
/lp group officer permission set edencorrections.guard.officer true
/lp group sergeant permission set edencorrections.guard.sergeant true
/lp group captain permission set edencorrections.guard.captain true
/lp group warden permission set edencorrections.guard.warden true

# Add additional permissions for higher ranks
/lp group sergeant permission set edencorrections.guard.admin true
/lp group captain permission set edencorrections.guard.admin true
/lp group warden permission set edencorrections.guard.admin true

# Set group inheritance (optional but recommended)
/lp group private parent set trainee
/lp group officer parent set private
/lp group sergeant parent set officer
/lp group captain parent set sergeant
/lp group warden parent set captain
```

### WorldGuard Region Setup

```bash
# Create essential regions for EdenCorrections

# 1. Guard Station (required for going on/off duty)
/rg define guard_station
/rg flag guard_station pvp deny
/rg flag guard_station mob-spawning deny

# 2. Safe zones (no chases allowed)
/rg define safezone
/rg flag safezone pvp deny
/rg flag safezone edencorrections-chase deny

# 3. Prisoner areas (where chases can occur)
/rg define prisoner_area
/rg flag prisoner_area pvp allow

# 4. Additional duty-required zones (guard lockers, etc.)
/rg define guard_lockers
/rg define guardplotstairs
/rg define guard_lockers2
```

### EdenCorrections Configuration

Edit `plugins/EdenCorrections/config.yml`:

```yaml
# === BASIC CONFIGURATION ===
debug: false
language: en

# === GUARD SYSTEM ===
guard-system:
  duty-region: "guard_station"
  immobilization-time: 5  # Seconds to stand still when going on duty
  
  # Map LuckPerms groups to EdenCorrections ranks
  rank-mappings:
    trainee: "trainee"
    private: "private"
    officer: "officer"
    sergeant: "sergeant"
    captain: "captain"
    warden: "warden"
  
  # Map ranks to CMI kit names
  kit-mappings:
    trainee: "guard_trainee"
    private: "guard_private"
    officer: "guard_officer"
    sergeant: "guard_sergeant"
    captain: "guard_captain"
    warden: "guard_warden"

# === TIMING CONFIGURATION ===
times:
  duty-transition: 10      # Time to wait before duty changes
  chase-duration: 300      # Max chase time (5 minutes)
  wanted-duration: 1800    # Wanted level expiry (30 minutes)
  jail-countdown: 10       # Arrest countdown time
  contraband-compliance: 10 # Time to surrender contraband

# === CHASE SYSTEM ===
chase:
  max-distance: 100        # Max blocks before chase ends
  warning-distance: 20     # Distance to warn about chase ending
  max-concurrent: 3        # Max simultaneous chases

# === JAIL SYSTEM ===
jail:
  base-time: 300          # Base jail time (5 minutes)
  level-multiplier: 60    # Extra time per wanted star (1 minute)
  max-wanted-level: 5     # Maximum wanted stars

# === DUTY BANKING ===
duty-banking:
  enabled: true
  conversion-rate: 100    # 100 seconds = 1 token
  minimum-conversion: 300 # 5 minutes minimum
  currency-command: "et give {player} {amount}"  # EconomyTokens example

# === CONTRABAND SYSTEM ===
contraband:
  enabled: true
  max-request-distance: 5
  grace-period: 3
  
  types:
    sword:
      items: "WOODEN_SWORD,STONE_SWORD,IRON_SWORD,GOLDEN_SWORD,DIAMOND_SWORD,NETHERITE_SWORD"
      description: "All swords and bladed weapons"
    bow:
      items: "BOW,CROSSBOW"
      description: "All ranged weapons"
    armor:
      items: "LEATHER_HELMET,LEATHER_CHESTPLATE,CHAINMAIL_HELMET,IRON_HELMET,DIAMOND_HELMET,NETHERITE_HELMET"
      description: "All armor pieces"
    drugs:
      items: "SUGAR,NETHER_WART,SPIDER_EYE,FERMENTED_SPIDER_EYE,BLAZE_POWDER"
      description: "Illegal substances and drugs"

# === REGIONS ===
regions:
  no-chase-zones: "safezone,spawn"
  duty-required-zones: "guard_lockers,guard_lockers2,guardplotstairs"
```

---

## 🎮 **Integration Examples**

### DeluxeMenus GUI Integration

Create `plugins/DeluxeMenus/gui/guard_panel.yml`:

```yaml
gui_title: "&9&lGuard Control Panel"
gui_size: 54

items:
  duty_status:
    slot: 22
    material: "%edencorrections_duty_active% == true ? GREEN_WOOL : RED_WOOL"
    name: "&f&lDuty Status"
    lore:
      - "&7Current Status: %edencorrections_duty_status%"
      - "&7Rank: &e%edencorrections_duty_rank%"
      - "%edencorrections_duty_active% == true ? '&7Time on Duty: &a%edencorrections_duty_time%s' : '&7Available Off-Duty Time: &c%edencorrections_banking_time%s'"
      - ""
      - "&eClick to toggle duty!"
    click_commands:
      - "[PLAYER] duty"
      - "[CLOSE]"

  wanted_board:
    slot: 24
    material: PAPER
    name: "&c&lWanted Players"
    lore:
      - "&7Click to view current"
      - "&7wanted players on the server"
    click_commands:
      - "[PLAYER] corrections wanted list"

  banking:
    slot: 20
    material: GOLD_INGOT
    name: "&6&lDuty Banking"
    lore:
      - "&7Available Tokens: &e%edencorrections_banking_tokens%"
      - "&7Total Duty Time: &a%edencorrections_banking_time%s"
      - ""
      - "&eClick to convert duty time!"
    click_commands:
      - "[PLAYER] dutybank convert"
      - "[MESSAGE] &aConversion attempted!"

  tips:
    slot: 40
    material: BOOK
    name: "&b&lGuard Tips"
    lore:
      - "&7Get helpful tips for"
      - "&7being an effective guard"
    click_commands:
      - "[PLAYER] tips duty"
```

### Citizens NPC Integration

```bash
# Create a Guard Checkpoint NPC
/npc create GuardCheckpoint --type PLAYER
/npc skin GuardCheckpoint Notch

# Add interactive commands
/npc command add GuardCheckpoint server corrections wanted check {player}
/npc command add GuardCheckpoint player say Checking your status, {player}...

# Add right-click action
/npc rightclickcommand GuardCheckpoint add server say Welcome to the checkpoint, {player}
/npc rightclickcommand GuardCheckpoint add server contraband-check {player}

# Create a Duty Station NPC
/npc create DutyStation --type PLAYER
/npc command add DutyStation player duty
/npc command add DutyStation console say {player} is changing duty status
```

### MythicMobs Integration

Create `plugins/MythicMobs/Mobs/GuardNPC.yml`:

```yaml
GuardNPC:
  Type: VILLAGER
  Display: '&9Guard Captain'
  Health: 100
  Options:
    Silent: true
    Despawn: false
  Skills:
  - command{c="corrections wanted check <target.name>"} @trigger ~onInteract
  - message{m="&cHalt! Let me check your status..."} @trigger ~onInteract
```

### Scoreboard Integration (with FeatherBoard)

Edit `plugins/FeatherBoard/animations.yml`:

```yaml
eden_guard_info:
  - "&9&l⚔ Guard Status ⚔"
  - "&7Duty: %edencorrections_duty_status%"
  - "&7Rank: &e%edencorrections_duty_rank%"
  - "&7Active Chases: &c%edencorrections_system_active_chases%"
  - ""
  - "&9&l⭐ Wanted System ⭐"
  - "&7Your Level: %edencorrections_wanted_stars%"
  - "&7Time Left: &c%edencorrections_wanted_time%s"
  - ""
  - "&9&l💰 Banking"
  - "&7Available: &a%edencorrections_banking_tokens% tokens"
```

---

## 🔧 **Advanced Configuration**

### Database Configuration

#### SQLite (Default)
```yaml
database:
  type: sqlite
  sqlite:
    file: "edencorrections.db"
```

#### MySQL (Recommended for Networks)
```yaml
database:
  type: mysql
  mysql:
    host: "localhost"
    port: 3306
    database: "edencorrections"
    username: "eden_user"
    password: "secure_password"
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
```

### Performance Tuning

```yaml
performance:
  chase-check-interval: 5      # Seconds between chase distance checks
  wanted-check-interval: 60    # Seconds between wanted expiry checks
  cleanup-interval: 300        # Seconds between database cleanup
  cache-player-data: true      # Cache player data for performance
  async-operations: true       # Use async database operations
```

### Multi-World Support

```yaml
worlds:
  enabled-worlds:
    - "world"
    - "prison_world"
    - "guard_world"
  disabled-worlds:
    - "creative"
    - "lobby"
```

---

## 🚨 **Troubleshooting Common Issues**

### Issue 1: "No valid guard rank" Error

**Problem**: Players can't go on duty even with permissions
**Causes**:
- LuckPerms groups not set up correctly
- Rank mappings in config don't match LuckPerms groups
- Player not in any guard group

**Solutions**:
```bash
# Check player's groups
/lp user <player> info

# Check if groups exist
/lp listgroups

# Add player to guard group
/lp user <player> parent add trainee

# Verify config.yml rank mappings match your LuckPerms groups
# Debug with:
/corrections system debug rank <player>
```

### Issue 2: WorldGuard Regions Not Working

**Problem**: Duty system doesn't respect regions
**Causes**:
- WorldGuard not installed properly
- Regions not defined
- Region names in config don't match actual regions

**Solutions**:
```bash
# Check if WorldGuard is loaded
/plugins

# List regions in current world
/rg list

# Check specific region
/rg info guard_station

# Verify config.yml region names match exactly
# Case-sensitive: "Guard_Station" ≠ "guard_station"
```

### Issue 3: Database Connection Issues

**Problem**: "Failed to connect to database" errors
**Solutions**:

For SQLite:
```bash
# Check file permissions
ls -la plugins/EdenCorrections/
chmod 644 plugins/EdenCorrections/edencorrections.db

# Check disk space
df -h
```

For MySQL:
```bash
# Test connection manually
mysql -h localhost -u eden_user -p edencorrections

# Check if user has permissions
GRANT ALL ON edencorrections.* TO 'eden_user'@'localhost';
FLUSH PRIVILEGES;
```

### Issue 4: PlaceholderAPI Integration

**Problem**: Placeholders show as raw text
**Solutions**:
```bash
# Install PlaceholderAPI expansion
/papi ecloud download EdenCorrections
/papi reload

# Check if expansion is loaded
/papi list
# Should show "EdenCorrections" in green

# Test placeholder
/papi parse <player> %edencorrections_duty_status%
```

### Issue 5: Performance Issues

**Problem**: Server lag with EdenCorrections
**Solutions**:

1. **Increase check intervals**:
```yaml
performance:
  chase-check-interval: 10     # Increase from 5
  wanted-check-interval: 120   # Increase from 60
  cleanup-interval: 600        # Increase from 300
```

2. **Enable debug mode temporarily**:
```yaml
debug: true
```
Check console for performance warnings, then disable debug.

3. **Database optimization**:
```bash
# For MySQL, optimize tables
OPTIMIZE TABLE eden_player_data;
OPTIMIZE TABLE eden_chase_data;
```

---

## 📊 **Verification & Testing**

### Post-Installation Checklist

#### ✅ **Basic Functionality**
```bash
# 1. Check plugin loaded
/plugins
# Should show "EdenCorrections v2.0.0" in green

# 2. Test basic commands
/corrections system stats
/duty
/tips

# 3. Check database connection
/corrections system debug
```

#### ✅ **Guard System**
```bash
# 1. Add yourself to guard group
/lp user <your_name> parent add trainee

# 2. Go to guard station region
/rg info guard_station
# Should show you're in the region

# 3. Test duty toggle
/duty
# Should show immobilization countdown
```

#### ✅ **Integration Testing**
```bash
# 1. Test PlaceholderAPI
/papi parse <player> %edencorrections_duty_status%
# Should return "On Duty" or "Off Duty"

# 2. Test WorldGuard integration
/rg flag guard_station greeting "Welcome to Guard Station"
# Enter region, should see message

# 3. Test economy integration (if configured)
/dutybank status
# Should show duty time and available tokens
```

### Load Testing Commands

```bash
# Test with multiple guards
/lp user guard1 parent add officer
/lp user guard2 parent add sergeant
/lp user guard3 parent add captain

# Simulate wanted players
/corrections wanted set prisoner1 3 Testing system
/corrections wanted set prisoner2 5 Maximum wanted level

# Test chase system
/chase prisoner1
# Then test capture and distance limits
```

---

## 🔄 **Maintenance & Updates**

### Regular Maintenance Tasks

#### Weekly Tasks
```bash
# 1. Check database size
ls -lh plugins/EdenCorrections/edencorrections.db

# 2. Review performance stats
/corrections system stats

# 3. Clean up old data (if using MySQL)
# Backup first!
mysqldump edencorrections > backup_$(date +%Y%m%d).sql
```

#### Monthly Tasks
```bash
# 1. Update dependencies
# Check for new versions of LuckPerms, WorldGuard, PlaceholderAPI

# 2. Review configuration
# Check for new config options in updates

# 3. Performance analysis
# Enable debug mode briefly to check for issues
```

### Update Procedure

```bash
# 1. Always backup first!
/stop
cp -r plugins/EdenCorrections/ plugins/EdenCorrections_backup_$(date +%Y%m%d)/

# 2. Download new version
# Place new jar in plugins/

# 3. Check config.yml for new options
# Compare with default config

# 4. Start server and verify
java -Xmx2G -Xms1G -jar server.jar nogui

# 5. Test all major features
/duty
/chase <player>
/corrections system stats
```

---

## 🌐 **Multi-Server Setup (Networks)**

### BungeeCord/Velocity Configuration

#### Central Database Setup
```yaml
# config.yml on all servers
database:
  type: mysql
  mysql:
    host: "central-db.yournetwork.com"
    database: "edencorrections_network"
    # Use same database for all servers
```

#### Per-Server Configuration
```yaml
# Unique region names per server
guard-system:
  duty-region: "server1_guard_station"  # server1
  duty-region: "server2_guard_station"  # server2

# Shared rank mappings
rank-mappings:
  # Same on all servers
  trainee: "trainee"
  # etc.
```

### Cross-Server Features

#### Shared Wanted System
- Wanted levels persist across servers
- Guards on any server can see wanted players
- Chase system respects server boundaries

#### Network-Wide Statistics
```bash
# View network-wide stats
/corrections system stats network

# Cross-server player lookup
/corrections player check <username>
```

---

## 🎯 **Best Practices**

### Security Recommendations

1. **Permission Isolation**:
```bash
# Don't give edencorrections.admin to regular guards
/lp group trainee permission unset edencorrections.admin
/lp group warden permission set edencorrections.admin true
```

2. **Region Security**:
```bash
# Protect guard areas
/rg flag guard_station entry -g nonguards deny
/rg flag guard_lockers use -g nonguards deny
```

3. **Database Security**:
```yaml
# Use strong passwords for MySQL
mysql:
  password: "ComplexPassword123!@#"
  
# Limit database user permissions
GRANT SELECT,INSERT,UPDATE,DELETE ON edencorrections.* TO 'eden_user'@'localhost';
```

### Performance Optimization

1. **Configure Check Intervals Based on Server Size**:
```yaml
# Small servers (< 50 players)
chase-check-interval: 5
wanted-check-interval: 60

# Medium servers (50-200 players)  
chase-check-interval: 10
wanted-check-interval: 120

# Large servers (200+ players)
chase-check-interval: 15
wanted-check-interval: 180
```

2. **Database Optimization**:
```sql
-- MySQL optimization
ALTER TABLE eden_player_data ADD INDEX idx_player_id (player_id);
ALTER TABLE eden_chase_data ADD INDEX idx_active (is_active);
```

3. **Memory Management**:
```bash
# Increase heap size for large servers
java -Xmx4G -Xms2G -XX:+UseG1GC -jar server.jar nogui
```

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete Installation Guide* 