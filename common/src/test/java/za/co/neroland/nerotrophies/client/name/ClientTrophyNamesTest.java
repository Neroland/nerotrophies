package za.co.neroland.nerotrophies.client.name;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.network.NamesDropPayload;
import za.co.neroland.nerotrophies.network.NamesUpdatePayload;

/** The client cache: filled only by updates, dropped entry-wise, cleared on reset and on a new or lost connection. */
class ClientTrophyNamesTest {

    private static final Identifier ONE = TestDefs.id("one");
    private static final Identifier ICON = Identifier.withDefaultNamespace("gold_ingot");

    @AfterEach
    void tidy() {
        ClientTrophyNames.clear();
    }

    private static NamesUpdatePayload named(long token, String name) {
        return new NamesUpdatePayload(List.of(new NamesUpdatePayload.Entry(token, ONE, NamesUpdatePayload.NAMED, name)),
                List.of(new NamesUpdatePayload.View(ONE, (byte) TrophyTier.GOLD.ordinal(), ICON)));
    }

    @Test
    void updateDropAndClear() {
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        ClientTrophyNames.addListener(listener);
        try {
            ClientTrophyNames.apply(named(101L, "Alice"));
            assertEquals("Alice", ClientTrophyNames.nameFor(101L));
            TrophyView view = ClientTrophyNames.viewFor(101L);
            assertEquals(TrophyTier.GOLD, view.tier());
            assertEquals(ICON, view.icon());
            assertTrue(view.named());
            assertNull(ClientTrophyNames.nameFor(202L), "unknown token: nothing");

            ClientTrophyNames.apply(new NamesDropPayload(new long[] {101L}, List.of()));
            assertNull(ClientTrophyNames.nameFor(101L));
            assertEquals(TrophyTier.GOLD, ClientTrophyNames.viewForTrophy(ONE).tier());

            ClientTrophyNames.apply(named(101L, "Alice"));
            ClientTrophyNames.clear();
            assertNull(ClientTrophyNames.viewFor(101L));
            assertNull(ClientTrophyNames.viewForTrophy(ONE));
            assertTrue(changes.get() >= 4);
        } finally {
            ClientTrophyNames.removeListener(listener);
        }
    }

    @Test
    void blankAndReplicaCarryNoName() {
        ClientTrophyNames.apply(new NamesUpdatePayload(List.of(
                new NamesUpdatePayload.Entry(1L, ONE, NamesUpdatePayload.BLANK, null),
                new NamesUpdatePayload.Entry(2L, ONE, NamesUpdatePayload.REPLICA, null)), List.of()));
        assertNull(ClientTrophyNames.nameFor(1L));
        assertEquals(NameState.BLANK, ClientTrophyNames.viewFor(1L).state());
        assertTrue(ClientTrophyNames.viewFor(2L).replica());
        assertNull(ClientTrophyNames.viewFor(0L), "reserved tokens are client-local");
    }

    @Test
    void aLostOrNewConnectionClearsTheCache() {
        Object first = new Object();
        AtomicReference<Object> live = new AtomicReference<>(first);
        ClientTrophyNames.bindSession(first, live::get);
        ClientTrophyNames.apply(named(101L, "Alice"));
        assertEquals("Alice", ClientTrophyNames.nameFor(101L));
        live.set(null); // disconnected
        assertNull(ClientTrophyNames.nameFor(101L));
        assertEquals(0, ClientTrophyNames.size());
        live.set(new Object());
        ClientTrophyNames.bindSession(live.get(), live::get);
    }
}
