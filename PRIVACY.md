# NeroTrophies privacy notice

NeroTrophies follows the Neroland family's privacy rules (POPIA and GDPR): store as little as possible, keep it no
longer than needed, and let every player export, erase and opt out.

## What NeroTrophies stores

NeroTrophies keeps exactly one kind of player data, in **one** saved-data store per world
(`data/nerotrophies/trophy_state.dat` on Minecraft 26.x, `data/nerotrophies_trophy_state.dat` on 1.21.x). A record
is created only when a player earns their first trophy or changes one of their toggles. It holds:

- the player's UUID;
- the ids of the trophies they have earned, and when each was awarded;
- a random **claim token** per trophy (so a lost trophy can be claimed again and an old copy recognised as a
  replica), whether it has been claimed and when it was last claimed;
- the cabinet trims and collections they have completed;
- which trophies have already paid a coin reward (so a reward is never paid twice);
- their **last-known gamertag** and when it was last refreshed (kept only for a player who has at least one trophy,
  refreshed when they log in), so their trophies can show who earned them;
- two personal toggles: whether their gold and nero trophies are announced in chat (`announce`), and whether their
  trophies show their name (`showname`).

Nothing else: no positions, no dimensions, no chat, no progress counters (statistic progress is read live from
vanilla statistics and never stored), and nothing about one player inside another player's record.

**Trophy items and blocks never carry a name or a UUID.** A trophy carries only its trophy id and its claim token.
The server looks the token up in the store to decide whose name to show, and sends that name only to players who
can see that trophy right now. Clients keep those names in memory only and never write them to disk.

**Other items on display are not NeroTrophies' record.** The Display Pedestal, Trophy Cabinet, Wall Cabinet and
Trophy Plaque hold any item a player puts in, exactly like a chest: the item is saved with the block, whole, with
everything the item itself carries, and is shown to anyone who can see the block. Some items carry player data of
their own (a player head, a signed book's author, a custom name someone typed). That data belongs to the item and to
the game, not to NeroTrophies: it is not added to your trophy record, never shown in the Trophy Book or the companion
app, never exported or counted, and Neroland's erasure does not edit items in the world, just as it does not edit a
chest's contents. Take the item out (or break the block) to remove it.

## Your name on your trophies

Every genuine trophy shows the gamertag of the player who earned it: on its nameplate, in its tooltip, and on
pedestals, plaques and cabinets that hold it. Other players who can see the trophy will see that name. If an
announcement is enabled, other players online also see your name in chat when you earn a gold or nero trophy.

To turn this off, run **`/nerotrophies showname off`** (or untick "Show my name on my trophies" in the Trophy Book).
Your name disappears from all your trophies at once, everywhere, and you are not named in chat either; your record
is kept. `/nerotrophies showname on` brings it back. **`/nerotrophies announce off`** stops chat announcements only.

## Seeing your data (export)

Run **`/nerotrophies export`** in game. Your whole record is sent to your own chat as JSON you can click to copy
(split into parts if it is very large). It holds every field listed under "What NeroTrophies stores" (including your claim tokens, as hex) and nothing about anyone else, and it is not written to
a file or the server log. Server administrators (permission level 3) can run `/nerotrophies export <player>` to
answer a request on a player's behalf; the export is sent to the administrator who asked.

Neroland Core's **`/neroland data exportme`** (Core 1.20.1 or later) includes the same record under `nerotrophies`,
with two differences: it leaves out your UUID (Core's export is already yours alone), and it never shows a claim
token itself, only whether each trophy has one (`has_claim_token`), because that export is a copy you may pass
on. It works whether you are online or not, is sent to you only, and is never logged or written to disk. A player
with no record gets nothing from NeroTrophies in it.

## Erasing your data

NeroTrophies registers with Neroland Core's data-erasure hook. One request removes you from NeroTrophies along
with every other Nero mod: `/neroland data eraseme`, or an operator's `/neroland data erase`. Erasure:

- deletes your whole record, including your gamertag and every claim token;
- refreshes the store's backup straight away, so restoring a backup or restarting the server cannot bring it back;
- tells every connected player to forget your name in the same server tick;
- removes any quarantined copy of a damaged store file that could still hold your record, even when the live
  store no longer has a record for you;
- if you are online, records nothing more for you until you next log in.

From that moment every trophy you earned, wherever it is (placed, in a chest, a cabinet, another player's ender
chest, or an unloaded part of the world), shows **no name**. Nothing in the world needs to be found or rewritten,
because the trophies never held your name.

What erasure does not change:

- Trophies that follow from progress the game itself still keeps (advancements, statistics, progression gates) are
  recorded again at your next login, silently, without coins or an announcement, because that progress is still
  yours. Erase the game's own data too (or ask the server operator) if you want a fresh start.
- An announcement of a trophy you earned put your gamertag into other players' chat, and so into their own client
  logs, which no server can reach. Turn announcements off with `/nerotrophies announce off`, or hide your name with
  `/nerotrophies showname off`.
- `/nerotrophies gallery` keeps a snapshot (`gallery.dat`) of the blocks the hall replaced until
  `/nerotrophies gallery clear`. It holds blocks and their block-entity data from that area, which could include
  another mod's data stored in those blocks, and nothing NeroTrophies records about players.

## How long it is kept

Records are kept while you play. If the server sets Neroland Core's `dataRetentionDays`, Core's retention sweep
erases the records of players who have not logged in for that many days, exactly as an erasure request would.
NeroTrophies keeps no activity data of its own.

## Logs, alerts and notifications

NeroTrophies' logs never contain a UUID, a gamertag, a token, a position or free text: counts and trophy ids only,
and trophy ids only at debug level. Chat announcements are sent to each player directly, so they do not appear in
the server log either. Companion-app notifications, alerts and crash reports carry no personal data.

## Crash reports (telemetry)

NeroTrophies can send anonymous crash reports about its own errors to Sentry (EU servers). In this build no
reporting project is configured, so **nothing is sent**. When reporting is switched on in a later build:

- It is on by default and you can turn it off: set `telemetryEnabled=false` in `config/nerotrophies.properties`
  (takes effect on restart). This setting is yours; a server cannot change it.
- A report holds the stack trace, the NeroTrophies, Minecraft, loader, operating system and Java versions, and
  the ids and versions of your other installed mods.
- It never holds your IP address, username, UUID, position, world data or chat. File paths are scrubbed of your
  account name. Only errors from NeroTrophies code are sent, at most ten per session.
- NeroTrophies reports through its own Sentry client, separate from every other mod's, so no other mod can
  silence it or receive its reports.

## Contact

Questions or requests: <info@neroland.co.za>, or open an issue at <https://github.com/Neroland/nerotrophies/issues>.
