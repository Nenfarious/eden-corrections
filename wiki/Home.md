# 🏛️ EdenCorrections Wiki

**Welcome to the official EdenCorrections documentation!**

EdenCorrections is a comprehensive prison guard management plugin for Minecraft servers, designed to create an immersive correctional facility experience with realistic guard duties, prisoner management, and law enforcement mechanics. Whether you're running a roleplay server, prison server, or just want to add sophisticated law enforcement mechanics, EdenCorrections provides everything you need.

---

## 🌟 **Key Features**

### 👮‍♂️ **Advanced Guard System**
- **6-Tier Rank Hierarchy** - Complete progression from Trainee to Warden
- **LuckPerms Integration** - Automatic rank detection and validation
- **Smart Duty Management** - Immobilization periods, region requirements, combat prevention
- **Performance-Based Time Off** - Earn off-duty time through arrests, searches, and performance
- **Automated Kit Management** - Seamless inventory swapping with rank-specific equipment
- **Duty Banking** - Convert duty time to server currency at configurable rates

**Real-World Usage:** Perfect for prison servers where guards need structured duty cycles, roleplay servers with law enforcement, or any server wanting sophisticated guard mechanics.

### ⚡ **Intelligent Wanted System**
- **5-Star Escalation** - Visual ⭐⭐⭐⭐⭐ system with automatic timeout
- **Smart Violation Detection** - Killing guards, contraband possession, combat violations
- **Guard Protection** - On-duty guards cannot receive wanted levels
- **Configurable Duration** - Default 30-minute expiration with admin override
- **Real-time Notifications** - Instant alerts to all on-duty guards

**Integration Examples:**
- **With DeluxeMenus**: Display wanted level in GUI bounty boards
- **With Citizens NPCs**: Check wanted status before allowing shop access
- **With WorldGuard**: Restrict regions based on wanted level

### 🏃‍♂️ **Dynamic Chase System**
- **Distance-Based Tracking** - Automatic termination at 100+ blocks (configurable)
- **Combat Timer Integration** - 5-second combat prevention system
- **Multi-Chase Support** - Handle up to 3 simultaneous pursuits
- **Safe Zone Respect** - WorldGuard region integration prevents chases in safe areas
- **Capture Mechanics** - 3-block proximity requirement with 10-second jail countdown

**Perfect For:** High-action prison escapes, manhunt events, or any scenario requiring pursuit mechanics.

### 🔍 **Advanced Contraband Detection**
- **Multi-Category Scanning** - Weapons (swords), Ranged (bows), Armor, Drugs (configurable items)
- **Compliance System** - 10-second grace period for voluntary surrender
- **Drug Testing Kits** - Instant detection for substance violations
- **Performance Rewards** - Bonus off-duty time for successful searches
- **Customizable Items** - Configure what constitutes contraband per category

**Usage Scenarios:**
- **Prison Searches**: Guards can request contraband checks during routine patrols
- **Entry Screening**: Automatic contraband detection at facility entrances
- **Random Testing**: Drug tests during prisoner interactions

### 🏛️ **Comprehensive Jail System**
- **Smart Sentencing** - Base time (5 minutes) + wanted level multiplier (1 minute per star)
- **Capture Process** - 10-second countdown with movement and distance validation
- **Offline Support** - Queue arrests for players who disconnect
- **Statistics Tracking** - Track guard arrests and prisoner violations
- **Integration Ready** - Works with any jail plugin via console commands

### 💰 **Duty Banking System**
- **Flexible Conversion** - Default 100 seconds = 1 token (configurable)
- **Minimum Thresholds** - Prevents micro-transactions (300-second minimum)
- **Auto-conversion** - Optional automatic conversion at threshold
- **Economy Integration** - Uses custom command system (supports any economy plugin)
- **Performance Bonuses** - Extra time for successful arrests, searches, detections

---

## 🗂️ **Documentation Navigation**

### 🚀 **Getting Started**
- [📦 Setup & Installation](Setup-and-Installation.md) - Complete installation guide with dependencies
- [⚙️ Configuration Guide](Configuration.md) - Detailed config.yml customization
- [🔧 Commands Reference](Commands.md) - All commands with examples and integration tips
- [🛡️ Permissions System](Permissions.md) - Complete permission nodes and hierarchy

### 📚 **Feature Guides**
- [👮‍♂️ Guard Duty System](features/Guard-Duty-System.md) - Duty mechanics, ranks, and performance
- [⭐ Wanted Level System](features/Wanted-System.md) - Violation detection and escalation
- [🏃‍♂️ Chase & Combat System](features/Chase-System.md) - Pursuit mechanics and combat timers
- [🔍 Contraband Detection](features/Contraband-System.md) - Search procedures and compliance
- [⚖️ Jail & Arrest System](features/Jail-System.md) - Capture and sentencing mechanics
- [💳 Duty Banking System](features/Banking-System.md) - Time-to-currency conversion
- [🎯 PlaceholderAPI Integration](features/PlaceholderAPI.md) - 50+ placeholders for external plugins

### 🛠️ **Integration & Development**
- [🔌 Developer API](Developer-API.md) - Plugin integration and event hooks
- [🎮 GUI Integration](Integration-GUI.md) - DeluxeMenus, ChestCommands examples
- [🤖 NPC Integration](Integration-NPCs.md) - Citizens, MythicMobs integration
- [🔧 Troubleshooting](Troubleshooting.md) - Common issues and solutions
- [❓ Frequently Asked Questions](FAQ.md) - Quick answers to common questions

---

## 🎯 **Quick Start Examples**

### Basic Prison Server Setup
```yaml
# 1. Install: LuckPerms, WorldGuard, EdenCorrections
# 2. Create LuckPerms groups: trainee, private, officer, sergeant, captain, warden
# 3. Set permissions: edencorrections.guard.trainee (etc.)
# 4. Create WorldGuard regions: guard_station, prisoner_area, safe_zone
# 5. Configure guard kits in CMI or custom plugin
```

### Integration with DeluxeMenus
```yaml
# Bounty Board GUI showing wanted players
wanted_board:
  items:
    wanted_player:
      material: PLAYER_HEAD
      name: "&c%edencorrections_player_name%"
      lore:
        - "&7Wanted Level: &e⭐ %edencorrections_wanted_stars%"
        - "&7Time Remaining: &f%edencorrections_wanted_time%s"
        - "&7Reason: &f%edencorrections_wanted_reason%"
```

### NPC Guard Commands (Citizens)
```
# Create a guard NPC that checks wanted status
/npc create GuardCheckpoint
/npc command add server corrections wanted check {player}
/npc command add player say I need to check your status, {player}
```

---

## 📊 **System Requirements & Compatibility**

| Component | Requirement | Notes |
|-----------|-------------|-------|
| **Minecraft** | 1.19.4+ | Tested up to 1.20.4 |
| **Java** | 17+ | Required for modern Minecraft |
| **Memory** | 512MB+ | Scales with player count |
| **Database** | SQLite/MySQL | SQLite included, MySQL for networks |

### Plugin Compatibility
| Plugin | Status | Integration Level |
|--------|--------|------------------|
| **LuckPerms** | ✅ Required | Full rank detection |
| **WorldGuard** | ✅ Required | Region-based restrictions |
| **PlaceholderAPI** | ✅ Recommended | 50+ placeholders |
| **CMI** | ✅ Optional | Kit management |
| **Citizens** | ✅ Compatible | NPC command integration |
| **DeluxeMenus** | ✅ Compatible | GUI placeholder support |
| **Vault** | ✅ Compatible | Economy integration |
| **EssentialsX** | ✅ Compatible | No conflicts |

---

## 🚀 **Performance & Scalability**

### Optimized for Large Servers
- **Async Database Operations** - No main thread blocking
- **Intelligent Caching** - 5-minute cache expiry system
- **Configurable Check Intervals** - Tune performance vs. responsiveness
- **Batch Operations** - Efficient multi-player data handling

### Resource Usage
- **CPU**: Minimal impact with default settings
- **Memory**: ~10MB base + ~1KB per active player
- **Database**: ~50KB per 1000 players
- **Network**: Minimal bandwidth usage

---

## 🤝 **Support & Community**

### Getting Help
- **📚 Wiki First**: Check this comprehensive documentation
- **🐛 GitHub Issues**: Report bugs with detailed reproduction steps
- **💡 Feature Requests**: Suggest improvements and new features
- **💬 Discord**: Real-time community support and discussion

### Contributing
- **📝 Documentation**: Help improve this wiki
- **🔧 Code**: Submit pull requests for fixes and features
- **🧪 Testing**: Help test new releases and report issues
- **🎨 Translations**: Contribute language translations

---

## 🎨 **Theming & Customization**

### Default Color Scheme
EdenCorrections uses a modern **Purple & Cyan** theme:
```yaml
Primary: "#9D4EDD"    # Vibrant purple for headers/important elements
Secondary: "#06FFA5"  # Bright cyan for regular text
Accent: "#FFB3C6"     # Soft pink for values/highlights
Success: "#51CF66"    # Fresh green for success messages
Warning: "#FFE066"    # Soft yellow for warnings
Error: "#FFA94D"      # Warm orange for errors (less harsh than red)
Info: "#74C0FC"       # Soft blue for information
Neutral: "#ADB5BD"    # Light gray for neutral text
```

### Customization Tips
- **MiniMessage Format**: Full support for gradients, click events, hover text
- **Placeholder Integration**: Works with any PlaceholderAPI-compatible plugin
- **Modular Messaging**: Each message category can be themed independently
- **Legacy Support**: Automatic conversion for older color codes

---

## 🔄 **Version Information**

| Version | Release Date | Key Features |
|---------|--------------|--------------|
| **2.0.0** | Current | Complete rewrite, modern codebase, enhanced features |
| **1.x.x** | Legacy | Original implementation (not recommended) |

### Upgrade Path
- **From 1.x**: Complete plugin replacement required
- **Database Migration**: Automatic SQLite migration included
- **Configuration**: Manual config.yml update required
- **Permissions**: Update to new permission structure

---

*Last Updated: 2024 | EdenCorrections v2.0.0 | Comprehensive Documentation* 