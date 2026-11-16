package za.co.neroland.nerotrophies.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.nerolandcore.registry.RegistrationProvider;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/** MinecraftForge entry point for NeroTrophies. */
@Mod(NeroTrophiesCommon.MOD_ID)
public final class NeroTrophiesForge {

    public NeroTrophiesForge(FMLJavaModLoadingContext context) {
        NeroTrophiesCommon.LOGGER.info("[NeroTrophies] Forge bootstrap");
        NeroTrophiesCommon.init();
        // Common init created NeroTrophies' deferred registers through Core's registration seam; this attaches them
        // to our mod bus group.
        //? if >=1.21.6 {
        RegistrationProvider.attach(context.getModBusGroup());
        //?} else {
        /*RegistrationProvider.attach(context.getModEventBus());
        *///?}
    }
}
