package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.FriseBlock;
import com.nailec.stagetents.furniture.PendrillonBlock;
import com.nailec.stagetents.network.ConfigureDrapePacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** Height of a pendrillon, or drop of a frise. The slider applies to the whole run. */
public class DrapeScreen extends Screen {
    private static final int W = 220;

    private final BlockPos pos;
    private final boolean pendrillon;
    private final int min;
    private final int max;

    public DrapeScreen(BlockPos pos, BlockState state) {
        super(Component.translatable(state.getBlock() instanceof PendrillonBlock
                ? "block.stagetents.pendrillon" : "block.stagetents.frise"));
        this.pos = pos;
        this.pendrillon = state.getBlock() instanceof PendrillonBlock;
        if (pendrillon) {
            min = 2;
            max = PendrillonBlock.MAX;
        } else {
            min = 1;
            max = FriseBlock.MAX;
        }
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = height / 2 - 24;
        BlockState state = minecraft != null && minecraft.level != null ? minecraft.level.getBlockState(pos) : null;
        int current = min;
        if (state != null && state.getBlock() instanceof PendrillonBlock) current = state.getValue(PendrillonBlock.HEIGHT);
        else if (state != null && state.getBlock() instanceof FriseBlock) current = state.getValue(FriseBlock.DROP);
        addRenderableWidget(new Slider(x, y, min, max, current, pendrillon, v ->
                ModNetwork.CHANNEL.sendToServer(new ConfigureDrapePacket(pos, v))));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, y + 28, W, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 46, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Slider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final boolean meters;
        private final java.util.function.IntConsumer set;

        Slider(int x, int y, int min, int max, int value, boolean meters, java.util.function.IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.meters = meters;
            this.set = set;
            updateMessage();
        }

        private int current() {
            return Mth.clamp(min + (int) Math.round(value * (max - min)), min, max);
        }

        @Override
        protected void updateMessage() {
            if (meters) setMessage(Component.translatable("screen.stagetents.drape.height", current()));
            else setMessage(Component.translatable("screen.stagetents.frise_drop", FriseBlock.label(current())));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
