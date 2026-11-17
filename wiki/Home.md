# NeroTrophies

NeroTrophies is the cross-mod achievement layer of the Neroland mod family: trophies for milestones across every
Nero mod, and trophy blocks, pedestals and cabinets to show them off in your base.

**Current version: 0.0.1-alpha.1.** This is an early scaffold: the mod loads, adds its (empty) creative tab and
its config file, and does nothing else yet.

## Requirements

- Minecraft 26.3 (older lines down to 1.21.1 are also built)
- Fabric (with Fabric API), NeoForge or Forge
- Neroland Core 1.20.0 or newer

## Configuration

`config/nerotrophies.properties`:

| Key | Default | What it does |
|-----|---------|--------------|
| `telemetryEnabled` | `true` | Anonymous NeroTrophies-only crash reports. Set `false` to opt out (takes effect on restart). This build sends nothing. |
| `announceTiers` | `gold,nero` | Tiers whose live awards are announced in chat. Empty for none. |
| `reclaimCooldownSeconds` | `600` | Wait between re-claims of the same trophy from the Trophy Book. |
| `retroPlayersPerTick` | `2` | Retroactive re-check: players started per server tick. |
| `retroChecksPerTick` | `512` | Retroactive re-check: trophy checks per server tick. |
| `retroMillisPerTick` | `2` | Retroactive re-check: milliseconds it may use per server tick. |
| `pollIntervalTicks` | `100` | How often statistic, item, block and dimension trophies are checked per player. |
| `rewardsEnabled` | `false` | Small coin rewards for live awards. Needs a real currency provider such as NeroEconomy. |
| `coinsBronze` / `coinsSilver` / `coinsGold` / `coinsNero` | `10` / `25` / `75` / `200` | Coin reward per tier (a trophy file may override it). |

## Commands

| Command | Who | What it does |
|---------|-----|--------------|
| `/nerotrophies list` | everyone | Your earned trophies. |
| `/nerotrophies announce on\|off` | everyone | Whether your gold and nero trophies are announced in chat. |
| `/nerotrophies showname on\|off` | everyone | Whether your name shows on your trophies (and in announcements). |
| `/nerotrophies export` | everyone | Your whole trophy record, sent to your chat to copy. |
| `/nerotrophies list <player>` | operators | That player's trophy ids and award times. |
| `/nerotrophies award <player> <trophy>` | operators | Award a trophy (no coins, no announcement). |
| `/nerotrophies revoke <player> <trophy>` | operators | Remove a trophy; any copy of it becomes a replica. |
| `/nerotrophies reload` | operators | Re-read trophy data and re-check online players. |
| `/nerotrophies export <player>` | admins (level 3) | Export a player's record to the admin who asked. |

## Privacy

NeroTrophies keeps one record per player who has earned a trophy: trophy ids, award times, claim tokens, completed
collections, the last-known gamertag and two toggles. Trophy items and blocks never hold a name or a UUID. Your
gamertag shows on your trophies to players who can see them; `/nerotrophies showname off` hides it everywhere at
once. `/nerotrophies export` shows your data, and Neroland Core's erasure (`/neroland data eraseme`) removes it, after
which your trophies show no name. Full details are in the mod's `PRIVACY.md`. Trophies never gate gameplay.
