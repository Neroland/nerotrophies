# NeroTrophies

NeroTrophies is the cross-mod achievement layer of the Neroland mod family. It listens for milestones from every Nero
mod, awards **trophies** defined in JSON, and gives you trophy blocks, pedestals and cabinets so your base becomes a
record of what you have done across the whole family. Every genuine trophy carries the name of the player who earned
it.

## Requirements

- Minecraft 26.3 (also built for 26.2, 26.1.2, 1.21.11, 1.21.10, 1.21.8, 1.21.5, 1.21.4 and 1.21.1)
- NeoForge, Forge or Fabric (Fabric also needs Fabric API)
- Minecraft 1.20.5 on Fabric, and 1.20.6 on all three loaders
- **Neroland Core 1.23.0 or newer** (below 2.0). Core is the only required mod.
- Optional: any other Nero mod, whose trophies light up when it is installed, and the NeroLink bridge mod for the
  companion app

Trophies never gate gameplay. They are a record and a cosmetic: nothing in any mod needs a trophy.

## Pages

**Playing**

- [Getting Started](Getting-Started)
- [Trophy Book](Trophy-Book): your record, claiming and re-claiming, your privacy switches
- [Tiers](Tiers): bronze, silver, gold and nero
- [Display Blocks](Display-Blocks): the trophy, pedestal, cabinets, plaque and spotlight
- [Collections](Collections): sets of trophies that unlock cabinet trims

**The catalogue**

- [Core and Vanilla](Catalogue-Core-and-Vanilla)
- [Space and Tech](Catalogue-Space-and-Tech): Nerospace, Nerotech, NeroPower, NeroLogistics
- [Society](Catalogue-Society): NeroEconomy, NeroFactions, NeroColonies, NeroSecurity, NeroEvents, NeroQuests
- [Life and World](Catalogue-Life-and-World): NeroCreatures, NeroAgriculture, NeroCompanion, NeroRuins
- [Play and Craft](Catalogue-Play-and-Craft): NeroDecor, NeroNotes, NeroArcade, NeroMaps, NeroLink

**Running a server**

- [Commands](Commands)
- [Config](Config)
- [Link Module](Link-Module): what the companion app can read
- [Privacy](Privacy)

**Making a pack or a mod**

- [Adding Trophies](Adding-Trophies): the JSON format, and awarding trophies from your own mod

## Privacy in one paragraph

NeroTrophies keeps one record per player who has earned a trophy: trophy ids, award times, claim tokens, completed
collections, the last-known gamertag and two switches. Trophy items and blocks never hold a name or a UUID. Your
gamertag shows on your trophies to players who can see them; `/nerotrophies showname off` hides it everywhere at once.
`/nerotrophies export` shows your data, and Neroland Core's `/neroland data eraseme` removes it, after which every
trophy you earned shows no name. The full text is on [Privacy](Privacy) and in the mod's `PRIVACY.md`.

## Crash reports

NeroTrophies can send anonymous, NeroTrophies-only crash reports. They carry no IP, name, UUID, position or world
data. Set `telemetryEnabled=false` in `config/nerotrophies.properties` to opt out (takes effect on restart).
