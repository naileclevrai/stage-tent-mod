package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.CurtainBlock;
import com.nailec.stagetents.network.ConfigureCurtainPacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** Closed, tied back, or parted. The drop and the opening apply to the whole run. */
public class CurtainScreen extends Screen {
    private static final int W = 248;

    private final BlockPos pos;
    private CurtainBlock.Mode mode = CurtainBlock.Mode.CLOSED;
    private int drop = 16;
    private int open = 6;
    private boolean synced;

    public CurtainScreen(BlockPos pos) {
        super(Component.translatable("block.stagetents.curtain"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        if (!synced && minecraft != null && minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.getBlock() instanceof CurtainBlock) {
                mode = state.getValue(CurtainBlock.MODE);
                drop = state.getValue(CurtainBlock.DROP);
                open = state.getValue(CurtainBlock.OPEN);
            }
            synced = true;
        }
        int x = (width - W) / 2;
        int y = height / 2 - 58;
        int bw = (W - 8) / 3;
        addMode(x, y, bw, CurtainBlock.Mode.CLOSED, "screen.stagetents.curtain.closed");
        addMode(x + bw + 4, y, bw, CurtainBlock.Mode.TIED, "screen.stagetents.curtain.tied");
        addMode(x + (bw + 4) * 2, y, bw, CurtainBlock.Mode.PARTED, "screen.stagetents.curtain.parted");
        addRenderableWidget(new DropSlider(x, y + 28, drop, v -> {
            drop = v;
            send();
        }));
        addRenderableWidget(new OpenSlider(x, y + 52, open, v -> {
            open = v;
            send();
        }));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 80, W, 20).build());
    }

    private void addMode(int x, int y, int w, CurtainBlock.Mode next, String key) {
        Button button = Button.builder(Component.translatable(key), b -> choose(next)).bounds(x, y, w, 20).build();
        button.active = mode != next;
        addRenderableWidget(button);
    }

    private void choose(CurtainBlock.Mode next) {
        mode = next;
        send();
        clearWidgets();
        init();
    }

    private void send() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureCurtainPacket(pos, mode.ordinal(), drop, open));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 78, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class DropSlider extends AbstractSliderButton {
        private final java.util.function.IntConsumer set;

        DropSlider(int x, int y, int value, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(),
                    (Mth.clamp(value, CurtainBlock.MIN_DROP, CurtainBlock.MAX_DROP) - CurtainBlock.MIN_DROP)
                            / (double) (CurtainBlock.MAX_DROP - CurtainBlock.MIN_DROP));
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(CurtainBlock.MIN_DROP + (int) Math.round(value * (CurtainBlock.MAX_DROP - CurtainBlock.MIN_DROP)),
                    CurtainBlock.MIN_DROP, CurtainBlock.MAX_DROP);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.stagetents.curtain.drop", CurtainBlock.label(current())));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }

    private static final class OpenSlider extends AbstractSliderButton {
        private final java.util.function.IntConsumer set;

        OpenSlider(int x, int y, int value, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), Mth.clamp(value, 0, CurtainBlock.MAX_OPEN) / (double) CurtainBlock.MAX_OPEN);
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp((int) Math.round(value * CurtainBlock.MAX_OPEN), 0, CurtainBlock.MAX_OPEN);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.stagetents.curtain.open", current() * 12));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
