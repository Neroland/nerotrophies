package za.co.neroland.nerotrophies.fabric;

//? if >=26.1 {
import java.util.List;
//?}

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
*///?}
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

import za.co.neroland.nerotrophies.client.render.CabinetRenderer;
import za.co.neroland.nerotrophies.client.render.DisplayTints;
import za.co.neroland.nerotrophies.client.render.PedestalRenderer;
import za.co.neroland.nerotrophies.client.render.PlaqueRenderer;
import za.co.neroland.nerotrophies.client.render.TrophyRenderClient;
import za.co.neroland.nerotrophies.client.render.TrophyRenderer;
import za.co.neroland.nerotrophies.client.screen.TrophyCabinetScreen;
import za.co.neroland.nerotrophies.client.screen.TrophyKeys;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;
//? if <26.1 {
/*import za.co.neroland.nerotrophies.registry.ModBlocks;
*///?}
import za.co.neroland.nerotrophies.registry.ModMenuTypes;

/**
 * Fabric client wiring for the blocks and screens: block-entity renderers, the cabinet screens, the finish / tier /
 * trim tints, the chunk layers (Fabric 1.21.x reads no model {@code render_type}; 26.x picks layers by texture
 * alpha), the Book key and its tick. Called once from the Fabric client entry point.
 */
public final class FabricTrophyClientContent {

    private FabricTrophyClientContent() {
    }

    public static void register() {
        TrophyRenderClient.init();
        BlockEntityRendererRegistry.register(ModBlockEntities.TROPHY.get(), ctx -> new TrophyRenderer());
        BlockEntityRendererRegistry.register(ModBlockEntities.DISPLAY_PEDESTAL.get(), ctx -> new PedestalRenderer());
        BlockEntityRendererRegistry.register(ModBlockEntities.TROPHY_CABINET.get(), ctx -> new CabinetRenderer());
        BlockEntityRendererRegistry.register(ModBlockEntities.WALL_CABINET.get(), ctx -> new CabinetRenderer());
        BlockEntityRendererRegistry.register(ModBlockEntities.TROPHY_PLAQUE.get(), ctx -> new PlaqueRenderer());
        MenuScreens.register(ModMenuTypes.TROPHY_CABINET.get(), TrophyCabinetScreen::new);
        MenuScreens.register(ModMenuTypes.WALL_CABINET.get(), TrophyCabinetScreen::new);
        //? if >=26.1 {
        BlockColorRegistry.register(List.copyOf(DisplayTints.sources()), DisplayTints.blocks());
        //?} else {
        /*ColorProviderRegistry.BLOCK.register(DisplayTints.FINISH, DisplayTints.blocks());
        *///?}
        //? if <1.21.4 {
        /*ColorProviderRegistry.ITEM.register((stack, tintIndex) -> DisplayTints.itemColour(tintIndex),
                ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_CABINET.get(), ModBlocks.WALL_CABINET.get(),
                ModBlocks.TROPHY_PLAQUE.get());
        *///?}
        layers();
        //? if >=26.1 {
        TrophyKeys.all().forEach(KeyMappingHelper::registerKeyMapping);
        //?} else {
        /*TrophyKeys.all().forEach(KeyBindingHelper::registerKeyBinding);
        *///?}
        ClientTickEvents.END_CLIENT_TICK.register(mc -> TrophyRenderClient.tick());
    }

    private static void layers() {
        //? if >=26.1 {
        // 26.x picks each quad's layer from its texture alpha; nothing to declare.
        //?} else if >=1.21.6 {
        /*net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT, ModBlocks.TROPHY.get(),
                ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_PLAQUE.get());
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT, ModBlocks.TROPHY_CABINET.get(),
                ModBlocks.WALL_CABINET.get(), ModBlocks.TROPHY_SPOTLIGHT.get());
        *///?} else {
        /*net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.cutout(), ModBlocks.TROPHY.get(),
                ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_PLAQUE.get());
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(
                net.minecraft.client.renderer.RenderType.translucent(), ModBlocks.TROPHY_CABINET.get(),
                ModBlocks.WALL_CABINET.get(), ModBlocks.TROPHY_SPOTLIGHT.get());
        *///?}
    }
}
