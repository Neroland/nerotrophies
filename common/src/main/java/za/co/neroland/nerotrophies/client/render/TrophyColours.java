package za.co.neroland.nerotrophies.client.render;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * NeroTrophies' client palette (ARGB); the texture tools use the same values byte for byte. Tier colours are never the
 * only tier signal: pips, silhouettes and labels always carry it too. Client only.
 */
public final class TrophyColours {

    public static final int G_DARK = 0xFF1A1D22;
    public static final int GRAPHITE = 0xFF2C3137;
    public static final int STEEL = 0xFF7A8492;
    public static final int STEEL_DK = 0xFF424A56;
    public static final int GLASS = 0xFF0B1119;
    public static final int PANEL = 0xFF141C26;
    public static final int PANEL_EDGE = 0xFF2A3A4D;
    public static final int TRACK = 0xFF1E2A3A;
    public static final int TEXT = 0xFFE8ECF4;
    public static final int TEXT_2 = 0xFF97A6BC;
    public static final int TEXT_FAINT = 0xFF5C6B82;
    public static final int ACCENT = 0xFFA77BFF;
    public static final int ACCENT_BRIGHT = 0xFFD3BEFF;
    public static final int ACCENT_DIM = 0xFF4A3878;
    public static final int VELVET = 0xFF1B1428;
    public static final int PRISM_A = 0xFF2FE6C8;
    public static final int PRISM_MID = 0xFF60D4E8;
    public static final int PRISM_B = 0xFF9B6BFF;
    public static final int REPLICA = 0xFF5E646C;
    public static final int REPLICA_DK = 0xFF3F444B;
    public static final int REPLICA_TEXT = 0xFF8A919A;
    public static final int GHOST_UNKNOWN = 0xFF97A6BC;
    /** The finish colour of a display block with none set: lands the tint layer on graphite. */
    public static final int DEFAULT_FINISH = 0xFF4A525C;

    private TrophyColours() {
    }

    /** The tier's {@code BASE} colour (frames, ghosts, bars). Nero: its first prism colour. */
    public static int base(@Nullable TrophyTier tier) {
        if (tier == null) {
            return GHOST_UNKNOWN;
        }
        return switch (tier) {
            case BRONZE -> 0xFFA5673A;
            case SILVER -> 0xFF9AA7B6;
            case GOLD -> 0xFFD9A520;
            case NERO -> PRISM_A;
        };
    }

    /** The tier's {@code DEEP} colour (filter chip fill). */
    public static int deep(@Nullable TrophyTier tier) {
        if (tier == null) {
            return PANEL;
        }
        return switch (tier) {
            case BRONZE -> 0xFF3A2218;
            case SILVER -> 0xFF2E3640;
            case GOLD -> 0xFF4A3208;
            case NERO -> 0xFF0B0E13;
        };
    }

    /** The tier's {@code INK}: nameplate text, titles. */
    public static int ink(@Nullable TrophyTier tier) {
        if (tier == null) {
            return TEXT;
        }
        return switch (tier) {
            case BRONZE -> 0xFFF0C9A4;
            case SILVER -> 0xFFE8ECF4;
            case GOLD -> 0xFFFFE9A0;
            case NERO -> 0xFFA8F0EA;
        };
    }

    /** The tier's {@code GLOW}: the lit pedestal ring. Nero: its first prism colour. */
    public static int glow(@Nullable TrophyTier tier) {
        if (tier == null) {
            return STEEL_DK;
        }
        return switch (tier) {
            case BRONZE -> 0xFFE8915A;
            case SILVER -> 0xFFCFE2F5;
            case GOLD -> 0xFFFFDF6B;
            case NERO -> PRISM_A;
        };
    }

    /** The tier's slot highlight in this mod's own screens. */
    public static int hilite(@Nullable TrophyTier tier) {
        if (tier == null) {
            return 0;
        }
        return switch (tier) {
            case BRONZE -> 0xFFD9824A;
            case SILVER -> 0xFFC9D3DF;
            case GOLD -> 0xFFFFDF3A;
            case NERO -> PRISM_A;
        };
    }

    /** {@code argb} with its RGB scaled by {@code factor} (the blank ornament is INK at 55 %). */
    public static int scale(int argb, float factor) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** {@code argb} with alpha {@code alpha} (0..1). */
    public static int alpha(int argb, float alpha) {
        return ((int) (Math.max(0F, Math.min(1F, alpha)) * 255F) << 24) | (argb & 0xFFFFFF);
    }
}
