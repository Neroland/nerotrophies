package za.co.neroland.nerotrophies.engine;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * What the engine tells a player's own client. Implemented by the network work package
 * ({@code record}, {@code record_delta}, {@code award_toast} payloads) and installed with
 * {@link EngineHooks#setClientNotifier}. Every call targets the earner only; nothing here reaches another player.
 */
public interface ClientNotifier {

    /** Nothing is sent (the default until the network layer installs itself). */
    ClientNotifier NONE = new ClientNotifier() {
    };

    /** Send the player's whole own record (login pass end, reload, revoke, erasure). */
    default void sendRecord(ServerPlayer player) {
    }

    /** One entry of the player's own record changed (award, claim, re-claim). */
    default void recordChanged(ServerPlayer player, Identifier trophy) {
    }

    /** {@code award_toast} kind 0: a live (or API / command) award. */
    default void awardToast(ServerPlayer player, Identifier trophy, TrophyTier tier) {
    }

    /** {@code award_toast} kind 1: a collection completed (not during a retroactive pass). */
    default void collectionToast(ServerPlayer player, Identifier collection) {
    }

    /** {@code award_toast} kind 2: a retroactive pass recorded {@code count} trophies. */
    default void retroSummary(ServerPlayer player, int count) {
    }
}
