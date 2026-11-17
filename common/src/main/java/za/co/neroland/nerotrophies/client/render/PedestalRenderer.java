package za.co.neroland.nerotrophies.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PlateState;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * The Display Pedestal: a trophy at 0.75 (its tier model and live parts) or any other item, turning once in
 * 16 s on the turntable, and the larger plaque on the shaft (name, ornament, or the pewter cover). Draws within
 * 32 blocks, the plaque within 16. Client only.
 */
public class PedestalRenderer extends DisplayRenderer<PedestalBlockEntity, PedestalRenderer.S> {

    private static final double PLAQUE_RANGE = 16.0D;

    public static final class S extends DisplayRenderer.State {
        final TrophyDraw.Captured trophy = new TrophyDraw.Captured();
        final Miniature other = new Miniature();
        boolean otherFlat = true;
    }

    @Override
    public S createRenderState() {
        return new S();
    }

    @Override
    protected int range() {
        return 32;
    }

    @Override
    protected boolean hasContent(PedestalBlockEntity be) {
        return !be.getItem(0).isEmpty();
    }

    @Override
    protected void extract(PedestalBlockEntity be, S state, Level level) {
        ItemStack stack = be.getItem(0);
        TrophyStamp stamp = DisplayBlockEntity.stampOf(stack);
        int seed = (int) be.getBlockPos().asLong();
        state.trophy.capture(stamp, be.view(0), level, seed, true);
        if (stamp == null && !stack.isEmpty()) {
            state.other.update(stack, level, seed);
            state.otherFlat = !(stack.getItem() instanceof BlockItem);
        } else {
            state.other.clear();
        }
    }

    @Override
    protected void draw(S state, PoseStack pose, Out out) {
        float turn = (state.time / 320.0F) * 360.0F;
        pose.pushPose();
        pose.translate(0.5F, 15.0F / 16.0F, 0.5F);
        rotateY(pose, turn);
        if (state.trophy.present()) {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(-0.5F, 0.0F, -0.5F);
            TrophyDraw.full(pose, out, state.trophy, state.time, state.lightCoords, state.distanceSq / 0.5625D,
                    TrophyDraw.PLATE_RANGE);
        } else if (state.other.ready()) {
            float size = state.otherFlat ? 0.5F : 0.4F;
            pose.translate(0.0F, size / 2.0F, 0.0F);
            pose.scale(size, size, size);
            out.item(pose, state.other, state.lightCoords);
        }
        pose.popPose();
        if (state.trophy.present() && state.distanceSq <= PLAQUE_RANGE * PLAQUE_RANGE) {
            this.plaque(state, pose, out);
        }
    }

    /** The shaft plaque: plate x 1..15, y 7..11 on the plane z 3; text area 12 x 3 px. */
    private void plaque(S state, PoseStack pose, Out out) {
        TrophyLook.Look look = state.trophy.look;
        if (look == null) {
            return;
        }
        TrophyTier tier = look.tierOrBronze();
        float z = 3.0F - 0.16F;
        PlateState plate = look.plate();
        if ((plate == PlateState.NAMED || plate == PlateState.GALLERY) && look.name() != null) {
            TrophyDraw.text(pose, out, look.name(), 8.0F, 9.0F, z, 0.75F, 1.0F / 72.0F, TrophyColours.ink(tier));
        } else if (plate == PlateState.BLANK) {
            TrophyDraw.ornament(pose, out, 3.0F, 8.0F, z, tier);
        } else if (plate == PlateState.REPLICA) {
            int light = state.lightCoords;
            out.geometry(pose, TrophyDraw.translucent(TrophyDraw.COVER_PEDESTAL), (p, c) ->
                    DisplayRenderer.quad(c, p, 1F, 7F, 15F, 11F, z, 0F, 0F, 1F, 1F, 0xFFFFFFFF, light));
        }
    }
}
