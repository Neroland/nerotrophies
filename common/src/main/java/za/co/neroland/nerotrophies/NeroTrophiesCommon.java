package za.co.neroland.nerotrophies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.neroland.nerotrophies.config.NeroTrophiesConfig;
import za.co.neroland.nerotrophies.platform.Services;
import za.co.neroland.nerotrophies.registry.ModCreativeTab;
import za.co.neroland.nerotrophies.telemetry.NeroTrophiesTelemetry;

/**
 * Loader-agnostic entry point for NeroTrophies. Each loader entry point (Fabric / Forge / NeoForge) calls
 * {@link #init()} once during mod construction, then wires its own event hooks.
 *
 * <p>The ordering below is not cosmetic. Fabric registers <em>eagerly</em> (the moment a registry class is
 * touched), so anything that must exist before something else has to be listed before it, on every loader.
 * The numbered steps are the Neroland convention; later work fills the reserved slots rather than inserting
 * itself wherever is convenient.
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

        // 3. Content registries. NeroTrophies' own creative tab comes last, after every contributor has queued
        //    its items. 0.0.1 registers no items yet, only the tab.
        ModCreativeTab.init();

        // 4. Player-data erasure registration (reserved): the trophy store registers with Core's
        //    PlayerDataErasure here, before any store can exist. 0.0.1 stores nothing.

        // 5. Compat detection (reserved). 6. Network payloads on NeroTrophies' own channel (reserved).
        // 7. Server hooks (reserved). 8. The NeroLink module goes last and swallows its own failures (reserved).
    }
}
