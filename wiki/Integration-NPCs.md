# 🤖 NPC Integration Guide

Complete guide for integrating EdenCorrections with NPC plugins, featuring realistic guard stations, automated checkpoints, and interactive law enforcement systems.

---

## 🎯 **Overview**

EdenCorrections seamlessly integrates with popular NPC plugins to create immersive guard stations, automated security checkpoints, and interactive law enforcement systems. This guide covers everything from basic guard NPCs to complex automated security networks.

### Supported NPC Plugins
| Plugin | Status | Integration Level | Best For |
|--------|--------|------------------|----------|
| **Citizens** | ✅ Full Support | Advanced | Interactive guards, checkpoints, patrols |
| **MythicMobs** | ✅ Full Support | Advanced | Combat guards, bosses, special events |
| **NPC-Lib** | ✅ Compatible | Basic | Simple interactions |
| **FancyNpcs** | ✅ Compatible | Standard | Modern NPC interactions |
| **ZNPCsPlus** | ✅ Compatible | Standard | Holograms and displays |

---

## 🚀 **Quick Start Examples**

### Basic Guard Station NPC (Citizens)
```bash
# Create the main duty station NPC
/npc create GuardStation --type PLAYER
/npc skin GuardStation steve

# Add duty toggle functionality
/npc rightclickcommand GuardStation add player duty
/npc rightclickcommand GuardStation add console say {player} is changing duty status

# Add visual feedback
/npc text GuardStation "Duty Station" "Right-click to toggle duty"
```

### Simple Checkpoint NPC (Citizens)
```bash
# Create checkpoint guard
/npc create Checkpoint --type VILLAGER
/npc lookclose Checkpoint true

# Add security scanning
/npc rightclickcommand Checkpoint add server drugtest {player}
/npc rightclickcommand Checkpoint add console tellraw {player} {"text":"Security scan complete","color":"green"}
```

---

## 👮‍♂️ **Citizens Integration**

### Complete Guard Station System

#### Main Guard Captain NPC
```bash
# === GUARD CAPTAIN SETUP ===
/npc create GuardCaptain --type PLAYER
/npc skin GuardCaptain police_officer
/npc lookclose GuardCaptain true
/npc glowcolor GuardCaptain BLUE

# Position and appearance
/npc mount GuardCaptain
/npc pose GuardCaptain STANDING
/npc trait GuardCaptain equipmenttrait

# Give guard equipment
/npc equipmenttrait GuardCaptain set HAND IRON_SWORD
/npc equipmenttrait GuardCaptain set CHESTPLATE IRON_CHESTPLATE
/npc equipmenttrait GuardCaptain set HELMET IRON_HELMET
/npc equipmenttrait GuardCaptain set LEGGINGS IRON_LEGGINGS
/npc equipmenttrait GuardCaptain set BOOTS IRON_BOOTS

# Interactive features
/npc text GuardCaptain add "Captain Johnson" "Right-click for services"

# Multiple interaction options
/npc rightclickcommand GuardCaptain add console say Welcome to the Guard Station, {player}!
/npc rightclickcommand GuardCaptain add player corrections wanted check {player}
/npc rightclickcommand GuardCaptain add console tellraw {player} {"text":"[Captain] Your status has been verified.","color":"green"}
/npc rightclickcommand GuardCaptain add delay 20
/npc rightclickcommand GuardCaptain add console deluxemenus open guard_center {player}

# Add dialogue trait for conversations
/npc trait GuardCaptain conversationtrait
# Then create conversation files for complex interactions
```

#### Duty Management NPC
```bash
# === DUTY STATION SETUP ===
/npc create DutyOfficer --type PLAYER
/npc skin DutyOfficer guard_uniform

# Position at duty station
/npc tp DutyOfficer
/npc anchor DutyOfficer --save duty_post

# Duty toggle with validation
/npc rightclickcommand DutyOfficer add console ec-execute-if-in-region guard_station player duty {player}
/npc rightclickcommand DutyOfficer add console ec-execute-if-not-in-region guard_station tellraw {player} {"text":"You must be in the guard station to change duty status!","color":"red"}

# Visual status indicator
/npc text DutyOfficer "Duty Officer" "Click to toggle duty status"

# Add proximity commands for realism
/npc trait DutyOfficer proximitytrait
/npc proximitytrait DutyOfficer add --range 5 --command console say Welcome to the duty station, {player}
```

#### Equipment Quartermaster NPC
```bash
# === QUARTERMASTER SETUP ===
/npc create Quartermaster --type VILLAGER
/npc profession Quartermaster ARMORER

# Equipment distribution based on rank
/npc rightclickcommand Quartermaster add console ec-kit-for-rank {player}
/npc rightclickcommand Quartermaster add console tellraw {player} {"text":"[Quartermaster] Equipment issued based on your rank.","color":"gold"}

# Custom script for rank-based kit giving
# This would be a custom plugin command that checks guard rank and gives appropriate kit
/npc rightclickcommand Quartermaster add console rankkit give {player}

# Inventory trait for shop-like interaction
/npc trait Quartermaster shopkeeper
```

### Automated Security Checkpoints

#### Entry Checkpoint System
```bash
# === ENTRY CHECKPOINT ===
/npc create EntryGuard --type VILLAGER
/npc profession EntryGuard CLERIC
/npc lookclose EntryGuard true

# Automated security scanning
/npc trait EntryGuard proximitytrait
/npc proximitytrait EntryGuard add --range 3 --command console drugtest {player}
/npc proximitytrait EntryGuard add --range 3 --command console tellraw {player} {"text":"[Security] Scanning for contraband...","color":"red"}
/npc proximitytrait EntryGuard add --range 3 --command delay 40
/npc proximitytrait EntryGuard add --range 3 --command console tellraw {player} {"text":"[Security] Scan complete. Proceed.","color":"green"}

# Visual indicators
/npc text EntryGuard "Security Checkpoint" "Automated scanning in progress"
/npc glowcolor EntryGuard RED

# Add sound effects
/npc proximitytrait EntryGuard add --range 3 --command console execute at {player} run playsound block.note_block.bell master @a ~ ~ ~ 1 1
```

#### Exit Verification Point
```bash
# === EXIT CHECKPOINT ===
/npc create ExitGuard --type VILLAGER
/npc profession ExitGuard WEAPONSMITH

# Wanted level verification
/npc rightclickcommand ExitGuard add server corrections wanted check {player}
/npc rightclickcommand ExitGuard add console ec-allow-exit-if-not-wanted {player}

# Conditional exit permission
/npc rightclickcommand ExitGuard add console tellraw {player} [{"text":"[Exit Control] ","color":"blue"},{"text":"Checking authorization...","color":"gray"}]
/npc rightclickcommand ExitGuard add delay 20
/npc rightclickcommand ExitGuard add console ec-conditional-message {player} wanted "Exit denied - Wanted individual" "Exit authorized"

/npc text ExitGuard "Exit Control" "Click for exit authorization"
```

### Patrol and Response NPCs

#### Roaming Patrol Guard
```bash
# === PATROL GUARD ===
/npc create PatrolGuard --type PLAYER
/npc skin PatrolGuard police_patrol

# Setup patrol route
/npc trait PatrolGuard waypoints
/npc waypoints PatrolGuard add --name checkpoint1
/npc waypoints PatrolGuard add --name checkpoint2  
/npc waypoints PatrolGuard add --name checkpoint3
/npc waypoints PatrolGuard linear true

# Patrol behavior
/npc speed PatrolGuard 0.8
/npc lookclose PatrolGuard false

# Interactive patrol features
/npc trait PatrolGuard proximitytrait
/npc proximitytrait PatrolGuard add --range 8 --command console ec-patrol-scan {player}
/npc proximitytrait PatrolGuard add --range 8 --command console tellraw {player} {"text":"[Patrol] Area secured.","color":"blue"}

# Equipment for patrol
/npc trait PatrolGuard equipmenttrait
/npc equipmenttrait PatrolGuard set HAND IRON_SWORD
/npc equipmenttrait PatrolGuard set CHESTPLATE LEATHER_CHESTPLATE
```

#### Emergency Response NPC
```bash
# === EMERGENCY RESPONDER ===
/npc create EmergencyResponse --type PLAYER
/npc skin EmergencyResponse swat_officer

# Emergency activation
/npc rightclickcommand EmergencyResponse add console broadcast &c[EMERGENCY] All guards report to active locations!
/npc rightclickcommand EmergencyResponse add server corrections chase list
/npc rightclickcommand EmergencyResponse add console ec-emergency-protocol activate

# Fast movement for emergencies
/npc speed EmergencyResponse 1.5
/npc jumpheight EmergencyResponse 1.2

# Equipment for emergency response
/npc equipmenttrait EmergencyResponse set HAND DIAMOND_SWORD
/npc equipmenttrait EmergencyResponse set CHESTPLATE DIAMOND_CHESTPLATE
/npc equipmenttrait EmergencyResponse set HELMET DIAMOND_HELMET

/npc text EmergencyResponse "Emergency Response" "EMERGENCY ACTIVATION ONLY"
/npc glowcolor EmergencyResponse RED
```

### Advanced Interactive Systems

#### Information Kiosk NPC
```bash
# === INFORMATION KIOSK ===
/npc create InfoKiosk --type VILLAGER
/npc profession InfoKiosk LIBRARIAN

# Information services menu
/npc rightclickcommand InfoKiosk add console deluxemenus open info_kiosk {player}

# Voice commands via conversation trait
/npc trait InfoKiosk conversationtrait

# Create conversation file: plugins/Citizens/conversations/info_kiosk.yml
```

**Info Kiosk Conversation** (`plugins/Citizens/conversations/info_kiosk.yml`):
```yaml
info_kiosk:
  format:
    player: "&7[{npc}] &f"
    npc: "&9[Info Kiosk] &f"
  
  initial: greeting
  
  greeting:
    text: "Welcome to the Information Kiosk. How can I assist you today?"
    options:
      wanted_info:
        text: "Check wanted information"
        response: wanted_check
      guard_help:
        text: "Guard assistance"  
        response: guard_help
      system_status:
        text: "System status"
        response: system_status
      exit:
        text: "Exit"
        response: goodbye

  wanted_check:
    text: "Let me check the current wanted status..."
    script: "corrections wanted check {player}"
    options:
      back:
        text: "Back to main menu"
        response: greeting

  guard_help:
    text: "What type of assistance do you need?"
    options:
      duty_help:
        text: "Duty system help"
        response: duty_help
      contraband_help:
        text: "Contraband scanning help"
        response: contraband_help
      back:
        text: "Back"
        response: greeting

  duty_help:
    text: "The duty system allows guards to toggle their status. Use the duty station or type /duty when in the guard region."
    script: "tips duty"
    options:
      back:
        text: "Back"
        response: greeting

  system_status:
    text: "Retrieving system information..."
    script: "corrections system stats"
    options:
      back:
        text: "Back"
        response: greeting

  goodbye:
    text: "Thank you for using the Information Kiosk. Stay safe!"
```

#### Training Instructor NPC
```bash
# === TRAINING INSTRUCTOR ===
/npc create Instructor --type PLAYER
/npc skin Instructor drill_sergeant

# Training programs
/npc rightclickcommand Instructor add console ec-start-training {player} basic
/npc rightclickcommand Instructor add console tellraw {player} {"text":"[Instructor] Welcome to guard training, recruit!","color":"gold"}

# Training validation
/npc rightclickcommand Instructor add console ec-check-training-eligibility {player}

# Equipment for instructor
/npc equipmenttrait Instructor set HAND STICK
/npc equipmenttrait Instructor set CHESTPLATE GOLDEN_CHESTPLATE

/npc text Instructor "Training Instructor" "Click to begin training"
```

---

## 🔥 **MythicMobs Integration**

### Elite Guard Boss NPCs

#### Warden Boss NPC
```yaml
# File: plugins/MythicMobs/Mobs/WardenBoss.yml
WardenBoss:
  Type: PLAYER
  Display: '&4&lWarden Commander'
  Health: 500
  Damage: 15
  Options:
    Silent: false
    Despawn: false
    PreventOtherDrops: true
  
  Equipment:
  - NETHERITE_SWORD:4
  - NETHERITE_HELMET:3
  - NETHERITE_CHESTPLATE:2
  - NETHERITE_LEGGINGS:1
  - NETHERITE_BOOTS:0
  
  Skills:
  # Interaction skills
  - command{c="corrections wanted check <trigger.name>"} @trigger ~onInteract
  - message{m="&4[Warden] &fState your business, <trigger.name>."} @trigger ~onInteract
  - delay 20
  - skill{s=WardenResponse} @trigger ~onInteract
  
  # Combat skills for special events
  - skill{s=WardenCombat} @self ~onDamaged
  - skill{s=GuardAlert} @NearbyPlayers{r=50} ~onCombat

WardenResponse:
  Skills:
  - command{c="deluxemenus open warden_interface <trigger.name>"} @trigger
  - message{m="&4[Warden] &fAccessing command interface..."} @trigger

WardenCombat:
  Skills:
  - message{m="&c[ALERT] Warden under attack! All guards respond!"} @NearbyPlayers{r=100}
  - command{c="corrections chase list"} @NearbyPlayers{r=100;permission=edencorrections.guard}
  - effect{type=STRENGTH;duration=200;amplifier=2} @self
  - teleport{location=<warden.spawn>} @self ~onHealthBelow{health=100}

GuardAlert:
  Skills:
  - message{m="&c[EMERGENCY] Code Red - Warden under attack!"} @trigger
  - command{c="corrections system debug emergency"} @trigger
  - sound{s=entity.wither.spawn;v=1;p=2} @trigger
```

#### Automated Guard Turret
```yaml
# File: plugins/MythicMobs/Mobs/GuardTurret.yml
GuardTurret:
  Type: ARMOR_STAND
  Display: '&9&lSecurity Turret'
  Health: 200
  Options:
    Silent: true
    Despawn: false
    Invulnerable: true
    NoGravity: true
  
  Equipment:
  - CROSSBOW:4
  - AIR:3
  - IRON_CHESTPLATE:2
  - AIR:1
  - AIR:0
  
  AITargetSelectors:
  - 0 clear
  - 1 players
  
  Skills:
  # Scanning behavior
  - skill{s=SecurityScan} @PlayersInRadius{r=10} ~onTimer:60
  - skill{s=TurretAlert} @PlayersInRadius{r=5} ~onTimer:20
  
  # Threat detection
  - skill{s=ThreatResponse} @target ~onDamaged
  - skill{s=WantedDetection} @PlayersInRadius{r=15} ~onTimer:100

SecurityScan:
  Skills:
  - message{m="&c[Turret] &7Scanning area... All clear."} @trigger
  - particles{p=redstone;a=20;vs=2;hs=2} @self
  - sound{s=block.note_block.hat;v=0.5;p=2} @self

TurretAlert:
  Skills:
  - message{m="&e[Turret] &7Motion detected. Identifying..."} @trigger
  - command{c="corrections wanted check <trigger.name>"} @trigger
  - delay 40
  - skill{s=IdentificationComplete} @trigger

WantedDetection:
  Skills:
  - command{c="corrections wanted check <trigger.name>"} @trigger
  - conditionalskill{s=WantedPlayerFound;condition="<trigger.placeholder.edencorrections_wanted_level> > 0"} @trigger

WantedPlayerFound:
  Skills:
  - message{m="&c[ALERT] Wanted individual detected: <trigger.name>"} @PlayersInRadius{r=50;permission=edencorrections.guard}
  - command{c="corrections chase <trigger.name>"} @PlayersInRadius{r=50;permission=edencorrections.guard}
  - particles{p=redstone;a=100;vs=5;hs=5;c=255,0,0} @self
  - sound{s=entity.wither.spawn;v=1;p=1} @PlayersInRadius{r=30}
```

### Interactive Security Systems

#### Checkpoint Scanner
```yaml
# File: plugins/MythicMobs/Mobs/CheckpointScanner.yml
CheckpointScanner:
  Type: SHULKER
  Display: '&6&lSecurity Scanner'
  Health: 1000
  Options:
    Silent: true
    Despawn: false
    Invulnerable: true
    NoAI: true
  
  Skills:
  # Proximity detection
  - skill{s=ScanInitiate} @PlayersInRadius{r=3} ~onTimer:40
  - skill{s=ContrabandScan} @PlayersInRadius{r=2} ~onTimer:80
  
  # Interactive features
  - skill{s=ManualScan} @trigger ~onInteract
  - skill{s=ScannerInfo} @trigger ~onLeftClick

ScanInitiate:
  Skills:
  - message{m="&6[Scanner] &7Please remain still for security scan..."} @trigger
  - particles{p=villager_happy;a=10;vs=1;hs=1} @trigger
  - sound{s=block.note_block.bell;v=0.8;p=1} @trigger
  - delay 60
  - skill{s=BasicSecurityScan} @trigger

BasicSecurityScan:
  Skills:
  - command{c="drugtest <trigger.name>"} @trigger
  - delay 20
  - message{m="&a[Scanner] &7Security scan complete. Proceed."} @trigger
  - particles{p=villager_happy;a=20;vs=2;hs=2;c=0,255,0} @trigger

ContrabandScan:
  Skills:
  - command{c="sword <trigger.name>"} @trigger
  - delay 200
  - command{c="bow <trigger.name>"} @trigger
  - delay 200  
  - command{c="armor <trigger.name>"} @trigger
  - message{m="&4[Scanner] &7Full contraband scan initiated."} @trigger

ManualScan:
  Skills:
  - message{m="&6[Scanner] &fAccessing manual scan options..."} @trigger
  - command{c="deluxemenus open contraband_scanner <trigger.name>"} @trigger
```

#### Emergency Beacon
```yaml
# File: plugins/MythicMobs/Mobs/EmergencyBeacon.yml
EmergencyBeacon:
  Type: BEACON
  Display: '&c&l⚠ EMERGENCY BEACON ⚠'
  Health: 1000
  Options:
    Silent: false
    Despawn: false
    Invulnerable: true
    Glowing: true
  
  Skills:
  # Emergency activation
  - skill{s=EmergencyAlert} @trigger ~onInteract
  - skill{s=BeaconPulse} @self ~onTimer:60
  
  # Automatic threat response
  - skill{s=ThreatDetected} @PlayersInRadius{r=20} ~onTimer:100

EmergencyAlert:
  Skills:
  - message{m="&c&l[EMERGENCY BEACON ACTIVATED]"} @PlayersInRadius{r=100}
  - message{m="&c&lAll guards report immediately!"} @PlayersInRadius{r=100;permission=edencorrections.guard}
  - command{c="corrections chase list"} @PlayersInRadius{r=50;permission=edencorrections.guard}
  - command{c="corrections wanted list"} @PlayersInRadius{r=50;permission=edencorrections.guard}
  - sound{s=entity.wither.spawn;v=2;p=0.5} @PlayersInRadius{r=100}
  - particles{p=redstone;a=200;vs=10;hs=10;c=255,0,0} @self
  - skill{s=BeaconFlash} @self

BeaconPulse:
  Skills:
  - particles{p=end_rod;a=50;vs=5;hs=5} @self
  - sound{s=block.beacon.ambient;v=1;p=1} @self

ThreatDetected:
  Skills:
  - conditionalskill{s=WantedThreat;condition="<trigger.placeholder.edencorrections_wanted_level> > 2"} @trigger

WantedThreat:
  Skills:
  - skill{s=EmergencyAlert} @self
  - message{m="&c[BEACON] High-threat individual detected: <trigger.name>"} @PlayersInRadius{r=100;permission=edencorrections.guard}
```

---

## 🎯 **Advanced NPC Scenarios**

### Complete Prison Complex

#### Reception Desk System
```bash
# === RECEPTION DESK ===
/npc create Reception --type VILLAGER
/npc profession Reception LIBRARIAN

# New prisoner intake
/npc rightclickcommand Reception add console ec-prisoner-intake {player}
/npc rightclickcommand Reception add console tellraw {player} {"text":"[Reception] Welcome to the facility. Please wait for processing.","color":"blue"}

# Visitor management
/npc rightclickcommand Reception add console ec-visitor-check {player}

# Information services
/npc rightclickcommand Reception add console deluxemenus open reception_services {player}

/npc text Reception "Reception Desk" "New arrivals report here"
```

#### Medical Bay NPC
```bash
# === MEDICAL OFFICER ===
/npc create MedicalOfficer --type VILLAGER
/npc profession MedicalOfficer CLERIC

# Health services
/npc rightclickcommand MedicalOfficer add console heal {player}
/npc rightclickcommand MedicalOfficer add console effect give {player} minecraft:regeneration 30 1
/npc rightclickcommand MedicalOfficer add console tellraw {player} {"text":"[Medical] You have been treated. Report any ongoing issues.","color":"green"}

# Drug test referrals
/npc rightclickcommand MedicalOfficer add console ec-medical-drugtest {player}

/npc text MedicalOfficer "Medical Officer" "Healthcare services available"
```

#### Cafeteria Staff NPC
```bash
# === CAFETERIA WORKER ===
/npc create CafeteriaWorker --type VILLAGER
/npc profession CafeteriaWorker FARMER

# Meal distribution (time-based)
/npc rightclickcommand CafeteriaWorker add console ec-meal-time-check {player}
/npc rightclickcommand CafeteriaWorker add console ec-distribute-meal {player}

# Special diet management
/npc rightclickcommand CafeteriaWorker add console ec-dietary-restrictions {player}

/npc text CafeteriaWorker "Cafeteria" "Meal times: 7AM, 12PM, 6PM"
```

### Automated Security Network

#### Central Security Console
```bash
# === SECURITY CONSOLE ===
/npc create SecurityConsole --type ARMOR_STAND
/npc glowcolor SecurityConsole BLUE

# Security dashboard access
/npc rightclickcommand SecurityConsole add console deluxemenus open security_dashboard {player}

# Emergency protocols
/npc rightclickcommand SecurityConsole add console ec-security-protocol {player}

# Requires admin permission
/npc trait SecurityConsole permissiontrait
/npc permissiontrait SecurityConsole add edencorrections.admin

/npc text SecurityConsole "Security Console" "AUTHORIZED PERSONNEL ONLY"
```

#### Lockdown System NPC
```bash
# === LOCKDOWN CONTROLLER ===
/npc create LockdownController --type WITHER_SKELETON

# Lockdown initiation
/npc rightclickcommand LockdownController add console broadcast &c[LOCKDOWN] Facility lockdown initiated!
/npc rightclickcommand LockdownController add console ec-lockdown-protocol activate
/npc rightclickcommand LockdownController add console corrections chase endall

# Visual effects
/npc rightclickcommand LockdownController add console execute at {player} run particle minecraft:smoke ~ ~ ~ 2 2 2 0.1 100
/npc rightclickcommand LockdownController add console execute at {player} run playsound entity.wither.spawn master @a ~ ~ ~ 2 0.5

/npc text LockdownController "Lockdown Control" "EMERGENCY USE ONLY"
/npc glowcolor LockdownController RED
```

---

## 🔧 **Custom Command Integration**

### Custom NPC Commands

#### Region-Based Command Execution
```bash
# Execute commands only if player is in specific region
/npc rightclickcommand GuardNPC add console execute if entity {player}[nbt={Location:{World:"world",Region:"guard_station"}}] run duty

# Alternative using WorldGuard integration
/npc rightclickcommand GuardNPC add console ec-execute-if-in-region guard_station player duty {player}
```

#### Conditional Command Execution
```bash
# Execute based on player's wanted status
/npc rightclickcommand CheckpointNPC add console execute if score {player} wanted_level matches 1.. run tellraw {player} {"text":"Access denied - Wanted individual","color":"red"}
/npc rightclickcommand CheckpointNPC add console execute unless score {player} wanted_level matches 1.. run tellraw {player} {"text":"Access granted","color":"green"}
```

#### Time-Based Commands
```bash
# Meal time announcements
/npc rightclickcommand CafeteriaNPC add console execute if predicate ec:meal_time run give {player} bread 3
/npc rightclickcommand CafeteriaNPC add console execute unless predicate ec:meal_time run tellraw {player} {"text":"Meals served at 7AM, 12PM, and 6PM","color":"yellow"}
```

### Multi-Step Interactions

#### Complex Dialogue Chains
```bash
# Setup multi-step prisoner intake process
/npc create IntakeOfficer --type VILLAGER
/npc trait IntakeOfficer conversationtrait

# Step 1: Initial greeting and ID check
/npc rightclickcommand IntakeOfficer add console tellraw {player} {"text":"[Intake] Please state your name for verification.","color":"blue"}
/npc rightclickcommand IntakeOfficer add console ec-start-intake-process {player}

# Step 2: Background check
/npc rightclickcommand IntakeOfficer add delay 60
/npc rightclickcommand IntakeOfficer add console corrections wanted check {player}

# Step 3: Assignment
/npc rightclickcommand IntakeOfficer add delay 40
/npc rightclickcommand IntakeOfficer add console ec-assign-cell {player}
/npc rightclickcommand IntakeOfficer add console tellraw {player} {"text":"[Intake] Processing complete. Welcome to the facility.","color":"green"}
```

---

## 📊 **Performance and Optimization**

### Efficient NPC Management

#### Reduce Command Spam
```bash
# Use cooldowns to prevent spam
/npc rightclickcommand GuardNPC add console ec-cooldown-check {player} 5
/npc rightclickcommand GuardNPC add console duty

# Alternative: Use Citizens cooldown trait
/npc trait GuardNPC cooldowntrait
/npc cooldowntrait GuardNPC set global 5000  # 5 second cooldown
```

#### Optimize Proximity Commands
```bash
# Use larger intervals for non-critical proximity detection
/npc trait PatrolNPC proximitytrait
/npc proximitytrait PatrolNPC add --range 10 --interval 60 --command console ec-patrol-scan {player}

# Critical security systems use shorter intervals
/npc proximitytrait SecurityNPC add --range 5 --interval 20 --command console drugtest {player}
```

#### Memory Management
```bash
# Disable unused traits to save memory
/npc trait GuardNPC -lookclose  # Remove if not needed
/npc trait GuardNPC -skinlayers # Disable if using default skin

# Use selective loading
/npc chunk GuardNPC --enable  # Only load when chunk is loaded
```

### Network Optimization

#### Command Batching
```bash
# Batch multiple commands into single operations
/npc rightclickcommand GuardNPC add console ec-batch-security-check {player}
# Instead of:
# /npc rightclickcommand GuardNPC add console drugtest {player}
# /npc rightclickcommand GuardNPC add console sword {player}
# /npc rightclickcommand GuardNPC add console bow {player}
```

#### Async Operations
```bash
# Use async commands for database operations
/npc rightclickcommand DatabaseNPC add console ec-async-player-lookup {player}
/npc rightclickcommand DatabaseNPC add console ec-async-update-stats {player}
```

---

## 🚨 **Troubleshooting NPC Issues**

### Common Problems

#### NPCs Not Responding
```bash
# Check if NPC exists and is loaded
/npc list

# Verify chunk loading
/npc chunk NPCName --enable

# Check for command conflicts
/npc rightclickcommand NPCName list
/npc rightclickcommand NPCName clear  # If needed

# Test basic functionality
/npc rightclickcommand NPCName add console say Test command
```

#### Permission Issues
```bash
# Check NPC permissions
/npc trait NPCName permissiontrait
/npc permissiontrait NPCName list

# Verify player permissions
/lp user PlayerName permission check edencorrections.guard

# Test command execution
/npc rightclickcommand NPCName add console tellraw {player} {"text":"Permission test","color":"green"}
```

#### Performance Issues
```bash
# Check for excessive commands
/npc rightclickcommand NPCName list
# Look for duplicate or conflicting commands

# Monitor server performance
/timings report

# Disable problematic NPCs temporarily
/npc select NPCName
/npc despawn
```

### Debug Commands

```bash
# NPC debugging
/npc debug NPCName
/npc info NPCName

# Command testing
/npc rightclickcommand NPCName add console tellraw @a {"text":"NPC Debug Test","color":"yellow"}

# Trait verification
/npc trait NPCName

# Position and state checks
/npc tp NPCName
/npc lookclose NPCName true
```

---

## 🎨 **Creative NPC Implementations**

### Themed Guard Characters

#### Medieval Prison Guards
```bash
# Knight Commander
/npc create KnightCommander --type PLAYER
/npc skin KnightCommander knight_armor
/npc equipmenttrait KnightCommander set HAND DIAMOND_SWORD
/npc equipmenttrait KnightCommander set CHESTPLATE DIAMOND_CHESTPLATE
/npc text KnightCommander "Sir Gareth" "Commander of the Guard"

# Archer Guard
/npc create ArcherGuard --type PLAYER
/npc skin ArcherGuard archer_outfit
/npc equipmenttrait ArcherGuard set HAND BOW
/npc equipmenttrait ArcherGuard set CHESTPLATE LEATHER_CHESTPLATE
```

#### Futuristic Security
```bash
# Cyber Guard
/npc create CyberGuard --type PLAYER
/npc skin CyberGuard cyber_officer
/npc glowcolor CyberGuard CYAN
/npc equipmenttrait CyberGuard set HAND NETHERITE_SWORD
/npc text CyberGuard "Unit-7734" "Cybernetic Security Officer"

# AI Security System
/npc create AISystem --type ARMOR_STAND
/npc glowcolor AISystem BLUE
/npc text AISystem "A.I.D.E.N." "Artificial Intelligence Defense Network"
```

### Interactive Environments

#### Living Guard Barracks
```bash
# Sleeping Guard (off-duty)
/npc create SleepingGuard --type PLAYER
/npc pose SleepingGuard SLEEPING
/npc rightclickcommand SleepingGuard add console tellraw {player} {"text":"[Guard] Zzz... five more minutes...","color":"gray"}

# Training Guard
/npc create TrainingGuard --type PLAYER
/npc trait TrainingGuard waypoints
/npc waypoints TrainingGuard add --name training1
/npc waypoints TrainingGuard add --name training2
/npc speed TrainingGuard 1.2
```

#### Dynamic Response System
```bash
# Alert Level NPCs that change behavior based on server state
/npc create DynamicGuard --type PLAYER

# Normal state commands
/npc rightclickcommand DynamicGuard add console ec-if-alert-level normal tellraw {player} {"text":"[Guard] All quiet on the watch.","color":"green"}

# High alert commands  
/npc rightclickcommand DynamicGuard add console ec-if-alert-level high tellraw {player} {"text":"[Guard] Stay alert! We have reports of trouble.","color":"red"}

# Emergency state commands
/npc rightclickcommand DynamicGuard add console ec-if-alert-level emergency tellraw {player} {"text":"[Guard] Code Red! All hands on deck!","color":"dark_red"}
```

---

## 📋 **NPC Quick Reference**

### Essential Commands

```bash
# Basic NPC Creation
/npc create <name> --type <type>
/npc skin <name> <skin>
/npc text <name> "Title" "Subtitle"

# Common Traits
/npc trait <name> lookclose
/npc trait <name> equipmenttrait
/npc trait <name> proximitytrait
/npc trait <name> conversationtrait

# Command Assignment
/npc rightclickcommand <name> add <type> <command>
/npc leftclickcommand <name> add <type> <command>

# Positioning and Movement
/npc tp <name>
/npc anchor <name> --save <anchor_name>
/npc speed <name> <speed>
```

### Integration Commands

```bash
# EdenCorrections Integration
/npc rightclickcommand GuardNPC add player duty
/npc rightclickcommand GuardNPC add server corrections wanted check {player}
/npc rightclickcommand GuardNPC add console drugtest {player}

# GUI Integration
/npc rightclickcommand GuardNPC add console deluxemenus open guard_center {player}

# Permission Integration
/npc trait GuardNPC permissiontrait
/npc permissiontrait GuardNPC add edencorrections.guard
```

### Best Practices

1. **Performance**: Use appropriate update intervals for proximity commands
2. **Immersion**: Give NPCs realistic names, skins, and equipment
3. **Functionality**: Combine multiple command types for complex interactions
4. **Maintenance**: Regular cleanup of unused NPCs and commands
5. **Testing**: Always test NPC interactions before deploying

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete NPC Integration Guide* 