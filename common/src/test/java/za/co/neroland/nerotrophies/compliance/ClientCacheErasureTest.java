package za.co.neroland.nerotrophies.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.client.name.ClientTrophyNames;
import za.co.neroland.nerotrophies.client.name.NameState;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyEraser;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyTokens;
import za.co.neroland.nerotrophies.network.NameSyncCore;
import za.co.neroland.nerotrophies.network.NamesDropPayload;
import za.co.neroland.nerotrophies.network.NamesUpdatePayload;

/**
 * Stage 6, end to end on a plain JVM: the server's name sync feeds a real client cache. Erasure, {@code showname off}
 * and a rename change what the client holds in the same call, and the backup is refreshed after the record is gone.
 * ({@code RecordErased}, which Core fires only with a real provider bound, is checked by the server game test.)
 */
class ClientCacheErasureTest {

    private static final UUID EARNER = UUID.fromString("0c0ffee0-0000-4000-8000-0000000000a1");
    private static final UUID VIEWER = UUID.fromString("0c0ffee0-0000-4000-8000-0000000000b2");
    private static final String NAME = "CacheAlice";
    private static final Identifier ONE = TestDefs.id("one");

    private final TrophyState state = new TrophyState();
    private final TrophyDefinitions.Snapshot defs = TestDefs.snapshot(List.of(TestDefs.trophy("one", TrophyTier.GOLD)),
            List.of(), 1L);
    private final NameSyncCore core = new NameSyncCore();
    private final long token;

    ClientCacheErasureTest() {
        token = TrophyTokens.mint(state);
        state.award(EARNER, ONE, 1L, token);
        state.refreshName(EARNER, NAME, 2L);
    }

    @AfterEach
    void tidy() {
        ClientTrophyNames.clear();
    }

    private NameSyncCore.Resolver resolver() {
        return new NameSyncCore.Resolver() {
            @Override
            public TokenResolver.Resolution resolve(Identifier trophy, long t) {
                return TokenResolver.resolve(state, trophy, t);
            }

            @Override
            public NamesUpdatePayload.View view(Identifier trophy) {
                var d = defs.trophies().get(trophy);
                return d == null ? null : new NamesUpdatePayload.View(trophy, (byte) d.tier().ordinal(), d.icon());
            }
        };
    }

    /** Delivers what the server sends to the viewer straight into the client cache, as the network handler does. */
    private NameSyncCore.Sink client() {
        return (UUID who, CustomPacketPayload p) -> {
            if (!who.equals(VIEWER)) {
                return;
            }
            if (p instanceof NamesUpdatePayload u) {
                ClientTrophyNames.apply(u);
            } else if (p instanceof NamesDropPayload d) {
                ClientTrophyNames.apply(d);
            }
        };
    }

    private void viewerSeesTheTrophy() {
        core.pass(VIEWER, List.of(new TrophyStamp(ONE, token)), resolver(), client());
        assertEquals(NAME, ClientTrophyNames.nameFor(token), "precondition: the client holds the name");
    }

    @Test
    void erasureDropsTheNameFromALoadedClientInTheSameCall() {
        viewerSeesTheTrophy();
        List<Boolean> backupSawRecord = new ArrayList<>();
        TrophyEraser eraser = new TrophyEraser(new TrophyEraser.Store() {
            @Override
            public TrophyState state(net.minecraft.server.MinecraftServer server) {
                return state;
            }

            @Override
            public void backup(net.minecraft.server.MinecraftServer server, TrophyState s) {
                backupSawRecord.add(s.hasRecord(EARNER) || s.anyTokenOwnedBy(EARNER));
            }
        });
        long[] tokens = state.tokensOf(EARNER);
        eraser.erase(null, EARNER);
        // What the live NameSync does with the erased tokens (installed on a server; null here).
        core.invalidate(tokens, resolver(), client());

        assertEquals(List.of(false), backupSawRecord, "backupNow runs once, after the record and its tokens are gone");
        // RecordErased only fires with a real provider bound; the game test checks it on a live server.
        assertNull(ClientTrophyNames.nameFor(token), "the client no longer holds the name");
        var view = ClientTrophyNames.viewFor(token);
        assertTrue(view == null || view.state() == NameState.BLANK, "blank, never replica");
        assertFalse(state.hasRecord(EARNER));
    }

    @Test
    void shownameOffBlanksWithoutErasingAndOnBringsItBack() {
        viewerSeesTheTrophy();
        state.setShowName(EARNER, false);
        core.invalidate(state.tokensOf(EARNER), resolver(), client());
        assertNull(ClientTrophyNames.nameFor(token));
        assertTrue(state.hasRecord(EARNER), "the record is kept");
        assertEquals(NAME, state.record(EARNER).orElseThrow().name().orElseThrow(), "the stored name is kept");
        assertTrue(TokenResolver.resolve(state, ONE, token).counts(), "still genuine");

        state.setShowName(EARNER, true);
        core.invalidate(state.tokensOf(EARNER), resolver(), client());
        assertEquals(NAME, ClientTrophyNames.nameFor(token));
    }

    @Test
    void aRenameUpdatesEveryHolderOnTheNextLogin() {
        viewerSeesTheTrophy();
        assertTrue(state.refreshName(EARNER, "CacheAliceRenamed", 3L), "login refresh stores the new gamertag");
        core.invalidate(state.tokensOf(EARNER), resolver(), client()); // NameSync.tokensChanged on login
        assertEquals("CacheAliceRenamed", ClientTrophyNames.nameFor(token));
        assertFalse(state.refreshName(EARNER, "CacheAliceRenamed", 4L), "an unchanged name pushes nothing");
    }
}
