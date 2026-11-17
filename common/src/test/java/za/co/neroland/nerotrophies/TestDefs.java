package za.co.neroland.nerotrophies;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyCollection;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyReward;
import za.co.neroland.nerolandcore.trophy.TrophyTier;
import za.co.neroland.nerolandcore.trophy.TrophyTrigger;

/** Builds definitions and snapshots for plain-JVM tests (no bootstrap). */
public final class TestDefs {

    private TestDefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("nerotrophies", path);
    }

    public static TrophyDefinition trophy(String path, TrophyTier tier) {
        return trophy(path, tier, Optional.of(new TrophyTrigger.DimensionVisited(Identifier.withDefaultNamespace("the_nether"))),
                Optional.empty());
    }

    public static TrophyDefinition trophy(String path, TrophyTier tier, Optional<TrophyTrigger> trigger,
            Optional<TrophyReward> reward) {
        return TrophyDefinition.of(id(path), new TrophyDefinition.Data(Identifier.withDefaultNamespace("gold_ingot"),
                tier, trigger, false, Optional.empty(), Optional.empty(), List.of(), Optional.empty(), reward, 0, 1));
    }

    public static TrophyCollection collection(String path, String trim, Identifier... members) {
        return TrophyCollection.of(id(path), new TrophyCollection.Data(Identifier.withDefaultNamespace("gold_ingot"),
                Optional.empty(), Optional.empty(), List.of(members), trim, Optional.empty(), List.of(), 1),
                List.of(members), Optional.empty());
    }

    public static TrophyDefinitions.Snapshot snapshot(List<TrophyDefinition> trophies, List<TrophyCollection> collections,
            long generation) {
        Map<Identifier, TrophyDefinition> t = new LinkedHashMap<>();
        trophies.forEach(d -> t.put(d.id(), d));
        Map<Identifier, TrophyCollection> c = new LinkedHashMap<>();
        collections.forEach(x -> c.put(x.id(), x));
        return new TrophyDefinitions.Snapshot(t, c, generation);
    }
}
