package za.co.neroland.nerotrophies.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
//? if >=1.21.5 {
import net.minecraft.core.component.DataComponentGetter;
//?}
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.palette.PaletteRegistry;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinition;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;

/**
 * The shared block entity of every trophy-bearing display (trophy, pedestal, both cabinets, plaque). It holds item
 * stacks in memory, but writes only {@link DisplaySave}: stamps (trophy id + token) per slot, a palette finish and,
 * for a cabinet, its binding and trim. A pedestal's non-trophy item is the one plain stack it saves. Never a name,
 * never a UUID.
 *
 * <p>The server re-resolves every stamp on load and then every {@link #refreshPeriod()} ticks (and at once on any
 * content change), and sends clients a {@link SlotView} per slot - tier, icon, replica - so they can draw the right
 * model. Those views are display only: the server never reads them back.
 */
public abstract class DisplayBlockEntity extends BlockEntity {

    private static final String SAVE_KEY = "display";
    private static final String OTHERS_KEY = "items";
    private static final String VIEWS_KEY = "views";

    /** A non-trophy stack in a slot (pedestal only). */
    record OtherItem(int slot, ItemStack stack) {
        static final Codec<OtherItem> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 63).fieldOf("slot").forGetter(OtherItem::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(OtherItem::stack)).apply(i, OtherItem::new));
    }

    protected final NonNullList<ItemStack> items;
    private Optional<Identifier> finish = Optional.empty();
    private List<SlotView> views = List.of();
    private int countdown = 1;
    private boolean indexed;

    protected DisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ============================================================================================
    // Contents
    // ============================================================================================

    public int size() {
        return this.items.size();
    }

    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    /** Whether {@code stack} may go into {@code slot} (trophies everywhere; a pedestal also takes displayables). */
    public abstract boolean accepts(int slot, ItemStack stack);

    /** Whether a finish can be set on this block. */
    public boolean tintable() {
        return true;
    }

    /** Ticks between the server's slow re-checks of the stamps. */
    protected int refreshPeriod() {
        return 200;
    }

    /** Puts {@code stack} (one item) into {@code slot}; the caller has checked {@link #accepts}. */
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.items.size()) {
            return;
        }
        this.items.set(slot, stack);
        this.contentsChanged();
    }

    /** Takes the whole stack out of {@code slot}. */
    public ItemStack removeItem(int slot) {
        if (slot < 0 || slot >= this.items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        if (!out.isEmpty()) {
            this.contentsChanged();
        }
        return out;
    }

    /** Called after any slot changed (also by the cabinet menu's container). */
    public void contentsChanged() {
        this.setChanged();
        if (this.level instanceof ServerLevel server) {
            this.refresh(server);
            this.sync();
            DisplayIndex.fire(server, this.worldPosition, this.tokens());
        }
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The stamp a stack carries, or null for a non-trophy stack. A trophy with no stamp is an unstamped replica. */
    @Nullable
    public static TrophyStamp stampOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.TROPHY.get())) {
            return null;
        }
        TrophyStamp stamp = stack.get(ModDataComponents.TROPHY.get());
        return stamp != null ? stamp : new TrophyStamp(TrophyIdsHolder.UNKNOWN, TrophyStamp.UNSTAMPED);
    }

    /** Every stamp held, reserved tokens included. */
    public List<TrophyStamp> stamps() {
        List<TrophyStamp> out = new ArrayList<>();
        for (ItemStack stack : this.items) {
            TrophyStamp stamp = stampOf(stack);
            if (stamp != null) {
                out.add(stamp);
            }
        }
        return out;
    }

    /** Tokens of the stamps held, reserved tokens excluded. */
    public long[] tokens() {
        return this.stamps().stream().mapToLong(TrophyStamp::token).filter(t -> !TrophyStamp.isReserved(t))
                .distinct().toArray();
    }

    /** Hands every stack out at the block and empties it (breaking any display returns everything inside it). */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : DisplayContents.drain(this.items)) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
        }
        this.setChanged();
    }

    // ============================================================================================
    // Finish
    // ============================================================================================

    public Optional<Identifier> finish() {
        return this.finish;
    }

    public void setFinish(Optional<Identifier> finish) {
        this.finish = finish;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            int glow = finish.flatMap(PaletteRegistry::getFinish)
                    .map(f -> f.emissive() ? Math.max(0, Math.min(15, f.emissiveLight())) : 0).orElse(0);
            if (state.hasProperty(DisplayProps.GLOW) && state.getValue(DisplayProps.GLOW) != glow) {
                this.level.setBlock(this.worldPosition, state.setValue(DisplayProps.GLOW, glow), 3);
            }
            this.sync();
        }
    }

    // ============================================================================================
    // Server resolution
    // ============================================================================================

    /** Display views for the client, as last sent (server) or received (client). */
    public List<SlotView> views() {
        return this.views;
    }

    /** The view of {@code slot}, if any. */
    public Optional<SlotView> view(int slot) {
        for (SlotView view : this.views) {
            if (view.slot() == slot) {
                return Optional.of(view);
            }
        }
        return Optional.empty();
    }

    /** Server ticker body: a countdown, then a re-check. Cheap when idle. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DisplayBlockEntity be) {
        if (--be.countdown > 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        be.countdown = be.refreshPeriod() + (int) Math.floorMod(pos.asLong(), 37L);
        if (be.refresh(server)) {
            be.sync();
        }
    }

    /** Re-resolves every stamp against the server's store; true if anything a client draws changed. */
    public boolean refresh(ServerLevel level) {
        MinecraftServer server = level.getServer();
        TrophyState state = TrophyState.get(server);
        List<SlotView> fresh = new ArrayList<>();
        for (int slot = 0; slot < this.items.size(); slot++) {
            TrophyStamp stamp = stampOf(this.items.get(slot));
            if (stamp == null) {
                continue;
            }
            TrophyStatus status = TokenResolver.resolve(state, stamp.trophy(), stamp.token()).status();
            Optional<TrophyDefinition> def = TrophyApi.definition(server, stamp.trophy());
            fresh.add(new SlotView(slot, def.map(TrophyDefinition::tier), def.map(TrophyDefinition::icon),
                    status == TrophyStatus.REPLICA));
        }
        boolean changed = !fresh.equals(this.views);
        this.views = List.copyOf(fresh);
        changed |= this.afterRefresh(level, state);
        return changed;
    }

    /** Subclass hook after the views were rebuilt (blockstate, pool); true if a client must be told. */
    protected boolean afterRefresh(ServerLevel level, TrophyState state) {
        return false;
    }

    /** Resolution of one stamp on the server (cabinets and pools). */
    protected static TrophyStatus statusOf(TrophyState state, TrophyStamp stamp) {
        return TokenResolver.resolve(state, stamp.trophy(), stamp.token()).status();
    }

    protected void sync() {
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // ============================================================================================
    // Lifecycle: the display index
    // ============================================================================================

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level instanceof ServerLevel server && !this.indexed) {
            DisplayIndex.add(server, this.worldPosition);
            this.indexed = true;
            this.countdown = 1;
        }
    }

    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel server && this.indexed) {
            DisplayIndex.remove(server, this.worldPosition);
            this.indexed = false;
        }
        super.setRemoved();
    }

    //? if >=1.21.5 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            this.beforeRemoved();
        }
        super.preRemoveSideEffects(pos, state);
    }
    //?}

    /** The block is going away for good (not a state change): drop contents, tell listeners. */
    public void beforeRemoved() {
        if (this.level == null) {
            return;
        }
        if (this.dropsContentsOnRemoval()) {
            this.dropContents(this.level, this.worldPosition);
        }
        if (this.level instanceof ServerLevel server) {
            DisplayIndex.fire(server, this.worldPosition, new long[0]);
        }
    }

    /** False for the trophy block, whose loot table returns the trophy with its stamp. */
    protected boolean dropsContentsOnRemoval() {
        return true;
    }

    // ============================================================================================
    // Persistence (DisplaySave) and client sync
    // ============================================================================================

    /** The persisted part of this display. */
    public DisplaySave toSave() {
        List<DisplaySave.SlotStamp> stamps = new ArrayList<>();
        for (int slot = 0; slot < this.items.size(); slot++) {
            TrophyStamp stamp = stampOf(this.items.get(slot));
            if (stamp != null) {
                stamps.add(new DisplaySave.SlotStamp(slot, stamp));
            }
        }
        return new DisplaySave(stamps, this.finish, this.savedCollection(), this.savedTrim(),
                this.savedDisplayOnly());
    }

    /** Cabinet binding; the base has none. */
    protected Optional<Identifier> savedCollection() {
        return Optional.empty();
    }

    /** Cabinet trim; the base keeps the default. */
    protected String savedTrim() {
        return DisplaySave.DEFAULT_TRIM;
    }

    /** Gallery-only flag; the base is never display-only. */
    protected boolean savedDisplayOnly() {
        return false;
    }

    /** Applies the persisted cabinet fields; the base ignores them. */
    protected void loadExtra(DisplaySave save) {
    }

    private List<OtherItem> others() {
        List<OtherItem> out = new ArrayList<>();
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty() && stampOf(stack) == null) {
                out.add(new OtherItem(slot, stack));
            }
        }
        return out;
    }

    private void apply(DisplaySave save, List<OtherItem> others) {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        for (DisplaySave.SlotStamp entry : save.stamps()) {
            if (entry.slot() < this.items.size()) {
                this.items.set(entry.slot(), DisplayContents.stack(entry.stamp()));
            }
        }
        for (OtherItem other : others) {
            if (other.slot() < this.items.size() && this.items.get(other.slot()).isEmpty()) {
                this.items.set(other.slot(), other.stack());
            }
        }
        this.finish = save.finish();
        this.loadExtra(save);
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.store(SAVE_KEY, DisplaySave.CODEC, this.toSave());
        List<OtherItem> others = this.others();
        if (!others.isEmpty()) {
            output.store(OTHERS_KEY, OtherItem.CODEC.listOf(), others);
        }
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        DisplaySave save = input.read(SAVE_KEY, DisplaySave.CODEC).orElse(DisplaySave.empty());
        List<OtherItem> others = input.read(OTHERS_KEY, OtherItem.CODEC.listOf()).orElse(List.of());
        this.apply(save, others);
        input.read(VIEWS_KEY, SlotView.CODEC.listOf()).ifPresent(list -> this.views = List.copyOf(list));
        this.loadClientExtra(input);
        if (this.level != null && this.level.isClientSide()) {
            // A finish or trim change re-tints baked faces: re-mesh this block.
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 8);
        }
    }

    /** Client-only fields from the update tag (cabinet pool pips); the base has none. */
    protected void loadClientExtra(ValueInput input) {
    }

    /** Client-only fields written into the update tag; the base has none. */
    protected void writeClientExtra(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        DisplaySave.CODEC.encodeStart(ops, this.toSave()).result().ifPresent(t -> tag.put(SAVE_KEY, t));
        List<OtherItem> others = this.others();
        if (!others.isEmpty()) {
            OtherItem.CODEC.listOf().encodeStart(ops, others).result().ifPresent(t -> tag.put(OTHERS_KEY, t));
        }
        SlotView.CODEC.listOf().encodeStart(ops, this.views).result().ifPresent(t -> tag.put(VIEWS_KEY, t));
        this.writeClientExtra(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ============================================================================================
    // Item components: a broken display keeps its finish
    // ============================================================================================

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        this.finish.ifPresent(f -> components.set(ModDataComponents.FINISH.get(), f));
    }

    //? if >=1.21.5 {
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.finish = Optional.ofNullable(components.get(ModDataComponents.FINISH.get()));
        this.applyItemComponents(components.get(ModDataComponents.TROPHY.get()));
    }
    //?} else {
    /*@Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        this.finish = Optional.ofNullable(components.get(ModDataComponents.FINISH.get()));
        this.applyItemComponents(components.get(ModDataComponents.TROPHY.get()));
    }
    *///?}

    /** The trophy block takes its stamp from the placed item; other displays ignore it. */
    protected void applyItemComponents(@Nullable TrophyStamp stamp) {
    }

    /** Holder for the "unknown trophy" id an unstamped trophy stack reports. */
    static final class TrophyIdsHolder {
        static final Identifier UNKNOWN = za.co.neroland.nerotrophies.registry.TrophyIds.id("unknown");

        private TrophyIdsHolder() {
        }
    }
}
