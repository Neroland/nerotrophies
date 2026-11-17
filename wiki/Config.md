# Config

NeroTrophies uses Neroland Core's config framework. The file is `config/nerotrophies.properties`; operators can
reload it with `/neroland config reload`. Every setting except `telemetryEnabled` is decided by the server and sent to
clients.

| Key | Default | Range | What it does |
|-----|---------|-------|--------------|
| `telemetryEnabled` | `true` | | Anonymous, NeroTrophies-only crash reports. Set `false` to opt out (takes effect on restart). This is each player's own choice; a server cannot change it. See [Privacy](Privacy). |
| `announceTiers` | `gold,nero` | any of `bronze,silver,gold,nero`, or empty | Tiers whose live awards are announced in chat. Empty for none. A player is only named if their own `announce` and `showname` switches are on. |
| `reclaimCooldownSeconds` | `600` | 0 to 86 400 | Wait between re-claims of the same trophy from the Trophy Book. |
| `retroPlayersPerTick` | `2` | 1 to 20 | Retroactive re-check: players started per server tick. |
| `retroChecksPerTick` | `512` | 32 to 8 192 | Retroactive re-check: trophy checks per server tick across all players. |
| `retroMillisPerTick` | `2` | 1 to 20 | Retroactive re-check: milliseconds it may use per server tick. |
| `pollIntervalTicks` | `100` | 20 to 1 200 | Ticks between checks of statistic, item, block and dimension trophies for each online player. |
| `rewardsEnabled` | `false` | | Pay a small coin reward through Neroland Core's currency for live awards. Needs a real currency provider such as NeroEconomy; otherwise nothing is paid. |
| `coinsBronze` | `10` | 0 to 10 000 | Coins for a bronze trophy (when `rewardsEnabled`). |
| `coinsSilver` | `25` | 0 to 10 000 | Coins for a silver trophy. |
| `coinsGold` | `75` | 0 to 10 000 | Coins for a gold trophy. |
| `coinsNero` | `200` | 0 to 10 000 | Coins for a nero trophy. |

A trophy file's own `reward` overrides the tier amount (see [Adding Trophies](Adding-Trophies)).

## Notes for server owners

- **The retroactive budget** is what keeps a big server smooth when players join or a pack reloads. The defaults
  start at most two players a tick and spend at most 2 ms a tick. Raise them only if catch-up feels slow.
- **Coins are paid only for live awards**: never for retroactive catch-up, operator awards, claims or re-claims, and
  at most once per trophy per player, even if it is revoked and earned again.
- **Data retention** follows Neroland Core's `dataRetentionDays`: when set, records of players who have not logged in
  for that long are erased. See [Privacy](Privacy).
- Per-player switches (`announce`, `showname`) are not config: each player sets their own with
  [Commands](Commands) or the [Trophy Book](Trophy-Book).
