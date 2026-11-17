package za.co.neroland.nerotrophies.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;

/** Small helpers over a display's slots. */
public final class DisplayContents {

    private DisplayContents() {
    }

    /** Every non-empty slot, in slot order, each exactly once; the slots are left empty. */
    public static <T> List<T> drain(List<T> slots, Predicate<T> isEmpty, Supplier<T> empty) {
        List<T> out = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            T value = slots.get(i);
            if (value != null && !isEmpty.test(value)) {
                out.add(value);
            }
            slots.set(i, empty.get());
        }
        return out;
    }

    /** {@link #drain(List, Predicate, Supplier)} for item stacks. */
    public static List<ItemStack> drain(List<ItemStack> slots) {
        return drain(slots, ItemStack::isEmpty, () -> ItemStack.EMPTY);
    }

    /** A trophy item carrying exactly {@code stamp}. */
    public static ItemStack stack(TrophyStamp stamp) {
        ItemStack stack = new ItemStack(ModItems.TROPHY.get());
        stack.set(ModDataComponents.TROPHY.get(), stamp);
        return stack;
    }

    /**
     * One item of {@code stack} as a display stores it: a trophy keeps only its stamp (an anvil name or other component
     * is not stored); any other item is kept whole, every component included.
     */
    public static ItemStack normalise(ItemStack stack) {
        TrophyStamp stamp = DisplayBlockEntity.stampOf(stack);
        if (stamp != null) {
            return stack(stamp);
        }
        return stack.copyWithCount(1);
    }
}
