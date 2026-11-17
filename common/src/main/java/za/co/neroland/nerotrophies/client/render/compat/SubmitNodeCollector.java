package za.co.neroland.nerotrophies.client.render.compat;

// Stand-in for 1.21.9's SubmitNodeCollector below 1.21.9 (see LegacyBlockEntityRenderer): geometry, text and block
// models are drawn immediately into the frame's MultiBufferSource. The order index only sorts submissions on 1.21.9+,
// so it is ignored here. Compiled only below 1.21.9.
//? if <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;

public final class SubmitNodeCollector {

    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return this.buffers;
    }

    public SubmitNodeCollector order(int order) {
        return this;
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), this.buffers.getBuffer(type));
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow,
            Font.DisplayMode mode, int light, int color, int background, int outline) {
        Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), this.buffers,
                mode, background, light);
    }

    public void submitBlock(PoseStack poseStack, BlockState state, int light, int overlay, int outline) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, this.buffers, light, overlay);
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }
}
*///?}
