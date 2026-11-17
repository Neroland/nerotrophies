package za.co.neroland.nerotrophies.engine;

import java.util.Objects;

/**
 * The seams the later work packages plug into, installed once during common init (the reserved steps in
 * {@code NeroTrophiesCommon}). Each defaults to a no-op so the engine runs, and is testable, on its own.
 */
public final class EngineHooks {

    private static volatile TriggerEvaluator triggers = TriggerEvaluator.NONE;
    private static volatile NameSync names = NameSync.NONE;
    private static volatile ClientNotifier client = ClientNotifier.NONE;

    private EngineHooks() {
    }

    public static TriggerEvaluator triggers() {
        return triggers;
    }

    public static NameSync names() {
        return names;
    }

    public static ClientNotifier client() {
        return client;
    }

    /** Installed by the triggers work package ({@code trigger/}). */
    public static void setTriggerEvaluator(TriggerEvaluator evaluator) {
        triggers = Objects.requireNonNullElse(evaluator, TriggerEvaluator.NONE);
    }

    /** Installed by the name-sync work package ({@code network/}); also receives erasure invalidations. */
    public static void setNameSync(NameSync sync) {
        names = Objects.requireNonNullElse(sync, NameSync.NONE);
    }

    /** Installed by the network work package. */
    public static void setClientNotifier(ClientNotifier notifier) {
        client = Objects.requireNonNullElse(notifier, ClientNotifier.NONE);
    }
}
