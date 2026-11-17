package za.co.neroland.nerotrophies.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import za.co.neroland.nerotrophies.client.particle.TrophyParticles;
import za.co.neroland.nerotrophies.client.toast.AwardFeedback;

/**
 * Fabric client wiring for award feedback: the five particle providers (Fabric's {@code ParticleProviderRegistry} on
 * 26.1+, {@code ParticleFactoryRegistry} before), the award-toast listener and the client tick that drives delayed,
 * placement, completion and idle particles. Physical client only.
 */
public final class FabricTrophyFeedbackClient {

    private FabricTrophyFeedbackClient() {
    }

    public static void register() {
        //? if >=26.1 {
        TrophyParticles.registerAll((type, factory) ->
                net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance()
                        .register(type, sprites -> factory.apply(sprites)));
        //?} else {
        /*TrophyParticles.registerAll((type, factory) ->
                net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance()
                        .register(type, sprites -> factory.apply(sprites)));
        *///?}
        AwardFeedback.init();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> AwardFeedback.tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TrophyParticles.clear());
    }
}
