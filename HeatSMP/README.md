# 🔥 HeatSMP Plugin

A custom PvP plugin for Paper 1.21.1 where fighting builds **Heat** — giving you power, but risking a deadly overheat.

---

## 📦 Building

**Requirements:** Java 21, Maven

```bash
cd HeatSMP
mvn clean package
```

The compiled `.jar` will be at `target/HeatSMP-1.0.0.jar`.  
Drop it into your server's `plugins/` folder and restart.

---

## 🌡️ Heat System

| Heat Range | Status    | Effect                          |
|------------|-----------|---------------------------------|
| 0–24       | ❄ Cold    | No bonus                        |
| 25–49      | 🌡 Warm   | Speed I                         |
| 50–74      | 🔥 Hot    | Strength I                      |
| 75–89      | 🔥🔥 Blazing | Strength II + Speed II       |
| 90–100     | 💀 OVERHEAT | Burning + damage over time + Slowness I |

**Heat sources:**
- Hit a player → +8
- Get hit → +4
- Sprinting in combat → +2/sec
- Using abilities → varies

**Cooling:**
- Idle (out of combat) → −3/sec
- Sneaking → −6/sec
- In water → −15/sec
- Cooling Pearl → −30 instantly

---

## ⚔️ Abilities

Use `/ability <name>` or the aliases below:

| Ability          | Alias    | Heat Cost | Cooldown | Effect |
|------------------|----------|-----------|----------|--------|
| `flamedash`      | `fd`     | 20        | 8s       | Dash forward, leave fire trail |
| `heatburst`      | `hb`     | 40        | 12s      | Knockback all nearby players |
| `emergencycool`  | `ec`     | ALL heat  | 30s      | Reset heat to 0, gain Weakness briefly |

---

## 🧪 Items

### ❄ Cooling Pearl
- **Craft:** Ice on N/S/E/W + Ender Pearl in center
- **Use:** Right-click to instantly remove 30 heat
- **Cooldown:** 20 seconds

### ✦ Heat Enchant
- Applied by admins: `/heatadmin enchant <player>`
- Weapon gains +30% heat per hit
- But overheat threshold is reduced by 10

---

## 🛠️ Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/heat [player]` | everyone | View heat status & ability cooldowns |
| `/ability <name>` | everyone | Use an ability |
| `/heatadmin set <player> <value>` | op | Set a player's heat |
| `/heatadmin reset <player>` | op | Reset a player's heat to 0 |
| `/heatadmin give <player> cooling_pearl` | op | Give a Cooling Pearl |
| `/heatadmin enchant <player>` | op | Apply Heat Enchant to held item |
| `/heatadmin reload` | op | Reload config.yml |

---

## ⚙️ Configuration

All values are in `plugins/HeatSMP/config.yml` and can be hot-reloaded with `/heatadmin reload`.

Key sections:
- `heat.*` — heat gain/loss rates
- `buffs.*` — thresholds for each buff tier
- `abilities.*` — per-ability heat costs and cooldowns
- `streaks.*` — combo system settings
- `cooling-pearl.*` — item settings
- `heat-enchant.*` — enchant multipliers
- `messages.*` — all player-facing text (supports `&` color codes)

---

## 🧠 Strategy Tips

- **Aggressive style:** Stack heat fast → huge buffs → risk overheating
- **Defensive style:** Manage heat below 90 → consistent Strength II
- **Counter-play:** Let an overheated opponent burn themselves out, then engage
- **Emergency Cool clutch:** Save it as a last resort — the Weakness window is a real risk

---

## 📁 Project Structure

```
src/main/java/dev/heatsmp/
├── HeatSMPPlugin.java          # Main plugin class
├── heat/
│   ├── HeatManager.java        # Core heat logic, buffs, combos
│   ├── HeatBar.java            # BossBar HUD
│   └── HeatParticles.java      # All visual/sound effects
├── abilities/
│   └── AbilityManager.java     # Flame Dash, Heat Burst, Emergency Cool
├── items/
│   ├── CoolingPearl.java       # Custom item + recipe
│   └── HeatEnchant.java        # Heat Enchant applicator
├── listeners/
│   ├── CombatListener.java     # PvP hit detection
│   ├── HeatTickListener.java   # Tick tasks (cool, damage, particles, HUD)
│   ├── CoolingPearlListener.java # Item right-click handler
│   └── PlayerSessionListener.java # Join/quit cleanup
├── commands/
│   ├── HeatCommand.java        # /heat
│   ├── HeatAdminCommand.java   # /heatadmin
│   └── AbilityCommand.java     # /ability
└── utils/
    └── MessageUtil.java        # Config message parser
```
