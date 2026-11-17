package za.co.neroland.nerotrophies.trigger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

/**
 * Live detection for the polled triggers ({@code stat}, {@code item_obtained}, {@code block_placed},
 * {@code dimension_visited}). Each online player is visited once every {@code interval} ticks, staggered by a slot
 * assigned on first sight so the load spreads evenly; one visit evaluates at most {@link #CHECKS_PER_VISIT} unearned
 * definitions and the rest carries to the next visit. Earned definitions are skipped before counting.
 *
 * <p>Pure scheduling: the game is reached only through {@link Visit}. Server thread only.
 */
public final class PeriodicPass {

    /** Checks per player visit. */
    public static final int CHECKS_PER_VISIT = 256;

    /** How a visit reaches the game. */
    public interface Visit {
        boolean earned(TrophyDefinition definition);

        boolean satisfied(TrophyDefinition definition);

        /** Award with source {@code LIVE}. */
        void award(TrophyDefinition definition);
    }

    private static final class Slot {
        final int slot;
        int cursor;

        Slot(int slot) {
            this.slot = slot;
        }
    }

    private final Map<UUID, Slot> players = new HashMap<>();
    private int nextSlot;

    /** Whether the player is due a visit on this tick (assigns a slot on first sight). */
    public boolean due(UUID player, long tick, int interval) {
        Slot s = players.computeIfAbsent(player, p -> new Slot(nextSlot++ & Integer.MAX_VALUE));
        int i = Math.max(1, interval);
        return Math.floorMod(tick + s.slot, (long) i) == 0;
    }

    /**
     * One visit: evaluates unearned definitions from the player's cursor, at most {@link #CHECKS_PER_VISIT}; the cursor
     * returns to the start once the list has been covered.
     *
     * @return the number of checks performed
     */
    public int visit(UUID player, List<TrophyDefinition> polled, Visit visit) {
        Slot s = players.computeIfAbsent(player, p -> new Slot(nextSlot++ & Integer.MAX_VALUE));
        if (s.cursor >= polled.size()) {
            s.cursor = 0;
        }
        int checks = 0;
        while (s.cursor < polled.size() && checks < CHECKS_PER_VISIT) {
            TrophyDefinition d = polled.get(s.cursor++);
            if (visit.earned(d)) {
                continue;
            }
            checks++;
            if (visit.satisfied(d)) {
                visit.award(d);
            }
        }
        if (s.cursor >= polled.size()) {
            s.cursor = 0;
        }
        return checks;
    }

    /** The player left: drop the slot and cursor. */
    public void forget(UUID player) {
        players.remove(player);
    }

    public void clear() {
        players.clear();
        nextSlot = 0;
    }

    /** Players currently scheduled (tests). */
    public int tracked() {
        return players.size();
    }
}
