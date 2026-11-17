package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.gallery.GalleryFrame.Local;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Cabinet;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Demo;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Face;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Mat;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Pedestal;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Sign;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Solid;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Spotlight;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Lays out the trophy hall: a lobby (demo plates, tier row, plate states) behind the entrance,
 * then three aisles sharing walls. Aisles 1 and 2 hold one bay per Book category, each next bay in the shortest of the
 * four bay rows; aisle 3 holds the collections wall, every finish and trim, and the specimens. Pure.
 */
public final class GalleryLayout {

    /** Widest hall allowed (the hall is always {@link GalleryFrame#WIDTH} wide). */
    public static final int MAX_WIDTH = 64;
    /**
     * Longest hall allowed. A 64-block hall fits about 120 trophies; the 152-trophy catalogue with every
     * Nero mod loaded needs about 70 (the bay aisles lengthen first), so the cap allows that with room to spare.
     */
    public static final int MAX_LENGTH = 96;

    static final int FIRST_AISLE_L = 9;
    static final int MIN_AISLE = 4;
    static final int[] AISLE_CENTRES = {-14, 0, 14};
    private static final int[] ROW_CENTRE = {-14, -14, 0, 0};
    private static final int[] ROW_SIDE = {-1, 1, -1, 1};
    static final int MAX_COLLECTION_CABINETS = 4;

    /** The 21 finishes of section F: none, Core's sixteen accents in dye order, then the four materials. */
    public static final List<Optional<Identifier>> FINISHES;
    static final String[] DYES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    static final String[] MATERIALS = {"nero_alloy", "starsteel", "void_crystal", "plasma_glass"};

    static {
        List<Optional<Identifier>> finishes = new ArrayList<>();
        finishes.add(Optional.empty());
        for (String dye : DYES) {
            finishes.add(Optional.of(Identifier.fromNamespaceAndPath("neroland", "accent/" + dye)));
        }
        for (String material : MATERIALS) {
            finishes.add(Optional.of(Identifier.fromNamespaceAndPath("neroland", material)));
        }
        FINISHES = List.copyOf(finishes);
    }

    /** A plan, or the length it would need when that is over {@link #MAX_LENGTH}. */
    public record Result(Optional<GalleryPlan> plan, int length) {
        public boolean tooLarge() {
            return this.plan.isEmpty();
        }
    }

    private final GalleryCatalogue catalogue;
    private final Map<Local, GalleryPiece> over = new LinkedHashMap<>();
    private final int[] ends = new int[3];

    private GalleryLayout(GalleryCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    public static Result plan(GalleryCatalogue catalogue) {
        return new GalleryLayout(catalogue).run();
    }

    /** {@code Lb}: 7 for up to seven trophies, then 4 more cells for every further three. */
    public static int bayLength(int trophies) {
        if (trophies <= 7) {
            return 7;
        }
        return 7 + 4 * ((trophies - 7 + 2) / 3);
    }

    // ---- the whole hall -----------------------------------------------------------------------------------------

    private Result run() {
        int[] rowLen = new int[4];
        for (GalleryCatalogue.Group group : this.catalogue.groups()) {
            int lb = bayLength(group.trophies().size());
            int row = 0;
            for (int r = 1; r < 4; r++) {
                if (rowLen[r] < rowLen[row]) {
                    row = r;
                }
            }
            this.bay(group, row, FIRST_AISLE_L + rowLen[row], lb);
            rowLen[row] += lb + 1;
        }
        this.ends[0] = FIRST_AISLE_L + Math.max(MIN_AISLE, Math.max(rowLen[0], rowLen[1]));
        this.ends[1] = FIRST_AISLE_L + Math.max(MIN_AISLE, Math.max(rowLen[2], rowLen[3]));
        this.ends[2] = this.aisleThree();
        int length = Math.max(this.ends[0], Math.max(this.ends[1], this.ends[2])) + 1;
        if (length > MAX_LENGTH || GalleryFrame.WIDTH > MAX_WIDTH) {
            return new Result(Optional.empty(), length);
        }
        this.lobby();
        for (int k = 0; k < 3; k++) {
            for (int l = FIRST_AISLE_L; l < this.ends[k]; l += 4) {
                this.over.putIfAbsent(new Local(l, AISLE_CENTRES[k], 6), new Spotlight(Face.DOWN));
            }
        }
        GalleryPiece[] cells = new GalleryPiece[length * GalleryFrame.WIDTH * GalleryFrame.HEIGHT];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Solid(this.shell(GalleryPlan.local(i), length));
        }
        List<GalleryPlan.PlateSpot> plates = new ArrayList<>();
        this.over.forEach((local, piece) -> {
            cells[GalleryPlan.index(local)] = piece;
            if (piece instanceof GalleryPiece.Plate plate) {
                plates.add(new GalleryPlan.PlateSpot(local, plate.demo()));
            }
        });
        return new Result(Optional.of(new GalleryPlan(length, cells, plates)), length);
    }

    private static int aisleOf(int w) {
        return w < -7 ? 0 : w > 7 ? 2 : 1;
    }

    /** The shell: floor, walls, ceiling and air (section 9.1), before anything is placed in it. */
    private Mat shell(Local c, int length) {
        int l = c.l();
        int w = c.w();
        int y = c.y();
        if (y == 0) {
            if (l >= 1 && l <= 7 && l == 4 && Math.abs(w) <= 20) {
                return Mat.RUNNER;
            }
            if (l >= FIRST_AISLE_L && w != -7 && w != 7 && Math.abs(w) < 21) {
                int k = aisleOf(w);
                if (l < this.ends[k] && Math.abs(w - AISLE_CENTRES[k]) <= 1) {
                    return Mat.RUNNER;
                }
            }
            return Mat.FLOOR;
        }
        if (y == GalleryFrame.HEIGHT - 1) {
            return Mat.CEILING;
        }
        if (l == 0) {
            return Math.abs(w) <= 2 && y <= 3 ? Mat.AIR : Mat.WALL;
        }
        if (w == GalleryFrame.W_MIN || w == GalleryFrame.W_MAX || l == length - 1) {
            return Mat.WALL;
        }
        if (l <= 7) {
            return Mat.AIR;
        }
        if (l == 8) {
            for (int centre : AISLE_CENTRES) {
                if (Math.abs(w - centre) <= 2 && y <= 4) {
                    return Mat.AIR;
                }
            }
            return Mat.WALL;
        }
        if (w == -7 || w == 7) {
            return Mat.WALL;
        }
        return l < this.ends[aisleOf(w)] ? Mat.AIR : Mat.WALL;
    }

    private void put(int l, int w, int y, GalleryPiece piece) {
        this.over.put(new Local(l, w, y), piece);
    }

    private static TrophyStamp display(TrophyDefinition def) {
        return new TrophyStamp(def.id(), TrophyStamp.GALLERY_TOKEN);
    }

    private static GalleryText tierName(TrophyTier tier) {
        return GalleryText.of("nerotrophies.tier." + tier.getSerializedName());
    }

    private static List<GalleryText> trophyLines(TrophyDefinition def) {
        return List.of(GalleryText.of(def.title()), GalleryText.of("tooltip.nerotrophies.tier", tierName(def.tier())),
                GalleryText.of(def.description()), GalleryText.BLANK);
    }

    private static Sign wallSign(Face face, GalleryText... lines) {
        return new Sign(face, true, List.of(lines));
    }

    private static Sign standingSign(Face face, GalleryText... lines) {
        return new Sign(face, false, List.of(lines));
    }

    private static GalleryText key(String suffix) {
        return GalleryText.of("nerotrophies.gallery." + suffix);
    }

    // ---- D. a mod bay (section 9.3) ------------------------------------------------------------------------------

    private void bay(GalleryCatalogue.Group group, int row, int l0, int lb) {
        int centre = ROW_CENTRE[row];
        int side = ROW_SIDE[row];
        Face toAisle = side > 0 ? Face.LEFT : Face.RIGHT;
        // Divider closing the far end, full height across d 1-4.
        for (int d = 1; d <= 4; d++) {
            for (int y = 1; y <= 6; y++) {
                this.put(l0 + lb, centre + side * (d + 2), y, new Solid(Mat.BAND));
            }
        }
        // The mod-name sign: on the lobby's back wall beside the opening, or on the previous divider's aisle face.
        GalleryText name = GalleryText.withFallback("nerotrophies.category." + group.category(), group.category());
        if (l0 == FIRST_AISLE_L) {
            this.put(l0, centre + side * 3, 3, wallSign(Face.AWAY, name));
        } else {
            this.put(l0 - 1, centre + side * 2, 3, wallSign(toAisle, name));
        }
        List<int[]> slots = new ArrayList<>(); // {cell, d}
        for (int cell = 1; cell <= lb; cell += 2) {
            slots.add(new int[] {cell, 4});
        }
        for (int cell = 4; cell < lb; cell += 4) {
            slots.add(new int[] {cell, 2});
        }
        slots.add(new int[] {1, 2});
        slots.add(new int[] {lb, 2});
        List<TrophyDefinition> trophies = group.trophies();
        for (int i = 0; i < trophies.size() && i < slots.size(); i++) {
            TrophyDefinition def = trophies.get(i);
            int cell = slots.get(i)[0];
            int d = slots.get(i)[1];
            int l = l0 + cell - 1;
            int w = centre + side * (d + 2);
            this.put(l, w, 1, new Pedestal(toAisle, Optional.empty(), Optional.of(display(def)), false));
            List<GalleryText> lines = trophyLines(def);
            if (d == 4) {
                this.put(l, w, 3, new Sign(toAisle, true, lines));
                this.put(l, w, 4, new Spotlight(toAisle));
            } else if (cell == 1 && i >= slots.size() - 2) {
                this.put(l, w, 3, new Sign(Face.AWAY, true, lines));
            } else if (cell == lb && i >= slots.size() - 2) {
                this.put(l, w, 3, new Sign(Face.TO_ENTRANCE, true, lines));
            } else {
                this.put(l, centre + side * 3, 1, new Sign(toAisle, false, lines));
            }
        }
    }

    // ---- A, B, C: the lobby (section 9.2) ------------------------------------------------------------------------

    private void lobby() {
        Demo[] demos = Demo.values();
        for (int i = 0; i < demos.length; i++) {
            int l = 2 + i;
            this.put(l, -19, 1, new GalleryPiece.Plate(demos[i]));
            this.put(l, -20, 2, wallSign(Face.RIGHT, key("demo.step"),
                    key("demo." + demos[i].name().toLowerCase(java.util.Locale.ROOT))));
        }
        TrophyTier[] tiers = TrophyTier.values();
        for (int i = 0; i < tiers.length; i++) {
            int w = -10 + 2 * i;
            TrophyTier tier = tiers[i];
            this.put(4, w, 1, new Solid(Mat.BASE));
            this.catalogue.first(tier).ifPresent(def ->
                    this.put(4, w, 2, new GalleryPiece.Trophy(Face.TO_ENTRANCE, display(def))));
            String cue = "tier." + tier.getSerializedName() + ".cue";
            this.put(3, w, 1, standingSign(Face.TO_ENTRANCE, tierName(tier), key(cue + "1"), key(cue + "2"),
                    key(cue + "3")));
        }
        Optional<TrophyDefinition> specimen = this.catalogue.specimen();
        long[] tokens = {TrophyStamp.GALLERY_TOKEN, TrophyStamp.GALLERY_BLANK_TOKEN, TrophyStamp.UNSTAMPED};
        String[][] labels = {{"plate.genuine"}, {"plate.hidden.1", "plate.hidden.2"}, {"plate.replica"}};
        for (int i = 0; i < 3; i++) {
            int w = 4 + 2 * i;
            long token = tokens[i];
            this.put(4, w, 1, new Solid(Mat.BASE));
            specimen.ifPresent(def ->
                    this.put(4, w, 2, new GalleryPiece.Trophy(Face.TO_ENTRANCE, new TrophyStamp(def.id(), token))));
            List<GalleryText> lines = new ArrayList<>();
            for (String label : labels[i]) {
                lines.add(key(label));
            }
            this.put(3, w, 1, new Sign(Face.TO_ENTRANCE, false, lines));
        }
        this.put(3, 20, 2, wallSign(Face.LEFT, key("legend.welcome.1"), key("legend.welcome.2"),
                key("legend.welcome.3")));
        this.put(4, 20, 2, wallSign(Face.LEFT, key("legend.tiers.1"), key("legend.tiers.2"), key("legend.tiers.3"),
                key("legend.tiers.4")));
        this.put(5, 20, 2, wallSign(Face.LEFT, key("legend.copies.1"), key("legend.copies.2"),
                key("legend.copies.3"), key("legend.copies.4")));
        for (int w = -20; w <= 20; w += 4) {
            this.put(4, w, 6, new Spotlight(Face.DOWN));
        }
        this.put(7, AISLE_CENTRES[0], 5, wallSign(Face.TO_ENTRANCE, key("aisle.bays"), key("aisle.one")));
        this.put(7, AISLE_CENTRES[1], 5, wallSign(Face.TO_ENTRANCE, key("aisle.bays"), key("aisle.two")));
        this.put(7, AISLE_CENTRES[2], 5, wallSign(Face.TO_ENTRANCE, key("aisle.collections"), key("aisle.finishes"),
                key("aisle.specimens")));
    }

    // ---- E, F, G: aisle 3 (sections 9.2 and 9.4) -----------------------------------------------------------------

    /** Lays out aisle 3 and returns its end wall's {@code l}. */
    private int aisleThree() {
        int centre = AISLE_CENTRES[2];
        int[] cursor = {FIRST_AISLE_L, FIRST_AISLE_L};
        int[] wallW = {centre - 6, centre + 6};
        Face[] toAisle = {Face.RIGHT, Face.LEFT};
        this.put(FIRST_AISLE_L, centre - 3, 3, wallSign(Face.AWAY, key("collections.legend.1"),
                key("collections.legend.2"), key("collections.legend.3"), key("collections.legend.4")));
        List<TrophyCollection> collections = this.catalogue.collections();
        for (int i = 0; i < collections.size(); i++) {
            TrophyCollection collection = collections.get(i);
            int s = i % 2;
            int w = wallW[s];
            Face face = toAisle[s];
            List<Identifier> members = collection.members();
            int shown = Math.min(members.size(), MAX_COLLECTION_CABINETS * 6);
            int width = Math.max(1, (shown + 5) / 6);
            int partial = (shown + 1) / 2;
            int l0 = cursor[s];
            Optional<Identifier> bound = Optional.of(collection.id());
            String trim = Trims.byId(collection.trim()).id();
            for (int j = 0; j < width; j++) {
                int l = l0 + j;
                this.put(l, w, 5, new Cabinet(face, false, bound, trim, Optional.empty(), true,
                        members(members, j, shown)));
                this.put(l, w, 4, new Solid(Mat.BAND));
                this.put(l, w, 3, new Cabinet(face, false, bound, TrophyIds.STANDARD_TRIM, Optional.empty(), true,
                        members(members, j, partial)));
                this.put(l, w, 2, new Solid(Mat.BAND));
                this.put(l, w, 1, new Cabinet(face, false, bound, TrophyIds.STANDARD_TRIM, Optional.empty(), true,
                        List.of()));
            }
            int pillar = l0 + width;
            for (int y = 1; y <= 6; y++) {
                this.put(pillar, w, y, new Solid(Mat.BAND));
            }
            this.put(pillar, w - (s == 0 ? -1 : 1), 3, wallSign(face, GalleryText.of(collection.title()),
                    key("collection.count").withArgs(members.size()),
                    key("collection.trim").withArgs(GalleryText.of("trim.nerotrophies." + trim))));
            cursor[s] = pillar + 1;
        }
        int lf = Math.max(cursor[0], cursor[1]) + 2;
        // F. Finishes on the left wall, trims on the right.
        for (int i = 0; i < FINISHES.size(); i++) {
            Optional<Identifier> finish = FINISHES.get(i);
            this.put(lf + i, wallW[0], 1, new Pedestal(Face.RIGHT, finish, Optional.empty(), false));
            this.put(lf + i, wallW[0], 2, wallSign(Face.RIGHT, finishName(finish)));
        }
        for (int i = 0; i < Trims.ALL.size(); i++) {
            int l = lf + i;
            int cabinetY = i % 2 == 0 ? 1 : 3;
            String trim = Trims.ALL.get(i).id();
            for (int y = 1; y <= 3; y++) {
                this.put(l, wallW[1], y, y == cabinetY
                        ? new Cabinet(Face.LEFT, false, Optional.empty(), trim, Optional.empty(), false, List.of())
                        : new Solid(Mat.BAND));
            }
            this.put(l, wallW[1] - 1, 2, wallSign(Face.LEFT, GalleryText.of("trim.nerotrophies." + trim)));
        }
        // G. Specimens: plaques and joined wall cabinets on the right wall, then the end wall.
        int g0 = lf + Trims.ALL.size();
        Optional<TrophyDefinition> specimen = this.catalogue.specimen();
        long[] tokens = {TrophyStamp.GALLERY_TOKEN, TrophyStamp.GALLERY_BLANK_TOKEN, TrophyStamp.UNSTAMPED};
        for (int i = 0; i < 3; i++) {
            long token = tokens[i];
            int l = g0 + i;
            specimen.ifPresent(def ->
                    this.put(l, wallW[1], 2, new GalleryPiece.Plaque(Face.LEFT, new TrophyStamp(def.id(), token))));
        }
        this.put(g0 + 1, wallW[1], 1, wallSign(Face.LEFT, key("specimen.plaques.1"), key("specimen.plaques.2")));
        List<TrophyDefinition> silver = this.catalogue.some(TrophyTier.SILVER, 6);
        for (int i = 0; i < 3; i++) {
            List<TrophyStamp> pair = new ArrayList<>();
            for (int k = 2 * i; k < 2 * i + 2 && k < silver.size(); k++) {
                pair.add(display(silver.get(k)));
            }
            this.put(g0 + 4 + i, wallW[1], 2, new Cabinet(Face.LEFT, true, Optional.empty(), TrophyIds.STANDARD_TRIM,
                    Optional.empty(), false, pair));
        }
        this.put(g0 + 5, wallW[1], 1, wallSign(Face.LEFT, key("specimen.wall_cabinets")));
        int last = lf + FINISHES.size();
        this.put(last, centre, 1, new Pedestal(Face.TO_ENTRANCE, Optional.empty(), Optional.empty(), true));
        this.put(last, centre, 3, wallSign(Face.TO_ENTRANCE, key("specimen.any_item.1"),
                key("specimen.any_item.2")));
        this.put(last, centre, 4, new Spotlight(Face.TO_ENTRANCE));
        this.put(last, centre, 6, new Spotlight(Face.DOWN));
        String[] emissive = {"void_crystal", "plasma_glass"};
        for (int i = 0; i < 2; i++) {
            int w = centre - 2 + 4 * i;
            Optional<Identifier> finish = Optional.of(Identifier.fromNamespaceAndPath("neroland", emissive[i]));
            this.put(last, w, 1, new Cabinet(Face.TO_ENTRANCE, false, Optional.empty(), TrophyIds.STANDARD_TRIM,
                    finish, false, List.of()));
            this.put(last, w, 2, wallSign(Face.TO_ENTRANCE, finishName(finish)));
        }
        return last + 1;
    }

    private static List<TrophyStamp> members(List<Identifier> members, int cabinet, int count) {
        List<TrophyStamp> out = new ArrayList<>();
        for (int k = cabinet * 6; k < cabinet * 6 + 6 && k < count; k++) {
            out.add(new TrophyStamp(members.get(k), TrophyStamp.GALLERY_TOKEN));
        }
        return out;
    }

    /** The lang key naming a finish ({@code neroland:accent/red} is {@code ...finish.accent.red}). */
    static GalleryText finishName(Optional<Identifier> finish) {
        return key("finish." + finish.map(id -> id.getPath().replace('/', '.')).orElse("default"));
    }
}
