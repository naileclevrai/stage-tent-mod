package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.WaterCannonBlock;
import com.nailec.stagetents.furniture.WaterCannonBlockEntity;
import com.nailec.stagetents.network.ConfigureCannonPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Tilt, pan and plume length of a water cannon. */
public class CannonScreen extends Screen {
    private static final int W = 220;

    private final BlockPos pos;
    private int pitch = 7;
    private int range = 4;
    private int yaw;

    public CannonScreen(BlockPos pos) {
        super(Component.translatable("block.stagetents.water_cannon"));
        this.pos = pos;
    }

    private void send() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureCannonPacket(pos, pitch, range, yaw));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.level != null && minecraft.level.getBlockState(pos).getBlock() instanceof WaterCannonBlock) {
            pitch = minecraft.level.getBlockState(pos).getValue(WaterCannonBlock.PITCH);
            if (minecraft.level.getBlockEntity(pos) instanceof WaterCannonBlockEntity cannon) {
                range = cannon.range();
                yaw = cannon.yaw();
            }
        }
        int x = (width - W) / 2;
        int y = height / 2 - 48;
        addRenderableWidget(new Slider(x, y, 0, WaterCannonBlock.MAX, pitch, 0, v -> {
            pitch = v;
            send();
        }));
        addRenderableWidget(new Slider(x, y + 24, 0, (WaterCannonBlockEntity.YAW_MAX - WaterCannonBlockEntity.YAW_MIN) / WaterCannonBlockEntity.YAW_STEP,
                WaterCannonBlockEntity.yawIndex(yaw), 1, v -> {
            yaw = WaterCannonBlockEntity.yawFromIndex(v);
            send();
        }));
        addRenderableWidget(new Slider(x, y + 48, 1, WaterCannonBlockEntity.RANGE_MAX, range, 2, v -> {
            range = v;
            send();
        }));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 76, W, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Slider extends AbstractSliderButton {
        private final int min;
        private final int max;
        /** 0 tilt, 1 pan, 2 range. */
        private final int kind;
        private final java.util.function.IntConsumer set;

        Slider(int x, int y, int min, int max, int value, int kind, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.kind = kind;
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(min + (int) Math.round(value * (max - min)), min, max);
        }

        @Override
        protected void updateMessage() {
            int v = current();
            if (kind == 0) setMessage(Component.translatable("screen.stagetents.cannon_pitch", WaterCannonBlock.elevation(v)));
            else if (kind == 1) setMessage(Component.translatable("screen.stagetents.cannon_yaw", WaterCannonBlockEntity.yawFromIndex(v)));
            else setMessage(Component.translatable("screen.stagetents.cannon_range", WaterCannonBlockEntity.metres(v)));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
