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

/** Catalogues for the gallery tests: the 0.1.0 shape (21 Book categories, 152 trophies, 20 collections) or any size. */
final class GalleryFixtures {

    /** Trophies per category in the 0.1.0 catalogue, in rail order. */
    static final int[] CATALOGUE_0_1_0 = {5, 6, 13, 7, 7, 6, 8, 11, 9, 6, 6, 3, 11, 8, 10, 14, 6, 5, 5, 4, 2};

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

    static GalleryCatalogue catalogue_0_1_0() {
        return GalleryCatalogue.of(snapshot(CATALOGUE_0_1_0, 20));
    }

    static GalleryPlan plan(GalleryCatalogue catalogue) {
        GalleryLayout.Result result = GalleryLayout.plan(catalogue);
        return result.plan().orElseThrow(() -> new AssertionError("too large: " + result.length()));
    }
}
