package za.co.neroland.nerotrophies.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;

/**
 * A replica (re-claimed, edited or unstamped copy) is never counted by a cabinet and never shows a
 * name; a blank (erased / forged) copy counts for nothing either; each token counts once; gallery copies count only
 * in a display-only cabinet.
 */
class ReplicaNeverCountsTest {

    private static final UUID A = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");
    private static final Identifier ONE = TestDefs.id("one");
    private static final Identifier TWO = TestDefs.id("two");
    private static final List<Identifier> MEMBERS = List.of(ONE, TWO);

    private static Function<TrophyStamp, TrophyStatus> resolver(TrophyState state) {
        return s -> TokenResolver.resolve(state, s.trophy(), s.token()).status();
    }

    @Test
    void aReclaimedCopyIsAReplicaThatCountsForNothingAndShowsNoName() {
        TrophyState state = new TrophyState();
        state.award(A, ONE, 1L, 1111L);
        state.refreshName(A, "AliceTheBold", 2L);
        state.replaceToken(A, ONE, 2222L, 3L);

        TrophyStamp old = new TrophyStamp(ONE, 1111L);
        TrophyStamp fresh = new TrophyStamp(ONE, 2222L);
        assertEquals(TrophyStatus.REPLICA, resolver(state).apply(old));
        assertFalse(CabinetPools.counts(old, resolver(state), false));
        assertFalse(CabinetPools.counts(old, resolver(state), true), "not even in a gallery cabinet");
        assertEquals(0L, CabinetPools.memberMask(MEMBERS, List.of(old), resolver(state), false));
        assertEquals(1L, CabinetPools.memberMask(MEMBERS, List.of(old, fresh), resolver(state), false));

        // Whatever a client was told, a replica plate never carries a name.
        assertEquals(PlateState.REPLICA, PlateState.of(old.token(), true, "AliceTheBold"));
        assertNull(PlateState.shownName(old.token(), true, "AliceTheBold"));
        assertEquals(PlateState.REPLICA, PlateState.of(TrophyStamp.UNSTAMPED, false, "AliceTheBold"));
        assertNull(PlateState.shownName(TrophyStamp.UNSTAMPED, false, "AliceTheBold"));
        assertEquals(PlateState.NAMED, PlateState.of(fresh.token(), false, "AliceTheBold"));
    }

    @Test
    void anEditedStampIsAReplica() {
        TrophyState state = new TrophyState();
        state.award(A, ONE, 1L, 1111L);
        TrophyStamp edited = new TrophyStamp(TWO, 1111L);
        assertEquals(TrophyStatus.REPLICA, resolver(state).apply(edited));
        assertEquals(0L, CabinetPools.memberMask(MEMBERS, List.of(edited), resolver(state), false));
    }

    @Test
    void unknownAndErasedTokensAreNamelessAndUncounted() {
        TrophyState state = new TrophyState();
        state.award(A, ONE, 1L, 1111L);
        state.refreshName(A, "AliceTheBold", 2L);
        TrophyStamp forged = new TrophyStamp(TWO, 987654321L);
        assertEquals(TrophyStatus.BLANK, resolver(state).apply(forged));
        assertFalse(CabinetPools.counts(forged, resolver(state), false));

        TrophyStamp genuine = new TrophyStamp(ONE, 1111L);
        assertTrue(CabinetPools.counts(genuine, resolver(state), false));
        state.erase(A);
        assertEquals(TrophyStatus.BLANK, resolver(state).apply(genuine));
        assertFalse(CabinetPools.counts(genuine, resolver(state), false));
        assertEquals(PlateState.BLANK, PlateState.of(genuine.token(), false, null));
    }

    @Test
    void eachTokenCountsOnceAndGalleryCopiesOnlyInADisplayOnlyCabinet() {
        TrophyState state = new TrophyState();
        state.award(A, ONE, 1L, 1111L);
        TrophyStamp genuine = new TrophyStamp(ONE, 1111L);
        assertEquals(1, Long.bitCount(CabinetPools.memberMask(MEMBERS, List.of(genuine, genuine), resolver(state),
                false)));

        TrophyStamp gallery = new TrophyStamp(TWO, TrophyStamp.GALLERY_TOKEN);
        TrophyStamp galleryBlank = new TrophyStamp(TWO, TrophyStamp.GALLERY_BLANK_TOKEN);
        assertEquals(0L, CabinetPools.memberMask(MEMBERS, List.of(gallery), resolver(state), false));
        assertEquals(2L, CabinetPools.memberMask(MEMBERS, List.of(gallery), resolver(state), true));
        assertEquals(0L, CabinetPools.memberMask(MEMBERS, List.of(galleryBlank), resolver(state), true));
        assertEquals(PlateState.GALLERY, PlateState.of(TrophyStamp.GALLERY_TOKEN, false, null));
        assertEquals(PlateState.BLANK, PlateState.of(TrophyStamp.GALLERY_BLANK_TOKEN, false, "x"));
    }
}
