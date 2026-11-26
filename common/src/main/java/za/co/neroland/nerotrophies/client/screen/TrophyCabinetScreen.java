package za.co.neroland.nerotrophies.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotrophies.block.Trims;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.client.net.ClientTrophyNet;
import za.co.neroland.nerotrophies.client.net.ClientTrophyRecord;
import za.co.neroland.nerotrophies.client.render.TrophyColours;
import za.co.neroland.nerotrophies.client.render.TrophyDraw;
import za.co.neroland.nerotrophies.client.render.TrophyLook;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.menu.CabinetMenu;
import za.co.neroland.nerotrophies.network.RecordPayload;
import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The cabinet screen, 176 x 186 at every GUI scale: the cabinet's slots (a genuine trophy's well
 * gets its tier border; empty slots of a bound cabinet show the missing members' ghosts), the collection field and
 * its list, the trim stepper and the player's inventory. Both controls ride vanilla menu buttons through
 * {@link ClientTrophyNet}; the server checks the trim and the row. Nothing here shows a name. Client only.
 */
public class TrophyCabinetScreen extends AbstractContainerScreen<CabinetMenu> {

    private static final int W = 176;
    private static final int H = 186;
    private static final int FIELD_X = 70;
    private static final int FIELD_Y = 28;
    private static final int FIELD_W = 98;
    private static final int ROW_H = 12;
    private static final int ROWS_VISIBLE = 6;

    private boolean listOpen;
    private int listScroll;

    public TrophyCabinetScreen(CabinetMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, W, H);
        //?} else {
        /*super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        *///?}
    }

    /** Every loaded collection in id order (the server's row order), from the viewer's own record. */
    private static List<RecordPayload.CollectionView> collections() {
        RecordPayload record = ClientTrophyRecord.current();
        if (record == null) {
            return List.of();
        }
        List<RecordPayload.CollectionView> sorted = new ArrayList<>(record.collections());
        sorted.sort(Comparator.comparing(RecordPayload.CollectionView::id));
        return sorted;
    }

    private RecordPayload.CollectionView bound() {
        int row = this.menu.boundRow();
        List<RecordPayload.CollectionView> all = collections();
        return row >= 1 && row <= all.size() ? all.get(row - 1) : null;
    }

    private static List<String> earnedTrims() {
        RecordPayload record = ClientTrophyRecord.current();
        return record == null ? List.of() : record.trims();
    }

    // ============================================================================================
    // Drawing
    // ============================================================================================

    private void drawAll(GuiGraphicsExtractor g, int mx, int my) {
        int x = this.leftPos;
        int y = this.topPos;
        Gui.blit(g, Gui.CABINET_SHEET, x, y, 0, 0, W, H, 0xFFFFFFFF);
        if (this.menu.isWall()) {
            Gui.blit(g, Gui.CABINET_SHEET, x + 8, y + 20, 176, 0, 54, 36, 0xFFFFFFFF);
        }
        Gui.text(g, this.font, Component.translatable(this.menu.isWall() ? "container.nerotrophies.wall_cabinet"
                : "container.nerotrophies.trophy_cabinet"), x + 8, y + 6, TrophyColours.TEXT);
        Gui.text(g, this.font, Component.translatable("screen.nerotrophies.cabinet.collection"), x + FIELD_X, y + 18,
                TrophyColours.TEXT_2);
        this.drawSlotDecor(g);
        // Collection field.
        RecordPayload.CollectionView bound = this.bound();
        boolean fieldHot = Gui.inside(mx, my, x + FIELD_X, y + FIELD_Y, FIELD_W, 16);
        Gui.blit(g, Gui.CABINET_SHEET, x + FIELD_X, y + FIELD_Y, 0, this.listOpen ? 222 : fieldHot ? 206 : 190,
                FIELD_W, 16, 0xFFFFFFFF);
        String name = bound == null ? Component.translatable("screen.nerotrophies.cabinet.none").getString()
                : Component.translatable(bound.title()).getString();
        Gui.text(g, this.font, Gui.fit(this.font, name, FIELD_W - 14), x + FIELD_X + 4, y + FIELD_Y + 4,
                bound == null ? TrophyColours.TEXT_FAINT : TrophyColours.TEXT);
        Gui.blit(g, Gui.CABINET_SHEET, x + FIELD_X + FIELD_W - 9, y + FIELD_Y + 7, 220, 190, 5, 3, 0xFFFFFFFF);
        if (fieldHot && !this.listOpen) {
            Gui.tooltip(g, this.font, List.of(Component.translatable("screen.nerotrophies.cabinet.collection.tooltip")),
                    mx, my);
        }
        if (!this.listOpen) {
            if (bound != null && this.menu.memberCount() > 0) {
                int members = this.menu.memberCount();
                int fill = Math.round(98F * this.menu.presentCount() / members);
                g.fill(x + 70, y + 47, x + 168, y + 51, TrophyColours.TRACK);
                g.fill(x + 70, y + 47, x + 70 + fill, y + 51, TrophyColours.ACCENT);
                String count = this.menu.presentCount() + " / " + members;
                Gui.text(g, this.font, count, x + 168 - this.font.width(count), y + 54, TrophyColours.TEXT_2);
            }
            this.drawTrim(g, mx, my);
        }
    }

    private void drawSlotDecor(GuiGraphicsExtractor g) {
        RecordPayload.CollectionView bound = this.bound();
        List<Identifier> missing = new ArrayList<>();
        if (bound != null) {
            for (int i = 0; i < bound.members().size(); i++) {
                if (!this.menu.memberPresent(i)) {
                    missing.add(bound.members().get(i));
                }
            }
        }
        int next = 0;
        RecordPayload record = ClientTrophyRecord.current();
        for (int i = 0; i < this.menu.slotCount(); i++) {
            Slot slot = this.menu.slots.get(i);
            int sx = this.leftPos + slot.x - 1;
            int sy = this.topPos + slot.y - 1;
            ItemStack stack = slot.getItem();
            TrophyStamp stamp = DisplayBlockEntity.stampOf(stack);
            if (stamp != null) {
                TrophyLook.Look look = TrophyLook.of(stamp, java.util.Optional.empty());
                if (!look.replica() && look.tier() != null) {
                    Gui.blit(g, Gui.CABINET_SHEET, sx, sy, 200, 190, 18, 18, TrophyColours.hilite(look.tier()));
                }
            } else if (stack.isEmpty() && next < missing.size()) {
                RecordPayload.Entry entry = record == null ? null : record.entry(missing.get(next++));
                if (entry != null && !entry.masked() && entry.icon() != null) {
                    Gui.item(g, TrophyDraw.iconStack(entry.icon()), sx + 1, sy + 1);
                    g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xB31B1428);
                } else {
                    Gui.text(g, this.font, "?", sx + 7, sy + 5, TrophyColours.GHOST_UNKNOWN);
                }
            }
        }
    }

    private List<String> offeredTrims() {
        List<String> offered = new ArrayList<>();
        String current = this.currentTrim();
        List<String> earned = earnedTrims();
        for (Trims.Trim trim : Trims.ALL) {
            if (trim.id().equals(TrophyIds.STANDARD_TRIM) || trim.id().equals(current) || earned.contains(trim.id())) {
                offered.add(trim.id());
            }
        }
        return offered;
    }

    private String currentTrim() {
        int index = this.menu.trimIndex();
        return index >= 0 && index < Trims.ALL.size() ? Trims.ALL.get(index).id() : TrophyIds.STANDARD_TRIM;
    }

    private boolean canStep() {
        return this.offeredTrims().size() > 1;
    }

    private void drawTrim(GuiGraphicsExtractor g, int mx, int my) {
        int x = this.leftPos;
        int y = this.topPos;
        Gui.text(g, this.font, Component.translatable("screen.nerotrophies.cabinet.trim"), x + 70, y + 62,
                TrophyColours.TEXT_2);
        boolean step = this.canStep();
        boolean prevHot = Gui.inside(mx, my, x + 70, y + 72, 12, 16);
        boolean nextHot = Gui.inside(mx, my, x + 156, y + 72, 12, 16);
        Gui.blit(g, Gui.CABINET_SHEET, x + 70, y + 72, 176 + 12 * (!step ? 2 : prevHot ? 1 : 0), 40, 12, 16,
                0xFFFFFFFF);
        Gui.blit(g, Gui.CABINET_SHEET, x + 156, y + 72, 176 + 12 * (!step ? 2 : nextHot ? 1 : 0), 56, 12, 16,
                0xFFFFFFFF);
        g.fill(x + 84, y + 72, x + 154, y + 88, TrophyColours.GLASS);
        Trims.Trim trim = Trims.byId(this.currentTrim());
        g.fill(x + 88, y + 78, x + 102, y + 82, 0xFF000000 | trim.accent());
        String trimName = Component.translatable("trim.nerotrophies." + trim.id()).getString();
        boolean applied = !trim.id().equals(TrophyIds.STANDARD_TRIM) && !earnedTrims().contains(trim.id());
        Gui.text(g, this.font, Gui.fit(this.font, trimName, 46), x + 106, y + 76, TrophyColours.TEXT);
        int earned = earnedTrims().size() + 1;
        Gui.text(g, this.font, Component.translatable("screen.nerotrophies.cabinet.trim.count", earned,
                Trims.ALL.size()), x + 70, y + 90, TrophyColours.TEXT_FAINT);
        if (prevHot || nextHot) {
            Gui.tooltip(g, this.font, List.of(step ? Component.translatable(prevHot
                    ? "screen.nerotrophies.cabinet.trim.previous" : "screen.nerotrophies.cabinet.trim.next")
                    : Component.translatable("screen.nerotrophies.cabinet.trim.none")), mx, my);
        } else if (Gui.inside(mx, my, x + 84, y + 72, 70, 16)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(trimName));
            lines.add(applied ? Component.translatable("screen.nerotrophies.cabinet.trim.applied")
                    : Component.translatable(trim.id().equals(TrophyIds.STANDARD_TRIM)
                            ? "screen.nerotrophies.cabinet.trim.always" : "screen.nerotrophies.cabinet.trim.earned"));
            Gui.tooltip(g, this.font, lines, mx, my);
        }
    }

    private void drawList(GuiGraphicsExtractor g, int mx, int my) {
        if (!this.listOpen) {
            return;
        }
        int x = this.leftPos + FIELD_X;
        int y = this.topPos + FIELD_Y + 16;
        List<RecordPayload.CollectionView> all = collections();
        int rows = Math.min(ROWS_VISIBLE, all.size() + 1);
        Gui.panel(g, x, y, FIELD_W, rows * ROW_H, TrophyColours.PANEL, TrophyColours.PANEL_EDGE);
        int bound = this.menu.boundRow();
        for (int i = 0; i < rows; i++) {
            int row = this.listScroll + i;
            if (row > all.size()) {
                break;
            }
            int ry = y + i * ROW_H;
            boolean hot = Gui.inside(mx, my, x, ry, FIELD_W, ROW_H);
            Gui.blit(g, Gui.CABINET_SHEET, x, ry, 100, hot ? 202 : 190, FIELD_W, ROW_H, 0xFFFFFFFF);
            if (row == bound) {
                g.fill(x, ry, x + 2, ry + ROW_H, TrophyColours.ACCENT);
            }
            String label;
            String count = "";
            if (row == 0) {
                label = Component.translatable("screen.nerotrophies.cabinet.none").getString();
            } else {
                RecordPayload.CollectionView view = all.get(row - 1);
                label = Component.translatable(view.title()).getString();
                count = earnedMembers(view) + "/" + view.members().size();
            }
            Gui.text(g, this.font, Gui.fit(this.font, label, FIELD_W - 30), x + 4, ry + 2, TrophyColours.TEXT);
            Gui.text(g, this.font, count, x + FIELD_W - 3 - this.font.width(count), ry + 2, TrophyColours.TEXT_2);
        }
    }

    private static int earnedMembers(RecordPayload.CollectionView view) {
        RecordPayload record = ClientTrophyRecord.current();
        if (record == null) {
            return 0;
        }
        int n = 0;
        for (Identifier member : view.members()) {
            RecordPayload.Entry entry = record.entry(member);
            if (entry != null && entry.earned()) {
                n++;
            }
        }
        return n;
    }

    // ============================================================================================
    // Input
    // ============================================================================================

    private boolean click(double mx, double my, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        int x = this.leftPos;
        int y = this.topPos;
        if (this.listOpen) {
            int lx = x + FIELD_X;
            int ly = y + FIELD_Y + 16;
            List<RecordPayload.CollectionView> all = collections();
            int rows = Math.min(ROWS_VISIBLE, all.size() + 1);
            if (Gui.inside(mx, my, lx, ly, FIELD_W, rows * ROW_H)) {
                int row = this.listScroll + (int) ((my - ly) / ROW_H);
                if (row >= 0 && row <= all.size()) {
                    ClientTrophyNet.cabinetBindRow(this.menu.containerId, row);
                }
            }
            this.listOpen = false;
            return true;
        }
        if (Gui.inside(mx, my, x + FIELD_X, y + FIELD_Y, FIELD_W, 16)) {
            this.listOpen = true;
            this.listScroll = 0;
            return true;
        }
        if (this.canStep() && Gui.inside(mx, my, x + 70, y + 72, 12, 16)) {
            ClientTrophyNet.cabinetTrimStep(this.menu.containerId, false);
            return true;
        }
        if (this.canStep() && Gui.inside(mx, my, x + 156, y + 72, 12, 16)) {
            ClientTrophyNet.cabinetTrimStep(this.menu.containerId, true);
            return true;
        }
        return false;
    }

    @Override
    //? if >=1.20.2 {
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
    //?} else {
    /*public boolean mouseScrolled(double mx, double my, double scrollY) {
    *///?}
        if (this.listOpen) {
            int max = Math.max(0, collections().size() + 1 - ROWS_VISIBLE);
            this.listScroll = Math.max(0, Math.min(max, this.listScroll + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        //? if >=1.20.2 {
        return super.mouseScrolled(mx, my, scrollX, scrollY);
        //?} else {
        /*return super.mouseScrolled(mx, my, scrollY);
        *///?}
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

    // ============================================================================================
    // Per-line entry points
    // ============================================================================================

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.drawAll(g, mouseX, mouseY);
        super.extractContents(g, mouseX, mouseY, partialTick);
        this.drawList(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Gui.text(g, this.font, this.playerInventoryTitle, 8, 92, TrophyColours.TEXT_2);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor g, float partialTick, int mouseX, int mouseY) {
        this.drawAll(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.drawList(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Gui.text(g, this.font, this.playerInventoryTitle, 8, 92, TrophyColours.TEXT_2);
    }
    *///?}
}
