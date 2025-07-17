# 🛡️ Permissions System

Complete guide to EdenCorrections permissions, including all permission nodes, rank-based setup, and best practices.

---

## 🎯 **Permission Overview**

EdenCorrections uses a hierarchical permission system integrated with **LuckPerms** for advanced rank management. The plugin supports both permission-based and group-based access control.

### Core Permission Structure
```
edencorrections.*
├── guard.*
│   ├── [rank permissions]
│   ├── chase
│   ├── jail
│   ├── contraband
│   └── banking
├── admin
└── reload
```

---

## 👮‍♂️ **Guard Permissions**

### Basic Guard Access
```yaml
# Required for all guard functions
edencorrections.guard
```

**Description**: Basic permission required for all guard-related commands and features.

**Grants Access To**:
- `/duty` command
- `/tips` command
- Basic guard functionality

### Rank-Based Permissions

#### Trainee Level
```yaml
edencorrections.guard.trainee
```
**Inherits**: `edencorrections.guard`
**Additional Access**:
- Basic duty toggle
- Training tips and guidance
- Observer-level access

#### Private Level
```yaml
edencorrections.guard.private
```
**Inherits**: `edencorrections.guard.trainee`
**Additional Access**:
- Basic contraband detection
- Simple patrol duties

#### Officer Level  
```yaml
edencorrections.guard.officer
```
**Inherits**: `edencorrections.guard.private`
**Additional Access**:
- Chase initiation
- Active law enforcement
- Arrest procedures

#### Sergeant Level
```yaml
edencorrections.guard.sergeant
```
**Inherits**: `edencorrections.guard.officer`
**Additional Access**:
- Basic administrative commands
- Wanted level management
- Team coordination

#### Captain Level
```yaml
edencorrections.guard.captain
```
**Inherits**: `edencorrections.guard.sergeant`
**Additional Access**:
- Chase management
- Advanced administrative tools
- System oversight

#### Warden Level
```yaml
edencorrections.guard.warden
```
**Inherits**: `edencorrections.guard.captain`
**Additional Access**:
- Full administrative control
- System configuration
- Debug tools

---

## 🔧 **Functional Permissions**

### Chase System
```yaml
# Chase initiation and management
edencorrections.guard.chase
```

**Required For**:
- `/chase <player>` - Start chase
- `/chase capture` - Capture during chase
- `/chase end` - End active chase

**Minimum Rank**: Officer
**Additional Requirements**:
- Must be on duty
- Target must be wanted

### Jail System
```yaml
# Arrest and jail players
edencorrections.guard.jail
```

**Required For**:
- `/jail <player> [reason]` - Arrest player
- Jail countdown process
- Arrest statistics

**Minimum Rank**: Officer
**Additional Requirements**:
- Must be on duty
- Target must be nearby

### Contraband Detection
```yaml
# Contraband searches and drug testing
edencorrections.guard.contraband
```

**Required For**:
- `/sword <player>` - Weapon search
- `/bow <player>` - Ranged weapon search
- `/armor <player>` - Armor search
- `/drugs <player>` - Drug search
- `/drugtest <player>` - Drug testing

**Minimum Rank**: Private
**Additional Requirements**:
- Must be on duty
- Must be within 5 blocks of target

### Banking System
```yaml
# Duty time banking
edencorrections.guard.banking
```

**Required For**:
- `/dutybank convert` - Convert duty time
- `/dutybank status` - Check banking status
- Banking notifications

**Minimum Rank**: Trainee
**Additional Requirements**:
- Must have minimum duty time

---

## 👑 **Administrative Permissions**

### Full Administrative Access
```yaml
# Complete administrative control
edencorrections.admin
```

**Required For**:
- `/corrections wanted set/clear/check/list`
- `/corrections chase list/end/endall`
- `/corrections duty list`
- `/corrections system stats/debug`
- `/jailoffline <player> [reason]`

**Minimum Rank**: Warden
**Grants Access To**:
- All administrative commands
- System management
- Debug tools
- Offline player management

### Advanced Administrative
```yaml
# Advanced administrative functions
edencorrections.guard.admin
```

**Required For**:
- `/jailoffline <player> [reason]`
- Advanced system commands
- Player data management

**Minimum Rank**: Captain

### System Reload
```yaml
# Configuration reload
edencorrections.reload
```

**Required For**:
- `/edenreload`
- `/corrections reload`

**Minimum Rank**: Warden
**Alternative**: `edencorrections.admin`

---

## 🎮 **Utility Permissions**

### Tips System
```yaml
# Access to tips and help
edencorrections.tips
```

**Required For**:
- `/tips [system]`
- Help system access

**Minimum Rank**: Trainee
**Note**: Included in `edencorrections.guard`

### Debug Access
```yaml
# Debug information access
edencorrections.debug
```

**Required For**:
- Debug message visibility
- System diagnostics
- Performance monitoring

**Minimum Rank**: Warden
**Note**: Included in `edencorrections.admin`

---

## 📊 **Permission Inheritance**

### Hierarchical Structure

```
edencorrections.guard.warden
├── edencorrections.guard.captain
│   ├── edencorrections.guard.sergeant
│   │   ├── edencorrections.guard.officer
│   │   │   ├── edencorrections.guard.private
│   │   │   │   ├── edencorrections.guard.trainee
│   │   │   │   │   └── edencorrections.guard
│   │   │   │   └── edencorrections.guard.contraband
│   │   │   └── edencorrections.guard.chase
│   │   │   └── edencorrections.guard.jail
│   │   └── edencorrections.guard.admin (partial)
│   └── edencorrections.guard.admin (full)
└── edencorrections.admin
└── edencorrections.reload
```

### Permission Inheritance Table

| Rank | Base | Contraband | Chase | Jail | Admin | Full Admin | Debug |
|------|------|------------|-------|------|-------|------------|-------|
| **Trainee** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Private** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Officer** | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Sergeant** | ✅ | ✅ | ✅ | ✅ | ⚠️ | ❌ | ❌ |
| **Captain** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Warden** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Legend**: ✅ Full Access | ⚠️ Partial Access | ❌ No Access

---

## 🔨 **LuckPerms Setup**

### Creating Guard Groups

```bash
# Create all guard groups
/lp creategroup trainee
/lp creategroup private
/lp creategroup officer
/lp creategroup sergeant
/lp creategroup captain
/lp creategroup warden

# Set up group inheritance
/lp group private parent add trainee
/lp group officer parent add private
/lp group sergeant parent add officer
/lp group captain parent add sergeant
/lp group warden parent add captain
```

### Assigning Permissions

#### Trainee Permissions
```bash
/lp group trainee permission set edencorrections.guard.trainee true
/lp group trainee permission set edencorrections.guard true
/lp group trainee permission set edencorrections.guard.banking true
/lp group trainee permission set edencorrections.tips true
```

#### Private Permissions
```bash
/lp group private permission set edencorrections.guard.private true
/lp group private permission set edencorrections.guard.contraband true
```

#### Officer Permissions
```bash
/lp group officer permission set edencorrections.guard.officer true
/lp group officer permission set edencorrections.guard.chase true
/lp group officer permission set edencorrections.guard.jail true
```

#### Sergeant Permissions
```bash
/lp group sergeant permission set edencorrections.guard.sergeant true
# Inherits all lower permissions
```

#### Captain Permissions
```bash
/lp group captain permission set edencorrections.guard.captain true
/lp group captain permission set edencorrections.guard.admin true
```

#### Warden Permissions
```bash
/lp group warden permission set edencorrections.guard.warden true
/lp group warden permission set edencorrections.admin true
/lp group warden permission set edencorrections.reload true
/lp group warden permission set edencorrections.debug true
```

### Assigning Players to Groups

```bash
# Add player to guard group
/lp user <player> parent add trainee

# Promote player
/lp user <player> parent remove trainee
/lp user <player> parent add private

# Check player's permissions
/lp user <player> info
```

---

## 🎯 **Permission Configuration**

### Config.yml Integration

```yaml
guard-system:
  rank-mappings:
    trainee: "trainee"      # LuckPerms group name
    private: "private"
    officer: "officer"
    sergeant: "sergeant"
    captain: "captain"
    warden: "warden"
    
integrations:
  luckperms:
    strict-mode: true       # Require exact group matching
```

### Strict Mode vs Permissive Mode

#### Strict Mode (Recommended)
```yaml
strict-mode: true
```
- Requires exact LuckPerms group names
- Enhanced security
- Prevents permission bypasses
- Recommended for production

#### Permissive Mode
```yaml
strict-mode: false
```
- Falls back to permission nodes
- Allows OP bypass
- Less secure
- Good for testing

---

## 🔒 **Security Best Practices**

### 1. Principle of Least Privilege
```bash
# Give minimum required permissions
/lp group trainee permission set edencorrections.guard.trainee true
# Don't give: edencorrections.admin
```

### 2. Separate Administrative Access
```bash
# Create separate admin group
/lp creategroup corrections_admin
/lp group corrections_admin permission set edencorrections.admin true
```

### 3. Regular Permission Audits
```bash
# Check group permissions
/lp group warden permission info

# Check user permissions
/lp user <player> permission info
```

### 4. Use Inheritance Properly
```bash
# Correct: Build hierarchy
/lp group officer parent add private

# Incorrect: Skip levels
/lp group officer parent add trainee
```

---

## 📋 **Permission Troubleshooting**

### Common Issues

#### Player Cannot Go On Duty
```bash
# Check if player has guard rank
/lp user <player> info

# Check group permissions
/lp group <group> permission info

# Debug rank detection
/corrections system debug rank <player>
```

#### Commands Not Working
```bash
# Check specific permission
/lp user <player> permission check edencorrections.guard.chase

# Check effective permissions
/lp user <player> permission info
```

#### Rank Not Detected
```bash
# Verify group name mapping
/corrections system debug rank <player>

# Check config mapping
guard-system:
  rank-mappings:
    officer: "officer"  # Must match LuckPerms group
```

### Debug Commands
```bash
# Enable debug mode
/corrections system debug on

# Check player's rank
/corrections system debug rank <player>

# View system stats
/corrections system stats
```

---

## 🎭 **Permission Templates**

### Small Server Template
```bash
# Basic setup for small servers
/lp creategroup guard
/lp group guard permission set edencorrections.guard true
/lp group guard permission set edencorrections.guard.contraband true
/lp group guard permission set edencorrections.guard.chase true
/lp group guard permission set edencorrections.guard.jail true
/lp group guard permission set edencorrections.guard.banking true

# Admin group
/lp creategroup guard_admin
/lp group guard_admin parent add guard
/lp group guard_admin permission set edencorrections.admin true
```

### Prison Server Template
```bash
# Full hierarchy for prison servers
/lp creategroup c_trainee
/lp creategroup c_private
/lp creategroup c_officer
/lp creategroup c_sergeant
/lp creategroup c_captain
/lp creategroup c_warden

# Set up inheritance
/lp group c_private parent add c_trainee
/lp group c_officer parent add c_private
/lp group c_sergeant parent add c_officer
/lp group c_captain parent add c_sergeant
/lp group c_warden parent add c_captain

# Base permissions
/lp group c_trainee permission set edencorrections.guard.trainee true
/lp group c_private permission set edencorrections.guard.private true
/lp group c_officer permission set edencorrections.guard.officer true
/lp group c_sergeant permission set edencorrections.guard.sergeant true
/lp group c_captain permission set edencorrections.guard.captain true
/lp group c_warden permission set edencorrections.guard.warden true

# Functional permissions
/lp group c_private permission set edencorrections.guard.contraband true
/lp group c_officer permission set edencorrections.guard.chase true
/lp group c_officer permission set edencorrections.guard.jail true
/lp group c_captain permission set edencorrections.guard.admin true
/lp group c_warden permission set edencorrections.admin true
```

---

## 🔗 **Integration with Other Plugins**

### WorldGuard Integration
```yaml
# WorldGuard region permissions
regions:
  guard_station:
    members:
      - g:guard      # All guard group members
    flags:
      entry: members
```

### CMI Integration
```yaml
# CMI kit permissions
kits:
  trainee_kit:
    permission: edencorrections.guard.trainee
  officer_kit:
    permission: edencorrections.guard.officer
```

### PlaceholderAPI Integration
```yaml
# Permission-based placeholders
placeholders:
  rank_display: "%luckperms_primary_group_name%"
  has_guard_perms: "%luckperms_has_permission_edencorrections.guard%"
```

---

## 📈 **Permission Analytics**

### Usage Statistics
```bash
# Check permission usage
/lp group <group> permission info

# View inheritance tree
/lp group <group> parent info

# Check effective permissions
/lp user <player> permission info
```

### Common Permission Combinations

| Feature | Required Permissions | Recommended Rank |
|---------|---------------------|------------------|
| **Basic Duty** | `edencorrections.guard.trainee` | Trainee+ |
| **Contraband Search** | `edencorrections.guard.contraband` | Private+ |
| **Chase & Arrest** | `edencorrections.guard.chase`, `edencorrections.guard.jail` | Officer+ |
| **Administrative** | `edencorrections.admin` | Warden |
| **Banking** | `edencorrections.guard.banking` | All Guards |

---

## 📚 **Related Documentation**

- [Commands Reference](Commands.md) - Commands and their required permissions
- [Configuration Guide](Configuration.md) - Permission-related configuration
- [Setup & Installation](Setup-and-Installation.md) - LuckPerms setup guide
- [Troubleshooting](Troubleshooting.md) - Permission troubleshooting

---

## 🎯 **Quick Reference**

### Essential Permission Nodes
```yaml
# Basic guard access
edencorrections.guard

# Rank-specific permissions
edencorrections.guard.trainee
edencorrections.guard.private
edencorrections.guard.officer
edencorrections.guard.sergeant
edencorrections.guard.captain
edencorrections.guard.warden

# Functional permissions
edencorrections.guard.chase
edencorrections.guard.jail
edencorrections.guard.contraband
edencorrections.guard.banking

# Administrative permissions
edencorrections.admin
edencorrections.guard.admin
edencorrections.reload
```

### Permission Hierarchy
```
Warden > Captain > Sergeant > Officer > Private > Trainee
```

---

*Permissions are processed in order of specificity. More specific permissions override general ones.* 