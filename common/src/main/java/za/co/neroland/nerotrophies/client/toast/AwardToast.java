package za.co.neroland.nerotrophies.client.toast;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.client.net.ClientTrophyRecord;
import za.co.neroland.nerotrophies.client.render.TrophyColours;
import za.co.neroland.nerotrophies.client.render.TrophyDraw;
import za.co.neroland.nerotrophies.client.render.TrophyLook;
import za.co.neroland.nerotrophies.client.screen.Gui;
import za.co.neroland.nerotrophies.network.AwardToastPayload;
import za.co.neroland.nerotrophies.network.RecordPayload;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The award toast: the vanilla 160x32 footprint, drawn in code so the frame
 * can animate, with the medallions, pip cells and sheen mask from {@code trophy_toast.png}. Three kinds: a trophy earned (per tier), a collection completed, and the retroactive summary (no sound, no sheen).
 * The title and icon are looked up at draw time from the player's own record, which arrives just after the toast, so
 * a hidden trophy's real title only ever reaches its earner. Client only.
 */
public final class AwardToast implements Toast {

    public static final Identifier SHEET = TrophyIds.id("textures/gui/trophy_toast.png");
    private static final int SHEET_W = 128;
    private static final int SHEET_H = 64;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;
    private static final int TEXT_WIDTH = 116;

    private static final int BRONZE_LT = 0xFFD08A52;
    private static final int SILVER_LT = 0xFFC9D3DF;
    private static final int GOLD_LT = 0xFFFFD54A;
    private static final int GOLD_HI = 0xFFFFF3B0;

    private final byte kind;
    private final Identifier id;
    @Nullable
    private final TrophyTier tier;
    private final int count;
    private final long durationMs;

    //? if >=1.21.2 {
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    //?}

    public AwardToast(AwardToastPayload payload) {
        this.kind = payload.kind();
        this.id = payload.id();
        this.tier = payload.kind() == AwardToastPayload.KIND_TROPHY ? payload.tier() : null;
        this.count = payload.count();
        this.durationMs = duration(this.kind, this.tier);
    }

    /** Time on screen: bronze, silver 5 s; gold 6 s; nero 7 s; collection 7 s; summary 5 s. */
    static long duration(byte kind, @Nullable TrophyTier tier) {
        if (kind == AwardToastPayload.KIND_COLLECTION) {
            return 7000L;
        }
        if (kind == AwardToastPayload.KIND_RETRO_SUMMARY || tier == null) {
            return 5000L;
        }
        return switch (tier) {
            case BRONZE, SILVER -> 5000L;
            case GOLD -> 6000L;
            case NERO -> 7000L;
        };
    }

    // ---- the per-line Toast contract ------------------------------------------------------------------------------

    //? if >=1.21.2 {
    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(net.minecraft.client.gui.components.toasts.ToastManager manager, long visibleFor) {
        this.visibility = visibleFor >= this.durationMs * manager.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
    //?}

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, Font font, long visibleFor) {
        this.draw(g, font, visibleFor);
    }
    //?} else if >=1.21.2 {
    /*@Override
    public void render(GuiGraphicsExtractor g, Font font, long visibleFor) {
        this.draw(g, font, visibleFor);
    }
    *///?} else {
    /*@Override
    public Toast.Visibility render(GuiGraphicsExtractor g,
            net.minecraft.client.gui.components.toasts.ToastComponent component, long visibleFor) {
        this.draw(g, component.getMinecraft().font, visibleFor);
        return visibleFor >= this.durationMs * component.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
    *///?}

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    // ---- drawing --------------------------------------------------------------------------------------------------

    private void draw(GuiGraphicsExtractor g, Font font, long t) {
        boolean trophy = this.kind == AwardToastPayload.KIND_TROPHY;
        boolean collection = this.kind == AwardToastPayload.KIND_COLLECTION;
        boolean nero = trophy && this.tier == TrophyTier.NERO;
        boolean gold = trophy && this.tier == TrophyTier.GOLD;

        g.fill(0, 0, WIDTH, HEIGHT, TrophyColours.PANEL);

        // Gold and nero: one diagonal sheen crossing left to right in 0.6 s, starting at 0.4 s.
        if ((gold || nero) && t >= 400L && t <= 1000L) {
            float p = (t - 400L) / 600F;
            int x = 1 + Math.round(p * (WIDTH - 12));
            Gui.blit(g, SHEET, x, 0, 0, 32, 10, 32, SHEET_W, SHEET_H, nero ? TrophyColours.PRISM_MID : GOLD_HI);
        }

        // Frame and left bar.
        int frame;
        if (nero) {
            frame = prismCycle(t);
        } else if (collection) {
            frame = TrophyColours.ACCENT;
        } else if (trophy) {
            frame = TrophyColours.base(this.tier);
        } else {
            frame = TrophyColours.PANEL_EDGE;
        }
        Gui.frame(g, 0, 0, WIDTH, HEIGHT, frame);
        if (nero) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                float f = (HEIGHT - 2 - y) / (float) (HEIGHT - 3); // bottom PRISM_A -> top PRISM_B
                g.fill(1, y, 4, y + 1, lerp(TrophyColours.PRISM_A, TrophyColours.PRISM_B, f));
            }
        } else {
            g.fill(1, 1, 4, HEIGHT - 1, this.barColour());
        }

        // Medallion and icon.
        int mu;
        int mv = 0;
        if (trophy) {
            mu = (this.tier == null ? 0 : this.tier.ordinal()) * 24;
        } else if (collection) {
            mu = 96;
        } else {
            mu = 16;
            mv = 32;
        }
        Gui.blit(g, SHEET, 6, 4, mu, mv, 24, 24, SHEET_W, SHEET_H, 0xFFFFFFFF);
        ItemStack icon = this.icon();
        if (!icon.isEmpty()) {
            Gui.item(g, icon, 10, 6);
        }

        // Pips (tier) or the awarded trim's crown (collection).
        int pipsWidth = 0;
        if (trophy && this.tier != null) {
            pipsWidth = this.pips(g);
        } else if (collection) {
            pipsWidth = this.crown(g);
        }

        // Lines.
        Component line1;
        int ink;
        if (trophy) {
            String tierName = this.tier == null ? TrophyTier.BRONZE.getSerializedName() : this.tier.getSerializedName();
            line1 = Component.translatable("nerotrophies.toast.earned." + tierName);
            ink = TrophyColours.ink(this.tier);
        } else if (collection) {
            line1 = Component.translatable("nerotrophies.toast.collection");
            ink = TrophyColours.ACCENT_BRIGHT;
        } else {
            line1 = Component.translatable("nerotrophies.toast.recorded", this.count);
            ink = TrophyColours.TEXT;
        }
        int line1Width = TEXT_WIDTH - (pipsWidth > 0 ? pipsWidth + 4 : 0);
        Gui.text(g, font, Gui.fit(font, line1.getString(), line1Width), 36, 7, ink);
        Gui.text(g, font, Gui.fit(font, this.line2().getString(), TEXT_WIDTH), 36, 18,
                trophy || collection ? TrophyColours.TEXT : TrophyColours.TEXT_2);
    }

    private int barColour() {
        if (this.kind == AwardToastPayload.KIND_COLLECTION) {
            return TrophyColours.ACCENT_BRIGHT;
        }
        if (this.kind == AwardToastPayload.KIND_RETRO_SUMMARY || this.tier == null) {
            return TrophyColours.ACCENT;
        }
        return switch (this.tier) {
            case BRONZE -> BRONZE_LT;
            case SILVER -> SILVER_LT;
            case GOLD -> GOLD_LT;
            case NERO -> TrophyColours.PRISM_A;
        };
    }

    /** Right-aligned at (154, 4): one, two or three 3x3 dots, or nero's single hex mark. Returns the width used. */
    private int pips(GuiGraphicsExtractor g) {
        int ink = TrophyColours.ink(this.tier);
        if (this.tier == TrophyTier.NERO) {
            Gui.blit(g, SHEET, 154, 4, 4, 24, 3, 3, SHEET_W, SHEET_H, ink);
            return 3;
        }
        int n = this.tier.ordinal() + 1;
        for (int k = 0; k < n; k++) {
            Gui.blit(g, SHEET, 154 - k * 4, 4, 0, 24, 3, 3, SHEET_W, SHEET_H, ink);
        }
        return n * 4 - 1;
    }

    /** The awarded trim's crown (14x4 from its trim sheet, accent tinted) at (140, 4). Returns the width used. */
    private int crown(GuiGraphicsExtractor g) {
        RecordPayload.CollectionView view = this.collectionView();
        if (view == null) {
            return 0;
        }
        Trims.Trim trim = Trims.byId(view.trim());
        String style = trim.style().getSerializedName();
        Gui.blit(g, TrophyIds.id("textures/block/trim_" + style + ".png"), 140, 4, 2, 2, 14, 4, 16, 16, 0xFFFFFFFF);
        boolean prism = "prism".equals(style);
        Gui.blit(g, TrophyIds.id("textures/block/trim_" + style + "_accent.png"), 140, 4, 2, 2, 14, 4, 16,
                prism ? 128 : 16, prism ? 0xFFFFFFFF : 0xFF000000 | trim.accent());
        return 14;
    }

    private Component line2() {
        if (this.kind == AwardToastPayload.KIND_TROPHY) {
            return Component.translatable(TrophyLook.titleKey(this.id));
        }
        if (this.kind == AwardToastPayload.KIND_COLLECTION) {
            return Component.translatable(TrophyLook.collectionTitleKey(this.id));
        }
        return Component.translatable("nerotrophies.toast.recorded.hint");
    }

    private ItemStack icon() {
        if (this.kind == AwardToastPayload.KIND_TROPHY) {
            RecordPayload.Entry entry = TrophyLook.ownEntry(this.id);
            return entry == null || entry.masked() ? ItemStack.EMPTY : TrophyDraw.iconStack(entry.icon());
        }
        if (this.kind == AwardToastPayload.KIND_COLLECTION) {
            RecordPayload.CollectionView view = this.collectionView();
            return view == null ? ItemStack.EMPTY : TrophyDraw.iconStack(view.icon());
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private RecordPayload.CollectionView collectionView() {
        RecordPayload record = ClientTrophyRecord.current();
        if (record == null) {
            return null;
        }
        for (RecordPayload.CollectionView view : record.collections()) {
            if (view.id().equals(this.id)) {
                return view;
            }
        }
        return null;
    }

    /** The nero frame: PRISM_A to PRISM_B and back, period 2 s. */
    private static int prismCycle(long t) {
        float phase = (t % 2000L) / 2000F;
        float tri = 1F - Math.abs(phase * 2F - 1F);
        return lerp(TrophyColours.PRISM_A, TrophyColours.PRISM_B, tri);
    }

    private static int lerp(int a, int b, float f) {
        float c = Math.max(0F, Math.min(1F, f));
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * c);
        int gr = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * c);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * c);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }
}
