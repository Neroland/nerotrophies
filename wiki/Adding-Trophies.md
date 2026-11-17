# Adding Trophies

Trophies and collections are plain JSON in a datapack or a mod's resources. The format belongs to **Neroland Core**
(1.20.0 or newer), so a pack or a sibling mod adds trophies **depending on nothing but Core**. NeroTrophies is the mod
that evaluates them, keeps the record and shows them; without it the files still load and simply do nothing.

## Where the files go

| What | Path | Id |
|------|------|----|
| A trophy | `data/<namespace>/neroland_trophies/<path>.json` | `<namespace>:<path>` |
| A collection | `data/<namespace>/neroland_trophy_collections/<path>.json` | `<namespace>:<path>` |

`<path>` may contain `/`, for example `mypack:space/first_launch`. A collection may not share its id with a trophy.
`/reload` (or `/nerotrophies reload`) picks up changes and re-checks online players, so new trophies are awarded for
work already done.

## A trophy

```json
{
  "schema": 1,
  "icon": "minecraft:elytra",
  "tier": "gold",
  "trigger": { "type": "advancement", "advancement": "minecraft:end/elytra" },
  "hidden": false,
  "title": "trophy.mypack.wings.title",
  "description": "trophy.mypack.wings.desc",
  "category": "mypack",
  "order": 10,
  "reward": { "coins": 50 }
}
```

| Key | Required | Meaning |
|-----|----------|---------|
| `icon` | yes | An item id, shown as the miniature on the trophy. If the item is not registered, the trophy is skipped quietly. |
| `tier` | yes | `bronze`, `silver`, `gold` or `nero` (see [Tiers](Tiers)). |
| `trigger` | no | What earns it (below). Without a trigger, a trophy is earned only when a mod calls `TrophyApi.award`, or as a collection's completion trophy. |
| `hidden` | no | `true` shows it as "???" until earned. Default `false`. Hidden trophies cannot be collection members. |
| `title`, `description` | no | **Lang keys**, not text (`[a-z0-9_.-]`, up to 128 characters). Default `trophy.<namespace>.<path>.title` and `.desc`, with `/` in the path turned into `.`. Put the English text in your own `en_us.json`. |
| `requires_mod` | no | A mod id, or a list of up to 8. If any is missing, the trophy is skipped quietly. |
| `category` | no | The Trophy Book tab. Default: the first `requires_mod`, else the namespace. Give the tab a name with the lang key `nerotrophies.category.<category>`. |
| `order` | no | Sort order inside the tab (then by id). Default `0`. |
| `reward` | no | `{ "coins": n }`, 0 to 10 000. Overrides the tier's default; paid only when the server turned rewards on and has a currency provider. |
| `schema` | no | Default `1`. Unknown keys are ignored, so files written for later versions still load. |

## Triggers

Target ids are matched as text and never looked up, so a trigger naming a mod that is not installed loads fine and
never fires.

| `type` | Fields | Earned when |
|--------|--------|-------------|
| `advancement` | `advancement` | The advancement is completed. Don't use recipe advancements (`<ns>:recipes/...`): they fire on ingredient pickup. |
| `nero_event` | `event` and the fields below | A Neroland Core event for that player. |
| `stat` | `stat_type` (default `minecraft:custom`), `stat`, `at_least` | A vanilla statistic reaches the value, for example `"stat_type": "minecraft:killed", "stat": "minecraft:warden"`. |
| `item_obtained` | `item`, `count` (default 1) | The player picked up, crafted or holds that many. A gift counts. |
| `block_placed` | `block`, `count` (default 1) | The player placed that many by hand. |
| `dimension_visited` | `dimension` | The player is in the dimension. Past visits do not count; prefer the world's advancement if it has one. |

`nero_event` kinds:

| `event` | Fields | Earned when |
|---------|--------|-------------|
| `gate` | `gate` | A **player-scope** progression gate opens for the player. Team and server gates never award. |
| `material_milestone` | `milestone`, optional `material` | A player-scope material milestone is reached. |
| `reputation` | `faction`, `at_least` | The player's standing with the faction rises to the value (needs a reputation provider such as NeroFactions). |
| `currency` | `currency` (default `nerolandcore:credits`), `at_least` | The player's balance rises to the value (needs a currency provider such as NeroEconomy). |

Numbers are whole numbers: `at_least` and `count` from 1 to 2 147 483 647.

**Prefer `advancement` and `nero_event`.** They award the moment the thing happens and can be checked retroactively.
`stat`, `item_obtained`, `block_placed` and `dimension_visited` are checked every few seconds per player (see
`pollIntervalTicks` on [Config](Config)). Keep counts honest: a trophy should mark something a player did, not a grind.

## A collection

```json
{
  "schema": 1,
  "icon": "minecraft:dragon_egg",
  "title": "collection.mypack.end_tour.title",
  "description": "collection.mypack.end_tour.desc",
  "trim": "laurel_gold",
  "members": ["mypack:wings", "mypack:dragon"],
  "trophy": "mypack:end_tour_complete"
}
```

- `members`: trophy ids, 2 or more. A member skipped because its mod is absent is dropped quietly; a collection left
  with fewer than two members is not loaded. Keep collections to 24 members or fewer so they fit a row of cabinets.
- `trim`: the cabinet trim it unlocks. NeroTrophies knows these: `standard`, `orbital_plasma`, `orbital_amber`,
  `laurel_gold`, `laurel_crimson`, `laurel_copper`, `wild_green`, `wild_sand`, `wild_teal`, `neon_pink`, `neon_rose`,
  `prism`.
- `trophy` (optional): a trophy with no trigger, awarded when the collection completes. It must not be a member.
- `requires_mod` works as on trophies.

## What gets skipped

Nothing in a trophy file can crash the game. Files over 16 KiB, invalid JSON or values out of range are rejected and
counted in one summary line in the log; a missing `requires_mod` or icon item is skipped without a word. There are hard
limits on files and members, so a huge or broken pack cannot lag a server.

## Awarding from your own mod

A mod with a Neroland Core 1.20.0 dependency can award a trophy directly, with no NeroTrophies dependency:

```java
TrophyApi.award(serverPlayer, Identifier.fromNamespaceAndPath("mymod", "first_boss"));
```

It returns `true` when the trophy was newly awarded, and `false` (doing nothing) when the player already has it, the
id is not loaded, or no trophy mod is installed. Call it on the server thread. Ship the trophy's JSON with no
`trigger` and NeroTrophies treats it as a live award: toast, sound, announcement and reward rules all apply.

Also in `za.co.neroland.nerolandcore.trophy`: `TrophyApi.has`, `TrophyApi.earned`, `TrophyApi.definitions`, and the
events `TrophyEvents.onAwarded`, `onRevoked`, `onCollectionCompleted` and `onRecordErased`. Neroland Core's own wiki
page "Trophies" documents the full contract.

## Checking your files

Load the pack and run `/nerotrophies reload`: the reply says how many trophies and collections loaded. If files were
rejected, the server log has one warning line with the counts. A trophy that should be there but is not usually names
a mod or an icon item that is not installed.
