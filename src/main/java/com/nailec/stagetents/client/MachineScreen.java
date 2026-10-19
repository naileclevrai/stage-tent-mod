package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.MachineBlockEntity;
import com.nailec.stagetents.furniture.MachineKind;
import com.nailec.stagetents.network.ConfigureMachinePacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Controls of a machine: lift, mast tilt and load for the forklift, outriggers and extension deck for the lifts. */
public class MachineScreen extends Screen {
    private static final int W = 230;

    private final BlockPos pos;
    private MachineKind kind = MachineKind.FORKLIFT;
    private int lift;
    private int tilt;
    private int load;
    private boolean outriggers;
    private boolean deck;

    public MachineScreen(BlockPos pos) {
        super(Component.empty());
        this.pos = pos;
    }

    private void send() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureMachinePacket(pos, lift, tilt, load, outriggers, deck));
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.stagetents." + switch (kind) {
            case FORKLIFT -> "forklift";
            case SCISSOR -> "scissor_lift";
            case RT_SCISSOR -> "rt_scissor_lift";
        });
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            kind = be.kind();
            lift = be.lift();
            tilt = be.tilt();
            load = be.load();
            outriggers = be.outriggers();
            deck = be.deck();
        }
        int x = (width - W) / 2;
        int y = height / 2 - 52;
        String liftKey = kind == MachineKind.FORKLIFT ? "screen.stagetents.machine.fork_height" : "screen.stagetents.machine.platform_height";
        addRenderableWidget(new Slider(x, y, 0, kind.liftSteps, lift, v -> Component.translatable(liftKey, metres(v)), v -> {
            lift = v;
            send();
        }));
        y += 24;
        if (kind.hasTilt()) {
            addRenderableWidget(new Slider(x, y, MachineKind.TILT_MIN, MachineKind.TILT_MAX, tilt,
                    v -> Component.translatable("screen.stagetents.machine.tilt", v), v -> {
                tilt = v;
                send();
            }));
            y += 24;
        }
        if (kind.hasLoad()) {
            addRenderableWidget(Button.builder(loadLabel(), b -> {
                load = (load + 1) % MachineKind.LOADS.length;
                b.setMessage(loadLabel());
                send();
            }).bounds(x, y, W, 20).build());
            y += 24;
        }
        if (kind.hasOutriggers()) {
            addRenderableWidget(Button.builder(toggle("outriggers", outriggers), b -> {
                outriggers = !outriggers;
                b.setMessage(toggle("outriggers", outriggers));
                send();
            }).bounds(x, y, W, 20).build());
            y += 24;
        }
        if (kind.hasDeck()) {
            addRenderableWidget(Button.builder(toggle("deck", deck), b -> {
                deck = !deck;
                b.setMessage(toggle("deck", deck));
                send();
            }).bounds(x, y, W, 20).build());
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x, y + 4, W, 20).build());
    }

    private String metres(int step) {
        double m = kind == MachineKind.FORKLIFT ? step * kind.step : kind.scissor().deck(step * kind.step);
        return String.format(Locale.ROOT, "%.2f", m);
    }

    private Component loadLabel() {
        return Component.translatable("screen.stagetents.machine.load",
                Component.translatable("screen.stagetents.machine.load." + MachineKind.LOADS[load]));
    }

    private static Component toggle(String what, boolean on) {
        return Component.translatable("screen.stagetents.machine." + what + (on ? ".on" : ".off"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, getTitle(), width / 2, height / 2 - 72, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Slider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer set;
        private int last;

        Slider(int x, int y, int min, int max, int value, IntFunction<Component> label, IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) Math.max(1, max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.set = set;
            this.last = value;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(min + (int) Math.round(value * (max - min)), min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(current()));
        }

        @Override
        protected void applyValue() {
            int v = current();
            if (v == last) return;
            last = v;
            set.accept(v);
        }
    }
}
