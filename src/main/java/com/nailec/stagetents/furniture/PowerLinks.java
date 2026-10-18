package com.nailec.stagetents.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Cables remembered by one power block. Both ends store the link. The feeding end draws it. */
public final class PowerLinks {
    public record Link(int socket, BlockPos other, int otherSocket) {}

    private final BlockEntity owner;
    private final List<Link> links = new ArrayList<>();

    public PowerLinks(BlockEntity owner) {
        this.owner = owner;
    }

    public List<Link> links() {
        return links;
    }

    @Nullable
    public Link find(int socket) {
        for (Link link : links) {
            if (link.socket == socket) return link;
        }
        return null;
    }

    public void attach(int socket, BlockPos other, int otherSocket) {
        drop(socket);
        links.add(new Link(socket, other.immutable(), otherSocket));
        sync();
    }

    /** Remove the cable on this socket and tell the other end. */
    public void unlink(int socket) {
        Link link = find(socket);
        if (link == null) return;
        links.remove(link);
        sync();
        Level level = owner.getLevel();
        if (level == null) return;
        PowerLinks other = PowerSockets.cables(level, link.other);
        if (other != null) other.drop(link.otherSocket);
    }

    /** Forget every cable, and tell the other ends. Used when the block is broken. */
    public void detach() {
        List<Link> copy = new ArrayList<>(links);
        links.clear();
        sync();
        Level level = owner.getLevel();
        if (level == null) return;
        for (Link link : copy) {
            PowerLinks other = PowerSockets.cables(level, link.other);
            if (other != null) other.drop(link.otherSocket);
        }
    }

    /** Forget a cable whose other end already dropped it. Does not call back. */
    public void drop(int socket) {
        if (links.removeIf(link -> link.socket == socket)) sync();
    }

    /** Drop a cable whose other end is loaded and no longer points back. */
    public void prune() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide || links.isEmpty()) return;
        BlockPos here = owner.getBlockPos();
        boolean changed = links.removeIf(link -> {
            if (!level.isLoaded(link.other)) return false;
            PowerLinks other = PowerSockets.cables(level, link.other);
            if (other == null) return true;
            Link back = other.find(link.otherSocket);
            return back == null || !back.other.equals(here) || back.otherSocket != link.socket;
        });
        if (changed) sync();
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Link link : links) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("S", link.socket);
            entry.putInt("X", link.other.getX());
            entry.putInt("Y", link.other.getY());
            entry.putInt("Z", link.other.getZ());
            entry.putInt("O", link.otherSocket);
            list.add(entry);
        }
        tag.put("Cables", list);
    }

    public void load(CompoundTag tag) {
        links.clear();
        ListTag list = tag.getList("Cables", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            links.add(new Link(entry.getInt("S"), new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")), entry.getInt("O")));
        }
    }

    private void sync() {
        owner.setChanged();
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = owner.getBlockState();
            level.sendBlockUpdated(owner.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }
}
