package za.co.neroland.nerotrophies.trigger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;

/**
 * Completed advancements reported by the {@code PlayerAdvancements#award} mixin, drained at the next trigger tick so
 * no award runs inside vanilla's award stack. Only advancements some loaded trophy names are queued; the queue is
 * bounded, and anything dropped is caught by the next retroactive pass.
 */
public final class AdvancementQueue {

    /** Upper bound on queued completions between two ticks. */
    public static final int MAX_QUEUED = 1024;

    /** A completed advancement for a player. */
    public record Completed(UUID player, Identifier advancement) {
    }

    private static final ArrayDeque<Completed> QUEUE = new ArrayDeque<>();
    private static volatile Set<Identifier> wanted = Set.of();

    private AdvancementQueue() {
    }

    /** Whether some loaded trophy names this advancement (the mixin's cheap first check). */
    public static boolean wants(Identifier advancement) {
        return advancement != null && wanted.contains(advancement);
    }

    /** Called by the mixin once an advancement is complete. */
    public static synchronized void offer(UUID player, Identifier advancement) {
        if (player == null || !wants(advancement) || QUEUE.size() >= MAX_QUEUED) {
            return;
        }
        QUEUE.add(new Completed(player, advancement));
    }

    /** Everything queued since the last drain, oldest first. */
    public static synchronized List<Completed> drain() {
        if (QUEUE.isEmpty()) {
            return List.of();
        }
        List<Completed> out = new ArrayList<>(QUEUE);
        QUEUE.clear();
        return out;
    }

    static void setWanted(Set<Identifier> ids) {
        wanted = Set.copyOf(ids);
    }

    static synchronized void clear() {
        QUEUE.clear();
        wanted = Set.of();
    }
}
