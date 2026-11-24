package za.co.neroland.nerotrophies.client.render;

import java.util.function.BiConsumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
//?}
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import za.co.neroland.nerotrophies.client.render.compat.BlockEntityRenderState;
import za.co.neroland.nerotrophies.client.render.compat.CameraRenderState;
import za.co.neroland.nerotrophies.client.render.compat.ItemStackRenderState;
import za.co.neroland.nerotrophies.client.render.compat.LegacyBlockEntityRenderer;
import za.co.neroland.nerotrophies.client.render.compat.ModelFeatureRenderer;
import za.co.neroland.nerotrophies.client.render.compat.SubmitNodeCollector;
*///?}
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;

/**
 * The shared base of NeroTrophies' block-entity renderers. Every per-line difference of the renderer API (the 1.21.9
 * extract / submit split, the 26.1 block-model submission, the 26.3 pose rotation) lives here and in
 * {@code compat/}; concrete renderers draw through {@link Out}, {@link Miniature} and {@link BlockShot} only.
 *
 * <p>Culling: never a blanket {@code true}. A renderer draws only when it has something to show, the camera is within
 * {@link #range()} blocks, and the block lies in front of the first-person view; NeoForge additionally
 * frustum-tests {@link #getRenderBoundingBox}. Client only.
 */
//? if >=1.21.9 {
public abstract class DisplayRenderer<T extends DisplayBlockEntity, S extends DisplayRenderer.State>
        implements BlockEntityRenderer<T, S> {
//?} else {
/*public abstract class DisplayRenderer<T extends DisplayBlockEntity, S extends DisplayRenderer.State>
        implements LegacyBlockEntityRenderer<T, S> {
*///?}

    public static final int FULL_BRIGHT = 0x00F000F0;

    /** Per-frame state common to every display renderer. */
    public static class State extends BlockEntityRenderState {
        /** Quarter turns from the north-facing layout to the block's facing. */
        public int turns;
        /** Squared distance from the camera to the block centre. */
        public double distanceSq;
        /** Level time in ticks (with the partial tick) for slow motions. */
        public float time;
    }

    /** Draw range in blocks. */
    protected abstract int range();

    /** Whether the block entity holds anything this renderer would draw. */
    protected abstract boolean hasContent(T be);

    /** Fills {@code state} from the block entity (client thread). */
    protected abstract void extract(T be, S state, Level level);

    /** Draws {@code state}, in a pose already turned to the block's facing (north-facing layout, 1 unit = 1 block). */
    protected abstract void draw(S state, PoseStack pose, Out out);

    @Override
    public int getViewDistance() {
        return this.range();
    }

    /** NeoForge frustum hook (inert elsewhere, so no {@code @Override}). */
    public AABB getRenderBoundingBox(T be) {
        return new AABB(be.getBlockPos()).inflate(0.25D, 0.75D, 0.25D);
    }

    @Override
    public boolean shouldRender(T be, Vec3 camera) {
        if (!this.hasContent(be)) {
            return false;
        }
        BlockPos pos = be.getBlockPos();
        double dx = pos.getX() + 0.5D - camera.x;
        double dy = pos.getY() + 0.5D - camera.y;
        double dz = pos.getZ() + 0.5D - camera.z;
        double sq = dx * dx + dy * dy + dz * dz;
        int range = this.range();
        if (sq > (double) range * range) {
            return false;
        }
        return sq < 9.0D || inView(dx, dy, dz, sq);
    }

    /** A cheap view-cone test (first person only): the block is not behind the camera. */
    private static boolean inView(double dx, double dy, double dz, double sq) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return true;
        }
        Vec3 look = mc.player.getViewVector(1.0F);
        double dot = (look.x * dx + look.y * dy + look.z * dz) / Math.sqrt(sq);
        // Generous: anything within ~120 degrees of the look direction, so wide FOVs never pop.
        return dot > -0.5D;
    }

    @Override
    public void extractRenderState(T be, S state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        //? if >=1.21.9 {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, camera, breaking);
        //?}
        BlockState block = be.getBlockState();
        Direction facing = block.hasProperty(DisplayProps.FACING) ? block.getValue(DisplayProps.FACING)
                : Direction.NORTH;
        state.turns = quarterTurns(facing);
        BlockPos pos = be.getBlockPos();
        state.distanceSq = camera.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        Level level = be.getLevel();
        state.time = level == null ? 0.0F : (level.getGameTime() % 24000L) + partialTick;
        if (level != null) {
            this.extract(be, state, level);
        }
    }

    @Override
    public void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        turn(pose, state.turns);
        this.draw(state, pose, new Out(collector));
        pose.popPose();
    }

    // ============================================================================================
    // Helpers
    // ============================================================================================

    public static int quarterTurns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    /** Turns the pose about the block centre so a north-facing layout faces {@code turns} quarter turns. */
    public static void turn(PoseStack pose, int turns) {
        pose.translate(0.5F, 0.0F, 0.5F);
        rotateY(pose, -90.0F * turns);
        pose.translate(-0.5F, 0.0F, -0.5F);
    }

    public static void rotateY(PoseStack pose, float degrees) {
        //? if >=26.3 {
        /*pose.rotate(Axis.YP.rotationDegrees(degrees));
        *///?} else {
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        //?}
    }

    public static void rotateX(PoseStack pose, float degrees) {
        //? if >=26.3 {
        /*pose.rotate(Axis.XP.rotationDegrees(degrees));
        *///?} else {
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
        //?}
    }

    /** {@code light} with its block light raised to at least {@code floor} (a genuine trophy never goes dark). */
    public static int withBlockFloor(int light, int floor) {
        int block = (light >> 4) & 0xF;
        int sky = (light >> 20) & 0xF;
        return (Math.max(block, floor) << 4) | (sky << 20);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            int argb, int light, float nz) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, nz);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z)
                .color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose, 0.0F, 0.0F, nz)
                .endVertex();
        *///?}
    }

    /**
     * A textured, double-sided quad on the plane {@code z} (pixels), spanning {@code x0..x1, y0..y1} (pixels), with
     * texture coordinates {@code u0..u1} left to right and {@code v0..v1} top to bottom, seen from the front (-z).
     */
    public static void quad(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z,
            float u0, float v0, float u1, float v1, int argb, int light) {
        float ax = x0 / 16F;
        float bx = x1 / 16F;
        float ay = y0 / 16F;
        float by = y1 / 16F;
        float zz = z / 16F;
        // Front (-z): viewer's left is +x, so u0 sits at x1.
        vertex(c, pose, bx, by, zz, u0, v0, argb, light, -1F);
        vertex(c, pose, ax, by, zz, u1, v0, argb, light, -1F);
        vertex(c, pose, ax, ay, zz, u1, v1, argb, light, -1F);
        vertex(c, pose, bx, ay, zz, u0, v1, argb, light, -1F);
        // Back.
        vertex(c, pose, bx, ay, zz, u0, v1, argb, light, 1F);
        vertex(c, pose, ax, ay, zz, u1, v1, argb, light, 1F);
        vertex(c, pose, ax, by, zz, u1, v0, argb, light, 1F);
        vertex(c, pose, bx, by, zz, u0, v0, argb, light, 1F);
    }

    /** A flat colour box (pixels): front, top, both sides. Textured from a white texel. */
    public static void box(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1,
            float z1, int argb, int light) {
        quad(c, pose, x0, y0, x1, y1, z0, 0F, 0F, 1F, 1F, argb, light);
        quad(c, pose, x0, y0, x1, y1, z1, 0F, 0F, 1F, 1F, argb, light);
        // top
        float y = y1 / 16F;
        vertexTop(c, pose, x0, z0, x1, z1, y, argb, light);
        side(c, pose, x0, y0, y1, z0, z1, argb, light);
        side(c, pose, x1, y0, y1, z0, z1, argb, light);
    }

    private static void vertexTop(VertexConsumer c, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float y,
            int argb, int light) {
        float ax = x0 / 16F;
        float bx = x1 / 16F;
        float az = z0 / 16F;
        float bz = z1 / 16F;
        vertex(c, pose, ax, y, az, 0F, 0F, argb, light, 0F);
        vertex(c, pose, ax, y, bz, 0F, 1F, argb, light, 0F);
        vertex(c, pose, bx, y, bz, 1F, 1F, argb, light, 0F);
        vertex(c, pose, bx, y, az, 1F, 0F, argb, light, 0F);
        vertex(c, pose, bx, y, az, 1F, 0F, argb, light, 0F);
        vertex(c, pose, bx, y, bz, 1F, 1F, argb, light, 0F);
        vertex(c, pose, ax, y, bz, 0F, 1F, argb, light, 0F);
        vertex(c, pose, ax, y, az, 0F, 0F, argb, light, 0F);
    }

    private static void side(VertexConsumer c, PoseStack.Pose pose, float x, float y0, float y1, float z0, float z1,
            int argb, int light) {
        float xx = x / 16F;
        float ay = y0 / 16F;
        float by = y1 / 16F;
        float az = z0 / 16F;
        float bz = z1 / 16F;
        vertex(c, pose, xx, ay, az, 0F, 1F, argb, light, 0F);
        vertex(c, pose, xx, by, az, 0F, 0F, argb, light, 0F);
        vertex(c, pose, xx, by, bz, 1F, 0F, argb, light, 0F);
        vertex(c, pose, xx, ay, bz, 1F, 1F, argb, light, 0F);
        vertex(c, pose, xx, ay, bz, 1F, 1F, argb, light, 0F);
        vertex(c, pose, xx, by, bz, 1F, 0F, argb, light, 0F);
        vertex(c, pose, xx, by, az, 0F, 0F, argb, light, 0F);
        vertex(c, pose, xx, ay, az, 0F, 1F, argb, light, 0F);
    }

    // ============================================================================================
    // The drawing seam
    // ============================================================================================

    /** What a renderer draws into, the same on every MC line. */
    public static final class Out {

        private final SubmitNodeCollector collector;

        Out(SubmitNodeCollector collector) {
            this.collector = collector;
        }

        /** Custom geometry with {@code type}; the body gets the pose and the consumer. */
        public void geometry(PoseStack pose, RenderType type, BiConsumer<PoseStack.Pose, VertexConsumer> body) {
            this.collector.order(0).submitCustomGeometry(pose, type, body::accept);
        }

        /** Text at {@code (x, y)} in the pose's units, no shadow. */
        public void text(PoseStack pose, float x, float y, FormattedCharSequence text, int colour, int light) {
            this.collector.submitText(pose, x, y, text, false, Font.DisplayMode.POLYGON_OFFSET, light, colour, 0, 0);
        }

        public void item(PoseStack pose, Miniature miniature, int light) {
            if (miniature.ready) {
                miniature.state.submit(pose, this.collector, light, OverlayTexture.NO_OVERLAY, 0);
            }
        }

        /** A block model in a unit cube at the pose's origin. */
        public void block(PoseStack pose, BlockShot shot, int light) {
            if (!shot.ready) {
                return;
            }
            //? if >=26.1 {
            shot.state.submit(pose, this.collector, light, OverlayTexture.NO_OVERLAY, 0);
            //?} else {
            /*this.collector.submitBlock(pose, shot.blockState, light, OverlayTexture.NO_OVERLAY, 0);
            *///?}
        }
    }

    /** An item drawn in the {@code FIXED} context (the trophy's icon, a pedestal's item). */
    public static final class Miniature {

        final ItemStackRenderState state = new ItemStackRenderState();
        boolean ready;

        public void update(ItemStack stack, Level level, int seed) {
            this.ready = !stack.isEmpty();
            if (!this.ready) {
                return;
            }
            //? if >=1.21.9 {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(this.state, stack,
                    ItemDisplayContext.FIXED, level, null, seed);
            //?} else {
            /*this.state.update(stack, ItemDisplayContext.FIXED, level, seed);
            *///?}
        }

        public void clear() {
            this.ready = false;
        }

        public boolean ready() {
            return this.ready;
        }
    }

    /** A block state's baked model drawn in a renderer (a trophy on a pedestal or in a cabinet). */
    public static final class BlockShot {

        //? if >=26.1 {
        private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
        private static BlockModelResolver resolver;
        final BlockModelRenderState state = new BlockModelRenderState();

        /** Vanilla's resolver over the client's model manager (built once; the getter on Minecraft is NeoForge's). */
        private static BlockModelResolver resolver() {
            if (resolver == null) {
                resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
            }
            return resolver;
        }
        //?}
        BlockState blockState;
        boolean ready;

        public void update(BlockState blockState) {
            this.blockState = blockState;
            this.ready = true;
            //? if >=26.1 {
            resolver().update(this.state, blockState, CONTEXT);
            //?}
        }

        public void clear() {
            this.ready = false;
        }
    }
}
