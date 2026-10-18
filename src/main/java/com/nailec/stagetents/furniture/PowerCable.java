package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** One lead from an output or an outlet to a single inlet. The server decides; the tool only remembers the first socket. */
public final class PowerCable {
    public static final String POS_KEY = "CablePos";
    public static final String SOCKET_KEY = "CableSocket";

    private PowerCable() {}

    public static boolean armed(ItemStack tool) {
        return tool.getTag() != null && tool.getTag().contains(POS_KEY);
    }

    /**
     * @param blockPoint where the outline ray hit, when this came from a block click
     * @param blockAt    the block that outline ray hit
     */
    public static InteractionResult interact(Level level, Player player, ItemStack tool,
                                             @Nullable Vec3 blockPoint, @Nullable BlockPos blockAt) {
        if (player == null) return InteractionResult.PASS;
        double reach = player.isCreative() ? 5.0 : 4.5;
        PowerSockets.Hit hit = PowerSockets.pick(level, player, reach, blockPoint, blockAt);
        BlockPos machine = blockAt == null ? null : PowerSockets.originOf(level, blockAt);
        if (player.isShiftKeyDown()) {
            if (hit != null) {
                if (!level.isClientSide) {
                    PowerLinks links = PowerSockets.cables(level, hit.pos());
                    if (links != null && links.find(hit.index()) != null) {
                        links.unlink(hit.index());
                        clear(tool);
                        level.playSound(null, hit.pos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.55F, 1.2F);
                        player.displayClientMessage(Component.translatable("message.stagetents.power_unlink"), true);
                    } else if (armed(tool)) {
                        clear(tool);
                        player.displayClientMessage(Component.translatable("message.stagetents.power_cancel"), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (armed(tool)) {
                if (!level.isClientSide) {
                    clear(tool);
                    player.displayClientMessage(Component.translatable("message.stagetents.power_cancel"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return machine == null ? InteractionResult.PASS : InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit == null) {
            return machine == null ? InteractionResult.PASS : InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        link(level, player, tool, hit);
        return InteractionResult.CONSUME;
    }

    private static void link(Level level, Player player, ItemStack tool, PowerSockets.Hit hit) {
        CompoundTag tag = tool.getTag();
        if (tag == null || !tag.contains(POS_KEY)) {
            tool.getOrCreateTag().putLong(POS_KEY, hit.pos().asLong());
            tool.getOrCreateTag().putInt(SOCKET_KEY, hit.index());
            player.displayClientMessage(Component.translatable("message.stagetents.power_pick"), true);
            return;
        }
        BlockPos fromPos = BlockPos.of(tag.getLong(POS_KEY));
        int fromIndex = tag.getInt(SOCKET_KEY);
        if (fromPos.equals(hit.pos()) && fromIndex == hit.index()) {
            clear(tool);
            player.displayClientMessage(Component.translatable("message.stagetents.power_cancel"), true);
            return;
        }
        if (!level.isLoaded(fromPos)) {
            clear(tool);
            player.displayClientMessage(Component.translatable("message.stagetents.power_gone"), true);
            return;
        }
        BlockState fromState = level.getBlockState(fromPos);
        List<PowerSockets.Socket> fromSockets = PowerSockets.of(fromState);
        if (fromSockets == null || fromIndex < 0 || fromIndex >= fromSockets.size()) {
            clear(tool);
            player.displayClientMessage(Component.translatable("message.stagetents.power_gone"), true);
            return;
        }
        if (fromPos.equals(hit.pos())) {
            player.displayClientMessage(Component.translatable("message.stagetents.power_self"), true);
            return;
        }
        PowerSockets.Socket fromSocket = fromSockets.get(fromIndex);
        PowerSockets.Socket toSocket = hit.socket();
        if (fromSocket.role().feeds() == toSocket.role().feeds()) {
            player.displayClientMessage(Component.translatable("message.stagetents.power_mismatch"), true);
            return;
        }
        Vec3 fromAnchor = PowerSockets.anchor(fromPos, fromState.getValue(FurnitureBlock.FACING), fromSocket);
        if (fromAnchor.distanceTo(hit.anchor()) > PowerSockets.REACH) {
            player.displayClientMessage(Component.translatable("message.stagetents.power_far"), true);
            return;
        }
        boolean fromFeeds = fromSocket.role().feeds();
        BlockPos feedPos = fromFeeds ? fromPos : hit.pos();
        int feedIndex = fromFeeds ? fromIndex : hit.index();
        BlockPos inletPos = fromFeeds ? hit.pos() : fromPos;
        int inletIndex = fromFeeds ? hit.index() : fromIndex;
        PowerLinks feed = PowerSockets.ensure(level, feedPos);
        PowerLinks inlet = PowerSockets.ensure(level, inletPos);
        if (feed == null || inlet == null) {
            player.displayClientMessage(Component.translatable("message.stagetents.power_gone"), true);
            return;
        }
        if (feed.find(feedIndex) != null || inlet.find(inletIndex) != null) {
            player.displayClientMessage(Component.translatable("message.stagetents.power_busy"), true);
            return;
        }
        feed.attach(feedIndex, inletPos, inletIndex);
        inlet.attach(inletIndex, feedPos, feedIndex);
        clear(tool);
        level.playSound(null, hit.pos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 0.9F);
        player.displayClientMessage(Component.translatable("message.stagetents.power_link"), true);
    }

    private static void clear(ItemStack tool) {
        tool.removeTagKey(POS_KEY);
        tool.removeTagKey(SOCKET_KEY);
    }
}
