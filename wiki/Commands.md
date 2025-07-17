# 🔧 Commands Reference

Complete reference for all EdenCorrections commands, including usage, permissions, GUI integration examples, and NPC integration guides.

---

## 🎯 **Quick Command Index**

| Category | Commands | Primary Use Cases |
|----------|----------|------------------|
| **[Guard Duty](#-guard-duty-commands)** | `/duty` | Manage guard duty status and immobilization |
| **[Chase System](#-chase-system-commands)** | `/chase` | Initiate pursuits, capture suspects, end chases |
| **[Jail System](#-jail-system-commands)** | `/jail`, `/jailoffline` | Arrest online/offline players with calculated sentencing |
| **[Contraband](#-contraband-commands)** | `/sword`, `/bow`, `/armor`, `/drugs`, `/drugtest` | Search for weapons, drugs, armor violations |
| **[Banking](#-banking-commands)** | `/dutybank` | Convert duty time to server currency |
| **[Administrative](#-administrative-commands)** | `/corrections` | Complete admin control over all systems |
| **[Utility](#-utility-commands)** | `/tips`, `/edenreload` | Help and system management |

---

## 👮‍♂️ **Guard Duty Commands**

### `/duty`
Toggle guard duty status with immobilization period and region validation.

#### Basic Usage
```bash
/duty
```

#### How It Works
1. **Going On Duty**:
   - 5-second immobilization countdown (configurable)
   - LuckPerms rank detection (trainee → warden)
   - Region validation (must be in guard station)
   - Automatic kit distribution based on rank
   - Combat and wanted level checks

2. **Going Off Duty**:
   - Off-duty time validation
   - Inventory restoration to pre-duty state
   - Performance-based time off calculation
   - Combat and chase status checks

#### Requirements
- **LuckPerms Group**: Must be in one of the configured guard groups
- **Region**: Must be inside the designated duty region
- **Combat Status**: Cannot be in combat (5-second timer)
- **Wanted Status**: Cannot be wanted when going on duty
- **Off-Duty Time**: Must have earned time off (for going off duty)

#### Permissions
```yaml
edencorrections.guard              # Basic guard permission
edencorrections.guard.trainee      # Trainee rank access
edencorrections.guard.private      # Private rank access
edencorrections.guard.officer      # Officer rank access
edencorrections.guard.sergeant     # Sergeant rank access
edencorrections.guard.captain      # Captain rank access
edencorrections.guard.warden       # Warden rank access
```

#### GUI Integration Examples

**DeluxeMenus Duty Panel**:
```yaml
duty_toggle:
  slot: 13
  material: "%edencorrections_duty_active% == true ? GREEN_CONCRETE : RED_CONCRETE"
  name: "&f&lDuty Status: %edencorrections_duty_status%"
  lore:
    - "&7Current Rank: &e%edencorrections_duty_rank%"
    - "%edencorrections_duty_active% == true ? '&7Time on Duty: &a%edencorrections_duty_time%s' : '&7Off-Duty Time Available: &c%edencorrections_banking_time%s'"
    - ""
    - "&eClick to toggle duty status!"
  click_commands:
    - "[PLAYER] duty"
    - "[MESSAGE] &aDuty status toggled!"
    - "[REFRESH]"
```

**ChestCommands Integration**:
```yaml
duty-panel:
  NAME: '&9&lGuard Panel'
  ROWS: 3
  'duty-toggle':
    ID: '%edencorrections_duty_active% == true ? green_wool : red_wool'
    POSITION-X: 5
    POSITION-Y: 2
    NAME: '&fDuty: %edencorrections_duty_status%'
    LORE:
      - '&7Rank: &e%edencorrections_duty_rank%'
      - '&7Click to toggle!'
    COMMAND: 'duty'
```

#### NPC Integration Examples

**Citizens NPC Duty Station**:
```bash
# Create duty station NPC
/npc create DutyOfficer --type PLAYER
/npc skin DutyOfficer Steve

# Add click interaction
/npc rightclickcommand DutyOfficer add player duty
/npc rightclickcommand DutyOfficer add console say {player} is changing duty status

# Add hover message
/npc text DutyOfficer "Click to toggle duty status"
```

**MythicMobs Duty Station**:
```yaml
DutyStation:
  Type: ARMOR_STAND
  Display: '&9Duty Station'
  Options:
    Silent: true
    Despawn: false
  Skills:
  - command{c="duty"} @trigger ~onInteract
  - message{m="&9Processing duty change..."} @trigger ~onInteract
```

#### Troubleshooting Examples

**Common Issues and Solutions**:
```bash
# Player can't go on duty - check rank
/corrections system debug rank {player}

# Player stuck in immobilization - cancel transition
/corrections system debug duty {player}

# Region issues - verify player location
/rg info guard_station
```

---

## 🏃‍♂️ **Chase System Commands**

### `/chase <player|capture|end>`
Comprehensive chase management system with distance tracking and combat integration.

#### Starting a Chase
```bash
/chase <player>
```

**Requirements for Starting**:
- Guard must be on duty
- Target must have a wanted level (1-5 stars)
- Target cannot already be in a chase
- Guard cannot already be chasing someone
- Maximum concurrent chases not exceeded (default: 3)
- Target must be within maximum distance (default: 100 blocks)

**What Happens**:
1. Chase data is created and stored
2. Target receives notification of pursuit
3. All on-duty guards receive alert
4. Distance monitoring begins (every 5 seconds)
5. Combat timer integration activates

#### Capturing a Target
```bash
/chase capture
```

**Capture Requirements**:
- Guard must be within 3 blocks of target
- Neither player can be in combat (5-second timer)
- Guard must be the one actively chasing the target

**Capture Process**:
1. 10-second jail countdown begins
2. Target cannot move more than 1 block during countdown
3. Guard must stay within 5 blocks during countdown
4. Successful capture triggers jail system
5. Chase is automatically ended

#### Ending a Chase
```bash
/chase end
```

**Manual End Reasons**:
- Guard discretion
- Target escaped to safe zone
- Chase duration exceeded
- Administrative intervention

#### Permissions
```yaml
edencorrections.guard.chase        # Basic chase permission
edencorrections.guard.capture      # Capture permission (inherited)
edencorrections.admin.chase        # Admin chase management
```

#### Automatic Chase Termination

**Distance-Based Termination**:
- **Warning Distance**: 20 blocks (configurable)
- **Maximum Distance**: 100 blocks (configurable)
- Guards receive warnings when approaching limit

**Time-Based Termination**:
- **Maximum Duration**: 5 minutes (configurable)
- Automatic cleanup prevents infinite chases

**Safe Zone Termination**:
- WorldGuard integration
- Chases end when target enters no-chase zones
- Configurable region list

#### GUI Integration for Chase Management

**Chase Dashboard (DeluxeMenus)**:
```yaml
chase_dashboard:
  gui_title: "&c&lActive Chases"
  gui_size: 54
  
  items:
    chase_list:
      slot: 10-16
      material: PLAYER_HEAD
      name: "&c%chase_target_name%"
      lore:
        - "&7Chased by: &e%chase_guard_name%"
        - "&7Distance: &f%chase_distance% blocks"
        - "&7Duration: &a%chase_time%s"
        - "&7Target Wanted Level: %edencorrections_wanted_stars%"
        - ""
        - "&eClick to spectate chase!"
      click_commands:
        - "[CONSOLE] tp {player} %chase_target_name%"
        - "[MESSAGE] &aTeleporting to chase..."
    
    end_all_chases:
      slot: 49
      material: BARRIER
      name: "&c&lEnd All Chases"
      lore:
        - "&7Click to terminate all"
        - "&7active chases on the server"
        - ""
        - "&c&lADMIN ONLY"
      click_commands:
        - "[PLAYER] corrections chase endall"
      view_requirements:
        requirements:
          permission:
            permissions:
            - "edencorrections.admin"
```

#### NPC Integration for Chase System

**Guard Alert NPC (Citizens)**:
```bash
# Create alert system NPC
/npc create ChaseMonitor --type VILLAGER
/npc trait ChaseMonitor commandtrait

# Add chase monitoring
/npc commandtrait ChaseMonitor add server corrections chase list
/npc text ChaseMonitor "Active Chases: {chase_count}"
```

**Emergency Response NPC**:
```bash
# Create emergency response system
/npc create EmergencyResponse
/npc rightclickcommand EmergencyResponse add console broadcast &c[EMERGENCY] All guards report to active chase locations!
/npc rightclickcommand EmergencyResponse add server corrections chase list
```

#### Advanced Chase Examples

**Multi-Guard Coordination**:
```bash
# Multiple guards can coordinate
Guard1: /chase criminal123
Guard2: /tp criminal123          # Support the chase
Guard3: /corrections wanted check criminal123  # Verify violation
```

**Admin Chase Management**:
```bash
# View all active chases
/corrections chase list

# End specific chase
/corrections chase end guard_username

# Emergency end all chases
/corrections chase endall

# Debug chase system
/corrections system debug chase
```

---

## ⚖️ **Jail System Commands**

### `/jail <player> [reason]`
Arrest players with calculated sentencing based on wanted level.

#### Basic Usage
```bash
/jail <player>                           # Arrest with default reason
/jail <player> Assault on guard         # Arrest with specific reason
/jail prisoner123 "Contraband possession and resisting arrest"  # Multi-word reason
```

#### Sentencing Calculation
```yaml
# Formula: base_time + (wanted_level * level_multiplier)
base_time: 300 seconds        # 5 minutes base
level_multiplier: 60 seconds  # +1 minute per star

# Examples:
Wanted Level 1: 300 + (1 × 60) = 360 seconds (6 minutes)
Wanted Level 3: 300 + (3 × 60) = 480 seconds (8 minutes) 
Wanted Level 5: 300 + (5 × 60) = 600 seconds (10 minutes)
```

#### What Happens When Jailing
1. **Wanted Level Check**: System reads current wanted level
2. **Time Calculation**: Applies sentencing formula
3. **Statistics Update**: Increments guard arrests, prisoner violations
4. **Wanted Level Clear**: Removes wanted status
5. **Jail Execution**: Runs configured jail command
6. **Performance Award**: Guard receives arrest performance bonus

#### Permissions
```yaml
edencorrections.guard.jail         # Basic jail permission
edencorrections.guard.jail.reason  # Custom reason permission
```

#### Integration with Jail Plugins

**EssentialsX Jail Integration**:
```yaml
# config.yml setup
jail:
  command: "jail {player} {time} {reason}"
  
# Automatic command execution:
# /jail prisoner123 480 Arrested by GuardName - Assault on guard
```

**CMI Jail Integration**:
```yaml
jail:
  command: "cmi jail {player} {time}s {reason}"
```

**Custom Jail Plugin Integration**:
```yaml
jail:
  command: "customprison jail {player} -t {time} -r '{reason}'"
```

### `/jailoffline <player> [reason]`
Arrest players who are offline - they will be jailed when they log in.

#### Usage Examples
```bash
/jailoffline EscapedPrisoner        # Queue arrest for offline player
/jailoffline GrieferPlayer "Destroyed guard station while offline"
```

#### How Offline Jailing Works
1. **Player Data Storage**: Creates pending jail record in database
2. **Login Detection**: Monitors for player login
3. **Automatic Execution**: Applies jail on login
4. **Notification System**: Informs player of arrest reason

#### Permissions
```yaml
edencorrections.guard.admin        # Required for offline arrests
edencorrections.guard.jailoffline  # Specific offline jail permission
```

#### GUI Integration for Jail Management

**Arrest Interface (DeluxeMenus)**:
```yaml
arrest_panel:
  gui_title: "&c&lArrest Interface"
  gui_size: 27
  
  items:
    target_info:
      slot: 13
      material: PLAYER_HEAD
      skull: "{target_player}"
      name: "&c{target_player}"
      lore:
        - "&7Wanted Level: %edencorrections_wanted_level%"
        - "&7Wanted Stars: %edencorrections_wanted_stars%"
        - "&7Sentence Time: &e{calculated_time}s"
        - "&7Reason: &f%edencorrections_wanted_reason%"
        - ""
        - "&eClick to arrest!"
      click_commands:
        - "[PLAYER] jail {target_player}"
        - "[CLOSE]"
    
    quick_reasons:
      slots: [10, 11, 12, 14, 15, 16]
      materials: [IRON_SWORD, BOW, DIAMOND_CHESTPLATE, SUGAR, TNT, BARRIER]
      names:
        - "&cWeapon Violation"
        - "&cRanged Weapon"
        - "&cArmor Violation" 
        - "&cDrug Possession"
        - "&cDestruction"
        - "&cGeneral Violation"
      click_commands:
        - "[PLAYER] jail {target_player} Weapon violation"
        - "[PLAYER] jail {target_player} Ranged weapon violation"
        - "[PLAYER] jail {target_player} Armor violation"
        - "[PLAYER] jail {target_player} Drug possession"
        - "[PLAYER] jail {target_player} Destruction of property"
        - "[PLAYER] jail {target_player} General violation"
```

#### Advanced Jail Examples

**Mass Arrest System**:
```bash
# Arrest multiple players (admin command)
/corrections jail batch prisoner1,prisoner2,prisoner3 "Mass riot participation"
```

**Conditional Jailing with WorldGuard**:
```bash
# Only jail if in specific region
/rg flag prison_area jail-on-entry true
```

---

## 🔍 **Contraband Commands**

Comprehensive contraband detection system with compliance timers and performance tracking.

### `/sword <player>`
Search for bladed weapons and sharp objects.

#### Default Contraband Items
```yaml
swords:
  - WOODEN_SWORD, STONE_SWORD, IRON_SWORD
  - GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD
  - TRIDENT (if configured)
```

#### Usage Example
```bash
/sword prisoner123

# Process:
# 1. Guard initiates search request
# 2. Target receives 10-second compliance timer
# 3. Target must drop all sword items
# 4. Automatic violation if items remain after timer
# 5. Performance points awarded to guard
```

### `/bow <player>`
Search for ranged weapons and ammunition.

#### Default Items
```yaml
ranged_weapons:
  - BOW, CROSSBOW
  - ARROW, SPECTRAL_ARROW, TIPPED_ARROW (if configured)
```

### `/armor <player>`
Search for protective equipment violations.

#### Configurable Armor Detection
```yaml
armor_items:
  - All helmet types (LEATHER_HELMET, IRON_HELMET, etc.)
  - All chestplate types
  - All leggings types  
  - All boots types
  - SHIELD (if configured)
```

### `/drugs <player>`
Search for substance violations and illegal materials.

#### Default Drug Items
```yaml
drugs:
  - SUGAR (stimulants)
  - NETHER_WART (hallucinogens)
  - SPIDER_EYE (poisons)
  - FERMENTED_SPIDER_EYE (processed drugs)
  - BLAZE_POWDER (stimulants)
```

### `/drugtest <player>`
Instant drug detection without compliance period.

#### How Drug Testing Works
1. **Instant Scan**: Immediately checks inventory
2. **Detection Result**: Reports positive/negative
3. **Automatic Violation**: Increases wanted level if positive
4. **Performance Award**: Guard receives detection bonus

#### Usage Examples
```bash
/drugtest suspicious_player

# Positive Result Output:
# "Drug test POSITIVE for suspicious_player: SUGAR detected"
# "suspicious_player's wanted level increased to 2 stars"

# Negative Result Output:
# "Drug test NEGATIVE for suspicious_player"
```

#### Contraband System Permissions
```yaml
edencorrections.guard.contraband     # Basic contraband search
edencorrections.guard.contraband.sword   # Weapon searches
edencorrections.guard.contraband.bow     # Ranged weapon searches  
edencorrections.guard.contraband.armor   # Armor searches
edencorrections.guard.contraband.drugs   # Drug searches
edencorrections.guard.contraband.test    # Drug testing
```

#### Advanced Contraband Integration

**Automatic Contraband Detection (WorldGuard)**:
```bash
# Set up regions with automatic detection
/rg flag entrance contraband-scan true
/rg flag prison_entrance auto-search true
```

**GUI Contraband Interface**:
```yaml
contraband_scanner:
  gui_title: "&c&lContraband Scanner"
  gui_size: 45
  
  items:
    scan_weapons:
      slot: 10
      material: IRON_SWORD
      name: "&c&lWeapon Scan"
      lore:
        - "&7Scan for all bladed weapons"
        - "&7and sharp objects"
        - ""
        - "&eClick to scan {target_player}!"
      click_commands:
        - "[PLAYER] sword {target_player}"
        - "[MESSAGE] &cInitiating weapon scan..."
    
    scan_drugs:
      slot: 12
      material: SUGAR
      name: "&6&lDrug Test"
      lore:
        - "&7Instant detection of"
        - "&7illegal substances"
        - ""
        - "&eClick to test {target_player}!"
      click_commands:
        - "[PLAYER] drugtest {target_player}"
        - "[MESSAGE] &6Performing drug test..."
    
    full_search:
      slot: 22
      material: CHEST
      name: "&4&lFull Search"
      lore:
        - "&7Complete contraband search"
        - "&7All categories scanned"
        - ""
        - "&eClick for full search!"
      click_commands:
        - "[PLAYER] sword {target_player}"
        - "[DELAY] 1000"
        - "[PLAYER] bow {target_player}"
        - "[DELAY] 1000"
        - "[PLAYER] armor {target_player}"
        - "[DELAY] 1000"
        - "[PLAYER] drugtest {target_player}"
        - "[MESSAGE] &4Complete search initiated!"
```

**NPC Contraband Checkpoint**:
```bash
# Create checkpoint NPC
/npc create ContrabandCheckpoint --type VILLAGER
/npc skin ContrabandCheckpoint Guard

# Add proximity-based scanning
/npc commandtrait ContrabandCheckpoint add server drugtest {player}
/npc text ContrabandCheckpoint "Mandatory drug screening area"

# Add WorldGuard integration
/rg flag checkpoint_area entry-commands "drugtest {player}"
```

---

## 💰 **Banking Commands**

Convert accumulated duty time into server currency tokens.

### `/dutybank <convert|status>`
Manage duty time banking and currency conversion.

#### Banking Status
```bash
/dutybank status

# Example Output:
# "Total Duty Time: 2,847 seconds (47 minutes)"
# "Available for Conversion: 28 tokens"
# "Minimum Required: 300 seconds (5 minutes)"
# "Conversion Rate: 100 seconds = 1 token"
```

#### Converting Duty Time
```bash
/dutybank convert

# Process:
# 1. Validates minimum conversion time (300 seconds default)
# 2. Calculates available tokens (time ÷ conversion_rate)
# 3. Executes configured currency command
# 4. Deducts converted time from total
# 5. Confirms transaction to player
```

#### Banking Configuration Examples

**EconomyTokens Integration**:
```yaml
duty-banking:
  currency-command: "et give {player} {amount}"
  conversion-rate: 100  # 100 seconds = 1 token
```

**EssentialsX Economy Integration**:
```yaml
duty-banking:
  currency-command: "eco give {player} {amount}"
  conversion-rate: 60   # 60 seconds = $1
```

**Custom Token Plugin**:
```yaml
duty-banking:
  currency-command: "tokens give {player} {amount}"
  conversion-rate: 120  # 120 seconds = 1 token
```

#### Banking Permissions
```yaml
edencorrections.guard.banking      # Basic banking access
edencorrections.guard.banking.convert  # Conversion permission
edencorrections.guard.banking.status   # Status checking
```

#### GUI Banking Interface

**Banking Terminal (DeluxeMenus)**:
```yaml
banking_terminal:
  gui_title: "&6&lDuty Banking Terminal"
  gui_size: 27
  
  items:
    account_status:
      slot: 13
      material: GOLD_BLOCK
      name: "&6&lAccount Status"
      lore:
        - "&7Total Duty Time: &a%edencorrections_banking_time%s"
        - "&7Available Tokens: &e%edencorrections_banking_tokens%"
        - "&7Conversion Rate: &f100s = 1 token"
        - ""
        - "&eAccount in good standing"
      
    convert_button:
      slot: 15
      material: EMERALD
      name: "&a&lConvert Now"
      lore:
        - "&7Convert your duty time"
        - "&7into spendable tokens"
        - ""
        - "&aClick to convert!"
      click_commands:
        - "[PLAYER] dutybank convert"
        - "[MESSAGE] &aConversion processing..."
        - "[REFRESH]
      view_requirements:
        requirements:
          placeholder:
            placeholders:
            - "%edencorrections_banking_tokens% >= 1"
    
    insufficient_time:
      slot: 15
      material: REDSTONE_BLOCK
      name: "&c&lInsufficient Time"
      lore:
        - "&7You need at least 300 seconds"
        - "&7of duty time to convert"
        - ""
        - "&cKeep working to earn more!"
      view_requirements:
        requirements:
          placeholder:
            placeholders:
            - "%edencorrections_banking_tokens% < 1"
```

---

## 🛠️ **Administrative Commands**

Complete administrative control over all EdenCorrections systems.

### `/corrections <wanted|chase|duty|player|system|reload|help>`
Comprehensive admin interface for system management.

#### Wanted System Management

**Set Wanted Level**:
```bash
/corrections wanted set <player> <level> [reason]

# Examples:
/corrections wanted set prisoner123 3 "Assault on guard"
/corrections wanted set griefer456 5 "Maximum security threat"
/corrections wanted set newbie789 1 "Minor violation"
```

**Clear Wanted Level**:
```bash
/corrections wanted clear <player>

# Clears wanted status and resets timer
```

**Check Wanted Status**:
```bash
/corrections wanted check <player>

# Example Output:
# "prisoner123 - Level 3 (⭐⭐⭐)"
# "Time Remaining: 1,247 seconds (20m 47s)"
# "Reason: Assault on guard"
```

**List All Wanted Players**:
```bash
/corrections wanted list

# Example Output:
# "=== Wanted Players ==="
# "prisoner123 - Level 3 (⭐⭐⭐) - 20m 47s remaining"
# "griefer456 - Level 5 (⭐⭐⭐⭐⭐) - 28m 12s remaining"
# "Total: 2 wanted players"
```

#### Chase System Management

**List Active Chases**:
```bash
/corrections chase list

# Example Output:
# "=== Active Chases ==="
# "Guard: officer_smith -> Target: prisoner123 (2m 34s remaining)"
# "Guard: sergeant_jones -> Target: runner456 (4m 12s remaining)"
# "Total: 2 active chases"
```

**End Specific Chase**:
```bash
/corrections chase end <guard_username>
```

**End All Chases**:
```bash
/corrections chase endall
```

#### System Information

**System Statistics**:
```bash
/corrections system stats

# Example Output:
# "=== EdenCorrections Statistics ==="
# "Online Players: 45/100"
# "Guards On Duty: 8"
# "Active Chases: 2"
# "Wanted Players: 5"
# "Database Status: Connected (MySQL)"
# "Debug Mode: Disabled"
```

**Debug Information**:
```bash
/corrections system debug

# Shows detailed debug information
/corrections system debug rank <player>   # Check rank detection
/corrections system debug duty <player>    # Check duty status
/corrections system debug chase <player>   # Check chase status
```

#### Administrative Permissions
```yaml
edencorrections.admin              # Full administrative access
edencorrections.admin.wanted       # Wanted system management
edencorrections.admin.chase        # Chase system management  
edencorrections.admin.duty         # Duty system management
edencorrections.admin.system       # System information access
edencorrections.admin.reload       # Configuration reload
```

---

## 🔧 **Utility Commands**

### `/tips [category]`
Get helpful tips for guard effectiveness and system usage.

#### Usage Examples
```bash
/tips                    # General guard tips
/tips duty              # Duty system tips
/tips contraband        # Contraband search tips
/tips chase             # Chase system tips
/tips jail              # Jail system tips
/tips banking           # Banking system tips
```

#### Sample Tips Content
```yaml
# Duty system tips
duty-system:
  - "&7💡 &fStay in the guard station region when toggling duty"
  - "&7💡 &fEarn performance bonuses through successful arrests and searches"
  - "&7💡 &fYour inventory is automatically restored when going off duty"

# Contraband tips  
contraband-system:
  - "&7💡 &fGive players 10 seconds to voluntarily surrender contraband"
  - "&7💡 &fDrug tests are instant - no compliance period needed"
  - "&7💡 &fSuccessful searches earn you extra off-duty time"
```

### `/edenreload`
Reload EdenCorrections configuration without restarting the server.

#### Usage
```bash
/edenreload

# Process:
# 1. Reloads config.yml
# 2. Validates configuration
# 3. Updates message cache
# 4. Refreshes integration settings
# 5. Reports reload status
```

#### Permissions
```yaml
edencorrections.admin.reload       # Configuration reload permission
```

---

## 🎮 **GUI Integration Reference**

### DeluxeMenus Complete Example

**Main Guard Interface**:
```yaml
guard_hub:
  gui_title: "&9&lGuard Command Center"
  gui_size: 54
  
  items:
    # Duty Management
    duty_status:
      slot: 10
      material: "%edencorrections_duty_active% == true ? GREEN_WOOL : RED_WOOL"
      name: "&f&lDuty Status"
      lore:
        - "&7Status: %edencorrections_duty_status%"
        - "&7Rank: &e%edencorrections_duty_rank%"
        - "&7Time on Duty: &a%edencorrections_duty_time%s"
        - ""
        - "&eClick to toggle!"
      click_commands:
        - "[PLAYER] duty"
        - "[REFRESH]"
    
    # Wanted System
    wanted_board:
      slot: 12
      material: PAPER
      name: "&c&lWanted Board"
      lore:
        - "&7View all wanted players"
        - "&7and their threat levels"
        - ""
        - "&eClick to view!"
      click_commands:
        - "[PLAYER] corrections wanted list"
    
    # Chase Management
    chase_monitor:
      slot: 14
      material: COMPASS
      name: "&6&lChase Monitor"
      lore:
        - "&7Active Chases: &c%edencorrections_system_active_chases%"
        - "&7Monitor ongoing pursuits"
        - ""
        - "&eClick to view details!"
      click_commands:
        - "[PLAYER] corrections chase list"
    
    # Contraband Scanner
    contraband_tools:
      slot: 16
      material: CHEST
      name: "&4&lContraband Tools"
      lore:
        - "&7Access search tools"
        - "&7and detection equipment"
        - ""
        - "&eClick to open scanner!"
      click_commands:
        - "[OPEN] contraband_scanner"
    
    # Banking Terminal
    banking:
      slot: 28
      material: GOLD_INGOT
      name: "&6&lBanking"
      lore:
        - "&7Available: &e%edencorrections_banking_tokens% tokens"
        - "&7Duty Time: &a%edencorrections_banking_time%s"
        - ""
        - "&eClick to manage!"
      click_commands:
        - "[OPEN] banking_terminal"
    
    # Tips and Help
    help_desk:
      slot: 34
      material: BOOK
      name: "&b&lHelp Desk"
      lore:
        - "&7Get tips and guidance"
        - "&7for effective guard work"
        - ""
        - "&eClick for tips!"
      click_commands:
        - "[PLAYER] tips"
```

### ChestCommands Integration

**Guard Tools Panel**:
```yaml
guard-tools:
  NAME: '&9&lGuard Tools'
  ROWS: 6
  
  'duty-toggle':
    ID: '%edencorrections_duty_active% == true ? green_concrete : red_concrete'
    POSITION-X: 2
    POSITION-Y: 2
    NAME: '&fDuty: %edencorrections_duty_status%'
    LORE:
      - '&7Rank: &e%edencorrections_duty_rank%'
      - '&eClick to toggle!'
    COMMAND: 'duty'
  
  'weapon-search':
    ID: iron_sword
    POSITION-X: 4
    POSITION-Y: 2
    NAME: '&cWeapon Search'
    LORE:
      - '&7Search for contraband weapons'
      - '&eClick then type player name!'
    COMMAND: 'tellraw {player} {"text":"Type: /sword <player>","color":"yellow"}'
  
  'drug-test':
    ID: sugar
    POSITION-X: 6
    POSITION-Y: 2
    NAME: '&6Drug Test'
    LORE:
      - '&7Instant drug detection'
      - '&eClick then type player name!'
    COMMAND: 'tellraw {player} {"text":"Type: /drugtest <player>","color":"yellow"}'
```

---

## 🤖 **NPC Integration Reference**

### Citizens NPC Complete Examples

**Multi-Function Guard NPC**:
```bash
# Create the main NPC
/npc create GuardCaptain --type PLAYER
/npc skin GuardCaptain police_officer

# Add multiple interaction options
/npc rightclickcommand GuardCaptain add console say Welcome to the guard station, {player}!
/npc rightclickcommand GuardCaptain add player corrections wanted check {player}
/npc rightclickcommand GuardCaptain add console tellraw {player} {"text":"[Guard Captain] Your status has been verified.","color":"green"}

# Add text display
/npc text GuardCaptain add "Guard Captain Johnson" "Right-click for status check"

# Add equipment trait
/npc trait GuardCaptain equipmenttrait
/npc equipmenttrait GuardCaptain set HAND IRON_SWORD
/npc equipmenttrait GuardCaptain set CHESTPLATE IRON_CHESTPLATE
```

**Automated Checkpoint System**:
```bash
# Entry checkpoint
/npc create EntryCheckpoint --type VILLAGER
/npc lookclose EntryCheckpoint true
/npc text EntryCheckpoint "Automated Security Checkpoint"

# Add proximity commands
/npc commandtrait EntryCheckpoint add server drugtest {player}
/npc commandtrait EntryCheckpoint add console tellraw {player} {"text":"[Security] Scanning for contraband...","color":"red"}

# Exit checkpoint
/npc create ExitCheckpoint --type VILLAGER  
/npc commandtrait ExitCheckpoint add server corrections wanted check {player}
/npc commandtrait ExitCheckpoint add console tellraw {player} {"text":"[Security] Exit authorized.","color":"green"}
```

### MythicMobs Advanced Integration

**Interactive Guard Post**:
```yaml
GuardPost:
  Type: ARMOR_STAND
  Display: '&9&lGuard Post Terminal'
  Health: 1000
  Options:
    Silent: true
    Despawn: false
    Invulnerable: true
  Equipment:
  - IRON_CHESTPLATE:4
  - IRON_HELMET:3
  Skills:
  - message{m="&9[Terminal] &fAccessing guard systems..."} @trigger ~onInteract
  - delay 20
  - command{c="deluxemenus open guard_hub {trigger.name}"} @trigger ~onInteract
  - message{m="&9[Terminal] &aAccess granted."} @trigger ~onInteract
```

**Emergency Alert System**:
```yaml
EmergencyBeacon:
  Type: BEACON
  Display: '&c&lEMERGENCY ALERT'
  Options:
    Silent: false
    Despawn: false
  Skills:
  - message{m="&c[EMERGENCY] All guards report immediately!"} @NearbyPlayers{r=50}
  - command{c="corrections chase list"} @NearbyPlayers{r=50;permission=edencorrections.guard}
  - sound{s=entity.wither.spawn;v=1;p=1} @NearbyPlayers{r=100}
  - particles{p=redstone;a=100;vs=2;hs=2} @self
```

---

## 🔍 **Command Troubleshooting**

### Common Command Issues

#### Permission Problems
```bash
# Check player permissions
/lp user <player> permission check edencorrections.guard

# Add missing permissions
/lp user <player> permission set edencorrections.guard.contraband true

# Check group permissions
/lp group trainee permission info
```

#### Region Issues
```bash
# Verify regions exist
/rg list

# Check player location
/rg info guard_station

# Test region membership
/rg members guard_station add <player>
```

#### Database Problems
```bash
# Check database connection
/corrections system debug

# View database stats
/corrections system stats

# Manual data refresh
/edenreload
```

#### Integration Issues
```bash
# Test PlaceholderAPI
/papi parse <player> %edencorrections_duty_status%

# Reload PlaceholderAPI
/papi reload

# Check expansion status
/papi list
```

### Debug Commands for Admins

```bash
# General debugging
/corrections system debug                    # Overall system status
/corrections system debug rank <player>     # Rank detection issues
/corrections system debug duty <player>     # Duty system problems
/corrections system debug chase <player>    # Chase system debugging

# Performance monitoring
/corrections system stats                   # Performance statistics
/timings report                             # Server performance (if using Paper/Purpur)

# Database debugging
/corrections system debug database          # Database connection status
/corrections system debug cache             # Cache performance
```

---

## 📋 **Command Quick Reference Card**

### Essential Commands for Guards
```bash
/duty                           # Toggle duty status
/chase <player>                 # Start chase
/chase capture                  # Capture during chase
/jail <player> [reason]         # Arrest player
/sword <player>                 # Search for weapons
/drugtest <player>             # Drug test
/dutybank status               # Check banking
/tips                          # Get help
```

### Essential Commands for Admins
```bash
/corrections wanted list        # View all wanted
/corrections chase list         # View all chases
/corrections system stats       # System overview
/edenreload                    # Reload config
/corrections system debug      # Debug info
```

### Emergency Commands
```bash
/corrections chase endall       # Stop all chases
/corrections wanted clear <player>  # Clear wanted status
/duty                          # Force duty toggle
/corrections system debug      # Diagnose issues
```

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete Command Reference* 