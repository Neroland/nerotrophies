package za.co.neroland.nerotrophies.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import com.mojang.serialization.JsonOps;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TrophyTokens;

/**
 * The trophy component carries a trophy id and a token, nothing else: no UUID (string or int-array form) and no
 * name, in its saved, JSON and network forms. Only the store holds identity.
 */
class NoIdentityLeakTest {

    private static final UUID A = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");
    private static final String NAME = "AliceTheBold";

    @Test
    void theStampHoldsNoIdentityInAnyForm() {
        TrophyState s = new TrophyState();
        long token = TrophyTokens.mint(s);
        s.award(A, TestDefs.id("one"), 1L, token);
        s.refreshName(A, NAME, 2L);
        TrophyStamp stamp = new TrophyStamp(TestDefs.id("one"), token);

        String json = TrophyStamp.CODEC.encodeStart(JsonOps.INSTANCE, stamp).getOrThrow().toString();
        assertNoIdentity(json);
        assertEquals(2, TrophyStamp.CODEC.encodeStart(JsonOps.INSTANCE, stamp).getOrThrow().getAsJsonObject().size());

        Tag nbt = TrophyStamp.CODEC.encodeStart(NbtOps.INSTANCE, stamp).getOrThrow();
        assertNoIdentity(nbt.toString());
        int[] uuidInts = UUIDUtil.uuidToIntArray(A);
        assertFalse(nbt.toString().contains(Arrays.toString(uuidInts)));
        for (int part : uuidInts) {
            assertFalse(nbt.toString().contains(Integer.toString(part)), "int-array UUID part");
        }

        //? if >=1.20.5 {
        ByteBuf buf = Unpooled.buffer();
        //?} else {
        /*var buf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
        *///?}
        TrophyStamp.STREAM_CODEC.encode(buf, stamp);
        byte[] wire = new byte[buf.readableBytes()];
        buf.getBytes(0, wire);
        String wireText = new String(wire, StandardCharsets.ISO_8859_1);
        assertNoIdentity(wireText);
        assertEquals(stamp, TrophyStamp.STREAM_CODEC.decode(buf));
    }

    @Test
    void theStoreIsTheOnlyPlaceIdentityLives() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("one"), 1L, 77L);
        s.refreshName(A, NAME, 2L);
        String store = TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow().toString();
        assertTrue(store.contains(A.toString()) && store.contains(NAME), "the store itself holds the record");
        s.erase(A);
        String after = TrophyState.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow().toString();
        assertNoIdentity(after);
    }

    private static void assertNoIdentity(String text) {
        assertFalse(text.contains(A.toString()), "UUID string");
        assertFalse(text.contains(A.toString().replace("-", "")), "UUID hex");
        assertFalse(text.contains(NAME), "gamertag");
    }
}
