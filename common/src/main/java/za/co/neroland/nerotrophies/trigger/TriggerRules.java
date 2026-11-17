package za.co.neroland.nerotrophies.trigger;

import java.util.Optional;
import java.util.OptionalLong;

import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

import za.co.neroland.nerotrophies.engine.TriggerEvaluator;

/**
 * Whether a trigger is met, and progress toward a counted one, as pure functions of {@link PlayerFacts}. The same
 * reads serve the retroactive pass, the periodic pass and the {@code record} payload, so the Book's progress bar and
 * the award can never disagree. An unknown trigger type or {@code nero_event} kind (a later Core) is never met.
 */
public final class TriggerRules {

    private TriggerRules() {
    }

    /** True if the player has already done what the trigger asks. */
    public static boolean satisfied(TrophyTrigger trigger, PlayerFacts facts) {
        if (trigger instanceof TrophyTrigger.Advancement a) {
            return facts.advancementDone(a.advancement());
        }
        if (trigger instanceof TrophyTrigger.NeroEvent e) {
            return neroEvent(e, facts);
        }
        if (trigger instanceof TrophyTrigger.Stat s) {
            return facts.stat(s.statType(), s.stat()) >= s.atLeast();
        }
        if (trigger instanceof TrophyTrigger.ItemObtained i) {
            return facts.itemsObtained(i.item()) >= i.count() || facts.inventoryCount(i.item()) >= i.count();
        }
        if (trigger instanceof TrophyTrigger.BlockPlaced b) {
            return facts.blocksPlaced(b.block()) >= b.count();
        }
        if (trigger instanceof TrophyTrigger.DimensionVisited d) {
            return d.dimension().equals(facts.dimension());
        }
        return false;
    }

    /**
     * Progress toward a {@code stat}, {@code item_obtained} or {@code block_placed} trigger; empty for every other
     * trigger and when the target id is unknown to the game.
     */
    public static Optional<TriggerEvaluator.Progress> progress(TrophyTrigger trigger, PlayerFacts facts) {
        if (trigger instanceof TrophyTrigger.Stat s) {
            return of(facts.stat(s.statType(), s.stat()), s.atLeast());
        }
        if (trigger instanceof TrophyTrigger.ItemObtained i) {
            long obtained = facts.itemsObtained(i.item());
            long held = facts.inventoryCount(i.item());
            return of(Math.max(obtained, held), i.count());
        }
        if (trigger instanceof TrophyTrigger.BlockPlaced b) {
            return of(facts.blocksPlaced(b.block()), b.count());
        }
        return Optional.empty();
    }

    /** Whether the trigger is one the periodic pass polls (statistics, inventory, current dimension). */
    public static boolean polled(TrophyTrigger trigger) {
        return trigger instanceof TrophyTrigger.Stat || trigger instanceof TrophyTrigger.ItemObtained
                || trigger instanceof TrophyTrigger.BlockPlaced || trigger instanceof TrophyTrigger.DimensionVisited;
    }

    /** A rising crossing: {@code old < atLeast <= now}. */
    public static boolean crossed(long old, long now, long atLeast) {
        return old < atLeast && atLeast <= now;
    }

    private static boolean neroEvent(TrophyTrigger.NeroEvent e, PlayerFacts facts) {
        TrophyTrigger.NeroEvent.Kind kind = e.event();
        if (kind == TrophyTrigger.NeroEvent.Kind.GATE) {
            return facts.playerGateOpen(e.target());
        }
        if (kind == TrophyTrigger.NeroEvent.Kind.MATERIAL_MILESTONE) {
            return facts.playerMilestoneObserved(e.target(), e.material());
        }
        if (kind == TrophyTrigger.NeroEvent.Kind.REPUTATION) {
            OptionalLong rep = facts.reputation(e.target());
            return rep.isPresent() && rep.getAsLong() >= e.atLeast();
        }
        if (kind == TrophyTrigger.NeroEvent.Kind.CURRENCY) {
            OptionalLong bal = facts.balance(e.target());
            return bal.isPresent() && bal.getAsLong() >= e.atLeast();
        }
        return false;
    }

    private static Optional<TriggerEvaluator.Progress> of(long value, int target) {
        if (value < 0) {
            return Optional.empty();
        }
        return Optional.of(new TriggerEvaluator.Progress((int) Math.min(value, target), target));
    }
}
