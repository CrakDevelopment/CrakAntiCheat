<p align="center">
  <img src="assets/crak-anticheat-logo.png" alt="Crak Anti-Cheat Logo" width="280">
</p>

<p align="center">
  <strong>Crak Anti-Cheat</strong><br>
  Minecraft Java Edition Anti-Cheat
</p>

**Crak Anti-Cheat (CAC)** is a Minecraft Java Edition anti-cheat focused on helping server staff detect suspicious behavior, manage violations, and take moderation actions from a single command system.

> **Current documented version:** `2.0.0`  
> **Author:** CrakDevelopment  
> **Example server platform:** Paper 1.20.4

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Highlights](#-highlights)
- [Commands](#-commands)
  - [Base Command](#base-command)
  - [Staff Commands](#-staff-commands)
  - [Admin Commands](#-admin-commands)
  - [Time Formats](#-time-formats-for-tempban)
- [Command Examples & Output](#-command-examples--output)
- [Permissions](#-permission-nodes)
- [Command Aliases](#-command-aliases)
- [Most Used Commands](#-most-used-commands)
- [Quick Reference](#-quick-reference)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Compatibility](#-compatibility)
- [Support](#-support)
- [License](#-license)

---

## 🎮 Overview

Crak Anti-Cheat is designed for Minecraft Java Edition servers that want a centralized system for anti-cheat alerts, player inspection, violation management, and moderation.

The command system provides separate **staff** and **admin** permissions so server owners can control which team members have access to sensitive moderation actions.

### What Crak Anti-Cheat provides

- 🛡️ Anti-cheat alert management
- 🔍 Player violation inspection
- 👥 Alt-account lookup
- 📊 Server statistics
- 🔄 Configuration reloads
- 🚫 Permanent and temporary bans
- 🔓 Unbanning
- 🔨 Softbans
- 🔇 Player mutes and unmutes
- 👢 Player kicks
- ⚠️ Player warnings
- 🎯 Permission-based staff controls

---

## ✨ Highlights

| Feature | Description |
|---|---|
| 🚨 Alerts | Toggle anti-cheat alerts for staff |
| 🔎 Player Checks | View violation levels for individual players |
| 👥 Alt Detection | Review accounts associated with the same IP |
| 📊 Statistics | View basic server and anti-cheat statistics |
| 🔄 Reload | Reload the anti-cheat configuration |
| 🚫 Punishments | Ban, tempban, softban, mute, kick, and warn players |
| 🔓 Unban | Remove a player's ban |
| 🧹 Reset | Clear a player's violation data |
| 🔐 Permissions | Separate staff, admin, and individual command permissions |

---

# 💻 Commands

## Base Command

The main Crak Anti-Cheat command is:

```text
/crakanticheat
```

### Base Command Aliases

```text
/cac
/anticheat
```

---

## 👮 Staff Commands

**Required permission:** `crakanticheat.staff`

| Command | Aliases | Description | Example |
|---|---|---|---|
| `/cac alerts` | `/cac alert`, `/cac toggle` | Toggle anti-cheat alerts on or off | `/cac alerts` |
| `/cac check <player>` | `/cac inspect`, `/cac info` | Check a player's violation levels | `/cac check Notch` |
| `/cac alts <player>` | `/cac alt` | Check alt accounts on the same IP | `/cac alts Notch` |
| `/cac stats` | `/cac statistics` | Show server statistics | `/cac stats` |
| `/cac info` | `/cac version`, `/cac about` | Show plugin information | `/cac info` |
| `/cac help` | `/cac ?` | Show the help menu | `/cac help` |

---

## 🔧 Admin Commands

**Required permission:** `crakanticheat.admin`

| Command | Aliases | Description | Example |
|---|---|---|---|
| `/cac reload` | `/cac rl` | Reload the configuration file | `/cac reload` |
| `/cac reset <player>` | `/cac clear` | Reset a player's violation data | `/cac reset Notch` |
| `/cac ban <player> <reason>` | `/cac punish` | Permanently ban a player | `/cac ban Notch Hacking` |
| `/cac tempban <player> <time> <reason>` | `/cac tb` | Temporarily ban a player | `/cac tempban Notch 1h Hacking` |
| `/cac unban <player>` | `/cac pardon` | Unban a player | `/cac unban Notch` |
| `/cac softban <player> <reason>` | `/cac sb` | Softban a player | `/cac softban Notch Suspicious` |
| `/cac mute <player> <reason>` | `/cac silence` | Mute a player | `/cac mute Notch Spamming` |
| `/cac unmute <player>` | — | Unmute a player | `/cac unmute Notch` |
| `/cac kick <player> <reason>` | — | Kick a player from the server | `/cac kick Notch Cheating` |
| `/cac warn <player> <reason>` | — | Warn a player | `/cac warn Notch Slow down!` |

---

# 🔍 Time Formats for `/cac tempban`

Crak Anti-Cheat supports the following documented time formats:

| Format | Example | Duration |
|---|---|---|
| `s` | `30s` | 30 seconds |
| `m` | `5m` | 5 minutes |
| `h` | `2h` | 2 hours |
| `d` | `7d` | 7 days |
| `w` | `2w` | 2 weeks |
| `mo` | `1mo` | 1 month |
| `y` | `1y` | 1 year |
| `perm` | `perm` | Permanent |

### Examples

```text
/cac tempban Notch 30s Testing
/cac tempban Notch 5m Spamming
/cac tempban Notch 2h Hacking
/cac tempban Notch 7d X-Ray
/cac tempban Notch 1mo Serious offense
/cac tempban Notch 1y Ban evasion
/cac tempban Notch perm Permanent ban
```

---

# 📊 Command Examples & Output

## 1. `/cac alerts`

### Enabled

```text
[AntiCheat] Anti-Cheat alerts enabled!
```

### Disabled

```text
[AntiCheat] Anti-Cheat alerts disabled!
```

---

## 2. `/cac check Notch`

```text
=== Notch - Anti-Cheat Info ===
Speed: 0
Fly: 0
NoFall: 0
Timer: 0
Phase/Noclip: 0
Scaffold: 0
Jesus: 0
Spider: 0
Step: 0
ESP: 0
Velocity: 0
KillAura: 0
Reach: 0
Aimbot: 0
TriggerBot: 0
HitBox: 0
Criticals: 0
BowAura: 0
FastClick: 0
FastBreak: 0
FastPlace: 0
FastConsume: 0
FastDrop: 0
AutoEat: 0
Dupe: 0
GodMode: 0
ChatSpam: 0
Advertising: 0
Swearing: 0
Status: CLEAN
```

---

## 3. `/cac alts Notch`

```text
=== Alt Account Info for Notch ===
IP Address: 192.168.1.100
Accounts on this IP: 3
Account list:
  - Notch (Online)
  - NotchAlt (Offline)
  - NotchAlt2 (Offline)
```

> **Privacy note:** The IP address above is an example value for documentation. Server owners should treat real IP information as sensitive data and handle it according to their applicable privacy requirements.

---

## 4. `/cac stats`

```text
=== Server Statistics ===
Online Players: 42
Total Bans: 156
Server Type: Paper
Version: git-Paper-396 (MC: 1.20.4)
Paper: true
```

---

## 5. `/cac info`

```text
=== Crak Anti-Cheat Info ===
Version: 2.0.0
Author: CrakDevelopment
Server: Paper 1.20.4
Paper: true
Status: Active
```

---

## 6. `/cac reload`

```text
Crak Anti-Cheat configuration reloaded!
```

---

## 7. `/cac reset Notch`

```text
Reset data for Notch
```

---

## 8. `/cac ban Notch Speed hacking`

```text
Notch has been banned for: Speed hacking
```

---

## 9. `/cac tempban Notch 2h KillAura`

```text
Notch has been temporarily banned for: KillAura
Duration: 2 hours
```

---

## 10. `/cac unban Notch`

```text
Unbanned Notch
```

---

## 11. `/cac softban Notch Suspicious movement`

```text
Notch has been softbanned for: Suspicious movement
Duration: 1 hour
```

---

## 12. `/cac mute Notch Spamming`

```text
Notch has been muted for: Spamming
```

---

## 13. `/cac unmute Notch`

```text
Notch has been unmuted
```

---

## 14. `/cac kick Notch Cheating`

```text
Notch has been kicked for: Cheating
```

---

## 15. `/cac warn Notch Slow down!`

```text
Notch has been warned: Slow down!
```

---

# 👥 Permission Nodes

| Permission | Description | Default |
|---|---|---|
| `crakanticheat.staff` | Access to staff commands and alerts | `OP` |
| `crakanticheat.admin` | Access to admin commands | `OP` |
| `crakanticheat.bypass` | Bypass all anti-cheat checks | `OP` |
| `crakanticheat.alerts` | Toggle alerts | `OP` |
| `crakanticheat.check` | Check player violations | `OP` |
| `crakanticheat.alts` | Check alt accounts | `OP` |
| `crakanticheat.stats` | View server statistics | `OP` |
| `crakanticheat.reload` | Reload configuration | `OP` |
| `crakanticheat.reset` | Reset player data | `OP` |
| `crakanticheat.ban` | Ban players | `OP` |
| `crakanticheat.unban` | Unban players | `OP` |
| `crakanticheat.mute` | Mute players | `OP` |
| `crakanticheat.kick` | Kick players | `OP` |
| `crakanticheat.warn` | Warn players | `OP` |

### Permission Structure

For simple server setups, grant:

```text
crakanticheat.staff
```

to trusted staff members.

Grant:

```text
crakanticheat.admin
```

only to administrators who should have access to punishment and configuration-management commands.

The individual permission nodes can also be used for more granular permission setups.

---

# 🔗 Command Aliases

## Base

| Main | Alias 1 | Alias 2 |
|---|---|---|
| `/crakanticheat` | `/cac` | `/anticheat` |

## Staff

| Main | Alias 1 | Alias 2 |
|---|---|---|
| `/cac alerts` | `/cac alert` | `/cac toggle` |
| `/cac check` | `/cac inspect` | `/cac info` |
| `/cac alts` | `/cac alt` | — |
| `/cac stats` | `/cac statistics` | — |
| `/cac info` | `/cac version` | `/cac about` |
| `/cac help` | `/cac ?` | — |

## Admin

| Main | Alias |
|---|---|
| `/cac reload` | `/cac rl` |
| `/cac reset` | `/cac clear` |
| `/cac ban` | `/cac punish` |
| `/cac tempban` | `/cac tb` |
| `/cac unban` | `/cac pardon` |
| `/cac softban` | `/cac sb` |
| `/cac mute` | `/cac silence` |

> **Implementation note:** The documented command list contains `/cac info` as both a standalone information command and an alias for `/cac check`. A command implementation should resolve this ambiguity consistently. A recommended approach is to reserve `/cac info` for plugin information and use `/cac check` or `/cac inspect` for player inspection.

---

# 📝 Time Formats Quick Reference

```text
s   = seconds     (e.g., 30s)
m   = minutes     (e.g., 5m)
h   = hours       (e.g., 2h)
d   = days        (e.g., 7d)
w   = weeks       (e.g., 2w)
mo  = months      (e.g., 1mo)
y   = years       (e.g., 1y)
perm = permanent
```

---

# 🔥 Most Used Commands

## Staff

```text
/cac alerts
/cac check Steve
/cac alts Steve
```

## Administration

```text
/cac reload
/cac ban Steve X-Ray
/cac tempban Steve 1d Speedhack
/cac unban Steve
/cac reset Steve
```

## Quick Punishments

```text
/cac ban Notch KillAura
/cac tempban Notch 1h Speed
/cac softban Notch Suspicious
```

---

# 📦 Installation

> Installation steps may vary depending on how Crak Anti-Cheat is distributed.

### 1. Download Crak Anti-Cheat

Download the Crak Anti-Cheat `.jar` release from your official project release page.

### 2. Open your server directory

Locate your Minecraft server's:

```text
plugins/
```

directory.

### 3. Install the plugin

Place the Crak Anti-Cheat `.jar` file into:

```text
plugins/
```

### 4. Restart the server

Restart your Minecraft server so the plugin can load.

### 5. Verify the installation

In-game or from the server console, verify that Crak Anti-Cheat is active.

You can use:

```text
/cac info
```

to view the documented plugin information.

### 6. Configure permissions

Give trusted staff the appropriate permission nodes listed in the [Permission Nodes](#-permission-nodes) section.

---

# ⚙️ Configuration

After installation, Crak Anti-Cheat can be configured through its generated configuration file when supported by the installed release.

After making configuration changes, use:

```text
/cac reload
```

to reload the configuration.

Expected output:

```text
Crak Anti-Cheat configuration reloaded!
```

> Always keep a backup of your configuration before making major changes.

---

# 🧩 Compatibility

The documented example environment is:

| Component | Version / Value |
|---|---|
| Minecraft | Java Edition |
| Server Software | Paper |
| Example Minecraft Version | 1.20.4 |
| Crak Anti-Cheat | 2.0.0 |
| Author | CrakDevelopment |

Compatibility may vary between releases. Check the release notes for the version you are installing before deploying it on a production server.

---

# 📈 Command Summary

| Category | Count |
|---|---:|
| Staff Commands | 6 |
| Admin Commands | 10 |
| Total Commands | **16** |
| Aliases | **15+** |
| Permissions | **13** |
| Time Formats | **8** |

> The command counts above follow the documented command categories supplied for Crak Anti-Cheat. Alias counts may vary depending on whether the base command and subcommand aliases are counted individually.

---

# 🛡️ Recommended Staff Workflow

A typical moderation workflow can look like this:

### 1. Receive an alert

Staff members receive an anti-cheat alert.

### 2. Inspect the player

```text
/cac check <player>
```

### 3. Review possible alternate accounts

```text
/cac alts <player>
```

### 4. Decide on the appropriate action

Depending on your server rules:

```text
/cac warn <player> <reason>
```

or:

```text
/cac kick <player> <reason>
```

or:

```text
/cac tempban <player> <time> <reason>
```

or:

```text
/cac ban <player> <reason>
```

### 5. Reset violation data when appropriate

```text
/cac reset <player>
```

---

# ⚠️ Important Notes

- Anti-cheat detections should be reviewed carefully before severe punishments are issued.
- Configure permissions so only trusted staff can access sensitive commands.
- The `crakanticheat.bypass` permission should only be granted when intentionally required.
- Real IP addresses and alt-account information should be treated as sensitive server data.
- Keep regular backups of your configuration and server data.
- Test configuration changes on a test server before deploying them to a production server.
- Do not rely exclusively on a single detection or violation level when making moderation decisions.

---

# 🐛 Bug Reports

When reporting a bug, include as much useful information as possible:

```text
Crak Anti-Cheat Version:
Minecraft Version:
Server Software:
Server Software Version:
Java Version:

Issue:
What happened?

Steps to Reproduce:
1.
2.
3.

Expected Behavior:

Actual Behavior:

Relevant Console Error:
```

Avoid posting real player IP addresses or other sensitive information in public bug reports.

---

# 💬 Support

For support, bug reports, feature requests, and development discussions, use the official support channels provided by the Crak Anti-Cheat project.

When asking for help, include:

- Crak Anti-Cheat version
- Minecraft version
- Server software and version
- Java version
- Relevant configuration
- Console errors
- Steps to reproduce the problem

---

# 📄 License

This project is distributed under the license specified by the Crak Anti-Cheat project.

If this repository has a `LICENSE` file, refer to that file for the complete terms and conditions.

---

# 🚀 Crak Anti-Cheat

**Protect your server. Stop the cheats. Keep Minecraft fair.**

Made for Minecraft Java Edition by **CrakDevelopment**.

---

## ⭐ Quick Command Reference

```text
/crakanticheat
/cac
/anticheat

/cac alerts
/cac check <player>
/cac alts <player>
/cac stats
/cac info
/cac help

/cac reload
/cac reset <player>
/cac ban <player> <reason>
/cac tempban <player> <time> <reason>
/cac unban <player>
/cac softban <player> <reason>
/cac mute <player> <reason>
/cac unmute <player>
/cac kick <player> <reason>
/cac warn <player> <reason>
```

**Crak Anti-Cheat — Built to help keep your Minecraft server fair, secure, and competitive.**
