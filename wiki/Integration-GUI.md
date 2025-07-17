# 🎮 GUI Integration Guide

Complete guide for integrating EdenCorrections with GUI plugins, featuring real-world examples and advanced configurations.

---

## 🎯 **Overview**

EdenCorrections provides 50+ PlaceholderAPI placeholders and full GUI integration capabilities, allowing you to create sophisticated graphical interfaces for guard management, prisoner monitoring, and administrative control.

### Supported GUI Plugins
| Plugin | Status | Integration Level | Best For |
|--------|--------|------------------|----------|
| **DeluxeMenus** | ✅ Full Support | Advanced | Complex interfaces, conditional displays |
| **ChestCommands** | ✅ Full Support | Standard | Simple tool panels, quick actions |
| **TrMenu** | ✅ Compatible | Advanced | Modern interfaces, animations |
| **BossShopPro** | ✅ Compatible | Basic | Shop-style interfaces |
| **InventoryGUI** | ✅ Compatible | Basic | Simple command GUIs |

---

## 🚀 **Quick Start Examples**

### Basic Guard Panel (DeluxeMenus)
```yaml
# File: plugins/DeluxeMenus/gui/guard_basic.yml
gui_title: "&9&lGuard Panel"
gui_size: 27

items:
  duty_toggle:
    slot: 13
    material: "%edencorrections_duty_active% == true ? GREEN_WOOL : RED_WOOL"
    name: "&fDuty: %edencorrections_duty_status%"
    lore:
      - "&7Rank: &e%edencorrections_duty_rank%"
      - "&eClick to toggle!"
    click_commands:
      - "[PLAYER] duty"
      - "[REFRESH]"
  
  wanted_check:
    slot: 15
    material: PAPER
    name: "&cWanted Players"
    lore:
      - "&7Check current wanted list"
    click_commands:
      - "[PLAYER] corrections wanted list"
```

### Basic Tool Panel (ChestCommands)
```yaml
# File: plugins/ChestCommands/menu/guard_tools.yml
name: '&9Guard Tools'
rows: 3

'duty-status':
  ID: '%edencorrections_duty_active% == true ? green_concrete : red_concrete'
  POSITION-X: 2
  POSITION-Y: 2
  NAME: '&fDuty: %edencorrections_duty_status%'
  COMMAND: 'duty'

'contraband-scan':
  ID: chest
  POSITION-X: 6
  POSITION-Y: 2
  NAME: '&cContraband Scanner'
  COMMAND: 'tellraw {player} {"text":"Use: /sword <player>","color":"red"}'
```

---

## 🔧 **DeluxeMenus Integration**

### Complete Guard Command Center

**Main Interface** (`guard_center.yml`):
```yaml
gui_title: "&9&l⚔ Guard Command Center ⚔"
gui_size: 54
open_commands:
  - "guardpanel"
  - "guards"

# Global requirements - only guards can access
view_requirements:
  requirements:
    permission:
      permissions:
      - "edencorrections.guard"

items:
  # === TOP ROW: STATUS INDICATORS ===
  server_status:
    slot: 4
    material: EMERALD_BLOCK
    name: "&a&lServer Status"
    lore:
      - "&7Online Players: &f%server_online%/%server_max%"
      - "&7Active Guards: &a%edencorrections_system_guards_online%"
      - "&7Active Chases: &c%edencorrections_system_active_chases%"
      - "&7Wanted Players: &e%edencorrections_system_wanted_count%"
      - ""
      - "&aAll systems operational"

  # === SECOND ROW: DUTY MANAGEMENT ===
  duty_management:
    slot: 19
    material: "%edencorrections_duty_active% == true ? DIAMOND_CHESTPLATE : LEATHER_CHESTPLATE"
    name: "&f&lDuty Management"
    lore:
      - "&7Current Status: %edencorrections_duty_status%"
      - "&7Guard Rank: &e%edencorrections_duty_rank%"
      - ""
      - "%edencorrections_duty_active% == true ? '&7⏰ Time on Duty: &a%edencorrections_duty_time%s' : '&7⏰ Off-Duty Time: &c%edencorrections_banking_time%s'"
      - ""
      - "&eClick to toggle duty status!"
    click_commands:
      - "[PLAYER] duty"
      - "[MESSAGE] &9Processing duty change..."
      - "[DELAY] 2000"
      - "[REFRESH]"
    view_requirements:
      requirements:
        string equals:
          input: "%edencorrections_duty_region_check%"
          output: "true"
        deny commands:
          - "[MESSAGE] &cYou must be in the guard station!"

  # === THIRD ROW: CORE FUNCTIONS ===
  wanted_system:
    slot: 20
    material: PAPER
    name: "&c&lWanted System"
    lore:
      - "&7Monitor and manage"
      - "&7wanted players"
      - ""
      - "&7Current Wanted: &c%edencorrections_system_wanted_count%"
      - ""
      - "&eClick to view wanted board!"
    click_commands:
      - "[OPEN] wanted_board"

  chase_system:
    slot: 21
    material: COMPASS
    name: "&6&lChase Monitor"
    lore:
      - "&7Track active pursuits"
      - "&7and coordinate responses"
      - ""
      - "&7Active Chases: &c%edencorrections_system_active_chases%"
      - ""
      - "&eClick to view chase board!"
    click_commands:
      - "[OPEN] chase_monitor"

  contraband_tools:
    slot: 22
    material: CHEST
    name: "&4&lContraband Tools"
    lore:
      - "&7Search and detection"
      - "&7equipment access"
      - ""
      - "&7Today's Searches: &a%edencorrections_player_searches%"
      - "&7Successful: &e%edencorrections_player_successful_searches%"
      - ""
      - "&eClick to open scanner!"
    click_commands:
      - "[OPEN] contraband_scanner"

  jail_system:
    slot: 23
    material: IRON_BARS
    name: "&8&lJail System"
    lore:
      - "&7Arrest and sentencing"
      - "&7management"
      - ""
      - "&7Your Arrests: &a%edencorrections_player_arrests%"
      - ""
      - "&eClick for arrest tools!"
    click_commands:
      - "[OPEN] arrest_interface"

  banking_system:
    slot: 24
    material: GOLD_INGOT
    name: "&6&lDuty Banking"
    lore:
      - "&7Convert duty time"
      - "&7to server currency"
      - ""
      - "&7Available Tokens: &e%edencorrections_banking_tokens%"
      - "&7Conversion Rate: &f100s = 1 token"
      - ""
      - "&eClick to manage banking!"
    click_commands:
      - "[OPEN] banking_terminal"

  # === BOTTOM ROW: UTILITIES ===
  help_system:
    slot: 40
    material: BOOK
    name: "&b&lHelp & Tips"
    lore:
      - "&7Get guidance for"
      - "&7effective guard work"
      - ""
      - "&eClick for tips menu!"
    click_commands:
      - "[OPEN] tips_menu"

  admin_panel:
    slot: 42
    material: REDSTONE_BLOCK
    name: "&c&lAdmin Panel"
    lore:
      - "&7Administrative tools"
      - "&7and system management"
      - ""
      - "&c&lRESTRICTED ACCESS"
    click_commands:
      - "[OPEN] admin_interface"
    view_requirements:
      requirements:
        permission:
          permissions:
          - "edencorrections.admin"

  # === NAVIGATION ===
  close_menu:
    slot: 49
    material: BARRIER
    name: "&c&lClose Menu"
    lore:
      - "&7Close this interface"
    click_commands:
      - "[CLOSE]"

  refresh_menu:
    slot: 45
    material: LIME_DYE
    name: "&a&lRefresh"
    lore:
      - "&7Update information"
    click_commands:
      - "[REFRESH]"
      - "[MESSAGE] &aInterface refreshed!"
```

### Wanted Player Board

**Wanted Board Interface** (`wanted_board.yml`):
```yaml
gui_title: "&c&l⭐ Wanted Player Board ⭐"
gui_size: 54

items:
  # Dynamic wanted player list (slots 10-43)
  wanted_player_template:
    slots: 
      - 10
      - 11
      - 12
      - 13
      - 14
      - 15
      - 16
      - 19
      - 20
      - 21
      - 22
      - 23
      - 24
      - 25
      - 28
      - 29
      - 30
      - 31
      - 32
      - 33
      - 34
      - 37
      - 38
      - 39
      - 40
      - 41
      - 42
      - 43
    material: PLAYER_HEAD
    skull: "%wanted_player_name%"
    name: "&c%wanted_player_name%"
    lore:
      - "&7Wanted Level: &c%wanted_level%"
      - "&7Stars: %wanted_stars%"
      - "&7Time Remaining: &e%wanted_time_remaining%s"
      - "&7Reason: &f%wanted_reason%"
      - ""
      - "&7Last Seen: &a%wanted_last_seen%"
      - "&7Location: &e%wanted_last_location%"
      - ""
      - "&eLeft Click: Start Chase"
      - "&cRight Click: Admin Options"
    left_click_commands:
      - "[PLAYER] chase %wanted_player_name%"
      - "[MESSAGE] &aInitiating chase with %wanted_player_name%!"
      - "[CLOSE]"
    right_click_commands:
      - "[OPEN] wanted_admin_menu"
      - "[SET] target_player %wanted_player_name%"
    view_requirements:
      requirements:
        placeholder:
          placeholders:
          - "%wanted_player_online% == true"

  # Header information
  board_header:
    slot: 4
    material: PAPER
    name: "&c&lWanted Board Status"
    lore:
      - "&7Total Wanted: &c%edencorrections_system_wanted_count%"
      - "&7Most Wanted: &4%most_wanted_player%"
      - "&7Average Level: &e%average_wanted_level%"
      - ""
      - "&7Board Updates: Every 30 seconds"

  # Navigation
  back_button:
    slot: 45
    material: ARROW
    name: "&7← Back to Guard Center"
    click_commands:
      - "[OPEN] guard_center"

  refresh_board:
    slot: 49
    material: CLOCK
    name: "&a&lRefresh Board"
    lore:
      - "&7Update wanted player list"
    click_commands:
      - "[REFRESH]"
      - "[MESSAGE] &aWanted board refreshed!"
```

### Contraband Scanner Interface

**Contraband Scanner** (`contraband_scanner.yml`):
```yaml
gui_title: "&4&l🔍 Contraband Scanner 🔍"
gui_size: 45

items:
  # Target selection
  target_display:
    slot: 4
    material: PLAYER_HEAD
    skull: "%target_player%"
    name: "&f&lTarget: %target_player%"
    lore:
      - "&7Status: %target_status%"
      - "&7Distance: &e%target_distance% blocks"
      - "&7Last Scan: %last_scan_time%"
      - ""
      - "&7Click to change target"
    click_commands:
      - "[CLOSE]"
      - "[MESSAGE] &eType the name of the player to scan:"

  # Scan Types
  weapon_scan:
    slot: 19
    material: IRON_SWORD
    name: "&c&lWeapon Scan"
    lore:
      - "&7Detect bladed weapons"
      - "&7and sharp objects"
      - ""
      - "&7Items: Swords, Axes, Tridents"
      - "&7Compliance: 10 seconds"
      - ""
      - "&eClick to initiate scan!"
    click_commands:
      - "[PLAYER] sword %target_player%"
      - "[MESSAGE] &cWeapon scan initiated on %target_player%"
      - "[CLOSE]"
    view_requirements:
      requirements:
        placeholder:
          placeholders:
          - "%target_player% != null"

  ranged_scan:
    slot: 20
    material: BOW
    name: "&6&lRanged Weapon Scan"
    lore:
      - "&7Detect ranged weapons"
      - "&7and ammunition"
      - ""
      - "&7Items: Bows, Crossbows, Arrows"
      - "&7Compliance: 10 seconds"
      - ""
      - "&eClick to initiate scan!"
    click_commands:
      - "[PLAYER] bow %target_player%"
      - "[MESSAGE] &6Ranged weapon scan initiated on %target_player%"
      - "[CLOSE]"

  armor_scan:
    slot: 21
    material: DIAMOND_CHESTPLATE
    name: "&b&lArmor Scan"
    lore:
      - "&7Detect protective equipment"
      - "&7violations"
      - ""
      - "&7Items: All armor types"
      - "&7Compliance: 10 seconds"
      - ""
      - "&eClick to initiate scan!"
    click_commands:
      - "[PLAYER] armor %target_player%"
      - "[MESSAGE] &bArmor scan initiated on %target_player%"
      - "[CLOSE]"

  drug_test:
    slot: 23
    material: SUGAR
    name: "&d&lDrug Test"
    lore:
      - "&7Instant substance detection"
      - "&7No compliance period"
      - ""
      - "&7Items: Sugar, Nether Wart, Eyes"
      - "&7Result: Immediate"
      - ""
      - "&eClick to test!"
    click_commands:
      - "[PLAYER] drugtest %target_player%"
      - "[MESSAGE] &dDrug test performed on %target_player%"
      - "[CLOSE]"

  # Full scan option
  full_scan:
    slot: 31
    material: CHEST
    name: "&4&lFull Contraband Scan"
    lore:
      - "&7Complete search protocol"
      - "&7All categories included"
      - ""
      - "&7Duration: ~45 seconds total"
      - "&7Includes: Weapons, Armor, Drugs"
      - ""
      - "&c&lClick to begin full scan!"
    click_commands:
      - "[MESSAGE] &4&lInitiating full contraband scan on %target_player%"
      - "[PLAYER] sword %target_player%"
      - "[DELAY] 12000"
      - "[PLAYER] bow %target_player%"
      - "[DELAY] 12000"
      - "[PLAYER] armor %target_player%"
      - "[DELAY] 12000"
      - "[PLAYER] drugtest %target_player%"
      - "[MESSAGE] &a&lFull scan completed on %target_player%"
      - "[CLOSE]"

  # Quick actions
  scan_history:
    slot: 36
    material: BOOK
    name: "&7&lScan History"
    lore:
      - "&7View recent scans"
      - "&7and results"
    click_commands:
      - "[OPEN] scan_history"

  performance_stats:
    slot: 44
    material: GOLD_INGOT
    name: "&e&lPerformance"
    lore:
      - "&7Your scan statistics"
      - ""
      - "&7Total Scans: &a%edencorrections_player_searches%"
      - "&7Successful: &e%edencorrections_player_successful_searches%"
      - "&7Success Rate: &b%scan_success_rate%%"
      - "&7Bonus Time Earned: &a%bonus_time_earned%m"

  # Navigation
  back_to_center:
    slot: 40
    material: ARROW
    name: "&7← Back to Guard Center"
    click_commands:
      - "[OPEN] guard_center"
```

### Banking Terminal

**Duty Banking Interface** (`banking_terminal.yml`):
```yaml
gui_title: "&6&l💰 Duty Banking Terminal 💰"
gui_size: 27

items:
  # Account overview
  account_status:
    slot: 4
    material: GOLD_BLOCK
    name: "&6&lAccount Overview"
    lore:
      - "&7Account Holder: &e%player_name%"
      - "&7Guard Rank: &a%edencorrections_duty_rank%"
      - ""
      - "&7Total Duty Time: &a%edencorrections_banking_time%s"
      - "&7Available for Conversion: &e%edencorrections_banking_tokens% tokens"
      - "&7Conversion Rate: &f100 seconds = 1 token"
      - ""
      - "&7Minimum Conversion: &c300 seconds"
      - "&7Last Conversion: &f%last_conversion_time%"

  # Conversion action
  convert_tokens:
    slot: 13
    material: EMERALD
    name: "&a&l💎 Convert Duty Time"
    lore:
      - "&7Convert your accumulated"
      - "&7duty time into tokens"
      - ""
      - "&7Will Convert: &e%edencorrections_banking_tokens% tokens"
      - "&7Time Used: &a%conversion_time%s"
      - "&7Remaining: &f%remaining_time%s"
      - ""
      - "&a&lClick to convert!"
    click_commands:
      - "[PLAYER] dutybank convert"
      - "[MESSAGE] &aProcessing conversion..."
      - "[DELAY] 2000"
      - "[REFRESH]"
    view_requirements:
      requirements:
        placeholder:
          placeholders:
          - "%edencorrections_banking_tokens% >= 1"

  # Insufficient funds message
  insufficient_time:
    slot: 13
    material: REDSTONE_BLOCK
    name: "&c&l❌ Insufficient Time"
    lore:
      - "&7You need at least 300 seconds"
      - "&7(5 minutes) to convert"
      - ""
      - "&7Current Time: &c%edencorrections_banking_time%s"
      - "&7Required: &a300s"
      - "&7Needed: &e%time_needed%s more"
      - ""
      - "&cKeep working to earn more!"
    view_requirements:
      requirements:
        placeholder:
          placeholders:
          - "%edencorrections_banking_tokens% < 1"

  # Banking information
  conversion_info:
    slot: 11
    material: PAPER
    name: "&b&lConversion Information"
    lore:
      - "&7How the banking system works:"
      - ""
      - "&7• Earn time by being on duty"
      - "&7• Performance bonuses available"
      - "&7• Convert at 100s = 1 token rate"
      - "&7• Minimum 5 minutes required"
      - "&7• Auto-conversion available"

  performance_bonuses:
    slot: 15
    material: EXPERIENCE_BOTTLE
    name: "&e&lPerformance Bonuses"
    lore:
      - "&7Earn extra time through:"
      - ""
      - "&7⚔ Successful Arrests: &a+8 minutes"
      - "&7🔍 Successful Searches: &a+10 minutes"
      - "&7🎯 Successful Detections: &a+10 minutes"
      - "&7⏰ Continuous Duty: &a+2 min/hour"
      - ""
      - "&7Your Performance Today:"
      - "&7• Arrests: &e%edencorrections_player_arrests%"
      - "&7• Searches: &e%edencorrections_player_searches%"
      - "&7• Detections: &e%edencorrections_player_detections%"

  # Auto-conversion toggle
  auto_convert_toggle:
    slot: 20
    material: "%auto_convert_enabled% == true ? LIME_DYE : GRAY_DYE"
    name: "&f&lAuto-Convert: %auto_convert_status%"
    lore:
      - "&7Automatically convert duty time"
      - "&7when you reach the threshold"
      - ""
      - "&7Threshold: &e3600 seconds (1 hour)"
      - "&7Status: %auto_convert_status%"
      - ""
      - "&eClick to toggle!"
    click_commands:
      - "[CONSOLE] ec player %player_name% banking auto-toggle"
      - "[REFRESH]"
    view_requirements:
      requirements:
        permission:
          permissions:
          - "edencorrections.guard.banking.auto"

  # Transaction history
  transaction_history:
    slot: 22
    material: WRITABLE_BOOK
    name: "&7&lTransaction History"
    lore:
      - "&7View your recent"
      - "&7banking transactions"
    click_commands:
      - "[OPEN] banking_history"

  # Back button
  back_button:
    slot: 18
    material: ARROW
    name: "&7← Back to Guard Center"
    click_commands:
      - "[OPEN] guard_center"

  # Help
  banking_help:
    slot: 26
    material: QUESTION_MARK_BANNER_PATTERN
    name: "&b&lBanking Help"
    lore:
      - "&7Need help with the"
      - "&7banking system?"
    click_commands:
      - "[PLAYER] tips banking"
      - "[MESSAGE] &bBanking tips sent to chat!"
```

---

## 🎨 **ChestCommands Integration**

### Guard Tools Panel

**Main Tools** (`guard_tools.yml`):
```yaml
name: '&9&l⚔ Guard Tools ⚔'
rows: 6

# Header
'header':
  ID: BLACK_STAINED_GLASS_PANE
  POSITION-X: 1-9
  POSITION-Y: 1
  NAME: ''

'title':
  ID: DIAMOND_CHESTPLATE
  POSITION-X: 5
  POSITION-Y: 1
  NAME: '&9&l⚔ Guard Arsenal ⚔'
  LORE:
    - '&7Your complete toolkit'
    - '&7for law enforcement'

# Duty Management Row
'duty-status':
  ID: '%edencorrections_duty_active% == true ? green_concrete : red_concrete'
  POSITION-X: 2
  POSITION-Y: 2
  NAME: '&fDuty: %edencorrections_duty_status%'
  LORE:
    - '&7Rank: &e%edencorrections_duty_rank%'
    - '&7Time: &a%edencorrections_duty_time%s'
    - ''
    - '&eClick to toggle!'
  COMMAND: 'duty'

'wanted-board':
  ID: paper
  POSITION-X: 4
  POSITION-Y: 2
  NAME: '&c&lWanted Board'
  LORE:
    - '&7Active: &c%edencorrections_system_wanted_count%'
    - '&7View current threats'
  COMMAND: 'deluxemenus open wanted_board'

'chase-monitor':
  ID: compass
  POSITION-X: 6
  POSITION-Y: 2
  NAME: '&6&lChase Monitor'
  LORE:
    - '&7Active: &c%edencorrections_system_active_chases%'
    - '&7Track pursuits'
  COMMAND: 'corrections chase list'

'banking':
  ID: gold_ingot
  POSITION-X: 8
  POSITION-Y: 2
  NAME: '&6&lBanking'
  LORE:
    - '&7Tokens: &e%edencorrections_banking_tokens%'
    - '&7Time: &a%edencorrections_banking_time%s'
  COMMAND: 'deluxemenus open banking_terminal'

# Contraband Tools Row
'weapon-scan':
  ID: iron_sword
  POSITION-X: 2
  POSITION-Y: 3
  NAME: '&c&lWeapon Scanner'
  LORE:
    - '&7Detect bladed weapons'
    - '&7and sharp objects'
    - ''
    - '&eClick then target player!'
  COMMAND: 'tellraw {player} {"text":"Usage: /sword <player>","color":"red"}'

'bow-scan':
  ID: bow
  POSITION-X: 3
  POSITION-Y: 3
  NAME: '&6&lRanged Scanner'
  LORE:
    - '&7Detect bows and crossbows'
  COMMAND: 'tellraw {player} {"text":"Usage: /bow <player>","color":"gold"}'

'armor-scan':
  ID: diamond_chestplate
  POSITION-X: 4
  POSITION-Y: 3
  NAME: '&b&lArmor Scanner'
  LORE:
    - '&7Detect armor violations'
  COMMAND: 'tellraw {player} {"text":"Usage: /armor <player>","color":"aqua"}'

'drug-test':
  ID: sugar
  POSITION-X: 5
  POSITION-Y: 3
  NAME: '&d&lDrug Tester'
  LORE:
    - '&7Instant substance test'
  COMMAND: 'tellraw {player} {"text":"Usage: /drugtest <player>","color":"light_purple"}'

'full-scanner':
  ID: chest
  POSITION-X: 6
  POSITION-Y: 3
  NAME: '&4&lFull Scanner'
  LORE:
    - '&7Complete contraband suite'
  COMMAND: 'deluxemenus open contraband_scanner'

# Action Row
'arrest-tools':
  ID: iron_bars
  POSITION-X: 2
  POSITION-Y: 4
  NAME: '&8&lArrest Tools'
  LORE:
    - '&7Jail and sentencing'
    - '&7Your arrests: &a%edencorrections_player_arrests%'
  COMMAND: 'tellraw {player} {"text":"Usage: /jail <player> [reason]","color":"gray"}'

'chase-tools':
  ID: leather_boots
  POSITION-X: 4
  POSITION-Y: 4
  NAME: '&6&lChase Tools'
  LORE:
    - '&7Pursuit management'
  COMMAND: 'tellraw {player} [{"text":"Chase Commands:","color":"gold"},{"text":"\\n/chase <player> - Start","color":"yellow"},{"text":"\\n/chase capture - Arrest","color":"yellow"},{"text":"\\n/chase end - Abort","color":"yellow"}]'

'admin-tools':
  ID: redstone_block
  POSITION-X: 6
  POSITION-Y: 4
  NAME: '&c&lAdmin Tools'
  LORE:
    - '&7Administrative functions'
    - ''
    - '&c&lRESTRICTED'
  COMMAND: 'deluxemenus open admin_interface'
  PERMISSION: 'edencorrections.admin'

# Utility Row
'tips':
  ID: book
  POSITION-X: 3
  POSITION-Y: 5
  NAME: '&b&lGuard Manual'
  LORE:
    - '&7Tips and guidance'
  COMMAND: 'tips'

'stats':
  ID: paper
  POSITION-X: 5
  POSITION-Y: 5
  NAME: '&e&lYour Statistics'
  LORE:
    - '&7Arrests: &a%edencorrections_player_arrests%'
    - '&7Searches: &e%edencorrections_player_searches%'
    - '&7Duty Time: &b%edencorrections_banking_time%s'
  COMMAND: 'tellraw {player} [{"text":"=== Your Guard Statistics ===","color":"gold"},{"text":"\\nArrests: %edencorrections_player_arrests%","color":"green"},{"text":"\\nSearches: %edencorrections_player_searches%","color":"yellow"},{"text":"\\nDuty Time: %edencorrections_banking_time%s","color":"aqua"}]'

'reload':
  ID: lime_dye
  POSITION-X: 7
  POSITION-Y: 5
  NAME: '&a&lRefresh'
  LORE:
    - '&7Update information'
  COMMAND: 'cc reload guard_tools'
```

---

## 🔥 **Advanced GUI Features**

### Conditional Display Examples

**Rank-Based Access Control**:
```yaml
# Show different items based on guard rank
captain_controls:
  slot: 35
  material: COMMAND_BLOCK
  name: "&c&lCaptain Controls"
  view_requirements:
    requirements:
      placeholder:
        placeholders:
        - "%edencorrections_duty_rank% == captain"
        - "%edencorrections_duty_rank% == warden"

warden_only:
  slot: 44
  material: NETHER_STAR
  name: "&4&lWarden Authority"
  view_requirements:
    requirements:
      placeholder:
        placeholders:
        - "%edencorrections_duty_rank% == warden"
```

**Dynamic Content Updates**:
```yaml
# Update content based on system state
emergency_alert:
  slot: 0
  material: "%edencorrections_system_active_chases% > 2 ? RED_CONCRETE : GREEN_CONCRETE"
  name: "%edencorrections_system_active_chases% > 2 ? '&c&lEMERGENCY STATUS' : '&a&lNORMAL STATUS'"
  lore:
    - "%edencorrections_system_active_chases% > 2 ? '&cMultiple chases active!' : '&aAll systems normal'"
    - "&7Active Chases: &c%edencorrections_system_active_chases%"
```

### Animation and Effects

**Pulsing Effects** (DeluxeMenus + PlaceholderAPI animations):
```yaml
pulsing_alert:
  slot: 4
  material: "%math_cos(time/10)*5+5 > 5 ? REDSTONE_BLOCK : RED_CONCRETE%"
  name: "%math_cos(time/10)*5+5 > 5 ? '&c&l⚠ ALERT ⚠' : '&4&l⚠ ALERT ⚠'"
  lore:
    - "&7Emergency notifications"
```

**Progress Bars**:
```yaml
duty_progress:
  slot: 13
  material: EXPERIENCE_BOTTLE
  name: "&a&lDuty Progress"
  lore:
    - "&7Time on Duty: %edencorrections_duty_time%s"
    - ""
    - "&a%progress_bar_10% &7Progress to next bonus"
    - "&7Next bonus in: %time_to_bonus%s"
```

---

## 🎯 **TrMenu Integration**

### Modern Guard Interface

**Main Menu** (`guard_modern.yml`):
```yaml
title: 
  - '&9&l⚔ GUARD SYSTEM ⚔'
  - '&7Professional Law Enforcement'

size: 54

binds:
  Player: '%player_name%'
  GuardRank: '%edencorrections_duty_rank%'
  DutyStatus: '%edencorrections_duty_status%'

items:
  # Animated background
  'background':
    display:
      mats: 
        - 'BLACK_STAINED_GLASS_PANE'
      name: ''
    slots: [0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53]

  # Main status display
  'status':
    display:
      mats:
        - 'condition::%edencorrections_duty_active% == true ? DIAMOND_CHESTPLATE ? LEATHER_CHESTPLATE'
      name: 
        - '&f&lGuard Status'
        - '&7%DutyStatus% • %GuardRank%'
      lore:
        - '&7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        - '&7Officer: &e%Player%'
        - '&7Status: %DutyStatus%'
        - '&7Rank: &a%GuardRank%'
        - '&7Time: &b%edencorrections_duty_time%s'
        - '&7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
        - ''
        - '&eClick to toggle duty status'
    slots: [4]
    actions:
      all:
        - 'player: duty'
        - 'refresh'

  # Quick action buttons
  'wanted':
    display:
      mats: ['PAPER']
      name: '&c&lWanted System'
      lore:
        - '&7Active Wanted: &c%edencorrections_system_wanted_count%'
        - ''
        - '&eClick to view wanted board'
    slots: [20]
    actions:
      all:
        - 'open: wanted_modern'

  'contraband':
    display:
      mats: ['CHEST']
      name: '&4&lContraband Scanner'
      lore:
        - '&7Your Searches: &a%edencorrections_player_searches%'
        - '&7Success Rate: &e%search_success_rate%%'
        - ''
        - '&eClick to open scanner'
    slots: [22]
    actions:
      all:
        - 'open: contraband_modern'

  # Banking with modern styling
  'banking':
    display:
      mats: ['GOLD_INGOT']
      name: '&6&l💰 Duty Banking'
      lore:
        - '&7Available: &e%edencorrections_banking_tokens% tokens'
        - '&7Duty Time: &a%edencorrections_banking_time%s'
        - '&7Rate: &f100s = 1 token'
        - ''
        - '&eClick to manage banking'
    slots: [24]
    actions:
      all:
        - 'open: banking_modern'

# Add particle effects for modern feel
effects:
  open:
    - 'sound: BLOCK_NOTE_BLOCK_PLING-1-2'
    - 'particle: VILLAGER_HAPPY-0.5-0.5-0.5-0.1-10'
  close:
    - 'sound: BLOCK_CHEST_CLOSE-1-1'
```

---

## 📊 **Performance Optimization**

### Efficient Placeholder Usage

**Cache Frequently Used Values**:
```yaml
# Instead of calling %edencorrections_duty_status% multiple times
cached_status: &duty_status "%edencorrections_duty_status%"
cached_rank: &guard_rank "%edencorrections_duty_rank%"

items:
  duty_display:
    name: "&fDuty: *duty_status"
    lore:
      - "&7Rank: &e*guard_rank"
```

**Reduce Database Calls**:
```yaml
# Use conditional requirements instead of multiple placeholders
view_requirements:
  requirements:
    placeholder:
      placeholders:
      - "%edencorrections_duty_active% == true"
      # This is better than checking multiple status placeholders
```

### Update Strategies

**Smart Refresh Timing**:
```yaml
# Auto-refresh for dynamic content
update_requirements:
  requirements:
    placeholder:
      placeholders:
      - "%server_ticks_since_start% % 100 == 0"  # Every 5 seconds
```

**Manual Refresh Points**:
```yaml
critical_actions:
  click_commands:
    - "[PLAYER] duty"
    - "[DELAY] 1000"  # Wait for command to process
    - "[REFRESH]"     # Then refresh display
```

---

## 🔧 **Troubleshooting GUI Issues**

### Common Problems

#### Placeholders Not Working
```yaml
# Problem: %edencorrections_duty_status% shows as raw text
# Solution: Verify PlaceholderAPI expansion is installed
# /papi ecloud download EdenCorrections
# /papi reload

# Test placeholder manually:
# /papi parse YourName %edencorrections_duty_status%
```

#### Permissions Issues
```yaml
# Problem: Menu doesn't show for guards
# Solution: Check view requirements
view_requirements:
  requirements:
    permission:
      permissions:
      - "edencorrections.guard"  # Ensure this exists
      deny commands:
      - "[MESSAGE] &cAccess denied - Contact admin"
```

#### Performance Issues
```yaml
# Problem: Menu is slow to load
# Solution: Optimize placeholder usage

# BAD - Too many placeholders
lore:
  - "&7Status: %edencorrections_duty_status%"
  - "&7Active: %edencorrections_duty_active%"
  - "&7Rank: %edencorrections_duty_rank%"
  - "&7Time: %edencorrections_duty_time%"

# GOOD - Cached values
lore:
  - "&7Status: %cache_duty_status%"
  - "&7Rank: %cache_guard_rank%"
```

### Debug Commands

```bash
# Test placeholder functionality
/papi parse <player> %edencorrections_duty_status%

# Check menu permissions
/lp user <player> permission check deluxemenus.menu.guard_center

# Reload menu configurations
/dm reload

# Check for plugin conflicts
/plugins
```

---

## 🚀 **Advanced Integration Examples**

### Multi-Server Network GUI

**Cross-Server Status** (with BungeeCord placeholders):
```yaml
network_status:
  slot: 8
  material: NETHER_STAR
  name: "&9&lNetwork Status"
  lore:
    - "&7Server: &e%bungee_server%"
    - "&7Network Guards: &a%bungee_total_guards%"
    - "&7Network Wanted: &c%bungee_total_wanted%"
    - ""
    - "&eClick to view network stats"
  click_commands:
    - "[BUNGEECOMMAND] ec network status"
```

### Integration with Other Plugins

**Economy Integration**:
```yaml
economy_display:
  slot: 26
  material: EMERALD
  name: "&a&lEconomy Status"
  lore:
    - "&7Balance: &a$%vault_eco_balance_formatted%"
    - "&7Duty Tokens: &e%edencorrections_banking_tokens%"
    - "&7Token Value: &f$%token_value%"
```

**Job Plugin Integration**:
```yaml
job_integration:
  slot: 35
  material: EXPERIENCE_BOTTLE
  name: "&b&lJob Progress"
  lore:
    - "&7Job: &e%jobs_current_job%"
    - "&7Level: &a%jobs_current_level%"
    - "&7Guard Rank: &e%edencorrections_duty_rank%"
    - ""
    - "&7Synced progression available"
```

---

## 📋 **GUI Quick Reference**

### Essential Placeholders for GUIs

```yaml
# Status Placeholders
%edencorrections_duty_status%          # "On Duty" / "Off Duty"
%edencorrections_duty_active%          # true / false
%edencorrections_duty_rank%            # Guard rank name
%edencorrections_duty_time%            # Seconds on duty

# System Placeholders
%edencorrections_system_active_chases% # Number of active chases
%edencorrections_system_wanted_count%  # Number of wanted players
%edencorrections_system_guards_online% # Online guards count

# Banking Placeholders
%edencorrections_banking_tokens%       # Available tokens
%edencorrections_banking_time%         # Total duty time in seconds

# Player Statistics
%edencorrections_player_arrests%       # Player's arrest count
%edencorrections_player_searches%      # Player's search count
%edencorrections_player_violations%    # Player's violation count
```

### Menu Command Integration

```yaml
# Open menus from commands
open_commands:
  - "guards"
  - "guardpanel"
  - "gp"

# Command aliases for quick access
aliases:
  - "gc"     # Guard Center
  - "ws"     # Wanted System  
  - "cs"     # Contraband Scanner
  - "bt"     # Banking Terminal
```

### Best Practices

1. **Performance**: Cache frequently used placeholders
2. **User Experience**: Use consistent color schemes and layouts
3. **Accessibility**: Provide clear navigation and help options
4. **Maintenance**: Document custom variables and conditions
5. **Testing**: Verify all commands work correctly in GUI context

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete GUI Integration Guide* 