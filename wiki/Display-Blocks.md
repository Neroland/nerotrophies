# Display Blocks

NeroTrophies adds one trophy block and five display blocks. Breaking any of them gives back the block and everything
inside it.

| Block | How you get it | What it does |
|-------|----------------|--------------|
| Trophy | Claimed from the [Trophy Book](Trophy-Book) | Any trophy, placed in the world |
| Display Pedestal | Crafted | Holds one trophy (or a displayable item) on a slow turntable; comparator output by tier |
| Trophy Cabinet | Crafted | A floor vitrine with six slots; binds to a collection |
| Wall Cabinet | Crafted (makes 2) | A wall-hung case with two slots; binds to a collection |
| Trophy Plaque | Crafted (makes 2) | A wall plaque showing one trophy's medallion, title and earner |
| Trophy Spotlight | Crafted (makes 2) | A ceiling lamp or wall arm that lights your display (light level 14) |

## Recipes

| Block | Recipe |
|-------|--------|
| Display Pedestal | Gold ingot on top, three iron ingots in the middle, three smooth stone below |
| Trophy Cabinet | Iron ingots top and bottom rows; glass, purple wool, glass in the middle |
| Wall Cabinet | Three iron ingots over glass pane, purple wool, glass pane |
| Trophy Plaque | Iron nuggets in the corners, iron ingots on the edges, purple wool in the centre |
| Trophy Spotlight | An iron nugget over iron ingot, glowstone, iron ingot |
| Trophy Book | Shapeless: book, gold ingot, purple dye |

The recipes unlock in your recipe book as you play.

## The trophy

There is one trophy item and one trophy block for every trophy: the block renders the tier's model with a miniature
of the trophy's icon on top and a nameplate below. The nameplate shows the earner's gamertag, or a plain ornament
when there is no name to show (see [Tiers](Tiers) for replicas and blank trophies). The tooltip says the tier, who
earned it and whether it is genuine or a replica.

The item and block hold only the trophy id and a claim token, never a name or a UUID. The server works out the name
and sends it only to players who can see the trophy.

## Display Pedestal

Use a trophy on it to set it on the turntable; use it with an empty hand to take the trophy back. It also accepts any
item in the item tag `nerotrophies:displayable` (empty by default, so a pack can add its own showpieces).

Comparator output: 4 bronze, 8 silver, 12 gold, 15 nero, 1 for a replica or any other item, 0 when empty.

## Trophy Plaque

Hangs on a wall and shows one trophy's medallion, title and earner. Use a trophy on it to mount it; use it with an
empty hand to take it back. Plaques hold trophies only.

## Trophy Cabinet and Wall Cabinet

Use a cabinet with an empty hand to open it. The screen has the trophy slots, the collection the cabinet is bound to,
and its trim.

- **Binding to a collection.** In the Trophy Book, open a collection and press **Use on cabinets**, then sneak and use
  the book on the cabinet. A bound cabinet shows completion from the **genuine** trophies inside it, with ghost
  outlines for the members that are missing. Replicas are never counted.
- **Joining cabinets.** Up to four cabinets of the same kind, side by side, facing the same way and bound to the same
  collection count together as one display. A big collection fits in a row of cabinets.
- **Trims.** Completing a collection unlocks its cabinet trim. Pick a trim in the cabinet screen with the previous and
  next buttons; you can choose Standard or any trim you have earned. The whole row of joined cabinets takes the same
  trim.
- A cabinet reads what is inside it and stores no identity of its own: a collection id and a trim name.

## Finishes (colour)

The Display Pedestal, both cabinets and the Trophy Plaque take a colour finish. They ship in a neutral graphite.

- Use any **vanilla dye** on one to give it that colour, or one of four Neroland Core materials for a material
  finish: Nero Alloy Ingot, Starsteel Ingot, Void Crystal or Plasma Glass. The item is not used up.
- **Sneak and use with an empty hand** to clear the finish.
- With **NeroDecor** installed, its Chroma tools recolour these blocks too.

The trophy itself and the Trophy Spotlight cannot be recoloured: a trophy's colour is its tier.

## Creative mode

In creative mode a trophy can be copied like any item (pick block, middle-click). A copy carries the same stamp, so it
shows the same earner and looks genuine. Survival play cannot copy a trophy: the only way to get another genuine copy
is re-claiming from the Trophy Book, which turns every older copy into a replica.
