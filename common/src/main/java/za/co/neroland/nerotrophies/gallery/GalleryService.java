package za.co.neroland.nerotrophies.gallery;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.engine.ClientNotifier;
import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * One server's gallery runtime: the running build or clear job, run a budgeted slice per server tick, and the demo
 * plates, driven from the common server tick. Who asked and who stands on a plate is kept in memory only, never
 * saved and never logged.
 */
final class GalleryService {

    /** Most cells handled per tick. */
    static final int OPS_PER_TICK = 1024;
    /** Time budget per tick (the slice stops at the first cell past it). */
    static final long NANOS_PER_TICK = 4_000_000L;
    /** Number shown by the summary demo toast. */
    static final int DEMO_SUMMARY_COUNT = 3;

    private static final Map<MinecraftServer, GalleryService> SERVICES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final WeakReference<MinecraftServer> server;
    private GalleryStore store;
    private GalleryJob job;
    private ServerLevel jobLevel;
    private UUID requester;
    private Map<BlockPos, GalleryPiece.Demo> plates;
    private ResourceKey<Level> platesLevel;
    private final Map<UUID, BlockPos> standing = new HashMap<>();
    private boolean failedOnce;

    private GalleryService(MinecraftServer server) {
        this.server = new WeakReference<>(server);
    }

    /** This server's gallery runtime, created on the first call. Server thread. */
    static GalleryService attach(MinecraftServer server) {
        GalleryService existing = SERVICES.get(server);
        if (existing != null) {
            return existing;
        }
        GalleryService service = new GalleryService(server);
        SERVICES.put(server, service);
        return service;
    }

    GalleryStore store() {
        MinecraftServer s = this.server.get();
        if (this.store == null && s != null) {
            this.store = GalleryStore.get(s);
        }
        return this.store;
    }

    boolean busy() {
        return this.job != null;
    }

    void start(GalleryJob job, ServerLevel level, UUID requester) {
        this.job = job;
        this.jobLevel = level;
        this.requester = requester;
        this.plates = null;
    }

    /** End of every server tick ({@link TrophyGallery#tick}). Cheap when no hall stands. */
    void tick() {
        MinecraftServer s = this.server.get();
        if (s == null) {
            return;
        }
        try {
            if (this.job != null) {
                this.runJob(s);
            }
            this.plates(s);
        } catch (RuntimeException e) {
            this.job = null;
            this.plates = Map.of();
            if (!this.failedOnce) {
                this.failedOnce = true;
                // The exception class only: a message could carry world data.
                NeroTrophiesCommon.LOGGER.warn("[NeroTrophies] gallery job failed; stopped ({})",
                        e.getClass().getName());
            }
            NeroTrophiesCommon.LOGGER.debug("[NeroTrophies] gallery job failure", e);
            this.tell(s, "nerotrophies.gallery.failed");
        }
    }

    private void runJob(MinecraftServer s) {
        GalleryJob current = this.job;
        boolean captured = current.step(new GalleryLevel(this.jobLevel), OPS_PER_TICK, NANOS_PER_TICK,
                System::nanoTime);
        if (captured) {
            // The whole snapshot is saved before the first block of the hall goes down.
            this.store().set(current.snapshot());
        }
        if (current.done()) {
            this.job = null;
            this.plates = null;
            if (current.kind() == GalleryJob.Kind.CLEAR) {
                this.store().clear();
                this.standing.clear();
                this.tell(s, "nerotrophies.gallery.cleared");
            } else {
                this.tell(s, "nerotrophies.gallery.built");
            }
            this.requester = null;
        }
    }

    private void tell(MinecraftServer s, String key) {
        if (this.requester != null) {
            ServerPlayer player = s.getPlayerList().getPlayer(this.requester);
            if (player != null) {
                player.sendSystemMessage(Component.translatable(key));
            }
        }
    }

    // ---- demo plates ------------------------------------------------------------------------------------------------

    private void plates(MinecraftServer s) {
        if (this.plates == null) {
            this.plates = new HashMap<>();
            this.store().snapshot().ifPresent(snapshot -> {
                snapshot.plates().forEach(p -> this.plates.put(p.pos(), p.demo()));
                this.platesLevel = ResourceKey.create(Registries.DIMENSION, snapshot.dimension());
            });
        }
        if (this.plates.isEmpty() || this.job != null) {
            if (!this.standing.isEmpty()) {
                this.standing.clear();
            }
            return;
        }
        ServerLevel level = s.getLevel(this.platesLevel);
        if (level == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            BlockPos at = player.blockPosition();
            GalleryPiece.Demo demo = this.plates.get(at);
            UUID id = player.getUUID();
            if (demo == null) {
                this.standing.remove(id);
            } else if (!at.equals(this.standing.put(id, at))) {
                play(s, player, demo);
            }
        }
        if (this.standing.size() > level.players().size()) {
            this.standing.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        }
    }

    /** Shows the demo toast and plays its sound for {@code player} only (the award_toast payload, earner-only). */
    static void play(MinecraftServer s, ServerPlayer player, GalleryPiece.Demo demo) {
        ClientNotifier client = EngineHooks.client();
        GalleryCatalogue catalogue = GalleryCatalogue.of(TrophyApi.definitions(s));
        switch (demo) {
            case COLLECTION -> client.collectionToast(player,
                    catalogue.firstCollection().orElse(TrophyIds.id("gallery/demo")));
            case SUMMARY -> client.retroSummary(player, DEMO_SUMMARY_COUNT);
            default -> {
                TrophyTier tier = TrophyTier.valueOf(demo.name());
                Identifier id = catalogue.first(tier).map(TrophyDefinition::id).orElse(TrophyIds.id("gallery/demo"));
                client.awardToast(player, id, tier);
            }
        }
    }
}
