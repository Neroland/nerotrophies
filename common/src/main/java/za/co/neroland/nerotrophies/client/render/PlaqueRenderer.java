package za.co.neroland.nerotrophies.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PlaqueBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PlateState;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * The Trophy Plaque: the icon in the medallion, the tier pips under it, the trophy title on the upper strip and the
 * earner on the lower strip (ornament when nameless, the pewter cover for a replica). Draws within 24 blocks, text
 * within 20. Client only.
 */
public class PlaqueRenderer extends DisplayRenderer<PlaqueBlockEntity, PlaqueRenderer.S> {

    private static final double TEXT_RANGE = 20.0D;

    public static final class S extends DisplayRenderer.State {
        final TrophyDraw.Captured trophy = new TrophyDraw.Captured();
        String title = "";
    }

    @Override
    public S createRenderState() {
        return new S();
    }

    @Override
    protected int range() {
        return 24;
    }

    @Override
    protected boolean hasContent(PlaqueBlockEntity be) {
        return !be.getItem(0).isEmpty();
    }

    @Override
    protected void extract(PlaqueBlockEntity be, S state, Level level) {
        TrophyStamp stamp = DisplayBlockEntity.stampOf(be.getItem(0));
        state.trophy.capture(stamp, be.view(0), level, (int) be.getBlockPos().asLong(), false);
        state.title = stamp == null ? "" : Component.translatable(TrophyLook.titleKey(stamp.trophy())).getString();
    }

    @Override
    protected void draw(S state, PoseStack pose, Out out) {
        TrophyLook.Look look = state.trophy.look;
        if (look == null) {
            return;
        }
        TrophyTier tier = look.tierOrBronze();
        boolean replica = look.replica();
        int light = replica ? state.lightCoords : withBlockFloor(state.lightCoords, 9);
        // The icon, flat in the medallion at (8, 12), 6 px across.
        pose.pushPose();
        pose.translate(0.5F, 12.0F / 16.0F, 13.9F / 16.0F);
        pose.scale(0.375F, 0.375F, 0.02F);
        out.item(pose, state.trophy.mini, light);
        pose.popPose();
        float z = 14.0F - 0.16F;
        if (!replica) {
            int cell = tier.ordinal();
            int ink = TrophyColours.ink(tier);
            out.geometry(pose, TrophyDraw.emissive(TrophyDraw.PIPS), (p, c) ->
                    DisplayRenderer.quad(c, p, 4F, 5F, 12F, 9F, z, cell / 5F, 0F, (cell + 1) / 5F, 1F, ink,
                            FULL_BRIGHT));
        }
        if (state.distanceSq > TEXT_RANGE * TEXT_RANGE) {
            return;
        }
        if (!state.title.isEmpty()) {
            TrophyDraw.text(pose, out, state.title, 8.0F, 4.0F, z, 0.75F, 1.0F / 96.0F,
                    replica ? TrophyColours.REPLICA_TEXT : TrophyColours.TEXT);
        }
        PlateState plate = look.plate();
        if ((plate == PlateState.NAMED || plate == PlateState.GALLERY) && look.name() != null) {
            TrophyDraw.text(pose, out, look.name(), 8.0F, 1.0F, z, 0.75F, 1.0F / 96.0F, TrophyColours.ink(tier));
        } else if (plate == PlateState.BLANK) {
            TrophyDraw.ornament(pose, out, 3.0F, 0.0F, z, tier);
        } else if (plate == PlateState.REPLICA) {
            int world = state.lightCoords;
            out.geometry(pose, TrophyDraw.translucent(TrophyDraw.COVER_PLAQUE), (p, c) ->
                    DisplayRenderer.quad(c, p, 1F, 0F, 15F, 2F, z, 0F, 0F, 1F, 1F, 0xFFFFFFFF, world));
        }
    }
}
