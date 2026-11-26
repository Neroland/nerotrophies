package za.co.neroland.nerotrophies.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/** The {@code record} payload holds the requester's record only, masks hidden unearned trophies, lists every collection. */
class RecordBuilderTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static TrophyDefinition hidden(String path) {
        return TrophyDefinition.of(TestDefs.id(path), new TrophyDefinition.Data(
                Identifier.withDefaultNamespace("diamond"), TrophyTier.NERO, Optional.empty(), true, Optional.empty(),
                Optional.empty(), List.of(), Optional.empty(), Optional.empty(), 0, 1));
    }

    private final TrophyDefinitions.Snapshot defs = TestDefs.snapshot(
            List.of(TestDefs.trophy("one", TrophyTier.GOLD), TestDefs.trophy("two", TrophyTier.BRONZE), hidden("secret")),
            List.of(TestDefs.collection("zeta", "standard", TestDefs.id("one")),
                    TestDefs.collection("alpha", "gilded", TestDefs.id("one"), TestDefs.id("two"))), 1L);

    @Test
    void aClientNeverReceivesAnotherPlayersRecord() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 10L, 101L);
        s.award(A, TestDefs.id("secret"), 11L, 102L);
        s.refreshName(A, "Alice", 12L);
        s.completeCollection(A, TestDefs.id("zeta"));
        s.award(B, TestDefs.id("two"), 20L, 201L);
        s.refreshName(B, "Bob", 21L);

        RecordPayload forB = RecordBuilder.build(s, B, defs, RecordBuilder.ProgressSource.NONE, 99L, 600_000L);
        assertTrue(forB.entry(TestDefs.id("two")).earned());
        assertFalse(forB.entry(TestDefs.id("one")).earned(), "A's trophy is not B's");
        assertTrue(forB.entry(TestDefs.id("secret")).masked(), "A earned it; B still sees ???");
        assertTrue(forB.collections().stream().noneMatch(RecordPayload.CollectionView::completed));
        String text = forB.toString();
        assertFalse(text.contains("Alice") || text.contains("Bob") || text.contains(A.toString())
                || text.contains("101") || text.contains("201"), "no names, UUIDs or tokens");
    }

    @Test
    void hiddenUnearnedIsMaskedWithoutProgressAndEarnedShowsAll() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("secret"), 5L, 102L);
        RecordPayload forA = RecordBuilder.build(s, A, defs,
                d -> Optional.of(new TriggerEvaluator.Progress(3, 10)), 99L, 0L);
        RecordPayload.Entry secret = forA.entry(TestDefs.id("secret"));
        assertFalse(secret.masked());
        assertEquals(TrophyTier.NERO, secret.tier());
        RecordPayload.Entry two = forA.entry(TestDefs.id("two"));
        assertEquals(3, two.progress());
        assertEquals(10, two.target());

        RecordPayload forB = RecordBuilder.build(s, B, defs,
                d -> Optional.of(new TriggerEvaluator.Progress(3, 10)), 99L, 0L);
        RecordPayload.Entry masked = forB.entry(TestDefs.id("secret"));
        assertTrue(masked.masked());
        assertNull(masked.tier());
        assertNull(masked.title());
        assertEquals(0, masked.target());
    }

    @Test
    void everyCollectionIsListedSortedById() {
        RecordPayload p = RecordBuilder.build(new TrophyState(), A, defs, RecordBuilder.ProgressSource.NONE, 0L, 0L);
        assertEquals(List.of(TestDefs.id("alpha"), TestDefs.id("zeta")),
                p.collections().stream().map(RecordPayload.CollectionView::id).toList());
        assertTrue(p.announce() && p.showName());
    }

    @Test
    void reclaimTimeAndTrims() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1L, 101L);
        s.markClaimed(A, TestDefs.id("one"), 1_000L);
        s.award(A, TrophyIds.trimId("gilded"), 2L, 0L);
        RecordPayload p = RecordBuilder.build(s, A, defs, RecordBuilder.ProgressSource.NONE, 5_000L, 600_000L);
        assertEquals(601_000L, p.entry(TestDefs.id("one")).canReclaimAt());
        assertEquals(List.of("gilded"), p.trims());
    }
    @Test
    void aPackAtCoresLimitsStaysUnderTheClientboundPayloadLimit() {
        java.util.List<TrophyDefinition> trophies = new java.util.ArrayList<>();
        Identifier[] members = new Identifier[256];
        for (int i = 0; i < 4096; i++) {
            TrophyDefinition d = TestDefs.trophy(String.format(java.util.Locale.ROOT, "a_rather_long_pack/trophy_%04d", i),
                    TrophyTier.BRONZE);
            trophies.add(d);
            if (i < members.length) {
                members[i] = d.id();
            }
        }
        java.util.List<za.co.neroland.nerolandcore.trophy.TrophyCollection> collections = new java.util.ArrayList<>();
        for (int i = 0; i < 512; i++) {
            collections.add(TestDefs.collection("set_" + i, "standard", members));
        }
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(trophies, collections, 1L);
        TrophyState s = new TrophyState();
        s.award(A, trophies.get(0).id(), 1L, 77L);

        RecordPayload p = RecordBuilder.build(s, A, defs, RecordBuilder.ProgressSource.NONE, 5_000L, 600_000L);
        io.netty.buffer.ByteBuf raw = io.netty.buffer.Unpooled.buffer();
        //? if >=1.20.5 {
        RecordPayload.CODEC.encode(new net.minecraft.network.RegistryFriendlyByteBuf(raw,
                net.minecraft.core.RegistryAccess.EMPTY), p);
        //?} else {
        /*RecordPayload.CODEC.encode(new net.minecraft.network.FriendlyByteBuf(raw), p);
        *///?}
        assertTrue(raw.writerIndex() <= RecordBuilder.RECORD_BUDGET_BYTES, raw.writerIndex() + " bytes");
        assertTrue(raw.writerIndex() < 1_048_576, "under the 1 MiB clientbound custom payload limit");
        assertTrue(p.collections().size() < 512, "collections are truncated by bytes");
        assertFalse(p.trophies().isEmpty());
        assertEquals(p.trophies().size(), PayloadCodecs.roundTrip(RecordPayload.CODEC, p).trophies().size());
        long delta = 0;
        for (RecordPayload.CollectionView c : RecordBuilder.collections(null, defs)) {
            delta += RecordBuilder.size(c);
        }
        assertTrue(delta <= RecordBuilder.COLLECTIONS_BUDGET_BYTES, "a record_delta's collection list is bounded too");
    }
}
