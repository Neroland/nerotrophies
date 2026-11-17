package za.co.neroland.nerotrophies.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.level.Level;

import za.co.neroland.nerotrophies.blockentity.TrophyBlockEntity;

/**
 * A placed trophy's live parts over its baked tier model: the icon miniature (still, spinning or floating by tier),
 * the nero ring, and the nameplate (name, ornament, or nothing for a replica, whose cover is baked in its model).
 * Draws within 48 blocks, the plate within 12. Client only.
 */
public class TrophyRenderer extends DisplayRenderer<TrophyBlockEntity, TrophyRenderer.S> {

    public static final class S extends DisplayRenderer.State {
        final TrophyDraw.Captured trophy = new TrophyDraw.Captured();
    }

    @Override
    public S createRenderState() {
        return new S();
    }

    @Override
    protected int range() {
        return 48;
    }

    @Override
    protected boolean hasContent(TrophyBlockEntity be) {
        return be.stamp() != null;
    }

    @Override
    protected void extract(TrophyBlockEntity be, S state, Level level) {
        state.trophy.capture(be.stamp(), be.view(0), level, (int) be.getBlockPos().asLong(), false);
    }

    @Override
    protected void draw(S state, PoseStack pose, Out out) {
        TrophyDraw.live(pose, out, state.trophy, state.time, state.lightCoords, state.distanceSq,
                TrophyDraw.PLATE_RANGE, false);
    }
}
