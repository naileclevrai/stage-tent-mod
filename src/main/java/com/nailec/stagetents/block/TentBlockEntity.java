package com.nailec.stagetents.block;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.WorldRepairQueue;
import com.nailec.stagetents.tent.TentParams;
import com.nailec.stagetents.tent.TentPart;
import com.nailec.stagetents.tent.TentShape;
import com.nailec.stagetents.tent.TentType;
import com.nailec.stagetents.tent.MobileStageShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class TentBlockEntity extends BlockEntity {
    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private TentParams params;
    private TentShape shape;
    private AABB renderBounds;
    /**
     * Settings the collision cells currently in the world were built from. Cleanup always uses these, so cells are
     * never orphaned when the settings change without a rebuild (old saves, failed packets...).
     */
    private TentParams placed;
    private Direction placedFacing;
    /** 1 = prototype, 2 = first detailed model, 3 = large festival model. */
    private int mobileCellsVersion = 3;

    /** Bumped whenever the client copy of the params changes so the renderer rebuilds its mesh. */
    private int clientVersion;
    /** Client-side render cache, typed as Object to keep client classes out of common code. */
    public Object clientMesh;

    public TentBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TENT_BE.get(), pos, state);
        params = type().defaults();
    }

    public TentType type() {
        return getBlockState().getBlock() instanceof TentBlock tb ? tb.type : TentType.BIG_TOP;
    }

    public TentParams params() {
        return params;
    }

    public int clientVersion() {
        return clientVersion;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(TentBlock.FACING) ? s.getValue(TentBlock.FACING) : Direction.EAST;
    }

    public TentShape shape() {
        if (shape == null || shape.facing != facing() || shape.params != params) {
            shape = TentShape.create(params, facing());
            renderBounds = null;
        }
        return shape;
    }

    /** Client preview from the settings screen: swaps the params without touching the world. */
    public void previewParams(TentParams p) {
        setParams(p);
        clientVersion++;
    }

    /** Server: rebuilds the collision cells for the new shape and syncs clients. */
    public void applyParams(TentParams p) {
        if (level == null || level.isClientSide) return;
        clearCells();
        setParams(p);
        placeCells();
        BlockState st = getBlockState();
        if (st.hasProperty(TentBlock.HIDDEN) && st.getValue(TentBlock.HIDDEN) == params.showPlate) {
            level.setBlock(worldPosition, st.setValue(TentBlock.HIDDEN, !params.showPlate), Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private void setParams(TentParams p) {
        params = p.copy();
        params.type = type();
        params.clamp();
        shape = null;
    }

    void placeCells() {
        if (level == null || level.isClientSide) return;
        placed = params.copy();
        placedFacing = facing();
        if (type() == TentType.OPUS_4200) mobileCellsVersion = 3;
        setChanged();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockState canvas = ModRegistry.CANVAS.get().defaultBlockState();
        shape().forEachCell(new TentShape.CellSink() {
            @Override
            public void cell(int dx, int dy, int dz, TentPart part, int lvl, int shift) {
                if (free(dx, dy, dz)) {
                    level.setBlock(mp, canvas.setValue(CanvasBlock.PART, part).setValue(CanvasBlock.LEVEL, lvl)
                            .setValue(CanvasBlock.SHIFT, shift), SILENT);
                }
            }

            @Override
            public void light(int dx, int dy, int dz, int lvl) {
                if (free(dx, dy, dz)) {
                    level.setBlock(mp, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lvl), Block.UPDATE_ALL);
                }
            }

            @Override
            public void rig(int dx, int dy, int dz, boolean alongX) {
                if (free(dx, dy, dz)) level.setBlock(mp, rigState(alongX), Block.UPDATE_ALL);
            }

            private boolean free(int dx, int dy, int dz) {
                mp.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                return !level.isOutsideBuildHeight(mp) && level.getBlockState(mp).isAir();
            }
        });
    }

    /** Shape of the cells currently in the world. */
    public TentShape placedShape() {
        return placed == null ? shape() : TentShape.create(placed, placedFacing == null ? facing() : placedFacing);
    }

    void clearCells() {
        if (level == null || level.isClientSide) return;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        TentShape.CellSink cleanup = new TentShape.CellSink() {
            @Override
            public void cell(int dx, int dy, int dz, TentPart part, int lvl, int shift) {
                at(dx, dy, dz);
                if (level.getBlockState(mp).is(ModRegistry.CANVAS.get())) {
                    level.setBlock(mp, Blocks.AIR.defaultBlockState(), SILENT);
                }
            }

            @Override
            public void light(int dx, int dy, int dz, int lvl) {
                at(dx, dy, dz);
                if (level.getBlockState(mp).is(Blocks.LIGHT)) {
                    level.setBlock(mp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }

            @Override
            public void rig(int dx, int dy, int dz, boolean alongX) {
                at(dx, dy, dz);
                BlockState st = level.getBlockState(mp);
                if (st.is(ModRegistry.RIGGING_BAR.get()) || (theatricalPipe() != Blocks.AIR && st.is(theatricalPipe()))) {
                    level.setBlock(mp, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }

            private void at(int dx, int dy, int dz) {
                mp.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
            }
        };
        if (type() == TentType.OPUS_4200 && mobileCellsVersion == 1) {
            MobileStageShape.legacyCells(placedFacing == null ? facing() : placedFacing, cleanup);
            mobileCellsVersion = 3;
        } else if (type() == TentType.OPUS_4200 && mobileCellsVersion == 2) {
            MobileStageShape.previousCells(placed == null ? params : placed,
                    placedFacing == null ? facing() : placedFacing, cleanup);
            mobileCellsVersion = 3;
        } else placedShape().forEachCell(cleanup);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tent", params.save());
        if (type() == TentType.OPUS_4200) tag.putInt("MobileStageCells", mobileCellsVersion);
        if (placed != null) {
            tag.put("Placed", placed.save());
            tag.putString("PlacedFacing", (placedFacing == null ? facing() : placedFacing).getSerializedName());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Tent")) {
            params = TentParams.load(tag.getCompound("Tent"), type());
            shape = null;
        }
        if (tag.contains("Placed")) {
            placed = TentParams.load(tag.getCompound("Placed"), type());
            Direction d = Direction.byName(tag.getString("PlacedFacing"));
            placedFacing = d != null && d.getAxis().isHorizontal() ? d : null;
            if (type() == TentType.OPUS_4200) mobileCellsVersion = Math.max(1, tag.getInt("MobileStageCells"));
        }
        clientVersion++;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (type() == TentType.OPUS_4200 && mobileCellsVersion < 3
                && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            WorldRepairQueue.submit(serverLevel, () -> {
                if (!isRemoved() && level.getBlockEntity(worldPosition) == this && mobileCellsVersion < 3) rebuildCells();
            });
        }
    }

    private static final ResourceLocation THEATRICAL_PIPE = new ResourceLocation("theatrical", "pipe");

    /** The Theatrical rigging pipe when that mod is installed (fixtures only hang from its own supports), else air. */
    private static Block theatricalPipe() {
        Block b = ForgeRegistries.BLOCKS.containsKey(THEATRICAL_PIPE) ? ForgeRegistries.BLOCKS.getValue(THEATRICAL_PIPE) : null;
        return b == null ? Blocks.AIR : b;
    }

    private static BlockState rigState(boolean alongX) {
        Block pipe = theatricalPipe();
        if (pipe != Blocks.AIR) {
            BlockState st = pipe.defaultBlockState();
            for (var prop : st.getProperties()) {
                if (prop.getName().equals("facing") && prop instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dp) {
                    Direction d = alongX ? Direction.EAST : Direction.SOUTH;
                    if (dp.getPossibleValues().contains(d)) st = st.setValue(dp, d);
                }
            }
            return st;
        }
        return ModRegistry.RIGGING_BAR.get().defaultBlockState()
                .setValue(RiggingBarBlock.AXIS, alongX ? Direction.Axis.X : Direction.Axis.Z);
    }

    /** Stretch tents: reads the stretch poles around the plate into {@code base} and applies it. */
    public void rescanStretchPoles(TentParams base) {
        if (level == null || level.isClientSide || type() != TentType.STRETCH) return;
        TentParams p = base.copy();
        List<int[]> found = new ArrayList<>();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int r = StretchPoleBlock.RANGE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    mp.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState st = level.getBlockState(mp);
                    if (st.is(ModRegistry.STRETCH_POLE.get()) && found.size() < TentParams.MAX_STRETCH_POLES) {
                        found.add(new int[]{dx, dz, st.getValue(StretchPoleBlock.HEIGHT) + dy});
                    }
                }
            }
        }
        p.stretchPoles = found;
        applyParams(p);
    }

    /** Removes and rebuilds the collision cells from the current settings. */
    public void rebuildCells() {
        clearCells();
        placeCells();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Tent", params.save());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Frees the client mesh (GPU buffers) when the tent goes away or its chunk unloads. */
    private void releaseClientMesh() {
        if (clientMesh instanceof AutoCloseable c) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        clientMesh = null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        releaseClientMesh();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        releaseClientMesh();
    }

    @Override
    public AABB getRenderBoundingBox() {
        TentShape s = shape();
        if (renderBounds == null) {
            renderBounds = s.renderBounds(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        }
        return renderBounds;
    }
}
