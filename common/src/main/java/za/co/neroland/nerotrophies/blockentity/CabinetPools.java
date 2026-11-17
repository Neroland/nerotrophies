package za.co.neroland.nerotrophies.blockentity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.engine.TrophyStatus;

/**
 * Collection completion for a cabinet pool (decision 003): which members of the bound collection the pool holds as
 * <b>genuine</b> trophies. A replica, a blank (erased / forged / unknown token) or an unstamped copy never counts;
 * each token counts once across the pool (a gallery copy once per trophy); gallery copies count only in a {@code display_only} gallery cabinet; the
 * gallery's nameless copy ({@code -2}) never counts. Pure: the caller supplies the resolver (the server's token check).
 */
public final class CabinetPools {

    /** Most members a pool tracks in its bitmask (decision 010: catalogue collections are capped at 24). */
    public static final int MAX_MEMBERS = 63;
    /** Most cabinets in one pool (003). */
    public static final int MAX_POOL = 4;

    private CabinetPools() {
    }

    /**
     * Bit {@code i} is set when member {@code i} of {@code members} is present genuinely in {@code stamps}. Only the
     * first {@link #MAX_MEMBERS} members are tracked.
     */
    public static long memberMask(List<Identifier> members, Iterable<TrophyStamp> stamps,
            Function<TrophyStamp, TrophyStatus> resolver, boolean displayOnly) {
        long mask = 0L;
        Set<Object> seen = new HashSet<>();
        for (TrophyStamp stamp : stamps) {
            // Gallery display copies all share one reserved token, so they count once per trophy instead.
            if (stamp == null) {
                continue;
            }
            Object key = stamp.token() == TrophyStamp.GALLERY_TOKEN ? stamp.trophy() : (Object) stamp.token();
            if (!seen.add(key)) {
                continue;
            }
            if (!counts(stamp, resolver, displayOnly)) {
                continue;
            }
            int index = members.indexOf(stamp.trophy());
            if (index >= 0 && index < MAX_MEMBERS) {
                mask |= 1L << index;
            }
        }
        return mask;
    }

    /**
     * Genuine members only, whatever cabinet holds them: never a display copy. The cabinet's {@link #memberMask} is
     * display state (pips, completion glow, screen count) and awards nothing; completion is read from the record.
     */
    public static long genuineMask(List<Identifier> members, Iterable<TrophyStamp> stamps,
            Function<TrophyStamp, TrophyStatus> resolver) {
        return memberMask(members, stamps, resolver, false);
    }

    /** Whether one stamp may count toward a collection. */
    public static boolean counts(TrophyStamp stamp, Function<TrophyStamp, TrophyStatus> resolver,
            boolean displayOnly) {
        if (stamp.token() == TrophyStamp.GALLERY_TOKEN) {
            return displayOnly;
        }
        if (TrophyStamp.isReserved(stamp.token())) {
            return false;
        }
        return resolver.apply(stamp) == TrophyStatus.GENUINE;
    }
}
