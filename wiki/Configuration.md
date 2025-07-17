# ⚙️ Configuration Guide

Complete configuration reference for EdenCorrections, covering all systems, integration settings, and performance optimization.

---

## 🎯 **Configuration Overview**

EdenCorrections uses a centralized `config.yml` file with modular sections for each system. This guide covers every configuration option with practical examples and best practices.

### Quick Navigation
| Section | Purpose | Key Features |
|---------|---------|--------------|
| **[Basic Settings](#-basic-settings)** | Core functionality | Debug, language, plugin info |
| **[Guard System](#-guard-system)** | Duty management | Ranks, regions, kits, immobilization |
| **[Wanted System](#-wanted-system)** | Violation tracking | Levels, duration, auto-expiry |
| **[Chase System](#-chase-system)** | Pursuit mechanics | Distance, duration, limits |
| **[Contraband System](#-contraband-system)** | Item detection | Types, compliance, scanning |
| **[Jail System](#-jail-system)** | Arrest mechanics | Time calculation, commands |
| **[Banking System](#-banking-system)** | Duty time conversion | Rates, currency, minimums |
| **[Performance](#-performance-settings)** | Optimization | Intervals, caching, async |
| **[Database](#-database-configuration)** | Data storage | SQLite, MySQL, pooling |
| **[Messages](#-message-customization)** | Text display | Colors, formatting, localization |

---

## 🔧 **Basic Settings**

```yaml
# === CORE CONFIGURATION ===
debug: false                    # Enable debug logging (performance impact)
language: "en"                  # Language code (en, es, fr, de, etc.)
version: "2.0.0"               # Plugin version (auto-managed)
config-version: 3              # Config version for migration

# === PLUGIN INFORMATION ===
plugin:
  name: "EdenCorrections"
  author: "LSDMC Development"
  website: "https://lsdmc.dev"
  support: "https://discord.gg/lsdmc"
```

### Debug Configuration
```yaml
debug-settings:
  enabled: false               # Master debug toggle
  components:
    duty-system: false         # Debug duty changes
    chase-system: false        # Debug chase mechanics  
    contraband: false          # Debug contraband detection
    database: false            # Debug database operations
    performance: false         # Debug performance metrics
  
  # Debug output options
  console-output: true         # Print to console
  file-output: false          # Write to debug.log
  player-feedback: false      # Send debug info to admins
```

---

## 👮‍♂️ **Guard System**

```yaml
guard-system:
  # === DUTY MECHANICS ===
  duty-region: "guard_station"           # WorldGuard region for duty changes
  immobilization-time: 5                 # Seconds player must stand still
  off-duty-time-required: 300           # Minimum seconds needed to go off duty
  
  # === RANK SYSTEM ===
  rank-mappings:
    # Map LuckPerms groups to EdenCorrections ranks
    trainee: "trainee"
    private: "private"  
    officer: "officer"
    sergeant: "sergeant"
    captain: "captain"
    warden: "warden"
  
  # === KIT SYSTEM ===
  kit-enabled: true
  kit-mappings:
    # Map ranks to CMI kit names (requires CMI or compatible kit plugin)
    trainee: "guard_trainee"
    private: "guard_private"
    officer: "guard_officer"
    sergeant: "guard_sergeant"
    captain: "guard_captain"
    warden: "guard_warden"
  
  # === RESTRICTIONS ===
  combat-timer: 5              # Seconds after combat before allowing duty change
  wanted-restriction: true     # Prevent wanted players from going on duty
  
  # === PERFORMANCE SYSTEM ===
  performance:
    arrest-bonus-time: 600     # Bonus seconds for successful arrest (10 minutes)
    search-bonus-time: 300     # Bonus seconds for successful contraband detection (5 minutes)
    detection-bonus-time: 600  # Bonus seconds for drug test detection (10 minutes)
    continuous-duty-bonus: 120 # Bonus seconds per hour of continuous duty (2 minutes)
```

### Advanced Guard Configuration

```yaml
guard-advanced:
  # === DUTY VALIDATION ===
  strict-region-checking: true          # Require exact region match
  allow-duty-in-vehicles: false        # Prevent duty changes in boats/minecarts
  validate-equipment: true             # Check if player has required items
  
  # === RANK PROGRESSION ===
  auto-promotion:
    enabled: false                     # Automatic rank progression
    requirements:
      private:
        arrests: 10
        duty-time: 36000              # 10 hours
      officer:
        arrests: 25
        duty-time: 108000             # 30 hours
        searches: 50
  
  # === DUTY SCHEDULING ===
  shift-system:
    enabled: false
    shifts:
      morning: "06:00-14:00"
      afternoon: "14:00-22:00"
      night: "22:00-06:00"
    required-coverage: 2              # Minimum guards per shift
  
  # === INTEGRATION ===
  worldguard:
    duty-required-regions:            # Regions requiring guards to be on duty
      - "guard_lockers"
      - "guard_armory"
      - "guard_offices"
    
  cmi-integration:
    enabled: true
    kit-command: "cmi kit {kit} {player}"    # Command template for kit giving
```

---

## ⭐ **Wanted System**

```yaml
wanted-system:
  # === LEVEL SYSTEM ===
  max-wanted-level: 5                  # Maximum stars (1-10 supported)
  default-duration: 1800               # Default wanted time (30 minutes)
  
  # === LEVEL CONFIGURATION ===
  levels:
    1:
      duration: 900                    # 15 minutes
      description: "Minor Violation"
      broadcast: false
    2:
      duration: 1200                   # 20 minutes  
      description: "Moderate Offense"
      broadcast: false
    3:
      duration: 1800                   # 30 minutes
      description: "Serious Crime"
      broadcast: true
    4:
      duration: 2700                   # 45 minutes
      description: "Major Crime"
      broadcast: true
      special-effects: true
    5:
      duration: 3600                   # 60 minutes
      description: "Maximum Security Threat"
      broadcast: true
      special-effects: true
      lockdown-trigger: true
  
  # === AUTO-EXPIRY ===
  auto-expire: true                    # Automatically clear wanted levels
  expiry-check-interval: 60           # Check every 60 seconds
  grace-period: 30                     # Extra seconds before expiry
  
  # === BROADCAST SYSTEM ===
  broadcast:
    high-level-threshold: 3            # Broadcast when reaching this level
    broadcast-channels:
      - "guard"                        # Custom channel for guards
      - "admin"                        # Admin notifications
    
  # === SPECIAL FEATURES ===
  effects:
    particle-effects: true            # Show particles around high-wanted players
    sound-alerts: true                # Play sounds for wanted notifications
    glowing-effect: false             # Make wanted players glow (may be OP)
```

### Wanted System Advanced Features

```yaml
wanted-advanced:
  # === ESCALATION SYSTEM ===
  auto-escalation:
    enabled: true
    triggers:
      evading-arrest: 1               # +1 level for evading guards
      attacking-guard: 2              # +2 levels for harming guards
      repeat-offense: 1               # +1 level for repeat violations
  
  # === ZONE RESTRICTIONS ===
  restricted-zones:
    high-security:
      regions: ["admin_area", "vault"]
      min-wanted-level: 3             # Minimum level to restrict access
      action: "teleport-out"          # "teleport-out", "damage", "alert-guards"
  
  # === BOUNTY SYSTEM ===
  bounty:
    enabled: false                    # Player-set bounties
    max-bounty: 10000                # Maximum bounty amount
    guard-bonus-multiplier: 1.5      # Extra reward for guards
```

---

## 🏃‍♂️ **Chase System**

```yaml
chase:
  # === DISTANCE SETTINGS ===
  max-distance: 100                   # Maximum blocks before chase ends
  warning-distance: 20                # Distance to warn about approaching limit
  
  # === TIME LIMITS ===
  max-duration: 300                   # Maximum chase time (5 minutes)
  warning-time: 60                    # Warn when this many seconds remain
  
  # === CHASE LIMITS ===
  max-concurrent: 3                   # Maximum simultaneous chases
  per-guard-limit: 1                  # Chases per guard (1 recommended)
  cooldown-after-end: 30             # Cooldown before starting new chase
  
  # === CAPTURE MECHANICS ===
  capture:
    max-distance: 3                   # Distance required for capture
    countdown-time: 10                # Jail countdown duration
    guard-proximity: 5                # Guard must stay within this distance
    movement-tolerance: 1             # Blocks target can move during countdown
    
  # === COMBAT INTEGRATION ===
  combat:
    prevent-capture-in-combat: true   # No captures during combat
    combat-timer: 5                   # Combat timer duration
    end-chase-on-guard-death: true   # End chase if guard dies
    
  # === PERFORMANCE ===
  check-interval: 5                   # Distance check frequency (seconds)
  async-checks: true                  # Use async distance calculations
```

### Advanced Chase Features

```yaml
chase-advanced:
  # === CHASE ZONES ===
  no-chase-zones:
    - "safezone"
    - "spawn"
    - "newbie_area"
  
  chase-only-zones:
    - "prison_yard"
    - "exercise_area"
  
  # === CHASE EFFECTS ===
  effects:
    guard-speed-boost: true           # Speed effect for chasing guards
    target-particles: true            # Show particles on chase target
    sound-effects: true               # Chase-related sounds
    
  # === BACKUP SYSTEM ===
  backup:
    auto-notify-guards: true          # Alert other guards
    notification-radius: 100          # Alert guards within this distance
    backup-speed-boost: true          # Extra speed for responding guards
  
  # === CHASE TERMINATION ===
  termination:
    safe-zone-grace-period: 5         # Seconds to leave safe zone
    altitude-limit: 256               # Maximum Y-level for chases
    world-restrictions: ["world"]     # Worlds where chases are allowed
```

---

## 🔍 **Contraband System**

```yaml
contraband:
  # === BASIC SETTINGS ===
  enabled: true                       # Master contraband toggle
  max-request-distance: 5             # Maximum distance for contraband requests
  grace-period: 10                    # Compliance countdown time
  
  # === CONTRABAND TYPES ===
  types:
    # Weapon contraband
    sword:
      items: "WOODEN_SWORD,STONE_SWORD,IRON_SWORD,GOLDEN_SWORD,DIAMOND_SWORD,NETHERITE_SWORD"
      description: "Bladed weapons and sharp objects"
      search-time: 10
      violation-level: 2
      
    bow:
      items: "BOW,CROSSBOW,ARROW,SPECTRAL_ARROW,TIPPED_ARROW"
      description: "Ranged weapons and ammunition"
      search-time: 10
      violation-level: 2
      
    # Armor contraband
    armor:
      items: "LEATHER_HELMET,LEATHER_CHESTPLATE,IRON_HELMET,IRON_CHESTPLATE,DIAMOND_HELMET,DIAMOND_CHESTPLATE,NETHERITE_HELMET,NETHERITE_CHESTPLATE"
      description: "Protective equipment and armor"
      search-time: 15
      violation-level: 1
      
    # Drug contraband
    drugs:
      items: "SUGAR,NETHER_WART,SPIDER_EYE,FERMENTED_SPIDER_EYE,BLAZE_POWDER,REDSTONE"
      description: "Illegal substances and drugs"
      search-time: 5
      violation-level: 3
      instant-test: true              # Drug testing is instant
  
  # === SEARCH MECHANICS ===
  search:
    cooldown-per-guard: 30           # Seconds between searches by same guard
    cooldown-per-target: 60          # Seconds before same target can be searched again
    success-bonus-time: 300          # Bonus duty time for successful searches
    
  # === COMPLIANCE SYSTEM ===
  compliance:
    boss-bar-enabled: true            # Show countdown boss bar
    particle-effects: true           # Show particles during countdown
    sound-effects: true              # Play countdown sounds
    voluntary-bonus: true            # Reward voluntary compliance
```

### Advanced Contraband Configuration

```yaml
contraband-advanced:
  # === CUSTOM CONTRABAND ===
  custom-types:
    explosives:
      items: "TNT,DYNAMITE,FIREWORK_ROCKET"
      description: "Explosive materials"
      search-time: 20
      violation-level: 4
      special-handling: true
      
    technology:
      items: "REDSTONE,REPEATER,COMPARATOR,PISTON"
      description: "Restricted technology"
      search-time: 15
      violation-level: 2
  
  # === DETECTION ZONES ===
  detection-zones:
    high-security:
      regions: ["vault", "admin_area"]
      auto-scan: true
      scan-interval: 30
      all-items-contraband: true
      
    entry-points:
      regions: ["main_entrance", "visitor_gate"]
      auto-scan: true
      scan-types: ["drugs", "weapons"]
  
  # === INTEGRATION ===
  integration:
    chest-detection: true             # Scan nearby chests
    item-frame-detection: true        # Check item frames
    enderchest-detection: false       # Scan ender chests (privacy concern)
```

---

## ⚖️ **Jail System**

```yaml
jail:
  # === TIME CALCULATION ===
  base-time: 300                      # Base jail time (5 minutes)
  level-multiplier: 60                # Additional time per wanted star (1 minute)
  max-jail-time: 1800                # Maximum possible jail time (30 minutes)
  
  # === JAIL INTEGRATION ===
  jail-plugin: "essentials"           # "essentials", "cmi", "custom"
  
  # Plugin-specific commands
  commands:
    essentials: "jail {player} {time} {reason}"
    cmi: "cmi jail {player} {time}s {reason}"
    custom: "customprison jail {player} -t {time} -r '{reason}'"
  
  # === OFFLINE JAILING ===
  offline-jail:
    enabled: true                     # Allow jailing offline players
    login-notification: true          # Notify when offline-jailed player logs in
    queue-limit: 100                  # Maximum queued offline jails
  
  # === JAIL PROCESS ===
  process:
    countdown-time: 10                # Arrest countdown duration
    movement-restriction: 1           # Blocks player can move during countdown
    cancel-on-combat: true           # Cancel arrest if combat starts
    
  # === STATISTICS ===
  stats:
    track-guard-arrests: true         # Count arrests per guard
    track-player-violations: true     # Count violations per player
    performance-bonus: 600            # Bonus duty time for arrest (10 minutes)
```

### Advanced Jail Configuration

```yaml
jail-advanced:
  # === JAIL CONDITIONS ===
  conditions:
    minimum-wanted-level: 1           # Minimum wanted level to jail
    require-chase-first: false        # Must chase before arresting
    guard-witness-required: false     # Require another guard as witness
  
  # === SPECIAL JAILING ===
  special-cases:
    high-priority:
      wanted-level: 4
      multiplier: 2.0                # Double jail time
      special-prison: "max_security"
      
    repeat-offender:
      violation-count: 5
      multiplier: 1.5
      extended-monitoring: true
  
  # === BAIL SYSTEM ===
  bail:
    enabled: false
    base-cost: 1000
    level-multiplier: 500
    max-bail: 10000
```

---

## 💰 **Banking System**

```yaml
duty-banking:
  # === BASIC SETTINGS ===
  enabled: true                       # Master banking toggle
  conversion-rate: 100                # Seconds per token (100s = 1 token)
  minimum-conversion: 300             # Minimum seconds to convert (5 minutes)
  
  # === CURRENCY INTEGRATION ===
  currency-command: "et give {player} {amount}"    # EconomyTokens example
  # Other examples:
  # currency-command: "eco give {player} {amount}"           # EssentialsX
  # currency-command: "tokens give {player} {amount}"        # TokenManager
  # currency-command: "cmi money give {player} {amount}"     # CMI
  
  # === AUTO-CONVERSION ===
  auto-conversion:
    enabled: false                    # Automatically convert at threshold
    threshold: 3600                   # Auto-convert when reaching 1 hour
    notification: true                # Notify player about auto-conversion
  
  # === BONUS SYSTEM ===
  bonuses:
    successful-arrest: 600            # 10 minutes bonus
    contraband-detection: 300         # 5 minutes bonus
    drug-test-positive: 600          # 10 minutes bonus
    continuous-duty: 120             # 2 minutes per hour bonus
    perfect-shift: 1800              # 30 minutes for 8-hour shift without violations
  
  # === BANKING LIMITS ===
  limits:
    max-conversion-per-day: 86400     # 24 hours worth per day
    max-stored-time: 259200          # Maximum storable time (72 hours)
    conversion-cooldown: 300          # 5 minutes between conversions
```

### Advanced Banking Features

```yaml
banking-advanced:
  # === PERFORMANCE METRICS ===
  performance-tracking:
    enabled: true
    metrics:
      efficiency-rating: true         # Track guard efficiency
      response-time: true            # Track emergency response times
      accuracy-rating: true          # Track contraband detection accuracy
  
  # === INVESTMENT SYSTEM ===
  investment:
    enabled: false
    interest-rate: 0.05              # 5% daily interest
    compound-frequency: "daily"       # "hourly", "daily", "weekly"
    max-investment: 168              # Max hours that can earn interest
  
  # === PAYROLL SYSTEM ===
  payroll:
    enabled: false
    base-pay: 50                     # Tokens per hour base pay
    rank-multipliers:
      trainee: 1.0
      private: 1.2
      officer: 1.5
      sergeant: 2.0
      captain: 2.5
      warden: 3.0
```

---

## 🌍 **Regions Configuration**

```yaml
regions:
  # === WORLDGUARD INTEGRATION ===
  worldguard:
    enabled: true
    default-world: "world"
    
  # === CORE REGIONS ===
  duty-region: "guard_station"        # Where guards can toggle duty
  
  # === NO-CHASE ZONES ===
  no-chase-zones:
    - "safezone"
    - "spawn"
    - "newbie_area"
    - "visitor_center"
    - "medical_bay"
  
  # === DUTY-REQUIRED ZONES ===
  duty-required-zones:
    - "guard_lockers"
    - "guard_armory"
    - "guard_offices"
    - "guardplotstairs"
    - "evidence_room"
  
  # === CONTRABAND DETECTION ZONES ===
  auto-scan-zones:
    - "main_entrance"
    - "visitor_gate"
    - "checkpoint_alpha"
  
  # === SPECIAL ZONES ===
  special-zones:
    high-security:
      regions: ["vault", "admin_area"]
      wanted-level-required: 0        # Only non-wanted players
      guard-escort-required: true
      
    medical-area:
      regions: ["medical_bay", "infirmary"]
      no-arrests: true
      healing-effects: true
      
    exercise-yard:
      regions: ["yard", "recreation"]
      enhanced-monitoring: true
      contraband-scanning: true
```

---

## 📊 **Performance Settings**

```yaml
performance:
  # === CHECK INTERVALS ===
  chase-check-interval: 5             # Seconds between chase distance checks
  wanted-check-interval: 60           # Seconds between wanted expiry checks
  cleanup-interval: 300               # Seconds between data cleanup
  autosave-interval: 1800             # Seconds between database saves
  
  # === ASYNC OPERATIONS ===
  async-operations: true              # Use async for database operations
  async-thread-pool-size: 4          # Number of async threads
  
  # === CACHING ===
  cache:
    player-data: true                 # Cache player data in memory
    cache-size: 1000                 # Maximum cached players
    cache-ttl: 3600                  # Cache time-to-live (seconds)
    
  # === DATABASE OPTIMIZATION ===
  database:
    connection-pool-size: 10          # Maximum database connections
    connection-timeout: 30000         # Connection timeout (milliseconds)
    query-timeout: 15000             # Query timeout (milliseconds)
    
  # === MEMORY MANAGEMENT ===
  memory:
    cleanup-inactive-data: true       # Clean up data for offline players
    inactive-threshold: 86400         # Consider data inactive after 24 hours
    gc-suggestions: true              # Suggest garbage collection
```

### Advanced Performance Configuration

```yaml
performance-advanced:
  # === MONITORING ===
  monitoring:
    enabled: true
    metrics-collection: true
    performance-warnings: true
    lag-detection: true
    
  # === OPTIMIZATION ===
  optimization:
    batch-database-operations: true
    compress-data: true
    use-prepared-statements: true
    optimize-queries: true
    
  # === RESOURCE LIMITS ===
  limits:
    max-concurrent-chases: 10
    max-wanted-players: 100
    max-database-connections: 20
    max-memory-usage: "512MB"
```

---

## 🗄️ **Database Configuration**

```yaml
database:
  # === DATABASE TYPE ===
  type: "sqlite"                     # "sqlite" or "mysql"
  
  # === SQLITE CONFIGURATION ===
  sqlite:
    file: "edencorrections.db"        # Database file name
    journal-mode: "WAL"               # Journal mode for performance
    synchronous: "NORMAL"             # Synchronization level
    
  # === MYSQL CONFIGURATION ===
  mysql:
    host: "localhost"
    port: 3306
    database: "edencorrections"
    username: "eden_user"
    password: "your_secure_password"
    
    # Connection pool settings
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
    # SSL settings
    ssl:
      enabled: false
      trust-certificate: true
      verify-certificate: false
      
    # Advanced MySQL settings
    properties:
      useSSL: false
      autoReconnect: true
      cachePrepStmts: true
      prepStmtCacheSize: 250
      prepStmtCacheSqlLimit: 2048
      useServerPrepStmts: true
      useLocalSessionState: true
      rewriteBatchedStatements: true
      cacheResultSetMetadata: true
      cacheServerConfiguration: true
      elideSetAutoCommits: true
      maintainTimeStats: false
  
  # === BACKUP SETTINGS ===
  backup:
    enabled: true
    interval: 21600                   # Backup every 6 hours
    keep-backups: 7                   # Keep 7 backup files
    compression: true                 # Compress backup files
    
  # === MIGRATION SETTINGS ===
  migration:
    auto-migrate: true                # Automatically update database schema
    backup-before-migration: true     # Backup before schema changes
```

---

## 💬 **Message Customization**

```yaml
messages:
  # === GLOBAL SETTINGS ===
  prefix: "<gradient:#9D4EDD:#06FFA5>[EdenCorrections]</gradient> "
  
  # Color scheme (user's preferred vibrant Purple & Cyan)
  colors:
    primary: "#9D4EDD"               # Vibrant purple for headers/important
    secondary: "#06FFA5"             # Bright cyan for regular text
    accent: "#FFB3C6"                # Soft pink for values/highlights
    error: "#FF6B6B"                 # Coral red for warnings/errors
    success: "#51CF66"               # Fresh green for success
    neutral: "#ADB5BD"               # Light gray for neutral text
  
  # === DUTY SYSTEM MESSAGES ===
  duty:
    activation:
      immobilization-start: "<color:#9D4EDD>Stand still for {time} seconds to go on duty...</color>"
      immobilization-moved: "<color:#FF6B6B>Movement detected! Duty change cancelled.</color>"
      success: "<color:#51CF66>You are now on duty as a {rank}!</color>"
      kit-given: "<color:#06FFA5>Equipment issued for rank: {rank}</color>"
      
    deactivation:
      success: "<color:#FFE066>You are now off duty. Thank you for your service!</color>"
      performance-summary: "<color:#9D4EDD>Duty Summary:</color> <color:#06FFA5>{time} seconds on duty, {arrests} arrests, {searches} searches</color>"
      
    errors:
      not-in-region: "<color:#FF6B6B>You must be in the guard station to change duty status!</color>"
      already-on-duty: "<color:#FF6B6B>You are already on duty!</color>"
      already-off-duty: "<color:#FF6B6B>You are already off duty!</color>"
      no-rank: "<color:#FF6B6B>No valid guard rank detected!</color>"
      in-combat: "<color:#FF6B6B>Cannot change duty status while in combat!</color>"
      wanted: "<color:#FF6B6B>Wanted individuals cannot go on duty!</color>"
      insufficient-time: "<color:#FF6B6B>You need {time} more seconds of earned time to go off duty!</color>"
  
  # === WANTED SYSTEM MESSAGES ===
  wanted:
    level-set: "<color:#FF6B6B>Your wanted level has been set to {level} stars. Reason: {reason}</color>"
    level-increased: "<color:#FF6B6B>Your wanted level increased to {level} stars! {reason}</color>"
    level-cleared: "<color:#51CF66>Your wanted status has been cleared.</color>"
    time-remaining: "<color:#FFB3C6>Wanted level {level} - Time remaining: {time}</color>"
    
    broadcast:
      new-wanted: "<color:#FF6B6B>ALERT:</color> <color:#9D4EDD>{player}</color> <color:#06FFA5>is now wanted - Level {level} ({reason})</color>"
      high-priority: "<color:#FF6B6B>HIGH PRIORITY ALERT:</color> <color:#9D4EDD>{player}</color> <color:#FF6B6B>- Maximum security threat!</color>"
  
  # === CHASE SYSTEM MESSAGES ===
  chase:
    started: "<color:#06FFA5>Chase initiated with {target}! Stay within {distance} blocks.</color>"
    target-notified: "<color:#FF6B6B>You are being pursued by guard {guard}!</color>"
    distance-warning: "<color:#FFE066>Warning: Target is {distance} blocks away! Chase will end at {max_distance} blocks.</color>"
    ended-distance: "<color:#FF6B6B>Chase ended - target too far away!</color>"
    ended-timeout: "<color:#FF6B6B>Chase ended - time limit exceeded!</color>"
    ended-safezone: "<color:#06FFA5>Chase ended - target reached safe zone.</color>"
    
    capture:
      initiated: "<color:#9D4EDD>Capture initiated! Hold position for {time} seconds...</color>"
      countdown: "<color:#FFB3C6>Arrest countdown: {time} seconds remaining</color>"
      success: "<color:#51CF66>Target captured successfully!</color>"
      failed-movement: "<color:#FF6B6B>Capture failed - target moved too far!</color>"
      failed-guard-distance: "<color:#FF6B6B>Capture failed - guard too far away!</color>"
  
  # === CONTRABAND MESSAGES ===
  contraband:
    request-sent: "<color:#9D4EDD>Contraband search requested for {type}. Please surrender items within {time} seconds.</color>"
    countdown: "<color:#FFB3C6>Surrender countdown: {time} seconds remaining</color>"
    voluntary-success: "<color:#51CF66>Thank you for your cooperation.</color>"
    violation-detected: "<color:#FF6B6B>Contraband violation detected! Wanted level increased.</color>"
    drug-test-positive: "<color:#FF6B6B>Drug test POSITIVE for {player}! Substances detected: {items}</color>"
    drug-test-negative: "<color:#51CF66>Drug test NEGATIVE for {player}.</color>"
    
  # === JAIL SYSTEM MESSAGES ===
  jail:
    countdown: "<color:#9D4EDD>Arrest countdown: {time} seconds. Remain still!</color>"
    success: "<color:#51CF66>Player {player} has been jailed for {time} seconds. Reason: {reason}</color>"
    offline-queued: "<color:#06FFA5>Offline jail queued for {player}. They will be jailed when they log in.</color>"
    
  # === BANKING MESSAGES ===
  banking:
    status: "<color:#9D4EDD>Banking Status:</color> <color:#06FFA5>{time} seconds ({tokens} tokens available)</color>"
    conversion-success: "<color:#51CF66>Converted {time} seconds to {tokens} tokens!</color>"
    insufficient-time: "<color:#FF6B6B>You need at least {required} seconds to convert (you have {current}).</color>"
    bonus-awarded: "<color:#FFB3C6>Performance bonus: +{time} seconds ({reason})</color>"
```

---

## 🎮 **Integration Examples**

### GUI Plugin Integration

**DeluxeMenus Example**:
```yaml
# Reference the GUI Integration Guide for complete examples
gui-integration:
  deluxemenus:
    enabled: true
    main-menu: "guard_center"
    contraband-scanner: "contraband_scanner"
    banking-terminal: "banking_terminal"
    
  placeholderapi:
    enabled: true
    expansion: "EdenCorrections"
    custom-placeholders: true
```

**For complete GUI integration examples, see:** [GUI Integration Guide](Integration-GUI.md)

### NPC Plugin Integration

**Citizens Example**:
```yaml
# Reference the NPC Integration Guide for complete examples
npc-integration:
  citizens:
    enabled: true
    guard-station-npc: "GuardCaptain"
    checkpoint-npcs: ["Checkpoint1", "Checkpoint2"]
    
  mythicmobs:
    enabled: true
    security-turrets: true
    guard-bosses: true
```

**For complete NPC integration examples, see:** [NPC Integration Guide](Integration-NPCs.md)

### Developer API Access

**Plugin Integration**:
```yaml
# Reference the Developer API Guide for complete examples
api-integration:
  developer-mode: false
  api-access-logging: true
  event-broadcasting: true
  
  custom-extensions:
    - "CustomRewards"
    - "AdvancedStatistics" 
    - "DiscordIntegration"
```

**For complete API documentation, see:** [Developer API Reference](Developer-API.md)

---

## 🔧 **Practical Configuration Examples**

### Small Server Setup (1-20 players)

```yaml
# Optimized for smaller communities
performance:
  chase-check-interval: 10
  wanted-check-interval: 120
  
chase:
  max-concurrent: 2
  max-duration: 180
  
contraband:
  grace-period: 15
  
database:
  type: "sqlite"
  
duty-banking:
  conversion-rate: 60      # Faster progression
  minimum-conversion: 180  # Lower barrier
```

### Large Server Setup (100+ players)

```yaml
# Optimized for high-population servers
performance:
  chase-check-interval: 5
  wanted-check-interval: 30
  async-operations: true
  cache:
    player-data: true
    cache-size: 2000
    
chase:
  max-concurrent: 10
  max-duration: 300
  
database:
  type: "mysql"
  mysql:
    pool:
      maximum-pool-size: 20
      minimum-idle: 5
      
duty-banking:
  conversion-rate: 120     # Slower progression
  minimum-conversion: 600  # Higher barrier for balance
```

### Roleplay Server Setup

```yaml
# Enhanced realism and immersion
guard-system:
  immobilization-time: 10   # Longer duty changes for realism
  shift-system:
    enabled: true
    
wanted-system:
  max-wanted-level: 10      # More granular threat levels
  levels:
    # Custom 10-level system with detailed descriptions
    
contraband:
  grace-period: 20          # More realistic search time
  enhanced-descriptions: true
  
jail:
  realistic-sentences: true
  base-time: 900           # Longer base sentences
```

### High-Security Prison Setup

```yaml
# Maximum security configuration
wanted-system:
  max-wanted-level: 5
  auto-escalation:
    enabled: true
    strict-penalties: true
    
chase:
  max-distance: 50         # Tighter chase limits
  no-chase-zones: []       # No safe zones except spawn
  
contraband:
  auto-scan-zones:
    - "all_areas"          # Scan everywhere
  enhanced-detection: true
  zero-tolerance: true
  
regions:
  lockdown-capable: true
  emergency-protocols: true
```

---

## 🛠️ **Configuration Validation**

### Config Checker Commands

```bash
# Validate configuration
/corrections config validate

# Check specific sections
/corrections config check guard-system
/corrections config check database

# Test database connection
/corrections config test-database

# Reload configuration
/edenreload
```

### Common Configuration Errors

```yaml
# WRONG - Invalid region name
duty-region: "guard station"    # Spaces not allowed in region names

# CORRECT - Valid region name  
duty-region: "guard_station"

# WRONG - Invalid time format
max-duration: "5 minutes"       # Must be in seconds

# CORRECT - Numeric seconds
max-duration: 300

# WRONG - Invalid color format
colors:
  primary: "purple"             # Must be hex color

# CORRECT - Hex color format
colors:
  primary: "#9D4EDD"
```

### Migration Guide

**Updating from v1.x to v2.0**:
```yaml
# Backup your old config.yml first!
# Run the migration command:
# /corrections migrate config

# New v2.0 features that need configuration:
performance:          # New performance section
  cache: {}

integration:          # New integration options
  gui: {}
  npc: {}

advanced-features:    # New advanced features
  analytics: {}
  automation: {}
```

---

## 📋 **Configuration Quick Reference**

### Essential Settings Checklist

```yaml
# Required for basic functionality
✓ guard-system.duty-region          # Set your guard station region
✓ guard-system.rank-mappings        # Map your LuckPerms groups
✓ database.type                     # Choose sqlite or mysql
✓ messages.prefix                   # Customize your prefix

# Recommended for production
✓ performance.async-operations      # Enable for better performance
✓ database.backup.enabled          # Enable automatic backups
✓ contraband.enabled               # Enable contraband system
✓ duty-banking.enabled             # Enable banking system

# Optional enhancements
□ integration.gui.enabled           # GUI plugin integration
□ integration.npc.enabled           # NPC plugin integration
□ advanced-features.analytics       # Performance analytics
□ custom-extensions                 # Developer API usage
```

### Performance Impact Settings

| Setting | Impact | Recommendation |
|---------|--------|----------------|
| `debug: true` | High | Only enable for troubleshooting |
| `chase-check-interval: 1` | High | Minimum value: 5 |
| `cache.player-data: false` | Medium | Enable for 50+ players |
| `async-operations: false` | Medium | Enable for production |
| `backup.interval: 60` | Low | Minimum: 3600 (1 hour) |

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Complete Configuration Guide* 