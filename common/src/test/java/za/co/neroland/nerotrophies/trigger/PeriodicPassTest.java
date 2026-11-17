package za.co.neroland.nerotrophies.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** The periodic pass keeps to its per-visit budget, resumes, skips earned trophies and staggers players. */
class PeriodicPassTest {

    private static List<TrophyDefinition> defs(int n) {
        List<TrophyDefinition> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(TestDefs.trophy(String.format("p%04d", i), TrophyTier.BRONZE));
        }
        return out;
    }

    private static final class Game implements PeriodicPass.Visit {
        final Set<TrophyDefinition> earned = new HashSet<>();
        final Set<TrophyDefinition> due = new HashSet<>();
        int satisfiedCalls;
        int awards;

        @Override
        public boolean earned(TrophyDefinition definition) {
            return earned.contains(definition);
        }

        @Override
        public boolean satisfied(TrophyDefinition definition) {
            satisfiedCalls++;
            return due.contains(definition);
        }

        @Override
        public void award(TrophyDefinition definition) {
            awards++;
            earned.add(definition);
        }
    }

    @Test
    void visitIsCappedAndResumes() {
        PeriodicPass pass = new PeriodicPass();
        UUID p = UUID.randomUUID();
        List<TrophyDefinition> polled = defs(600);
        Game g = new Game();
        assertEquals(PeriodicPass.CHECKS_PER_VISIT, pass.visit(p, polled, g));
        assertEquals(PeriodicPass.CHECKS_PER_VISIT, pass.visit(p, polled, g));
        assertEquals(600 - 2 * PeriodicPass.CHECKS_PER_VISIT, pass.visit(p, polled, g));
        assertEquals(600, g.satisfiedCalls, "every definition checked exactly once over three visits");
        assertEquals(PeriodicPass.CHECKS_PER_VISIT, pass.visit(p, polled, g), "then it starts over");
    }

    @Test
    void earnedAreSkippedBeforeCountingAndDueAreAwarded() {
        PeriodicPass pass = new PeriodicPass();
        UUID p = UUID.randomUUID();
        List<TrophyDefinition> polled = defs(10);
        Game g = new Game();
        g.earned.addAll(polled.subList(0, 4));
        g.due.add(polled.get(7));
        assertEquals(6, pass.visit(p, polled, g));
        assertEquals(1, g.awards);
        assertEquals(5, pass.visit(p, polled, g), "the awarded one is now earned and skipped");
        assertEquals(1, g.awards, "nothing is awarded twice");
    }

    @Test
    void eachPlayerIsDueOncePerInterval() {
        PeriodicPass pass = new PeriodicPass();
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            players.add(UUID.randomUUID());
        }
        int interval = 100;
        int[] visits = new int[players.size()];
        Set<Long> ticksUsed = new HashSet<>();
        for (long tick = 1; tick <= interval * 3L; tick++) {
            for (int i = 0; i < players.size(); i++) {
                if (pass.due(players.get(i), tick, interval)) {
                    visits[i]++;
                    ticksUsed.add(tick % interval);
                }
            }
        }
        for (int v : visits) {
            assertEquals(3, v);
        }
        assertEquals(players.size(), ticksUsed.size(), "players are staggered onto different ticks");
        pass.forget(players.get(0));
        assertEquals(players.size() - 1, pass.tracked());
    }
}
