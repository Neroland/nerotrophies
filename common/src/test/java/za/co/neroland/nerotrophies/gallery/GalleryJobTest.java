package za.co.neroland.nerotrophies.gallery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

/**
 * {@code gallery clear} puts the world back exactly (snapshot compare of the box and a margin around it), the build
 * captures everything before it changes anything, never spills a container, stays inside its box, and is budgeted
 * over ticks.
 */
class GalleryJobTest {

    private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");

    private static void run(GalleryJob job, FakeGalleryWorld world, int maxOps, Runnable onCaptured) {
        int guard = 0;
        while (!job.done()) {
            int before = world.ops;
            if (job.step(world, maxOps, Long.MAX_VALUE, () -> 0L)) {
                onCaptured.run();
            }
            assertTrue(world.ops - before <= maxOps, "a slice never exceeds its budget");
            assertTrue(++guard < 1_000_000, "job finishes");
        }
    }

    private static void seed(FakeGalleryWorld world, GalleryFrame frame) {
        // A varied "before": grass, water, a full chest and a sign inside the box, and things just outside it.
        world.cells.put(frame.pos(3, 0, 1), new GalleryWorld.Prior(FakeGalleryWorld.state("minecraft:chest"),
                Optional.of(FakeGalleryWorld.container(5))));
        world.cells.put(frame.pos(10, -14, 2), new GalleryWorld.Prior(FakeGalleryWorld.state("minecraft:oak_sign"),
                Optional.of(FakeGalleryWorld.state("text"))));
        for (int w = -21; w <= 21; w++) {
            world.cells.put(frame.pos(5, w, 0), new GalleryWorld.Prior(FakeGalleryWorld.state("minecraft:grass_block"),
                    Optional.empty()));
            world.cells.put(frame.pos(6, w, 1), new GalleryWorld.Prior(FakeGalleryWorld.state("minecraft:water"),
                    Optional.empty()));
        }
        world.cells.put(frame.pos(-1, 0, 1), new GalleryWorld.Prior(FakeGalleryWorld.state("minecraft:chest"),
                Optional.of(FakeGalleryWorld.container(2))));
    }

    @Test
    void clearLeavesTheAreaIdentical() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryFixtures.shippedCatalogue());
        GalleryFrame frame = GalleryFrame.inFrontOf(new BlockPos(100, 64, -40), Direction.EAST, plan.length());
        FakeGalleryWorld world = new FakeGalleryWorld();
        seed(world, frame);
        BlockPos a = frame.pos(-3, -24, -2);
        BlockPos b = frame.pos(plan.length() + 2, 24, GalleryFrame.HEIGHT + 2);
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), b.getY(), Math.max(a.getZ(), b.getZ()));
        Map<BlockPos, GalleryWorld.Prior> before = world.snapshot(min, max);

        GalleryJob build = GalleryJob.build(OVERWORLD, frame, plan);
        world.expectedCells = frame.cells();
        GallerySnapshot[] saved = new GallerySnapshot[1];
        run(build, world, 500, () -> saved[0] = build.snapshot());
        assertEquals(0, world.placesBeforeCaptureDone, "every cell is captured before the first block changes");
        assertEquals(0, world.spilled, "a container is emptied before it is replaced");
        assertTrue(build.ticks() > 1, "a hall this size is spread over several ticks");
        assertNotEquals(before, world.snapshot(min, max), "the hall was built");
        java.util.Set<BlockPos> box = new java.util.HashSet<>();
        for (int i = 0; i < frame.cells(); i++) {
            box.add(frame.pos(i));
        }
        for (BlockPos pos : world.written) {
            assertTrue(box.contains(pos), "nothing outside the box is touched: " + pos);
        }

        // The snapshot is what a restart would read back.
        GallerySnapshot reloaded = roundTrip(saved[0]);
        GalleryJob clear = GalleryJob.clear(reloaded);
        run(clear, world, 777, () -> { });
        assertEquals(before, world.snapshot(min, max), "gallery clear restores every cell, block entities included");
        assertEquals(6, reloaded.plates().size());
    }

    @Test
    void anInterruptedClearCanSimplyRunAgain() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryCatalogue.of(GalleryFixtures.snapshot(new int[] {3, 2}, 2)));
        GalleryFrame frame = GalleryFrame.inFrontOf(BlockPos.ZERO, Direction.SOUTH, plan.length());
        FakeGalleryWorld world = new FakeGalleryWorld();
        seed(world, frame);
        BlockPos a = frame.pos(0, -21, 0);
        BlockPos b = frame.pos(plan.length() - 1, 21, 7);
        Map<BlockPos, GalleryWorld.Prior> before = world.snapshot(a, b);
        GalleryJob build = GalleryJob.build(OVERWORLD, frame, plan);
        run(build, world, 300, () -> { });
        GalleryJob partial = GalleryJob.clear(build.snapshot());
        partial.step(world, 4000, Long.MAX_VALUE, () -> 0L);
        partial.step(world, 4000, Long.MAX_VALUE, () -> 0L);
        run(GalleryJob.clear(build.snapshot()), world, 1000, () -> { });
        assertEquals(before, world.snapshot(a, b));
    }

    @Test
    void theTimeBudgetEndsASlice() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryCatalogue.of(GalleryFixtures.snapshot(new int[] {3}, 1)));
        GalleryFrame frame = GalleryFrame.inFrontOf(BlockPos.ZERO, Direction.NORTH, plan.length());
        FakeGalleryWorld world = new FakeGalleryWorld();
        GalleryJob job = GalleryJob.build(OVERWORLD, frame, plan);
        long[] now = {0L};
        job.step(world, Integer.MAX_VALUE, 10L, () -> now[0]++);
        assertTrue(world.ops <= 11, "stops once the clock passes the budget: " + world.ops);
    }

    @Test
    void theSnapshotSurvivesSaving() {
        GalleryPlan plan = GalleryFixtures.plan(GalleryCatalogue.of(GalleryFixtures.snapshot(new int[] {1}, 0)));
        GalleryFrame frame = GalleryFrame.inFrontOf(new BlockPos(-7, 70, 12), Direction.WEST, plan.length());
        FakeGalleryWorld world = new FakeGalleryWorld();
        seed(world, frame);
        GalleryJob build = GalleryJob.build(OVERWORLD, frame, plan);
        while (build.phase() == GalleryJob.Phase.CAPTURE) {
            build.step(world, 10_000, Long.MAX_VALUE, () -> 0L);
        }
        GallerySnapshot s = build.snapshot();
        GallerySnapshot back = roundTrip(s);
        assertEquals(s.dimension(), back.dimension());
        assertEquals(s.origin(), back.origin());
        assertEquals(s.forward(), back.forward());
        assertEquals(s.length(), back.length());
        assertEquals(s.palette(), back.palette());
        assertArrayEquals(s.states(), back.states());
        assertEquals(s.blockEntities(), back.blockEntities());
        assertEquals(s.plates(), back.plates());
        for (int i = 0; i < frame.cells(); i++) {
            assertEquals(s.prior(i), back.prior(i));
        }
    }

    private static GallerySnapshot roundTrip(GallerySnapshot s) {
        Tag tag = GallerySnapshot.CODEC.encodeStart(NbtOps.INSTANCE, s).getOrThrow();
        return GallerySnapshot.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }
}
