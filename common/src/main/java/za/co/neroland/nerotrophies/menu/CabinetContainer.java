package za.co.neroland.nerotrophies.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayContents;

/**
 * The server-side view of a cabinet's slots for its menu: one item per slot. A trophy is stored with its stamp only;
 * any other item is stored whole, with every component.
 */
final class CabinetContainer implements Container {

    private final CabinetBlockEntity cabinet;

    CabinetContainer(CabinetBlockEntity cabinet) {
        this.cabinet = cabinet;
    }

    @Override
    public int getContainerSize() {
        return this.cabinet.size();
    }

    @Override
    public boolean isEmpty() {
        return this.cabinet.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.cabinet.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count <= 0 ? ItemStack.EMPTY : this.cabinet.removeItem(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.cabinet.removeItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.cabinet.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : DisplayContents.normalise(stack));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return this.cabinet.accepts(slot, stack);
    }

    @Override
    public void setChanged() {
        this.cabinet.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this.cabinet, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.cabinet.size(); i++) {
            this.cabinet.removeItem(i);
        }
    }
}
