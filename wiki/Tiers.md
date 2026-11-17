# Tiers

Every trophy has one of four tiers. The tier sets the trophy's model, its colour, its award sound and toast, and how a
Display Pedestal signals it. You can tell tiers apart by shape first, then by colour, so they read at a glance and for
colour-blind players.

| Tier | What it is for | Announced in chat by default | Default coin reward (off by default) | Pedestal comparator signal |
|------|----------------|------------------------------|--------------------------------------|----------------------------|
| Bronze | First steps in a mod | No | 10 | 4 |
| Silver | Real progress | No | 25 | 8 |
| Gold | Big milestones | Yes | 75 | 12 |
| Nero | The hardest milestone of a mod; at most one per mod | Yes | 200 | 15 |

A replica or any other item on a pedestal gives signal 1; an empty pedestal gives 0.

## Announcements

When you earn a gold or nero trophy live (not retroactively, not from an operator command), every online player sees
"*name* earned the gold trophy *title*" in chat. You are named only if both your **announce** and **showname**
switches are on, and only if the server has vanilla `announceAdvancements` on. Servers choose which tiers are
announced with `announceTiers` (see [Config](Config)).

## Coin rewards

Off by default. When a server turns on `rewardsEnabled` and has a real currency provider (such as NeroEconomy), a live
award pays the tier's coins once. Retroactive awards, operator awards, claims and re-claims never pay, and a trophy
pays at most once per player even if it is revoked and earned again. A trophy file can set its own amount.

## Replicas and blank trophies

A **replica** is a copy whose claim is no longer current: its earner re-claimed it, or an operator revoked the
trophy. It turns dull pewter whatever its tier, shows no name, is never counted by a cabinet, and gives a pedestal
signal of 1.

A **blank** trophy keeps its tier look but shows a plain ornament where the name would be. That is what every trophy
of a player whose data was erased looks like, and what any trophy looks like while its earner has `showname` off. A
blank trophy whose earner was erased counts for nothing.
