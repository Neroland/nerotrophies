# Using Neroland Core

NeroTrophies hard-depends on **Neroland Core** and on nothing else. Milestones from the other Nero mods reach
NeroTrophies only through Core's APIs and event buses.

## Version pin and floor

`nerolandcore_version` in `gradle.properties` (currently **1.19.0**) is the Core version NeroTrophies compiles
against. Every manifest derives its floor from it, so the loader refuses a Core older than the one NeroTrophies
was built against:

- NeoForge / Forge: `versionRange = "[${nerolandcoreVersion},2.0)"`
- Fabric: `"nerolandcore": ">=${nerolandcore_version} <2.0.0"`

Pin a **tagged** Core release, never a working-tree version.

## Resolving Core

Core is consumed per cell as `za.co.neroland.nerolandcore:nerolandcore-<loader>-<mc>:<version>`, resolved
from:

1. `mavenLocal()` — run Core's `publishToMavenLocal` for local development, or
2. GitHub Packages (`NerolandCoreGitHubPackages` in `stonecutter.gradle`) — needs `gpr.user` / `gpr.key`
   Gradle properties, or `GITHUB_ACTOR` / `GITHUB_TOKEN` in the environment (CI has both).

## Core APIs NeroTrophies uses

| Core API | Used for |
|----------|----------|
| `config.ConfigSchema` / `ConfigManager` / `ConfigValue` | `config/nerotrophies.properties` |
| `registry.RegistrationProvider` | Cross-loader registration (the creative tab so far) |

NeroTrophies adds nothing to Core's creative tab and nothing to Core's network channel. The player-data store,
and with it `data.PlayerDataErasure` and `data.SavedDataRecovery`, arrives in a later version.
