package za.co.neroland.nerotrophies.client.render;

import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.DisplayProps;
import za.co.neroland.nerotrophies.blockentity.PlateState;
import za.co.neroland.nerotrophies.blockentity.SlotView;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModBlocks;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Draws one trophy's live parts (the icon miniature, the nameplate, the nero ring) in a trophy-local unit cube facing
 * north, and optionally its tier model too (for a trophy held by a pedestal or a cabinet). Nameplate rules:
 * text only for a resolved name, the lit ornament for every nameless genuine state (byte-identical
 * whatever the cause), the pewter cover for a replica, nothing at all beyond the draw distance. Client only.
 */
public final class TrophyDraw {

    public static final Identifier BLANK = TrophyIds.id("textures/entity/nameplate_blank.png");
    public static final Identifier REPLICA_PLATE = TrophyIds.id("textures/entity/nameplate_replica.png");
    public static final Identifier COVER_PEDESTAL = TrophyIds.id("textures/entity/plate_cover_pedestal.png");
    public static final Identifier COVER_PLAQUE = TrophyIds.id("textures/entity/plate_cover_plaque.png");
    public static final Identifier PIPS = TrophyIds.id("textures/entity/tier_pips.png");
    public static final Identifier RING = TrophyIds.id("textures/entity/trophy_ring.png");
    public static final Identifier GHOST = TrophyIds.id("textures/block/trophy_ghost.png");
    public static final Identifier WHITE = Identifier.withDefaultNamespace("textures/misc/white.png");

    /** Nameplate draw distance for a placed trophy. */
    public static final double PLATE_RANGE = 12.0D;

    private TrophyDraw() {
    }

    /** One trophy's client data, captured at extract time. */
    public static final class Captured {
        public TrophyLook.Look look;
        public final DisplayRenderer.Miniature mini = new DisplayRenderer.Miniature();
        public final DisplayRenderer.BlockShot shot = new DisplayRenderer.BlockShot();
        public boolean flat = true;

        public boolean present() {
            return this.look != null;
        }

        public void clear() {
            this.look = null;
            this.mini.clear();
            this.shot.clear();
        }

        /** Captures {@code stamp}; with {@code model} also its tier model (pedestal, cabinet). */
        public void capture(@Nullable TrophyStamp stamp, Optional<SlotView> hint, Level level, int seed,
                boolean model) {
            if (stamp == null) {
                this.clear();
                return;
            }
            TrophyLook.Look look = TrophyLook.of(stamp, hint);
            this.look = look;
            ItemStack icon = iconStack(look.icon());
            this.flat = !(icon.getItem() instanceof BlockItem);
            this.mini.update(icon, level, seed);
            if (model) {
                BlockState state = ModBlocks.TROPHY.get().defaultBlockState()
                        .setValue(DisplayProps.TIER, look.tierOrBronze())
                        .setValue(DisplayProps.REPLICA, look.replica());
                this.shot.update(state);
            } else {
                this.shot.clear();
            }
        }
    }

    /** The icon item, or empty when it is unknown here (no miniature is drawn, never a missing-model cube). */
    public static ItemStack iconStack(@Nullable Identifier icon) {
        if (icon == null) {
            return ItemStack.EMPTY;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(icon);
        return item.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** The y (pixels) the miniature stands on, per tier. */
    public static float stageY(TrophyTier tier) {
        return switch (tier) {
            case BRONZE -> 5.0F;
            case SILVER -> 7.0F;
            case GOLD -> 6.0F;
            case NERO -> 8.0F;
        };
    }

    /**
     * Draws a held trophy: its tier model, then its live parts. The pose is the trophy's own unit cube (north-facing).
     * {@code platesWithin} is the squared distance under which the nameplate is drawn.
     */
    public static void full(PoseStack pose, DisplayRenderer.Out out, Captured c, float time, int light,
            double distanceSq, double plateRange) {
        if (c.look == null) {
            return;
        }
        int modelLight = c.look.replica() ? light : DisplayRenderer.withBlockFloor(light, 9);
        out.block(pose, c.shot, modelLight);
        live(pose, out, c, time, light, distanceSq, plateRange, false);
    }

    /** The miniature, the nero ring and the nameplate. {@code coverReplica}: draw the pewter cover too. */
    public static void live(PoseStack pose, DisplayRenderer.Out out, Captured c, float time, int light,
            double distanceSq, double plateRange, boolean coverReplica) {
        TrophyLook.Look look = c.look;
        if (look == null) {
            return;
        }
        TrophyTier tier = look.tierOrBronze();
        boolean genuine = !look.replica();
        int itemLight = genuine ? DisplayRenderer.withBlockFloor(light, 9) : light;
        float size = c.flat ? 0.5F : 0.4F;
        float bottom = stageY(tier) / 16.0F;
        float spin = 0.0F;
        if (genuine && tier == TrophyTier.GOLD) {
            spin = (time / 160.0F) * 360.0F;
        } else if (genuine && tier == TrophyTier.NERO) {
            spin = (time / 120.0F) * 360.0F;
            bottom += (float) Math.sin(time / 80.0F * Math.PI * 2.0D) * (0.5F / 16.0F);
        } else if (!genuine && tier == TrophyTier.NERO) {
            bottom = 6.0F / 16.0F;
        }
        pose.pushPose();
        pose.translate(0.5F, bottom + size / 2.0F, 0.5F);
        DisplayRenderer.rotateY(pose, spin);
        pose.scale(size, size, size);
        out.item(pose, c.mini, itemLight);
        pose.popPose();
        if (genuine && tier == TrophyTier.NERO) {
            ring(pose, out, time);
        }
        if (distanceSq <= plateRange * plateRange) {
            plate(pose, out, look, tier, light, coverReplica);
        }
    }

    /** The nero ring: one quad, tilted 15 degrees, turning against the miniature, teal near and violet far. */
    private static void ring(PoseStack pose, DisplayRenderer.Out out, float time) {
        pose.pushPose();
        pose.translate(0.5F, 12.0F / 16.0F, 0.5F);
        DisplayRenderer.rotateY(pose, -(time / 240.0F) * 360.0F);
        DisplayRenderer.rotateX(pose, 90.0F - 15.0F);
        pose.translate(-0.5F, -0.5F, 0.0F);
        out.geometry(pose, RenderTypes.entityTranslucentEmissive(RING), (p, c) -> {
            DisplayRenderer.quad(c, p, 1F, 8F, 15F, 15F, 0F, 0F, 0F, 1F, 0.5F, TrophyColours.PRISM_B,
                    DisplayRenderer.FULL_BRIGHT);
            DisplayRenderer.quad(c, p, 1F, 1F, 15F, 8F, 0F, 0F, 0.5F, 1F, 1F, TrophyColours.PRISM_A,
                    DisplayRenderer.FULL_BRIGHT);
        });
        pose.popPose();
    }

    /** The trophy nameplate: the 10x2 px text area on the name block's front (z 2), centred at x 8, y 2. */
    private static void plate(PoseStack pose, DisplayRenderer.Out out, TrophyLook.Look look, TrophyTier tier,
            int light, boolean coverReplica) {
        PlateState plate = look.plate();
        float z = 2.0F - 0.16F;
        switch (plate) {
            case NAMED, GALLERY -> {
                if (look.name() != null) {
                    text(pose, out, look.name(), 8.0F, 2.0F, z, 0.625F, 1.0F / 96.0F, TrophyColours.ink(tier));
                }
            }
            case BLANK -> ornament(pose, out, 3.0F, 1.0F, z, tier);
            case REPLICA -> {
                if (coverReplica) {
                    out.geometry(pose, RenderTypes.entityTranslucent(REPLICA_PLATE), (p, c) -> DisplayRenderer.quad(c,
                            p, 2F, 1F, 14F, 4F, z, 0F, 0F, 1F, 1F, 0xFFFFFFFF, light));
                }
            }
            default -> {
            }
        }
    }

    /** The lit blank ornament (10x2 px) with its lower-left corner at {@code (x, y)}. */
    public static void ornament(PoseStack pose, DisplayRenderer.Out out, float x, float y, float z, TrophyTier tier) {
        int colour = TrophyColours.scale(TrophyColours.ink(tier), 0.55F);
        out.geometry(pose, RenderTypes.entityTranslucentEmissive(BLANK), (p, c) -> DisplayRenderer.quad(c, p, x, y,
                x + 10F, y + 2F, z, 0F, 0F, 1F, 1F, colour, DisplayRenderer.FULL_BRIGHT));
    }

    /**
     * Full-bright, shadowless text centred at {@code (cx, cy)} (pixels) on the plane {@code z}, facing north, fitted
     * to {@code width} blocks at no more than {@code maxScale} block per font pixel and no less than 1/160.
     */
    public static void text(PoseStack pose, DisplayRenderer.Out out, String text, float cx, float cy, float z,
            float width, float maxScale, int colour) {
        Font font = Minecraft.getInstance().font;
        float floor = 1.0F / 160.0F;
        String shown = text;
        int w = font.width(shown);
        if (w * floor > width) {
            int room = (int) (width / floor) - font.width("…");
            shown = font.plainSubstrByWidth(text, Math.max(0, room)) + "…";
            w = font.width(shown);
        }
        float scale = Math.max(floor, Math.min(maxScale, width / Math.max(1, w)));
        FormattedCharSequence line = FormattedCharSequence.forward(shown, Style.EMPTY);
        pose.pushPose();
        pose.translate(cx / 16.0F, cy / 16.0F, z / 16.0F);
        DisplayRenderer.rotateY(pose, 180.0F);
        pose.scale(scale, -scale, scale);
        out.text(pose, -w / 2.0F, -4.0F, line, colour, DisplayRenderer.FULL_BRIGHT);
        pose.popPose();
    }

    /** The render type of a flat tinted quad. */
    public static RenderType flat() {
        return RenderTypes.entityTranslucent(WHITE);
    }

    /** A translucent, world-lit render type for {@code texture}. */
    public static RenderType translucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    /** A translucent, full-bright render type for {@code texture}. */
    public static RenderType emissive(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }
}
