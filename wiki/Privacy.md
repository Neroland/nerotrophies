# Privacy

NeroTrophies follows the Neroland family's privacy rules (POPIA and GDPR): store as little as possible, keep it no
longer than needed, and let every player see, erase and switch off what is kept about them. The mod's `PRIVACY.md` is
the full text; this page says the same in short.

## What is stored

One record per player who has earned a trophy (or changed one of their switches), in one saved-data file per world:

- your UUID;
- the trophies you earned and when;
- a random claim token per trophy, whether you claimed it and when you last claimed it;
- the collections and cabinet trims you completed;
- which trophies already paid a coin reward;
- your **last-known gamertag** and when it was refreshed (on login), so your trophies can show who earned them;
- your two switches, `announce` and `showname`.

Nothing else: no positions, no chat, no progress counters (statistic progress is read live from vanilla statistics),
and nothing about one player inside another's record.

**Trophy items and blocks never hold a name or a UUID.** They hold a trophy id and a claim token. The server looks the
token up to decide whose name to show, and sends it only to players who can see that trophy right now. Clients keep
those names in memory only.

**Other items on display are not your trophy record.** The Display Pedestal, cabinets and Trophy Plaque hold any item,
exactly like a chest: the item is saved with the block, whole, and anyone who can see the block sees it. An item can
carry player data of its own (a player head, a signed book's author, a custom name). That belongs to the item, not to
NeroTrophies: it is never added to your record, shown in the Trophy Book or the app, exported or counted, and erasure
does not edit items in the world (as it does not edit a chest). Take the item out to remove it.

## Your name on your trophies

Every genuine trophy shows the gamertag of the player who earned it, on its nameplate, in its tooltip, and on
pedestals, plaques and cabinets that hold it. Other players who can see the trophy see that name. If announcements are
on, other players also see your name in chat when you earn a gold or nero trophy.

- **`/nerotrophies showname off`** (or untick "Show my name" in the [Trophy Book](Trophy-Book)) blanks your name on
  every trophy you earned, everywhere, at once, and stops you being named in chat. Your record is kept.
  `showname on` brings it back.
- **`/nerotrophies announce off`** stops chat announcements only.

These four places are the only ones your name appears: the trophy's nameplate, its tooltip, the display blocks
holding it, and a chat announcement. The companion app never shows a gamertag.

## See your data

`/nerotrophies export` sends your whole record to your own chat as JSON you click to copy. It contains every field
above (claim tokens as hex) and nothing about anyone else. It is not written to a file or to the server log.
Administrators (permission level 3) can run `/nerotrophies export <player>` to answer a request on a player's behalf.

## Erase your data

NeroTrophies is part of Neroland Core's erasure: `/neroland data eraseme` removes you from every Nero mod at once (an
operator can do it for you). Erasure deletes your whole record, including your gamertag and every claim token,
refreshes the store's backup straight away so a restore or restart cannot bring it back (and deletes any quarantined
copy of a damaged store file, even if the live store no longer has your record), tells every connected player to
forget your name in the same tick, and records nothing more for you until you next log in.

From that moment every trophy you earned, wherever it is (placed, in a chest or cabinet, in another player's ender
chest, or in a part of the world nobody has loaded), shows **no name**. Nothing in the world has to be found and
rewritten, because the trophies never held your name.

What erasure does not change:

- Trophies that follow from progress the game still keeps (advancements, statistics, progression gates) are recorded
  again at your next login, silently and without coins or an announcement.
- An announcement put your gamertag into other players' chat and their own client logs, which no server can reach.
  `/nerotrophies announce off` stops announcements; `/nerotrophies showname off` hides your name.
- The trophy gallery keeps a snapshot (`gallery.dat`) of the blocks it replaced until `/nerotrophies gallery clear`.

## How long it is kept

While you play. If the server sets Neroland Core's `dataRetentionDays`, records of players who have not logged in for
that many days are erased exactly as an erasure request would.

## Logs, notifications and crash reports

- Logs hold counts and trophy ids only (ids only at debug level): never a UUID, gamertag, token, position or free
  text. Chat announcements are sent to each player directly, so they do not appear in the server log.
- Companion-app notifications name the trophy only and are off by default. See [Link Module](Link-Module).
- Crash reports are anonymous and NeroTrophies-only: no IP, username, UUID, position, world data or chat, and file
  paths are scrubbed of your account name. They are on by default; set `telemetryEnabled=false` in
  `config/nerotrophies.properties` to opt out (takes effect on restart). In this build no reporting project is
  configured, so nothing is sent.

## Contact

Questions or requests: <info@neroland.co.za>, or open an issue at <https://github.com/Neroland/nerotrophies/issues>.
