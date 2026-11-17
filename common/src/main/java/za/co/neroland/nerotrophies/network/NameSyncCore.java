package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;

/**
 * The name-sync bookkeeping, free of Minecraft's server so it is testable on a plain JVM. For each viewer it
 * keeps the tokens (and trophy views) that viewer's client currently holds and turns a visible set into upserts and
 * drops. A viewer only ever receives names for stamps in its own visible set: there is no path from a token the client
 * names to a lookup. Never logs. Server thread.
 */
public final class NameSyncCore {

    /** Tokens a client may hold at once. */
    public static final int MAX_SENT = 1024;
    /** Trophy views a client may hold at once. */
    public static final int MAX_VIEWS = 1024;
    /** Upserts per visibility pass (the rest go next pass). */
    public static final int MAX_UPSERTS_PER_PASS = NamesUpdatePayload.MAX_ENTRIES;

    /** Token resolution and definition lookup (the live server, or a test store). */
    public interface Resolver {
        TokenResolver.Resolution resolve(Identifier trophy, long token);

        /** Tier ordinal (-1 unknown) and icon (null unknown) of a trophy id; null if it is not a loaded definition. */
        NamesUpdatePayload.View view(Identifier trophy);
    }

    /** Delivers one payload to one viewer's client. */
    @FunctionalInterface
    public interface Sink {
        void send(UUID viewer, CustomPacketPayload payload);
    }

    private static final Identifier CONFLICT = Identifier.fromNamespaceAndPath("nerotrophies", "conflict");

    private static final class Viewer {
        final Long2ObjectLinkedOpenHashMap<Identifier> sent = new Long2ObjectLinkedOpenHashMap<>();
        final Set<Identifier> views = new LinkedHashSet<>();
    }

    private final Map<UUID, Viewer> viewers = new HashMap<>();

    /**
     * One visibility pass for {@code viewer}: drops what is no longer visible, then upserts what newly is (capped).
     * Reserved tokens are never sent by name; their trophy ids still get a view.
     */
    public void pass(UUID viewer, Collection<TrophyStamp> visible, Resolver resolver, Sink sink) {
        Viewer v = viewers.computeIfAbsent(viewer, k -> new Viewer());
        Long2ObjectLinkedOpenHashMap<Identifier> want = new Long2ObjectLinkedOpenHashMap<>();
        Set<Identifier> wantViews = new LinkedHashSet<>();
        for (TrophyStamp s : visible) {
            if (wantViews.size() < MAX_VIEWS) {
                wantViews.add(s.trophy());
            }
            if (TrophyStamp.isReserved(s.token())) {
                continue;
            }
            Identifier seen = want.get(s.token());
            if (seen == null) {
                want.put(s.token(), s.trophy());
            } else if (!seen.equals(s.trophy())) {
                want.put(s.token(), CONFLICT); // one token on two trophy ids: never named
            }
        }

        LongArrayList dropTokens = new LongArrayList();
        for (Long2ObjectMap.Entry<Identifier> e : v.sent.long2ObjectEntrySet()) {
            if (!want.containsKey(e.getLongKey())) {
                dropTokens.add(e.getLongKey());
            }
        }
        List<Identifier> dropViews = new ArrayList<>();
        for (Identifier id : v.views) {
            if (!wantViews.contains(id)) {
                dropViews.add(id);
            }
        }
        for (int i = 0; i < dropTokens.size(); i++) {
            v.sent.remove(dropTokens.getLong(i));
        }
        dropViews.forEach(v.views::remove);
        if (!dropTokens.isEmpty() || !dropViews.isEmpty()) {
            sink.send(viewer, new NamesDropPayload(dropTokens.toLongArray(), dropViews));
        }

        List<NamesUpdatePayload.Entry> names = new ArrayList<>();
        for (Long2ObjectMap.Entry<Identifier> e : want.long2ObjectEntrySet()) {
            if (names.size() >= MAX_UPSERTS_PER_PASS) {
                break;
            }
            long token = e.getLongKey();
            Identifier trophy = e.getValue();
            Identifier had = v.sent.get(token);
            if (trophy.equals(had)) {
                continue;
            }
            if (had == null && v.sent.size() >= MAX_SENT) {
                continue;
            }
            v.sent.put(token, trophy);
            names.add(entry(token, trophy, resolver));
        }
        List<NamesUpdatePayload.View> views = new ArrayList<>();
        for (Identifier id : wantViews) {
            if (views.size() >= MAX_UPSERTS_PER_PASS) {
                break;
            }
            if (v.views.contains(id)) {
                continue;
            }
            NamesUpdatePayload.View view = resolver.view(id);
            if (view != null) {
                v.views.add(id);
                views.add(view);
            }
        }
        if (!names.isEmpty() || !views.isEmpty()) {
            sink.send(viewer, new NamesUpdatePayload(names, views));
        }
    }

    /**
     * Re-resolves {@code tokens} for every viewer that holds any of them and pushes the result now (erasure,
     * {@code showname}, rename, re-claim, revoke). An erased player's tokens resolve to blank.
     */
    public void invalidate(long[] tokens, Resolver resolver, Sink sink) {
        if (tokens.length == 0) {
            return;
        }
        LongOpenHashSet set = new LongOpenHashSet(tokens);
        for (Map.Entry<UUID, Viewer> ve : viewers.entrySet()) {
            Viewer v = ve.getValue();
            List<NamesUpdatePayload.Entry> names = new ArrayList<>();
            for (Long2ObjectMap.Entry<Identifier> e : v.sent.long2ObjectEntrySet()) {
                if (set.contains(e.getLongKey())) {
                    names.add(entry(e.getLongKey(), e.getValue(), resolver));
                    if (names.size() == NamesUpdatePayload.MAX_ENTRIES) {
                        sink.send(ve.getKey(), new NamesUpdatePayload(names, List.of()));
                        names = new ArrayList<>();
                    }
                }
            }
            if (!names.isEmpty()) {
                sink.send(ve.getKey(), new NamesUpdatePayload(names, List.of()));
            }
        }
    }

    /** Whether {@code viewer}'s client currently holds {@code token}. */
    public boolean holds(UUID viewer, long token) {
        Viewer v = viewers.get(viewer);
        return v != null && v.sent.containsKey(token);
    }

    public int heldCount(UUID viewer) {
        Viewer v = viewers.get(viewer);
        return v == null ? 0 : v.sent.size();
    }

    /** The viewer's client was told to forget everything (dimension change, login). */
    public void reset(UUID viewer) {
        viewers.remove(viewer);
    }

    /** The viewer left. */
    public void remove(UUID viewer) {
        viewers.remove(viewer);
    }

    public void clear() {
        viewers.clear();
    }

    private static NamesUpdatePayload.Entry entry(long token, Identifier trophy, Resolver resolver) {
        if (trophy == CONFLICT) {
            return new NamesUpdatePayload.Entry(token, trophy, NamesUpdatePayload.REPLICA, null);
        }
        TokenResolver.Resolution r = resolver.resolve(trophy, token);
        if (r.status() == TrophyStatus.REPLICA) {
            return new NamesUpdatePayload.Entry(token, trophy, NamesUpdatePayload.REPLICA, null);
        }
        if (r.status() == TrophyStatus.GENUINE && r.name().isPresent()) {
            return new NamesUpdatePayload.Entry(token, trophy, NamesUpdatePayload.NAMED, r.name().get());
        }
        // Blank and "genuine, name hidden" look identical on purpose.
        return new NamesUpdatePayload.Entry(token, trophy, NamesUpdatePayload.BLANK, null);
    }
}
