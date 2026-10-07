package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.LightTowerBlock;
import com.nailec.stagetents.furniture.LightTowerBlockEntity;
import com.nailec.stagetents.network.ConfigureLightTowerPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Mast height, the way the lamp bank turns, and how far the floods tilt down. */
public class LightTowerScreen extends Screen {
    private static final int W = 230;

    private final BlockPos pos;
    private int mast = 6;
    private int yaw;
    private int tilt = 20;

    public LightTowerScreen(BlockPos pos) {
        super(Component.translatable("block.stagetents.light_tower"));
        this.pos = pos;
    }

    private void send() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureLightTowerPacket(pos, mast, tilt, yaw));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.getBlock() instanceof LightTowerBlock) mast = state.getValue(LightTowerBlock.HEIGHT);
            BlockEntity be = minecraft.level.getBlockEntity(pos);
            if (be instanceof LightTowerBlockEntity tower) {
                yaw = tower.yaw();
                tilt = tower.tilt();
            }
        }
        int x = (width - W) / 2;
        int y = height / 2 - 46;
        addRenderableWidget(new IntSlider(x, y, LightTowerBlock.MIN_HEIGHT, LightTowerBlock.MAX_HEIGHT, 1, mast,
                v -> Component.translatable("screen.stagetents.light_tower.height", v), v -> {
            mast = v;
            send();
        }));
        addRenderableWidget(new IntSlider(x, y + 24, LightTowerBlockEntity.YAW_MIN, LightTowerBlockEntity.YAW_MAX,
                LightTowerBlockEntity.YAW_STEP, yaw,
                v -> Component.translatable("screen.stagetents.light_tower.yaw", v), v -> {
            yaw = v;
            send();
        }));
        addRenderableWidget(new IntSlider(x, y + 48, 0, LightTowerBlockEntity.TILT_MAX, LightTowerBlockEntity.TILT_STEP, tilt,
                v -> Component.translatable("screen.stagetents.light_tower.tilt", v), v -> {
            tilt = v;
            send();
        }));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 76, W, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, this.height / 2 - 68, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final int step;
        private final java.util.function.IntFunction<Component> label;
        private final java.util.function.IntConsumer set;

        IntSlider(int x, int y, int min, int max, int step, int value,
                  java.util.function.IntFunction<Component> label, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.set = set;
            updateMessage();
        }

        private int current() {
            int raw = min + (int) Math.round(value * (max - min));
            int snapped = Math.round(raw / (float) step) * step;
            return Mth.clamp(snapped, min, max);
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
