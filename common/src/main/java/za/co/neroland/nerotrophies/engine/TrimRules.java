package za.co.neroland.nerotrophies.engine;

import java.util.UUID;
import java.util.regex.Pattern;

import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Who may put which trim on a cabinet. The cabinet screen calls {@link #canApply} on the server for
 * every trim change; the trim is a cosmetic string on the block entity, never an identity.
 */
public final class TrimRules {

    private static final Pattern TRIM = Pattern.compile("[a-z0-9_]{1,32}");

    private TrimRules() {
    }

    public static boolean isValid(String trim) {
        return trim != null && TRIM.matcher(trim).matches();
    }

    /**
     * Whether {@code clicker} may set {@code trim} on a cabinet currently showing {@code current}: always for
     * {@code standard} and for the trim already applied (a viewer may step away, never back), otherwise only if the
     * clicker's record holds {@code nerotrophies:trim/<trim>}.
     */
    public static boolean canApply(TrophyState state, UUID clicker, String trim, String current) {
        if (!isValid(trim)) {
            return false;
        }
        if (TrophyIds.STANDARD_TRIM.equals(trim) || trim.equals(current)) {
            return true;
        }
        return state.record(clicker).map(r -> r.has(TrophyIds.trimId(trim))).orElse(false);
    }
}
