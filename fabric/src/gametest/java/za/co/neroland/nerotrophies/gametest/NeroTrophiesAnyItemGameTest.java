package za.co.neroland.nerotrophies.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyCollection;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayContents;
import za.co.neroland.nerotrophies.blockentity.DisplayIndex;
import za.co.neroland.nerotrophies.blockentity.DisplaySync;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.ClaimService;
import za.co.neroland.nerotrophies.engine.TrophyTokens;
import za.co.neroland.nerotrophies.registry.ModBlocks;
import za.co.neroland.nerotrophies.registry.ModDataComponents;

/**
 * The Display Pedestal, both cabinets and the Trophy Plaque take any item (Fabric 26.3 game test, never shipped). A
 * plain item keeps every component through save, load, client sync and breaking; it never counts toward a collection,
 * never reaches the token lists name sync reads, gives comparator 1 on a pedestal; an item too large to sync whole
 * reaches clients by id only while the server keeps it intact. The trophy block stays trophy-only.
 */
public class NeroTrophiesAnyItemGameTest {

    private static RuntimeException fail(GameTestHelper h, String message) {
        return h.assertionException(Component.literal(message));
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        if (!ok) {
            throw fail(h, message);
        }
    }

    /** A sword with a custom name, damage and an enchantment. */
    private static ItemStack sword(ServerLevel level) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
        stack.setDamageValue(17);
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
                3);
        return stack;
    }

    /** A shulker box with contents. */
    private static ItemStack box() {
        ItemStack stack = new ItemStack(Items.SHULKER_BOX);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(
                List.of(new ItemStack(Items.DIAMOND, 5), new ItemStack(Items.TORCH, 12))));
        return stack;
    }

    /** A paper carrying {@code bytes} of custom data. */
    private static ItemStack heavy(int bytes, String tag) {
        ItemStack stack = new ItemStack(Items.PAPER);
        CompoundTag data = new CompoundTag();
        data.putString("blob", tag + "x".repeat(bytes));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    /** Inserts {@code stack} the way a player does (accept check, one item, normalised). */
    private static void insert(GameTestHelper h, DisplayBlockEntity be, int slot, ItemStack stack) {
        check(h, be.accepts(slot, stack), be.getType() + " refuses " + stack);
        be.setItem(slot, DisplayContents.normalise(stack));
    }

    private static DisplayBlockEntity place(GameTestHelper h, BlockPos rel, Block block) {
        h.setBlock(rel, block);
        return h.getBlockEntity(rel, DisplayBlockEntity.class);
    }

    /** A detached block entity of the same kind, loaded from {@code tag} (a save, or what a client receives). */
    private static DisplayBlockEntity reload(ServerLevel level, DisplayBlockEntity be, CompoundTag tag) {
        BlockEntity fresh = be.getType().create(be.getBlockPos(), be.getBlockState());
        if (!(fresh instanceof DisplayBlockEntity display)) {
            throw new IllegalStateException("not a display: " + be.getType());
        }
        display.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        return display;
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return ItemStack.matches(a.copyWithCount(1), b);
    }

    /** Breaks the display at {@code rel} and returns (and removes) what it dropped. */
    private static List<ItemStack> breakAndCollect(GameTestHelper h, BlockPos rel) {
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(rel);
        level.destroyBlock(abs, true);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(1.5D));
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity drop : drops) {
            out.add(drop.getItem().copy());
            drop.discard();
        }
        return out;
    }

    // ---- 1. a plain item keeps every component through save, load, sync and break --------------------------------

    @GameTest
    public void plainItemsRoundTripAndComeBackOut(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack sword = sword(level);
        ItemStack box = box();
        ItemStack apple = new ItemStack(Items.GOLDEN_APPLE);
        ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.CUSTOM_NAME, Component.literal("Field notes"));

        DisplayBlockEntity pedestal = place(h, new BlockPos(1, 1, 1), ModBlocks.DISPLAY_PEDESTAL.get());
        DisplayBlockEntity cabinet = place(h, new BlockPos(4, 1, 1), ModBlocks.TROPHY_CABINET.get());
        DisplayBlockEntity wall = place(h, new BlockPos(1, 1, 4), ModBlocks.WALL_CABINET.get());
        DisplayBlockEntity plaque = place(h, new BlockPos(4, 1, 4), ModBlocks.TROPHY_PLAQUE.get());
        insert(h, pedestal, 0, sword);
        insert(h, cabinet, 0, box);
        insert(h, cabinet, 3, sword);
        insert(h, cabinet, 5, apple);
        insert(h, wall, 1, book);
        insert(h, plaque, 0, sword);

        DisplayBlockEntity trophyBlock = place(h, new BlockPos(6, 1, 6), ModBlocks.TROPHY.get());
        check(h, !trophyBlock.accepts(0, sword), "the trophy block takes only a trophy");
        check(h, !pedestal.accepts(0, ItemStack.EMPTY), "an empty stack is never accepted");

        for (DisplayBlockEntity be : List.of(pedestal, cabinet, wall, plaque)) {
            check(h, be.stamps().isEmpty() && be.tokens().length == 0,
                    be.getType() + " reports a plain item as a stamp");
            check(h, DisplayIndex.stampsAt(level, be.getBlockPos()).isEmpty()
                    && DisplayIndex.tokensAt(level, be.getBlockPos()).length == 0,
                    be.getType() + ": a plain item reached the display index token lists");
            check(h, be.views().isEmpty(), be.getType() + " sends a trophy view for a plain item");
            DisplayBlockEntity saved = reload(level, be, be.saveWithFullMetadata(level.registryAccess()));
            DisplayBlockEntity synced = reload(level, be, be.getUpdateTag(level.registryAccess()));
            for (int slot = 0; slot < be.size(); slot++) {
                ItemStack held = be.getItem(slot);
                check(h, same(held, saved.getItem(slot)), be.getType() + " slot " + slot + " save/load lost data: "
                        + held + " -> " + saved.getItem(slot));
                check(h, same(held, synced.getItem(slot)), be.getType() + " slot " + slot + " client sync lost data: "
                        + held + " -> " + synced.getItem(slot));
            }
        }
        check(h, same(sword, pedestal.getItem(0)) && same(box, cabinet.getItem(0)) && same(book, wall.getItem(1))
                && same(sword, plaque.getItem(0)), "an inserted item lost a component on the way in");
        check(h, ((PedestalBlockEntity) pedestal).signal() == 1, "a plain item on a pedestal gives comparator 1");

        // Breaking any display returns everything inside it, components intact.
        record Expect(BlockPos rel, List<ItemStack> items) {
        }
        List<Expect> expected = List.of(new Expect(new BlockPos(1, 1, 1), List.of(sword)),
                new Expect(new BlockPos(4, 1, 1), List.of(box, sword, apple)),
                new Expect(new BlockPos(1, 1, 4), List.of(book)),
                new Expect(new BlockPos(4, 1, 4), List.of(sword)));
        for (Expect e : expected) {
            List<ItemStack> drops = breakAndCollect(h, e.rel());
            for (ItemStack want : e.items()) {
                int match = -1;
                for (int i = 0; i < drops.size() && match < 0; i++) {
                    if (same(want, drops.get(i))) {
                        match = i;
                    }
                }
                check(h, match >= 0,
                        "breaking at " + e.rel() + " did not return " + want + " intact; dropped " + drops);
                drops.remove(match);
            }
        }
        h.succeed();
    }

    // ---- 2. plain items never count toward a collection -----------------------------------------------------------

    @GameTest
    public void plainItemsNeverCount(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        Optional<TrophyCollection> pick = TrophyApi.definitions(server).collections().values().stream()
                .filter(c -> c.members().size() >= 2).findFirst();
        check(h, pick.isPresent(), "precondition: a collection with two members exists");
        TrophyCollection collection = pick.get();
        Identifier first = collection.members().get(0);
        Identifier second = collection.members().get(1);
        UUID earner = UUID.fromString("5eed0000-0000-4000-8000-000000000c01");
        TrophyState state = TrophyState.get(server);
        if (state.hasRecord(earner)) {
            state.erase(earner); // a left-over record from an earlier run in the same (persisted) test world
        }
        long t1 = TrophyTokens.mint(state);
        state.award(earner, first, System.currentTimeMillis(), t1);
        long t2 = TrophyTokens.mint(state);
        state.award(earner, second, System.currentTimeMillis(), t2);

        CabinetBlockEntity cabinet =
                (CabinetBlockEntity) place(h, new BlockPos(2, 1, 2), ModBlocks.TROPHY_CABINET.get());
        cabinet.bindPool(Optional.of(collection.id()));
        insert(h, cabinet, 0, ClaimService.stamped(first, t1));
        // A non-trophy item carrying a genuine member's stamp component is still not a trophy.
        ItemStack forged = new ItemStack(Items.STICK);
        forged.set(ModDataComponents.TROPHY.get(), new TrophyStamp(second, t2));
        insert(h, cabinet, 1, forged);
        insert(h, cabinet, 2, sword(level));
        insert(h, cabinet, 3, new ItemStack(Items.NETHER_STAR));
        cabinet.refreshRow();

        check(h, cabinet.presentCount() == 1, "present count " + cabinet.presentCount() + " (want 1: the trophy only)");
        check(h, cabinet.stamps().size() == 1 && cabinet.stamps().get(0).token() == t1, "stamps: " + cabinet.stamps());
        check(h, Arrays.equals(cabinet.tokens(), new long[] {t1}), "tokens: " + Arrays.toString(cabinet.tokens()));
        check(h, Arrays.equals(DisplayIndex.tokensAt(level, cabinet.getBlockPos()), new long[] {t1}),
                "the display index lists a plain item's token");
        check(h, same(forged, cabinet.getItem(1)), "the stick lost its components");
        check(h, cabinet.view(1).isEmpty() && cabinet.view(2).isEmpty(), "a plain item got a trophy view");

        breakAndCollect(h, new BlockPos(2, 1, 2));
        state.erase(earner);
        h.succeed();
    }

    // ---- 3. an item too large to sync whole reaches clients by id only; the server keeps it all -------------------

    @GameTest
    public void oversizedItemsSyncByIdOnly(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        DisplayBlockEntity pedestal = place(h, new BlockPos(1, 1, 1), ModBlocks.DISPLAY_PEDESTAL.get());
        ItemStack big = heavy(DisplaySync.ITEM_MAX_BYTES + 4096, "big");
        insert(h, pedestal, 0, big);
        DisplayBlockEntity synced = reload(level, pedestal, pedestal.getUpdateTag(level.registryAccess()));
        DisplayBlockEntity saved = reload(level, pedestal, pedestal.saveWithFullMetadata(level.registryAccess()));
        check(h, synced.getItem(0).is(Items.PAPER) && !synced.getItem(0).has(DataComponents.CUSTOM_DATA),
                "an oversized item was synced with its data: " + synced.getItem(0));
        check(h, same(big, saved.getItem(0)), "the server's save lost the oversized item's data");
        check(h, same(big, pedestal.getItem(0)), "the server's copy lost data");

        DisplayBlockEntity cabinet = place(h, new BlockPos(4, 1, 1), ModBlocks.TROPHY_CABINET.get());
        List<ItemStack> six = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack item = heavy(DisplaySync.ITEM_MAX_BYTES - 512, "slot" + i);
            six.add(item);
            insert(h, cabinet, i, item);
        }
        CompoundTag update = cabinet.getUpdateTag(level.registryAccess());
        check(h, update.sizeInBytes() <= DisplaySync.DISPLAY_MAX_BYTES + 4096,
                "update tag of a full cabinet is " + update.sizeInBytes() + " bytes");
        DisplayBlockEntity cabinetSynced = reload(level, cabinet, update);
        DisplayBlockEntity cabinetSaved = reload(level, cabinet, cabinet.saveWithFullMetadata(level.registryAccess()));
        for (int i = 0; i < 6; i++) {
            check(h, cabinetSynced.getItem(i).is(Items.PAPER), "slot " + i + " missing on the client");
            check(h, same(six.get(i), cabinetSaved.getItem(i)), "slot " + i + " lost data in the save");
        }
        breakAndCollect(h, new BlockPos(1, 1, 1));
        breakAndCollect(h, new BlockPos(4, 1, 1));
        h.succeed();
    }
}
