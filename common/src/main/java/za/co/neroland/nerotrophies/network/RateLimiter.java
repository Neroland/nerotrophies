package za.co.neroland.nerotrophies.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Per-player C2S budget (decision 009): {@value #PER_SECOND} payloads per second, excess dropped silently, plus a
 * separate {@code book_refresh} interval. Keyed by the sender's UUID in memory only, forgotten on logout.
 * Server thread.
 */
public final class RateLimiter {

    public static final int PER_SECOND = 10;
    public static final long REFRESH_INTERVAL_NANOS = 5_000_000_000L;
    private static final long WINDOW_NANOS = 1_000_000_000L;

    private static final class Window {
        long start;
        int count;
        long lastRefresh = Long.MIN_VALUE;
    }

    private final Map<UUID, Window> windows = new HashMap<>();
    private final LongSupplier clock;

    public RateLimiter(LongSupplier nanoClock) {
        this.clock = nanoClock;
    }

    /** Whether one more payload from {@code player} may be handled now. */
    public boolean tryAcquire(UUID player) {
        long now = clock.getAsLong();
        Window w = windows.computeIfAbsent(player, k -> {
            Window x = new Window();
            x.start = now;
            return x;
        });
        if (now - w.start >= WINDOW_NANOS) {
            w.start = now;
            w.count = 0;
        }
        if (w.count >= PER_SECOND) {
            return false;
        }
        w.count++;
        return true;
    }

    /** Whether a {@code book_refresh} may be answered now (at most one per 5 s). */
    public boolean tryRefresh(UUID player) {
        long now = clock.getAsLong();
        Window w = windows.computeIfAbsent(player, k -> {
            Window x = new Window();
            x.start = now;
            return x;
        });
        if (w.lastRefresh != Long.MIN_VALUE && now - w.lastRefresh < REFRESH_INTERVAL_NANOS) {
            return false;
        }
        w.lastRefresh = now;
        return true;
    }

    public void forget(UUID player) {
        windows.remove(player);
    }

    public void clear() {
        windows.clear();
    }
}
