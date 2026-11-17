package za.co.neroland.nerotrophies.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TrophyTokens;

/**
 * Decision 003 "names never written down": everything a display block entity saves to the chunk ({@link DisplaySave})
 * and everything it sends in its update tag ({@link SlotView}s, the pool bitmask) holds no UUID (string, hex or
 * int-array form) and no gamertag, even when the earner's record holds both.
 */
class DisplayDataNoIdentityTest {

    private static final UUID A = UUID.fromString("12345678-9abc-def0-1234-56789abcdef0");
    private static final String NAME = "AliceTheBold";

    @Test
    void savedAndSyncedDisplayDataCarryNoIdentity() {
        TrophyState state = new TrophyState();
        long token = TrophyTokens.mint(state);
        state.award(A, TestDefs.id("one"), 1L, token);
        state.refreshName(A, NAME, 2L);
        TrophyStamp stamp = new TrophyStamp(TestDefs.id("one"), token);

        DisplaySave save = new DisplaySave(List.of(new DisplaySave.SlotStamp(0, stamp),
                new DisplaySave.SlotStamp(3, new TrophyStamp(TestDefs.id("two"), TrophyStamp.UNSTAMPED))),
                Optional.of(Identifier.fromNamespaceAndPath("neroland", "accent/purple")),
                Optional.of(TestDefs.id("astronaut")), "orbital_plasma", false);
        Tag nbt = DisplaySave.CODEC.encodeStart(NbtOps.INSTANCE, save).getOrThrow();
        String json = DisplaySave.CODEC.encodeStart(JsonOps.INSTANCE, save).getOrThrow().toString();
        assertNoIdentity(nbt.toString());
        assertNoIdentity(json);
        assertEquals(save, DisplaySave.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());

        List<SlotView> views = List.of(new SlotView(0, Optional.of(TrophyTier.GOLD),
                Optional.of(Identifier.withDefaultNamespace("gold_ingot")), false));
        assertNoIdentity(SlotView.CODEC.listOf().encodeStart(NbtOps.INSTANCE, views).getOrThrow().toString());
        assertNoIdentity(SlotView.CODEC.listOf().encodeStart(JsonOps.INSTANCE, views).getOrThrow().toString());
    }

    private static void assertNoIdentity(String text) {
        assertFalse(text.contains(A.toString()), "UUID string");
        assertFalse(text.contains(A.toString().replace("-", "")), "UUID hex");
        assertFalse(text.contains(NAME), "gamertag");
        assertFalse(text.contains(Arrays.toString(UUIDUtil.uuidToIntArray(A))), "UUID int array");
        for (int part : UUIDUtil.uuidToIntArray(A)) {
            assertFalse(text.contains(Integer.toString(part)), "UUID int-array part");
        }
    }
}
