# Link Module

NeroTrophies registers the `nerotrophies` module (schema 1) with Neroland Core's link API. The NeroLink bridge mod
serves it to paired companion apps, where it appears as a **Trophies** screen: a trophy wall grouped by mod with tier
filters and progress, a detail page per trophy, and your collections. The module is listed only on servers that run
NeroTrophies, so the app hides the screen everywhere else.

**Every answer is scoped to the player the device is paired to.** The app can read your own record and nothing else:
no other player's record, no leaderboard, no coordinates, no gamertags (not even yours), no claim tokens and no UUIDs.
Gallery display copies never appear.

## Sections

All are `GET /nerotrophies/<section>` with no parameters.

| Section | What it returns |
|---------|-----------------|
| `trophies` | Every loaded trophy with its mod, tier, icon item, title and description, whether you have earned it and when, and your progress on statistic trophies while you are online. Hidden trophies you have not earned are listed only as "???" with their mod: no id, tier, icon or description. |
| `collections` | Every loaded collection: title, icon, the trim it unlocks, whether you completed it, and which of its members you hold (hidden members you have not earned stay anonymous). |
| `summary` | Counts: earned and total per tier, per mod and for collections, and when you last earned a trophy. Hidden trophies are included in the counts only. |

Each trophy and collection carries a short stable `key` (`t_…` or `c_…`, derived from its id) that the app uses for
links. Titles and descriptions come resolved in English, along with their lang keys for apps that ship translations.
Times are epoch milliseconds.

## Actions

None in this version. The app cannot claim, re-claim, award or change anything.

## Events and notifications

The module sends two events, to the earner only:

| Event | When |
|-------|------|
| `TROPHY_EARNED` | You earned a trophy while playing, or a mod awarded you one. |
| `COLLECTION_COMPLETED` | You completed a collection. |

Retroactive catch-up and operator awards send nothing, and at most five events a minute are sent per player.

Push notifications use the category **Trophies**, which is **off by default**; turn it on in the app's notification
settings. A notification says "Trophy earned" or "Collection complete" and the trophy or collection's title, nothing
more: no name, no position. Tapping it opens the Trophies screen in the app.

## Requirements

The NeroLink bridge mod on the server and a paired companion app. NeroTrophies does not need the bridge; without it the
module is simply never asked.
