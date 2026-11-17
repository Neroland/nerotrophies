package za.co.neroland.nerotrophies.link;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.LinkModuleInfo;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.engine.EngineServer;
import za.co.neroland.nerotrophies.network.RecordBuilder;
import za.co.neroland.nerotrophies.platform.Services;

/**
 * NeroTrophies' plug into Neroland Core's link API: module {@value #MODULE_ID}, schema {@value #SCHEMA_VERSION},
 * read-only sections {@code trophies}, {@code collections}, {@code summary}, no actions, and two player-scoped events.
 * Every read is scoped strictly by the requester's UUID (see {@link TrophyLinkSnapshots}); events go to the earner only
 * (see {@link TrophyLinkEvents}). Gallery display copies are blocks and items, never records, so they cannot appear.
 *
 * <p>Registered last from common init. Costs one registry entry with no bridge installed; a failure here never stops
 * the mod.
 */
public final class TrophyLinkModule {

    public static final String MODULE_ID = NeroTrophiesCommon.MOD_ID;
    /** Additive changes keep 1; bump on any change to an existing field's shape. */
    public static final int SCHEMA_VERSION = 1;

    public static final String SECTION_TROPHIES = "trophies";
    public static final String SECTION_COLLECTIONS = "collections";
    public static final String SECTION_SUMMARY = "summary";
    public static final List<String> SECTIONS = List.of(SECTION_TROPHIES, SECTION_COLLECTIONS, SECTION_SUMMARY);
    public static final List<String> ACTIONS = List.of();

    private static final TrophyLinkEvents EVENTS = new TrophyLinkEvents();

    private TrophyLinkModule() {
    }

    /** Common init step 8 (last). */
    public static void init() {
        try {
            LinkModuleInfo info = new LinkModuleInfo(MODULE_ID, modVersion(), SCHEMA_VERSION, SECTIONS, ACTIONS);
            NeroLinkRegistry.registerSnapshotProvider(new TrophyLinkSnapshots(TrophyLinkModule::view), info);
            TrophyEvents.onAwarded(e -> EVENTS.onAwarded(env(), e));
            TrophyEvents.onCollectionCompleted(e -> EVENTS.onCollectionCompleted(env(), e));
            TrophyEvents.onRecordErased(e -> EVENTS.forget(e.player()));
        } catch (RuntimeException | LinkageError e) {
            // The link module is optional; the mod runs without it.
        }
    }

    /** The shared event pipeline (server thread). */
    public static TrophyLinkEvents events() {
        return EVENTS;
    }

    @Nullable
    private static TrophyLinkSnapshots.View view() {
        MinecraftServer server = EngineServer.current();
        if (server == null) {
            return null;
        }
        return new TrophyLinkSnapshots.View() {
            @Override
            public TrophyState state() {
                return TrophyState.get(server);
            }

            @Override
            public TrophyDefinitions.Snapshot definitions() {
                return TrophyApi.definitions(server);
            }

            @Override
            public RecordBuilder.ProgressSource progress(UUID player) {
                ServerPlayer online = server.getPlayerList().getPlayer(player);
                return online == null ? RecordBuilder.ProgressSource.NONE
                        : d -> EngineHooks.triggers().progress(online, d);
            }

            @Override
            public LinkText text() {
                return LinkText.SERVER;
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }
        };
    }

    @Nullable
    private static TrophyLinkEvents.Env env() {
        MinecraftServer server = EngineServer.current();
        if (server == null) {
            return null;
        }
        return new TrophyLinkEvents.Env() {
            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public long tick() {
                return server.getTickCount();
            }

            @Override
            public TrophyDefinitions.Snapshot definitions() {
                return TrophyApi.definitions(server);
            }

            @Override
            public LinkText text() {
                return LinkText.SERVER;
            }

            @Override
            public void publish(LinkEvent event) {
                NeroLinkRegistry.eventBus().publish(event);
            }
        };
    }

    private static String modVersion() {
        try {
            String version = Services.PLATFORM.getModVersion();
            return version == null || version.isBlank() ? "unknown" : version;
        } catch (RuntimeException | LinkageError e) {
            return "unknown";
        }
    }
}
