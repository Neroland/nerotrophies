package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.data.TrophyRecord;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TriggerEvaluator;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Builds the {@code record} payload for <b>one</b> player from that player's record only. Pure: no server, no player object. Stat progress is computed here at build time
 * and never stored.
 */
public final class RecordBuilder {

    /** Live progress toward one unearned, non-hidden definition (the trigger evaluator, or nothing in tests). */
    @FunctionalInterface
    public interface ProgressSource {
        ProgressSource NONE = d -> Optional.empty();

        Optional<TriggerEvaluator.Progress> progress(TrophyDefinition definition);
    }

    /**
     * Worst-case wire budget of one {@code record} payload, well under the 1 MiB a clientbound custom payload may carry.
     * A pack at Core's definition limits gets a truncated Book instead of a multi-megabyte packet.
     */
    public static final int RECORD_BUDGET_BYTES = 768 * 1024;
    /** Worst-case budget of the collection list (it is also sent with every {@code record_delta}). */
    public static final int COLLECTIONS_BUDGET_BYTES = 192 * 1024;

    private static final Comparator<TrophyDefinition> ORDER = Comparator.comparing(d -> d.id().toString());

    private RecordBuilder() {
    }

    public static RecordPayload build(TrophyState state, UUID player, TrophyDefinitions.Snapshot defs,
            ProgressSource progress, long now, long cooldownMillis) {
        TrophyRecord record = state.record(player).orElse(null);
        List<TrophyDefinition> sorted = new ArrayList<>(defs.trophies().values());
        sorted.sort(ORDER);
        List<RecordPayload.CollectionView> collections = collections(record, defs);
        long budget = RECORD_BUDGET_BYTES;
        for (RecordPayload.CollectionView c : collections) {
            budget -= size(c);
        }
        List<RecordPayload.Entry> entries = new ArrayList<>(Math.min(sorted.size(), RecordPayload.MAX_TROPHIES));
        for (TrophyDefinition d : sorted) {
            if (entries.size() >= RecordPayload.MAX_TROPHIES) {
                break;
            }
            RecordPayload.Entry entry = entry(record, d, progress, cooldownMillis);
            budget -= size(entry);
            if (budget < 0) {
                break;
            }
            entries.add(entry);
        }
        if (record == null) {
            return RecordPayload.empty(entries, collections, now);
        }
        return new RecordPayload(entries, collections, trims(record), record.announce(), record.showName(), now);
    }

    /** One definition as seen by {@code record}'s owner ({@code record} may be null: nothing earned yet). */
    public static RecordPayload.Entry entry(TrophyRecord record, TrophyDefinition d, ProgressSource progress,
            long cooldownMillis) {
        Optional<TrophyRecord.Earned> earned = record == null ? Optional.empty() : record.earned(d.id());
        String category = clip(d.bookCategory());
        if (earned.isEmpty() && d.hidden()) {
            return RecordPayload.Entry.masked(d.id(), category, d.order());
        }
        int current = 0;
        int target = 0;
        if (earned.isEmpty()) {
            try {
                Optional<TriggerEvaluator.Progress> p = progress.progress(d);
                if (p.isPresent()) {
                    current = p.get().current();
                    target = p.get().target();
                }
            } catch (RuntimeException e) {
                // progress is cosmetic: a failing read shows no bar
            }
        }
        long earnedAt = earned.map(TrophyRecord.Earned::at).orElse(0L);
        boolean claimed = earned.map(TrophyRecord.Earned::claimed).orElse(false);
        long canReclaimAt = claimed ? earned.get().lastClaim() + Math.max(0L, cooldownMillis) : 0L;
        return new RecordPayload.Entry(d.id(), category, earned.isPresent(), claimed, d.hidden(), false, d.tier(),
                d.icon(), clip(d.title()), clip(d.description()), earnedAt, canReclaimAt, d.order(), current, target);
    }

    /** Every loaded collection, sorted by id ascending (the cabinet screen's button order relies on this). */
    public static List<RecordPayload.CollectionView> collections(TrophyRecord record, TrophyDefinitions.Snapshot defs) {
        List<TrophyCollection> sorted = new ArrayList<>(defs.collections().values());
        sorted.sort(Comparator.comparing(c -> c.id().toString()));
        List<RecordPayload.CollectionView> out = new ArrayList<>(Math.min(sorted.size(), RecordPayload.MAX_COLLECTIONS));
        long budget = COLLECTIONS_BUDGET_BYTES;
        for (TrophyCollection c : sorted) {
            if (out.size() >= RecordPayload.MAX_COLLECTIONS) {
                break;
            }
            List<Identifier> members = c.members().size() > RecordPayload.MAX_MEMBERS
                    ? c.members().subList(0, RecordPayload.MAX_MEMBERS) : c.members();
            boolean done = record != null && record.completedCollections().contains(c.id());
            RecordPayload.CollectionView view = new RecordPayload.CollectionView(c.id(), clip(c.title()),
                    clip(c.description()), c.icon(),
                    c.trim().length() > RecordPayload.MAX_TRIM ? TrophyIds.STANDARD_TRIM : c.trim(), members, done);
            budget -= size(view);
            if (budget < 0) {
                break;
            }
            out.add(view);
        }
        return out;
    }

    /** Worst-case encoded size of one entry (strings at three bytes a character, plus fixed fields). */
    static long size(RecordPayload.Entry e) {
        return size(e.id()) + size(e.category()) + size(e.icon()) + size(e.title()) + size(e.description()) + 64;
    }

    /** Worst-case encoded size of one collection view, members included. */
    static long size(RecordPayload.CollectionView c) {
        long n = size(c.id()) + size(c.title()) + size(c.description()) + size(c.icon()) + size(c.trim()) + 16;
        for (Identifier m : c.members()) {
            n += size(m);
        }
        return n;
    }

    private static long size(Object value) {
        return value == null ? 1 : 5L + 3L * value.toString().length();
    }

    /** The earned trim names ({@code standard} is implicit and never listed). */
    public static List<String> trims(TrophyRecord record) {
        List<String> out = new ArrayList<>();
        if (record == null) {
            return out;
        }
        for (TrophyRecord.Earned e : record.earnedEntries()) {
            if (TrophyIds.isTrimId(e.trophy()) && out.size() < RecordPayload.MAX_TRIMS) {
                String trim = e.trophy().getPath().substring(TrophyIds.TRIM_PREFIX.length());
                if (!trim.isEmpty() && trim.length() <= RecordPayload.MAX_TRIM) {
                    out.add(trim);
                }
            }
        }
        return out;
    }

    private static String clip(String s) {
        return s.length() > PayloadIo.MAX_KEY ? s.substring(0, PayloadIo.MAX_KEY) : s;
    }
}
