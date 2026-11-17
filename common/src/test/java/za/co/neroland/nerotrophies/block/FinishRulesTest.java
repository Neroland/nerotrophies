package za.co.neroland.nerotrophies.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

/** Dyes set accent finishes, four Core materials set material finishes, nothing else sets one. */
class FinishRulesTest {

    @Test
    void dyesAndCoreMaterialsMapToFinishes() {
        assertEquals(Optional.of(Identifier.fromNamespaceAndPath("neroland", "accent/light_blue")),
                FinishRules.finishFor("minecraft:light_blue_dye"));
        assertEquals(Optional.of(Identifier.fromNamespaceAndPath("neroland", "plasma_glass")),
                FinishRules.finishFor("nerolandcore:plasma_glass"));
        assertEquals(Optional.empty(), FinishRules.finishFor("minecraft:glass"));
        assertEquals(Optional.empty(), FinishRules.finishFor("minecraft:not_a_colour_dye"));
        assertEquals(Optional.empty(), FinishRules.finishFor("othermod:red_dye"));
    }

    @Test
    void unknownTrimsDrawAsStandard() {
        assertEquals("standard", Trims.byId("gone_from_the_pack").id());
        assertEquals(0, Trims.indexOf("gone_from_the_pack"));
        assertEquals(TrimStyle.PRISM, Trims.byId("prism").style());
        assertEquals(12, Trims.ALL.size());
    }
}
