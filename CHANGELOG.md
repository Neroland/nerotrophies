# Changelog

All notable changes to NeroTrophies are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [0.1.0-beta.1] — 2026-11-17

First feature release. Requires **Neroland Core 1.20.0** (the release that adds Core's trophy API).

### Added

- **Trophy engine.** NeroTrophies is Neroland Core's trophy provider. Trophies are defined in JSON under
  `data/<namespace>/neroland_trophies/` and collections under `data/<namespace>/neroland_trophy_collections/`;
  any mod or datapack can add more, and other mods can award one directly through Core's `TrophyApi.award`.
  Awards are recorded once, each with a random claim token, and are followed by collection completion (with its
  cabinet trim and optional completion trophy), the optional coin reward, the chat announcement and the toast.
- **Six trigger types:** `advancement`, `nero_event` (player-scoped progression gates, material milestones,
  reputation and currency crossings), `stat`, `item_obtained`, `block_placed` and `dimension_visited`. Ids are
  matched as strings, so trophies for mods that aren't installed cost nothing and are skipped quietly.
- **Retroactive awards:** on login, datapack reload and `/nerotrophies reload`, players are re-checked within a
  per-tick budget, so trophies added later are awarded for work already done (silently, without coins).
- **Four tiers** — bronze, silver, gold and nero — each with its own model, toast, sound and particles.
- **Starter catalogue:** 152 trophies across Neroland Core, vanilla Minecraft and every Neroland mod (bronze 63,
  silver 51, gold 31, nero 7; 16 hidden until earned) and 20 collections, including cross-mod ones. Each lights
  up only when its mod is installed.
- **One trophy item and block** that renders any trophy: a tier base, a miniature of the trophy's icon and a
  nameplate engraved with the earner's gamertag. The item stores only the trophy id and its claim token.
- **Display blocks:** Display Pedestal (slow turntable, comparator output by tier), Trophy Cabinet and Wall
  Cabinet (join side by side, bind to a collection, show ghost outlines for missing trophies and the collection's
  trim), Trophy Plaque (title and earner) and Trophy Spotlight. Pedestals and cabinets take a colour finish from
  dyes or Core materials, and from NeroDecor's colour tools when NeroDecor is installed.
- **Trophy Book:** a tab per mod, progress bars, tier filters, collections, hidden trophies shown as "???", and
  re-claim. Opens from the item or a keybind.
- **Re-claim:** mints a new token so a lost copy becomes a dull replica (no name, never counted by a cabinet);
  there is only ever one genuine copy. 10-minute cooldown; never pays coins.
- **Award feedback:** award toast for a trophy, a completed collection and a login summary; tier sounds
  (original, with subtitles) and particles.
- **NeroLink:** a `nerotrophies` link module (schema 1) that serves only the asking player's own trophies,
  collections and counts, plus trophy-earned and collection-completed events (push category `nerotrophies`, off by
  default; the message names the trophy only).
- **`/nerotrophies gallery`** (creative, players only): a walkable hall with a bay per mod, a tier row, a genuine
  trophy beside its replica, a cabinet per collection in empty, partial and complete states, every finish and trim,
  and a demo plate for the toast and sound. Gallery copies use a placeholder name, award nothing and count for
  nothing. `/nerotrophies gallery clear` restores the area exactly, even after a restart.
- A "Hall of Fame" NeroQuests chapter, shipped as data (used only when NeroQuests is installed).
- Commands: `/nerotrophies list`, `announce on|off`, `showname on|off`, `export`, `gallery`, `gallery clear`;
  operator `list <player>`, `award`, `revoke`, `reload`; admin `export <player>`.
- Config: `announceTiers`, `reclaimCooldownSeconds`, `retroPlayersPerTick`, `retroChecksPerTick`,
  `retroMillisPerTick`, `pollIntervalTicks`, `rewardsEnabled`, `coinsBronze|Silver|Gold|Nero`.
- Tools: a trophy-data validator, an asset checker and a wiki catalogue generator, all run in CI; generators for
  textures, models, sounds and particles.

### Known limitations

- Trophies for milestones that a mod doesn't report per player yet are not shipped (see "Catalogue scope").
  Threshold events and progression gates that only open for a recorded machine owner are not used.
- Dyeing a pedestal or cabinet works on a plain right-click (vanilla skips sneak-use while an item is held).
- A renamed trophy item loses its custom name when placed.
- The toast's gold and nero sheen always plays; there is no reduced-motion option yet.
- The gallery is capped at 96 blocks long.

### Privacy

- One store (`trophy_state`) holds everything NeroTrophies knows about a player: UUID, earned trophy ids and award
  times, claim tokens, completed collections and trims, which rewards were paid, the last-known gamertag and the
  `announce` / `showname` toggles. Trophy items, blocks, cabinets and chunks hold a trophy id and a token only.
- Erasure through Neroland Core (`/neroland data eraseme` or an operator's erase) removes the whole record and its
  tokens, refreshes the store's backup at once, tells every connected player to forget the name in the same tick and
  fires `TrophyEvents.RecordErased`. Every copy of that player's trophies then shows no name, wherever it is.
- Core's `dataRetentionDays` erases the records of players inactive for longer, through the same eraser.
- `/nerotrophies export` sends your own record to your chat as click-to-copy JSON, never to a file or the log. It now
  also lists coin rewards paid for trophies that were revoked later.
- `/nerotrophies showname off` hides your name on all your trophies and from announcements without erasing anything.
- Logs carry counts and trophy ids only; crash reports carry no personal data and stay opt-out.
- Verified by a server game test on Fabric 26.3 (`:fabric:26.3:runGameTest`): real block-entity, item, player and
  chunk data hold no UUID or name, and after erasure no store file on disk (primary, backup or quarantine) does.

### Catalogue scope

- The catalogue aims for at least six trophies per Neroland mod. A few ship fewer for now: NeroLink 0, NeroQuests 3,
  NeroMaps 4, NeroArcade 5 and NeroNotes 5. More arrive when those mods expose the events.

## [0.0.1-alpha.1]

### Added

- Project scaffold: one entry point per loader (Fabric, NeoForge, Forge) across 27 builds (Minecraft 1.21.1,
  1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3).
- Hard dependency on Neroland Core 1.20.0 (accepts any Core from 1.20.0 below 2.0).
- The NeroTrophies creative tab (empty for now).
- `config/nerotrophies.properties` with the `telemetryEnabled` opt-out.
- Anonymous, opt-out crash reporting, inert in this build (no reporting project is configured yet).
