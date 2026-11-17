package za.co.neroland.nerotrophies.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;

/**
 * The shared behaviour of NeroTrophies' block-entity blocks: horizontal facing, a server ticker that re-checks
 * stamps slowly, finishes from dyes and Core materials (decision 006), and "breaking returns everything inside"
 * on every MC line. Interactions return a {@link Use} that is mapped to each line's result type here.
 *
 * <p>Finish rule as wired: <b>use</b> with a dye or Core material sets it (not consumed); <b>sneak-use with an empty
 * hand</b> clears it. Vanilla skips a block's use when a sneaking player holds an item, so the set path cannot
 * require sneaking on every loader.
 */
public abstract class DisplayBlock extends BaseEntityBlock {

    /** A loader-neutral interaction result. */
    protected enum Use { PASS, SUCCESS, CONSUME }

    @SuppressWarnings("this-escape")
    protected DisplayBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaults(this.stateDefinition.any()));
    }

    /** Default values of every property this block declares. */
    protected BlockState defaults(BlockState state) {
        return state.setValue(DisplayProps.FACING, Direction.NORTH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DisplayProps.FACING);
    }

    /** The block-entity type this block creates (for the ticker). */
    protected abstract BlockEntityType<? extends DisplayBlockEntity> displayType();

    /** Whether this block takes a finish. */
    protected boolean tintable() {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(DisplayProps.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(DisplayProps.FACING, rotation.rotate(state.getValue(DisplayProps.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(DisplayProps.FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide() || type != this.displayType()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<DisplayBlockEntity>) DisplayBlockEntity::serverTick;
    }

    // ============================================================================================
    // Interaction
    // ============================================================================================

    /** Use with an item in hand (finish items are handled before this). */
    protected Use useWith(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand) {
        return Use.PASS;
    }

    /** Use with an empty hand, not sneaking. */
    protected Use useEmpty(BlockState state, Level level, BlockPos pos, Player player) {
        return Use.PASS;
    }

    private Use handleItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand) {
        Optional<Identifier> finish = this.tintable() ? FinishRules.finishFor(stack) : Optional.empty();
        if (finish.isPresent()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
                display.setFinish(finish);
                actionBar(player, Component.translatable("nerotrophies.finish.set"));
            }
            return Use.SUCCESS;
        }
        return this.useWith(stack, state, level, pos, player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.isShiftKeyDown() && this.tintable()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DisplayBlockEntity display
                    && display.finish().isPresent()) {
                display.setFinish(Optional.empty());
                actionBar(player, Component.translatable("nerotrophies.finish.cleared"));
            }
            return InteractionResult.SUCCESS;
        }
        return switch (this.useEmpty(state, level, pos, player)) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            default -> InteractionResult.PASS;
        };
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return switch (this.handleItem(stack, state, level, pos, player, hand)) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            default -> InteractionResult.TRY_WITH_EMPTY_HAND;
        };
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return switch (this.handleItem(stack, state, level, pos, player, hand)) {
            case SUCCESS -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            default -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    *///?}

    // ============================================================================================
    // Removal: everything inside comes back out
    // ============================================================================================

    /** After the block is gone (server): cabinets re-join their neighbours. */
    protected void afterRemoved(ServerLevel level, BlockPos pos, BlockState oldState) {
    }

    //? if >=1.21.5 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        this.afterRemoved(level, pos, state);
    }
    //?} else {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        boolean gone = !state.is(newState.getBlock());
        if (gone && level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
            display.beforeRemoved();
        }
        super.onRemove(state, level, pos, newState, moved);
        if (gone && level instanceof ServerLevel server) {
            this.afterRemoved(server, pos, state);
        }
    }
    *///?}

    /** Gives {@code stack} to the player's hand, or their inventory, or drops it at their feet. */
    protected static void give(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
        } else if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        }
    }

    /** An action-bar line to a server player (no-op on the client). */
    public static void actionBar(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(message, true);
        }
    }
}
