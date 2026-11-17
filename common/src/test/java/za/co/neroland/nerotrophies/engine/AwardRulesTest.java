package za.co.neroland.nerotrophies.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/** Idempotent awards, collection completion, trims and the announcement rule. */
class AwardRulesTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    @Test
    void awardIsIdempotent() {
        TrophyState s = new TrophyState();
        assertTrue(s.award(A, TestDefs.id("t"), 1L, 5L).added());
        assertFalse(s.award(A, TestDefs.id("t"), 2L, 6L).added());
        TrophyRecord r = s.record(A).orElseThrow();
        assertEquals(1, r.earnedEntries().size());
        assertEquals(1L, r.earned(TestDefs.id("t")).orElseThrow().at(), "the first award time is kept");
        assertEquals(5L, r.earned(TestDefs.id("t")).orElseThrow().token());
        assertTrue(s.owner(6L).isEmpty(), "the second token was never indexed");
    }

    @Test
    void earnedEntriesAreBounded() {
        TrophyState s = new TrophyState();
        for (int i = 0; i < TrophyState.MAX_EARNED; i++) {
            assertTrue(s.award(A, TestDefs.id("t" + i), 1L, 100L + i).added());
        }
        assertFalse(s.award(A, TestDefs.id("over"), 1L, 99_999L).added());
    }

    @Test
    void collectionsCompleteOnceWhenEveryMemberIsHeld() {
        TrophyCollection c = TestDefs.collection("set", "laurel_gold", TestDefs.id("a"), TestDefs.id("b"));
        TrophyDefinitions.Snapshot defs = TestDefs.snapshot(
                List.of(TestDefs.trophy("a", TrophyTier.BRONZE), TestDefs.trophy("b", TrophyTier.SILVER)), List.of(c), 1L);
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("a"), 1L, 10L);
        assertTrue(CollectionRules.newlyCompletedBy(s.record(A).orElseThrow(), defs, TestDefs.id("a")).isEmpty());
        s.award(A, TestDefs.id("b"), 2L, 11L);
        assertEquals(List.of(c), CollectionRules.newlyCompletedBy(s.record(A).orElseThrow(), defs, TestDefs.id("b")));
        assertTrue(s.completeCollection(A, c.id()));
        assertFalse(s.completeCollection(A, c.id()));
        assertTrue(CollectionRules.allNewlyCompleted(s.record(A).orElseThrow(), defs).isEmpty());
    }

    @Test
    void trimsNeedTheEarnedTrimExceptStandardAndTheCurrentOne() {
        TrophyState s = new TrophyState();
        assertTrue(TrimRules.canApply(s, A, TrophyIds.STANDARD_TRIM, "prism"));
        assertTrue(TrimRules.canApply(s, A, "prism", "prism"), "may keep what someone else applied");
        assertFalse(TrimRules.canApply(s, A, "prism", TrophyIds.STANDARD_TRIM));
        s.award(A, TrophyIds.trimId("prism"), 1L, 0L);
        assertTrue(TrimRules.canApply(s, A, "prism", TrophyIds.STANDARD_TRIM));
        assertFalse(TrimRules.canApply(s, A, "Bad Trim!", TrophyIds.STANDARD_TRIM));
        assertTrue(TrophyIds.isTrimId(TrophyIds.trimId("prism")));
        assertFalse(TrophyIds.isTrimId(TestDefs.id("prism")));
    }

    @Test
    void announcementsNeedTierLiveSourceBothTogglesAndTheGameRule() {
        TrophyState s = new TrophyState();
        s.award(A, TestDefs.id("t"), 1L, 5L);
        TrophyRecord r = s.record(A).orElseThrow();
        EnumSet<TrophyTier> tiers = EnumSet.copyOf(NeroTrophiesConfig.parseTiers("gold, NERO ,bogus"));
        assertEquals(EnumSet.of(TrophyTier.GOLD, TrophyTier.NERO), tiers);
        assertTrue(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.LIVE, r, tiers, true));
        assertTrue(Announcements.shouldAnnounce(TrophyTier.NERO, AwardSource.API, r, tiers, true));
        assertFalse(Announcements.shouldAnnounce(TrophyTier.BRONZE, AwardSource.LIVE, r, tiers, true));
        assertFalse(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.RETROACTIVE, r, tiers, true));
        assertFalse(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.COMMAND, r, tiers, true));
        assertFalse(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.LIVE, r, tiers, false));
        s.setShowName(A, false);
        assertFalse(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.LIVE, r, tiers, true),
                "a hidden name is not announced either");
        s.setShowName(A, true);
        s.setAnnounce(A, false);
        assertFalse(Announcements.shouldAnnounce(TrophyTier.GOLD, AwardSource.LIVE, r, tiers, true));
    }
}
