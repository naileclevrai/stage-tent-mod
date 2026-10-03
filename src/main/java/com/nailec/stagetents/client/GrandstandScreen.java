package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.GrandstandBuilder;
import com.nailec.stagetents.network.BuildGrandstandPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

import java.util.function.IntConsumer;

/** Grandstand builder opened with the rigging wrench on a bleacher. */
public class GrandstandScreen extends Screen {
    private static final int W = 220, ROW_H = 24;
    private static int rows = 6, cols = 12, aisle = 6;
    private static DyeColor color = DyeColor.WHITE;

    private final BlockPos pos;

    public GrandstandScreen(BlockPos pos) {
        super(Component.translatable("screen.stagetents.grandstand"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = height / 2 - 70;
        addRenderableWidget(new Slider(x, y, "grandstand.rows", 1, GrandstandBuilder.MAX_ROWS, rows, v -> rows = v));
        addRenderableWidget(new Slider(x, y + ROW_H, "grandstand.width", 1, GrandstandBuilder.MAX_WIDTH, cols, v -> cols = v));
        addRenderableWidget(new Slider(x, y + ROW_H * 2, "grandstand.aisle", 0, GrandstandBuilder.MAX_AISLE, aisle, v -> aisle = v));
        addRenderableWidget(CycleButton.<DyeColor>builder(c -> Component.translatable("color.minecraft." + c.getName()))
                .withValues(DyeColor.values()).withInitialValue(color)
                .create(x, y + ROW_H * 3, W, 20, Component.translatable("screen.stagetents.grandstand.color"), (b, v) -> color = v));
        addRenderableWidget(Button.builder(Component.translatable("screen.stagetents.grandstand.build"), b -> {
            ModNetwork.CHANNEL.sendToServer(new BuildGrandstandPacket(pos, rows, cols, aisle, color.getId()));
            onClose();
        }).bounds(x, y + ROW_H * 4 + 8, W / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + W / 2 + 3, y + ROW_H * 4 + 8, W / 2 - 3, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int x = (width - W) / 2, y = height / 2 - 70;
        g.drawCenteredString(font, title, width / 2, y - 18, 0xFFFFFF);
        int seats = aisle > 0 ? cols - cols / (aisle + 1) : cols;
        g.drawCenteredString(font, Component.translatable("screen.stagetents.grandstand.info", seats * rows, rows, rows - 1),
                width / 2, y + ROW_H * 5 + 14, 0xA0A0A0);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class Slider extends AbstractSliderButton {
        private final String key;
        private final int min, max;
        private final IntConsumer set;

        Slider(int x, int y, String key, int min, int max, int value, IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(min + (int) Math.round(value * (max - min)), min, max);
        }

        @Override
        protected void updateMessage() {
            if (key != null) setMessage(Component.translatable("screen.stagetents." + key, current()));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
