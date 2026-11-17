package za.co.neroland.nerotrophies.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;

/** A codec round trip for every {@code nerotrophies:main} payload, plus the decode bounds. */
class PayloadRoundTripTest {

    private static final Identifier ONE = TestDefs.id("one");
    private static final Identifier ICON = Identifier.withDefaultNamespace("gold_ingot");

    @Test
    void namesUpdate() {
        NamesUpdatePayload p = new NamesUpdatePayload(List.of(
                new NamesUpdatePayload.Entry(101L, ONE, NamesUpdatePayload.NAMED, "Alice"),
                new NamesUpdatePayload.Entry(-77L, ONE, NamesUpdatePayload.BLANK, null),
                new NamesUpdatePayload.Entry(Long.MAX_VALUE, ONE, NamesUpdatePayload.REPLICA, null)),
                List.of(new NamesUpdatePayload.View(ONE, (byte) 2, ICON), new NamesUpdatePayload.View(ONE, (byte) -1, null)));
        assertEquals(p, PayloadCodecs.roundTrip(NamesUpdatePayload.CODEC, p));
    }

    @Test
    void namesUpdateNeverCarriesANameUnlessNamed() {
        NamesUpdatePayload.Entry e = new NamesUpdatePayload.Entry(5L, ONE, NamesUpdatePayload.BLANK, "Alice");
        assertNull(e.name());
        assertThrows(IllegalArgumentException.class,
                () -> new NamesUpdatePayload.Entry(5L, ONE, NamesUpdatePayload.NAMED, "x".repeat(33)));
    }

    @Test
    void namesDropAndReset() {
        NamesDropPayload d = new NamesDropPayload(new long[] {1L, -9L, 42L}, List.of(ONE));
        assertEquals(d, PayloadCodecs.roundTrip(NamesDropPayload.CODEC, d));
        assertEquals(NamesResetPayload.INSTANCE, PayloadCodecs.roundTrip(NamesResetPayload.CODEC, NamesResetPayload.INSTANCE));
    }

    @Test
    void record() {
        RecordPayload.Entry earned = new RecordPayload.Entry(ONE, "nerospace", true, true, false, false, TrophyTier.GOLD,
                ICON, "trophy.a.title", "trophy.a.desc", 123L, 456L, 3, 0, 0);
        RecordPayload.Entry progress = new RecordPayload.Entry(TestDefs.id("two"), "minecraft", false, false, false,
                false, TrophyTier.BRONZE, ICON, "t", "d", 0L, 0L, -1, 5, 10);
        RecordPayload.Entry masked = RecordPayload.Entry.masked(TestDefs.id("secret"), "nerotrophies", 7);
        RecordPayload p = new RecordPayload(List.of(earned, progress, masked),
                List.of(new RecordPayload.CollectionView(TestDefs.id("set"), "c.t", "c.d", ICON, "gilded",
                        List.of(ONE, TestDefs.id("two")), true)),
                List.of("gilded"), false, true, 999L);
        assertEquals(p, PayloadCodecs.roundTrip(RecordPayload.CODEC, p));
        RecordDeltaPayload delta = new RecordDeltaPayload(earned, p.collections(), p.trims(), 1000L);
        assertEquals(delta, PayloadCodecs.roundTrip(RecordDeltaPayload.CODEC, delta));
    }

    @Test
    void awardToastKinds() {
        for (AwardToastPayload p : List.of(AwardToastPayload.trophy(ONE, TrophyTier.NERO),
                AwardToastPayload.collection(TestDefs.id("set")), AwardToastPayload.summary(12))) {
            assertEquals(p, PayloadCodecs.roundTrip(AwardToastPayload.CODEC, p));
        }
        assertThrows(IllegalArgumentException.class, () -> new AwardToastPayload((byte) 3, ONE, null, 0));
    }

    @Test
    void serverbound() {
        assertEquals(new ClaimPayload(ONE), PayloadCodecs.roundTrip(ClaimPayload.CODEC, new ClaimPayload(ONE)));
        SetTogglePayload t = new SetTogglePayload(SetTogglePayload.SHOW_NAME, false);
        assertEquals(t, PayloadCodecs.roundTrip(SetTogglePayload.CODEC, t));
        assertEquals(BookRefreshPayload.INSTANCE,
                PayloadCodecs.roundTrip(BookRefreshPayload.CODEC, BookRefreshPayload.INSTANCE));
        for (SelectCollectionPayload s : List.of(new SelectCollectionPayload(Optional.of(ONE)),
                new SelectCollectionPayload(Optional.empty()))) {
            assertEquals(s, PayloadCodecs.roundTrip(SelectCollectionPayload.CODEC, s));
        }
    }

    @Test
    void oversizedListIsRejectedOnDecode() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeVarInt(NamesDropPayload.MAX + 1);
        assertThrows(RuntimeException.class, () -> NamesDropPayload.CODEC.decode(buf));
    }

    @Test
    void noServerboundPayloadCarriesATokenOrName() {
        for (Class<?> c : List.of(ClaimPayload.class, SetTogglePayload.class, BookRefreshPayload.class,
                SelectCollectionPayload.class)) {
            for (var rc : c.getRecordComponents()) {
                Class<?> t = rc.getType();
                assertEquals(false, t == long.class || t == Long.class || t == long[].class || t == String.class,
                        c.getSimpleName() + "." + rc.getName());
            }
        }
    }
}
