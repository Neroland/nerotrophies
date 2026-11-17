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
 * Builds the {@code record} payload for <b>one</b> player from that player's record only (decision 009: a client never
 * receives another player's record). Pure: no server, no player object. Stat progress is computed here at build time
 * and never stored (010 section 4).
 */
public final class RecordBuilder {

    /** Live progress toward one unearned, non-hidden definition (the trigger evaluator, or nothing in tests). */
    @FunctionalInterface
    public interface ProgressSource {
        ProgressSource NONE = d -> Optional.empty();

        Optional<TriggerEvaluator.Progress> progress(TrophyDefinition definition);
    }

    private static final Comparator<TrophyDefinition> ORDER = Comparator.comparing(d -> d.id().toString());

    private RecordBuilder() {
    }

    public static RecordPayload build(TrophyState state, UUID player, TrophyDefinitions.Snapshot defs,
            ProgressSource progress, long now, long cooldownMillis) {
        TrophyRecord record = state.record(player).orElse(null);
        List<TrophyDefinition> sorted = new ArrayList<>(defs.trophies().values());
        sorted.sort(ORDER);
        List<RecordPayload.Entry> entries = new ArrayList<>(Math.min(sorted.size(), RecordPayload.MAX_TROPHIES));
        for (TrophyDefinition d : sorted) {
            if (entries.size() >= RecordPayload.MAX_TROPHIES) {
                break;
            }
            entries.add(entry(record, d, progress, cooldownMillis));
        }
        List<RecordPayload.CollectionView> collections = collections(record, defs);
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
        for (TrophyCollection c : sorted) {
            if (out.size() >= RecordPayload.MAX_COLLECTIONS) {
                break;
            }
            List<Identifier> members = c.members().size() > RecordPayload.MAX_MEMBERS
                    ? c.members().subList(0, RecordPayload.MAX_MEMBERS) : c.members();
            boolean done = record != null && record.completedCollections().contains(c.id());
            out.add(new RecordPayload.CollectionView(c.id(), clip(c.title()), clip(c.description()), c.icon(),
                    c.trim().length() > RecordPayload.MAX_TRIM ? TrophyIds.STANDARD_TRIM : c.trim(), members, done));
        }
        return out;
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
