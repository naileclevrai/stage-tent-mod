package com.nailec.stagetents.client;

import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.network.ModNetwork;
import com.nailec.stagetents.network.UpdateTentPacket;
import com.nailec.stagetents.tent.Entrance;
import com.nailec.stagetents.tent.FloorMode;
import com.nailec.stagetents.tent.LightMode;
import com.nailec.stagetents.tent.Palette;
import com.nailec.stagetents.tent.Presets;
import com.nailec.stagetents.tent.RectShape;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentShape;
import com.nailec.stagetents.tent.TentType;
import com.nailec.stagetents.tent.WallMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/**
 * Tent settings, in three tabs. Changes are previewed live on the client copy of the tent; "Done" sends them to the
 * server, which rebuilds the collision cells. Closing any other way restores the previous settings.
 */
public class TentScreen extends Screen {
    private static final int COL_W = 150, GAP = 6, ROW_H = 22, PAD = 8, HEX_W = 52, ROWS = 7;

    private enum Tab { STRUCTURE, STYLE, FITTINGS }

    private static Tab lastTab = Tab.STRUCTURE;

    private final TentBlockEntity be;
    private final TentType type;
    private final TentParams original;
    private final TentParams edit;
    private final List<Presets.Preset> presets;
    private int presetIndex = -1;
    private boolean applied;
    private Tab tab = lastTab;
    private Component status = Component.empty();

    public TentScreen(TentBlockEntity be) {
        super(Component.translatable("block.stagetents." + be.type().id));
        this.be = be;
        this.type = be.type();
        this.original = be.params().copy();
        this.edit = be.params().copy();
        this.presets = Presets.forType(type);
        if (tab == Tab.FITTINGS && !hasFittings()) tab = Tab.STRUCTURE;
    }

    private boolean hasFittings() {
        return type.hasInterior();
    }

    private int panelWidth() {
        return COL_W * 3 + GAP * 2 + 8;
    }

    private int left() {
        return Math.max(PAD, (width - panelWidth()) / 2);
    }

    private int top() {
        return PAD + 22 + 24;
    }

    @Override
    protected void init() {
        int x1 = left() + 4, x2 = x1 + COL_W + GAP, x3 = x2 + COL_W + GAP;

        // Tabs.
        int tx = x1;
        for (Tab t : Tab.values()) {
            if (t == Tab.FITTINGS && !hasFittings()) continue;
            Button b = Button.builder(tr("tab." + lower(t)), btn -> {
                tab = t;
                lastTab = t;
                rebuildWidgets();
            }).bounds(tx, PAD + 22, 100, 20).build();
            b.active = t != tab;
            addRenderableWidget(b);
            tx += 104;
        }

        switch (tab) {
            case STRUCTURE -> structure(x1, x2, x3, top());
            case STYLE -> style(x1, x2, x3, top());
            case FITTINGS -> fittings(x1, x2, x3, top());
        }

        int by = top() + ROW_H * ROWS + 6;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> apply())
                .bounds(x1, by, COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x2, by, COL_W, 20).build());
    }

    // ------------------------------------------------------------------ tabs

    private void structure(int x1, int x2, int x3, int y) {
        TentType.Ranges r = type.ranges;
        String t = type.id;
        int row = 0;
        addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "width." + t, r.minWidth(), r.maxWidth(), 1,
                () -> edit.width, v -> edit.width = v, null));
        if (r.maxLength() > 0) {
            addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, (type == TentType.FRAME || type == TentType.GAZEBO) ? "length" : "length_extra",
                    r.minLength(), r.maxLength(), r.lengthStep(), () -> edit.length, v -> edit.length = v, null));
        }
        addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "height." + t, r.minHeight(), r.maxHeight(), 1,
                () -> edit.height, v -> edit.height = v, null));
        addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "wall_height." + t, r.minWall(), r.maxWall(), 1,
                () -> edit.wallHeight, v -> edit.wallHeight = v, null));
        if (type != TentType.ARCH) {
            addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "spacing." + t, r.minSpacing(), r.maxSpacing(), 1,
                    () -> edit.poleSpacing, v -> edit.poleSpacing = v, null));
        }
        if (type == TentType.BIG_TOP) {
            addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "masts", 2, TentParams.MAX_MASTS, 1,
                    () -> Math.max(2, edit.masts), v -> edit.masts = v, null));
        }

        row = 0;
        if (type.hasInterior()) {
            addRenderableWidget(CycleButton.<WallMode>builder(m -> tr("walls." + lower(m)))
                    .withValues(WallMode.values()).withInitialValue(edit.walls)
                    .create(x2, y + ROW_H * row++, COL_W, 20, tr("walls"), (b, v) -> { edit.walls = v; preview(); }));
            addRenderableWidget(CycleButton.<Entrance>builder(m -> tr("entrance." + lower(m)))
                    .withValues(Entrance.values()).withInitialValue(edit.entrance)
                    .create(x2, y + ROW_H * row++, COL_W, 20, tr("entrance"), (b, v) -> { edit.entrance = v; preview(); }));
        }
        if (type.isModular()) {
            String[] sides = {"left", "back", "right", "front"};
            for (int e : new int[]{3, 1, 0, 2}) {
                int bit = 1 << e;
                addRenderableWidget(CycleButton.onOffBuilder((edit.joined & bit) != 0)
                        .create(x2, y + ROW_H * row++, COL_W, 20, tr("joined." + sides[e]), (b, v) -> {
                            edit.joined = v ? edit.joined | bit : edit.joined & ~bit;
                            preview();
                        }));
            }
            addRenderableWidget(Button.builder(tr("auto_join"), b -> autoJoin())
                    .bounds(x3, y, COL_W, 20).build());
        }
        if (type == TentType.STRETCH) {
            addRenderableWidget(Button.builder(tr("rescan_poles"), b -> rescanPoles())
                    .bounds(x2, y + ROW_H * row++, COL_W, 20).build());
            status = Component.translatable("screen.stagetents.poles_found", edit.stretchPoles.size());
        }
    }

    private void style(int x1, int x2, int x3, int y) {
        int row = 0;
        colorRow(x1, y + ROW_H * row++, "color_a", false, () -> edit.colorA, v -> edit.colorA = v);
        colorRow(x1, y + ROW_H * row++, "color_b", false, () -> edit.colorB, v -> edit.colorB = v);
        if (type != TentType.ARCH) colorRow(x1, y + ROW_H * row++, "lining", true, () -> edit.lining, v -> edit.lining = v);

        row = 0;
        addRenderableWidget(CycleButton.onOffBuilder(edit.stripes)
                .create(x2, y + ROW_H * row++, COL_W, 20, tr("stripes"), (b, v) -> { edit.stripes = v; preview(); }));
        if (type.hasInterior()) {
            addRenderableWidget(CycleButton.onOffBuilder(edit.valance)
                    .create(x2, y + ROW_H * row++, COL_W, 20, tr("valance"), (b, v) -> { edit.valance = v; preview(); }));
        }
        if (type != TentType.ARCH) {
            addRenderableWidget(CycleButton.<LightMode>builder(m -> tr("lights." + lower(m)))
                    .withValues(LightMode.values()).withInitialValue(edit.lights)
                    .create(x2, y + ROW_H * row++, COL_W, 20, tr("lights"), (b, v) -> { edit.lights = v; preview(); }));
        }
        addRenderableWidget(CycleButton.onOffBuilder(edit.guyRopes)
                .create(x2, y + ROW_H * row++, COL_W, 20, tr("guy_ropes"), (b, v) -> { edit.guyRopes = v; preview(); }));
        if (type == TentType.BIG_TOP || type == TentType.PAGODA || type == TentType.STRETCH) {
            addRenderableWidget(CycleButton.onOffBuilder(edit.flags)
                    .create(x2, y + ROW_H * row++, COL_W, 20, tr("flags"), (b, v) -> { edit.flags = v; preview(); }));
        }

        row = 0;
        Component presetName = presetIndex < 0 ? tr("preset.choose") : tr("preset." + presets.get(presetIndex).key());
        addRenderableWidget(Button.builder(Component.translatable("screen.stagetents.preset", presetName), b -> {
            presetIndex = (presetIndex + (hasShiftDown() ? presets.size() - 1 : 1)) % presets.size();
            presets.get(presetIndex).apply().accept(edit);
            preview();
            rebuildWidgets();
        }).bounds(x3, y + ROW_H * row++, COL_W, 20).build());
        addRenderableWidget(Button.builder(tr("reset"), b -> {
            TentParams d = type.defaults();
            edit.copyStyle(d);
            edit.width = d.width;
            edit.length = d.length;
            edit.height = d.height;
            edit.wallHeight = d.wallHeight;
            edit.poleSpacing = d.poleSpacing;
            edit.masts = d.masts;
            presetIndex = -1;
            preview();
            rebuildWidgets();
        }).bounds(x3, y + ROW_H * row++, COL_W, 20).build());
        addRenderableWidget(CycleButton.onOffBuilder(edit.showPlate)
                .create(x3, y + ROW_H * row++, COL_W, 20, tr("show_plate"), (b, v) -> { edit.showPlate = v; preview(); }));
    }

    private void fittings(int x1, int x2, int x3, int y) {
        int row = 0;
        addRenderableWidget(CycleButton.<FloorMode>builder(m -> tr("floor." + lower(m)))
                .withValues(FloorMode.values()).withInitialValue(edit.floor)
                .create(x1, y + ROW_H * row++, COL_W, 20, tr("floor"), (b, v) -> { edit.floor = v; preview(); }));
        addRenderableWidget(new IntSlider(x1, y + ROW_H * row++, "floor_height", 0, TentParams.MAX_FLOOR, 1,
                () -> edit.floorHeight, v -> edit.floorHeight = v, v -> String.format(Locale.ROOT, "%.2f", v * 0.25)));

        row = 0;
        addRenderableWidget(CycleButton.onOffBuilder(edit.stage)
                .create(x2, y + ROW_H * row++, COL_W, 20, tr("stage"), (b, v) -> { edit.stage = v; preview(); }));
        addRenderableWidget(new IntSlider(x2, y + ROW_H * row++, "stage_height", TentParams.MIN_STAGE, TentParams.MAX_STAGE, 1,
                () -> edit.stageHeight, v -> edit.stageHeight = v, null));
        addRenderableWidget(new IntSlider(x2, y + ROW_H * row++, "stage_depth", TentParams.MIN_STAGE_DEPTH, TentParams.MAX_STAGE_DEPTH, 1,
                () -> edit.stageDepth, v -> edit.stageDepth = v, null));
        addRenderableWidget(CycleButton.onOffBuilder(edit.curtain)
                .create(x2, y + ROW_H * row++, COL_W, 20, tr("curtain"), (b, v) -> { edit.curtain = v; preview(); }));

        row = 0;
        addRenderableWidget(CycleButton.onOffBuilder(edit.rigging)
                .create(x3, y + ROW_H * row++, COL_W, 20, tr("rigging"), (b, v) -> { edit.rigging = v; preview(); }));
    }

    // ------------------------------------------------------------------ actions

    /** Finds rectangular tents whose sides touch this one and joins both with a gutter. */
    private void autoJoin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        TentShape mine = TentShape.create(edit, be.facing());
        if (!(mine instanceof RectShape me)) return;
        int found = 0;
        int pcx = be.getBlockPos().getX() >> 4, pcz = be.getBlockPos().getZ() >> 4;
        for (int cx = pcx - 3; cx <= pcx + 3; cx++) {
            for (int cz = pcz - 3; cz <= pcz + 3; cz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockEntity e : chunk.getBlockEntities().values()) {
                    if (!(e instanceof TentBlockEntity other) || other == be || !other.type().isModular()) continue;
                    if (!(other.shape() instanceof RectShape them)) continue;
                    for (int a = 0; a < 4; a++) {
                        for (int b = 0; b < 4; b++) {
                            if (!edgesTouch(me, be, a, them, other, b)) continue;
                            edit.joined |= 1 << a;
                            TentParams op = other.params().copy();
                            if ((op.joined & (1 << b)) == 0) {
                                op.joined |= 1 << b;
                                ModNetwork.CHANNEL.sendToServer(new UpdateTentPacket(other.getBlockPos(), op.save()));
                            }
                            found++;
                        }
                    }
                }
            }
        }
        status = Component.translatable("screen.stagetents.joined_found", found);
        preview();
        rebuildWidgets();
    }

    /** World-space segment of edge {@code e}: x0, z0, x1, z1, outward nx, nz. */
    private static double[] worldEdge(RectShape s, TentBlockEntity owner, int e) {
        double ax = s.ax, az = s.az;
        double[][] local = {{ax, az, -ax, az, 0, 1}, {-ax, az, -ax, -az, -1, 0}, {-ax, -az, ax, -az, 0, -1}, {ax, -az, ax, az, 1, 0}};
        double[] l = local[e];
        double ox = owner.getBlockPos().getX() + 0.5, oz = owner.getBlockPos().getZ() + 0.5;
        return new double[]{
                ox + s.toWorldX(l[0], l[1]), oz + s.toWorldZ(l[0], l[1]),
                ox + s.toWorldX(l[2], l[3]), oz + s.toWorldZ(l[2], l[3]),
                s.toWorldX(l[4], l[5]), s.toWorldZ(l[4], l[5])};
    }

    private static boolean edgesTouch(RectShape a, TentBlockEntity ab, int ea, RectShape b, TentBlockEntity bb, int eb) {
        double[] p = worldEdge(a, ab, ea), q = worldEdge(b, bb, eb);
        if (p[4] * q[4] + p[5] * q[5] > -0.99) return false; // must face each other
        // Distance between the two parallel lines, measured along the normal.
        double gap = (q[0] - p[0]) * p[4] + (q[1] - p[1]) * p[5];
        if (Math.abs(gap) > 0.6) return false;
        // Overlap along the edge direction.
        double tx = -p[5], tz = p[4];
        double p0 = p[0] * tx + p[1] * tz, p1 = p[2] * tx + p[3] * tz, q0 = q[0] * tx + q[1] * tz, q1 = q[2] * tx + q[3] * tz;
        double overlap = Math.min(Math.max(p0, p1), Math.max(q0, q1)) - Math.max(Math.min(p0, p1), Math.min(q0, q1));
        return overlap > 1;
    }

    private void rescanPoles() {
        applied = true;
        edit.clamp();
        CompoundTag tag = edit.save();
        tag.putBoolean("RescanPoles", true);
        ModNetwork.CHANNEL.sendToServer(new UpdateTentPacket(be.getBlockPos(), tag));
        onClose();
    }

    private void colorRow(int x, int y, String key, boolean allowAuto, IntSupplier get, IntConsumer set) {
        EditBox hex = new EditBox(font, x + COL_W - HEX_W + 1, y + 1, HEX_W - 2, 18, tr(key));
        ColorButton button = new ColorButton(x, y, COL_W - HEX_W - 2, key, allowAuto, get, v -> {
            set.accept(v);
            hex.setValue(v < 0 ? "" : hex(v));
            preview();
        });
        hex.setMaxLength(7);
        hex.setValue(get.getAsInt() < 0 ? "" : hex(get.getAsInt()));
        hex.setResponder(text -> {
            Integer v = parseHex(text);
            if (v != null && v != get.getAsInt()) {
                set.accept(v);
                button.refresh();
                preview();
            }
        });
        addRenderableWidget(button);
        addRenderableWidget(hex);
    }

    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    private static Integer parseHex(String text) {
        String s = text.startsWith("#") ? text.substring(1) : text;
        if (s.length() != 6) return null;
        try {
            return Integer.parseInt(s, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Component colorName(int rgb, boolean allowAuto) {
        if (allowAuto && rgb < 0) return tr("auto");
        int i = Palette.indexOf(rgb);
        return i >= 0 ? tr("color." + Palette.COLORS.get(i).key()) : Component.literal(hex(rgb));
    }

    private static Component tr(String key) {
        return Component.translatable("screen.stagetents." + key);
    }

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private void preview() {
        edit.clamp();
        be.previewParams(edit);
    }

    private void apply() {
        applied = true;
        edit.clamp();
        ModNetwork.CHANNEL.sendToServer(new UpdateTentPacket(be.getBlockPos(), edit.save()));
        be.previewParams(edit);
        onClose();
    }

    @Override
    public void onClose() {
        if (!applied) be.previewParams(original);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // No full-screen dim: the tent behind stays visible as a live preview.
        int l = left(), right = l + panelWidth(), bottom = top() + ROW_H * (ROWS + 1) + 10;
        g.fill(l, PAD, right, bottom, 0xB0101418);
        g.drawString(font, title, l + 4, PAD + 7, 0xFFFFFF);
        String size = switch (type) {
            case FRAME, GAZEBO -> edit.width + " x " + Math.max(edit.length, type == TentType.GAZEBO ? edit.width : 0) + "  h" + edit.height;
            case ARCH -> edit.width + "  h" + edit.height;
            case STRETCH -> "h" + edit.height + "  +" + edit.stretchPoles.size();
            default -> edit.width + " x " + (edit.width + edit.length) + "  h" + edit.height;
        };
        g.drawString(font, size, right - 4 - font.width(size), PAD + 7, 0xA0A0A0);
        if (!status.getString().isEmpty()) {
            g.drawString(font, status, l + 4 + COL_W * 2 + GAP * 2, top() + ROW_H * ROWS + 12, 0xC8C8C8);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class IntSlider extends AbstractSliderButton {
        private final String key;
        private final int min, max, step;
        private final IntConsumer setter;
        private final IntFunction<String> display;

        IntSlider(int x, int y, String key, int min, int max, int step, IntSupplier getter, IntConsumer setter, IntFunction<String> display) {
            super(x, y, COL_W, 20, Component.empty(), max == min ? 0 : (Mth.clamp(getter.getAsInt(), min, max) - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            this.setter = setter;
            this.display = display;
            updateMessage();
        }

        private int current() {
            if (max == min || step == 0) return min;
            int v = min + (int) Math.round(value * (max - min) / step) * step;
            return Mth.clamp(v, min, max);
        }

        @Override
        protected void updateMessage() {
            if (key == null) return;
            int v = current();
            setMessage(Component.translatable("screen.stagetents." + key, display == null ? String.valueOf(v) : display.apply(v)));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
            preview();
        }
    }

    /** Cycles through the palette (shift-click goes backwards) and shows a swatch of the current colour. */
    private static class ColorButton extends Button {
        private final String key;
        private final boolean allowAuto;
        private final IntSupplier get;
        private final IntConsumer set;

        ColorButton(int x, int y, int w, String key, boolean allowAuto, IntSupplier get, IntConsumer set) {
            super(x, y, w, 20, Component.empty(), b -> ((ColorButton) b).cycle(), DEFAULT_NARRATION);
            this.key = key;
            this.allowAuto = allowAuto;
            this.get = get;
            this.set = set;
            refresh();
        }

        void refresh() {
            setMessage(Component.translatable("screen.stagetents." + key, colorName(get.getAsInt(), allowAuto)));
        }

        private void cycle() {
            int dir = hasShiftDown() ? -1 : 1;
            int cur = get.getAsInt();
            int last = Palette.COLORS.size() - 1;
            int next;
            if (allowAuto && cur < 0) {
                next = Palette.COLORS.get(dir > 0 ? 0 : last).rgb();
            } else if (allowAuto && ((dir > 0 && Palette.indexOf(cur) == last) || (dir < 0 && Palette.indexOf(cur) == 0))) {
                next = -1;
            } else {
                next = Palette.next(cur, dir);
            }
            set.accept(next);
            refresh();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            int c = get.getAsInt();
            if (c >= 0) {
                int sx = getX() + getWidth() - 15, sy = getY() + 5;
                g.fill(sx - 1, sy - 1, sx + 11, sy + 11, 0xFF000000);
                g.fill(sx, sy, sx + 10, sy + 10, 0xFF000000 | c);
            }
        }
    }
}
