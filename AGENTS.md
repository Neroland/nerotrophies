# NeroTrophies — agent guide

> Distribution: uploads go to CurseForge and GitHub Releases only. Do not add references to any other mod-hosting platform.

NeroTrophies is the cross-mod achievement layer of the Neroland mod family, built on **Neroland Core**. It listens
for milestones from every Nero mod, awards **trophies** defined in JSON, and gives players physical trophy blocks,
pedestals and cabinets so a base becomes a record of what its owner has done across the ecosystem. The trophy
registry and API live in Neroland Core; this mod is the provider, the store and the blocks.

**Status: 0.0.1-alpha.1 — empty scaffold.** One entry class per loader, common init, the creative tab, config and
opt-out telemetry. No content, no store, no network payloads yet.

## Repository layout

| Path | What lives there |
|------|------------------|
| `common/src/main/java/za/co/neroland/nerotrophies/` | Loader-agnostic code (vanilla + Core APIs only, no loader imports) |
| `common/src/main/resources/` | Shared assets, lang, mixin config, `pack.mcmeta`, mod-list logo |
| `fabric/`, `forge/`, `neoforge/` | One branch per loader: entry points, `PlatformInfo` implementation, manifest templates |
| `tools/` | `ecj.prefs` (analyzer settings), `gen_logo.py` (family logo), the Gradle MCP server |
| `art/` | Logo outputs |
| `wiki/` | Player-facing wiki pages, published to the public GitHub wiki |

Common packages so far:

| Package | What lives there |
|---------|------------------|
| (root) | `NeroTrophiesCommon`: `MOD_ID`, `LOGGER`, the numbered `init()` |
| `platform` | `PlatformInfo` (loader facts) and `Services` (resolved once during init) |
| `config` | `NeroTrophiesConfig` (Core `ConfigSchema`, file `config/nerotrophies.properties`; only `telemetryEnabled` so far) |
| `telemetry` | `NeroTrophiesTelemetry` (opt-out Sentry, placeholder DSN), `SentryLogAppender`, `ForeignMixinFailure` |
| `registry` | `ModCreativeTab` (id `nerotrophies`, title `itemGroup.nerotrophies`, through Core's `RegistrationProvider`) |

## Build

Stonecutter produces 27 cells: three loaders x nine Minecraft lines (1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10,
1.21.11, 26.1.2, 26.2, 26.3). Source is written for 26.x (26.3 is the target); older lines use `//?` blocks,
`legacyRenames` in `stonecutter.gradle` or `common/src/main/resources-<mc>` overlays. Java 25 for 26.x, Java 21
bytecode for 1.21.x.

Never run a plain `build`: the root `:fabric`, `:forge` and `:neoforge` projects are vestigial containers. Build
the cells explicitly, 26.x first, then 1.21.11 down to 1.21.1:

```text
./gradlew :neoforge:26.3:build :fabric:26.3:build :forge:26.3:build \
          :neoforge:26.2:build :fabric:26.2:build :forge:26.2:build ...
./gradlew :fabric:26.3:ecjCheck
```

All 27 cells must build and `:fabric:26.3:ecjCheck` must pass at every gate. `ecjCheck` runs the Eclipse
analyzer with `tools/ecj.prefs` (the same settings VS Code uses) and fails only on errors. Any unit tests in
`common/src/test/java` run as part of every NeoForge cell build.

Neroland Core is resolved from `mavenLocal()` or GitHub Packages; see [USING-CORE.md](USING-CORE.md).

## Ground rules

- **POPIA/GDPR.** The mod stores exactly one kind of player data: player UUID to earned trophy ids, award
  timestamps, a claim token per trophy, the player's last-known gamertag (for display on their trophies) and
  their two toggles (announce, show name). All of it lives in one saved-data store. Logs, telemetry, alerts and
  pushes carry no personal data: no UUIDs, names, coordinates or free text. Erasure, export, retention and
  `PRIVACY.md` land in the same change as the store. A second player-keyed store needs an owner decision.
- **Every trophy shows the gamertag of the player who earned it, and shows no name once that player's data is
  erased.**
- **Neroland Core is the only hard dependency.** No sibling mod in any manifest and no reflection into a
  sibling. Sibling milestones are reached through Core's buses, vanilla advancements and statistics by id,
  optional tags (`"required": false`) and, only where nothing else works, `isModLoaded` plus a `compat/*`
  façade guarded against `LinkageError`.
- **Server-authoritative everything.** Awards, claims, collection completion and reward grants resolve on the
  server. Never use nearest-player lookups for credit, ownership or push recipients. Every client-to-server
  payload is validated against the sender.
- **Trophies never gate gameplay.** They are a record and a cosmetic. Rewards are cosmetic or small and
  config-tunable. Nothing in another mod may become unreachable without a trophy.
- **English only for new text**, and every string lives in the lang file. No datagen: JSON is hand-authored or
  produced by Python tools in `tools/`.

## Conventions

- **Core floor is derived, never hardcoded.** `nerolandcore_version_range: "[${nerolandcoreVersion},2.0)"` in
  the NeoForge and Forge builds, `">=${nerolandcore_version} <2.0.0"` in `fabric.mod.json`. Pin
  `nerolandcore_version` to a tagged Core release.
- **Every `SavedData` goes through Core's `SavedDataRecovery`.** A corrupt `.dat` must never become a repeating
  crash. Use Core's guard, never a local copy.
- **Player data routes through Core's `PlayerDataErasure`** (POPIA/GDPR): minimise it, set retention limits,
  support export, erasure and opt-out.
- **Own network channel `nerotrophies:main`** when payloads arrive; never add to Core's network registry.
- **Platform seam.** Loader facts come from `Services.PLATFORM`, resolved during init and never lazily on a tick
  path.
- **Numbered init.** `NeroTrophiesCommon.init()` runs fixed, numbered steps; new work fills the reserved slot.
- **One `MenuOpener` for every GUI** once screens arrive. Never call `Player#openMenu` directly.
- **Block-entity renderers cull by distance and frustum** and never return a blanket `true` from `shouldRender`.
- **Item highlight tags** (`neroland:highlight/*`) on every item.
- **Config through Core's `ConfigSchema`** (`config/nerotrophies.properties`).
- **Opt-out Sentry telemetry** with no personal data. The DSN is a compile-time constant in
  `NeroTrophiesTelemetry`; it is still the placeholder, so the build sends nothing. Keep the placeholder guard so
  a fork stays silent.
- **`publish.yml` rules.** Direct-curl CurseForge upload with `max-parallel: 1` and the 4-attempt 5xx retry
  loop. The CurseForge project id is the `CURSEFORGE_PROJECT_ID` secret (not created yet). No automatic
  dependency-update workflow.
- **Own creative tab** (`registry/ModCreativeTab`, id `nerotrophies`, title `itemGroup.nerotrophies`). Never add
  items to Core's tab.
- **Never commit, tag or push.** Leave version control to the owner.
- **The wiki is public.** Everything under `wiki/` is published as is; keep private planning material out of it
  and out of the repository. `wiki.yml` runs a guard that fails on such references.
- **`CLAUDE.md` and `AGENTS.md` stay byte-identical.** Update `wiki/`, `CHANGELOG.md` and `PRIVACY.md` in the
  same change as the feature.

## Translation workflow

English is the source language for new and changed text. Community translations are intended to be contributed through Weblate; other-language translations are not required when implementing a feature. Preserve existing translations. This supersedes older instructions that require translating every change.
