package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.AccessBadgeItem;
import com.nailec.stagetents.network.ConfigureBadgePacket;
import com.nailec.stagetents.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/** Programs the id and kind of an access badge. */
public class BadgeScreen extends Screen {
    private static final int W = 220;

    private final InteractionHand hand;
    private String accessId;
    private AccessBadgeItem.Kind kind;
    private EditBox idBox;

    public BadgeScreen(InteractionHand hand, String accessId, AccessBadgeItem.Kind kind) {
        super(Component.translatable("screen.stagetents.badge"));
        this.hand = hand;
        this.accessId = accessId;
        this.kind = kind;
    }

    private int top() {
        return (height - 148) / 2;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = top() + 28;
        idBox = new EditBox(font, x, y, W, 20, Component.translatable("screen.stagetents.turnstile.id"));
        idBox.setMaxLength(16);
        idBox.setValue(accessId);
        idBox.setHint(Component.translatable("screen.stagetents.turnstile.id_hint"));
        idBox.setFilter(s -> s.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_'));
        idBox.setResponder(s -> accessId = s);
        addRenderableWidget(idBox);
        setInitialFocus(idBox);
        y += 28;
        addRenderableWidget(CycleButton.builder((AccessBadgeItem.Kind k) ->
                        Component.translatable("screen.stagetents.badge.kind." + k.name().toLowerCase()))
                .withValues(AccessBadgeItem.Kind.values()).withInitialValue(kind)
                .create(x, y, W, 20, Component.translatable("screen.stagetents.turnstile.mode"), (b, v) -> kind = v));
        y += 36;
        addRenderableWidget(Button.builder(Component.translatable("screen.stagetents.turnstile.apply"), b -> {
            ModNetwork.CHANNEL.sendToServer(new ConfigureBadgePacket(hand == InteractionHand.MAIN_HAND, accessId, kind.ordinal()));
            onClose();
        }).bounds(x, y, W / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x + W / 2 + 3, y, W / 2 - 3, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int x = (width - W) / 2;
        int y = top();
        g.fill(x - 10, y - 8, x + W + 10, y + 140, 0xE0101418);
        g.fill(x - 10, y - 8, x + W + 10, y - 6, 0xFFD4AF37);
        g.drawCenteredString(font, title, width / 2, y, 0xFFF2E6C8);
        g.drawString(font, Component.translatable("screen.stagetents.turnstile.id"), x, y + 16, 0xFFB7B7B7);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, Component.translatable("screen.stagetents.badge.info." + kind.name().toLowerCase()),
                width / 2, y + 118, 0xFF9A9A9A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
