package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Beer left in a keg, gas left in a cylinder, and the hose ends. Both sides of a hose store the other position. */
public class DraftBlockEntity extends BlockEntity {
    private int beer = DraftBeer.KEG_SERVINGS;
    private int gas = DraftBeer.CYLINDER_SERVINGS;
    private BlockPos beerPeer;
    private BlockPos gasPeer;
    private final List<Vec3> beerPath = new ArrayList<>();
    private final List<Vec3> gasPath = new ArrayList<>();
    private long pourUntil;

    public DraftBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DRAFT_BE.get(), pos, state);
    }

    public int beer() {
        return beer;
    }

    public int gas() {
        return gas;
    }

    public long pourUntil() {
        return pourUntil;
    }

    public BlockPos peer(DraftBeer.Line line) {
        return line == DraftBeer.Line.BEER ? beerPeer : gasPeer;
    }

    /** Pins between this block and {@link #peer}, in that order. Empty when the hose runs straight. */
    public List<Vec3> path(DraftBeer.Line line) {
        return line == DraftBeer.Line.BEER ? beerPath : gasPath;
    }

    public void setPeer(DraftBeer.Line line, BlockPos pos) {
        setRoute(line, pos, List.of());
    }

    public void setRoute(DraftBeer.Line line, BlockPos pos, List<Vec3> points) {
        BlockPos stored = pos == null ? null : pos.immutable();
        List<Vec3> path = path(line);
        if (line == DraftBeer.Line.BEER) beerPeer = stored;
        else gasPeer = stored;
        path.clear();
        if (stored != null) path.addAll(points);
        sync();
    }

    public void addBeer(int delta) {
        beer = DraftBeer.clampBeer(beer + delta);
        sync();
    }

    public void addGas(int delta) {
        gas = DraftBeer.clampGas(gas + delta);
        sync();
    }

    public void setPourUntil(long until) {
        pourUntil = until;
        sync();
    }

    /** Drop both hoses. A missing neighbour is cleaned up later, when its chunk is loaded. */
    public void detach() {
        if (level == null) return;
        clearPeer(DraftBeer.Line.BEER);
        clearPeer(DraftBeer.Line.GAS);
    }

    private void clearPeer(DraftBeer.Line line) {
        BlockPos other = peer(line);
        setPeer(line, null);
        if (other != null && level.isLoaded(other) && level.getBlockEntity(other) instanceof DraftBlockEntity be
                && worldPosition.equals(be.peer(line))) {
            be.setPeer(line, null);
        }
    }

    /** Forget a hose whose other end is loaded and no longer agrees. */
    public void prune() {
        if (level == null) return;
        prune(DraftBeer.Line.BEER);
        prune(DraftBeer.Line.GAS);
    }

    private void prune(DraftBeer.Line line) {
        BlockPos other = peer(line);
        if (other == null || !level.isLoaded(other)) return;
        DraftBeer.Kind expect = switch (line) {
            case BEER -> DraftBeer.kind(getBlockState()) == DraftBeer.Kind.TAP ? DraftBeer.Kind.KEG : DraftBeer.Kind.TAP;
            case GAS -> DraftBeer.kind(getBlockState()) == DraftBeer.Kind.CYLINDER ? DraftBeer.Kind.KEG : DraftBeer.Kind.CYLINDER;
        };
        if (DraftBeer.kind(level.getBlockState(other)) != expect
                || !(level.getBlockEntity(other) instanceof DraftBlockEntity be)
                || !worldPosition.equals(be.peer(line))) {
            setPeer(line, null);
        }
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
        AABB box = new AABB(worldPosition).inflate(0.5).expandTowards(0, 2, 0);
        if (beerPeer != null) box = box.minmax(new AABB(beerPeer));
        if (gasPeer != null) box = box.minmax(new AABB(gasPeer));
        for (Vec3 p : beerPath) box = box.minmax(new AABB(p, p));
        for (Vec3 p : gasPath) box = box.minmax(new AABB(p, p));
        return box.inflate(0.5);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        DraftBeer.Kind kind = DraftBeer.kind(getBlockState());
        if (kind == DraftBeer.Kind.KEG) tag.putInt("Beer", beer);
        if (kind == DraftBeer.Kind.CYLINDER) tag.putInt("Gas", gas);
        if (beerPeer != null) tag.putLong("BeerLink", beerPeer.asLong());
        if (gasPeer != null) tag.putLong("GasLink", gasPeer.asLong());
        if (!beerPath.isEmpty()) tag.put("BeerPath", DraftBeer.writePoints(beerPath));
        if (!gasPath.isEmpty()) tag.put("GasPath", DraftBeer.writePoints(gasPath));
        if (pourUntil != 0) tag.putLong("PourUntil", pourUntil);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Beer")) beer = DraftBeer.clampBeer(tag.getInt("Beer"));
        if (tag.contains("Gas")) gas = DraftBeer.clampGas(tag.getInt("Gas"));
        beerPeer = tag.contains("BeerLink") ? BlockPos.of(tag.getLong("BeerLink")) : null;
        gasPeer = tag.contains("GasLink") ? BlockPos.of(tag.getLong("GasLink")) : null;
        beerPath.clear();
        gasPath.clear();
        if (tag.contains("BeerPath", Tag.TAG_LIST)) DraftBeer.readPoints((ListTag) tag.get("BeerPath"), beerPath);
        if (tag.contains("GasPath", Tag.TAG_LIST)) DraftBeer.readPoints((ListTag) tag.get("GasPath"), gasPath);
        pourUntil = tag.getLong("PourUntil");
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
