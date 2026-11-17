package za.co.neroland.nerotrophies.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * Drawing helpers for NeroTrophies' screens, and the one place the per-line GUI calls differ (blit, item, tooltip).
 * The family's flat procedural chrome: fills and 1 px frames in the art palette, sheet blits for glyphs and
 * emblems. Client only.
 */
public final class Gui {

    public static final Identifier BOOK_SHEET = TrophyIds.id("textures/gui/trophy_book.png");
    public static final Identifier CABINET_SHEET = TrophyIds.id("textures/gui/trophy_cabinet.png");
    public static final Identifier EMBLEMS = TrophyIds.id("textures/gui/mod_emblems.png");

    private Gui() {
    }

    /** A filled rect with a 1 px frame. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int edge) {
        g.fill(x, y, x + w, y + h, edge);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    /** A 1 px frame. */
    public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int edge) {
        g.fill(x, y, x + w, y + 1, edge);
        g.fill(x, y + h - 1, x + w, y + h, edge);
        g.fill(x, y, x + 1, y + h, edge);
        g.fill(x + w - 1, y, x + w, y + h, edge);
    }

    /** A 1 px frame, left half one colour and right half another (the nero tier). */
    public static void splitFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right) {
        int mid = x + w / 2;
        g.fill(x, y, mid, y + 1, left);
        g.fill(x, y + h - 1, mid, y + h, left);
        g.fill(x, y, x + 1, y + h, left);
        g.fill(mid, y, x + w, y + 1, right);
        g.fill(mid, y + h - 1, x + w, y + h, right);
        g.fill(x + w - 1, y, x + w, y + h, right);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int colour) {
        g.text(font, text, x, y, colour, false);
    }

    public static void text(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int colour) {
        g.text(font, text, x, y, colour, false);
    }

    /** {@code text} cut to {@code width} px with an ellipsis. */
    public static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    public static void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        g.item(stack, x, y);
        //?} else {
        /*g.renderItem(stack, x, y);
        *///?}
    }

    /** Blits {@code (u, v, w, h)} of a 256x256 sheet at 1:1, tinted. */
    public static void blit(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int u, int v, int w, int h,
            int argb) {
        blit(g, sheet, x, y, u, v, w, h, 256, 256, argb);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier sheet, int x, int y, int u, int v, int w, int h,
            int texW, int texH, int argb) {
        //? if >=1.21.6 {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sheet, x, y, u, v, w, h, w, h, texW, texH,
                argb);
        //?} else if >=1.21.2 {
        /*g.blit(net.minecraft.client.renderer.RenderType::guiTextured, sheet, x, y, u, v, w, h, w, h, texW, texH,
                argb);
        *///?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(sheet, x, y, w, h, (float) u, (float) v, w, h, texW, texH);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        *///?}
    }

    /** One of the 24 16x16 mod emblems (ART-PLAN §7.9). */
    public static void emblem(GuiGraphicsExtractor g, int cell, int x, int y) {
        int c = Math.max(0, Math.min(23, cell));
        blit(g, EMBLEMS, x, y, (c % 8) * 16, (c / 8) * 16, 16, 16, 128, 48, 0xFFFFFFFF);
    }

    /** Queues a wrapped tooltip at the mouse. */
    public static void tooltip(GuiGraphicsExtractor g, Font font, List<Component> lines, int x, int y) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(font.split(line, 220));
        }
        //? if >=1.21.6 {
        g.setTooltipForNextFrame(wrapped, x, y);
        //?} else {
        /*g.renderTooltip(font, wrapped, x, y);
        *///?}
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
