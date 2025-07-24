# EdenCorrections

A fully custom guard management system for Eden Prison S4. EdenCorrections provides a complete guard duty system with dynamic wanted levels, chase mechanics, contraband management, and integrated economy features.

## Features

### Guard System
- **Rank-based duty system** with LuckPerms integration
- **WorldGuard region restrictions** for duty activation
- **Automatic kit distribution** via CMI integration
- **Inventory caching** during duty transitions
- **Duty banking** - convert duty time to economy tokens

### Criminal Management
- **Dynamic wanted levels** (1-5 stars) with automatic decay
- **Realistic chase mechanics** with distance tracking and safe zones
- **Configurable jail sentences** based on crime severity
- **Offline player jailing** capabilities

### Contraband System
- **Customizable contraband lists** for different item types
- **Drug testing mechanics** with configurable compliance timers
- **Bonus duty time** rewards for successful searches

### Technical Features
- **Dual database support** - SQLite and MySQL with HikariCP
- **Async operations** for optimal performance
- **PlaceholderAPI integration** with comprehensive placeholders
- **MiniMessage support** for rich text formatting
- **CoinsEngine integration** for economy features

## Quick Start

1. Download `EdenCorrections.jar` and place it in your `/plugins` folder
2. Restart your server - the plugin will generate a default `config.yml`
3. Configure essential settings:
   - Database type (SQLite/MySQL)
   - Duty region name
   - Kit mappings for different ranks
4. Use `/edenreload` in-game to apply configuration changes
5. Assign the `edencorrections.guard` permission to your staff members

## Commands

### Guard Commands
| Command | Description |
|---------|-------------|
| `/duty` | Toggle duty status (locks/unlocks inventory, applies restrictions) |
| `/chase <player>` | Initiate a chase with the specified player |
| `/chase capture` | Arrest a player at close range |
| `/chase end` | End the current chase |
| `/jail <player> [reason]` | Send a player to jail with automatic sentencing |
| `/sword <player>` | Request player to drop weapons |
| `/bow <player>` | Request player to drop ranged weapons |
| `/armor <player>` | Request player to remove armor |
| `/drugs <player>` | Search player for contraband |
| `/drugtest <player>` | Administer a drug test |
| `/dutybank convert` | Convert duty time to economy tokens |
| `/dutybank status` | Check current duty banking status |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/corrections wanted set <player> <level>` | Set player's wanted level (0-5) |
| `/corrections chase list` | View all active chases |
| `/corrections chase end <player>` | End a specific chase |
| `/corrections chase endall` | End all active chases |
| `/corrections duty list` | List all players on/off duty |
| `/corrections system stats` | View system statistics |
| `/corrections system debug` | Toggle debug mode |
| `/corrections reload` | Reload configuration and messages |
| `/jailoffline <player> [reason]` | Jail an offline player |

## Configuration

The plugin uses a comprehensive configuration system with detailed comments. Key sections include:

```yaml
database:
  type: sqlite          # sqlite or mysql
  # Database connection settings

guard-system:
  duty-region: "guard"  # WorldGuard region for duty activation
  kit-mappings:
    trainee: trainee-kit
    private: private-kit
    officer: officer-kit
    sergeant: sergeant-kit
    captain: captain-kit
    warden: warden-kit

duty-banking:
  enabled: true
  conversion-rate: 100  # seconds per token
  auto-cashout: false   # automatically convert duty time

chase-system:
  max-distance: 100     # maximum chase distance in blocks
  safe-zones: []        # regions where chases are cancelled
```

## Permissions

### Base Permissions
- `edencorrections.guard` - Base guard permission
- `edencorrections.admin` - Administrative access

### Rank-Specific Permissions
- `edencorrections.guard.trainee`
- `edencorrections.guard.private`
- `edencorrections.guard.officer`
- `edencorrections.guard.sergeant`
- `edencorrections.guard.captain`
- `edencorrections.guard.warden`

### Feature Permissions
- `edencorrections.guard.chase` - Chase system access
- `edencorrections.guard.jail` - Jail system access
- `edencorrections.guard.contraband` - Contraband system access
- `edencorrections.guard.banking` - Duty banking access

## Placeholders

EdenCorrections provides extensive PlaceholderAPI integration:

```
%edencorrections_duty_status%      # Current duty status
%edencorrections_wanted_level%     # Player's wanted level (0-5)
%edencorrections_banking_tokens%   # Banked duty tokens
%edencorrections_duty_time%        # Current duty time in seconds
%edencorrections_jail_time%        # Remaining jail time
%edencorrections_chase_status%     # Current chase status
```

A complete placeholder list is generated at `/plugins/EdenCorrections/placeholder_list.txt` on first run.

## Dependencies

### Required
- **WorldGuard** - Region-based duty restrictions
- **LuckPerms** - Rank detection and permissions
- **PlaceholderAPI** - Placeholder integration

### Optional
- **CMI** - Kit system integration
- **CoinsEngine** - Economy integration (`/et` commands)

## Support

For issues, feature requests, or questions:
1. Check the [wiki](wiki/) for detailed documentation
2. Review the configuration comments for customization options
3. Open an issue with:
   - Server version and plugin version
   - Relevant logs
   - Steps to reproduce the problem
   - Expected vs actual behavior

## Contributing

Contributions are welcome! Please ensure your code follows the existing style and includes appropriate documentation. 
