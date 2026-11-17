package za.co.neroland.nerotrophies.link;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;
import za.co.neroland.nerotrophies.network.RecordBuilder;

/**
 * The read surface of the {@code nerotrophies} link module (schema 1): sections {@code trophies}, {@code collections}
 * and {@code summary}, each built from <b>the requester's own record only</b>. Nothing here can name another player,
 * a token, a position or a gamertag (the record's own stored name is never read). Hidden trophies the requester has
 * not earned are listed as {@code ???} with their book category and nothing else.
 *
 * <p>The builders are pure ({@link #trophies}, {@link #collections}, {@link #summary}); the provider only finds the
 * running server's state through a {@link View}.
 */
public final class TrophyLinkSnapshots implements LinkSnapshotProvider {

    public static final String HIDDEN_TITLE_KEY = "nerotrophies.link.hidden_title";
    public static final String HIDDEN_TITLE = "???";
    public static final int MAX_TROPHIES = 4096;
    public static final int MAX_MEMBERS = 256;

    /** What a snapshot reads: one server's store and definitions. */
    public interface View {
        TrophyState state();

        TrophyDefinitions.Snapshot definitions();

        /** Progress for {@code player}'s unearned trophies (online players only; {@code NONE} otherwise). */
        RecordBuilder.ProgressSource progress(UUID player);

        LinkText text();

        long nowMillis();
    }

    private static final Comparator<TrophyDefinition> ORDER = Comparator
            .comparing(TrophyDefinition::bookCategory)
            .thenComparingInt(TrophyDefinition::order)
            .thenComparing(d -> d.id().toString());

    private final Supplier<View> view;

    public TrophyLinkSnapshots(Supplier<View> view) {
        this.view = view;
    }

    @Override
    public String moduleId() {
        return TrophyLinkModule.MODULE_ID;
    }

    @Override
    public int schemaVersion() {
        return TrophyLinkModule.SCHEMA_VERSION;
    }

    @Override
    public List<String> sections() {
        return TrophyLinkModule.SECTIONS;
    }

    @Override
    public JsonObject snapshot(UUID playerId, String section, Map<String, String> params) {
        if (playerId == null) {
            return error("UNAVAILABLE");
        }
        View v;
        try {
            v = this.view.get();
        } catch (RuntimeException | LinkageError e) {
            v = null;
        }
        if (v == null) {
            return error("UNAVAILABLE");
        }
        TrophyRecord record = v.state().record(playerId).orElse(null);
        TrophyDefinitions.Snapshot defs = v.definitions();
        long now = v.nowMillis();
        if (section == null) {
            return error("UNKNOWN_SECTION");
        }
        return switch (section) {
            case TrophyLinkModule.SECTION_TROPHIES -> trophies(record, defs, v.text(), v.progress(playerId), now);
            case TrophyLinkModule.SECTION_COLLECTIONS -> collections(record, defs, v.text(), now);
            case TrophyLinkModule.SECTION_SUMMARY -> summary(record, defs, now);
            default -> error("UNKNOWN_SECTION");
        };
    }

    // ---- trophies -------------------------------------------------------------------------------------------------

    /** Section {@code trophies}: every loaded definition as seen by {@code record}'s owner (null = nothing earned). */
    public static JsonObject trophies(@Nullable TrophyRecord record, TrophyDefinitions.Snapshot defs, LinkText text,
            RecordBuilder.ProgressSource progress, long now) {
        List<TrophyDefinition> sorted = new ArrayList<>(defs.trophies().values());
        sorted.sort(ORDER);
        JsonArray shown = new JsonArray();
        List<TrophyDefinition> masked = new ArrayList<>();
        for (TrophyDefinition d : sorted) {
            Optional<TrophyRecord.Earned> earned = earned(record, d.id());
            if (earned.isEmpty() && d.hidden()) {
                masked.add(d);
                continue;
            }
            if (shown.size() >= MAX_TROPHIES) {
                break;
            }
            shown.add(trophy(d, earned, text, progress));
        }
        int n = 0;
        for (TrophyDefinition d : masked) {
            if (shown.size() >= MAX_TROPHIES) {
                break;
            }
            JsonObject o = new JsonObject();
            o.addProperty("key", TrophyLinkKeys.HIDDEN_PREFIX + n++);
            o.addProperty("mod", d.bookCategory());
            o.addProperty("hidden", true);
            o.addProperty("earned", false);
            o.addProperty("title", text.resolve(HIDDEN_TITLE_KEY, HIDDEN_TITLE));
            shown.add(o);
        }
        JsonObject out = new JsonObject();
        out.addProperty("asOf", now);
        out.add("trophies", shown);
        return out;
    }

    private static JsonObject trophy(TrophyDefinition d, Optional<TrophyRecord.Earned> earned, LinkText text,
            RecordBuilder.ProgressSource progress) {
        JsonObject o = new JsonObject();
        o.addProperty("key", TrophyLinkKeys.trophy(d.id()));
        o.addProperty("id", d.id().toString());
        o.addProperty("mod", d.bookCategory());
        o.addProperty("tier", d.tier().getSerializedName());
        o.addProperty("tierRank", d.tier().rank());
        o.addProperty("icon", d.icon().toString());
        o.addProperty("titleKey", d.title());
        o.addProperty("title", title(d, text));
        o.addProperty("descKey", d.description());
        o.addProperty("description", text.resolve(d.description(), ""));
        o.addProperty("hidden", d.hidden());
        o.addProperty("earned", earned.isPresent());
        earned.ifPresent(e -> o.addProperty("at", e.at()));
        o.addProperty("order", d.order());
        if (earned.isEmpty()) {
            try {
                Optional<TriggerEvaluator.Progress> p = progress.progress(d);
                if (p.isPresent()) {
                    JsonObject bar = new JsonObject();
                    bar.addProperty("current", Math.min(Math.max(0, p.get().current()), p.get().target()));
                    bar.addProperty("target", p.get().target());
                    o.add("progress", bar);
                }
            } catch (RuntimeException e) {
                // progress is cosmetic: no bar
            }
        }
        return o;
    }

    static String title(TrophyDefinition d, LinkText text) {
        return text.resolve(d.title(), LinkText.humanise(d.id()));
    }

    static String title(TrophyCollection c, LinkText text) {
        return text.resolve(c.title(), LinkText.humanise(c.id()));
    }

    // ---- collections ----------------------------------------------------------------------------------------------

    /** Section {@code collections}: every loaded collection, sorted by id, with the requester's progress. */
    public static JsonObject collections(@Nullable TrophyRecord record, TrophyDefinitions.Snapshot defs, LinkText text,
            long now) {
        List<TrophyCollection> sorted = new ArrayList<>(defs.collections().values());
        sorted.sort(Comparator.comparing(c -> c.id().toString()));
        JsonArray list = new JsonArray();
        for (TrophyCollection c : sorted) {
            JsonObject o = new JsonObject();
            o.addProperty("key", TrophyLinkKeys.collection(c.id()));
            o.addProperty("id", c.id().toString());
            o.addProperty("titleKey", c.title());
            o.addProperty("title", title(c, text));
            o.addProperty("descKey", c.description());
            o.addProperty("description", text.resolve(c.description(), ""));
            o.addProperty("icon", c.icon().toString());
            o.addProperty("trim", c.trim());
            o.addProperty("completed", record != null && record.completedCollections().contains(c.id()));
            JsonArray members = new JsonArray();
            int earnedCount = 0;
            for (Identifier member : c.members()) {
                boolean has = record != null && record.has(member);
                if (has) {
                    earnedCount++;
                }
                if (members.size() >= MAX_MEMBERS) {
                    continue;
                }
                JsonObject m = new JsonObject();
                TrophyDefinition def = defs.trophies().get(member);
                if (!has && (def == null || def.hidden())) {
                    m.addProperty("key", TrophyLinkKeys.HIDDEN_PREFIX + "m");
                    m.addProperty("hidden", true);
                } else {
                    m.addProperty("key", TrophyLinkKeys.trophy(member));
                }
                m.addProperty("earned", has);
                members.add(m);
            }
            o.addProperty("earnedCount", earnedCount);
            o.addProperty("memberCount", c.members().size());
            o.add("members", members);
            list.add(o);
        }
        JsonObject out = new JsonObject();
        out.addProperty("asOf", now);
        out.add("collections", list);
        return out;
    }

    // ---- summary --------------------------------------------------------------------------------------------------

    /** Section {@code summary}: counts per tier and per book category, collection counts, last award time. */
    public static JsonObject summary(@Nullable TrophyRecord record, TrophyDefinitions.Snapshot defs, long now) {
        Map<TrophyTier, int[]> tiers = emptyTiers();
        Map<String, Map<TrophyTier, int[]>> mods = new TreeMap<>();
        int total = 0;
        int earnedTotal = 0;
        long last = 0L;
        for (TrophyDefinition d : defs.trophies().values()) {
            Optional<TrophyRecord.Earned> earned = earned(record, d.id());
            Map<TrophyTier, int[]> mod = mods.computeIfAbsent(d.bookCategory(), k -> emptyTiers());
            total++;
            tiers.get(d.tier())[1]++;
            mod.get(d.tier())[1]++;
            if (earned.isPresent()) {
                earnedTotal++;
                tiers.get(d.tier())[0]++;
                mod.get(d.tier())[0]++;
                last = Math.max(last, earned.get().at());
            }
        }
        JsonObject out = new JsonObject();
        out.addProperty("asOf", now);
        out.addProperty("total", total);
        out.addProperty("earned", earnedTotal);
        out.add("tiers", tiers(tiers));
        JsonArray modList = new JsonArray();
        mods.forEach((name, counts) -> {
            JsonObject m = new JsonObject();
            m.addProperty("mod", name);
            int e = 0;
            int t = 0;
            for (int[] c : counts.values()) {
                e += c[0];
                t += c[1];
            }
            m.addProperty("earned", e);
            m.addProperty("total", t);
            m.add("tiers", tiers(counts));
            modList.add(m);
        });
        out.add("mods", modList);
        JsonObject cols = new JsonObject();
        int completed = 0;
        if (record != null) {
            for (Identifier id : record.completedCollections()) {
                if (defs.collections().containsKey(id)) {
                    completed++;
                }
            }
        }
        cols.addProperty("completed", completed);
        cols.addProperty("total", defs.collections().size());
        out.add("collections", cols);
        if (last > 0L) {
            out.addProperty("lastEarnedAt", last);
        }
        return out;
    }

    private static Map<TrophyTier, int[]> emptyTiers() {
        Map<TrophyTier, int[]> m = new EnumMap<>(TrophyTier.class);
        for (TrophyTier t : TrophyTier.values()) {
            m.put(t, new int[2]);
        }
        return m;
    }

    private static JsonObject tiers(Map<TrophyTier, int[]> counts) {
        JsonObject o = new JsonObject();
        counts.forEach((tier, c) -> {
            JsonObject t = new JsonObject();
            t.addProperty("earned", c[0]);
            t.addProperty("total", c[1]);
            o.add(tier.getSerializedName(), t);
        });
        return o;
    }

    private static Optional<TrophyRecord.Earned> earned(@Nullable TrophyRecord record, Identifier id) {
        return record == null ? Optional.empty() : record.earned(id);
    }

    static JsonObject error(String code) {
        JsonObject o = new JsonObject();
        o.addProperty("error", code);
        return o;
    }
}
