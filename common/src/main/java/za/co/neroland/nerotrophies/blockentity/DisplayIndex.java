package za.co.neroland.nerotrophies.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * The server-side index of loaded trophy-bearing display blocks: per level, chunk to
 * the positions of display block entities in it, maintained by the block entities as they are loaded and removed.
 * Name sync reads it to work out which tokens a player can see, and listens for content changes to push in the same
 * tick. Server thread only. Holds positions and nothing about players.
 */
public final class DisplayIndex {

    /** Told the same tick a display's contents change (insert, take, break). */
    @FunctionalInterface
    public interface Listener {
        /** {@code tokens}: the non-reserved tokens now in the display (empty when it was emptied or removed). */
        void contentsChanged(ServerLevel level, BlockPos pos, long[] tokens);
    }

    private static final Map<ServerLevel, Map<ChunkPos, Set<BlockPos>>> LOADED = new WeakHashMap<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private DisplayIndex() {
    }

    /** A read-only view of the loaded displays of {@code level}, chunk to positions. */
    public static synchronized Map<ChunkPos, Set<BlockPos>> loaded(ServerLevel level) {
        Map<ChunkPos, Set<BlockPos>> byChunk = LOADED.get(level);
        if (byChunk == null) {
            return Map.of();
        }
        Map<ChunkPos, Set<BlockPos>> copy = new HashMap<>(byChunk.size());
        byChunk.forEach((chunk, set) -> copy.put(chunk, Collections.unmodifiableSet(new HashSet<>(set))));
        return Collections.unmodifiableMap(copy);
    }

    /** The loaded display positions in one chunk of {@code level} (a copy of one small set; empty if none). */
    public static synchronized Set<BlockPos> inChunk(ServerLevel level, ChunkPos chunk) {
        Map<ChunkPos, Set<BlockPos>> byChunk = LOADED.get(level);
        Set<BlockPos> set = byChunk == null ? null : byChunk.get(chunk);
        return set == null || set.isEmpty() ? Set.of() : Set.copyOf(set);
    }

    /** Tokens of the stamps in the display at {@code pos}, reserved tokens excluded; empty if there is none. */
    public static long[] tokensAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
            return display.tokens();
        }
        return new long[0];
    }

    /** Every stamp in the display at {@code pos}, reserved tokens included (for tier / icon of gallery copies). */
    public static List<TrophyStamp> stampsAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
            return display.stamps();
        }
        return List.of();
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    static synchronized void add(ServerLevel level, BlockPos pos) {
        LOADED.computeIfAbsent(level, l -> new HashMap<>())
                .computeIfAbsent(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), c -> new HashSet<>())
                .add(pos.immutable());
    }

    static synchronized void remove(ServerLevel level, BlockPos pos) {
        Map<ChunkPos, Set<BlockPos>> byChunk = LOADED.get(level);
        if (byChunk == null) {
            return;
        }
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        Set<BlockPos> set = byChunk.get(chunk);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                byChunk.remove(chunk);
            }
        }
        if (byChunk.isEmpty()) {
            LOADED.remove(level);
        }
    }

    static void fire(ServerLevel level, BlockPos pos, long[] tokens) {
        for (Listener listener : new ArrayList<>(LISTENERS)) {
            try {
                listener.contentsChanged(level, pos, tokens);
            } catch (RuntimeException failure) {
                // A listener's failure never breaks a block interaction; nothing identifying is logged.
                za.co.neroland.nerotrophies.NeroTrophiesCommon.LOGGER.debug("[NeroTrophies] display listener failed",
                        failure);
            }
        }
    }
}
