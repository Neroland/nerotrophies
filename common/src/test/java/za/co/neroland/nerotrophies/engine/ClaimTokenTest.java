package za.co.neroland.nerotrophies.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;

/** the design: token mint, claim, re-claim, replica detection, cooldown and the reserved tokens. */
class ClaimTokenTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final Identifier T = TestDefs.id("first");
    private static final long COOLDOWN = 600_000L;

    private static TrophyState earned() {
        TrophyState s = new TrophyState();
        s.award(A, T, 1L, TrophyTokens.mint(s, new Random(1)));
        s.refreshName(A, "Alice", 1L);
        return s;
    }

    @Test
    void mintedTokensAreNeverReservedOrReused() {
        TrophyState s = new TrophyState();
        // A generator that first yields reserved values and a used one must be redrawn past them.
        long[] seq = {0L, -1L, -2L, -16L, 42L, 42L, 43L};
        int[] i = {0};
        java.util.random.RandomGenerator fake = () -> seq[i[0]++];
        assertEquals(42L, TrophyTokens.mint(s, fake));
        s.award(A, T, 1L, 42L);
        assertEquals(43L, TrophyTokens.mint(s, fake));
        for (long r = TrophyStamp.RESERVED_MIN; r <= 0; r++) {
            assertTrue(TrophyStamp.isReserved(r));
        }
        assertFalse(TrophyStamp.isReserved(1L));
        assertFalse(TrophyStamp.isReserved(TrophyStamp.RESERVED_MIN - 1));
    }

    @Test
    void firstClaimHandsOutTheCurrentTokenAndItIsGenuineWithTheName() {
        TrophyState s = earned();
        long token = s.record(A).orElseThrow().earned(T).orElseThrow().token();
        ClaimService.Outcome o = ClaimService.claim(s, A, T, true, 10L, COOLDOWN, new Random(2));
        assertEquals(ClaimService.Result.CLAIMED, o.result());
        assertEquals(token, o.token());
        TokenResolver.Resolution r = TokenResolver.resolve(s, T, token);
        assertEquals(TrophyStatus.GENUINE, r.status());
        assertEquals("Alice", r.name().orElseThrow());
        assertTrue(r.counts());
    }

    @Test
    void reclaimMintsANewTokenAndTheOldCopyBecomesAReplica() {
        TrophyState s = earned();
        long first = ClaimService.claim(s, A, T, true, 10L, COOLDOWN, new Random(2)).token();
        ClaimService.Outcome again = ClaimService.claim(s, A, T, true, 10L + COOLDOWN, COOLDOWN, new Random(3));
        assertEquals(ClaimService.Result.RECLAIMED, again.result());
        assertEquals(first, again.retired());
        assertNotEquals(first, again.token());
        assertEquals(TrophyStatus.REPLICA, TokenResolver.resolve(s, T, first).status());
        assertFalse(TokenResolver.resolve(s, T, first).counts());
        assertTrue(TokenResolver.resolve(s, T, first).name().isEmpty());
        assertEquals(TrophyStatus.GENUINE, TokenResolver.resolve(s, T, again.token()).status());
        assertEquals(1, s.retiredCount(), "exactly one genuine copy remains");
    }

    @Test
    void reclaimHonoursTheCooldown() {
        TrophyState s = earned();
        ClaimService.claim(s, A, T, true, 10L, COOLDOWN, new Random(2));
        ClaimService.Outcome early = ClaimService.claim(s, A, T, true, 10L + COOLDOWN - 1, COOLDOWN, new Random(3));
        assertEquals(ClaimService.Result.COOLDOWN, early.result());
        assertEquals(10L + COOLDOWN, early.cooldownEndsAt());
    }

    @Test
    void claimsAreRefusedForWhatWasNotEarned() {
        TrophyState s = earned();
        assertEquals(ClaimService.Result.NOT_EARNED,
                ClaimService.claim(s, A, TestDefs.id("other"), true, 1L, COOLDOWN, new Random()).result());
        assertEquals(ClaimService.Result.NOT_EARNED,
                ClaimService.claim(s, UUID.randomUUID(), T, true, 1L, COOLDOWN, new Random()).result());
        assertEquals(ClaimService.Result.UNKNOWN_TROPHY,
                ClaimService.claim(s, A, T, false, 1L, COOLDOWN, new Random()).result());
        assertEquals(ClaimService.Result.NOT_CLAIMABLE,
                ClaimService.claim(s, A, TestDefs.id("trim/prism"), true, 1L, COOLDOWN, new Random()).result());
    }

    @Test
    void forgedEditedAndReservedTokensNeverShowAName() {
        TrophyState s = earned();
        long token = s.record(A).orElseThrow().earned(T).orElseThrow().token();
        assertEquals(TrophyStatus.BLANK, TokenResolver.resolve(s, T, 0x1234_5678L).status(), "forged");
        assertEquals(TrophyStatus.REPLICA, TokenResolver.resolve(s, TestDefs.id("gold_thing"), token).status(),
                "a real token on another trophy id");
        assertEquals(TrophyStatus.REPLICA, TokenResolver.resolve(s, T, TrophyStamp.UNSTAMPED).status());
        assertEquals(TrophyStatus.GALLERY, TokenResolver.resolve(s, T, TrophyStamp.GALLERY_TOKEN).status());
        assertEquals(TrophyStatus.BLANK, TokenResolver.resolve(s, T, TrophyStamp.GALLERY_BLANK_TOKEN).status());
        assertEquals(TrophyStatus.BLANK, TokenResolver.resolve(s, T, -9L).status());
    }

    @Test
    void showNameOffBlanksTheNameButKeepsTheTrophyGenuine() {
        TrophyState s = earned();
        long token = s.record(A).orElseThrow().earned(T).orElseThrow().token();
        s.setShowName(A, false);
        TokenResolver.Resolution r = TokenResolver.resolve(s, T, token);
        assertEquals(TrophyStatus.GENUINE, r.status());
        assertTrue(r.name().isEmpty());
        assertTrue(r.counts());
    }

    @Test
    void revokeRetiresTheToken() {
        TrophyState s = earned();
        long token = s.record(A).orElseThrow().earned(T).orElseThrow().token();
        assertTrue(s.revoke(A, T));
        assertEquals(TrophyStatus.REPLICA, TokenResolver.resolve(s, T, token).status());
        assertFalse(s.revoke(A, T));
    }
}
