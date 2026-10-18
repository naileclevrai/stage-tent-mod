package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Cables on a generator, a distro or a rack. The light tower keeps its own block entity and shares {@link PowerLinks}. */
public class PowerNodeBlockEntity extends BlockEntity {
    private final PowerLinks cables = new PowerLinks(this);

    public PowerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.POWER_NODE_BE.get(), pos, state);
    }

    public PowerLinks cables() {
        return cables;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerNodeBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20L != 0) return;
        be.cables.prune();
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.POWER_NODE_BE.get()) return null;
        return (l, p, s, be) -> serverTick(l, p, s, (PowerNodeBlockEntity) be);
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
        for (PowerLinks.Link link : cables.links()) {
            BlockPos other = link.other();
            box = box.minmax(new AABB(other));
        }
        return box.inflate(0.8);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        cables.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cables.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
