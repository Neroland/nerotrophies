package za.co.neroland.nerotrophies.gallery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import za.co.neroland.nerolandcore.palette.PaletteRegistry;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayContents;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Cabinet;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Pedestal;
import za.co.neroland.nerotrophies.gallery.GalleryPiece.Sign;
import za.co.neroland.nerotrophies.registry.ModBlocks;

/**
 * The live {@link GalleryWorld} over a {@link ServerLevel}. Every block change uses {@link #FLAGS}: clients are told,
 * but neighbours get no shape update, nothing drops and (from 1.21.5) no block-entity side effect or {@code onPlace}
 * runs, so the hall never disturbs, and {@code gallery clear} never disturbs, anything outside its box. Containers and
 * displays are emptied before their block is replaced, so nothing spills.
 */
public final class GalleryLevel implements GalleryWorld {

    /** Clients, known shape, suppress drops; from 1.21.5 also skip block-entity side effects and onPlace. */
    //? if >=1.21.5 {
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE;
    //?} else {
    /*static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    *///?}

    private final ServerLevel level;

    public GalleryLevel(ServerLevel level) {
        this.level = level;
    }

    // ---- snapshot ---------------------------------------------------------------------------------------------------

    @Override
    public Prior capture(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        CompoundTag stateTag = NbtUtils.writeBlockState(state);
        BlockEntity be = this.level.getBlockEntity(pos);
        Optional<CompoundTag> data = be == null ? Optional.empty()
                : Optional.of(be.saveWithFullMetadata(this.level.registryAccess()));
        return new Prior(stateTag, data);
    }

    @Override
    public void empty(BlockPos pos) {
        BlockEntity be = this.level.getBlockEntity(pos);
        if (be instanceof DisplayBlockEntity display) {
            for (int slot = 0; slot < display.size(); slot++) {
                if (!display.getItem(slot).isEmpty()) {
                    display.removeItem(slot);
                }
            }
        } else if (be instanceof Clearable clearable) {
            clearable.clearContent();
        }
    }

    @Override
    public void restore(BlockPos pos, Prior prior) {
        this.empty(pos);
        BlockState state = NbtUtils.readBlockState(this.level.registryAccess().lookupOrThrow(Registries.BLOCK),
                prior.state());
        this.level.setBlock(pos, state, FLAGS);
        if (prior.blockEntity().isPresent()) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be != null) {
                // Load into the block entity the block just made, rather than swapping in a new one, so anything keyed
                // by position (the display index) sees one block entity arrive, not one arrive and another leave.
                load(be, prior.blockEntity().get(), this.level);
                be.setChanged();
                this.level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void load(BlockEntity be, CompoundTag tag, ServerLevel level) {
        //? if >=1.21.6 {
        be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
        //?} else {
        /*be.loadWithComponents(tag, level.registryAccess());
        *///?}
    }

    // ---- building ---------------------------------------------------------------------------------------------------

    @Override
    public void placeBlock(BlockPos pos, GalleryPiece piece, GalleryFrame frame) {
        this.level.setBlock(pos, this.stateOf(piece, frame), FLAGS);
    }

    BlockState stateOf(GalleryPiece piece, GalleryFrame frame) {
        return switch (piece) {
            case GalleryPiece.Solid solid -> switch (solid.mat()) {
                case AIR -> Blocks.AIR.defaultBlockState();
                case FLOOR, BAND -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                case RUNNER, CEILING -> Blocks.DEEPSLATE_TILES.defaultBlockState();
                case WALL -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                case BASE -> Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            };
            case GalleryPiece.Spotlight light ->
                    ModBlocks.TROPHY_SPOTLIGHT.get().defaultBlockState()
                            .setValue(DisplayProps.SPOT_FACING, frame.world(light.face()));
            case GalleryPiece.Plate plate -> plateBlock(plate);
            case Sign sign -> sign.wall()
                    ? Blocks.WARPED_WALL_SIGN.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, frame.world(sign.face()))
                    : Blocks.WARPED_SIGN.defaultBlockState()
                            .setValue(BlockStateProperties.ROTATION_16, frame.world(sign.face()).get2DDataValue() * 4);
            case GalleryPiece.Trophy trophy -> ModBlocks.TROPHY.get().defaultBlockState()
                    .setValue(DisplayProps.FACING, frame.world(trophy.face()))
                    .setValue(DisplayProps.TIER, this.tierOf(trophy.stamp()))
                    .setValue(DisplayProps.REPLICA, trophy.stamp().token() == TrophyStamp.UNSTAMPED);
            case Pedestal pedestal -> ModBlocks.DISPLAY_PEDESTAL.get().defaultBlockState()
                    .setValue(DisplayProps.FACING, frame.world(pedestal.face()));
            case Cabinet cabinet -> (cabinet.wall() ? ModBlocks.WALL_CABINET : ModBlocks.TROPHY_CABINET).get()
                    .defaultBlockState()
                    .setValue(DisplayProps.FACING, frame.world(cabinet.face()))
                    .setValue(DisplayProps.TRIM, Trims.byId(cabinet.trim()).style());
            case GalleryPiece.Plaque plaque -> ModBlocks.TROPHY_PLAQUE.get().defaultBlockState()
                    .setValue(DisplayProps.FACING, frame.world(plaque.face()));
        };
    }

    /** Every demo plate is the same block; which award it plays is kept in the snapshot, not the world. */
    private static BlockState plateBlock(GalleryPiece.Plate plate) {
        return plate.demo() == null ? Blocks.AIR.defaultBlockState()
                : Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState();
    }

    private TrophyTier tierOf(TrophyStamp stamp) {
        return TrophyApi.definition(this.level.getServer(), stamp.trophy()).map(TrophyDefinition::tier)
                .orElse(TrophyTier.BRONZE);
    }

    @Override
    public void fill(BlockPos pos, GalleryPiece piece, GalleryFrame frame) {
        BlockEntity be = this.level.getBlockEntity(pos);
        switch (piece) {
            case Sign sign -> {
                if (be instanceof SignBlockEntity entity) {
                    writeSign(entity, sign.lines());
                    entity.setWaxed(true);
                }
            }
            case GalleryPiece.Trophy trophy -> {
                if (be instanceof DisplayBlockEntity display) {
                    display.setItem(0, DisplayContents.stack(trophy.stamp()));
                }
            }
            case GalleryPiece.Plaque plaque -> {
                if (be instanceof DisplayBlockEntity display) {
                    display.setItem(0, DisplayContents.stack(plaque.stamp()));
                }
            }
            case Pedestal pedestal -> {
                if (be instanceof PedestalBlockEntity display) {
                    display.setFinish(known(pedestal.finish()));
                    if (pedestal.stamp().isPresent()) {
                        display.setItem(0, DisplayContents.stack(pedestal.stamp().get()));
                    } else if (pedestal.showpiece()) {
                        display.setItem(0, showpiece());
                    }
                }
            }
            case Cabinet cabinet -> {
                if (be instanceof CabinetBlockEntity display) {
                    display.setDisplayOnly(cabinet.displayOnly());
                    if (cabinet.collection().isPresent()) {
                        display.bindPool(cabinet.collection());
                    }
                    display.trimPool(Trims.byId(cabinet.trim()).id());
                    display.setFinish(known(cabinet.finish()));
                    List<TrophyStamp> contents = cabinet.contents();
                    for (int slot = 0; slot < contents.size() && slot < display.size(); slot++) {
                        display.setItem(slot, DisplayContents.stack(contents.get(slot)));
                    }
                    display.refreshRow();
                }
            }
            default -> {
            }
        }
    }

    /** A finish Core actually has (a missing one shows the default rather than an unknown id). */
    private static Optional<Identifier> known(Optional<Identifier> finish) {
        return finish.filter(PaletteRegistry::contains);
    }

    /** The gallery's showpiece: an ordinary vanilla item, showing that a display takes any item. */
    static ItemStack showpiece() {
        return new ItemStack(Items.NETHER_STAR);
    }

    static Component component(GalleryText text) {
        if (text.blank()) {
            return Component.empty();
        }
        if (text.fallback().isPresent()) {
            return Component.translatableWithFallback(text.key(), text.fallback().get());
        }
        List<Object> args = new ArrayList<>();
        for (Object arg : text.args()) {
            args.add(arg instanceof GalleryText nested ? component(nested) : arg);
        }
        return Component.translatable(text.key(), args.toArray());
    }

    private static void writeSign(SignBlockEntity sign, List<GalleryText> lines) {
        Component[] messages = new Component[4];
        for (int i = 0; i < 4; i++) {
            messages[i] = i < lines.size() ? component(lines.get(i)) : Component.empty();
        }
        //? if >=26.3 {
        /*SignText text = new SignText(List.of(messages), List.of(messages), DyeColor.WHITE, true);
        sign.setText(text, net.minecraft.world.level.block.entity.SignTextSlot.FRONT);
        *///?} else {
        SignText text = new SignText();
        for (int i = 0; i < 4; i++) {
            text = text.setMessage(i, messages[i]);
        }
        text = text.setColor(DyeColor.WHITE).setHasGlowingText(true);
        sign.setText(text, true);
        //?}
    }
}
