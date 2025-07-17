# ❓ Frequently Asked Questions

Common questions and answers about EdenCorrections plugin.

---

## 🚀 **Installation & Setup**

### Q: What plugins do I need to install EdenCorrections?
**A:** EdenCorrections requires:
- **LuckPerms** (required) - For guard rank management
- **WorldGuard** (required) - For region-based restrictions
- **PlaceholderAPI** (optional) - For enhanced placeholder support
- **CMI** (optional) - For guard kit management

### Q: Can I use EdenCorrections without LuckPerms?
**A:** No, LuckPerms is required for the guard rank system to function. The plugin uses LuckPerms groups to determine guard ranks and permissions.

### Q: Does EdenCorrections work with PermissionsEx or other permission plugins?
**A:** EdenCorrections is designed specifically for LuckPerms integration. While basic permissions might work with other plugins, the rank detection system requires LuckPerms.

### Q: What Minecraft versions are supported?
**A:** EdenCorrections supports Minecraft 1.21.4+ and requires Java 21+.

---

## 👮‍♂️ **Guard System**

### Q: How do guards earn off-duty time?
**A:** Guards earn off-duty time through:
- **Base time**: 30 minutes after 15 minutes of duty
- **Performance bonuses**: Searches, arrests, detections
- **Continuous duty**: 2 minutes per hour on duty
- **Combat bonuses**: Eliminations while on duty

### Q: Can guards go off duty anywhere?
**A:** No, guards must be in specific regions to go off duty:
- Guard station region
- Any duty-required zone (configurable)
- Valid off-duty regions as configured

### Q: What happens if a guard disconnects while on duty?
**A:** When a guard disconnects:
- Their duty time is automatically saved
- Any active chases are ended
- Their inventory is safely stored
- They can resume duty normally when reconnecting

### Q: Can guards be wanted while on duty?
**A:** No, guards on duty cannot be set as wanted. This protection prevents abuse and maintains law enforcement integrity.

---

## ⭐ **Wanted System**

### Q: How long do wanted levels last?
**A:** By default, wanted levels last 30 minutes. This can be configured in the config.yml file under `times.wanted-duration`.

### Q: Can admins set custom wanted levels?
**A:** Yes, admins can use `/corrections wanted set <player> <level> [reason]` to set any wanted level from 1-5 with custom reasons.

### Q: What happens when a wanted player is arrested?
**A:** When arrested:
- Wanted level is cleared
- Jail time is calculated based on wanted level
- Statistics are updated
- Guard receives performance bonus

### Q: Can wanted players teleport?
**A:** No, wanted players cannot use teleportation commands including `/home`, `/spawn`, `/warp`, etc.

---

## 🏃‍♂️ **Chase System**

### Q: How close do guards need to be to start a chase?
**A:** Guards must be within the configured `chase.max-distance` (default: 100 blocks) to start a chase.

### Q: Can guards chase players in safe zones?
**A:** Chases automatically end when the target enters a safe zone (no-chase zone). This provides balanced gameplay mechanics.

### Q: What is the combat timer and how does it work?
**A:** The combat timer:
- Activates when players damage each other
- Lasts 5 seconds by default
- Prevents capturing during combat
- Blocks teleportation
- Shows visual countdown

### Q: How many chases can happen simultaneously?
**A:** The server can handle up to 3 concurrent chases by default. This can be adjusted in the configuration.

---

## 🔍 **Contraband System**

### Q: What types of contraband can be detected?
**A:** EdenCorrections detects:
- **Weapons**: Swords, bows, crossbows
- **Armor**: All armor types
- **Drugs**: Sugar, nether wart, spider eyes, etc.
- **Custom items**: Configurable contraband types

### Q: How long do players have to surrender contraband?
**A:** Players have 10 seconds by default to surrender requested contraband. This can be configured under `times.contraband-compliance`.

### Q: Can guards search other guards?
**A:** Guards cannot search other guards who are on duty. This prevents abuse and maintains system integrity.

### Q: What happens if a player doesn't surrender contraband?
**A:** If a player fails to surrender contraband within the time limit, their wanted level automatically increases by 1.

---

## 💳 **Banking System**

### Q: How does the duty banking system work?
**A:** The banking system:
- Converts duty time to server currency
- Default rate: 100 seconds = 1 token
- Requires minimum 5 minutes for conversion
- Supports various economy plugins

### Q: Can banking be automated?
**A:** Yes, auto-conversion can be enabled in the configuration. When enabled, duty time is automatically converted when reaching the threshold.

### Q: What economy plugins are supported?
**A:** EdenCorrections supports:
- **CoinsEngine** (default)
- **EssentialsX**
- **TokenManager**
- Any plugin accepting console commands

---

## 🗄️ **Database & Storage**

### Q: What database types are supported?
**A:** EdenCorrections supports:
- **SQLite** (default) - File-based database
- **MySQL** - Server-based database for multiple servers

### Q: How do I migrate from SQLite to MySQL?
**A:** To migrate:
1. Set up MySQL database
2. Update config.yml with MySQL credentials
3. Restart server (automatic migration)
4. Verify data integrity

### Q: Are player inventories safe during duty transitions?
**A:** Yes, player inventories are:
- Serialized and stored in the database
- Automatically restored when going off duty
- Protected from server crashes
- Cleaned up automatically

---

## 🌍 **WorldGuard Integration**

### Q: What regions do I need to create?
**A:** Essential regions:
- **guard_station** - Where guards go on/off duty
- **safezone** - No-chase zones
- **duty-required zones** - Areas requiring duty status

### Q: Can I have multiple guard stations?
**A:** Currently, only one guard station is supported. Multiple duty-required zones can be configured for off-duty transitions.

### Q: How do I create no-chase zones?
**A:** Configure no-chase zones in config.yml:
```yaml
regions:
  no-chase-zones: "safezone,visitor_area,medical_bay"
```

---

## 🔧 **Configuration & Customization**

### Q: Can I customize the messages?
**A:** Yes, all messages are fully customizable using MiniMessage formatting. Edit the `messages` section in config.yml.

### Q: How do I add custom contraband types?
**A:** Add custom contraband types in config.yml:
```yaml
contraband:
  types:
    explosives:
      items: "TNT,GUNPOWDER,FIRE_CHARGE"
      description: "Explosive materials"
```

### Q: Can I adjust the performance bonuses?
**A:** Yes, all performance bonuses are configurable under `guard-system.off-duty-earning.performance-bonuses`.

---

## 🎯 **PlaceholderAPI**

### Q: What placeholders are available?
**A:** EdenCorrections provides placeholders for:
- Duty status and rank
- Wanted level and time
- Chase status
- Banking information
- Performance statistics

### Q: How do I use placeholders in other plugins?
**A:** Example usage:
```
%edencorrections_duty_status%
%edencorrections_wanted_level%
%edencorrections_wanted_stars%
%edencorrections_banking_tokens%
```

---

## 🚨 **Common Issues**

### Q: Guards can't go on duty - "No valid guard rank"
**A:** This indicates:
- Player doesn't have a guard rank in LuckPerms
- Rank mapping is incorrect in config.yml
- LuckPerms groups don't match configuration

**Solution**: Verify LuckPerms groups and config mappings.

### Q: Wanted levels aren't expiring
**A:** Check:
- System time synchronization
- Wanted cleanup task is running
- Database connectivity
- Configuration times

### Q: Players are losing items during duty transitions
**A:** Verify:
- Database connectivity
- Inventory serialization is working
- Clean plugin shutdown procedures
- Backup/restore functionality

### Q: Chase system isn't working
**A:** Common causes:
- Guard not on duty
- Target not wanted
- Distance too great
- WorldGuard region issues

---

## 📊 **Performance & Optimization**

### Q: How do I optimize EdenCorrections for large servers?
**A:** For large servers (100+ players):
- Use MySQL instead of SQLite
- Reduce check intervals
- Increase concurrent limits
- Monitor database performance

### Q: What are the recommended settings for small servers?
**A:** For small servers (1-50 players):
- Keep SQLite database
- Increase check intervals
- Reduce concurrent limits
- Lower performance thresholds

### Q: How do I monitor plugin performance?
**A:** Use commands:
```bash
/corrections system stats
/corrections system debug on
```

---

## 🔄 **Updates & Maintenance**

### Q: How do I update EdenCorrections?
**A:** To update:
1. Backup your config and database
2. Stop the server
3. Replace the plugin jar file
4. Start the server
5. Check for new config options

### Q: Are my configurations preserved during updates?
**A:** Yes, but new options may be added. Always backup your config before updating.

### Q: Can I rollback to a previous version?
**A:** Yes, but you may need to restore database backups if the new version made schema changes.

---

## 🛡️ **Security & Permissions**

### Q: How secure is the guard protection system?
**A:** The guard protection system:
- Uses LuckPerms for rank validation
- Prevents duty bypass attempts
- Logs all protection events
- Provides debug information

### Q: Can players bypass the wanted system?
**A:** No, the wanted system:
- Blocks teleportation
- Persists across sessions
- Cannot be bypassed by guards
- Logs all wanted changes

### Q: What happens if LuckPerms fails?
**A:** If LuckPerms fails:
- Guards cannot go on duty
- Rank detection fails
- System enters safe mode
- Debug information is logged

---

## 🎮 **Gameplay Balance**

### Q: How balanced are the jail times?
**A:** Jail times are calculated as:
```
Jail Time = base-time + (wanted-level × level-multiplier)
```
Default: 5 minutes base + 1 minute per star

### Q: Can guards abuse the system?
**A:** Guards cannot:
- Set themselves as wanted while on duty
- Search other on-duty guards
- Bypass combat timers
- Abuse performance bonuses

### Q: How do I prevent guard vs guard combat?
**A:** Configure appropriate:
- Guard permissions
- Server rules
- WorldGuard regions
- Administrative policies

---

## 📱 **External Integration**

### Q: Can I integrate EdenCorrections with Discord?
**A:** Yes, using the Developer API you can:
- Send chase alerts to Discord
- Monitor wanted levels
- Track performance statistics
- Provide real-time updates

### Q: Does EdenCorrections work with other prison plugins?
**A:** EdenCorrections is designed to be compatible with most prison plugins. It provides comprehensive APIs for integration.

### Q: Can I use custom GUIs with EdenCorrections?
**A:** Yes, using the API you can create custom GUIs that display guard information, wanted levels, and other data.

---

## 🔗 **Additional Resources**

### Q: Where can I get help if my question isn't answered here?
**A:** Additional support:
- **Discord Community**: Join our server for real-time help
- **GitHub Issues**: Report bugs and feature requests
- **Documentation**: Check other wiki pages
- **Email Support**: Contact the development team

### Q: How do I contribute to EdenCorrections?
**A:** You can contribute by:
- Reporting bugs
- Suggesting features
- Submitting pull requests
- Improving documentation
- Testing new releases

### Q: Are there video tutorials available?
**A:** Check our YouTube channel for setup tutorials and feature demonstrations.

---

## 🎯 **Quick Reference**

### Essential Commands
```bash
/duty                           # Toggle duty status
/chase <player>                # Start chase
/jail <player> [reason]        # Arrest player
/corrections wanted list       # Check wanted players
/dutybank status              # Check banking info
```

### Important Config Sections
```yaml
guard-system:                  # Guard configuration
  duty-region: "guard_station"
  rank-mappings: ...
  
times:                         # Time settings
  wanted-duration: 1800
  chase-duration: 300
  
chase:                         # Chase settings
  max-distance: 100
  max-concurrent: 3
```

### Common Permissions
```yaml
edencorrections.guard         # Basic guard access
edencorrections.guard.chase   # Chase permissions
edencorrections.guard.jail    # Jail permissions
edencorrections.admin         # Administrative access
```

---

*Still have questions? Check our other documentation pages or join our community Discord server for real-time support!* 