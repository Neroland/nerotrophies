package za.co.neroland.nerotrophies.trigger;

import java.util.Optional;
import java.util.OptionalLong;

import net.minecraft.resources.Identifier;

/**
 * Everything a trigger reads about one player, as plain values. {@link VanillaPlayerFacts} reads the live game; tests
 * use a fake. Every id is matched as text: an id the game does not know (an absent mod's stat, item, block,
 * advancement, gate or milestone) answers {@link #UNKNOWN} / {@code false} / empty and never matches. Implementations
 * never log and never throw for an unknown id.
 */
public interface PlayerFacts {

    /** A count read that could not be resolved (unknown stat type, stat, item or block, or air). */
    long UNKNOWN = -1L;

    /** The advancement exists and is complete for the player. */
    boolean advancementDone(Identifier advancement);

    /** The gate is defined with {@code PLAYER} scope and is open for this player. TEAM/SERVER gates are never true. */
    boolean playerGateOpen(Identifier gate);

    /**
     * The milestone is defined with {@code PLAYER} scope and observed for this player, for {@code material} if given,
     * else for any material.
     */
    boolean playerMilestoneObserved(Identifier milestone, Optional<Identifier> material);

    /** The player's standing with the faction, or empty when no real reputation provider is registered. */
    OptionalLong reputation(Identifier faction);

    /** The player's balance in the currency, or empty when no real currency provider is registered. */
    OptionalLong balance(Identifier currency);

    /** A vanilla statistic's value, or {@link #UNKNOWN}. */
    long stat(Identifier statType, Identifier stat);

    /** {@code minecraft:picked_up} + {@code minecraft:crafted} of the item, or {@link #UNKNOWN}. */
    long itemsObtained(Identifier item);

    /** How many of the item the player carries now (main, armour, off hand), or {@link #UNKNOWN}. */
    long inventoryCount(Identifier item);

    /** {@code minecraft:used} of the block's item (successful placements by the player), or {@link #UNKNOWN}. */
    long blocksPlaced(Identifier block);

    /** The player's current dimension. */
    Identifier dimension();
}
