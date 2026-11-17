[![NeroLink App — Beta](https://img.shields.io/badge/NeroLink_App-Now_in_Beta-60d4e8?style=for-the-badge)](https://nerolandmc.net/nerolink/#beta) [![Explore the Neroland ecosystem](https://img.shields.io/badge/Explore-The_Neroland_Ecosystem-1a5a6c?style=for-the-badge)](https://nerolandmc.net/ecosystem/)

> 📱 **NeroLink App Beta — your Neroland world on your phone.** Check energy, alerts and machines live, claim quest rewards and search your storage without logging in. **[Join the beta at nerolandmc.net →](https://nerolandmc.net/nerolink/#beta)**
>
> 🌌 **Explore the Neroland ecosystem.** See how NeroTrophies fits together with the rest of the Nero mods — every mod, wiki and changelog in one place. **[View the ecosystem at nerolandmc.net →](https://nerolandmc.net/ecosystem/)** · [NeroTrophies on the website](https://nerolandmc.net/mods/nerotrophies/)

---

# NeroTrophies

**Earn trophies for what you do across every Neroland mod, then put them on display: pedestals, cabinets and plaques that turn your base into a record of everything you have achieved.**

NeroTrophies is the **achievement layer** of the Neroland ecosystem. It listens for milestones from vanilla Minecraft, Neroland Core and every Nero mod you have installed, and awards **trophies** in four tiers. Each trophy is a real block you can place, and it carries the gamertag of the player who earned it. Group them into **collections**, show them off in cabinets that light up as a collection fills, and follow your progress in the **Trophy Book** or in the **NeroLink app**.

Built on **Neroland Core**, the only mod it requires. Every other Nero mod is optional: its trophies simply appear when it is installed.

*(Beta — 0.1.0-beta.1, the first feature release.)*

---

## What it adds

1. **152 trophies to start with.** Trophies for vanilla Minecraft and Neroland Core, plus a set for every Nero mod: first rocket launch and every world visited in Nerospace, faction ranks, boss studies and bestiary progress, harvest milestones, ruin codices, colony growth, arcade play and more. Sixteen of them are **hidden** and show as "???" until you earn one.
2. **Four tiers.** **Bronze**, **silver**, **gold** and **nero**, each with its own trophy model, toast, sound and particles. Nero trophies are the rare ones, kept for each mod's hardest milestone.
3. **Twenty collections.** Named sets such as Astronaut, Boss Hunter and Codex, including collections that span several mods. Completing one unlocks a **cabinet trim**, and some award a trophy of their own.
4. **Trophies that show who earned them.** One trophy block renders any trophy: a tier base, a miniature of the trophy's icon and a nameplate engraved with the **earner's gamertag**. Place it, move it, gift it — it always shows who earned it.
5. **Display blocks.** They hold your trophies, or any other item you want to show off. A **Display Pedestal** with a slow turntable and comparator output by tier, a **Trophy Cabinet** and a slim **Wall Cabinet** that join side by side, bind to a collection and show ghost outlines for the trophies still missing, a **Trophy Plaque** with the trophy's title and earner, and a **Trophy Spotlight** to light your hall. Pedestals and cabinets take a colour finish from dyes — and from NeroDecor's colour tools when NeroDecor is installed.
6. **The Trophy Book.** A tab per mod, progress bars, tier filters, your collections and the re-claim button. Craft it, or bind a key to open it in Controls.
7. **Re-claim, never duplicate.** Lost a trophy? Re-claim it from the book. The new copy is genuine and the old one becomes a dull **replica** — no name, still decorative, never counted by a cabinet — so there is only ever one genuine copy of each trophy.
8. **Earned for what you've already done.** When a trophy is added later, or you join a server with progress behind you, it is awarded for work you've already done — quietly, without a burst of announcements.
9. **The trophy gallery.** `/nerotrophies gallery` (creative, operators) builds a walkable hall with every trophy, tier, display block, finish and trim, and `/nerotrophies gallery clear` puts the area back exactly as it was.

## Trophies in the NeroLink app

- 🏆 **Your trophy wall.** Every trophy grouped by mod, with tier filters and progress bars, a detail page for each one, and your collections.
- 🔔 **Optional notifications** when you earn a trophy or complete a collection — off by default, and they name the trophy only.
- 🔒 **Only your own record.** The app shows your trophies and nobody else's: no leaderboards, no other players' names, no coordinates.

## For pack makers and mod authors

Trophies and collections are plain JSON under `data/<namespace>/neroland_trophies/` and `data/<namespace>/neroland_trophy_collections/`. Six trigger types cover advancements, Neroland events, statistics, items obtained, blocks placed and dimensions visited. Any mod can award a trophy directly through Neroland Core's trophy API, without depending on NeroTrophies. A trophy for a mod that isn't installed is skipped quietly. See [Adding Trophies](https://github.com/Neroland/nerotrophies/wiki/Adding-Trophies) on the wiki.

## Fair by design

- 🎖️ **A record, never a gate.** Trophies are cosmetic. Nothing in any Nero mod needs a trophy to be reachable.
- 🪙 **Optional coin rewards** through Neroland Core's currency, **off by default**, paid only once per trophy and never for re-claims or past progress.
- 📣 **Announcements** in chat for gold and nero trophies, which each player can turn off for themselves.

## Privacy (POPIA / GDPR)

NeroTrophies stores one record per player, under their player UUID: the trophies they earned and when, a random claim token per trophy, completed collections and trims, which rewards were paid, their **last-known gamertag** (so their trophies can show it) and two toggles. Trophy items, blocks and cabinets hold only a trophy id and a token — **never a name or a UUID**. A trophy's gamertag is shown to other players who can see that trophy; turn that off any time with `/nerotrophies showname off`. Get a copy of your record with `/nerotrophies export`. Erase everything with Neroland Core's `/neroland data eraseme`: every trophy you earned, wherever it is in the world, shows no name from that moment, and your NeroTrophies data is cleared alongside every other Neroland mod. Inactive records follow Neroland Core's retention setting.

> **Telemetry notice:** NeroTrophies contains opt-out, anonymous crash reporting via Sentry (EU servers) — **on by default.** A report carries only the stack trace of an error in NeroTrophies code, the mod/game/loader/Java versions and the list of loaded mod ids — never IPs, usernames, UUIDs, gamertags, positions, world data or chat. Opt out any time by setting `telemetryEnabled=false` in `config/nerotrophies.properties`. Full details: [PRIVACY.md](https://github.com/Neroland/nerotrophies/blob/main/PRIVACY.md).

## Why it fits the ecosystem

- 🧩 **Built on Neroland Core** — the trophy registry and API live in Core, so every Nero mod and datapack can add trophies; NeroTrophies records them, displays them and shares them with the app. It uses Core's shared per-player erasure hook and ships in its own creative tab.
- 📱 **NeroLink module** — your trophy wall in the companion app.
- 🤝 **Interoperates, never hard-depends** — every sibling's trophies are inert data that light up when that mod is installed. NeroTrophies works in full on Core alone.
- 🧱 **Cross-loader** — NeoForge, Forge and Fabric on Minecraft **1.21.1** to **1.21.11** and **26.1.2**, **26.2** and **26.3**.

## Requirements & compatibility

- **Requires [Neroland Core](https://www.curseforge.com/minecraft/mc-mods/neroland-core) 1.20.0 or newer** — install it alongside NeroTrophies (it loads first).
- **Minecraft** 1.21.1, 1.21.4, 1.21.5, 1.21.8 (also 1.21.7), 1.21.10 (also 1.21.9), 1.21.11, 26.1.2, 26.2 and 26.3, on **NeoForge, Forge and Fabric** (Fabric also needs Fabric API).
- Optional: **NeroLink** for the app, any other Nero mod for its trophies, **NeroQuests** for the "Hall of Fame" quest chapter and **NeroDecor** for extra finishes. NeroTrophies is complete without them.
- **Modpacks are allowed and encouraged** — any platform, no need to ask. Use the official files and credit *NeroTrophies by Neroland* with links to the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/nerotrophies) and the [GitHub repository](https://github.com/Neroland/nerotrophies). Full terms: [LICENSE](https://github.com/Neroland/nerotrophies/blob/main/LICENSE).

## Links

- 📖 **[Wiki](https://github.com/Neroland/nerotrophies/wiki)** — getting started, the Trophy Book, tiers, display blocks, collections, the full trophy catalogue, adding trophies, commands and config.
- 💬 **[Discord](https://discord.gg/ArPXvYUzJG)** — chat, help, and sneak peeks.
- 🐞 **[Issues](https://github.com/Neroland/nerotrophies/issues)** — bug reports and feature requests.
- 🗒️ **[Changelog](https://github.com/Neroland/nerotrophies/blob/main/CHANGELOG.md)**

---

*Created by Neroland. The project logo was made with the help of AI image tools; in-game art is generated by the project's own tooling and refined by hand.*
