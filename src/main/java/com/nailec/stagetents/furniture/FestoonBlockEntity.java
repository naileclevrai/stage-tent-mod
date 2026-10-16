package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** The other posts this one is strung to. Both ends remember the cable. */
public class FestoonBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();

    public FestoonBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FESTOON_BE.get(), pos, state);
    }

    public List<BlockPos> links() {
        return links;
    }

    public int linkCount() {
        return links.size();
    }

    public boolean linkedTo(BlockPos pos) {
        return links.contains(pos);
    }

    public void addLink(BlockPos pos) {
        if (!links.contains(pos)) links.add(pos.immutable());
        sync();
    }

    public void removeLink(BlockPos pos) {
        if (links.remove(pos)) sync();
    }

    /** Drop every cable, and tell the other ends. */
    public void detach() {
        if (level == null) return;
        List<BlockPos> copy = new ArrayList<>(links);
        links.clear();
        for (BlockPos other : copy) {
            if (level.getBlockEntity(other) instanceof FestoonBlockEntity be) be.removeLink(worldPosition);
        }
        sync();
    }

    /** Forget a cable whose other post has gone. */
    public void prune() {
        if (level == null || links.isEmpty()) return;
        boolean changed = links.removeIf(p -> level.isLoaded(p) && (!(level.getBlockState(p).getBlock() instanceof FestoonBlock)
                || !(level.getBlockEntity(p) instanceof FestoonBlockEntity other) || !other.linkedTo(worldPosition)));
        if (changed) sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        int height = getBlockState().getBlock() instanceof FestoonBlock ? getBlockState().getValue(FestoonBlock.HEIGHT) : 3;
        AABB box = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + height + 0.5, worldPosition.getZ() + 1);
        for (BlockPos link : links) box = box.minmax(new AABB(link));
        return box.inflate(0.5);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (BlockPos link : links) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", link.getX());
            entry.putInt("Y", link.getY());
            entry.putInt("Z", link.getZ());
            list.add(entry);
        }
        tag.put("Links", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        links.clear();
        ListTag list = tag.getList("Links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            links.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
        }
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
