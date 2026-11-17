package za.co.neroland.nerotrophies.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.client.net.ClientTrophyRecord;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.network.RecordPayload;

/**
 * Trophy Cabinet and Wall Cabinet: each held trophy at slot scale (its tier model and live parts, names within 6
 * blocks), any other held item as its normal item model standing in its slot (it never counts), a ghost outline in empty slots for the bound collection's missing members, and the completion pips on the
 * crown, spread over the pool (seven per crown). Pips come from the server's member bitmask only; a ghost shows a
 * hidden member as "?" with no tier or icon. Draws within 32 blocks. Client only.
 */
public class CabinetRenderer extends DisplayRenderer<CabinetBlockEntity, CabinetRenderer.S> {

    private static final double NAME_RANGE = 6.0D;
    private static final float[] FLOOR_X = {3F, 8F, 13F, 3F, 8F, 13F};
    private static final float[] FLOOR_Y = {2F, 2F, 2F, 9F, 9F, 9F};
    private static final float[] WALL_X = {4.5F, 11.5F};
    private static final int PIPS_PER_CROWN = 7;

    /** A missing member drawn as a ghost: its tier (null = hidden / unknown). */
    record Ghost(int slot, @Nullable TrophyTier tier) {
    }

    public static final class S extends DisplayRenderer.State {
        boolean wall;
        final List<TrophyDraw.Captured> slots = new ArrayList<>();
        /** Plain (non-trophy) items per slot, and whether each is drawn flat (not a block item). */
        final List<Miniature> others = new ArrayList<>();
        final List<Boolean> othersFlat = new ArrayList<>();
        final List<Ghost> ghosts = new ArrayList<>();
        long mask;
        int members;
        int poolIndex;
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
    protected boolean hasContent(CabinetBlockEntity be) {
        return !be.isEmpty() || be.collection().isPresent();
    }

    @Override
    protected void extract(CabinetBlockEntity be, S state, Level level) {
        state.wall = be.isWall();
        while (state.slots.size() < be.size()) {
            state.slots.add(new TrophyDraw.Captured());
            state.others.add(new Miniature());
            state.othersFlat.add(Boolean.TRUE);
        }
        int seed = (int) be.getBlockPos().asLong();
        for (int i = 0; i < be.size(); i++) {
            ItemStack stack = be.getItem(i);
            TrophyStamp stamp = DisplayBlockEntity.stampOf(stack);
            state.slots.get(i).capture(stamp, be.view(i), level, seed + i, true);
            if (stamp == null && !stack.isEmpty()) {
                state.others.get(i).update(stack, level, seed + i);
                state.othersFlat.set(i, !(stack.getItem() instanceof BlockItem));
            } else {
                state.others.get(i).clear();
            }
        }
        state.mask = be.mask();
        state.members = be.memberCount();
        state.poolIndex = be.poolIndex();
        state.ghosts.clear();
        be.collection().ifPresent(id -> ghosts(be, id, state));
    }

    /** Missing members (in collection order) fill this cabinet's empty slots, offset by its place in the pool. */
    private static void ghosts(CabinetBlockEntity be, Identifier collection, S state) {
        RecordPayload record = ClientTrophyRecord.current();
        if (record == null) {
            return;
        }
        Optional<RecordPayload.CollectionView> view = record.collections().stream()
                .filter(c -> c.id().equals(collection)).findFirst();
        if (view.isEmpty()) {
            return;
        }
        List<Identifier> missing = new ArrayList<>();
        List<Identifier> members = view.get().members();
        for (int i = 0; i < members.size() && i < 63; i++) {
            if ((state.mask & (1L << i)) == 0) {
                missing.add(members.get(i));
            }
        }
        int next = state.poolIndex * be.size();
        for (int slot = 0; slot < be.size() && next < missing.size(); slot++) {
            if (!be.getItem(slot).isEmpty()) {
                continue;
            }
            RecordPayload.Entry entry = record.entry(missing.get(next++));
            TrophyTier tier = entry == null || entry.masked() ? null : entry.tier();
            state.ghosts.add(new Ghost(slot, tier));
        }
    }

    @Override
    protected void draw(S state, PoseStack pose, Out out) {
        float scale = state.wall ? 0.5F : 0.31F;
        for (int i = 0; i < state.slots.size(); i++) {
            TrophyDraw.Captured slot = state.slots.get(i);
            if (!slot.present()) {
                continue;
            }
            pose.pushPose();
            slotOrigin(pose, state.wall, i, scale);
            TrophyDraw.full(pose, out, slot, state.time, state.lightCoords, state.distanceSq / (scale * scale),
                    NAME_RANGE / scale);
            pose.popPose();
        }
        for (int i = 0; i < state.others.size(); i++) {
            Miniature other = state.others.get(i);
            if (!other.ready()) {
                continue;
            }
            float size = state.othersFlat.get(i) ? 0.7F : 0.55F;
            pose.pushPose();
            slotOrigin(pose, state.wall, i, scale);
            pose.translate(0.5F, size / 2.0F, 0.5F);
            pose.scale(size, size, size);
            out.item(pose, other, state.lightCoords);
            pose.popPose();
        }
        for (Ghost ghost : state.ghosts) {
            pose.pushPose();
            slotOrigin(pose, state.wall, ghost.slot(), scale);
            ghost(pose, out, ghost.tier(), state.lightCoords);
            pose.popPose();
        }
        if (state.members > 0) {
            this.pips(state, pose, out);
        }
    }

    /** Moves the pose to slot {@code i}'s trophy-local unit cube at {@code scale}. */
    private static void slotOrigin(PoseStack pose, boolean wall, int i, float scale) {
        float x = wall ? WALL_X[Math.min(i, 1)] : FLOOR_X[Math.min(i, 5)];
        float y = wall ? 3F : FLOOR_Y[Math.min(i, 5)];
        float z = wall ? 13F : 9F;
        pose.translate(x / 16F, y / 16F, z / 16F);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, 0.0F, -0.5F);
    }

    /**
     * A ghost: base and stage from {@code trophy_ghost.json} ({@link GhostModel}) as translucent scanline faces in tier
     * BASE (or GHOST_UNKNOWN) at 35 %, and the model's marker quad showing the tier's pip cell ("?" when hidden).
     */
    private static void ghost(PoseStack pose, Out out, @Nullable TrophyTier tier, int light) {
        GhostModel.Parts parts = GhostModel.parts();
        int colour = TrophyColours.alpha(tier == null ? TrophyColours.GHOST_UNKNOWN : TrophyColours.base(tier), 0.35F);
        out.geometry(pose, TrophyDraw.translucent(TrophyDraw.GHOST), (p, c) -> GhostModel.body(c, p, parts, colour,
                light));
        int cell = tier == null ? 4 : tier.ordinal();
        int marker = TrophyColours.alpha(tier == null ? TrophyColours.GHOST_UNKNOWN : TrophyColours.ink(tier), 0.6F);
        out.geometry(pose, TrophyDraw.translucent(TrophyDraw.PIPS), (p, c) ->
                DisplayRenderer.quad(c, p, parts.mx0(), parts.my0(), parts.mx1(), parts.my1(), parts.mz(), cell / 5F,
                        0F, (cell + 1) / 5F, 1F, marker, light));
    }

    /** Completion pips along the crown's middle row: lit for a genuine member present, dark for a missing one. */
    private void pips(S state, PoseStack pose, Out out) {
        int first = state.poolIndex * PIPS_PER_CROWN;
        int count = Math.min(PIPS_PER_CROWN, state.members - first);
        if (count <= 0) {
            return;
        }
        float y = state.wall ? 14F : 16F;
        float z = state.wall ? 8.4F : -0.6F;
        float width = count * 2F - 1F;
        float start = 8F - width / 2F;
        long mask = state.mask;
        out.geometry(pose, TrophyDraw.flat(), (p, c) -> {
            for (int k = 0; k < count; k++) {
                boolean lit = (mask & (1L << (first + k))) != 0;
                float x = start + k * 2F;
                DisplayRenderer.quad(c, p, x, y - 0.5F, x + 1F, y + 0.5F, z, 0F, 0F, 1F, 1F,
                        lit ? TrophyColours.ACCENT_BRIGHT : TrophyColours.STEEL_DK, lit ? FULL_BRIGHT : 0x00F00000);
            }
        });
    }
}
