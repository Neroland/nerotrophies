package za.co.neroland.nerotrophies.platform;

import java.nio.file.Path;
import java.util.List;

/**
 * The loader-specific facts the common module is allowed to depend on.
 *
 * <p>Each loader module ships exactly one implementation, registered via a
 * {@code META-INF/services/za.co.neroland.nerotrophies.platform.PlatformInfo} file so {@link Services} can load it
 * with {@link java.util.ServiceLoader}. This keeps {@code common/} free of {@code net.neoforged.*} /
 * {@code net.fabricmc.*} / {@code net.minecraftforge.*} imports.
 *
 * <p>Neroland Core's own platform helper reports <em>Core's</em> mod version, so NeroTrophies needs its own seam to
 * tag crash reports with the NeroTrophies release. Everything exposed here is a public manifest string or a local
 * path, never personal data (POPIA/GDPR).
 */
public interface PlatformInfo {

    /** Human-readable platform name ("Fabric" / "Forge" / "NeoForge"). */
    String getPlatformName();

    /** True when running in a development (dev/data/test) environment. */
    boolean isDevelopmentEnvironment();

    /** True on the physical client (renderers, screens, HUD available). */
    boolean isClient();

    /** This mod's version string (a public manifest value, safe as a telemetry release tag), or "unknown". */
    String getModVersion();

    /** Whether a mod id is present in this launch (the gate for any soft integration). */
    boolean isModLoaded(String modId);

    /**
     * The ids + versions of every loaded mod ("modid version"), sorted, for crash mod-conflict triage. These are
     * public manifest strings only, never personal data (POPIA/GDPR).
     */
    List<String> getLoadedModIds();

    /** The instance's config directory (where Core writes {@code nerotrophies.properties}). */
    Path getConfigDir();
}
