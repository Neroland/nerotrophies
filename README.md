# NeroTrophies

![NeroTrophies logo](art/logo/nerotrophies_logo_400.png)

**NeroTrophies** is the cross-mod achievement layer of the Neroland mod family. It listens for milestones from
every Nero mod, awards trophies defined in JSON, and gives you trophy blocks, pedestals and cabinets so your base
becomes a record of what you have done across the whole family.

## Features

- **A data-driven trophy engine.** Trophies and collections are JSON in Neroland Core's trophy format, earned by
  advancements, Neroland Core events (progression gates, material milestones, reputation, credits), vanilla
  statistics, items obtained, blocks placed or dimensions visited. Four tiers (bronze, silver, gold, nero), hidden
  trophies, and awards that are idempotent and retroactive within a per-tick budget.
- **A starter catalogue for the whole family:** 218 trophies and 21 collections, each lighting up only when its mod is
  installed. A Core-only server still has vanilla and NeroTrophies trophies to earn.
- **Your name on your trophies.** Every genuine trophy shows the gamertag of the player who earned it. Items and blocks
  hold only a trophy id and a claim token; re-claim a lost trophy and the old copy becomes a nameless replica.
- **Things to show off:** the trophy block, Display Pedestal, Trophy Cabinet, Wall Cabinet, Trophy Plaque and Trophy
  Spotlight, with collection binding, cabinet trims and colour finishes (NeroDecor's Chroma tools work too).
- **The Trophy Book:** your record by mod and tier, collections, claim and re-claim, and your privacy switches.
- **NeroLink:** a `nerotrophies` link module scoped to the asking player, for the companion app's Trophies screen.
- **For packs and mods:** add trophies with JSON, or award one from any mod through Neroland Core's `TrophyApi`.

Full documentation is in the [wiki](https://github.com/Neroland/nerotrophies/wiki).

## Requirements

- Minecraft 26.3 (also built for 26.2, 26.1.2 and 1.21.1 / 1.21.4 / 1.21.5 / 1.21.7-1.21.8 / 1.21.9-1.21.10 /
  1.21.11)
- Fabric (with Fabric API), NeoForge or Forge
- Minecraft 1.20 through 1.20.6 on Fabric, 1.20 through 1.20.4 and 1.20.6 on Forge, and 1.20.4 through 1.20.6 on NeoForge. Minecraft 1.20.5 on NeoForge requires loader 20.5.21-beta and Core 1.24.0 or newer. NeoForge 1.20.1 uses the Forge 1.20.1 jar.
- [Neroland Core](https://github.com/Neroland/neroland-core) 1.23.0 or newer (below 2.0)

NeroTrophies depends on Neroland Core and nothing else. Trophies never gate gameplay: they are a record and a
cosmetic.

## Privacy

NeroTrophies keeps one record per player who has earned a trophy: trophy ids, award times, claim tokens, completed
collections, the last-known gamertag and two switches (`announce`, `showname`). Trophy items and blocks never hold a
name or a UUID. `/nerotrophies showname off` hides your name on all your trophies at once, `/nerotrophies export`
shows your data, and Neroland Core's `/neroland data eraseme` erases it, after which every trophy you earned shows no
name. See [PRIVACY.md](PRIVACY.md).

## Building

```text
./gradlew :neoforge:26.3:build :fabric:26.3:build :forge:26.3:build
./gradlew :fabric:26.3:ecjCheck
```

Build each cell explicitly (`:<loader>:<minecraft>:build`); a plain `build` is not supported. Neroland Core is
resolved from `mavenLocal()` or GitHub Packages, see [USING-CORE.md](USING-CORE.md).

## License

All Rights Reserved; modpacks allowed. See [LICENSE](LICENSE).
