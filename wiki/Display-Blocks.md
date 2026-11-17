# Display Blocks

NeroTrophies adds one trophy block and five display blocks. The Display Pedestal, both cabinets and the Trophy Plaque
hold **any item**, one per slot: a trophy, or anything else you want to show off. Breaking any of them gives back the
block and everything inside it, exactly as it went in (enchantments, names, damage, contents and all).

| Block | How you get it | What it does |
|-------|----------------|--------------|
| Trophy | Claimed from the [Trophy Book](Trophy-Book) | Any trophy, placed in the world |
| Display Pedestal | Crafted | Holds one item on a slow turntable; comparator output by trophy tier |
| Trophy Cabinet | Crafted | A floor vitrine with six slots for any items; binds to a collection |
| Wall Cabinet | Crafted (makes 2) | A wall-hung case with two slots for any items; binds to a collection |
| Trophy Plaque | Crafted (makes 2) | A wall plaque showing one trophy's medallion, title and earner, or any item and its name |
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

Use any item on it to set it on the turntable; use it with an empty hand to take it back. A trophy shows its tier
model and nameplate; any other item turns on the turntable as its normal model.

Comparator output: 4 bronze, 8 silver, 12 gold, 15 nero, 1 for a replica or any other item, 0 when empty.

## Trophy Plaque

Hangs on a wall and shows one trophy's medallion, title and earner. Use any item on it to mount it; use it with an
empty hand to take it back. Any other item sits in the medallion with its own name as the title and a plain earner
strip (only a trophy has an earner).

## Putting things on display

- One item goes in each slot. Hold an item and use the pedestal or plaque; open a cabinet and move items into its
  slots.
- **Dyes and the four finish materials** (Nero Alloy Ingot, Starsteel Ingot, Void Crystal, Plasma Glass) recolour a
  pedestal or plaque instead of going on it (see [Finishes](#finishes-colour)). Put them in a cabinet through its
  screen if you want to show them.
- **Sneak** to place a block against a display instead of putting it in.
- Items are kept whole: enchantments, custom names, damage, a shulker box's contents. A very large item (a full
  shulker box of written books, for example) is still kept whole on the server, but other players' screens draw it
  as its plain item, so one display cannot flood the game with data.
- Only **genuine trophies** count toward a cabinet's collection. Other items never count, and they are not part of
  anyone's trophy record (see [Privacy](Privacy)).

## Trophy Cabinet and Wall Cabinet

Use a cabinet with an empty hand to open it. The screen has the slots (one item of any kind each), the collection the
cabinet is bound to, and its trim.

- **Binding to a collection.** In the Trophy Book, open a collection and press **Use on cabinets**, then sneak and use
  the book on the cabinet. A bound cabinet shows completion from the **genuine** trophies inside it, with ghost
  outlines for the members that are missing. Replicas and other items are never counted; an item in a slot just sits
  there.
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
