# EdenCorrections

A corrections / prison system plugin for Minecraft that lets your guards behave like professionals (and look fabulous while doing it) – duty toggles, chases, contraband searches, jail timers, duty banking and more. Fun fact: the codebase was rebuilt **three** times before we liked it enough to hand it to testers. Third time’s the charm. 🚀

## Why You Might Want It (30-second pitch)

• Guards: rank-aware, WorldGuard-checked duty system with auto-kits & inventory caching  
• Criminals: dynamic wanted levels, 100-block chases, jail time that fits the crime  
• Economy: turn guard hours into tokens via CoinsEngine (`/et`)  
• Beautiful messages: purple-cyan gradients & emojis out-of-the-box  
• SQLite **or** MySQL – pick your poison

---

## Quick Start

1. Drop `EdenCorrections.jar` in `/plugins`
2. Restart the server once – the plugin will create `config.yml`  
3. Edit the **few** things you care about (database type, duty region, kit names)  
4. `/edenreload` in-game to apply changes  
5. Give guards the `edencorrections.guard` permission – they can `/duty` immediately

That’s it. Everything else ships with sensible defaults.

---

## Command Cheat-Sheet

| Command | Who | Notes |
|---------|-----|-------|
| `/duty` | Guard | Toggle duty (locks / unlocks inventory, kits, restrictions) |
| `/chase <player>` | Guard | Begin 100-block chase |
| `/chase capture` | Guard | Arrest at close range |
| `/chase end` | Guard | End current chase |
| `/jail <player> [reason]` | Guard | Send player to jail (auto time) |
| `/sword /bow /armor /drugs <player>` | Guard | Contraband requests |
| `/drugtest <player>` | Guard | Drug test kit |
| `/dutybank convert|status` | Guard | Convert duty seconds → tokens |
| `/corrections …` | Admin | See “Admin corner” below |

---

## Core Systems In One Breath

• **Guard Duty** – immobilisation countdown, LuckPerms rank detection, CMI kits, inventory saved / restored, WorldGuard region checks.  
• **Wanted** – 1-5 stars, auto decay, violator messages, stars placeholder.  
• **Chase** – distance warnings, safe-zone cancel, boss bar progress, combat timer block.  
• **Jail** – 10-second countdown, offline jail command, stats tracking.  
• **Contraband** – configurable item lists, 10-second compliance, drug tests, bonus duty minutes.  
• **Duty Banking** – 100 sec = 1 token (configurable), optional autocash-out.  
• **Database** – Async SQLite / MySQL via Hikari, plus inventory cache table.  
• **Messages** – MiniMessage gradients, PlaceholderAPI placeholders out-of-the-box.

(Yes, that was technically one sentence. Breathe now.)

---

## Permissions Snapshot

```
edencorrections.guard               # base permission
edencorrections.guard.<rank>        # trainee|private|officer|sergeant|captain|warden
edencorrections.guard.chase|jail|contraband|banking
edencorrections.admin               # top-level admin
```

---

## Tiny Config Teaser

```yaml
database:
  type: sqlite          # or mysql
guard-system:
  duty-region: "guard"  # WorldGuard region where /duty works
  kit-mappings:
    trainee: trainee
    warden: warden
duty-banking:
  enabled: true
  conversion-rate: 100  # seconds per token
```

Don’t worry, the full `config.yml` is heavily commented.

---

## Admin Corner

* `/corrections wanted set <player> <level>` – adjust stars  
* `/corrections chase list|end|endall` – view / end chases  
* `/corrections duty list` – who’s on / off duty  
* `/corrections system stats|debug` – live stats & toggle debug  
* `/corrections reload` – reload config / messages  
* `/jailoffline <player> [reason]` – jail offline player

---

## Placeholders (Selection)

```
%edencorrections_duty_status%   → On Duty / Off Duty
%edencorrections_wanted_level%  → 0-5
%edencorrections_banking_tokens%→ banked tokens
```

More in `/plugins/EdenCorrections/placeholder_list.txt` (generated on first run).

---

## Contributing / Bugs

Open an issue, attach logs, include steps – we’ll look after our inmates. 