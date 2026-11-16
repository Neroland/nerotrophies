# NeroTrophies

NeroTrophies is the cross-mod achievement layer of the Neroland mod family: trophies for milestones across every
Nero mod, and trophy blocks, pedestals and cabinets to show them off in your base.

**Current version: 0.0.1-alpha.1.** This is an early scaffold: the mod loads, adds its (empty) creative tab and
its config file, and does nothing else yet.

## Requirements

- Minecraft 26.3 (older lines down to 1.21.1 are also built)
- Fabric (with Fabric API), NeoForge or Forge
- Neroland Core 1.19.0 or newer

## Configuration

`config/nerotrophies.properties`:

| Key | Default | What it does |
|-----|---------|--------------|
| `telemetryEnabled` | `true` | Anonymous NeroTrophies-only crash reports. Set `false` to opt out (takes effect on restart). This build sends nothing. |

## Privacy

Version 0.0.1-alpha.1 stores no player data at all. Trophies never gate gameplay.
