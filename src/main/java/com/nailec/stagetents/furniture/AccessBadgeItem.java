package com.nailec.stagetents.furniture;

import com.nailec.stagetents.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Access badge read by a turnstile. Right-click in the air to set its id and kind.
 * <ul>
 *     <li>{@link Kind#STANDARD}: opens a badge turnstile with the same id. A blank id only opens a turnstile that
 *     has none.</li>
 *     <li>{@link Kind#MASTER}: opens any badge turnstile.</li>
 *     <li>{@link Kind#SINGLE}: like a standard badge, then it is used up.</li>
 * </ul>
 */
public class AccessBadgeItem extends Item {
    public enum Kind {
        STANDARD, MASTER, SINGLE;

        public static Kind byId(int id) {
            Kind[] v = values();
            return v[Math.floorMod(id, v.length)];
        }
    }

    private static final String ID = "BadgeId";
    private static final String KIND = "BadgeKind";

    public AccessBadgeItem(Properties props) {
        super(props);
    }

    /** Letters, digits, dash and underscore, upper case, at most 16. */
    public static String sanitize(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length() && out.length() < 16; i++) {
            char c = Character.toUpperCase(raw.charAt(i));
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') out.append(c);
        }
        return out.toString();
    }

    public static String idOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : sanitize(tag.getString(ID));
    }

    public static Kind kind(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? Kind.STANDARD : Kind.byId(tag.getInt(KIND));
    }

    public static void program(ItemStack stack, String id, Kind kind) {
        CompoundTag tag = stack.getOrCreateTag();
        String clean = sanitize(id);
        if (clean.isEmpty()) tag.remove(ID);
        else tag.putString(ID, clean);
        if (kind == Kind.STANDARD) tag.remove(KIND);
        else tag.putInt(KIND, kind.ordinal());
        if (tag.isEmpty()) stack.setTag(null);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openBadgeScreen(hand));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (kind(stack) == Kind.MASTER) return Component.translatable("item.stagetents.access_badge.master");
        String id = idOf(stack);
        return id.isEmpty() ? super.getName(stack) : Component.translatable("item.stagetents.access_badge.named", id);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind(stack) == Kind.MASTER;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String id = idOf(stack);
        tooltip.add(Component.translatable(id.isEmpty() ? "tooltip.stagetents.badge.none" : "tooltip.stagetents.badge.id", id)
                .withStyle(ChatFormatting.GOLD));
        Kind kind = kind(stack);
        tooltip.add(Component.translatable("tooltip.stagetents.badge." + kind.name().toLowerCase()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stagetents.badge.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
