package za.co.neroland.nerotrophies.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Cabinet;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Mat;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Pedestal;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Sign;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Solid;

/** The hall shows every loaded trophy, collection, finish, trim and display block, and fits. */
class GalleryLayoutTest {

    @Test
    void bayLengthGrowsByFourForEveryThreeTrophies() {
        assertEquals(7, GalleryLayout.bayLength(1));
        assertEquals(7, GalleryLayout.bayLength(7));
        assertEquals(11, GalleryLayout.bayLength(8));
        assertEquals(11, GalleryLayout.bayLength(10));
        assertEquals(15, GalleryLayout.bayLength(11));
        assertEquals(19, GalleryLayout.bayLength(16));
    }

    @Test
    void theFullCatalogueFitsAndEveryTrophyHasOnePedestalAndSign() {
        GalleryCatalogue catalogue = GalleryFixtures.shippedCatalogue();
        GalleryPlan plan = GalleryFixtures.plan(catalogue);
        assertTrue(plan.length() <= GalleryLayout.MAX_LENGTH, "length " + plan.length());
        assertTrue(GalleryFrame.WIDTH <= GalleryLayout.MAX_WIDTH);

        Map<Identifier, Integer> onPedestals = new HashMap<>();
        int trophySigns = 0;
        Set<String> titles = new HashSet<>();
        for (TrophyDefinition def : catalogue.all()) {
            titles.add(def.title());
        }
        for (GalleryPiece piece : plan.cells()) {
            if (piece instanceof Pedestal p && p.stamp().isPresent()) {
                onPedestals.merge(p.stamp().get().trophy(), 1, Integer::sum);
                assertEquals(TrophyStamp.GALLERY_TOKEN, p.stamp().get().token());
            }
            if (piece instanceof Sign s && !s.lines().isEmpty() && titles.contains(s.lines().get(0).key())) {
                trophySigns++;
            }
        }
        assertEquals(catalogue.trophyCount(), onPedestals.size(), "every loaded trophy is on a pedestal");
        onPedestals.values().forEach(n -> assertEquals(1, n, "each exactly once"));
        assertEquals(catalogue.trophyCount(), trophySigns, "each with a sign");
        assertEquals(214, catalogue.trophyCount());
    }

    @Test
    void theLobbyHasSixPlatesFourTiersAndThreePlateStates() {
        GalleryCatalogue catalogue = GalleryFixtures.shippedCatalogue();
        GalleryPlan plan = GalleryFixtures.plan(catalogue);
        assertEquals(6, plan.plates().size());
        assertEquals(List.of(GalleryPiece.Demo.values()), plan.plates().stream().map(GalleryPlan.PlateSpot::demo).toList());
        Set<TrophyTier> tierRow = new HashSet<>();
        List<Long> plateTokens = new ArrayList<>();
        for (int w = -10; w <= -4; w += 2) {
            GalleryPiece piece = plan.at(new GalleryFrame.Local(4, w, 2));
            TrophyStamp stamp = ((GalleryPiece.Trophy) piece).stamp();
            assertEquals(TrophyStamp.GALLERY_TOKEN, stamp.token());
            tierRow.add(catalogue.all().stream().filter(d -> d.id().equals(stamp.trophy())).findFirst()
                    .orElseThrow().tier());
        }
        assertEquals(Set.of(TrophyTier.values()), tierRow);
        for (int w = 4; w <= 8; w += 2) {
            plateTokens.add(((GalleryPiece.Trophy) plan.at(new GalleryFrame.Local(4, w, 2))).stamp().token());
        }
        assertEquals(List.of(TrophyStamp.GALLERY_TOKEN, TrophyStamp.GALLERY_BLANK_TOKEN, TrophyStamp.UNSTAMPED),
                plateTokens, "named display copy, nameless copy, replica side by side");
    }

    @Test
    void everyCollectionStandsEmptyPartialAndComplete() {
        GalleryCatalogue catalogue = GalleryFixtures.shippedCatalogue();
        GalleryPlan plan = GalleryFixtures.plan(catalogue);
        for (TrophyCollection collection : catalogue.collections()) {
            List<Cabinet> bound = new ArrayList<>();
            for (GalleryPiece piece : plan.cells()) {
                if (piece instanceof Cabinet c && c.collection().equals(Optional.of(collection.id()))) {
                    bound.add(c);
                    assertTrue(c.displayOnly(), "gallery collection cabinets are display-only");
                }
            }
            int width = (collection.members().size() + 5) / 6;
            assertEquals(3 * width, bound.size());
            Set<Integer> fills = new HashSet<>();
            int complete = 0;
            int partial = 0;
            for (Cabinet c : bound) {
                if (!c.trim().equals("standard")) {
                    complete += c.contents().size();
                    assertEquals(Trims.byId(collection.trim()).id(), c.trim(), "the collection's own trim on top");
                } else {
                    partial += c.contents().size();
                }
                fills.add(c.contents().size());
            }
            assertEquals(collection.members().size(), complete);
            assertEquals((collection.members().size() + 1) / 2, partial);
            assertTrue(fills.contains(0), "an empty row");
        }
    }

    @Test
    void everyFinishAndTrimIsShown() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.shippedCatalogue());
        Set<Optional<Identifier>> finishes = new HashSet<>();
        Set<String> trims = new HashSet<>();
        int emptyPedestals = 0;
        for (GalleryPiece piece : plan.cells()) {
            if (piece instanceof Pedestal p && p.stamp().isEmpty() && !p.showpiece()) {
                finishes.add(p.finish());
                emptyPedestals++;
            }
            if (piece instanceof Cabinet c && c.collection().isEmpty() && !c.wall() && c.finish().isEmpty()) {
                trims.add(c.trim());
            }
        }
        assertEquals(21, emptyPedestals);
        assertEquals(Set.copyOf(GalleryLayout.FINISHES), finishes);
        assertEquals(Trims.ALL.stream().map(Trims.Trim::id).collect(java.util.stream.Collectors.toSet()), trims);
        assertTrue(java.util.Arrays.stream(plan.cells()).anyMatch(p -> p instanceof GalleryPiece.Plaque));
        assertTrue(java.util.Arrays.stream(plan.cells()).anyMatch(p -> p instanceof Cabinet c && c.wall()));
        assertTrue(java.util.Arrays.stream(plan.cells()).anyMatch(p -> p instanceof Pedestal d && d.showpiece()));
        assertTrue(java.util.Arrays.stream(plan.cells()).anyMatch(p -> p instanceof GalleryPiece.Spotlight));
    }

    @Test
    void theShellIsClosedExceptTheEntrance() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.shippedCatalogue());
        for (int i = 0; i < plan.cells().length; i++) {
            GalleryFrame.Local c = GalleryPlan.local(i);
            boolean boundary = c.y() == 0 || c.y() == GalleryFrame.HEIGHT - 1 || c.l() == 0
                    || c.l() == plan.length() - 1 || c.w() == GalleryFrame.W_MIN || c.w() == GalleryFrame.W_MAX;
            if (!boundary) {
                continue;
            }
            boolean entrance = c.l() == 0 && Math.abs(c.w()) <= 2 && c.y() >= 1 && c.y() <= 3;
            GalleryPiece piece = plan.cells()[i];
            if (entrance) {
                assertEquals(new Solid(Mat.AIR), piece);
            } else {
                assertTrue(piece instanceof Solid s && s.mat() != Mat.AIR, "closed at " + c);
            }
        }
    }

    @Test
    void signsCarryLangKeysOnly() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.shippedCatalogue());
        for (GalleryPiece piece : plan.cells()) {
            if (piece instanceof Sign s) {
                assertFalse(s.lines().isEmpty());
                for (GalleryText line : s.lines()) {
                    assertTrue(line.blank() || line.key().matches("[a-z0-9_.]+"), line.key());
                }
            }
        }
    }

    @Test
    void aCatalogueTooBigForTheCapIsRefusedWithItsLength() {
        int[] sizes = new int[60];
        java.util.Arrays.fill(sizes, 16);
        GalleryLayout.Result result = GalleryLayout.plan(GalleryCatalogue.of(GalleryFixtures.snapshot(sizes, 0)));
        assertTrue(result.tooLarge());
        assertTrue(result.length() > GalleryLayout.MAX_LENGTH);
    }

    @Test
    void aSmallCatalogueStillBuildsAClosedHall() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryCatalogue.of(GalleryFixtures.snapshot(new int[] {2}, 1)));
        assertTrue(plan.length() >= GalleryLayout.FIRST_AISLE_L + GalleryLayout.MIN_AISLE);
    }
}
