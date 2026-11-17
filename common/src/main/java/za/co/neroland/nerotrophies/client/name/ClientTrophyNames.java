package za.co.neroland.nerotrophies.client.name;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.network.NamesDropPayload;
import za.co.neroland.nerotrophies.network.NamesUpdatePayload;

/**
 * The client's name cache (decision 009): <b>memory only</b>, never written to disk, never logged. Filled only by the
 * server's {@code names_update} for trophies this client can currently see; emptied entry-wise by {@code names_drop},
 * wholly by {@code names_reset} and on disconnect. Client thread only.
 *
 * <p>Read API for renderers and tooltips: {@link #nameFor}, {@link #viewFor}, {@link #viewForTrophy} and the change
 * listener. The {@code apply*} / {@link #clear} methods are for the network package only.
 */
public final class ClientTrophyNames {

    private record Held(Identifier trophy, NameState state, String name) {
    }

    private record Look(TrophyTier tier, Identifier icon) {
    }

    private static final Long2ObjectOpenHashMap<Held> NAMES = new Long2ObjectOpenHashMap<>();
    private static final Map<Identifier, Look> VIEWS = new HashMap<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static Supplier<Object> sessionProbe;
    private static WeakReference<Object> session = new WeakReference<>(null);

    private ClientTrophyNames() {
    }

    // ---- read API ------------------------------------------------------------------------------------------------

    /** The earner's gamertag, only for a token the server resolved as NAMED; null otherwise. */
    public static String nameFor(long token) {
        checkSession();
        if (TrophyStamp.isReserved(token)) {
            return null;
        }
        Held h = NAMES.get(token);
        return h != null && h.state() == NameState.NAMED ? h.name() : null;
    }

    /** What the server said about this stamped token; null = not (yet) resolved (draw the blank ornament). */
    public static TrophyView viewFor(long token) {
        checkSession();
        if (TrophyStamp.isReserved(token)) {
            return null;
        }
        Held h = NAMES.get(token);
        if (h == null) {
            return null;
        }
        Look look = VIEWS.get(h.trophy());
        return new TrophyView(h.state(), h.state() == NameState.NAMED ? h.name() : null,
                look == null ? null : look.tier(), look == null ? null : look.icon());
    }

    /** Tier and icon for a visible trophy id (for reserved tokens, which are never sent); null if not yet known. */
    public static TrophyView viewForTrophy(Identifier trophy) {
        checkSession();
        Look look = VIEWS.get(trophy);
        return look == null ? null : new TrophyView(NameState.BLANK, null, look.tier(), look.icon());
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    // ---- network package only --------------------------------------------------------------------------------------

    public static void apply(NamesUpdatePayload update) {
        for (NamesUpdatePayload.Entry e : update.names()) {
            if (TrophyStamp.isReserved(e.token())) {
                continue;
            }
            NameState state = switch (e.status()) {
                case NamesUpdatePayload.NAMED -> NameState.NAMED;
                case NamesUpdatePayload.REPLICA -> NameState.REPLICA;
                default -> NameState.BLANK;
            };
            NAMES.put(e.token(), new Held(e.trophy(), state, state == NameState.NAMED ? e.name() : null));
        }
        for (NamesUpdatePayload.View v : update.views()) {
            TrophyTier[] tiers = TrophyTier.values();
            TrophyTier tier = v.tier() >= 0 && v.tier() < tiers.length ? tiers[v.tier()] : null;
            VIEWS.put(v.trophy(), new Look(tier, v.icon()));
        }
        changed();
    }

    public static void apply(NamesDropPayload drop) {
        for (long t : drop.tokens()) {
            NAMES.remove(t);
        }
        drop.trophies().forEach(VIEWS::remove);
        changed();
    }

    /** Forget everything (reset, disconnect, a new session). */
    public static void clear() {
        boolean had = !NAMES.isEmpty() || !VIEWS.isEmpty();
        NAMES.clear();
        NAMES.trim();
        VIEWS.clear();
        if (had) {
            changed();
        }
    }

    /**
     * Ties the cache to the current connection: a different (or no) connection object than the one the cache was
     * filled under clears it, so names never survive a disconnect even if a loader's disconnect event is missed.
     */
    public static void bindSession(Object current, Supplier<Object> probe) {
        sessionProbe = probe;
        if (session.get() != current) {
            clear();
            session = new WeakReference<>(current);
        }
    }

    public static int size() {
        return NAMES.size();
    }

    private static void checkSession() {
        Supplier<Object> probe = sessionProbe;
        if (probe == null) {
            return;
        }
        Object current = probe.get();
        if (current == null || current != session.get()) {
            clear();
            session = new WeakReference<>(current);
        }
    }

    private static void changed() {
        for (Runnable r : LISTENERS) {
            r.run();
        }
    }
}
