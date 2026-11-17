package za.co.neroland.nerotrophies.trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

/**
 * The loaded definitions indexed by what fires them, rebuilt once per definition generation. Lookups are by the
 * trigger's target id as text, so an id no loaded mod provides simply never matches. Immutable.
 */
public final class TriggerIndex {

    public static final TriggerIndex EMPTY = new TriggerIndex(List.of());

    private final Map<Identifier, List<TrophyDefinition>> advancements;
    private final Map<Identifier, List<TrophyDefinition>> gates;
    private final Map<Identifier, List<TrophyDefinition>> milestones;
    private final Map<Identifier, List<TrophyDefinition>> factions;
    private final Map<Identifier, List<TrophyDefinition>> currencies;
    private final Map<Identifier, List<TrophyDefinition>> dimensions;
    private final List<TrophyDefinition> polled;

    public TriggerIndex(Collection<TrophyDefinition> definitions) {
        Map<Identifier, List<TrophyDefinition>> adv = new HashMap<>();
        Map<Identifier, List<TrophyDefinition>> gate = new HashMap<>();
        Map<Identifier, List<TrophyDefinition>> ms = new HashMap<>();
        Map<Identifier, List<TrophyDefinition>> fac = new HashMap<>();
        Map<Identifier, List<TrophyDefinition>> cur = new HashMap<>();
        Map<Identifier, List<TrophyDefinition>> dim = new HashMap<>();
        List<TrophyDefinition> poll = new ArrayList<>();
        List<TrophyDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(Comparator.comparing(d -> d.id().toString()));
        for (TrophyDefinition d : sorted) {
            if (d.trigger().isEmpty()) {
                continue;
            }
            TrophyTrigger t = d.trigger().get();
            if (t instanceof TrophyTrigger.Advancement a) {
                add(adv, a.advancement(), d);
            } else if (t instanceof TrophyTrigger.NeroEvent e) {
                switch (e.event()) {
                    case GATE -> add(gate, e.target(), d);
                    case MATERIAL_MILESTONE -> add(ms, e.target(), d);
                    case REPUTATION -> add(fac, e.target(), d);
                    case CURRENCY -> add(cur, e.target(), d);
                    default -> {
                        // A later Core's kind: never met.
                    }
                }
            }
            if (t instanceof TrophyTrigger.DimensionVisited v) {
                add(dim, v.dimension(), d);
            }
            if (TriggerRules.polled(t)) {
                poll.add(d);
            }
        }
        this.advancements = freeze(adv);
        this.gates = freeze(gate);
        this.milestones = freeze(ms);
        this.factions = freeze(fac);
        this.currencies = freeze(cur);
        this.dimensions = freeze(dim);
        this.polled = List.copyOf(poll);
    }

    public List<TrophyDefinition> byAdvancement(Identifier id) {
        return advancements.getOrDefault(id, List.of());
    }

    public Set<Identifier> advancementIds() {
        return advancements.keySet();
    }

    public List<TrophyDefinition> byGate(Identifier id) {
        return gates.getOrDefault(id, List.of());
    }

    public List<TrophyDefinition> byMilestone(Identifier id) {
        return milestones.getOrDefault(id, List.of());
    }

    public List<TrophyDefinition> byFaction(Identifier id) {
        return factions.getOrDefault(id, List.of());
    }

    public List<TrophyDefinition> byCurrency(Identifier id) {
        return currencies.getOrDefault(id, List.of());
    }

    public List<TrophyDefinition> byDimension(Identifier id) {
        return dimensions.getOrDefault(id, List.of());
    }

    public boolean hasDimensionTriggers() {
        return !dimensions.isEmpty();
    }

    /** {@code stat}, {@code item_obtained}, {@code block_placed} and {@code dimension_visited}, sorted by id. */
    public List<TrophyDefinition> polled() {
        return polled;
    }

    private static void add(Map<Identifier, List<TrophyDefinition>> map, Identifier key, TrophyDefinition d) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
    }

    private static Map<Identifier, List<TrophyDefinition>> freeze(Map<Identifier, List<TrophyDefinition>> map) {
        Map<Identifier, List<TrophyDefinition>> out = new HashMap<>(map.size() * 2);
        map.forEach((k, v) -> out.put(k, List.copyOf(v)));
        return Map.copyOf(out);
    }
}
