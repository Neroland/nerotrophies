package za.co.neroland.nerotrophies.engine;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotrophies.data.NameInvalidation;

/**
 * The server side of name sync, implemented by the name-sync work package in {@code network/} and
 * installed with {@link EngineHooks#setNameSync}. The engine calls these on the server thread; the implementation
 * keeps each viewer's sent-set, runs the visibility pass and sends {@code names_update / names_drop / names_reset}.
 * It resolves tokens through {@link TokenResolver} only, never asks the client, and never logs a token or a name.
 */
public interface NameSync extends NameInvalidation {

    /** Nothing is synced (the default until the network layer installs itself). */
    NameSync NONE = new NameSync() {
        @Override
        public void invalidate(MinecraftServer server, long[] tokens) {
        }
    };

    /** A player joined (after the engine refreshed their stored name). */
    default void playerJoined(ServerPlayer player) {
    }

    /** A player left: discard their sent-set. */
    default void playerLeft(MinecraftServer server, UUID player) {
    }

    /** Once per server tick, after the engine's own work (the staggered visibility pass). */
    default void tick(MinecraftServer server) {
    }

    /**
     * The status or name of {@code tokens} may have changed (earner renamed, {@code showname on}, a re-claim minted a
     * new token): re-resolve and push to every client currently holding them.
     */
    default void tokensChanged(MinecraftServer server, long[] tokens) {
        invalidate(server, tokens);
    }
}
