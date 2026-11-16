# NeroTrophies privacy notice

NeroTrophies follows the Neroland family's privacy rules (POPIA and GDPR): store as little as possible, keep it no
longer than needed, and let every player export, erase and opt out.

## What 0.0.1-alpha.1 stores

**Nothing.** This version keeps no data about any player and writes no world data at all.

## What later versions will store

This section is a placeholder. It will be completed, with retention periods and the exact export and erasure
steps, in the same release that adds the store.

NeroTrophies is planned to keep exactly one kind of player data, in one saved-data store per world:

- the player's UUID and the ids of the trophies they have earned;
- when each trophy was awarded;
- a claim token per trophy (so a lost trophy block can be claimed again and a copy recognised as a replica);
- the player's last-known gamertag, so their trophies can show who earned them;
- two personal toggles: whether awards are announced in chat, and whether trophies show the player's name.

That store will register with Neroland Core's data-erasure hook, so one request (`/neroland data eraseme` or an
operator's erase) removes the player from NeroTrophies along with every other Nero mod. Once a player's data is
erased, their trophies show no name.

Logs, crash reports, alerts and companion-app notifications never carry personal data: no UUIDs, names,
coordinates or free text.

## Crash reports (telemetry)

NeroTrophies can send anonymous crash reports about its own errors to Sentry (EU servers). In this build no
reporting project is configured, so **nothing is sent**. When reporting is switched on in a later build:

- It is on by default and you can turn it off: set `telemetryEnabled=false` in `config/nerotrophies.properties`
  (takes effect on restart). This setting is yours; a server cannot change it.
- A report holds the stack trace, the NeroTrophies, Minecraft, loader, operating system and Java versions, and
  the ids and versions of your other installed mods.
- It never holds your IP address, username, UUID, position, world data or chat. File paths are scrubbed of your
  account name. Only errors from NeroTrophies code are sent, at most ten per session.

## Contact

Questions or requests: <info@neroland.co.za>, or open an issue at <https://github.com/Neroland/nerotrophies/issues>.
