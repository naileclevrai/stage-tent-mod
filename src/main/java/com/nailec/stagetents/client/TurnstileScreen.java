package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.TurnstileBlock;
import com.nailec.stagetents.furniture.TurnstileBlockEntity;
import com.nailec.stagetents.network.ConfigureTurnstilePacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** Turnstile settings, opened by sneaking with the rigging wrench. */
public class TurnstileScreen extends Screen {
    private static final int W = 232;

    private final TurnstileBlockEntity be;
    private TurnstileBlock.Mode mode;
    private boolean oneWay;
    private String accessId;
    private int holdSeconds;
    private boolean closeOnPass;
    private boolean clicks;
    private boolean pulseOut;
    private int passages;
    private boolean resetCount;
    private EditBox idBox;

    public TurnstileScreen(TurnstileBlockEntity be) {
        super(Component.translatable("screen.stagetents.turnstile.title"));
        this.be = be;
        mode = be.getBlockState().getValue(TurnstileBlock.MODE);
        oneWay = be.oneWay();
        accessId = be.accessId();
        holdSeconds = be.holdTicks() / 20;
        closeOnPass = be.closeOnPass();
        clicks = be.clicks();
        pulseOut = be.pulseOut();
        passages = be.passages();
    }

    private int top() {
        return (height - 268) / 2;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = top();
        addRenderableWidget(CycleButton.builder(TurnstileScreen::modeName)
                .withValues(TurnstileBlock.Mode.values()).withInitialValue(mode)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.mode"), (b, v) -> mode = v));
        y += 24;
        addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("screen.stagetents.turnstile.one_way"),
                        Component.translatable("screen.stagetents.turnstile.both"))
                .withInitialValue(oneWay)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.direction"), (b, v) -> oneWay = v));
        y += 36;
        idBox = new EditBox(font, x, y, W, 20, Component.translatable("screen.stagetents.turnstile.id"));
        idBox.setMaxLength(16);
        idBox.setValue(accessId);
        idBox.setHint(Component.translatable("screen.stagetents.turnstile.id_hint"));
        idBox.setFilter(s -> s.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_'));
        idBox.setResponder(s -> accessId = s);
        addRenderableWidget(idBox);
        setInitialFocus(idBox);
        y += 24;
        addRenderableWidget(new Slider(x, y, 1, 20, holdSeconds, v -> holdSeconds = v));
        y += 24;
        addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("screen.stagetents.turnstile.close_on"),
                        Component.translatable("screen.stagetents.turnstile.close_off"))
                .withInitialValue(closeOnPass)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.after"), (b, v) -> closeOnPass = v));
        y += 24;
        addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("screen.stagetents.turnstile.sound_on"),
                        Component.translatable("screen.stagetents.turnstile.sound_off"))
                .withInitialValue(clicks)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.sound"), (b, v) -> clicks = v));
        y += 24;
        addRenderableWidget(CycleButton.booleanBuilder(
                        Component.translatable("screen.stagetents.turnstile.redstone_on"),
                        Component.translatable("screen.stagetents.turnstile.redstone_off"))
                .withInitialValue(pulseOut)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.redstone"), (b, v) -> pulseOut = v));
        y += 24;
        addRenderableWidget(Button.builder(countLabel(), b -> {
            resetCount = true;
            passages = 0;
            b.setMessage(countLabel());
        }).bounds(x, y, W, 20).build());
        y += 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.stagetents.turnstile.apply"), b -> apply())
                .bounds(x, y, W / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + W / 2 + 3, y, W / 2 - 3, 20).build());
    }

    private Component countLabel() {
        return Component.translatable(resetCount ? "screen.stagetents.turnstile.reset_done" : "screen.stagetents.turnstile.reset", passages);
    }

    private void apply() {
        ModNetwork.CHANNEL.sendToServer(new ConfigureTurnstilePacket(be.getBlockPos(), mode.ordinal(), oneWay,
                accessId, holdSeconds, closeOnPass, clicks, pulseOut, resetCount));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int x = (width - W) / 2;
        int y = top();
        g.fill(x - 10, y - 22, x + W + 10, y + 252, 0xE0101418);
        g.fill(x - 10, y - 22, x + W + 10, y - 20, 0xFFD4AF37);
        g.drawCenteredString(font, title, width / 2, y - 14, 0xFFF2E6C8);
        g.drawString(font, Component.translatable("screen.stagetents.turnstile.id"), x, y + 49, 0xFFB7B7B7);
        super.render(g, mouseX, mouseY, partialTick);
        Component hint = hint();
        g.drawCenteredString(font, hint, width / 2, y + 236, 0xFF9A9A9A);
    }

    private Component hint() {
        Component line = switch (mode) {
            case FREE -> Component.translatable("screen.stagetents.turnstile.hint.free");
            case LOCKED -> Component.translatable("screen.stagetents.turnstile.hint.locked");
            case BADGE -> accessId.isBlank()
                    ? Component.translatable("screen.stagetents.turnstile.hint.badge")
                    : Component.translatable("screen.stagetents.turnstile.hint.badge_id", accessId.toUpperCase());
        };
        if (!oneWay) return line;
        return Component.translatable("screen.stagetents.turnstile.hint.with_way", line);
    }

    private static Component modeName(TurnstileBlock.Mode mode) {
        return Component.translatable("screen.stagetents.turnstile.mode." + mode.getSerializedName());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class Slider extends AbstractSliderButton {
        private final int min, max;
        private final IntConsumer set;

        Slider(int x, int y, int min, int max, int value, IntConsumer set) {
            super(x, y, W, 20, Component.empty(), (Mth.clamp(value, min, max) - min) / (double) (max - min));
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
            setMessage(Component.translatable("screen.stagetents.turnstile.hold", current()));
        }

        @Override
        protected void applyValue() {
            set.accept(current());
        }
    }
}
