package za.co.neroland.nerotrophies.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.trophy.TrophyApi;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyStatus;
import za.co.neroland.nerotrophies.gallery.GalleryCatalogue;
import za.co.neroland.nerotrophies.gallery.GalleryFrame;
import za.co.neroland.nerotrophies.gallery.GalleryJob;
import za.co.neroland.nerotrophies.gallery.GalleryLayout;
import za.co.neroland.nerotrophies.gallery.GalleryLevel;
import za.co.neroland.nerotrophies.gallery.GalleryPiece;
import za.co.neroland.nerotrophies.gallery.GalleryPlan;
import za.co.neroland.nerotrophies.gallery.GalleryStore;

/**
 * The gallery on a real game-test server (Fabric 26.3 only, never shipped): building the hall over real blocks
 * (a full chest included) and clearing it leaves every block state and block entity exactly as it was, with no item
 * dropped; the hall's trophies are display copies that never resolve to a player or reach name sync; and the command
 * refuses a console and a survival player.
 */
public class NeroTrophiesGalleryGameTest {

    private static RuntimeException fail(GameTestHelper h, String message) {
        return h.assertionException(Component.literal(message));
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        if (!ok) {
            throw fail(h, message);
        }
    }

    private record Cell(BlockState state, CompoundTag data) {
    }

    private static Map<BlockPos, Cell> read(ServerLevel level, GalleryFrame frame, int margin) {
        Map<BlockPos, Cell> out = new HashMap<>();
        BlockPos a = frame.pos(-margin, GalleryFrame.W_MIN - margin, -margin);
        BlockPos b = frame.pos(frame.length() - 1 + margin, GalleryFrame.W_MAX + margin,
                GalleryFrame.HEIGHT - 1 + margin);
        for (BlockPos pos : BlockPos.betweenClosed(a, b)) {
            BlockEntity be = level.getBlockEntity(pos);
            out.put(pos.immutable(), new Cell(level.getBlockState(pos),
                    be == null ? null : be.saveWithFullMetadata(level.registryAccess())));
        }
        return out;
    }

    private static void run(GalleryJob job, GalleryLevel world) {
        int guard = 0;
        while (!job.done() && guard++ < 100_000) {
            job.step(world, 4096, Long.MAX_VALUE, () -> 0L);
        }
    }

    @GameTest
    public void clearLeavesTheAreaIdentical(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        GalleryCatalogue catalogue = GalleryCatalogue.of(TrophyApi.definitions(server));
        check(h, !catalogue.isEmpty(), "the test server loads NeroTrophies' own trophies");
        GalleryPlan plan = GalleryLayout.plan(catalogue).plan().orElseThrow();
        // Well away from the test structures, over the flat test world.
        BlockPos feet = h.absolutePos(new BlockPos(0, 1, 0)).offset(640, 0, 0);
        GalleryFrame frame = GalleryFrame.inFrontOf(feet, Direction.SOUTH, plan.length());
        BlockPos chestPos = frame.pos(3, 1, 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        ((ChestBlockEntity) level.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.DIAMOND, 7));
        level.setBlock(frame.pos(9, -14, 1), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        level.setBlock(frame.pos(2, -19, 2), Blocks.WATER.defaultBlockState(), 3);
        Map<BlockPos, Cell> before = read(level, frame, 1);
        TrophyState state = TrophyState.get(server);
        int records = state.recordCount();

        GalleryLevel world = new GalleryLevel(level);
        GalleryJob build = GalleryJob.build(level.dimension().identifier(), frame, plan);
        run(build, world);
        check(h, build.done(), "the build finishes");
        check(h, level.getBlockState(chestPos).isAir(), "the chest was replaced by the hall's floor space");

        // The hall's trophies are display copies: reserved tokens, never sent through name sync, never genuine.
        int checked = 0;
        for (int i = 0; i < plan.cells().length; i++) {
            if (plan.cells()[i] instanceof GalleryPiece.Pedestal p && p.stamp().isPresent()) {
                DisplayBlockEntity display = (DisplayBlockEntity) level.getBlockEntity(frame.pos(i));
                check(h, display != null, "a pedestal stands where the plan says");
                List<TrophyStamp> stamps = display.stamps();
                check(h, stamps.size() == 1 && stamps.get(0).token() == TrophyStamp.GALLERY_TOKEN,
                        "a gallery pedestal holds one display copy");
                check(h, display.tokens().length == 0, "a display copy is never offered to name sync");
                check(h, TokenResolver.resolve(state, stamps.get(0).trophy(), stamps.get(0).token()).status()
                        == TrophyStatus.GALLERY, "a display copy resolves as a gallery copy");
                checked++;
            }
        }
        check(h, checked == catalogue.trophyCount(), "every loaded trophy is on a pedestal");
        check(h, state.recordCount() == records, "building the hall records nothing");

        run(GalleryJob.clear(build.snapshot()), world);
        Map<BlockPos, Cell> after = read(level, frame, 1);
        for (Map.Entry<BlockPos, Cell> e : before.entrySet()) {
            Cell was = e.getValue();
            Cell now = after.get(e.getKey());
            check(h, was.state().equals(now.state()), "block state restored at " + e.getKey());
            check(h, java.util.Objects.equals(was.data(), now.data()), "block entity restored at " + e.getKey());
        }
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        check(h, chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 7, "the chest kept its items");
        BlockPos a = frame.pos(0, GalleryFrame.W_MIN, 0);
        BlockPos b = frame.pos(frame.length() - 1, GalleryFrame.W_MAX, GalleryFrame.HEIGHT - 1);
        AABB box = new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, b.getY() + 1, Math.max(a.getZ(), b.getZ()) + 1).inflate(3);
        check(h, level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), "nothing dropped");
        check(h, state.recordCount() == records, "clearing the hall records nothing");
        h.succeed();
    }

    @GameTest
    public void theCommandRefusesTheConsoleAndSurvivalPlayers(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        check(h, refused(server, server.createCommandSourceStack()), "the console is refused");
        // Operator rights from the console source, so only the player and creative checks can refuse.
        ServerPlayer survival = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        check(h, !survival.isCreative(), "the mock player is in survival");
        check(h, refused(server, server.createCommandSourceStack().withEntity(survival)),
                "a survival player is refused");
        check(h, GalleryStore.get(server).snapshot().isEmpty(), "no hall was started");
        h.succeed();
    }

    private static boolean refused(MinecraftServer server, net.minecraft.commands.CommandSourceStack source) {
        try {
            return server.getCommands().getDispatcher().execute("nerotrophies gallery", source) == 0;
        } catch (CommandSyntaxException notPermitted) {
            return true;
        }
    }
}
