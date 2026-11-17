package za.co.neroland.nerotrophies.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** Events go to the earner only, for live awards only, rate-limited, with bodies naming the trophy and nothing else. */
class TrophyLinkEventsTest {

    private static final java.util.UUID A = TrophyLinkSnapshotsTest.A;
    private static final java.util.UUID B = TrophyLinkSnapshotsTest.B;

    private static final class FakeEnv implements TrophyLinkEvents.Env {
        final List<LinkEvent> out = new ArrayList<>();
        long now = 1_000L;
        long tick = 1L;

        @Override
        public long nowMillis() {
            return now;
        }

        @Override
        public long tick() {
            return tick;
        }

        @Override
        public TrophyDefinitions.Snapshot definitions() {
            return TrophyLinkSnapshotsTest.DEFS;
        }

        @Override
        public LinkText text() {
            return (key, fallback) -> fallback;
        }

        @Override
        public void publish(LinkEvent event) {
            out.add(event);
        }
    }

    private static TrophyEvents.Awarded award(java.util.UUID p, String path, AwardSource source) {
        return new TrophyEvents.Awarded(p, TestDefs.id(path), TrophyTier.GOLD, source);
    }

    @Test
    void liveAwardIsPlayerScopedAndBodyNamesTheTrophyOnly() {
        FakeEnv env = new FakeEnv();
        TrophyLinkEvents events = new TrophyLinkEvents();
        events.onCollectionCompleted(env, new TrophyEvents.CollectionCompleted(A, TestDefs.id("pair"), "laurel"));
        events.onAwarded(env, award(A, "one", AwardSource.LIVE));
        assertEquals(2, env.out.size());
        for (LinkEvent e : env.out) {
            assertEquals(A, e.playerId());
            assertEquals(TrophyLinkModule.MODULE_ID, e.moduleId());
            assertEquals(TrophyLinkEvents.TOPIC, e.topic());
            String payload = e.payload().toString();
            assertFalse(payload.contains(A.toString()) || payload.contains(B.toString()), "no UUIDs");
            assertFalse(payload.contains("Alice"), "no names");
            JsonObject notify = e.payload().getAsJsonObject(TrophyLinkEvents.NOTIFY);
            String link = notify.get("deepLink").getAsString();
            assertTrue(TrophyLinkEvents.DEEP_LINK.matcher(link.replace("{server}", "srv")).matches(), link);
        }
        assertEquals(TrophyLinkEvents.TYPE_TROPHY, env.out.get(0).payload().get("type").getAsString());
        assertEquals("One", env.out.get(0).payload().getAsJsonObject("_notify").get("body").getAsString());
        assertEquals(TrophyLinkEvents.TYPE_COLLECTION, env.out.get(1).payload().get("type").getAsString());
        assertEquals("Pair", env.out.get(1).payload().getAsJsonObject("_notify").get("body").getAsString());
    }

    @Test
    void retroactiveAndCommandAwardsAndTheirCompletionsAreSilent() {
        FakeEnv env = new FakeEnv();
        TrophyLinkEvents events = new TrophyLinkEvents();
        events.onCollectionCompleted(env, new TrophyEvents.CollectionCompleted(A, TestDefs.id("pair"), "laurel"));
        events.onAwarded(env, award(A, "one", AwardSource.RETROACTIVE));
        events.onAwarded(env, award(A, "two", AwardSource.COMMAND));
        // a sweep completion with no award after it, then a live award a tick later: the stale completion is dropped
        events.onCollectionCompleted(env, new TrophyEvents.CollectionCompleted(A, TestDefs.id("pair"), "laurel"));
        env.tick++;
        events.onAwarded(env, award(A, "two", AwardSource.LIVE));
        assertEquals(1, env.out.size());
        assertEquals(TrophyLinkEvents.TYPE_TROPHY, env.out.get(0).payload().get("type").getAsString());
    }

    @Test
    void anotherPlayersAwardDoesNotReleaseMyCompletion() {
        FakeEnv env = new FakeEnv();
        TrophyLinkEvents events = new TrophyLinkEvents();
        events.onCollectionCompleted(env, new TrophyEvents.CollectionCompleted(A, TestDefs.id("pair"), "laurel"));
        events.onAwarded(env, award(B, "one", AwardSource.LIVE));
        assertEquals(1, env.out.size());
        assertEquals(B, env.out.get(0).playerId());
        events.forget(A);
        assertFalse(events.holdsAnyFor(A), "erasure forgets the parked completion");
    }

    @Test
    void rateLimitedPerPlayerAndForgottenOnErasure() {
        FakeEnv env = new FakeEnv();
        TrophyLinkEvents events = new TrophyLinkEvents();
        for (int i = 0; i < 10; i++) {
            events.onAwarded(env, award(A, "one", AwardSource.API));
        }
        assertEquals(TrophyLinkEvents.MAX_PER_WINDOW, env.out.size());
        events.onAwarded(env, award(B, "one", AwardSource.LIVE));
        assertEquals(TrophyLinkEvents.MAX_PER_WINDOW + 1, env.out.size(), "B has its own budget");
        env.now += TrophyLinkEvents.WINDOW_MS;
        events.onAwarded(env, award(A, "one", AwardSource.LIVE));
        assertEquals(TrophyLinkEvents.MAX_PER_WINDOW + 2, env.out.size());
        assertTrue(events.holdsAnyFor(A));
        events.forget(A);
        assertFalse(events.holdsAnyFor(A));
    }

    @Test
    void notifyCopyIsCappedAndCleaned() {
        assertEquals("ab", TrophyLinkEvents.cap("a\u0000§b​ ", 10));
        assertEquals(120, TrophyLinkEvents.cap("x".repeat(500), 120).length());
        assertEquals(null, TrophyLinkEvents.deepLink("bad:key/x"));
    }
}
