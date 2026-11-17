# Trophy Book

The Trophy Book is your record of every trophy you can earn and every one you hold. It is crafted (shapeless: book,
gold ingot, purple dye) and opened by using it, or with the "Open Trophy Book" key, which you can bind in the controls
menu (unbound by default).

The book only ever shows **your own** record. The server never sends you anyone else's.

## Layout

- **Tabs**: one per mod that has at least one trophy loaded on this server, plus "All trophies". A tab's tooltip shows
  how many of its trophies you have earned.
- **Overall progress**: how many of all loaded trophies you have earned.
- **Tier filters**: bronze, silver, gold and nero. Click one to show only that tier; the tooltip shows your count for
  it. While a tier filter is on, hidden trophies are not listed (their tier is a secret too).
- **Trophy list and detail**: each trophy shows its tier, title and state:
  - **Locked**: not earned yet. Statistic trophies show your progress (for example "3 / 10"), read live from vanilla
    statistics.
  - **Ready to claim**: earned, never claimed.
  - **Earned**: earned and claimed, with the date.
- **Hidden trophies** appear as **???** with "Keep playing to discover this trophy" until you earn one.
- **Collections**: every collection on the server, how many of its members you have, the cabinet trim it unlocks, and
  whether it is complete. See [Collections](Collections).

## Claim and re-claim

- **Claim** hands you a genuine trophy for an earned trophy. It goes to your inventory, or drops at your feet if that
  is full.
- **Re-claim** is for a trophy you lost. It asks you to click again to confirm, then gives you a new genuine copy. The
  old copy, wherever it is, becomes a **replica** with no name, and no cabinet counts it. There is never more than
  one genuine copy of your trophy.
- Re-claiming the same trophy has a cooldown (10 minutes by default, `reclaimCooldownSeconds`). Claiming and
  re-claiming never pay coins.

## Binding a cabinet

On a collection, press **Use on cabinets** while holding the book. Then sneak and use the book on a Trophy Cabinet or
Wall Cabinet: the cabinet (and any cabinets joined to it) binds to that collection. The book's tooltip shows the
collection it binds. See [Display Blocks](Display-Blocks).

## Privacy switches

At the bottom of the book:

- **Show my name**: on by default. Off blanks your name on every trophy you earned, for everyone, and stops chat
  announcements. Your trophies and record stay yours. Same as `/nerotrophies showname on|off`.
- **Announce gold and nero**: on by default. Off stops chat announcements of your gold and nero trophies. Needs "Show
  my name" on. Same as `/nerotrophies announce on|off`.
