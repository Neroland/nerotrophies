package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * What the hall shows, read once from the loaded definitions: one group per Trophy Book category in the Book's rail
 * order (definitions by {@code order}, then id; a category appears where its first trophy does), each group bronze
 * first and nero last; every collection by id. Hidden trophies are shown like any other: the gallery is a creative-mode
 * catalogue. Pure; carries ids and lang keys only.
 */
public record GalleryCatalogue(List<Group> groups, List<TrophyCollection> collections) {

    /** One mod bay: a category and its trophies in fill order. */
    public record Group(String category, List<TrophyDefinition> trophies) {
        public Group {
            trophies = List.copyOf(trophies);
        }
    }

    public GalleryCatalogue {
        groups = List.copyOf(groups);
        collections = List.copyOf(collections);
    }

    private static final Comparator<TrophyDefinition> RAIL = Comparator.comparingInt(TrophyDefinition::order)
            .thenComparing(d -> d.id().toString());

    private static final Comparator<TrophyDefinition> IN_BAY = Comparator
            .comparingInt((TrophyDefinition d) -> d.tier().rank()).thenComparing(RAIL);

    public static GalleryCatalogue of(TrophyDefinitions.Snapshot snapshot) {
        List<TrophyDefinition> ordered = new ArrayList<>(snapshot.trophies().values());
        ordered.sort(RAIL);
        Map<String, List<TrophyDefinition>> byCategory = new LinkedHashMap<>();
        for (TrophyDefinition def : ordered) {
            byCategory.computeIfAbsent(def.bookCategory(), k -> new ArrayList<>()).add(def);
        }
        List<Group> groups = new ArrayList<>();
        byCategory.forEach((category, defs) -> {
            List<TrophyDefinition> sorted = new ArrayList<>(defs);
            sorted.sort(IN_BAY);
            groups.add(new Group(category, sorted));
        });
        List<TrophyCollection> collections = new ArrayList<>(snapshot.collections().values());
        collections.sort(Comparator.comparing(c -> c.id().toString()));
        return new GalleryCatalogue(groups, collections);
    }

    public boolean isEmpty() {
        return this.groups.isEmpty();
    }

    public int trophyCount() {
        return this.groups.stream().mapToInt(g -> g.trophies().size()).sum();
    }

    /** Every trophy, in rail order. */
    public List<TrophyDefinition> all() {
        List<TrophyDefinition> out = new ArrayList<>();
        this.groups.forEach(g -> out.addAll(g.trophies()));
        out.sort(RAIL);
        return out;
    }

    /** The first trophy of {@code tier} by rail order, a visible one if there is one (the toast shows its title). */
    public Optional<TrophyDefinition> first(TrophyTier tier) {
        List<TrophyDefinition> all = this.all();
        Optional<TrophyDefinition> visible = all.stream().filter(d -> d.tier() == tier && !d.hidden()).findFirst();
        return visible.isPresent() ? visible : all.stream().filter(d -> d.tier() == tier).findFirst();
    }

    /** Up to {@code n} trophies of {@code tier}, visible ones first, topped up from other tiers. */
    public List<TrophyDefinition> some(TrophyTier tier, int n) {
        List<TrophyDefinition> out = new ArrayList<>();
        List<TrophyDefinition> all = this.all();
        all.stream().filter(d -> d.tier() == tier && !d.hidden()).forEach(out::add);
        all.stream().filter(d -> d.tier() == tier && d.hidden()).forEach(out::add);
        all.stream().filter(d -> d.tier() != tier).forEach(out::add);
        return List.copyOf(out.subList(0, Math.min(n, out.size())));
    }

    /** The specimen trophy for the plate states: the first gold one, else the first of any tier. */
    public Optional<TrophyDefinition> specimen() {
        Optional<TrophyDefinition> gold = this.first(TrophyTier.GOLD);
        return gold.isPresent() ? gold : this.all().stream().findFirst();
    }

    public Optional<Identifier> firstCollection() {
        return this.collections.stream().map(TrophyCollection::id).findFirst();
    }
}
