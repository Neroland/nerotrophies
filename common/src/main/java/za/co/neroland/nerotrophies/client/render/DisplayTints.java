package za.co.neroland.nerotrophies.client.render;

import java.util.List;
//? if >=26.1 {
import java.util.Set;
//?}

import net.minecraft.core.BlockPos;
//? if >=26.1 {
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.properties.Property;
//?} else {
/*import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.palette.Finish;
import za.co.neroland.nerolandcore.palette.PaletteRegistry;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.blockentity.SlotView;
import za.co.neroland.nerotrophies.registry.ModBlocks;

/**
 * Block tints of the four tintable display blocks (decision 006; art tint indices): {@code 0} the palette finish
 * (default graphite {@code #4A525C}), {@code 1} the tier colour of what the block holds (pedestal ring, plaque
 * medallion), {@code 2} the cabinet trim's accent. Read from the block entity at the position; never a name. With
 * NeroDecor present its Chroma hub drives the same faces (it re-meshes them), so nothing here depends on NeroDecor.
 * 26.x registers one tint source per index; 1.21.x one {@code BlockColor}. Client only.
 */
//? if >=26.1 {
public final class DisplayTints implements BlockTintSource {
//?} else {
/*public final class DisplayTints implements BlockColor {
*///?}

    public static final DisplayTints FINISH = new DisplayTints(0);
    public static final DisplayTints TIER = new DisplayTints(1);
    public static final DisplayTints ACCENT = new DisplayTints(2);

    private final int index;

    private DisplayTints(int index) {
        this.index = index;
    }

    /** The blocks these tints are registered for. */
    public static Block[] blocks() {
        return new Block[] {ModBlocks.DISPLAY_PEDESTAL.get(), ModBlocks.TROPHY_CABINET.get(),
                ModBlocks.WALL_CABINET.get(), ModBlocks.TROPHY_PLAQUE.get()};
    }

    /** The 26.x tint source list, by tint index. */
    public static List<DisplayTints> sources() {
        return List.of(FINISH, TIER, ACCENT);
    }

    /** The colour (opaque RGB) for {@code index} given the block entity, or the default without one. */
    public static int colour(int index, @Nullable DisplayBlockEntity be) {
        return switch (index) {
            case 0 -> 0xFF000000 | (be == null ? TrophyColours.DEFAULT_FINISH : be.finish()
                    .flatMap(PaletteRegistry::getFinish).map(Finish::rgb).orElse(TrophyColours.DEFAULT_FINISH));
            case 1 -> tierColour(be);
            case 2 -> 0xFF000000 | (be instanceof CabinetBlockEntity cabinet ? Trims.byId(cabinet.trim()).accent()
                    : Trims.ALL.get(0).accent());
            default -> 0xFFFFFFFF;
        };
    }

    private static int tierColour(@Nullable DisplayBlockEntity be) {
        if (be == null) {
            return TrophyColours.STEEL_DK;
        }
        SlotView view = be.view(0).orElse(null);
        if (view == null || view.tier().isEmpty()) {
            return TrophyColours.STEEL_DK;
        }
        TrophyTier tier = view.tier().get();
        if (be instanceof PedestalBlockEntity) {
            return view.replica() ? TrophyColours.STEEL_DK : TrophyColours.glow(tier);
        }
        return view.replica() ? TrophyColours.REPLICA : TrophyColours.base(tier);
    }

    @Nullable
    private static DisplayBlockEntity at(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof DisplayBlockEntity display ? display : null;
    }

    //? if >=26.1 {
    @Override
    public int color(BlockState state) {
        return colour(this.index, null);
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colour(this.index, at(level, pos));
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of();
    }
    //?} else {
    /*@Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
        return colour(tintIndex, at(level, pos));
    }

    // 1.21.1 item tints of the display-block items (later lines name constant tints in their item definitions).
    public static int itemColour(int tintIndex) {
        return colour(tintIndex, null);
    }
    *///?}
}
