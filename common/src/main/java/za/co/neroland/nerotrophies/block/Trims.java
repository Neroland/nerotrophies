package za.co.neroland.nerotrophies.block;

import java.util.List;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The twelve cabinet trims: a trim id, the frame style it draws and its accent colour (tint index
 * 2). The order is the order the cabinet screen steps through. An unknown id (a pack removed it) draws as
 * {@code standard} and is never rewritten.
 */
public final class Trims {

    /** One trim: id, frame style, accent RGB (0 for none; {@code prism} animates and ignores it). */
    public record Trim(String id, TrimStyle style, int accent) {
    }

    public static final List<Trim> ALL = List.of(
            new Trim(TrophyIds.STANDARD_TRIM, TrimStyle.PLATE, 0x5B6670),
            new Trim("orbital_plasma", TrimStyle.ORBITAL, 0x60D4E8),
            new Trim("orbital_amber", TrimStyle.ORBITAL, 0xFFB347),
            new Trim("laurel_gold", TrimStyle.LAUREL, 0xFFD54A),
            new Trim("laurel_crimson", TrimStyle.LAUREL, 0xD6424A),
            new Trim("laurel_copper", TrimStyle.LAUREL, 0xE48C42),
            new Trim("wild_green", TrimStyle.WILD, 0x78D678),
            new Trim("wild_sand", TrimStyle.WILD, 0xC89858),
            new Trim("wild_teal", TrimStyle.WILD, 0x56CCC4),
            new Trim("neon_pink", TrimStyle.NEON, 0xFF3FA4),
            new Trim("neon_rose", TrimStyle.NEON, 0xE8788E),
            new Trim("prism", TrimStyle.PRISM, 0x9B6BFF));

    private Trims() {
    }

    /** The trim with this id, else {@code standard}. */
    public static Trim byId(String id) {
        for (Trim trim : ALL) {
            if (trim.id().equals(id)) {
                return trim;
            }
        }
        return ALL.get(0);
    }

    /** Index of {@code id} in {@link #ALL}; 0 ({@code standard}) for an unknown id. */
    public static int indexOf(String id) {
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(id)) {
                return i;
            }
        }
        return 0;
    }
}
