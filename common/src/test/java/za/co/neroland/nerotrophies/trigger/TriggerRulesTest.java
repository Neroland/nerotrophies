package za.co.neroland.nerotrophies.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

import za.co.neroland.nerotrophies.engine.TriggerEvaluator;

/** Each trigger type's retroactive read and progress, including ids no loaded mod provides. */
class TriggerRulesTest {

    private static Identifier mc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static Identifier id(String ns, String path) {
        return Identifier.fromNamespaceAndPath(ns, path);
    }

    @Test
    void advancementNeedsCompletion() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger t = new TrophyTrigger.Advancement(mc("story/enter_the_nether"));
        assertFalse(TriggerRules.satisfied(t, f));
        f.advancements.add(mc("story/enter_the_nether"));
        assertTrue(TriggerRules.satisfied(t, f));
        assertFalse(TriggerRules.satisfied(new TrophyTrigger.Advancement(id("absentmod", "x")), f),
                "an absent mod's advancement never matches");
        assertTrue(TriggerRules.progress(t, f).isEmpty(), "advancements carry no progress");
    }

    @Test
    void gateAndMilestoneArePlayerScopedReads() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger gate = TrophyTrigger.NeroEvent.ofGate(id("nerospace", "first_launch"));
        assertFalse(TriggerRules.satisfied(gate, f));
        f.playerGates.add(id("nerospace", "first_launch"));
        assertTrue(TriggerRules.satisfied(gate, f));

        TrophyTrigger any = TrophyTrigger.NeroEvent.ofMaterialMilestone(id("nerolandcore", "material_discovered"),
                Optional.empty());
        TrophyTrigger iron = TrophyTrigger.NeroEvent.ofMaterialMilestone(id("nerolandcore", "material_discovered"),
                Optional.of(mc("iron")));
        assertFalse(TriggerRules.satisfied(any, f));
        f.milestones.put(id("nerolandcore", "material_discovered"), Set.of(mc("copper")));
        assertTrue(TriggerRules.satisfied(any, f));
        assertFalse(TriggerRules.satisfied(iron, f));
        f.milestones.put(id("nerolandcore", "material_discovered"), Set.of(mc("copper"), mc("iron")));
        assertTrue(TriggerRules.satisfied(iron, f));
    }

    @Test
    void reputationAndCurrencyNeedARealProvider() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger rep = TrophyTrigger.NeroEvent.ofReputation(id("nerofactions", "guild"), 100);
        TrophyTrigger cash = TrophyTrigger.NeroEvent.ofCurrency(TrophyTrigger.NeroEvent.DEFAULT_CURRENCY, 500);
        f.reputation.put(id("nerofactions", "guild"), 150L);
        f.balances.put(TrophyTrigger.NeroEvent.DEFAULT_CURRENCY, 900L);
        assertFalse(TriggerRules.satisfied(rep, f), "no provider: never");
        assertFalse(TriggerRules.satisfied(cash, f), "no provider: never");
        f.realReputation = true;
        f.realCurrency = true;
        assertTrue(TriggerRules.satisfied(rep, f));
        assertTrue(TriggerRules.satisfied(cash, f));
        f.balances.put(TrophyTrigger.NeroEvent.DEFAULT_CURRENCY, 499L);
        assertFalse(TriggerRules.satisfied(cash, f));
        TrophyTrigger negative = TrophyTrigger.NeroEvent.ofReputation(id("nerofactions", "pirates"), -50);
        assertTrue(TriggerRules.satisfied(negative, f), "an unseen faction reads 0, which is above -50");
    }

    @Test
    void statReadsTypedVanillaStatistics() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger kills = new TrophyTrigger.Stat(mc("killed"), id("nerocreatures", "glimmerback"), 10);
        assertFalse(TriggerRules.satisfied(kills, f), "unknown entity: never");
        assertTrue(TriggerRules.progress(kills, f).isEmpty(), "unknown entity: no progress bar");
        f.stats.put(mc("killed") + "|" + id("nerocreatures", "glimmerback"), 4L);
        assertFalse(TriggerRules.satisfied(kills, f));
        assertEquals(new TriggerEvaluator.Progress(4, 10), TriggerRules.progress(kills, f).orElseThrow());
        f.stats.put(mc("killed") + "|" + id("nerocreatures", "glimmerback"), 25L);
        assertTrue(TriggerRules.satisfied(kills, f));
        assertEquals(new TriggerEvaluator.Progress(10, 10), TriggerRules.progress(kills, f).orElseThrow(),
                "progress is clamped to the target");
    }

    @Test
    void itemObtainedFromStatsOrInventory() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger t = new TrophyTrigger.ItemObtained(mc("diamond"), 8);
        assertFalse(TriggerRules.satisfied(t, f));
        f.obtained.put(mc("diamond"), 3L);
        f.held.put(mc("diamond"), 5L);
        assertFalse(TriggerRules.satisfied(t, f));
        assertEquals(5, TriggerRules.progress(t, f).orElseThrow().current());
        f.held.put(mc("diamond"), 8L);
        assertTrue(TriggerRules.satisfied(t, f), "held from a chest counts");
        f.held.put(mc("diamond"), 0L);
        f.obtained.put(mc("diamond"), 9L);
        assertTrue(TriggerRules.satisfied(t, f), "picked up + crafted counts");
        assertFalse(TriggerRules.satisfied(new TrophyTrigger.ItemObtained(id("absentmod", "gem"), 1), f));
    }

    @Test
    void blockPlacedReadsUsedStat() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger t = new TrophyTrigger.BlockPlaced(id("nerodecor", "hull_panel"), 64);
        assertFalse(TriggerRules.satisfied(t, f));
        f.placed.put(id("nerodecor", "hull_panel"), 63L);
        assertFalse(TriggerRules.satisfied(t, f));
        assertEquals(new TriggerEvaluator.Progress(63, 64), TriggerRules.progress(t, f).orElseThrow());
        f.placed.put(id("nerodecor", "hull_panel"), 64L);
        assertTrue(TriggerRules.satisfied(t, f));
    }

    @Test
    void dimensionIsTheCurrentOne() {
        FakeFacts f = new FakeFacts();
        TrophyTrigger t = new TrophyTrigger.DimensionVisited(id("nerospace", "mars"));
        assertFalse(TriggerRules.satisfied(t, f));
        f.dimension = id("nerospace", "mars");
        assertTrue(TriggerRules.satisfied(t, f));
        assertTrue(TriggerRules.progress(t, f).isEmpty());
    }

    @Test
    void crossingIsRisingOnly() {
        assertTrue(TriggerRules.crossed(99, 100, 100));
        assertTrue(TriggerRules.crossed(0, 1000, 100));
        assertFalse(TriggerRules.crossed(100, 150, 100), "already past: not a crossing");
        assertFalse(TriggerRules.crossed(150, 50, 100), "falling: not a crossing");
        assertFalse(TriggerRules.crossed(10, 99, 100));
        assertTrue(TriggerRules.crossed(-100, -50, -50));
    }
}
