package za.co.neroland.nerotrophies.trigger;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.economy.CurrencyEvents;
import za.co.neroland.nerolandcore.progression.GateEvents;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneEvents;
import za.co.neroland.nerolandcore.reputation.ReputationEvents;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

/**
 * Live {@code nero_event} matching against Core's buses, as pure functions. Only events that name one player award:
 * a {@code PLAYER}-scope gate that opened, a {@code PLAYER}-scope material milestone that became present, and a
 * reputation or currency value that rose across {@code at_least} while a real provider is registered. TEAM and
 * SERVER gates and milestones never award, a target that is not a UUID never awards, and nothing is ever resolved to
 * a nearby or online player. {@code ThresholdEvents} is not listened to at all.
 */
public final class NeroEventRules {

    /** Where a matched trophy goes ({@code TrophyEngine.awardLive} in the game). Returns true if newly earned. */
    @FunctionalInterface
    public interface Awarder {
        boolean award(UUID player, Identifier trophy);
    }

    private NeroEventRules() {
    }

    /** @return how many trophies were newly earned */
    public static int onGate(TriggerIndex index, GateEvents.GateChange e, Awarder awarder) {
        if (e.scope() != GateScope.PLAYER || !e.open()) {
            return 0;
        }
        Optional<UUID> player = playerTarget(e.target());
        if (player.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (TrophyDefinition d : index.byGate(e.gate())) {
            n += awarder.award(player.get(), d.id()) ? 1 : 0;
        }
        return n;
    }

    public static int onMilestone(TriggerIndex index, MaterialMilestoneEvents.Change e, Awarder awarder) {
        if (e.scope() != GateScope.PLAYER || !e.present()) {
            return 0;
        }
        Optional<UUID> player = playerTarget(e.target());
        if (player.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (TrophyDefinition d : index.byMilestone(e.milestone())) {
            Optional<Identifier> wanted = d.trigger().get() instanceof TrophyTrigger.NeroEvent ne
                    ? ne.material() : Optional.empty();
            if (wanted.isEmpty() || wanted.get().equals(e.material())) {
                n += awarder.award(player.get(), d.id()) ? 1 : 0;
            }
        }
        return n;
    }

    public static int onReputation(TriggerIndex index, ReputationEvents.ReputationChange e, boolean realProvider,
            Awarder awarder) {
        if (!realProvider || e.player() == null) {
            return 0;
        }
        int n = 0;
        for (TrophyDefinition d : index.byFaction(e.faction())) {
            if (TriggerRules.crossed(e.oldValue(), e.newValue(), atLeast(d))) {
                n += awarder.award(e.player(), d.id()) ? 1 : 0;
            }
        }
        return n;
    }

    public static int onCurrency(TriggerIndex index, CurrencyEvents.BalanceChange e, boolean realProvider,
            Awarder awarder) {
        if (!realProvider || e.player() == null || e.currency() == null) {
            return 0;
        }
        int n = 0;
        for (TrophyDefinition d : index.byCurrency(e.currency().id())) {
            if (TriggerRules.crossed(e.oldBalance(), e.newBalance(), atLeast(d))) {
                n += awarder.award(e.player(), d.id()) ? 1 : 0;
            }
        }
        return n;
    }

    /** A {@code PLAYER}-scope target is a UUID string; anything else names no player. */
    public static Optional<UUID> playerTarget(String target) {
        if (target == null || target.length() != 36) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(target));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static long atLeast(TrophyDefinition d) {
        return d.trigger().get() instanceof TrophyTrigger.NeroEvent ne ? ne.atLeast() : Long.MAX_VALUE;
    }
}
