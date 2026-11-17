package za.co.neroland.nerotrophies.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

/**
 * The budgeted retroactive re-check. A player is queued on login (due 40 ticks later), on a definition
 * change and on {@code /nerotrophies reload}; each tick at most {@code playersPerTick} queued players start, at most
 * {@code checksPerTick} unearned definitions are evaluated in total, and work stops once {@code nanosBudget} of wall
 * time is spent. A pass resumes from its cursor next tick. Awards it finds are {@code RETROACTIVE}: no coins, no
 * announcement, no per-trophy toast; one summary at the end.
 *
 * <p>Pure scheduling: the game is reached only through {@link Checks}, so it is tested without a server. Server
 * thread only. At most one entry per player; re-enqueueing restarts the pass.
 */
public final class RetroQueue {

    /** How the queue reaches the game. */
    public interface Checks {
        /** Whether the player is still online (an offline player's pass is dropped). */
        boolean online(UUID player);

        /** Whether the player already holds the trophy (earned ones are skipped before counting). */
        boolean has(UUID player, Identifier trophy);

        /** The retroactive read ({@link TriggerEvaluator#retroactive}). */
        boolean satisfied(UUID player, TrophyDefinition definition);

        /** Award with source {@code RETROACTIVE}; true if newly earned. */
        boolean award(UUID player, Identifier trophy);
    }

    /** A pass that reached the end of the definition list. */
    public record Finished(UUID player, int awarded) {
    }

    private static final class Pass {
        int cursor;
        long generation;
        int awarded;

        Pass(long generation) {
            this.generation = generation;
        }
    }

    private final Map<UUID, Long> waiting = new LinkedHashMap<>();
    private final Map<UUID, Pass> active = new LinkedHashMap<>();
    private int checksLastTick;

    /** Queue (or restart) a player's pass, due at {@code dueTick}. */
    public void enqueue(UUID player, long dueTick) {
        active.remove(player);
        waiting.put(player, dueTick);
    }

    /** The player left: forget them. */
    public void cancel(UUID player) {
        waiting.remove(player);
        active.remove(player);
    }

    public void clear() {
        waiting.clear();
        active.clear();
    }

    /** A {@code RETROACTIVE} award for {@code player} was recorded (counts toward the summary toast). */
    public void noteAward(UUID player) {
        Pass pass = active.get(player);
        if (pass != null) {
            pass.awarded++;
        }
    }

    /** Whether nothing is waiting or running (the tick then does no work at all). */
    public boolean idle() {
        return waiting.isEmpty() && active.isEmpty();
    }

    public boolean isQueued(UUID player) {
        return waiting.containsKey(player) || active.containsKey(player);
    }

    /** Definition checks performed by the last {@link #tick} (for tests and diagnostics). */
    public int checksLastTick() {
        return checksLastTick;
    }

    /**
     * One server tick of work. {@code definitions} is the generation's list of triggered definitions in a stable
     * order. Returns the passes that finished this tick.
     */
    public List<Finished> tick(long nowTick, List<TrophyDefinition> definitions, long generation, Checks game,
            int playersPerTick, int checksPerTick, long nanosBudget, LongSupplier nanoClock) {
        checksLastTick = 0;
        List<Finished> finished = new ArrayList<>();
        int started = 0;
        for (Iterator<Map.Entry<UUID, Long>> it = waiting.entrySet().iterator(); it.hasNext() && started < playersPerTick;) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() <= nowTick) {
                it.remove();
                active.put(e.getKey(), new Pass(generation));
                started++;
            }
        }
        if (active.isEmpty()) {
            return finished;
        }
        long start = nanoClock.getAsLong();
        int checks = 0;
        for (Iterator<Map.Entry<UUID, Pass>> it = active.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Pass> e = it.next();
            UUID player = e.getKey();
            Pass pass = e.getValue();
            if (!game.online(player)) {
                it.remove();
                continue;
            }
            if (pass.generation != generation) {
                pass.generation = generation;
                pass.cursor = 0;
            }
            while (pass.cursor < definitions.size()) {
                if (checks >= checksPerTick || nanoClock.getAsLong() - start >= nanosBudget) {
                    checksLastTick = checks;
                    return finished;
                }
                TrophyDefinition def = definitions.get(pass.cursor++);
                if (game.has(player, def.id())) {
                    continue;
                }
                checks++;
                if (game.satisfied(player, def)) {
                    game.award(player, def.id());
                }
            }
            it.remove();
            finished.add(new Finished(player, pass.awarded));
        }
        checksLastTick = checks;
        return finished;
    }
}
