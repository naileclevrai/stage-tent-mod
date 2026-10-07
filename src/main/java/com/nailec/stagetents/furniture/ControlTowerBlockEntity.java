package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import com.nailec.stagetents.WorldRepairQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Size of a scaffolding control tower, and the invisible cells that make the decks walkable.
 * Width and depth are counts of bays. Each level is one lift tall; the top deck is {@code levels * LIFT} metres up.
 */
public class ControlTowerBlockEntity extends BlockEntity {
    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private int baysW = 3;
    private int baysD = 2;
    private int levels = 3;
    private boolean roof = true;
    private boolean skirt = true;

    /** What the cells in the world were built from, so a resize removes the old ones. */
    /** Bumped when the cell layout changes, so older towers drop the posts that used to fill the room. */
    private static final int LAYOUT = 5;
    private int layout;
    private int placedW = -1;
    private int placedD = -1;
    private int placedLevels = -1;
    private Direction placedFacing = Direction.NORTH;

    public ControlTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.CONTROL_TOWER_BE.get(), pos, state);
    }

    public int baysW() {
        return baysW;
    }

    public int baysD() {
        return baysD;
    }

    public int levels() {
        return levels;
    }

    public boolean roof() {
        return roof;
    }

    public boolean skirt() {
        return skirt;
    }

    /** Client only: show the new size before the server confirms it. */
    public void preview(int baysW, int baysD, int levels, boolean roof, boolean skirt) {
        if (level != null && !level.isClientSide) return;
        this.baysW = Mth.clamp(baysW, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_WIDTH);
        this.baysD = Mth.clamp(baysD, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_DEPTH);
        this.levels = Mth.clamp(levels, ControlTowerBlock.MIN_LEVELS, ControlTowerBlock.MAX_LEVELS);
        this.roof = roof;
        this.skirt = skirt;
    }

    public void configure(int baysW, int baysD, int levels, boolean roof, boolean skirt) {
        this.baysW = Mth.clamp(baysW, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_WIDTH);
        this.baysD = Mth.clamp(baysD, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_DEPTH);
        this.levels = Mth.clamp(levels, ControlTowerBlock.MIN_LEVELS, ControlTowerBlock.MAX_LEVELS);
        this.roof = roof;
        this.skirt = skirt;
        rebuild();
    }

    public void rebuild() {
        if (level == null || level.isClientSide) return;
        clearCells();
        placeCells();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public void clearCells() {
        if (level == null || level.isClientSide || placedW < 0) return;
        int w = placedW * ControlTowerBlock.BAY;
        int d = placedD * ControlTowerBlock.BAY;
        int top = placedLevels * ControlTowerBlock.LIFT;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int bx = 0; bx < w; bx++) {
            for (int bz = 0; bz < d; bz++) {
                if (bx == 0 && bz == 0) continue;
                int[] at = ControlTowerBlock.cell(placedFacing, bx, bz);
                for (int by = 0; by <= top; by++) {
                    mp.set(worldPosition.getX() + at[0], worldPosition.getY() + by, worldPosition.getZ() + at[1]);
                    if (level.getBlockState(mp).getBlock() instanceof ScaffoldCellBlock) {
                        level.setBlock(mp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), SILENT);
                    }
                }
            }
        }
        // The anchor column above the plate can hold a standard too.
        int[] origin = ControlTowerBlock.cell(placedFacing, 0, 0);
        for (int by = 1; by <= top; by++) {
            mp.set(worldPosition.getX() + origin[0], worldPosition.getY() + by, worldPosition.getZ() + origin[1]);
            if (level.getBlockState(mp).getBlock() instanceof ScaffoldCellBlock) {
                level.setBlock(mp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), SILENT);
            }
        }
        placedW = -1;
    }

    private void placeCells() {
        Direction facing = getBlockState().getValue(FurnitureBlock.FACING);
        BlockState cell = ModRegistry.SCAFFOLD_CELL.get().defaultBlockState();
        walk(facing, baysW, baysD, levels, (pos, deck, post, rail, ladder) -> {
            if (level.isOutsideBuildHeight(pos)) return;
            BlockState there = level.getBlockState(pos);
            if (!there.isAir() && !(there.getBlock() instanceof ScaffoldCellBlock)) return;
            level.setBlock(pos, cell.setValue(ScaffoldCellBlock.DECK, deck)
                    .setValue(ScaffoldCellBlock.POST, post)
                    .setValue(ScaffoldCellBlock.RAIL, rail)
                    .setValue(ScaffoldCellBlock.LADDER, ladder), SILENT);
        });
        placedW = baysW;
        placedD = baysD;
        placedLevels = levels;
        placedFacing = facing;
        layout = LAYOUT;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (layout < LAYOUT && level instanceof net.minecraft.server.level.ServerLevel server) {
            WorldRepairQueue.submit(server, () -> {
                if (!isRemoved() && level.getBlockEntity(worldPosition) == this && layout < LAYOUT) rebuild();
            });
        }
    }

    /** Visits every cell the tower needs. The anchor block itself is skipped. */
    private void walk(Direction facing, int baysW, int baysD, int levels, CellConsumer consumer) {
        int w = baysW * ControlTowerBlock.BAY;
        int d = baysD * ControlTowerBlock.BAY;
        int top = levels * ControlTowerBlock.LIFT;
        for (int bx = 0; bx < w; bx++) {
            for (int bz = 0; bz < d; bz++) {
                int post = worldPost(facing, postOf(bx, bz, w, d));
                // The back-left column is the ladder hatch: no deck, so each floor can be climbed through.
                boolean hatch = bx == 0 && bz == d - 1;
                int ladderSide = hatch ? worldFace(facing, 0.5, 1.0) : 0;
                for (int by = 0; by <= top; by++) {
                    if (bx == 0 && bz == 0 && by == 0) continue;
                    // Deck slab in the block under the walking surface.
                    boolean deck = by % ControlTowerBlock.LIFT == ControlTowerBlock.LIFT - 1 && by < top && !hatch;
                    int rail = 0;
                    if (deck) {
                        int model = 0;
                        if (bx == 0) model |= 1;
                        if (bx == w - 1) model |= 2;
                        // Sheeted towers keep the front open. An open scaffold gets a rail there too.
                        if (!skirt && bz == 0) model |= 4;
                        if (bz == d - 1) model |= 8;
                        rail = worldRail(facing, model);
                    }
                    int ladder = hatch ? ladderSide : 0;
                    // The block under the player's feet stays air, so a block can be set on every deck tile.
                    if (by > 0 && by % ControlTowerBlock.LIFT == 0 && ladder == 0) continue;
                    if (!deck && post == 0 && rail == 0 && ladder == 0) continue;
                    int[] at = ControlTowerBlock.cell(facing, bx, bz);
                    consumer.accept(worldPosition.offset(at[0], by, at[1]), deck, post, rail, ladder);
                }
            }
        }
    }

    /** Which corner of this model block owns a standard. 0 if none. */
    private static int postOf(int bx, int bz, int w, int d) {
        int corner = 0;
        if (owns(bx, bz, 0, 0, w, d)) corner = 1;
        else if (owns(bx, bz, w, 0, w, d)) corner = 2;
        else if (owns(bx, bz, 0, d, w, d)) corner = 3;
        else if (owns(bx, bz, w, d, w, d)) corner = 4;
        else {
            for (int sx = 0; sx <= w; sx += ControlTowerBlock.BAY) {
                for (int sz = 0; sz <= d; sz += ControlTowerBlock.BAY) {
                    if ((sx == 0 || sx == w || sz == 0 || sz == d) && owns(bx, bz, sx, sz, w, d)) {
                        boolean maxX = sx == w;
                        boolean maxZ = sz == d;
                        if (!maxX && !maxZ) return 1;
                        if (maxX && !maxZ) return 2;
                        if (!maxX) return 3;
                        return 4;
                    }
                }
            }
        }
        return corner;
    }

    /** The block just inside the grid point {@code (sx, sz)} is the one that carries that standard. */
    private static boolean owns(int bx, int bz, int sx, int sz, int w, int d) {
        int obx = Math.min(sx, w - 1);
        int obz = Math.min(sz, d - 1);
        return bx == obx && bz == obz && (sx == bx || sx == bx + 1) && (sz == bz || sz == bz + 1);
    }

    private static int worldPost(Direction facing, int modelCorner) {
        if (modelCorner == 0) return 0;
        boolean maxX = modelCorner == 2 || modelCorner == 4;
        boolean maxZ = modelCorner == 3 || modelCorner == 4;
        double[] p = ControlTowerBlock.modelToWorld(facing, maxX ? 0.99 : 0.01, maxZ ? 0.99 : 0.01);
        boolean wMaxX = p[0] > 0.5;
        boolean wMaxZ = p[1] > 0.5;
        if (!wMaxX && !wMaxZ) return 1;
        if (wMaxX && !wMaxZ) return 2;
        if (!wMaxX) return 3;
        return 4;
    }

    /** World rail bits for a model-space mask (bit 0 minX, 1 maxX, 2 minZ, 3 maxZ). */
    private static int worldRail(Direction facing, int model) {
        int out = 0;
        if ((model & 1) != 0) out |= faceBit(facing, 0.0, 0.5);
        if ((model & 2) != 0) out |= faceBit(facing, 1.0, 0.5);
        if ((model & 4) != 0) out |= faceBit(facing, 0.5, 0.0);
        if ((model & 8) != 0) out |= faceBit(facing, 0.5, 1.0);
        return out;
    }

    /** Ladder and rail faces: 1 minX, 2 maxX, 3 minZ, 4 maxZ in world space. */
    static int worldFace(Direction facing, double mx, double mz) {
        int bit = faceBit(facing, mx, mz);
        return switch (bit) {
            case 1 -> 1;
            case 2 -> 2;
            case 4 -> 3;
            default -> 4;
        };
    }

    private static int faceBit(Direction facing, double mx, double mz) {
        double[] p = ControlTowerBlock.modelToWorld(facing, mx, mz);
        boolean xFace = mx == 0.0 || mx == 1.0;
        if (xFace) return p[0] < 0.5 ? 1 : 2;
        return p[1] < 0.5 ? 4 : 8;
    }

    @Override
    public AABB getRenderBoundingBox() {
        Direction facing = getBlockState().hasProperty(FurnitureBlock.FACING)
                ? getBlockState().getValue(FurnitureBlock.FACING) : Direction.NORTH;
        int w = baysW * ControlTowerBlock.BAY;
        int d = baysD * ControlTowerBlock.BAY;
        double minX = 0, minZ = 0, maxX = 1, maxZ = 1;
        for (double mx : new double[]{0, w}) {
            for (double mz : new double[]{0, d}) {
                double[] p = ControlTowerBlock.modelToWorld(facing, mx, mz);
                minX = Math.min(minX, p[0]);
                minZ = Math.min(minZ, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxZ = Math.max(maxZ, p[1]);
            }
        }
        double top = levels * ControlTowerBlock.LIFT + (roof ? 3.2 : 1.6);
        return new AABB(worldPosition.getX() + minX, worldPosition.getY(), worldPosition.getZ() + minZ,
                worldPosition.getX() + maxX, worldPosition.getY() + top, worldPosition.getZ() + maxZ);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        write(tag);
        tag.putInt("PlacedW", placedW);
        tag.putInt("PlacedD", placedD);
        tag.putInt("PlacedLevels", placedLevels);
        tag.putString("PlacedFacing", placedFacing.getSerializedName());
        tag.putInt("Layout", layout);
    }

    private void write(CompoundTag tag) {
        tag.putInt("BaysW", baysW);
        tag.putInt("BaysD", baysD);
        tag.putInt("Levels", levels);
        tag.putBoolean("Roof", roof);
        tag.putBoolean("Skirt", skirt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        read(tag);
        if (tag.contains("PlacedW")) {
            placedW = tag.getInt("PlacedW");
            placedD = tag.getInt("PlacedD");
            placedLevels = tag.getInt("PlacedLevels");
            Direction d = Direction.byName(tag.getString("PlacedFacing"));
            placedFacing = d != null && d.getAxis().isHorizontal() ? d : Direction.NORTH;
        }
        if (tag.contains("Layout")) layout = tag.getInt("Layout");
    }

    private void read(CompoundTag tag) {
        baysW = Mth.clamp(tag.contains("BaysW") ? tag.getInt("BaysW") : baysW, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_WIDTH);
        baysD = Mth.clamp(tag.contains("BaysD") ? tag.getInt("BaysD") : baysD, ControlTowerBlock.MIN_BAYS, ControlTowerBlock.MAX_DEPTH);
        levels = Mth.clamp(tag.contains("Levels") ? tag.getInt("Levels") : levels, ControlTowerBlock.MIN_LEVELS, ControlTowerBlock.MAX_LEVELS);
        if (tag.contains("Roof")) roof = tag.getBoolean("Roof");
        if (tag.contains("Skirt")) skirt = tag.getBoolean("Skirt");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        write(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @FunctionalInterface
    private interface CellConsumer {
        void accept(BlockPos pos, boolean deck, int post, int rail, int ladder);
    }
}
