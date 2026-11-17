package za.co.neroland.nerotrophies.registry;

import java.util.List;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies' five award particles: sprites from {@code particles/<id>.json}, greyscale and
 * tinted in code per tier. They are only ever spawned client-side (the award burst is the earner's own, the idle and
 * placement effects are local), so nothing about a particle crosses the network. Each loader registers the client
 * providers through {@code client.particle.TrophyParticles}.
 */
public final class ModParticles {

    public static final RegistrationProvider<ParticleType<?>> PARTICLES =
            RegistrationProvider.get(Registries.PARTICLE_TYPE, NeroTrophiesCommon.MOD_ID);

    public static final RegistryEntry<SimpleParticleType> TROPHY_SPARK = PARTICLES.register("trophy_spark",
            key -> simple());
    public static final RegistryEntry<SimpleParticleType> TROPHY_GLINT = PARTICLES.register("trophy_glint",
            key -> simple());
    public static final RegistryEntry<SimpleParticleType> TROPHY_SHARD = PARTICLES.register("trophy_shard",
            key -> simple());
    public static final RegistryEntry<SimpleParticleType> TROPHY_RING = PARTICLES.register("trophy_ring",
            key -> simple());
    public static final RegistryEntry<SimpleParticleType> TROPHY_MOTE = PARTICLES.register("trophy_mote",
            key -> simple());

    /** Every particle id path, in registration order. */
    public static final List<String> IDS = List.of("trophy_spark", "trophy_glint", "trophy_shard", "trophy_ring",
            "trophy_mote");

    private ModParticles() {
    }

    private static SimpleParticleType simple() {
        // The constructor is protected outside NeoForge's access transformer: an empty subclass works everywhere.
        // alwaysShow=true: the award burst is feedback the earner asked for by earning, not ambience.
        return new SimpleParticleType(true) {
        };
    }

    /** Classload-forcing no-op (common init). */
    public static void init() {
    }
}
