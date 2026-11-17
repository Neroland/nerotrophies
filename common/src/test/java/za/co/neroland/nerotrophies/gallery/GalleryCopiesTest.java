package za.co.neroland.nerotrophies.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;

import za.co.neroland.nerotrophies.blockentity.CabinetPools;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;

/**
 * Gallery trophies are tagged display copies: reserved tokens only, so they are never indexed,
 * awarded, counted by an ordinary cabinet, resolved to a name, or written to the store the link module reads; and
 * building a hall changes nothing in that store.
 */
class GalleryCopiesTest {

    private static final UUID EARNER = UUID.fromString("0a11e000-0000-4000-8000-00000000ca11");

    @Test
    void everyTrophyInTheHallCarriesAReservedToken() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.catalogue_0_1_0());
        Set<Long> tokens = new HashSet<>();
        List<TrophyStamp> stamps = plan.stamps();
        assertFalse(stamps.isEmpty());
        stamps.forEach(s -> tokens.add(s.token()));
        assertEquals(Set.of(TrophyStamp.GALLERY_TOKEN, TrophyStamp.GALLERY_BLANK_TOKEN, TrophyStamp.UNSTAMPED), tokens);
    }

    @Test
    void aPieceCannotCarryALiveToken() {
        Identifier id = Identifier.fromNamespaceAndPath("nerotrophies", "minecraft/dragonslayer");
        assertThrowsIae(() -> new GalleryPiece.Trophy(GalleryPiece.Face.AWAY, new TrophyStamp(id, 12345L)));
        assertThrowsIae(() -> new GalleryPiece.Plaque(GalleryPiece.Face.AWAY, new TrophyStamp(id, 99L)));
    }

    private static void assertThrowsIae(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected a refusal");
    }

    @Test
    void galleryCopiesNeverResolveToAPlayerOrCount() {
        GalleryCatalogue catalogue = GalleryFixtures.catalogue_0_1_0();
        GalleryPlan plan = GalleryFixtures.plan(catalogue);
        // A real earner of every trophy the hall shows, with a name and show-name on.
        TrophyState state = new TrophyState();
        long token = 777_000L;
        for (var def : catalogue.all()) {
            state.award(EARNER, def.id(), 1L, token++);
        }
        state.refreshName(EARNER, "RealPlayerName", 2L);
        Function<TrophyStamp, TrophyStatus> resolver = s -> TokenResolver.resolve(state, s.trophy(), s.token()).status();

        for (TrophyStamp stamp : plan.stamps()) {
            TokenResolver.Resolution r = TokenResolver.resolve(state, stamp.trophy(), stamp.token());
            assertTrue(r.name().isEmpty(), "a gallery copy never resolves a name");
            assertFalse(r.counts(), "a gallery copy is never genuine");
            assertTrue(r.status() != TrophyStatus.GENUINE);
            assertFalse(CabinetPools.counts(stamp, resolver, false), "an ordinary cabinet never counts it");
            if (stamp.token() != TrophyStamp.GALLERY_TOKEN) {
                assertFalse(CabinetPools.counts(stamp, resolver, true), "nameless copies and replicas never count");
            }
        }
        TrophyCollection first = catalogue.collections().get(0);
        List<TrophyStamp> copies = first.members().stream()
                .map(m -> new TrophyStamp(m, TrophyStamp.GALLERY_TOKEN)).toList();
        assertEquals(0L, CabinetPools.memberMask(first.members(), copies, resolver, false),
                "a survival cabinet full of gallery copies shows nothing");
    }

    @Test
    void displayCopiesFillTheVisualMaskButNeverTheGenuineOne() {
        TrophyState state = new TrophyState();
        Function<TrophyStamp, TrophyStatus> resolver = s -> TokenResolver.resolve(state, s.trophy(), s.token()).status();
        TrophyCollection first = GalleryFixtures.catalogue_0_1_0().collections().get(0);
        List<Identifier> members = first.members();
        List<TrophyStamp> all = members.stream().map(m -> new TrophyStamp(m, TrophyStamp.GALLERY_TOKEN)).toList();
        long full = (1L << members.size()) - 1;
        assertEquals(full, CabinetPools.memberMask(members, all, resolver, true), "a gallery cabinet shows complete");
        List<TrophyStamp> half = all.subList(0, (members.size() + 1) / 2);
        assertEquals(half.size(), Long.bitCount(CabinetPools.memberMask(members, half, resolver, true)),
                "and partial");
        assertEquals(0L, CabinetPools.memberMask(members, List.of(), resolver, true), "and empty");
        assertEquals(0L, CabinetPools.genuineMask(members, all, resolver), "never genuine");
        List<TrophyStamp> nameless = members.stream()
                .map(m -> new TrophyStamp(m, TrophyStamp.GALLERY_BLANK_TOKEN)).toList();
        assertEquals(0L, CabinetPools.memberMask(members, nameless, resolver, true), "nameless copies never count");
    }

    @Test
    void buildingAndClearingTheHallLeavesTheTrophyStoreUntouched() {
        TrophyState state = new TrophyState();
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.catalogue_0_1_0());
        GalleryFrame frame = GalleryFrame.inFrontOf(BlockPos.ZERO, Direction.NORTH, plan.length());
        FakeGalleryWorld world = new FakeGalleryWorld();
        GalleryJob build = GalleryJob.build(Identifier.withDefaultNamespace("overworld"), frame, plan);
        while (!build.done()) {
            build.step(world, 100_000, Long.MAX_VALUE, () -> 0L);
        }
        GalleryJob clear = GalleryJob.clear(build.snapshot());
        while (!clear.done()) {
            clear.step(world, 100_000, Long.MAX_VALUE, () -> 0L);
        }
        assertEquals(0, state.recordCount());
        for (TrophyStamp stamp : plan.stamps()) {
            assertFalse(state.tokenKnown(stamp.token()), "no gallery token is ever indexed");
        }
    }
}
