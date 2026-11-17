package za.co.neroland.nerotrophies.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TrimRules;
import za.co.neroland.nerotrophies.registry.ModItems;
import za.co.neroland.nerotrophies.registry.ModMenuTypes;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The Trophy Cabinet / Wall Cabinet menu (ART-PLAN §7.11): the cabinet's slots (trophies only), the player's
 * inventory, and two controls through vanilla {@code clickMenuButton} (decision 010 §1, ids in {@link CabinetRows}):
 * trim steps (the server checks the clicker's earned trims with {@link TrimRules}) and collection rows (bound against
 * the list captured at open; a click after a reload changed it is refused and the screen closes). Synced numbers only:
 * the bound row, the trim, and the pool's genuine-member mask. No name, no token, no identity.
 */
public class CabinetMenu extends AbstractContainerMenu {

    public static final int FLOOR_SLOTS = 6;
    public static final int WALL_SLOTS = 2;
    /** Data slots: 0 bound row (0 none, k = k-th collection, -1 unknown), 1 trim index, 2 present, 3 members, 4-7 mask. */
    public static final int DATA_COUNT = 8;
    public static final int MASK_BITS = 15;

    private final Container container;
    private final ContainerData data;
    private final boolean wall;
    @Nullable
    private final CabinetBlockEntity cabinet;
    @Nullable
    private final CabinetRows rows;

    /** Client: floor cabinet. */
    public static CabinetMenu floor(int id, Inventory inventory) {
        return new CabinetMenu(ModMenuTypes.TROPHY_CABINET.get(), id, inventory, new SimpleContainer(FLOOR_SLOTS),
                new SimpleContainerData(DATA_COUNT), false, null, null);
    }

    /** Client: wall cabinet. */
    public static CabinetMenu wall(int id, Inventory inventory) {
        return new CabinetMenu(ModMenuTypes.WALL_CABINET.get(), id, inventory, new SimpleContainer(WALL_SLOTS),
                new SimpleContainerData(DATA_COUNT), true, null, null);
    }

    @SuppressWarnings("this-escape")
    protected CabinetMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inventory,
            Container container, ContainerData data, boolean wall, @Nullable CabinetBlockEntity cabinet,
            @Nullable CabinetRows rows) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.wall = wall;
        this.cabinet = cabinet;
        this.rows = rows;
        if (wall) {
            this.addSlot(new TrophySlot(container, 0, 18, 30));
            this.addSlot(new TrophySlot(container, 1, 36, 30));
        } else {
            for (int i = 0; i < 3; i++) {
                this.addSlot(new TrophySlot(container, i, 9 + 18 * i, 39));
            }
            for (int i = 0; i < 3; i++) {
                this.addSlot(new TrophySlot(container, 3 + i, 9 + 18 * i, 21));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
        this.addDataSlots(data);
    }

    /** The provider the cabinet block opens through {@link MenuOpener}. */
    public static MenuProvider provider(CabinetBlockEntity cabinet) {
        boolean wall = cabinet.isWall();
        return new SimpleMenuProvider((id, inventory, player) -> {
            ServerLevel level = (ServerLevel) cabinet.getLevel();
            List<Identifier> ids = new ArrayList<>(level == null ? List.of()
                    : TrophyApi.definitions(level.getServer()).collections().keySet());
            CabinetRows rows = new CabinetRows(ids, TrophyDefinitions.generation());
            return new CabinetMenu(wall ? ModMenuTypes.WALL_CABINET.get() : ModMenuTypes.TROPHY_CABINET.get(), id,
                    inventory, new CabinetContainer(cabinet), new LiveData(cabinet, rows), wall, cabinet, rows);
        }, Component.translatable(wall ? "container.nerotrophies.wall_cabinet" : "container.nerotrophies.trophy_cabinet"));
    }

    public boolean isWall() {
        return this.wall;
    }

    public int slotCount() {
        return this.wall ? WALL_SLOTS : FLOOR_SLOTS;
    }

    /** The bound collection's row (1-based in id order), 0 for none, -1 when unknown. */
    public int boundRow() {
        return this.data.get(0);
    }

    /** Index of the applied trim in {@link Trims#ALL}. */
    public int trimIndex() {
        return this.data.get(1);
    }

    public int presentCount() {
        return this.data.get(2);
    }

    public int memberCount() {
        return this.data.get(3);
    }

    /** Whether member {@code i} of the bound collection is present genuinely in the pool. */
    public boolean memberPresent(int i) {
        if (i < 0 || i >= 4 * MASK_BITS) {
            return false;
        }
        return ((this.data.get(4 + i / MASK_BITS) >> (i % MASK_BITS)) & 1) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.cabinet == null || this.rows == null || !(player instanceof ServerPlayer server)
                || !(this.cabinet.getLevel() instanceof ServerLevel level) || !this.stillValid(player)) {
            return false;
        }
        CabinetRows.Action action = this.rows.resolve(id, TrophyDefinitions.generation());
        switch (action.kind()) {
            case PREVIOUS_TRIM, NEXT_TRIM -> {
                TrophyState state = TrophyState.get(level.getServer());
                String current = this.cabinet.trim();
                List<String> offered = offeredTrims(state, server, current);
                int at = Math.max(0, offered.indexOf(current));
                int step = action.kind() == CabinetRows.Kind.NEXT_TRIM ? 1 : -1;
                String next = offered.get(Math.floorMod(at + step, offered.size()));
                if (next.equals(current) || !TrimRules.canApply(state, server.getUUID(), next, current)) {
                    return false;
                }
                this.cabinet.trimPool(next);
                return true;
            }
            case UNBIND -> {
                this.cabinet.bindPool(Optional.empty());
                return true;
            }
            case BIND -> {
                this.cabinet.bindPool(action.collection());
                return true;
            }
            case STALE -> {
                server.sendSystemMessage(Component.translatable("nerotrophies.cabinet.stale"), true);
                server.closeContainer();
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** The trims the clicker may step through: standard, those they earned, and the one applied now. */
    static List<String> offeredTrims(TrophyState state, ServerPlayer player, String current) {
        List<String> offered = new ArrayList<>();
        for (Trims.Trim trim : Trims.ALL) {
            String id = trim.id();
            if (TrophyIds.STANDARD_TRIM.equals(id) || id.equals(current)
                    || TrimRules.canApply(state, player.getUUID(), id, TrophyIds.STANDARD_TRIM)) {
                offered.add(id);
            }
        }
        return offered;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int size = this.slotCount();
        if (index < size) {
            if (!this.moveItemStackTo(stack, size, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ModItems.TROPHY.get()) || !this.moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    /** A cabinet slot: one trophy. */
    static final class TrophySlot extends Slot {
        TrophySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.TROPHY.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Server-side data read live from the cabinet. */
    static final class LiveData implements ContainerData {
        private final CabinetBlockEntity cabinet;
        private final CabinetRows rows;

        LiveData(CabinetBlockEntity cabinet, CabinetRows rows) {
            this.cabinet = cabinet;
            this.rows = rows;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> {
                    int button = this.rows.buttonFor(this.cabinet.collection());
                    yield button < 0 ? -1 : button - CabinetRows.UNBIND;
                }
                case 1 -> Trims.indexOf(this.cabinet.trim());
                case 2 -> this.cabinet.presentCount();
                case 3 -> this.cabinet.memberCount();
                case 4, 5, 6, 7 -> (int) ((this.cabinet.mask() >>> (MASK_BITS * (index - 4))) & ((1 << MASK_BITS) - 1));
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
