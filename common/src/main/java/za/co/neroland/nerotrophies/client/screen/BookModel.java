package za.co.neroland.nerotrophies.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.network.RecordPayload;

/**
 * What the Trophy Book shows, derived from the player's own record only: tabs (one per category with at least one
 * loaded trophy, in catalogue order), counts per tier, and the filtered card list (hidden unearned cards last, and left
 * out entirely while a tier filter is on, so their tier never leaks). Pure; tested without a client.
 */
public final class BookModel {

    /** One rail tab: a category, or {@code null} for "All trophies". */
    public record Tab(@Nullable String category, int earned, int total) {
    }

    private final RecordPayload record;
    private final List<Tab> tabs;

    public BookModel(RecordPayload record) {
        this.record = record;
        Map<String, int[]> counts = new LinkedHashMap<>();
        List<RecordPayload.Entry> ordered = new ArrayList<>(record.trophies());
        ordered.sort(Comparator.comparingInt(RecordPayload.Entry::order).thenComparing(e -> e.id().toString()));
        int earned = 0;
        for (RecordPayload.Entry entry : ordered) {
            int[] c = counts.computeIfAbsent(entry.category(), k -> new int[2]);
            c[1]++;
            if (entry.earned()) {
                c[0]++;
                earned++;
            }
        }
        List<Tab> list = new ArrayList<>();
        list.add(new Tab(null, earned, ordered.size()));
        counts.forEach((category, c) -> list.add(new Tab(category, c[0], c[1])));
        this.tabs = List.copyOf(list);
    }

    public RecordPayload record() {
        return this.record;
    }

    public List<Tab> tabs() {
        return this.tabs;
    }

    /** Earned / total for one tier (hidden unearned trophies are counted in no tier: their tier is not sent). */
    public int[] tierCount(TrophyTier tier) {
        int earned = 0;
        int total = 0;
        for (RecordPayload.Entry entry : this.record.trophies()) {
            if (entry.tier() == tier) {
                total++;
                if (entry.earned()) {
                    earned++;
                }
            }
        }
        return new int[] {earned, total};
    }

    /** The cards of {@code category} (null = all) passing {@code filters} (empty = every tier). */
    public List<RecordPayload.Entry> cards(@Nullable String category, Set<TrophyTier> filters) {
        List<RecordPayload.Entry> out = new ArrayList<>();
        for (RecordPayload.Entry entry : this.record.trophies()) {
            if (category != null && !category.equals(entry.category())) {
                continue;
            }
            if (!filters.isEmpty() && (entry.masked() || entry.tier() == null || !filters.contains(entry.tier()))) {
                continue;
            }
            out.add(entry);
        }
        out.sort(Comparator.comparing((RecordPayload.Entry e) -> e.masked())
                .thenComparingInt(RecordPayload.Entry::order)
                .thenComparing(e -> e.id().toString()));
        return out;
    }

    /** An empty filter set. */
    public static Set<TrophyTier> noFilters() {
        return EnumSet.noneOf(TrophyTier.class);
    }

    /** The collections, in id order. */
    public List<RecordPayload.CollectionView> collections() {
        List<RecordPayload.CollectionView> sorted = new ArrayList<>(this.record.collections());
        sorted.sort(Comparator.comparing(RecordPayload.CollectionView::id));
        return sorted;
    }

    /** Members of {@code view} the player has earned. */
    public int earnedIn(RecordPayload.CollectionView view) {
        int n = 0;
        for (var id : view.members()) {
            RecordPayload.Entry entry = this.record.entry(id);
            if (entry != null && entry.earned()) {
                n++;
            }
        }
        return n;
    }
}
