package za.co.neroland.nerotrophies.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.Currency;
import za.co.neroland.nerolandcore.economy.CurrencyEvents;
import za.co.neroland.nerolandcore.progression.GateEvents;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneEvents;
import za.co.neroland.nerolandcore.progression.MaterialObservation;
import za.co.neroland.nerolandcore.reputation.ReputationEvents;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.data.TrophyState;

/** Live Core-bus matching: only player-scoped events award, crossings rise, and repeats award nothing new. */
class NeroEventRulesTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final Identifier GATE = Identifier.fromNamespaceAndPath("nerospace", "first_launch");
    private static final Identifier MILESTONE = Identifier.fromNamespaceAndPath("nerolandcore", "material_discovered");
    private static final Identifier IRON = Identifier.withDefaultNamespace("iron");
    private static final Identifier FACTION = Identifier.fromNamespaceAndPath("nerofactions", "guild");

    private static TrophyDefinition def(String path, TrophyTrigger trigger) {
        return TestDefs.trophy(path, TrophyTier.SILVER, Optional.of(trigger), Optional.empty());
    }

    private static final TriggerIndex INDEX = new TriggerIndex(List.of(
            def("gate", TrophyTrigger.NeroEvent.ofGate(GATE)),
            def("any_material", TrophyTrigger.NeroEvent.ofMaterialMilestone(MILESTONE, Optional.empty())),
            def("iron", TrophyTrigger.NeroEvent.ofMaterialMilestone(MILESTONE, Optional.of(IRON))),
            def("rep", TrophyTrigger.NeroEvent.ofReputation(FACTION, 100)),
            def("rich", TrophyTrigger.NeroEvent.ofCurrency(TrophyTrigger.NeroEvent.DEFAULT_CURRENCY, 1000))));

    /** Records every award request. */
    private static final class Log implements NeroEventRules.Awarder {
        final List<String> awards = new ArrayList<>();

        @Override
        public boolean award(UUID player, Identifier trophy) {
            awards.add(player + " " + trophy.getPath());
            return true;
        }
    }

    @Test
    void playerScopeGateOpenAwards() {
        Log log = new Log();
        assertEquals(1, NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.PLAYER, A.toString(), GATE,
                true), log));
        assertEquals(List.of(A + " gate"), log.awards);
    }

    @Test
    void teamServerClosedAndMalformedGatesNeverAward() {
        Log log = new Log();
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.TEAM, "red", GATE, true), log);
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.TEAM, A.toString(), GATE, true), log);
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.SERVER, "", GATE, true), log);
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.PLAYER, A.toString(), GATE, false), log);
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.PLAYER, "not-a-uuid", GATE, true), log);
        NeroEventRules.onGate(INDEX, new GateEvents.GateChange(GateScope.PLAYER, A.toString(),
                Identifier.fromNamespaceAndPath("absentmod", "gate"), true), log);
        assertTrue(log.awards.isEmpty(), log.awards.toString());
    }

    @Test
    void milestoneMatchesMaterialAndScope() {
        Log log = new Log();
        NeroEventRules.onMilestone(INDEX, new MaterialMilestoneEvents.Change(GateScope.PLAYER, A.toString(), MILESTONE,
                Identifier.withDefaultNamespace("copper"), MaterialObservation.PLAYER_PICKUP, true), log);
        assertEquals(List.of(A + " any_material"), log.awards);
        log.awards.clear();
        NeroEventRules.onMilestone(INDEX, new MaterialMilestoneEvents.Change(GateScope.PLAYER, A.toString(), MILESTONE,
                IRON, MaterialObservation.PLAYER_PICKUP, true), log);
        assertEquals(List.of(A + " any_material", A + " iron"), log.awards);
        log.awards.clear();
        NeroEventRules.onMilestone(INDEX, new MaterialMilestoneEvents.Change(GateScope.TEAM, "red", MILESTONE, IRON,
                MaterialObservation.PLAYER_PICKUP, true), log);
        NeroEventRules.onMilestone(INDEX, new MaterialMilestoneEvents.Change(GateScope.SERVER, "", MILESTONE, IRON,
                MaterialObservation.PLAYER_PICKUP, true), log);
        NeroEventRules.onMilestone(INDEX, new MaterialMilestoneEvents.Change(GateScope.PLAYER, A.toString(), MILESTONE,
                IRON, MaterialObservation.ADMIN, false), log);
        assertTrue(log.awards.isEmpty(), "team, server and revocations never award");
    }

    @Test
    void reputationCrossingNeedsRealProvider() {
        Log log = new Log();
        NeroEventRules.onReputation(INDEX, new ReputationEvents.ReputationChange(A, FACTION, 50, 120), false, log);
        assertTrue(log.awards.isEmpty());
        NeroEventRules.onReputation(INDEX, new ReputationEvents.ReputationChange(A, FACTION, 120, 130), true, log);
        assertTrue(log.awards.isEmpty(), "already above: no crossing");
        NeroEventRules.onReputation(INDEX, new ReputationEvents.ReputationChange(A, FACTION, 50, 120), true, log);
        assertEquals(List.of(A + " rep"), log.awards);
    }

    @Test
    void currencyCrossingMatchesTheCurrency() {
        Log log = new Log();
        Currency scrip = Currency.of(Identifier.fromNamespaceAndPath("neroeconomy", "scrip"), "currency.scrip");
        NeroEventRules.onCurrency(INDEX, new CurrencyEvents.BalanceChange(A, scrip, 0, 5000), true, log);
        NeroEventRules.onCurrency(INDEX, new CurrencyEvents.BalanceChange(A, CoreCurrencies.CREDITS, 0, 5000), false,
                log);
        assertTrue(log.awards.isEmpty(), "other currency, or the default in-memory provider: never");
        NeroEventRules.onCurrency(INDEX, new CurrencyEvents.BalanceChange(A, CoreCurrencies.CREDITS, 999, 1000), true,
                log);
        assertEquals(List.of(A + " rich"), log.awards);
    }

    @Test
    void repeatedEventsAreIdempotentThroughTheStore() {
        TrophyState state = new TrophyState();
        long[] token = {100L};
        NeroEventRules.Awarder engine = (p, t) -> state.award(p, t, 1L, token[0]++).added();
        GateEvents.GateChange open = new GateEvents.GateChange(GateScope.PLAYER, A.toString(), GATE, true);
        assertEquals(1, NeroEventRules.onGate(INDEX, open, engine));
        assertEquals(0, NeroEventRules.onGate(INDEX, open, engine), "a second open earns nothing new");
        assertEquals(1, state.record(A).orElseThrow().earnedEntries().size());
    }

    @Test
    void playerTargetParsesUuidsOnly() {
        assertEquals(Optional.of(A), NeroEventRules.playerTarget(A.toString()));
        assertTrue(NeroEventRules.playerTarget("").isEmpty());
        assertTrue(NeroEventRules.playerTarget(null).isEmpty());
        assertTrue(NeroEventRules.playerTarget("red-team-name-that-is-36-chars-long!").isEmpty());
    }
}
