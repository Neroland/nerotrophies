package za.co.neroland.nerotrophies.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerolandcore.registry.PropertiesCompat;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * NeroTrophies' items: one trophy item for every trophy (the {@code nerotrophies:trophy} component says which), the
 * five display-block items and the Trophy Book. <b>Stage 2A placeholders</b>: plain items; the blocks and UI work
 * packages swap in their own item classes (tooltip, book screen) and own this file from then on.
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NeroTrophiesCommon.MOD_ID);

    /** The one trophy item. Claimed from the Book, never crafted; stacks to one so each copy keeps its token. */
    public static final RegistryEntry<Item> TROPHY = ITEMS.register(TrophyIds.TROPHY,
            key -> new BlockItem(ModBlocks.TROPHY.get(), PropertiesCompat.item(key).stacksTo(1)
                    .useBlockDescriptionPrefix()));
    public static final RegistryEntry<Item> DISPLAY_PEDESTAL = block(TrophyIds.DISPLAY_PEDESTAL, ModBlocks.DISPLAY_PEDESTAL);
    public static final RegistryEntry<Item> TROPHY_CABINET = block(TrophyIds.TROPHY_CABINET, ModBlocks.TROPHY_CABINET);
    public static final RegistryEntry<Item> WALL_CABINET = block(TrophyIds.WALL_CABINET, ModBlocks.WALL_CABINET);
    public static final RegistryEntry<Item> TROPHY_PLAQUE = block(TrophyIds.TROPHY_PLAQUE, ModBlocks.TROPHY_PLAQUE);
    public static final RegistryEntry<Item> TROPHY_SPOTLIGHT = block(TrophyIds.TROPHY_SPOTLIGHT, ModBlocks.TROPHY_SPOTLIGHT);
    public static final RegistryEntry<Item> TROPHY_BOOK = ITEMS.register(TrophyIds.TROPHY_BOOK,
            key -> new Item(PropertiesCompat.item(key).stacksTo(1)));

    private ModItems() {
    }

    private static RegistryEntry<Item> block(String id, Supplier<Block> block) {
        return ITEMS.register(id, key -> new BlockItem(block.get(), PropertiesCompat.item(key).useBlockDescriptionPrefix()));
    }

    /** Forces registration and queues the items in NeroTrophies' creative tab (common init step 3). */
    public static void init() {
        // The bare trophy item is not listed: without a stamp it is only a replica.
        ModCreativeTab.add(TROPHY_BOOK::get);
        ModCreativeTab.add(DISPLAY_PEDESTAL::get);
        ModCreativeTab.add(TROPHY_CABINET::get);
        ModCreativeTab.add(WALL_CABINET::get);
        ModCreativeTab.add(TROPHY_PLAQUE::get);
        ModCreativeTab.add(TROPHY_SPOTLIGHT::get);
    }
}
