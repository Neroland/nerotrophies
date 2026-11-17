package za.co.neroland.nerotrophies.trigger;

import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.event.CoreEvents;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneEvents;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;

/**
 * Installs the {@link TriggerEvaluator} with {@link EngineHooks#setTriggerEvaluator} and subscribes it to Core's
 * player-scoped buses: progression gates, material milestones, reputation and currency. {@code ThresholdEvents} is
 * deliberately not subscribed (a crossing names no player). Advancements arrive through the common
 * {@code PlayerAdvancements#award} mixin; statistics, items, blocks and dimensions through the periodic pass. Live
 * awards go through {@link za.co.neroland.nerotrophies.engine.TrophyEngine#awardLive}.
 */
public final class TrophyTriggers {

    private static boolean installed;

    private TrophyTriggers() {
    }

    /** Common init step 5. Idempotent. */
    public static synchronized void init() {
        if (installed) {
            return;
        }
        installed = true;
        VanillaTriggerEvaluator evaluator = VanillaTriggerEvaluator.INSTANCE;
        EngineHooks.setTriggerEvaluator(evaluator);
        CoreEvents.onProgression(evaluator::onGate);
        MaterialMilestoneEvents.onChange(evaluator::onMilestone);
        CoreEvents.onReputation(evaluator::onReputation);
        CoreEvents.onCurrency(evaluator::onCurrency);
    }

    /**
     * Progress toward a {@code stat}, {@code item_obtained} or {@code block_placed} trophy, computed now from vanilla
     * statistics and never stored. Empty for every other trigger, for hidden trophies and for ids the game does not
     * know. For the {@code record} payload and the link section.
     */
    public static Optional<TriggerEvaluator.Progress> progress(ServerPlayer player, TrophyDefinition definition) {
        return EngineHooks.triggers().progress(player, definition);
    }
}
