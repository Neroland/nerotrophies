# Changelog

All notable changes to NeroTrophies are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- The trophy engine: NeroTrophies is now Neroland Core's trophy provider. Awards are recorded once (idempotent),
  each with a random claim token, and every award by any mod through Core's `TrophyApi` gets the same follow-up:
  collection completion (with its cabinet trim and optional completion trophy), the optional coin reward, the chat
  announcement and the earner's toast.
- One player-data store, `nerotrophies:trophy_state` (schema 1), read through Core's saved-data recovery guard. The
  token index is rebuilt on load and never saved.
- Claim tokens: claim and re-claim with a 10-minute cooldown; an old copy becomes a replica, a forged or erased one
  shows no name. Reserved tokens for gallery copies.
- Retroactive re-checks on login, on datapack reload and on `/nerotrophies reload`, within a per-tick budget.
- Optional coin rewards through Core's currency API: off by default, and paid only with a real currency provider,
  only for live awards, once per trophy.
- Chat announcements for gold and nero trophies, sent per player (never echoed to the server log), and only when
  the earner has both `announce` and `showname` on.
- Commands: `/nerotrophies list`, `announce on|off`, `showname on|off`, `export`; operator `list <player>`,
  `award`, `revoke`, `reload`; admin `export <player>`.
- Data erasure through Core's erasure hook (record, gamertag and tokens removed, backup refreshed, names dropped on
  every client the same tick), retention through Core's sweep, and the export command.
- Config: `announceTiers`, `reclaimCooldownSeconds`, `retroPlayersPerTick`, `retroChecksPerTick`,
  `retroMillisPerTick`, `pollIntervalTicks`, `rewardsEnabled`, `coinsBronze|Silver|Gold|Nero`.
- The `nerotrophies:trophy` data component (trophy id and token only), plus `nerotrophies:finish` and
  `nerotrophies:book_selection`.
- Registry ids for the six blocks and the Trophy Book (placeholder behaviour for now), and the `nerotrophies:main`
  payload channel.

## [0.0.1-alpha.1]

### Added

- Project scaffold: one entry point per loader (Fabric, NeoForge, Forge) across 27 builds (Minecraft 1.21.1,
  1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3).
- Hard dependency on Neroland Core 1.20.0 (accepts any Core from 1.20.0 below 2.0).
- The NeroTrophies creative tab (empty for now).
- `config/nerotrophies.properties` with the `telemetryEnabled` opt-out.
- Anonymous, opt-out crash reporting, inert in this build (no reporting project is configured yet).
