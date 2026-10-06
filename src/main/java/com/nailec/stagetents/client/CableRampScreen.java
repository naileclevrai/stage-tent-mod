package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.CableRampBlock;
import com.nailec.stagetents.network.ConfigureCableRampPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** How many grooves one cable-ramp module has. */
public class CableRampScreen extends Screen {
    private static final int W = 220;

    private final BlockPos pos;
    private int channels = 2;

    public CableRampScreen(BlockPos pos) {
        super(Component.translatable("block.stagetents.cable_ramp"));
        this.pos = pos;
    }

    private void send() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureCableRampPacket(pos, channels));
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockState(pos).getBlock() instanceof CableRampBlock) {
            channels = minecraft.level.getBlockState(pos).getValue(CableRampBlock.CHANNELS);
        }
        int x = (width - W) / 2;
        int y = height / 2 - 20;
        addRenderableWidget(new Slider(x, y, channels, v -> {
            channels = v;
            send();
        }));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 28, W, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 42, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Slider extends AbstractSliderButton {
        private final java.util.function.IntConsumer set;

        Slider(int x, int y, int value, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, 1, 5) - 1) / 4.0);
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(1 + (int) Math.round(value * 4), 1, 5);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.stagetents.cable_channels", current()));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
