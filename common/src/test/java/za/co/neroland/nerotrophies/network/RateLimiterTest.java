package za.co.neroland.nerotrophies.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** Ten C2S payloads per second per player, one book refresh per five seconds; players are independent. */
class RateLimiterTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void tenPerSecond() {
        AtomicLong now = new AtomicLong();
        RateLimiter r = new RateLimiter(now::get);
        for (int i = 0; i < RateLimiter.PER_SECOND; i++) {
            assertTrue(r.tryAcquire(A));
        }
        assertFalse(r.tryAcquire(A));
        assertTrue(r.tryAcquire(B));
        now.addAndGet(1_000_000_000L);
        assertTrue(r.tryAcquire(A));
    }

    @Test
    void refreshEveryFiveSeconds() {
        AtomicLong now = new AtomicLong(123L);
        RateLimiter r = new RateLimiter(now::get);
        assertTrue(r.tryRefresh(A));
        now.addAndGet(4_000_000_000L);
        assertFalse(r.tryRefresh(A));
        now.addAndGet(1_000_000_000L);
        assertTrue(r.tryRefresh(A));
    }
}
