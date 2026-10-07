package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.ControlTowerBlock;
import com.nailec.stagetents.furniture.ControlTowerBlockEntity;
import com.nailec.stagetents.network.ConfigureControlTowerPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Width, depth and height of a scaffolding control tower, plus the roof and the side sheets. */
public class ControlTowerScreen extends Screen {
    private static final int W = 230;

    private final BlockPos pos;
    private int baysW = 3;
    private int baysD = 2;
    private int floors = 3;
    private boolean roof = true;
    private boolean skirt = true;

    public ControlTowerScreen(BlockPos pos) {
        super(Component.translatable("block.stagetents.control_tower"));
        this.pos = pos;
    }

    private void send() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof ControlTowerBlockEntity be) {
            be.preview(baysW, baysD, floors, roof, skirt);
        }
        ModNetwork.CHANNEL.sendToServer(new ConfigureControlTowerPacket(pos, baysW, baysD, floors, roof, skirt));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof ControlTowerBlockEntity be) {
            baysW = be.baysW();
            baysD = be.baysD();
            floors = be.levels();
            roof = be.roof();
            skirt = be.skirt();
        }
        int x = (width - W) / 2;
        int y = this.height / 2 - 78;
        addRenderableWidget(new IntSlider(x, y, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_WIDTH, baysW,
                v -> Component.translatable("screen.stagetents.tower.width", v * ControlTowerBlock.BAY), v -> {
            baysW = v;
            send();
        }));
        addRenderableWidget(new IntSlider(x, y + 24, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_DEPTH, baysD,
                v -> Component.translatable("screen.stagetents.tower.depth", v * ControlTowerBlock.BAY), v -> {
            baysD = v;
            send();
        }));
        addRenderableWidget(new IntSlider(x, y + 48, ControlTowerBlock.MIN_LEVELS, ControlTowerBlock.MAX_LEVELS, floors,
                v -> Component.translatable("screen.stagetents.tower.levels", v, v * ControlTowerBlock.LIFT), v -> {
            floors = v;
            send();
        }));
        addRenderableWidget(Button.builder(roofLabel(), b -> {
            roof = !roof;
            b.setMessage(roofLabel());
            send();
        }).bounds(x, y + 76, W, 20).build());
        addRenderableWidget(Button.builder(skirtLabel(), b -> {
            skirt = !skirt;
            b.setMessage(skirtLabel());
            send();
        }).bounds(x, y + 100, W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 126, W, 20).build());
    }

    private Component roofLabel() {
        return Component.translatable(roof ? "screen.stagetents.tower.roof_on" : "screen.stagetents.tower.roof_off");
    }

    private Component skirtLabel() {
        return Component.translatable(skirt ? "screen.stagetents.tower.skirt_on" : "screen.stagetents.tower.skirt_off");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, this.height / 2 - 96, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final java.util.function.IntFunction<Component> label;
        private final java.util.function.IntConsumer set;

        IntSlider(int x, int y, int min, int max, int value,
                  java.util.function.IntFunction<Component> label, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.set = set;
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
            set.accept(current());
        }
    }
}
