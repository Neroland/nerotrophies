package za.co.neroland.nerotrophies.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.TestDefs;
import za.co.neroland.nerotrophies.network.RecordPayload;

/** The Trophy Book's tabs and filters: hidden unearned trophies sort last and never appear under a tier filter. */
class BookModelTest {

    private static RecordPayload.Entry entry(String path, String category, TrophyTier tier, boolean earned, int order) {
        return new RecordPayload.Entry(TestDefs.id(path), category, earned, earned, false, false, tier,
                Identifier.withDefaultNamespace("gold_ingot"), "trophy.t." + path, "trophy.d." + path, earned ? 5L : 0L,
                0L, order, 0, 0);
    }

    @Test
    void tabsCountsAndFilters() {
        RecordPayload record = new RecordPayload(List.of(
                entry("a", "nerospace", TrophyTier.GOLD, true, 1),
                RecordPayload.Entry.masked(TestDefs.id("secret"), "nerospace", 0),
                entry("b", "nerotech", TrophyTier.BRONZE, false, 2)), List.of(), List.of(), true, true, 10L);
        BookModel model = new BookModel(record);
        assertEquals(3, model.tabs().size());
        assertEquals(1, model.tabs().get(0).earned());
        assertEquals(3, model.tabs().get(0).total());
        assertEquals("nerospace", model.tabs().get(1).category());

        List<RecordPayload.Entry> space = model.cards("nerospace", BookModel.noFilters());
        assertEquals(TestDefs.id("a"), space.get(0).id());
        assertTrue(space.get(1).masked(), "hidden unearned last");

        List<RecordPayload.Entry> gold = model.cards(null, EnumSet.of(TrophyTier.GOLD));
        assertEquals(1, gold.size());
        assertTrue(model.cards(null, EnumSet.of(TrophyTier.GOLD, TrophyTier.BRONZE)).stream()
                .noneMatch(RecordPayload.Entry::masked), "a filter never shows a hidden card");
        assertEquals(1, model.tierCount(TrophyTier.GOLD)[0]);
        assertEquals(0, model.tierCount(TrophyTier.NERO)[1]);
    }
}
