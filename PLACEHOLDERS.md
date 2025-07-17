# EdenCorrections PlaceholderAPI Integration

This plugin provides PlaceholderAPI integration for both **providing placeholders** (for other plugins to use) and **consuming placeholders** (from other plugins).

## Installation

1. **PlaceholderAPI** must be installed on your server
2. **EdenCorrections** automatically registers its placeholders when PlaceholderAPI is detected
3. No additional configuration required

## Available Placeholders

All EdenCorrections placeholders use the format: `%edencorrections_<category>_<type>%`

### Wanted System Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_wanted_level%` | Current wanted level | `3` |
| `%edencorrections_wanted_stars%` | Wanted level as stars | `⭐⭐⭐` |
| `%edencorrections_wanted_time%` | Remaining wanted time (seconds) | `1200` |
| `%edencorrections_wanted_reason%` | Reason for wanted level | `Attacking a guard` |
| `%edencorrections_wanted_active%` | Whether player is wanted | `true` |

### Duty System Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_duty_status%` | Current duty status | `On Duty` |
| `%edencorrections_duty_active%` | Whether player is on duty | `true` |
| `%edencorrections_duty_rank%` | Guard rank | `senior` |
| `%edencorrections_duty_time%` | Current duty session time (seconds) | `3600` |
| `%edencorrections_duty_total%` | Total duty time (seconds) | `86400` |

### Chase System Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_chase_active%` | Whether player is in a chase | `true` |
| `%edencorrections_chase_target%` | Target being chased (for guards) | `PlayerName` |
| `%edencorrections_chase_guard%` | Guard chasing player (for targets) | `GuardName` |
| `%edencorrections_chase_time%` | Current chase duration (seconds) | `180` |
| `%edencorrections_chase_combat%` | Whether player is in combat | `true` |

### Jail System Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_jail_countdown%` | Whether jail countdown is active | `true` |

### Contraband System Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_contraband_request%` | Whether contraband request is active | `true` |

### Duty Banking Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_banking_tokens%` | Available tokens for conversion | `50` |
| `%edencorrections_banking_time%` | Total banked duty time (seconds) | `5000` |
| `%edencorrections_banking_enabled%` | Whether duty banking is enabled | `true` |

### Player Statistics Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%edencorrections_player_arrests%` | Total arrests made | `25` |
| `%edencorrections_player_violations%` | Total violations committed | `10` |
| `%edencorrections_player_power%` | Player power (duty time) | `86400` |
| `%edencorrections_player_name%` | Player name | `PlayerName` |

## Usage Examples

### In Other Plugins

You can use these placeholders in any plugin that supports PlaceholderAPI:

**Chat Plugin:**
```yaml
format: "%edencorrections_duty_status% {player}: {message}"
```

**Scoreboard Plugin:**
```yaml
lines:
  - "Wanted Level: %edencorrections_wanted_level%"
  - "Duty Status: %edencorrections_duty_status%"
  - "Available Tokens: %edencorrections_banking_tokens%"
```

**GUI Plugin:**
```yaml
display-name: "Duty Status: %edencorrections_duty_status%"
lore:
  - "Rank: %edencorrections_duty_rank%"
  - "Time on Duty: %edencorrections_duty_time%"
```

### In EdenCorrections Messages

The plugin automatically parses other plugins' placeholders in its messages:

```yaml
messages:
  duty:
    activation:
      success: "Welcome %player_name%! You are now on duty as %edencorrections_duty_rank%!"
      rank-info: "Your rank: %vault_rank% | Guard rank: %edencorrections_duty_rank%"
```

## Testing Placeholders

Use the PlaceholderAPI parse command to test placeholders:

```
/papi parse me %edencorrections_wanted_level%
/papi parse me %edencorrections_duty_status%
/papi parse me %edencorrections_banking_tokens%
```

## Integration Features

### External Placeholder Support

EdenCorrections automatically parses placeholders from other plugins in its messages, including:
- `%player_name%` (Player expansion)
- `%vault_rank%` (Vault expansion)
- `%luckperms_group%` (LuckPerms expansion)
- Any other PlaceholderAPI expansion

### Automatic Registration

The plugin automatically:
1. Detects PlaceholderAPI on startup
2. Registers the EdenCorrections expansion
3. Enables placeholder parsing in messages
4. Logs integration status

## Troubleshooting

### Placeholder Not Working

1. **Check PlaceholderAPI installation:**
   ```
   /papi list
   ```
   Should show "EdenCorrections" in the list

2. **Test placeholder directly:**
   ```
   /papi parse me %edencorrections_wanted_level%
   ```

3. **Check console for errors:**
   Look for PlaceholderAPI registration messages in server console

### Common Issues

- **Returns `%placeholder%`**: PlaceholderAPI not installed or expansion not registered
- **Returns `null`**: Player data not found or player not online
- **Returns `0` or `false`**: Default values when no data available

## Technical Details

- **Expansion Identifier:** `edencorrections`
- **Author:** LSDMC  
- **Version:** Matches plugin version
- **Persistence:** Enabled (survives PlaceholderAPI reloads)
- **Player Context:** Required for all placeholders
- **Offline Support:** Limited (player must be online)

## API Integration

For developers wanting to integrate with EdenCorrections placeholders:

```java
// Check if EdenCorrections expansion is available
if (PlaceholderAPI.getRegisteredExpansions().contains("edencorrections")) {
    String wantedLevel = PlaceholderAPI.setPlaceholders(player, "%edencorrections_wanted_level%");
}
``` 