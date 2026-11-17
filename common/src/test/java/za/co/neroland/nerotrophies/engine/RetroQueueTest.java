package za.co.neroland.nerotrophies.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** The retroactive scheduler keeps to its budget, resumes, skips earned trophies and awards what is due. */
class RetroQueueTest {

    private static final long NO_TIME_LIMIT = Long.MAX_VALUE;

    private static List<TrophyDefinition> defs(int n) {
        List<TrophyDefinition> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(TestDefs.trophy(String.format("t%04d", i), TrophyTier.BRONZE));
        }
        return out;
    }

    /** A fake game: every even-numbered trophy is satisfied. */
    private static final class Game implements RetroQueue.Checks {
        final Set<String> held = new HashSet<>();
        final Set<UUID> online = new HashSet<>();
        int satisfiedCalls;
        int awards;
        RetroQueue queue;

        @Override
        public boolean online(UUID player) {
            return online.contains(player);
        }

        @Override
        public boolean has(UUID player, Identifier trophy) {
            return held.contains(player + trophy.toString());
        }

        @Override
        public boolean satisfied(UUID player, TrophyDefinition definition) {
            satisfiedCalls++;
            return Integer.parseInt(definition.id().getPath().substring(1)) % 2 == 0;
        }

        @Override
        public boolean award(UUID player, Identifier trophy) {
            if (!held.add(player + trophy.toString())) {
                return false;
            }
            awards++;
            queue.noteAward(player); // what the post-award listener does for RETROACTIVE awards
            return true;
        }
    }

    @Test
    void passesRespectTheCheckBudgetAndResume() {
        RetroQueue q = new RetroQueue();
        Game g = new Game();
        g.queue = q;
        UUID p = UUID.randomUUID();
        g.online.add(p);
        q.enqueue(p, 40);
        List<TrophyDefinition> d = defs(100);

        assertTrue(q.tick(39, d, 1, g, 2, 30, NO_TIME_LIMIT, System::nanoTime).isEmpty(), "not due before login delay");
        assertEquals(0, g.satisfiedCalls);
        assertTrue(q.tick(40, d, 1, g, 2, 30, NO_TIME_LIMIT, System::nanoTime).isEmpty());
        assertEquals(30, q.checksLastTick());
        q.tick(41, d, 1, g, 2, 30, NO_TIME_LIMIT, System::nanoTime);
        q.tick(42, d, 1, g, 2, 30, NO_TIME_LIMIT, System::nanoTime);
        List<RetroQueue.Finished> done = q.tick(43, d, 1, g, 2, 30, NO_TIME_LIMIT, System::nanoTime);
        assertEquals(1, done.size());
        assertEquals(50, done.get(0).awarded());
        assertEquals(100, g.satisfiedCalls);
        assertFalse(q.isQueued(p));
    }

    @Test
    void earnedTrophiesAreSkippedAndAwardsAreIdempotent() {
        RetroQueue q = new RetroQueue();
        Game g = new Game();
        g.queue = q;
        UUID p = UUID.randomUUID();
        g.online.add(p);
        List<TrophyDefinition> d = defs(10);
        q.enqueue(p, 0);
        q.tick(0, d, 1, g, 2, 512, NO_TIME_LIMIT, System::nanoTime);
        assertEquals(5, g.awards);
        q.enqueue(p, 0);
        List<RetroQueue.Finished> again = q.tick(1, d, 1, g, 2, 512, NO_TIME_LIMIT, System::nanoTime);
        assertEquals(0, again.get(0).awarded());
        assertEquals(15, g.satisfiedCalls, "the second pass only checks the five unearned ones");
    }

    @Test
    void playersStartedPerTickAreCappedAndOfflinePlayersAreDropped() {
        RetroQueue q = new RetroQueue();
        Game g = new Game();
        g.queue = q;
        UUID[] ps = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        for (UUID p : ps) {
            g.online.add(p);
            q.enqueue(p, 0);
        }
        g.online.remove(ps[1]);
        List<RetroQueue.Finished> done = q.tick(0, defs(4), 1, g, 2, 512, NO_TIME_LIMIT, System::nanoTime);
        assertEquals(1, done.size(), "two started, one of them offline");
        assertTrue(q.isQueued(ps[2]), "the third waits for the next tick");
        assertFalse(q.isQueued(ps[1]));
        done = q.tick(1, defs(4), 1, g, 2, 512, NO_TIME_LIMIT, System::nanoTime);
        assertEquals(ps[2], done.get(0).player());
    }

    @Test
    void theTimeBudgetStopsWorkAndAGenerationChangeRestartsThePass() {
        RetroQueue q = new RetroQueue();
        Game g = new Game();
        g.queue = q;
        UUID p = UUID.randomUUID();
        g.online.add(p);
        q.enqueue(p, 0);
        long[] clock = {0};
        // Each clock read advances 1 ms; budget 3 ms.
        q.tick(0, defs(50), 1, g, 2, 512, 3_000_000L, () -> clock[0] += 1_000_000L);
        assertTrue(q.checksLastTick() < 50 && q.checksLastTick() > 0);
        int before = g.satisfiedCalls;
        q.tick(1, defs(50), 2, g, 2, 512, NO_TIME_LIMIT, System::nanoTime);
        assertTrue(g.satisfiedCalls - before >= 25, "restarted from the top for the new generation");
        q.cancel(p);
        assertFalse(q.isQueued(p));
    }
}
