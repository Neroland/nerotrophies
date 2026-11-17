package za.co.neroland.nerotrophies.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;

/** Name sync on a plain JVM: who receives which names, and how they are withdrawn. */
class NameSyncCoreTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID V = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final Identifier ONE = TestDefs.id("one");
    private static final Identifier TWO = TestDefs.id("two");

    private final TrophyState state = new TrophyState();
    private final TrophyDefinitions.Snapshot defs = TestDefs.snapshot(
            List.of(TestDefs.trophy("one", TrophyTier.GOLD), TestDefs.trophy("two", TrophyTier.BRONZE)), List.of(), 1L);
    private final List<Map.Entry<UUID, CustomPacketPayload>> sent = new ArrayList<>();
    private final NameSyncCore core = new NameSyncCore();

    NameSyncCoreTest() {
        state.award(A, ONE, 1L, 101L);
        state.refreshName(A, "Alice", 2L);
        state.award(B, ONE, 3L, 201L);
        state.refreshName(B, "Bob", 4L);
    }

    private NameSyncCore.Resolver resolver() {
        return new NameSyncCore.Resolver() {
            @Override
            public TokenResolver.Resolution resolve(Identifier trophy, long token) {
                return TokenResolver.resolve(state, trophy, token);
            }

            @Override
            public NamesUpdatePayload.View view(Identifier trophy) {
                var d = defs.trophies().get(trophy);
                return d == null ? null : new NamesUpdatePayload.View(trophy, (byte) d.tier().ordinal(), d.icon());
            }
        };
    }

    private void pass(UUID viewer, TrophyStamp... visible) {
        core.pass(viewer, List.of(visible), resolver(), (who, p) -> sent.add(Map.entry(who, p)));
    }

    private List<NamesUpdatePayload.Entry> namesTo(UUID viewer) {
        List<NamesUpdatePayload.Entry> out = new ArrayList<>();
        for (var e : sent) {
            if (e.getKey().equals(viewer) && e.getValue() instanceof NamesUpdatePayload u) {
                out.addAll(u.names());
            }
        }
        return out;
    }

    private boolean anyNameTo(UUID viewer, String name) {
        return namesTo(viewer).stream().anyMatch(e -> name.equals(e.name()));
    }

    @Test
    void namesOnlyForVisibleTokensAndWithdrawnWhenOutOfView() {
        pass(V, new TrophyStamp(ONE, 101L));
        assertTrue(anyNameTo(V, "Alice"));
        assertFalse(anyNameTo(V, "Bob"));
        assertTrue(core.holds(V, 101L));
        assertFalse(core.holds(V, 201L));

        sent.clear();
        pass(V);
        assertFalse(core.holds(V, 101L));
        NamesDropPayload drop = (NamesDropPayload) sent.get(0).getValue();
        assertEquals(101L, drop.tokens()[0]);
        assertEquals(List.of(ONE), drop.trophies());
    }

    @Test
    void tierAndIconAreSentForAVisibleTrophyEvenUnstamped() {
        pass(V, new TrophyStamp(TWO, TrophyStamp.UNSTAMPED), new TrophyStamp(ONE, TrophyStamp.GALLERY_TOKEN));
        assertTrue(namesTo(V).isEmpty(), "reserved tokens are never sent by name");
        NamesUpdatePayload u = (NamesUpdatePayload) sent.get(0).getValue();
        assertEquals(2, u.views().size());
    }

    @Test
    void erasureDropsNamesInTheSameCall() {
        pass(V, new TrophyStamp(ONE, 101L));
        sent.clear();
        long[] tokens = state.erase(A);
        core.invalidate(tokens, resolver(), (who, p) -> sent.add(Map.entry(who, p)));
        List<NamesUpdatePayload.Entry> names = namesTo(V);
        assertEquals(1, names.size());
        assertEquals(NamesUpdatePayload.BLANK, names.get(0).status());
        assertNull(names.get(0).name());
    }

    @Test
    void shownameOffBlanksAndOnlyHoldersAreTold() {
        pass(V, new TrophyStamp(ONE, 101L));
        pass(B, new TrophyStamp(ONE, 201L));
        sent.clear();
        state.setShowName(A, false);
        core.invalidate(state.tokensOf(A), resolver(), (who, p) -> sent.add(Map.entry(who, p)));
        assertEquals(NamesUpdatePayload.BLANK, namesTo(V).get(0).status());
        assertTrue(namesTo(B).isEmpty(), "B never held A's token");
    }

    @Test
    void twoPlayerHarvestAttemptGetsNothing() {
        // A holds their own trophy; B sees only their own inventory. B learns nothing of A, whatever B knows.
        pass(A, new TrophyStamp(ONE, 101L));
        pass(B, new TrophyStamp(ONE, 201L));
        assertFalse(anyNameTo(B, "Alice"));
        assertFalse(core.holds(B, 101L));
        assertTrue(namesTo(B).stream().allMatch(e -> e.token() == 201L));
    }

    @Test
    void forgedTokenResolvesToNothing() {
        pass(V, new TrophyStamp(ONE, 0x1234_5678_9ABCL), new TrophyStamp(TWO, 101L));
        List<NamesUpdatePayload.Entry> names = namesTo(V);
        assertEquals(2, names.size());
        for (NamesUpdatePayload.Entry e : names) {
            assertNull(e.name());
        }
        assertEquals(NamesUpdatePayload.BLANK, names.get(0).status());
        assertEquals(NamesUpdatePayload.REPLICA, names.get(1).status(), "a real token on another trophy id");
    }

    @Test
    void oneTokenOnTwoTrophyIdsIsNeverNamed() {
        pass(V, new TrophyStamp(ONE, 101L), new TrophyStamp(TWO, 101L));
        assertFalse(anyNameTo(V, "Alice"));
    }

    @Test
    void upsertsAreCappedPerPassAndTheRestFollow() {
        List<TrophyStamp> many = new ArrayList<>();
        for (long t = 1000; t < 1300; t++) {
            many.add(new TrophyStamp(ONE, t));
        }
        core.pass(V, many, resolver(), (who, p) -> sent.add(Map.entry(who, p)));
        assertEquals(NameSyncCore.MAX_UPSERTS_PER_PASS, core.heldCount(V));
        core.pass(V, many, resolver(), (who, p) -> sent.add(Map.entry(who, p)));
        assertEquals(300, core.heldCount(V));
    }

    @Test
    void resetAndRemoveForgetTheViewer() {
        pass(V, new TrophyStamp(ONE, 101L));
        core.reset(V);
        assertFalse(core.holds(V, 101L));
        pass(V, new TrophyStamp(ONE, 101L));
        core.remove(V);
        assertEquals(0, core.heldCount(V));
    }
}
