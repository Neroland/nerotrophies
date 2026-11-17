package za.co.neroland.nerotrophies.data;

import java.util.UUID;
import java.util.function.Function;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.data.PlayerDataEraser;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;

import za.co.neroland.nerotrophies.engine.EngineHooks;

/**
 * NeroTrophies' one {@link PlayerDataEraser}, registered with Core's {@code PlayerDataErasure} at init.
 * Core runs it for {@code /neroland data eraseme}, an operator's erase, and its retention sweep
 * ({@code dataRetentionDays}), so NeroTrophies needs no sweep of its own.
 *
 * <p>Steps, in this order: remove the whole record and drop its tokens from the index; refresh the store's backup
 * ({@code backupNow}) so a restart cannot bring the name back; tell loaded clients to drop the names this tick
 * ({@link NameInvalidation}); fire {@code TrophyEvents.RecordErased}; send an online player their now-empty record.
 * Nothing in the world is searched or rewritten: every trophy the player earned now resolves to blank.
 */
public final class TrophyEraser implements PlayerDataEraser {

    /** Where the store lives and how it is backed up (a seam so plain-JVM tests need no server). */
    public interface Store {
        TrophyState state(MinecraftServer server);

        void backup(MinecraftServer server, TrophyState state);
    }

    /** The live store: the overworld's {@link TrophyState} and Core's recovery backup. */
    public static final Store LIVE = new Store() {
        @Override
        public TrophyState state(MinecraftServer server) {
            return TrophyState.get(server);
        }

        @Override
        public void backup(MinecraftServer server, TrophyState state) {
            // Throws IllegalStateException if the backup cannot be written; Core then reports this eraser failed.
            SavedDataRecovery.backupNow(server.overworld(), TrophyState.type(), state, TrophyState.ID.toString());
        }
    };

    private final Store store;

    public TrophyEraser() {
        this(LIVE);
    }

    public TrophyEraser(Store store) {
        this.store = store;
    }

    /** A test eraser over a fixed state, with no backup and no server. */
    public static TrophyEraser forState(TrophyState state) {
        return forStore(server -> state);
    }

    static TrophyEraser forStore(Function<MinecraftServer, TrophyState> lookup) {
        return new TrophyEraser(new Store() {
            @Override
            public TrophyState state(MinecraftServer server) {
                return lookup.apply(server);
            }

            @Override
            public void backup(MinecraftServer server, TrophyState state) {
            }
        });
    }

    @Override
    public void erase(MinecraftServer server, UUID player) {
        TrophyState state = store.state(server);
        boolean had = state.hasRecord(player);
        long[] tokens = state.erase(player);
        if (!had) {
            return;
        }
        store.backup(server, state);
        if (server != null) {
            EngineHooks.names().invalidate(server, tokens);
        }
        TrophyEvents.fireRecordErased(new TrophyEvents.RecordErased(player));
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(player);
            if (online != null) {
                EngineHooks.client().sendRecord(online);
            }
        }
    }
}
