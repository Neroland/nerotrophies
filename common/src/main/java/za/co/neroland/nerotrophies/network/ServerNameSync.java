package za.co.neroland.nerotrophies.network;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.NameSync;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.platform.Services;
import za.co.neroland.nerotrophies.registry.ModDataComponents;

/**
 * The live {@link NameSync}, installed from {@link TrophyPayloads}. Each online player's visible set is
 * their own inventory, carried stack and open menu's slots, plus stamps in loaded display blocks that sit in a chunk the
 * player tracks and within {@value #VIEW_DISTANCE} blocks. Every {@value #PASS_INTERVAL} ticks (staggered per player)
 * the visible set is diffed against what the client holds; erasure, {@code showname} and re-claims push the same tick.
 * Nothing here is ever logged and no client request can name a token.
 */
public final class ServerNameSync implements NameSync {

    public static final ServerNameSync INSTANCE = new ServerNameSync();

    public static final int PASS_INTERVAL = 20;
    public static final int VIEW_DISTANCE = 64;
    /** Stamps considered per pass (inventory + menu + displays). */
    public static final int MAX_VISIBLE = 4096;

    /** Where loaded display blocks are and what they hold. Installed by the blocks package. */
    public interface DisplayLookup {
        DisplayLookup NONE = new DisplayLookup() {
            @Override
            public Map<ChunkPos, Set<BlockPos>> loaded(ServerLevel level) {
                return Map.of();
            }

            @Override
            public List<TrophyStamp> stampsAt(ServerLevel level, BlockPos pos) {
                return List.of();
            }
        };

        /** Read-only view: chunk to display positions, for loaded display block entities in {@code level}. */
        Map<ChunkPos, Set<BlockPos>> loaded(ServerLevel level);

        /** Every stamp in the display at {@code pos}, reserved tokens included. */
        List<TrophyStamp> stampsAt(ServerLevel level, BlockPos pos);

        /** The loaded display positions in one chunk (only the chunks near a player are looked up). */
        default Set<BlockPos> inChunk(ServerLevel level, ChunkPos chunk) {
            return loaded(level).getOrDefault(chunk, Set.of());
        }
    }

    /** Chunks either side of the player's chunk that can hold a display within {@link #VIEW_DISTANCE}. */
    static final int VIEW_CHUNKS = (VIEW_DISTANCE >> 4) + 1;

    private static volatile DisplayLookup displays = DisplayLookup.NONE;

    private final NameSyncCore core = new NameSyncCore();
    private final Map<UUID, ResourceKey<Level>> dimensions = new HashMap<>();
    private WeakReference<MinecraftServer> server = new WeakReference<>(null);
    private boolean passAll;
    private long ticks;

    private ServerNameSync() {
    }

    /** The blocks package installs its display index here. */
    public static void setDisplayLookup(DisplayLookup lookup) {
        displays = lookup == null ? DisplayLookup.NONE : lookup;
    }

    /**
     * A display's contents changed (the block entity calls this the same tick): every player tracking it within view gets a
     * pass now.
     */
    public static void displayChanged(ServerLevel level, BlockPos pos) {
        INSTANCE.onDisplayChanged(level, pos);
    }

    // ---- NameSync -----------------------------------------------------------------------------------------------

    @Override
    public void invalidate(MinecraftServer s, long[] tokens) {
        bind(s);
        core.invalidate(tokens, resolver(s), sink(s));
    }

    @Override
    public void tokensChanged(MinecraftServer s, long[] tokens) {
        invalidate(s, tokens);
        passAll = true; // a freshly claimed token is picked up by everyone who can see it on the next tick
    }

    @Override
    public void playerJoined(ServerPlayer player) {
        MinecraftServer s = player.level().getServer();
        if (s == null) {
            return;
        }
        bind(s);
        core.reset(player.getUUID());
        dimensions.put(player.getUUID(), player.level().dimension());
        Services.NETWORK.sendToPlayer(player, NamesResetPayload.INSTANCE);
    }

    @Override
    public void playerLeft(MinecraftServer s, UUID player) {
        core.remove(player);
        dimensions.remove(player);
        ServerPayloadHandlers.forget(player);
    }

    @Override
    public void tick(MinecraftServer s) {
        bind(s);
        ticks++;
        boolean all = passAll;
        passAll = false;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            ResourceKey<Level> dim = p.level().dimension();
            ResourceKey<Level> was = dimensions.put(id, dim);
            if (was != null && !was.equals(dim)) {
                core.reset(id);
                Services.NETWORK.sendToPlayer(p, NamesResetPayload.INSTANCE);
                pass(s, p);
                continue;
            }
            if (all || (ticks + (id.hashCode() & 0x7fffffff)) % PASS_INTERVAL == 0) {
                pass(s, p);
            }
        }
    }

    // ---- internals -----------------------------------------------------------------------------------------------

    private void onDisplayChanged(ServerLevel level, BlockPos pos) {
        MinecraftServer s = level.getServer();
        bind(s);
        for (ServerPlayer p : level.players()) {
            if (withinView(p, pos) && tracks(level, p, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4))) {
                pass(s, p);
            }
        }
    }

    private void pass(MinecraftServer s, ServerPlayer p) {
        core.pass(p.getUUID(), visible(p), resolver(s), sink(s));
    }

    /** The stamps {@code player} can see right now (the visible set). */
    static List<TrophyStamp> visible(ServerPlayer player) {
        List<TrophyStamp> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && out.size() < MAX_VISIBLE; i++) {
            add(out, inv.getItem(i));
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            add(out, menu.getCarried());
            for (Slot slot : menu.slots) {
                if (out.size() >= MAX_VISIBLE) {
                    break;
                }
                add(out, slot.getItem());
            }
        }
        ServerLevel level = player.level();
        DisplayLookup lookup = displays;
        // Only the chunks around the player: the cost no longer grows with every display loaded on the server.
        int pcx = player.getBlockX() >> 4;
        int pcz = player.getBlockZ() >> 4;
        for (int cx = pcx - VIEW_CHUNKS; cx <= pcx + VIEW_CHUNKS && out.size() < MAX_VISIBLE; cx++) {
            for (int cz = pcz - VIEW_CHUNKS; cz <= pcz + VIEW_CHUNKS && out.size() < MAX_VISIBLE; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                Set<BlockPos> positions = lookup.inChunk(level, chunk);
                if (!positions.isEmpty()) {
                    addChunk(out, level, player, lookup, chunk, positions);
                }
            }
        }
        return out;
    }

    private static void addChunk(List<TrophyStamp> out, ServerLevel level, ServerPlayer player, DisplayLookup lookup,
            ChunkPos chunk, Set<BlockPos> positions) {
        boolean tracked = false;
        boolean checked = false;
        for (BlockPos pos : positions) {
            if (!withinView(player, pos)) {
                continue;
            }
            if (!checked) {
                tracked = tracks(level, player, chunk);
                checked = true;
            }
            if (!tracked) {
                return;
            }
            for (TrophyStamp stamp : lookup.stampsAt(level, pos)) {
                if (out.size() < MAX_VISIBLE) {
                    out.add(stamp);
                }
            }
        }
    }

    private static void add(List<TrophyStamp> out, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        if (stamp != null) {
            out.add(stamp);
        }
    }

    private static boolean withinView(ServerPlayer p, BlockPos pos) {
        double dx = p.getX() - (pos.getX() + 0.5);
        double dy = p.getY() - (pos.getY() + 0.5);
        double dz = p.getZ() - (pos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= (double) VIEW_DISTANCE * VIEW_DISTANCE;
    }

    private static boolean tracks(ServerLevel level, ServerPlayer player, ChunkPos chunk) {
        return level.getChunkSource().chunkMap.getPlayers(chunk, false).contains(player);
    }

    private static NameSyncCore.Resolver resolver(MinecraftServer s) {
        TrophyState state = TrophyState.get(s);
        return new NameSyncCore.Resolver() {
            @Override
            public TokenResolver.Resolution resolve(Identifier trophy, long token) {
                return TokenResolver.resolve(state, trophy, token);
            }

            @Override
            public NamesUpdatePayload.View view(Identifier trophy) {
                return TrophyApi.definition(s, trophy)
                        .map(d -> new NamesUpdatePayload.View(trophy, PayloadIo.tierByte(d.tier()), d.icon()))
                        .orElse(null);
            }
        };
    }

    private static NameSyncCore.Sink sink(MinecraftServer s) {
        return (UUID viewer, CustomPacketPayload payload) -> {
            ServerPlayer p = s.getPlayerList().getPlayer(viewer);
            if (p != null) {
                Services.NETWORK.sendToPlayer(p, payload);
            }
        };
    }

    /** A different server (integrated world switch) starts from nothing. */
    private void bind(MinecraftServer s) {
        if (server.get() != s) {
            server = new WeakReference<>(s);
            core.clear();
            dimensions.clear();
            ServerPayloadHandlers.LIMITER.clear();
            passAll = false;
        }
    }
}
