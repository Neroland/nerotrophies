package za.co.neroland.nerotrophies.engine;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

/**
 * Trigger evaluation, implemented by the triggers work package in {@code trigger/} and installed with
 * {@link EngineHooks#setTriggerEvaluator}. The engine's {@link RetroQueue} calls {@link #retroactive} within the
 * retroactive budget; the evaluator runs its own live listeners (Core buses, the advancement mixin queue) and the
 * staggered periodic pass from {@link #tick}, awarding through {@link TrophyEngine#awardLive}.
 *
 * <p>Every method runs on the server thread, must be cheap, must never throw for an unknown id (absent mods cost
 * nothing) and must never log a player.
 */
public interface TriggerEvaluator {

    /** Evaluates nothing (the default until the trigger layer installs itself). */
    TriggerEvaluator NONE = new TriggerEvaluator() {
        @Override
        public boolean retroactive(ServerPlayer player, TrophyDefinition definition) {
            return false;
        }
    };

    /**
     * The retroactive read for one unearned definition : true if the player has
     * already done the thing. Definitions without a trigger never reach here. {@code dimension_visited} may only
     * answer for the current dimension.
     */
    boolean retroactive(ServerPlayer player, TrophyDefinition definition);

    /**
     * Progress toward a {@code stat}, {@code item_obtained} or {@code block_placed} trophy, read live from vanilla
     * statistics. Empty for every other trigger. Never stored.
     */
    default Optional<Progress> progress(ServerPlayer player, TrophyDefinition definition) {
        return Optional.empty();
    }

    /** Once per server tick, before the retroactive queue: live queues and the periodic pass. */
    default void tick(MinecraftServer server) {
    }

    /** Definitions changed (generation moved): rebuild any per-generation caches. */
    default void definitionsChanged(MinecraftServer server) {
    }

    /** A player left: drop any per-player scheduling state. */
    default void playerLeft(MinecraftServer server, UUID player) {
    }

    /** Current value toward a target; {@code current} is clamped to {@code target} by the caller. */
    record Progress(int current, int target) {
        public Progress {
            target = Math.max(1, target);
            current = Math.max(0, Math.min(current, target));
        }
    }
}
