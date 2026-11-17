package za.co.neroland.nerotrophies.link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;

/**
 * The live surface of the {@code nerotrophies} link module: {@code TROPHY_EARNED} and {@code COLLECTION_COMPLETED},
 * topic {@value #TOPIC}, each {@link LinkEvent#forPlayer player-scoped} to the earner. The bridge turns a player-scoped
 * event into a push in category {@code nerotrophies} (off until the player switches it on) when the player is not
 * watching live, with copy from {@value #NOTIFY}.
 *
 * <p><b>What is sent.</b> Only {@link AwardSource#LIVE} and {@link AwardSource#API} awards: a retroactive catch-up at
 * login or reload, or an operator grant, is not news. At most {@value #MAX_PER_WINDOW} events per player per
 * {@value #WINDOW_MS} ms; the rest are dropped (the snapshot still shows them). The body is the trophy or collection
 * title and nothing else: never a player name, UUID, token, position or another player's anything.
 *
 * <p><b>Collection source.</b> Core's {@code CollectionCompleted} carries no award source. The engine fires it from
 * inside its own {@code Awarded} listener, which runs before this module's (the link module registers last), so a
 * completion is parked and released or dropped by the {@code Awarded} that caused it (same player, same tick). One
 * that no award follows (the end-of-pass retroactive sweep) is dropped.
 *
 * <p>No Core alert is raised: an alert is itself a generic push. Rate-limit keys are in memory only and forgotten on
 * erasure and server stop.
 */
public final class TrophyLinkEvents {

    public static final String TOPIC = "events";
    public static final String NOTIFY = "_notify";
    public static final String TYPE_TROPHY = "TROPHY_EARNED";
    public static final String TYPE_COLLECTION = "COLLECTION_COMPLETED";
    public static final long WINDOW_MS = 60_000L;
    public static final int MAX_PER_WINDOW = 5;
    public static final int TITLE_MAX = 120;
    public static final int BODY_MAX = 240;
    public static final int LINK_MAX = 256;
    public static final String SERVER_PLACEHOLDER = "{server}";
    public static final Pattern DEEP_LINK = Pattern.compile(
            "^nerolink://server/[A-Za-z0-9_-]{1,64}/[a-z0-9_-]{1,32}(/[A-Za-z0-9_-]{1,64}){0,2}$");
    public static final String KEY_TROPHY_TITLE = "nerotrophies.link.notify.trophy_title";
    public static final String KEY_COLLECTION_TITLE = "nerotrophies.link.notify.collection_title";
    static final int MAX_PLAYERS = 8192;
    static final int MAX_PENDING = 256;

    /** What the pipeline talks to: the live server, or a test. */
    public interface Env {
        long nowMillis();

        /** A counter that moves once per server tick. */
        long tick();

        TrophyDefinitions.Snapshot definitions();

        LinkText text();

        void publish(LinkEvent event);
    }

    private record Pending(UUID player, Identifier collection, long tick) {
    }

    private final LinkedHashMap<UUID, Deque<Long>> sent = new LinkedHashMap<>();
    private final List<Pending> pending = new ArrayList<>();

    /** {@code Awarded}: publishes the trophy, then releases (or drops) the completions it caused. */
    public synchronized void onAwarded(@Nullable Env env, TrophyEvents.Awarded event) {
        if (env == null || event == null || event.player() == null) {
            return;
        }
        long tick = env.tick();
        boolean news = event.source() == AwardSource.LIVE || event.source() == AwardSource.API;
        TrophyDefinitions.Snapshot defs = env.definitions();
        if (news) {
            TrophyDefinition d = defs.trophies().get(event.trophy());
            if (d != null) {
                send(env, event.player(), trophyPayload(d, env.text(), env.nowMillis()));
            }
        }
        Iterator<Pending> it = this.pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.tick() != tick) {
                it.remove(); // stale: no award claimed it
                continue;
            }
            if (!p.player().equals(event.player())) {
                continue;
            }
            it.remove();
            TrophyCollection c = defs.collections().get(p.collection());
            if (news && c != null) {
                send(env, p.player(), collectionPayload(c, env.text(), env.nowMillis()));
            }
        }
    }

    /** {@code CollectionCompleted}: parked until the {@code Awarded} that caused it decides. */
    public synchronized void onCollectionCompleted(@Nullable Env env, TrophyEvents.CollectionCompleted event) {
        if (env == null || event == null || event.player() == null || event.collection() == null) {
            return;
        }
        long tick = env.tick();
        this.pending.removeIf(p -> p.tick() != tick);
        if (this.pending.size() < MAX_PENDING) {
            this.pending.add(new Pending(event.player(), event.collection(), tick));
        }
    }

    private void send(Env env, UUID player, JsonObject payload) {
        if (!admit(player, env.nowMillis())) {
            return;
        }
        try {
            env.publish(LinkEvent.forPlayer(TrophyLinkModule.MODULE_ID, TOPIC, player, payload));
        } catch (RuntimeException | LinkageError e) {
            // a lost notification never breaks an award
        }
    }

    private boolean admit(UUID player, long now) {
        Deque<Long> times = this.sent.get(player);
        if (times == null) {
            if (this.sent.size() >= MAX_PLAYERS) {
                Iterator<UUID> oldest = this.sent.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            times = new ArrayDeque<>();
            this.sent.put(player, times);
        }
        while (!times.isEmpty() && (now - times.peekFirst() >= WINDOW_MS || now < times.peekFirst())) {
            times.pollFirst();
        }
        if (times.size() >= MAX_PER_WINDOW) {
            return false;
        }
        times.addLast(now);
        return true;
    }

    /** Erasure: forget every in-memory key of {@code player}. */
    public synchronized void forget(UUID player) {
        this.sent.remove(player);
        this.pending.removeIf(p -> p.player().equals(player));
    }

    public synchronized boolean holdsAnyFor(UUID player) {
        return this.sent.containsKey(player) || this.pending.stream().anyMatch(p -> p.player().equals(player));
    }

    public synchronized void clear() {
        this.sent.clear();
        this.pending.clear();
    }

    // ---- payloads (the only builders of notification text) --------------------------------------------------------

    static JsonObject trophyPayload(TrophyDefinition d, LinkText text, long now) {
        String key = TrophyLinkKeys.trophy(d.id());
        String title = TrophyLinkSnapshots.title(d, text);
        JsonObject o = new JsonObject();
        o.addProperty("type", TYPE_TROPHY);
        o.addProperty("at", now);
        o.addProperty("key", key);
        o.addProperty("mod", d.bookCategory());
        o.addProperty("tier", d.tier().getSerializedName());
        o.addProperty("titleKey", d.title());
        o.addProperty("title", title);
        o.add(NOTIFY, notify(text.resolve(KEY_TROPHY_TITLE, "Trophy earned"), title, key));
        return o;
    }

    static JsonObject collectionPayload(TrophyCollection c, LinkText text, long now) {
        String key = TrophyLinkKeys.collection(c.id());
        String title = TrophyLinkSnapshots.title(c, text);
        JsonObject o = new JsonObject();
        o.addProperty("type", TYPE_COLLECTION);
        o.addProperty("at", now);
        o.addProperty("key", key);
        o.addProperty("trim", c.trim());
        o.addProperty("titleKey", c.title());
        o.addProperty("title", title);
        o.add(NOTIFY, notify(text.resolve(KEY_COLLECTION_TITLE, "Collection complete"), title, key));
        return o;
    }

    static JsonObject notify(String title, String body, String key) {
        JsonObject n = new JsonObject();
        n.addProperty("title", cap(title, TITLE_MAX));
        n.addProperty("body", cap(body, BODY_MAX));
        String link = deepLink(key);
        if (link != null) {
            n.addProperty("deepLink", link);
        }
        return n;
    }

    /** {@code nerolink://server/{server}/nerotrophies/<key>}, or null if it would not pass the bridge. */
    @Nullable
    static String deepLink(String key) {
        if (key == null || !TrophyLinkKeys.KEY.matcher(key).matches()) {
            return null;
        }
        String link = "nerolink://server/" + SERVER_PLACEHOLDER + "/" + TrophyLinkModule.MODULE_ID + "/" + key;
        String probe = link.replace(SERVER_PLACEHOLDER, "x");
        return link.length() <= LINK_MAX && DEEP_LINK.matcher(probe).matches() ? link : null;
    }

    /** Control and format characters removed, trimmed, cut to {@code max} without splitting a surrogate pair. */
    static String cap(String text, int max) {
        StringBuilder out = new StringBuilder();
        (text == null ? "" : text).codePoints().filter(cp -> {
            int type = Character.getType(cp);
            return type != Character.CONTROL && type != Character.FORMAT && type != Character.LINE_SEPARATOR
                    && type != Character.PARAGRAPH_SEPARATOR && cp != 0x00A7;
        }).forEach(out::appendCodePoint);
        String clean = out.toString().trim();
        if (clean.length() <= max) {
            return clean;
        }
        int end = max;
        if (Character.isHighSurrogate(clean.charAt(end - 1))) {
            end--;
        }
        return clean.substring(0, end).trim();
    }
}
