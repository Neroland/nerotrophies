package za.co.neroland.nerotrophies.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TrimRules;
import za.co.neroland.nerotrophies.registry.ModBlockEntities;

/**
 * Trophy Cabinet (six slots) and Wall Cabinet (two). Each slot holds one item of any kind; only genuine trophies
 * ever count. Bound to a collection, a cabinet shows completion from the genuine trophies inside it and its pool: up to four adjacent cabinets of the same kind, facing the same way, with the
 * same binding. It reads what is inside and stores no identity: a collection id, a trim string and the
 * gallery-only {@code display_only} flag. Completion is resolved here on the server, on content change and every 100
 * ticks, and sent to clients as a member bitmask only.
 */
public class CabinetBlockEntity extends DisplayBlockEntity {

    private static final String PIPS_KEY = "pips";

    private final boolean wall;
    private Optional<Identifier> collection = Optional.empty();
    private String trim = Trims.ALL.get(0).id();
    private boolean displayOnly;
    /** Pool state, recomputed on the server and synced: present-member mask, member count, place in the pool. */
    private long mask;
    private int memberCount;
    private int poolIndex;
    private int poolLength = 1;

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, false);
    }

    protected CabinetBlockEntity(BlockPos pos, BlockState state, boolean wall) {
        super(wall ? ModBlockEntities.WALL_CABINET.get() : ModBlockEntities.TROPHY_CABINET.get(), pos, state,
                wall ? 2 : 6);
        this.wall = wall;
    }

    /** The Wall Cabinet's block-entity factory. */
    public static CabinetBlockEntity wall(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state, true);
    }

    public boolean isWall() {
        return this.wall;
    }

    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return anyItem(stack);
    }

    @Override
    protected int refreshPeriod() {
        return 100;
    }

    public Optional<Identifier> collection() {
        return this.collection;
    }

    public String trim() {
        return this.trim;
    }

    public boolean displayOnly() {
        return this.displayOnly;
    }

    /** Gallery only: a display-only cabinet also counts gallery copies. */
    public void setDisplayOnly(boolean displayOnly) {
        this.displayOnly = displayOnly;
        this.setChanged();
    }

    public long mask() {
        return this.mask;
    }

    public int memberCount() {
        return this.memberCount;
    }

    public int poolIndex() {
        return this.poolIndex;
    }

    public int poolLength() {
        return this.poolLength;
    }

    // ============================================================================================
    // Pools
    // ============================================================================================

    private Direction facing() {
        BlockState state = this.getBlockState();
        return state.hasProperty(DisplayProps.FACING) ? state.getValue(DisplayProps.FACING) : Direction.NORTH;
    }

    /** Whether {@code other} may join this cabinet's pool: same kind, same facing, same binding. */
    private boolean joins(CabinetBlockEntity other) {
        return other.wall == this.wall && other.facing() == this.facing()
                && other.collection.equals(this.collection);
    }

    private static CabinetBlockEntity at(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof CabinetBlockEntity cabinet ? cabinet : null;
    }

    /** This cabinet's pool, left to right (at most {@link CabinetPools#MAX_POOL}), with its own index. */
    public record Pool(List<CabinetBlockEntity> members, int index) {
    }

    public Pool pool() {
        Level level = this.level;
        if (level == null) {
            return new Pool(List.of(this), 0);
        }
        Direction left = this.facing().getCounterClockWise();
        // How far the joined run reaches to the left (bounded).
        int run = 0;
        BlockPos cursor = this.worldPosition;
        for (int i = 0; i < 64; i++) {
            CabinetBlockEntity next = at(level, cursor.relative(left));
            if (next == null || !this.joins(next)) {
                break;
            }
            cursor = cursor.relative(left);
            run++;
        }
        int index = run % CabinetPools.MAX_POOL;
        BlockPos start = this.worldPosition.relative(left, index);
        List<CabinetBlockEntity> members = new ArrayList<>();
        Direction right = left.getOpposite();
        for (int i = 0; i < CabinetPools.MAX_POOL; i++) {
            CabinetBlockEntity member = at(level, start.relative(right, i));
            if (member == null || (member != this && !this.joins(member))) {
                break;
            }
            members.add(member);
        }
        if (!members.contains(this)) {
            return new Pool(List.of(this), 0);
        }
        return new Pool(List.copyOf(members), members.indexOf(this));
    }

    /** Binds every cabinet of this pool to {@code collection} (empty = unbind) and re-joins the row. */
    public void bindPool(Optional<Identifier> collection) {
        List<CabinetBlockEntity> pool = this.pool().members();
        for (CabinetBlockEntity member : pool) {
            member.collection = collection;
            member.setChanged();
        }
        this.refreshRow();
    }

    /** Applies {@code trim} to every cabinet of this pool, so the frame stays one frame. */
    public void trimPool(String trim) {
        if (!TrimRules.isValid(trim)) {
            return;
        }
        for (CabinetBlockEntity member : this.pool().members()) {
            member.trim = trim;
            member.setChanged();
        }
        this.refreshRow();
    }

    /** Refreshes this cabinet and its neighbours along the row (a pool or binding changed). */
    public void refreshRow() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        Direction left = this.facing().getCounterClockWise();
        for (int offset = -CabinetPools.MAX_POOL; offset <= CabinetPools.MAX_POOL; offset++) {
            CabinetBlockEntity cabinet = at(server, this.worldPosition.relative(left, offset));
            if (cabinet != null && cabinet.refresh(server)) {
                cabinet.sync();
            }
        }
    }

    /** Refreshes the cabinets beside {@code pos} after a block there changed (placed or removed). */
    public static void refreshAround(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int step = 1; step <= CabinetPools.MAX_POOL; step++) {
                CabinetBlockEntity cabinet = at(level, pos.relative(side, step));
                if (cabinet == null) {
                    break;
                }
                if (cabinet.refresh(level)) {
                    cabinet.sync();
                }
            }
        }
    }

    @Override
    protected boolean afterRefresh(ServerLevel level, TrophyState state) {
        Pool pool = this.pool();
        boolean changed = false;
        int members = 0;
        long newMask = 0L;
        if (this.collection.isPresent()) {
            Optional<TrophyCollection> def = TrophyApi.definitions(level.getServer()).collection(this.collection.get());
            if (def.isPresent()) {
                List<Identifier> list = def.get().members();
                members = Math.min(list.size(), CabinetPools.MAX_MEMBERS);
                List<TrophyStamp> stamps = new ArrayList<>();
                for (CabinetBlockEntity member : pool.members()) {
                    stamps.addAll(member.stamps());
                }
                newMask = CabinetPools.memberMask(list, stamps, s -> statusOf(state, s), this.displayOnly);
            }
        }
        if (newMask != this.mask || members != this.memberCount || pool.index() != this.poolIndex
                || pool.members().size() != this.poolLength) {
            this.mask = newMask;
            this.memberCount = members;
            this.poolIndex = pool.index();
            this.poolLength = pool.members().size();
            changed = true;
        }
        BlockState current = this.getBlockState();
        if (current.hasProperty(DisplayProps.LEFT)) {
            BlockState next = current
                    .setValue(DisplayProps.LEFT, pool.index() > 0)
                    .setValue(DisplayProps.RIGHT, pool.index() < pool.members().size() - 1)
                    .setValue(DisplayProps.TRIM, Trims.byId(this.trim).style());
            if (next != current) {
                level.setBlock(this.worldPosition, next, 3);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void contentsChanged() {
        super.contentsChanged();
        // Every cabinet of the pool shows the pool's completion: refresh the row, not just this block.
        this.refreshRow();
    }

    /** Genuine members present in the pool. */
    public int presentCount() {
        return Long.bitCount(this.mask);
    }

    // ============================================================================================
    // Persistence
    // ============================================================================================

    @Override
    protected Optional<Identifier> savedCollection() {
        return this.collection;
    }

    @Override
    protected String savedTrim() {
        return this.trim;
    }

    @Override
    protected boolean savedDisplayOnly() {
        return this.displayOnly;
    }

    @Override
    protected void loadExtra(DisplaySave save) {
        this.collection = save.collection();
        this.trim = TrimRules.isValid(save.trim()) ? save.trim() : Trims.ALL.get(0).id();
        this.displayOnly = save.displayOnly();
    }

    @Override
    protected void writeClientExtra(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(PIPS_KEY, new long[] {this.mask, this.memberCount, this.poolIndex, this.poolLength});
    }

    @Override
    protected void loadClientExtra(ValueInput input) {
        input.read(PIPS_KEY, com.mojang.serialization.Codec.LONG_STREAM).ifPresent(stream -> {
            long[] values = stream.toArray();
            if (values.length == 4) {
                this.mask = values[0];
                this.memberCount = (int) values[1];
                this.poolIndex = (int) values[2];
                this.poolLength = (int) Math.max(1, values[3]);
            }
        });
    }
}
