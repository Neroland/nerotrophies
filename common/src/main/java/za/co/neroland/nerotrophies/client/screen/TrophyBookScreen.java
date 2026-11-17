package za.co.neroland.nerotrophies.client.screen;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.DisplaySounds;
import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;
import za.co.neroland.nerotrophies.client.net.ClientTrophyRecord;
import za.co.neroland.nerotrophies.client.render.TrophyColours;
import za.co.neroland.nerotrophies.client.render.TrophyDraw;
import za.co.neroland.nerotrophies.network.RecordPayload;
import za.co.neroland.nerotrophies.registry.ModDataComponents;
import za.co.neroland.nerotrophies.registry.ModItems;

/**
 * The Trophy Book: one responsive hull; a header with overall progress and four tier chips that are
 * both counters and filters; a rail of mod tabs (only categories with a loaded trophy) and a pinned Collections row;
 * a grid of cards (hidden unearned ones as "???", never their tier); a detail strip with Claim / Re-claim (re-claim
 * needs a second click within 3 s); the Collections view with "Use on cabinets"; and the two privacy checkboxes. It
 * shows the viewer's own record only, never another player's. Client only.
 */
public class TrophyBookScreen extends Screen {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final long ARM_MILLIS = 3000L;
    /** Emblem cells by category. */
    private static final Map<String, Integer> EMBLEMS = Map.ofEntries(Map.entry("nerolandcore", 2),
            Map.entry("minecraft", 2), Map.entry("nerospace", 3), Map.entry("nerotech", 4), Map.entry("neropower", 5),
            Map.entry("nerologistics", 6), Map.entry("neroeconomy", 7), Map.entry("nerofactions", 8),
            Map.entry("nerocolonies", 9), Map.entry("nerosecurity", 10), Map.entry("neroevents", 11),
            Map.entry("neroquests", 12), Map.entry("nerocreatures", 13), Map.entry("neroagriculture", 14),
            Map.entry("nerocompanion", 15), Map.entry("neroruins", 16), Map.entry("nerodecor", 17),
            Map.entry("neronotes", 18), Map.entry("neroarcade", 19), Map.entry("nerolink", 20),
            Map.entry("neromaps", 21), Map.entry("nerotrophies", 22));

    private final Runnable listener = this::recordChanged;
    private int hx;
    private int hy;
    private int hw;
    private int hh;
    private int railW;
    private int cardW;
    private int columns;
    private int tab;
    private boolean collectionsView;
    private final Set<TrophyTier> filters = BookModel.noFilters();
    private int scroll;
    @Nullable
    private Identifier selected;
    @Nullable
    private Identifier selectedCollection;
    private long armedUntil;
    private long recordSeenAt = System.currentTimeMillis();
    private final List<Component> tooltip = new ArrayList<>();

    public TrophyBookScreen() {
        super(Component.translatable("screen.nerotrophies.book.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.hw = Math.max(260, Math.min(560, this.width - 60));
        this.hh = Math.max(180, Math.min(340, this.height - 30));
        this.hx = (this.width - this.hw) / 2;
        this.hy = (this.height - this.hh) / 2;
        this.railW = this.hw < 427 ? 28 : this.hw < 520 ? 92 : 112;
        this.cardW = this.hw < 520 ? 100 : 104;
        this.columns = Math.max(1, (this.mainW() + 4) / (this.cardW + 4));
        ClientTrophyRecord.addListener(this.listener);
        ClientTrophyNet.refreshBook();
        play(DisplaySounds.BOOK_OPEN);
    }

    @Override
    public void removed() {
        ClientTrophyRecord.removeListener(this.listener);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void recordChanged() {
        this.recordSeenAt = System.currentTimeMillis();
    }

    private int mainX() {
        return this.hx + 6 + this.railW + 4;
    }

    private int mainW() {
        return this.hw - 6 - (6 + this.railW + 4);
    }

    private int detailY() {
        return this.hy + this.hh - 74;
    }

    private static void play(SoundEvent sound) {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    // ============================================================================================
    // Drawing
    // ============================================================================================

    private void draw(GuiGraphicsExtractor g, int mx, int my) {
        this.tooltip.clear();
        Gui.panel(g, this.hx, this.hy, this.hw, this.hh, TrophyColours.PANEL, TrophyColours.PANEL_EDGE);
        this.brackets(g);
        RecordPayload record = ClientTrophyRecord.current();
        Gui.text(g, this.font, this.title, this.hx + 8, this.hy + 7, TrophyColours.TEXT);
        g.fill(this.hx + 1, this.hy + 22, this.hx + this.hw - 1, this.hy + 23, TrophyColours.PANEL_EDGE);
        if (record == null) {
            Component loading = Component.translatable("screen.nerotrophies.book.loading");
            Gui.text(g, this.font, loading, this.hx + (this.hw - this.font.width(loading)) / 2,
                    this.hy + this.hh / 2 - 4, TrophyColours.TEXT_2);
            return;
        }
        BookModel model = new BookModel(record);
        this.header(g, model, mx, my);
        this.rail(g, model, mx, my);
        if (this.collectionsView) {
            this.collections(g, model, mx, my);
        } else {
            this.grid(g, model, mx, my);
        }
        this.footer(g, record, mx, my);
        if (!this.tooltip.isEmpty()) {
            Gui.tooltip(g, this.font, this.tooltip, mx, my);
        }
    }

    private void brackets(GuiGraphicsExtractor g) {
        int a = TrophyColours.ACCENT;
        int x0 = this.hx;
        int y0 = this.hy;
        int x1 = this.hx + this.hw;
        int y1 = this.hy + this.hh;
        g.fill(x0, y0, x0 + 3, y0 + 1, a);
        g.fill(x0, y0, x0 + 1, y0 + 3, a);
        g.fill(x1 - 3, y0, x1, y0 + 1, a);
        g.fill(x1 - 1, y0, x1, y0 + 3, a);
        g.fill(x0, y1 - 1, x0 + 3, y1, a);
        g.fill(x0, y1 - 3, x0 + 1, y1, a);
        g.fill(x1 - 3, y1 - 1, x1, y1, a);
        g.fill(x1 - 1, y1 - 3, x1, y1, a);
    }

    private void pips(GuiGraphicsExtractor g, TrophyTier tier, int x, int y, int colour) {
        if (tier == TrophyTier.NERO) {
            Gui.blit(g, Gui.BOOK_SHEET, x, y, 36, 56, 3, 3, colour);
            return;
        }
        int n = tier.ordinal() + 1;
        for (int i = 0; i < n; i++) {
            Gui.blit(g, Gui.BOOK_SHEET, x + i * 4, y, 32, 56, 3, 3, colour);
        }
    }

    private void header(GuiGraphicsExtractor g, BookModel model, int mx, int my) {
        BookModel.Tab all = model.tabs().get(0);
        int barX = this.hx + 78;
        int barW = this.hw < 520 ? 92 : 220;
        g.fill(barX, this.hy + 9, barX + barW, this.hy + 13, TrophyColours.TRACK);
        int fill = all.total() == 0 ? 0 : barW * all.earned() / all.total();
        g.fill(barX, this.hy + 9, barX + fill, this.hy + 13, TrophyColours.ACCENT);
        String count = all.earned() + " / " + all.total();
        Gui.text(g, this.font, count, barX + barW + 4, this.hy + 7, TrophyColours.TEXT_2);
        if (Gui.inside(mx, my, barX, this.hy + 6, barW, 10)) {
            this.tooltip.add(Component.translatable("screen.nerotrophies.book.overall.tooltip", all.earned(),
                    all.total()));
        }
        int cx = this.hx + this.hw - 198;
        for (TrophyTier tier : TrophyTier.values()) {
            int[] c = model.tierCount(tier);
            boolean on = this.filters.contains(tier);
            boolean hot = Gui.inside(mx, my, cx, this.hy + 4, 46, 14);
            if (on) {
                g.fill(cx, this.hy + 4, cx + 46, this.hy + 18, TrophyColours.deep(tier));
            }
            if (hot) {
                Gui.frame(g, cx, this.hy + 4, 46, 14, TrophyColours.ACCENT);
            } else if (on && tier == TrophyTier.NERO) {
                Gui.splitFrame(g, cx, this.hy + 4, 46, 14, TrophyColours.PRISM_A, TrophyColours.PRISM_B);
            } else {
                Gui.frame(g, cx, this.hy + 4, 46, 14, on ? TrophyColours.base(tier) : TrophyColours.PANEL_EDGE);
            }
            this.pips(g, tier, cx + 4, this.hy + 9, TrophyColours.ink(tier));
            Gui.text(g, this.font, c[0] + "/" + c[1], cx + 17, this.hy + 7,
                    on ? TrophyColours.ink(tier) : TrophyColours.TEXT_2);
            if (hot) {
                Component tierName = Component.translatable("nerotrophies.tier." + tier.getSerializedName());
                this.tooltip.add(Component.translatable("screen.nerotrophies.book.filter.tooltip", tierName, c[0],
                        c[1]));
            }
            cx += 48;
        }
    }

    private String categoryName(@Nullable String category) {
        if (category == null) {
            return Component.translatable("screen.nerotrophies.book.tab.all").getString();
        }
        String key = "nerotrophies.category." + category;
        return Language.getInstance().has(key) ? Language.getInstance().getOrDefault(key) : category;
    }

    private void rail(GuiGraphicsExtractor g, BookModel model, int mx, int my) {
        int x = this.hx + 6;
        int y0 = this.hy + 26;
        int bottom = this.hy + this.hh - 38;
        List<BookModel.Tab> tabs = model.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int y = y0 + i * 20;
            if (y + 20 > bottom) {
                break;
            }
            BookModel.Tab t = tabs.get(i);
            boolean sel = !this.collectionsView && this.tab == i;
            boolean hot = Gui.inside(mx, my, x, y, this.railW, 20);
            this.railRow(g, x, y, i == 0 ? 0 : EMBLEMS.getOrDefault(t.category(), 23), this.categoryName(t.category()),
                    t.earned(), t.total(), sel, hot);
            if (hot) {
                this.tooltip.add(Component.translatable("screen.nerotrophies.book.tab.tooltip",
                        this.categoryName(t.category()), t.earned(), t.total()));
            }
        }
        g.fill(x, this.hy + this.hh - 38, x + this.railW, this.hy + this.hh - 37, TrophyColours.PANEL_EDGE);
        List<RecordPayload.CollectionView> cols = model.collections();
        int done = (int) cols.stream().filter(RecordPayload.CollectionView::completed).count();
        int cy = this.hy + this.hh - 36;
        this.railRow(g, x, cy, 1, Component.translatable("screen.nerotrophies.book.collections").getString(), done,
                cols.size(), this.collectionsView, Gui.inside(mx, my, x, cy, this.railW, 20));
    }

    private void railRow(GuiGraphicsExtractor g, int x, int y, int emblem, String name, int earned, int total,
            boolean selected, boolean hot) {
        if (selected) {
            g.fill(x, y, x + this.railW, y + 20, TrophyColours.TRACK);
            g.fill(x, y, x + 2, y + 20, TrophyColours.ACCENT);
        } else if (hot) {
            Gui.frame(g, x, y, this.railW, 20, TrophyColours.PANEL_EDGE);
        }
        Gui.emblem(g, emblem, x + (this.railW < 40 ? 6 : 2), y + 2);
        int barX = this.railW < 40 ? x + 4 : x + 22;
        int barW = this.railW < 40 ? 20 : this.railW - 28;
        if (this.railW >= 40) {
            Gui.text(g, this.font, Gui.fit(this.font, name, this.railW - 26), x + 22, y + 2, TrophyColours.TEXT);
        }
        g.fill(barX, y + 14 + (this.railW < 40 ? 4 : 0), barX + barW, y + 16 + (this.railW < 40 ? 4 : 0),
                TrophyColours.TRACK);
        int fill = total == 0 ? 0 : barW * earned / total;
        g.fill(barX, y + 14 + (this.railW < 40 ? 4 : 0), barX + fill, y + 16 + (this.railW < 40 ? 4 : 0),
                TrophyColours.ACCENT);
    }

    private List<RecordPayload.Entry> currentCards(BookModel model) {
        int t = Math.min(this.tab, model.tabs().size() - 1);
        return model.cards(model.tabs().get(Math.max(0, t)).category(), this.filters);
    }

    private int gridBottom() {
        return this.selected != null ? this.detailY() - 4 : this.hy + this.hh - 16;
    }

    private void grid(GuiGraphicsExtractor g, BookModel model, int mx, int my) {
        int x = this.mainX();
        int w = this.mainW();
        List<RecordPayload.Entry> cards = this.currentCards(model);
        BookModel.Tab tabInfo = model.tabs().get(Math.max(0, Math.min(this.tab, model.tabs().size() - 1)));
        Gui.text(g, this.font, this.categoryName(tabInfo.category()), x, this.hy + 27, TrophyColours.TEXT);
        String count = tabInfo.earned() + " / " + tabInfo.total();
        Gui.text(g, this.font, count, x + w - this.font.width(count), this.hy + 27, TrophyColours.TEXT_2);
        int top = this.hy + 38;
        int bottom = this.gridBottom();
        if (cards.isEmpty()) {
            Component empty = Component.translatable("screen.nerotrophies.book.filter.empty");
            Gui.text(g, this.font, empty, x + (w - this.font.width(empty)) / 2, top + 20, TrophyColours.TEXT_FAINT);
        }
        g.enableScissor(x, top, x + w, bottom);
        int rows = (cards.size() + this.columns - 1) / this.columns;
        int visibleRows = Math.max(1, (bottom - top) / 42);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, rows - visibleRows)));
        for (int i = 0; i < cards.size(); i++) {
            int row = i / this.columns - this.scroll;
            if (row < 0 || row >= visibleRows + 1) {
                continue;
            }
            int cx = x + (i % this.columns) * (this.cardW + 4);
            int cy = top + row * 42;
            this.card(g, cards.get(i), cx, cy, mx, my, cy + 38 <= bottom);
        }
        g.disableScissor();
        if (!this.filters.isEmpty()) {
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.filter.hidden_note"), x,
                    bottom - 9, TrophyColours.TEXT_FAINT);
        }
        if (this.selected != null) {
            RecordPayload.Entry entry = model.record().entry(this.selected);
            if (entry != null) {
                this.detail(g, model, entry, mx, my);
            }
        }
    }

    private void card(GuiGraphicsExtractor g, RecordPayload.Entry e, int x, int y, int mx, int my, boolean hoverable) {
        int w = this.cardW;
        boolean hot = hoverable && Gui.inside(mx, my, x, y, w, 38);
        g.fill(x, y, x + w, y + 38, TrophyColours.PANEL);
        if (e.masked()) {
            for (int i = 0; i < w; i += 4) {
                g.fill(x + i, y, x + Math.min(w, i + 2), y + 1, TrophyColours.TEXT_FAINT);
                g.fill(x + i, y + 37, x + Math.min(w, i + 2), y + 38, TrophyColours.TEXT_FAINT);
            }
            Gui.blit(g, Gui.BOOK_SHEET, x + 2, y + 2, 40, 64, 20, 20, 0xFFFFFFFF);
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.hidden.title"), x + 26, y + 4,
                    TrophyColours.TEXT_FAINT);
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.hidden.line"), x + 26, y + 15,
                    TrophyColours.TEXT_FAINT);
            if (hot) {
                this.tooltip.add(Component.translatable("screen.nerotrophies.book.hidden.tooltip"));
            }
        } else {
            TrophyTier tier = e.tier();
            if (e.earned() && tier == TrophyTier.NERO) {
                Gui.splitFrame(g, x, y, w, 38, TrophyColours.PRISM_A, TrophyColours.PRISM_B);
            } else {
                Gui.frame(g, x, y, w, 38, e.earned() ? TrophyColours.base(tier) : TrophyColours.PANEL_EDGE);
            }
            Gui.blit(g, Gui.BOOK_SHEET, x + 2, y + 2, e.earned() ? 0 : 20, 64, 20, 20, 0xFFFFFFFF);
            ItemStack icon = TrophyDraw.iconStack(e.icon());
            if (!icon.isEmpty()) {
                Gui.item(g, icon, x + 4, y + 4);
                if (!e.earned()) {
                    g.fill(x + 4, y + 4, x + 20, y + 20, 0x99141C26);
                }
            }
            String title = e.title() == null ? e.id().toString() : Component.translatable(e.title()).getString();
            Gui.text(g, this.font, Gui.fit(this.font, title, w - 44), x + 26, y + 4,
                    e.earned() ? TrophyColours.TEXT : TrophyColours.TEXT_2);
            if (tier != null) {
                int ink = e.earned() ? TrophyColours.ink(tier) : TrophyColours.scale(TrophyColours.ink(tier), 0.55F);
                this.pips(g, tier, x + w - 16, y + 5, ink);
            }
            if (e.earned()) {
                Component line = e.claimed() ? Component.literal(DATE.format(Instant.ofEpochMilli(e.earnedAt())))
                        : Component.translatable("screen.nerotrophies.book.unclaimed");
                Gui.text(g, this.font, line, x + 26, y + 15, e.claimed() ? TrophyColours.TEXT_2 : TrophyColours.ink(tier));
                Gui.blit(g, Gui.BOOK_SHEET, x + 26, y + 27, 0, 56, 7, 7, TrophyColours.ink(tier));
                Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.earned"), x + 36, y + 27,
                        TrophyColours.TEXT_2);
            } else if (e.target() > 0) {
                Gui.text(g, this.font, e.progress() + " / " + e.target(), x + 26, y + 15, TrophyColours.TEXT_2);
                int bw = w - 30;
                g.fill(x + 26, y + 28, x + 26 + bw, y + 32, TrophyColours.TRACK);
                g.fill(x + 26, y + 28, x + 26 + bw * Math.min(e.progress(), e.target()) / e.target(), y + 32,
                        TrophyColours.base(tier));
            } else {
                Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.locked"), x + 26, y + 15,
                        TrophyColours.TEXT_FAINT);
            }
            if (hot) {
                this.tooltip.add(Component.literal(title));
                if (tier != null) {
                    this.tooltip.add(Component.translatable("tooltip.nerotrophies.tier",
                            Component.translatable("nerotrophies.tier." + tier.getSerializedName())));
                }
                if (e.description() != null) {
                    this.tooltip.add(Component.translatable(e.description()));
                }
            }
        }
        if (e.id().equals(this.selected)) {
            Gui.frame(g, x - 1, y - 1, w + 2, 40, TrophyColours.ACCENT_BRIGHT);
        }
    }

    private long nowServer(RecordPayload record) {
        return record.serverNow() + (System.currentTimeMillis() - this.recordSeenAt);
    }

    private void detail(GuiGraphicsExtractor g, BookModel model, RecordPayload.Entry e, int mx, int my) {
        int x = this.mainX();
        int w = this.mainW();
        int y = this.detailY();
        g.fill(x, y, x + w, y + 1, TrophyColours.PANEL_EDGE);
        if (e.masked()) {
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.hidden.title"), x + 34, y + 5,
                    TrophyColours.TEXT_FAINT);
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.hidden.detail"), x + 34, y + 16,
                    TrophyColours.TEXT_2);
            return;
        }
        TrophyTier tier = e.tier();
        if (tier != null) {
            Gui.blit(g, Gui.BOOK_SHEET, x + 4, y + 5, 24 * tier.ordinal(), 88, 24, 24, 0xFFFFFFFF);
            this.pips(g, tier, x + w - 16, y + 6, TrophyColours.ink(tier));
        }
        Gui.item(g, TrophyDraw.iconStack(e.icon()), x + 8, y + 9);
        String title = e.title() == null ? e.id().toString() : Component.translatable(e.title()).getString();
        Gui.text(g, this.font, Gui.fit(this.font, title, w - 60), x + 34, y + 5, TrophyColours.ink(tier));
        if (e.description() != null) {
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                    Component.translatable(e.description()), Math.max(40, w - 142));
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.text(this.font, lines.get(i), x + 34, y + 16 + 10 * i, TrophyColours.TEXT_2, false);
            }
        }
        Component status = e.earned()
                ? Component.translatable("screen.nerotrophies.book.detail.earned",
                        DATE.format(Instant.ofEpochMilli(e.earnedAt())))
                : e.target() > 0 ? Component.literal(e.progress() + " / " + e.target())
                        : Component.translatable("screen.nerotrophies.book.detail.locked");
        Gui.text(g, this.font, status, x + 34, y + 37, TrophyColours.TEXT_2);
        for (RecordPayload.CollectionView view : model.collections()) {
            if (view.members().contains(e.id())) {
                Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.detail.collection",
                        Component.translatable(view.title())), x + 34, y + 47, TrophyColours.ACCENT_BRIGHT);
                break;
            }
        }
        this.claimButton(g, e, model.record(), x + w - 104, y + 36, mx, my);
    }

    private void claimButton(GuiGraphicsExtractor g, RecordPayload.Entry e, RecordPayload record, int bx, int by,
            int mx, int my) {
        boolean hot = Gui.inside(mx, my, bx, by, 100, 16);
        long now = this.nowServer(record);
        boolean armed = System.currentTimeMillis() < this.armedUntil;
        Component label;
        Component tip;
        int edge;
        int text;
        boolean enabled = true;
        if (!e.earned()) {
            label = Component.translatable("screen.nerotrophies.book.claim");
            tip = Component.translatable("screen.nerotrophies.book.claim.locked");
            enabled = false;
            edge = TrophyColours.PANEL_EDGE;
            text = TrophyColours.TEXT_FAINT;
        } else if (!e.claimed()) {
            label = Component.translatable("screen.nerotrophies.book.claim");
            tip = Component.translatable("screen.nerotrophies.book.claim.tooltip");
            edge = TrophyColours.base(e.tier());
            text = TrophyColours.ink(e.tier());
        } else if (e.canReclaimAt() > now) {
            long left = (e.canReclaimAt() - now) / 1000L;
            String time = (left / 60) + ":" + String.format("%02d", left % 60);
            label = Component.translatable("screen.nerotrophies.book.reclaim");
            tip = Component.translatable("screen.nerotrophies.book.reclaim.cooldown", time);
            Gui.text(g, this.font, time, bx + 100 - this.font.width(time), by - 10, TrophyColours.TEXT_FAINT);
            enabled = false;
            edge = TrophyColours.PANEL_EDGE;
            text = TrophyColours.TEXT_FAINT;
        } else if (armed) {
            label = Component.translatable("screen.nerotrophies.book.reclaim.confirm");
            tip = Component.translatable("screen.nerotrophies.book.reclaim.confirm.tooltip");
            edge = TrophyColours.ACCENT;
            text = TrophyColours.TEXT;
            g.fill(bx, by, bx + 100, by + 16, TrophyColours.ACCENT_DIM);
        } else {
            label = Component.translatable("screen.nerotrophies.book.reclaim");
            tip = Component.translatable("screen.nerotrophies.book.reclaim.tooltip");
            edge = TrophyColours.PANEL_EDGE;
            text = TrophyColours.TEXT;
        }
        Gui.frame(g, bx, by, 100, 16, hot && enabled ? TrophyColours.ACCENT : edge);
        String s = Gui.fit(this.font, label.getString(), 96);
        Gui.text(g, this.font, s, bx + (100 - this.font.width(s)) / 2, by + 4, text);
        if (hot) {
            this.tooltip.add(tip);
        }
    }

    private void collections(GuiGraphicsExtractor g, BookModel model, int mx, int my) {
        int x = this.mainX();
        int w = this.mainW();
        Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.collections"), x, this.hy + 27,
                TrophyColours.TEXT);
        List<RecordPayload.CollectionView> cols = model.collections();
        int top = this.hy + 38;
        int bottom = this.selectedCollection != null ? this.detailY() - 4 : this.hy + this.hh - 16;
        int visible = Math.max(1, (bottom - top) / 48);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, cols.size() - visible)));
        g.enableScissor(x, top, x + w, bottom);
        for (int i = 0; i < visible + 1 && this.scroll + i < cols.size(); i++) {
            RecordPayload.CollectionView view = cols.get(this.scroll + i);
            int y = top + i * 48;
            this.collectionRow(g, model, view, x, y, w, mx, my);
        }
        g.disableScissor();
        if (this.selectedCollection != null) {
            for (RecordPayload.CollectionView view : cols) {
                if (view.id().equals(this.selectedCollection)) {
                    this.collectionDetail(g, view, mx, my);
                }
            }
        }
    }

    private void collectionRow(GuiGraphicsExtractor g, BookModel model, RecordPayload.CollectionView view, int x,
            int y, int w, int mx, int my) {
        g.fill(x, y, x + w, y + 44, TrophyColours.PANEL);
        Gui.frame(g, x, y, w, 44, view.completed() ? TrophyColours.ACCENT : TrophyColours.PANEL_EDGE);
        g.fill(x + 4, y + 10, x + 28, y + 34, TrophyColours.GLASS);
        g.fill(x + 9, y + 20, x + 23, y + 24, 0xFF000000 | Trims.byId(view.trim()).accent());
        if (Gui.inside(mx, my, x + 4, y + 10, 24, 24)) {
            this.tooltip.add(Component.translatable("screen.nerotrophies.book.collection.trim",
                    Component.translatable("trim.nerotrophies." + view.trim())));
        }
        Gui.text(g, this.font, Gui.fit(this.font, Component.translatable(view.title()).getString(), w - 90), x + 32,
                y + 4, TrophyColours.TEXT);
        int earned = model.earnedIn(view);
        int size = view.members().size();
        Component count = view.completed() ? Component.translatable("screen.nerotrophies.book.collection.complete")
                : Component.literal(earned + " / " + size);
        Gui.text(g, this.font, count, x + w - 6 - this.font.width(count), y + 4,
                view.completed() ? TrophyColours.ACCENT_BRIGHT : TrophyColours.TEXT_2);
        g.fill(x + 32, y + 15, x + w - 8, y + 19, TrophyColours.TRACK);
        g.fill(x + 32, y + 15, x + 32 + (size == 0 ? 0 : (w - 40) * earned / size), y + 19, TrophyColours.ACCENT);
        int ix = x + 32;
        int max = Math.max(1, (w - 40) / 18);
        for (int k = 0; k < view.members().size(); k++) {
            if (k >= max - 1 && view.members().size() > max) {
                Gui.text(g, this.font, "+" + (view.members().size() - k), ix + 2, y + 27, TrophyColours.TEXT_2);
                break;
            }
            RecordPayload.Entry member = model.record().entry(view.members().get(k));
            if (member == null || member.masked()) {
                Gui.text(g, this.font, "?", ix + 5, y + 27, TrophyColours.TEXT_FAINT);
            } else {
                Gui.item(g, TrophyDraw.iconStack(member.icon()), ix, y + 23);
                if (member.earned()) {
                    g.fill(ix, y + 39, ix + 16, y + 40, TrophyColours.base(member.tier()));
                } else {
                    g.fill(ix, y + 23, ix + 16, y + 39, 0xB3141C26);
                }
            }
            ix += 18;
        }
        if (view.id().equals(this.selectedCollection)) {
            Gui.frame(g, x - 1, y - 1, w + 2, 46, TrophyColours.ACCENT_BRIGHT);
        }
    }

    @Nullable
    private Identifier bookSelection() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = this.minecraft.player.getItemInHand(hand);
            if (stack.is(ModItems.TROPHY_BOOK.get())) {
                return stack.get(ModDataComponents.BOOK_SELECTION.get());
            }
        }
        return null;
    }

    private boolean holdsBook() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (this.minecraft.player.getItemInHand(hand).is(ModItems.TROPHY_BOOK.get())) {
                return true;
            }
        }
        return false;
    }

    private void collectionDetail(GuiGraphicsExtractor g, RecordPayload.CollectionView view, int mx, int my) {
        int x = this.mainX();
        int w = this.mainW();
        int y = this.detailY();
        g.fill(x, y, x + w, y + 1, TrophyColours.PANEL_EDGE);
        g.fill(x + 4, y + 5, x + 28, y + 29, TrophyColours.GLASS);
        Gui.item(g, TrophyDraw.iconStack(view.icon()), x + 8, y + 9);
        Gui.text(g, this.font, Gui.fit(this.font, Component.translatable(view.title()).getString(), w - 150), x + 34,
                y + 5, TrophyColours.ACCENT_BRIGHT);
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(
                Component.translatable(view.description()), Math.max(40, w - 142));
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.text(this.font, lines.get(i), x + 34, y + 16 + 10 * i, TrophyColours.TEXT_2, false);
        }
        Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.collection.trim",
                Component.translatable("trim.nerotrophies." + view.trim())), x + 34, y + 37, TrophyColours.TEXT_2);
        boolean chosen = view.id().equals(this.bookSelection());
        int bx = x + w - 104;
        int by = y + 36;
        boolean enabled = this.holdsBook();
        boolean hot = Gui.inside(mx, my, bx, by, 100, 16);
        if (chosen) {
            g.fill(bx, by, bx + 100, by + 16, TrophyColours.ACCENT_DIM);
        }
        Gui.frame(g, bx, by, 100, 16, hot && enabled ? TrophyColours.ACCENT : TrophyColours.PANEL_EDGE);
        Component label = Component.translatable(chosen ? "screen.nerotrophies.book.collection.selected"
                : "screen.nerotrophies.book.collection.select");
        String s = Gui.fit(this.font, label.getString(), 96);
        Gui.text(g, this.font, s, bx + (100 - this.font.width(s)) / 2, by + 4,
                enabled ? TrophyColours.TEXT : TrophyColours.TEXT_FAINT);
        if (hot) {
            this.tooltip.add(Component.translatable(enabled ? "screen.nerotrophies.book.collection.select.tooltip"
                    : "screen.nerotrophies.book.collection.need_book"));
        }
        if (chosen) {
            Gui.text(g, this.font, Component.translatable("screen.nerotrophies.book.collection.bind_hint"), x + 34,
                    y + 47, TrophyColours.TEXT_FAINT);
        }
    }

    private void footer(GuiGraphicsExtractor g, RecordPayload record, int mx, int my) {
        int y = this.hy + this.hh - 12;
        int x = this.hx + 8;
        x = this.checkbox(g, x, y, record.showName(), true, "screen.nerotrophies.book.showname",
                "screen.nerotrophies.book.showname.tooltip", mx, my);
        this.checkbox(g, x + 10, y, record.announce(), record.showName(), "screen.nerotrophies.book.announce",
                "screen.nerotrophies.book.announce.tooltip", mx, my);
        Component hints = Component.translatable("screen.nerotrophies.book.hints");
        if (this.hw >= 427) {
            Gui.text(g, this.font, hints, this.hx + this.hw - 8 - this.font.width(hints), y, TrophyColours.TEXT_FAINT);
        }
    }

    private int checkbox(GuiGraphicsExtractor g, int x, int y, boolean on, boolean live, String key, String tipKey,
            int mx, int my) {
        Component label = Component.translatable(key);
        int w = 11 + this.font.width(label);
        boolean hot = Gui.inside(mx, my, x, y - 1, w, 10);
        Gui.blit(g, Gui.BOOK_SHEET, x, y, 48 + (on ? 8 : 0) + (hot ? 16 : 0), 24, 8, 8, 0xFFFFFFFF);
        Gui.text(g, this.font, label, x + 11, y, live ? TrophyColours.TEXT_2 : TrophyColours.TEXT_FAINT);
        if (hot) {
            this.tooltip.add(Component.translatable(tipKey));
        }
        return x + w;
    }

    // ============================================================================================
    // Input
    // ============================================================================================

    private boolean click(double mx, double my, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        RecordPayload record = ClientTrophyRecord.current();
        if (record == null) {
            return false;
        }
        BookModel model = new BookModel(record);
        // Tier chips.
        int cx = this.hx + this.hw - 198;
        for (TrophyTier tier : TrophyTier.values()) {
            if (Gui.inside(mx, my, cx, this.hy + 4, 46, 14)) {
                if (!this.filters.remove(tier)) {
                    this.filters.add(tier);
                }
                this.scroll = 0;
                return true;
            }
            cx += 48;
        }
        // Rail.
        int rx = this.hx + 6;
        for (int i = 0; i < model.tabs().size(); i++) {
            int y = this.hy + 26 + i * 20;
            if (y + 20 > this.hy + this.hh - 38) {
                break;
            }
            if (Gui.inside(mx, my, rx, y, this.railW, 20)) {
                this.tab = i;
                this.collectionsView = false;
                this.scroll = 0;
                this.selected = null;
                play(DisplaySounds.BOOK_PAGE);
                return true;
            }
        }
        if (Gui.inside(mx, my, rx, this.hy + this.hh - 36, this.railW, 20)) {
            this.collectionsView = true;
            this.scroll = 0;
            this.selected = null;
            play(DisplaySounds.BOOK_PAGE);
            return true;
        }
        // Footer.
        int fy = this.hy + this.hh - 12;
        int fx = this.hx + 8;
        int w1 = 11 + this.font.width(Component.translatable("screen.nerotrophies.book.showname"));
        if (Gui.inside(mx, my, fx, fy - 1, w1, 10)) {
            ClientTrophyNet.setShowName(!record.showName());
            return true;
        }
        int w2 = 11 + this.font.width(Component.translatable("screen.nerotrophies.book.announce"));
        if (Gui.inside(mx, my, fx + w1 + 10, fy - 1, w2, 10)) {
            ClientTrophyNet.setAnnounce(!record.announce());
            return true;
        }
        return this.collectionsView ? this.clickCollections(model, mx, my) : this.clickGrid(model, record, mx, my);
    }

    private boolean clickGrid(BookModel model, RecordPayload record, double mx, double my) {
        int x = this.mainX();
        int w = this.mainW();
        if (this.selected != null) {
            RecordPayload.Entry e = record.entry(this.selected);
            int y = this.detailY();
            if (e != null && !e.masked() && Gui.inside(mx, my, x + w - 104, y + 36, 100, 16)) {
                this.claim(e, record);
                return true;
            }
            if (Gui.inside(mx, my, x, y, w, 58)) {
                return true;
            }
        }
        int top = this.hy + 38;
        int bottom = this.gridBottom();
        if (!Gui.inside(mx, my, x, top, w, bottom - top)) {
            return false;
        }
        List<RecordPayload.Entry> cards = this.currentCards(model);
        int col = (int) ((mx - x) / (this.cardW + 4));
        int row = (int) ((my - top) / 42) + this.scroll;
        int index = row * this.columns + col;
        if (col < this.columns && index >= 0 && index < cards.size()) {
            Identifier id = cards.get(index).id();
            this.selected = id.equals(this.selected) ? null : id;
            this.armedUntil = 0L;
            return true;
        }
        return false;
    }

    private void claim(RecordPayload.Entry e, RecordPayload record) {
        if (!e.earned()) {
            return;
        }
        if (!e.claimed()) {
            ClientTrophyNet.claim(e.id());
            play(DisplaySounds.BOOK_RECLAIM);
            return;
        }
        if (e.canReclaimAt() > this.nowServer(record)) {
            return;
        }
        if (System.currentTimeMillis() < this.armedUntil) {
            this.armedUntil = 0L;
            ClientTrophyNet.claim(e.id());
            play(DisplaySounds.BOOK_RECLAIM);
        } else {
            this.armedUntil = System.currentTimeMillis() + ARM_MILLIS;
        }
    }

    private boolean clickCollections(BookModel model, double mx, double my) {
        int x = this.mainX();
        int w = this.mainW();
        List<RecordPayload.CollectionView> cols = model.collections();
        if (this.selectedCollection != null) {
            int y = this.detailY();
            if (Gui.inside(mx, my, x + w - 104, y + 36, 100, 16) && this.holdsBook()) {
                boolean chosen = this.selectedCollection.equals(this.bookSelection());
                ClientTrophyNet.selectCollection(chosen ? null : this.selectedCollection);
                return true;
            }
            if (Gui.inside(mx, my, x, y, w, 58)) {
                return true;
            }
        }
        int top = this.hy + 38;
        int row = (int) ((my - top) / 48) + this.scroll;
        if (mx >= x && mx < x + w && my >= top && row >= 0 && row < cols.size()) {
            Identifier id = cols.get(row).id();
            this.selectedCollection = id.equals(this.selectedCollection) ? null : id;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        this.scroll = Math.max(0, this.scroll + (scrollY > 0 ? -1 : 1));
        return true;
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.click(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.click(mx, my, button)) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    *///?}

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        this.draw(g, mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.draw(g, mouseX, mouseY);
    }
    *///?}
}
