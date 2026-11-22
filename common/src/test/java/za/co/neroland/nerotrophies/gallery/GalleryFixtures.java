package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

import za.co.neroland.nerotrophies.TestDefs;

/** Catalogues for the gallery tests: the shipped shape (22 Book categories, 218 trophies, 21 collections) or any size. */
final class GalleryFixtures {

    /** Trophies per category in the shipped catalogue, in rail order. */
    static final int[] SHIPPED = {5, 6, 17, 7, 13, 6, 10, 20, 12, 7, 8, 10, 14, 11, 11, 21, 9, 9, 7, 5, 8, 2};

    private GalleryFixtures() {
    }

    static TrophyDefinition trophy(String category, int n, TrophyTier tier, boolean hidden, int order) {
        return TrophyDefinition.of(TestDefs.id(category + "/t" + n), new TrophyDefinition.Data(
                Identifier.withDefaultNamespace("gold_ingot"), tier,
                Optional.of(new TrophyTrigger.DimensionVisited(Identifier.withDefaultNamespace("the_nether"))), hidden,
                Optional.empty(), Optional.empty(), List.of(), Optional.of(category), Optional.empty(), order, 1));
    }

    /** {@code sizes[i]} trophies in category {@code cat<i>}, tiers cycling, every ninth hidden; collections of 2-9. */
    static TrophyDefinitions.Snapshot snapshot(int[] sizes, int collections) {
        List<TrophyDefinition> trophies = new ArrayList<>();
        TrophyTier[] tiers = TrophyTier.values();
        int k = 0;
        for (int c = 0; c < sizes.length; c++) {
            for (int n = 0; n < sizes[c]; n++) {
                trophies.add(trophy("cat" + c, n, tiers[(k * 7 + n) % 4], k % 9 == 8, c));
                k++;
            }
        }
        List<TrophyCollection> cols = new ArrayList<>();
        for (int i = 0; i < collections && !trophies.isEmpty(); i++) {
            int size = 2 + i % 8;
            Identifier[] members = new Identifier[size];
            for (int m = 0; m < size; m++) {
                members[m] = trophies.get((i * 5 + m) % trophies.size()).id();
            }
            cols.add(TestDefs.collection("collection/c" + i, i % 2 == 0 ? "laurel_gold" : "prism",
                    members));
        }
        return TestDefs.snapshot(trophies, cols, 1L);
    }

    static GalleryCatalogue shippedCatalogue() {
        return GalleryCatalogue.of(snapshot(SHIPPED, 21));
    }

    static GalleryPlan plan(GalleryCatalogue catalogue) {
        GalleryLayout.Result result = GalleryLayout.plan(catalogue);
        return result.plan().orElseThrow(() -> new AssertionError("too large: " + result.length()));
    }
}
