package za.co.neroland.nerotrophies.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

import za.co.neroland.nerotrophies.TestDefs;

/** Definitions are indexed by what fires them; award-only definitions are not indexed at all. */
class TriggerIndexTest {

    @Test
    void indexesEveryTriggerType() {
        Identifier adv = Identifier.withDefaultNamespace("story/mine_diamond");
        Identifier mars = Identifier.fromNamespaceAndPath("nerospace", "mars");
        TrophyDefinition a = TestDefs.trophy("a", TrophyTier.BRONZE, Optional.of(new TrophyTrigger.Advancement(adv)),
                Optional.empty());
        TrophyDefinition d = TestDefs.trophy("d", TrophyTier.BRONZE,
                Optional.of(new TrophyTrigger.DimensionVisited(mars)), Optional.empty());
        TrophyDefinition s = TestDefs.trophy("s", TrophyTier.BRONZE, Optional.of(new TrophyTrigger.Stat(
                TrophyTrigger.Stat.DEFAULT_STAT_TYPE, Identifier.withDefaultNamespace("jump"), 10)), Optional.empty());
        TrophyDefinition only = TestDefs.trophy("only", TrophyTier.BRONZE, Optional.empty(), Optional.empty());
        TriggerIndex idx = new TriggerIndex(List.of(s, only, d, a));
        assertEquals(List.of(a), idx.byAdvancement(adv));
        assertEquals(Set.of(adv), idx.advancementIds());
        assertEquals(List.of(d), idx.byDimension(mars));
        assertTrue(idx.hasDimensionTriggers());
        assertEquals(List.of(d, s), idx.polled(), "polled, sorted by id, award-only excluded");
        assertTrue(idx.byGate(Identifier.fromNamespaceAndPath("absentmod", "g")).isEmpty());
        assertTrue(TriggerIndex.EMPTY.polled().isEmpty());
    }
}
