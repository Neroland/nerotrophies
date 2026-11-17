package za.co.neroland.nerotrophies.trigger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import net.minecraft.resources.Identifier;

/** A scripted player for trigger tests; anything not set is unknown to the "game". */
final class FakeFacts implements PlayerFacts {

    final Set<Identifier> advancements = new HashSet<>();
    final Set<Identifier> playerGates = new HashSet<>();
    final Map<Identifier, Set<Identifier>> milestones = new HashMap<>();
    final Map<Identifier, Long> reputation = new HashMap<>();
    final Map<Identifier, Long> balances = new HashMap<>();
    final Map<String, Long> stats = new HashMap<>();
    final Map<Identifier, Long> obtained = new HashMap<>();
    final Map<Identifier, Long> held = new HashMap<>();
    final Map<Identifier, Long> placed = new HashMap<>();
    boolean realReputation;
    boolean realCurrency;
    Identifier dimension = Identifier.withDefaultNamespace("overworld");

    @Override
    public boolean advancementDone(Identifier advancement) {
        return advancements.contains(advancement);
    }

    @Override
    public boolean playerGateOpen(Identifier gate) {
        return playerGates.contains(gate);
    }

    @Override
    public boolean playerMilestoneObserved(Identifier milestone, Optional<Identifier> material) {
        Set<Identifier> seen = milestones.getOrDefault(milestone, Set.of());
        return material.isPresent() ? seen.contains(material.get()) : !seen.isEmpty();
    }

    @Override
    public OptionalLong reputation(Identifier faction) {
        return realReputation ? OptionalLong.of(reputation.getOrDefault(faction, 0L)) : OptionalLong.empty();
    }

    @Override
    public OptionalLong balance(Identifier currency) {
        return realCurrency ? OptionalLong.of(balances.getOrDefault(currency, 0L)) : OptionalLong.empty();
    }

    @Override
    public long stat(Identifier statType, Identifier stat) {
        return stats.getOrDefault(statType + "|" + stat, UNKNOWN);
    }

    @Override
    public long itemsObtained(Identifier item) {
        return obtained.getOrDefault(item, UNKNOWN);
    }

    @Override
    public long inventoryCount(Identifier item) {
        return held.getOrDefault(item, UNKNOWN);
    }

    @Override
    public long blocksPlaced(Identifier block) {
        return placed.getOrDefault(block, UNKNOWN);
    }

    @Override
    public Identifier dimension() {
        return dimension;
    }
}
