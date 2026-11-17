# Commands

Everything is under `/nerotrophies`. Output about a player goes only to the person who ran the command, and nothing
here writes a name, UUID or token to the server log.

## For every player

| Command | What it does |
|---------|--------------|
| `/nerotrophies list` | Your earned trophies, with tier and title, and how many of the loaded trophies you have. |
| `/nerotrophies announce on` / `off` | Whether your gold and nero trophies are announced in chat. Same as the "Announce gold and nero" switch in the [Trophy Book](Trophy-Book). |
| `/nerotrophies showname on` / `off` | Whether your name shows on your trophies (and in announcements). Off blanks it on every trophy you earned, at once; your record is kept. |
| `/nerotrophies export` | Your whole trophy record, sent to your own chat as JSON you click to copy (in parts if it is long). Players only; it is never written to a file or the log. |

## For operators (permission level 2)

| Command | What it does |
|---------|--------------|
| `/nerotrophies list <player>` | That player's earned trophy ids and award times, shown to you only. |
| `/nerotrophies award <player> <trophy>` | Award a trophy. Pays no coins and makes no announcement. Tab-completes loaded trophy ids. |
| `/nerotrophies revoke <player> <trophy>` | Remove a trophy from a player's record. Any copy of it becomes a replica. If the player still meets the trophy's trigger, it can be awarded again (no second coin reward). |
| `/nerotrophies reload` | Re-read trophy data, report how many trophies and collections loaded, and re-check online players. |

## For administrators (permission level 3)

| Command | What it does |
|---------|--------------|
| `/nerotrophies export <player>` | Export a player's record to the administrator who asked, to answer a data request on their behalf. |

## Gallery (creative, operators)

Run by a player in creative mode with operator permission; refused from the console or in survival.

| Command | What it does |
|---------|-----------------|
| `/nerotrophies gallery` | Build a walkable trophy hall next to you showing every loaded trophy, tier, display block, finish and trim, using display copies with a placeholder name. Creative mode, operators only. Gallery copies award nothing and count for nothing. |
| `/nerotrophies gallery clear` | Remove the gallery completely and restore the area exactly as it was, even after a restart. |

## Related commands from Neroland Core

| Command | What it does |
|---------|--------------|
| `/neroland data eraseme` | Erase your data from every Nero mod, NeroTrophies included. See [Privacy](Privacy). |
| `/neroland data erase <uuid>` | An operator's erasure on a player's behalf (permission level 2), by the player's UUID. |
| `/neroland config reload` | Operators: reload `config/nerotrophies.properties` along with the other Nero configs. |
