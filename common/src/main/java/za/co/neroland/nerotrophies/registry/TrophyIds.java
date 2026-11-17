package za.co.neroland.nerotrophies.registry;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * Every registry path NeroTrophies owns, in one place. Later work packages
 * register behaviour under these ids; they never invent new ones without adding them here.
 */
public final class TrophyIds {

    // Blocks (block id budget: six, owner-confirmed 2026-11-17) and their items.
    public static final String TROPHY = "trophy";
    public static final String DISPLAY_PEDESTAL = "display_pedestal";
    public static final String TROPHY_CABINET = "trophy_cabinet";
    public static final String WALL_CABINET = "wall_cabinet";
    public static final String TROPHY_PLAQUE = "trophy_plaque";
    public static final String TROPHY_SPOTLIGHT = "trophy_spotlight";
    // Items without a block.
    public static final String TROPHY_BOOK = "trophy_book";

    // Data components.
    /** {@link za.co.neroland.nerotrophies.component.TrophyStamp}: trophy id + claim token. */
    public static final String COMPONENT_TROPHY = "trophy";
    /** A display block's Core palette finish id. */
    public static final String COMPONENT_FINISH = "finish";
    /** The Trophy Book's selected collection id. */
    public static final String COMPONENT_BOOK_SELECTION = "book_selection";

    // Payload ids on the nerotrophies:main channel. Registered by the network package.
    public static final String PAYLOAD_NAMES_UPDATE = "names_update";
    public static final String PAYLOAD_NAMES_DROP = "names_drop";
    public static final String PAYLOAD_NAMES_RESET = "names_reset";
    public static final String PAYLOAD_RECORD = "record";
    public static final String PAYLOAD_RECORD_DELTA = "record_delta";
    public static final String PAYLOAD_AWARD_TOAST = "award_toast";
    public static final String PAYLOAD_CLAIM = "claim";
    public static final String PAYLOAD_SET_TOGGLE = "set_toggle";
    public static final String PAYLOAD_BOOK_REFRESH = "book_refresh";
    public static final String PAYLOAD_SELECT_COLLECTION = "select_collection";

    /** The prefix of an earned cabinet-trim id: {@code nerotrophies:trim/<trim>}. */
    public static final String TRIM_PREFIX = "trim/";
    /** The trim every cabinet has without earning anything. */
    public static final String STANDARD_TRIM = "standard";

    private TrophyIds() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroTrophiesCommon.MOD_ID, path);
    }

    /** The earned id that records an unlocked trim. */
    public static Identifier trimId(String trim) {
        return id(TRIM_PREFIX + trim);
    }

    /** Whether {@code id} is an earned-trim id rather than a trophy. */
    public static boolean isTrimId(Identifier id) {
        return NeroTrophiesCommon.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith(TRIM_PREFIX);
    }
}
