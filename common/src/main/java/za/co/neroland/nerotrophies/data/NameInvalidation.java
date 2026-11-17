package za.co.neroland.nerotrophies.data;

import net.minecraft.server.MinecraftServer;

/**
 * Makes loaded clients forget names in the same server tick. Called by the eraser with the
 * erased player's tokens, and by the engine whenever a token's name must vanish now ({@code showname off}, revoke,
 * re-claim of the old token).
 *
 * <p>Implemented by the name-sync work package ({@link za.co.neroland.nerotrophies.engine.NameSync} extends this);
 * installed with {@link za.co.neroland.nerotrophies.engine.EngineHooks#setNameSync}. Until then it is a no-op, which
 * is safe: a client that was never sent a name has none to drop.
 */
@FunctionalInterface
public interface NameInvalidation {

    /** No client holds names yet. */
    NameInvalidation NONE = (server, tokens) -> {
    };

    /**
     * Every online client that holds any of {@code tokens} must be told, this tick, to show those trophies without a
     * name (the server re-resolves each token; an erased player's tokens resolve to blank). Server thread.
     */
    void invalidate(MinecraftServer server, long[] tokens);
}
