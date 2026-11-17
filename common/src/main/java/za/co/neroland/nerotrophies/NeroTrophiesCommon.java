package za.co.neroland.nerotrophies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import za.co.neroland.nerotrophies.command.TrophyCommands;
import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.data.NeroTrophiesData;
import za.co.neroland.nerotrophies.engine.EngineServer;
import za.co.neroland.nerotrophies.engine.TrophyEngine;
import za.co.neroland.nerotrophies.gallery.TrophyGallery;
import za.co.neroland.nerotrophies.link.TrophyLinkModule;
import za.co.neroland.nerotrophies.network.TrophyNetwork;
import za.co.neroland.nerotrophies.platform.Services;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;
import za.co.neroland.nerotrophies.registry.ModBlocks;
import za.co.neroland.nerotrophies.registry.ModCreativeTab;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;
import za.co.neroland.nerotrophies.registry.ModMenuTypes;
import za.co.neroland.nerotrophies.telemetry.NeroTrophiesTelemetry;
import za.co.neroland.nerotrophies.trigger.TrophyTriggers;

/**
 * Loader-agnostic entry point for NeroTrophies. Each loader entry point (Fabric / Forge / NeoForge) calls
 * {@link #init()} once during mod construction, then wires {@link #onServerTick}, {@link #onServerStopped} and
 * {@link #registerCommands} to its own events.
 *
 * <p>The ordering below is not cosmetic. Fabric registers <em>eagerly</em> (the moment a registry class is
 * touched), so anything that must exist before something else has to be listed before it, on every loader.
 * Each later work package owns the one class its step calls; nobody edits another package's step.
 */
public final class NeroTrophiesCommon {

    public static final String MOD_ID = "nerotrophies";
    public static final Logger LOGGER = LoggerFactory.getLogger("NeroTrophies");

    private NeroTrophiesCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[NeroTrophies] common init");

        // 0. Platform seam (PlatformInfo), resolved here during construction and never lazily on a tick
        //    path: a late ServiceLoader read can throw ServiceConfigurationError out of gameplay code.
        Services.init();

        // 1. Config first: everything below reads it, including telemetry's opt-out flag.
        NeroTrophiesConfig.init();

        // 2. Anonymous, NeroTrophies-only crash reporting. Follows the config registration and precedes the rest
        //    of init so early failures are still reported. On by default (opt-out via telemetryEnabled=false);
        //    a build whose DSN is the placeholder stays inert (see NeroTrophiesTelemetry's PLACEHOLDER_DSN guard).
        NeroTrophiesTelemetry.init();

        // 3. Content registries: data components first, the creative tab last.
        ModDataComponents.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModItems.init();
        ModMenuTypes.init();
        ModCreativeTab.init();

        // 4. Player-data erasure (and with it Core's retention sweep) before any store can exist, then the trophy
        //    provider: TrophyApi now records into NeroTrophies' one store (TrophyState).
        NeroTrophiesData.init();
        TrophyEngine.init();

        // 5. Trigger evaluation (owned by trigger/): installs the TriggerEvaluator and the live listeners.
        TrophyTriggers.init();

        // 6. Network payloads on NeroTrophies' own channel, before any loader builds it (payloads owned by network/).
        TrophyNetwork.init();

        // 7. Commands are registered by each loader through registerCommands; the gallery extends them here.
        TrophyGallery.init();

        // 8. The NeroLink module (Core's link API) goes last and swallows its own failures.
        TrophyLinkModule.init();
    }

    /** End of every server tick (every loader). */
    public static void onServerTick(MinecraftServer server) {
        EngineServer.tick(server);
    }

    /** The server stopped (every loader). */
    public static void onServerStopped() {
        EngineServer.stopped();
    }

    /** Command registration (every loader). */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        TrophyCommands.register(dispatcher);
    }
}
