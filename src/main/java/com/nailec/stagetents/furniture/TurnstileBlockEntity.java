package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Turnstile state. Server side: badge credit, one passage, and the shove when the rotor is locked. Client side: the
 * rotor, pushed by whoever is walking through the lane.
 */
public class TurnstileBlockEntity extends BlockEntity {
    /** Ticks a bump against a locked rotor takes. */
    private static final int BUMP_TICKS = 6;
    /** Ticks before a badge gate closes again after a passage. */
    private static final int TURN_TICKS = 10;
    /** Distance from the centre at which a body meets the arm, and past the centre at which it has cleared it. */
    private static final double CONTACT = 0.5, CLEAR = 0.35;

    private int credit;
    private int cooldown;
    private boolean closeAfterPass;

    private String accessId = "";
    private boolean oneWay;
    private int holdTicks = TurnstileBlock.BADGE_TICKS;
    private boolean closeOnPass = true;
    private boolean clicks = true;
    private boolean pulseOut;
    private int passages;
    /** Ticks left on a comparator spike after a passage. */
    private int pulse;

    // Rotor, in thirds of a turn. {@code base} is the detent; {@code shown} eases toward the arm the body is pushing.
    private float base, shown, prevShown;
    private UUID pusher;
    private int side;
    private boolean committed;

    public TurnstileBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TURNSTILE_BE.get(), pos, state);
    }

    public int credit() {
        return credit;
    }

    public String accessId() {
        return accessId;
    }

    public boolean oneWay() {
        return oneWay;
    }

    public int holdTicks() {
        return holdTicks;
    }

    public boolean closeOnPass() {
        return closeOnPass;
    }

    public boolean clicks() {
        return clicks;
    }

    public boolean pulseOut() {
        return pulseOut;
    }

    public int passages() {
        return passages;
    }

    /** Comparator reads 15 while a passage spike is running. */
    public boolean emitting() {
        return pulse > 0;
    }

    /** A standard badge matches the id. A blank turnstile id accepts any badge. A master badge accepts itself. */
    public boolean accepts(ItemStack stack) {
        if (!(stack.getItem() instanceof AccessBadgeItem)) return false;
        if (AccessBadgeItem.kind(stack) == AccessBadgeItem.Kind.MASTER) return true;
        if (accessId.isEmpty()) return true;
        return accessId.equals(AccessBadgeItem.idOf(stack));
    }

    /** Server: a badge was read. */
    public void grant(int ticks) {
        credit = ticks;
    }

    /** Server: apply the wrench screen. */
    public void configure(TurnstileBlock.Mode mode, boolean oneWay, String accessId, int holdSeconds,
                          boolean closeOnPass, boolean clicks, boolean pulseOut, boolean resetCount) {
        if (level == null) return;
        this.oneWay = oneWay;
        this.accessId = AccessBadgeItem.sanitize(accessId);
        this.holdTicks = Mth.clamp(holdSeconds, 1, 20) * 20;
        this.closeOnPass = closeOnPass;
        this.clicks = clicks;
        this.pulseOut = pulseOut;
        if (resetCount) passages = 0;
        if (mode != TurnstileBlock.Mode.BADGE) {
            credit = 0;
            closeAfterPass = false;
        }
        BlockState cur = getBlockState();
        BlockState s = cur.setValue(TurnstileBlock.MODE, mode);
        boolean open = TurnstileBlock.shouldBeOpen(s) || (mode == TurnstileBlock.Mode.BADGE && credit > 0);
        level.setBlock(worldPosition, s.setValue(TurnstileBlock.OPEN, open), Block.UPDATE_ALL);
        sync();
    }

    public void tick() {
        if (level == null) return;
        if (!level.isClientSide) {
            if (pulse > 0 && --pulse == 0) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            if (cooldown > 0 && --cooldown == 0 && closeAfterPass) {
                closeAfterPass = false;
                close();
            }
            if (credit > 0 && --credit == 0) close();
        }
        Lane lane = lane();
        if (level.isClientSide) clientTurn(lane);
        else serverTurn(lane);
    }

    private void close() {
        BlockState s = getBlockState();
        if (s.getBlock() instanceof TurnstileBlock && s.getValue(TurnstileBlock.OPEN) && !TurnstileBlock.shouldBeOpen(s)) {
            level.setBlock(worldPosition, s.setValue(TurnstileBlock.OPEN, false), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ the body in the lane

    private record Lane(LivingEntity body, double along) {}

    /** Whoever is pushing, preferring the body already engaged, else the one nearest the arm. */
    private Lane lane() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TurnstileBlock)) return null;
        Direction facing = state.getValue(TurnstileBlock.FACING), right = facing.getClockWise();
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        LivingEntity best = null;
        double bestAlong = 0, bestAbs = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(0.3, 0, 0.3))) {
            if (!e.isAlive() || e.isSpectator()) continue;
            double ox = e.getX() - cx, oz = e.getZ() - cz;
            double across = ox * right.getStepX() + oz * right.getStepZ();
            double along = ox * facing.getStepX() + oz * facing.getStepZ();
            if (across < -0.2 || Math.abs(along) > 0.8) continue;
            if (e.getUUID().equals(pusher)) return new Lane(e, along);
            if (Math.abs(along) < bestAbs) {
                best = e;
                bestAlong = along;
                bestAbs = Math.abs(along);
            }
        }
        return best == null ? null : new Lane(best, bestAlong);
    }

    /** 0 when the body meets the arm on its side, 1 when it has cleared it on the other. */
    private double progress(double along) {
        return Mth.clamp((along - side * CONTACT) / (-side * (CONTACT + CLEAR)), 0, 1);
    }

    private void engage(Lane lane) {
        if (lane == null) {
            pusher = null;
            committed = false;
            return;
        }
        if (!lane.body.getUUID().equals(pusher)) {
            pusher = lane.body.getUUID();
            side = lane.along < 0 ? -1 : 1;
            committed = false;
        }
    }

    /** Walking forward (negative along) turns the rotor one way; coming back the other. */
    private float dir() {
        return side < 0 ? 1 : -1;
    }

    private void clientTurn(Lane lane) {
        prevShown = shown;
        BlockState state = getBlockState();
        boolean open = state.getBlock() instanceof TurnstileBlock && state.getValue(TurnstileBlock.OPEN);
        engage(lane);
        float target = base;
        if (lane != null) {
            boolean allowed = open && !(oneWay && side > 0);
            double u = progress(lane.along);
            if (!allowed) u = Math.min(u, 0.05);
            if (committed) {
                target = base;
            } else if (allowed && u >= 1) {
                base += dir();
                committed = true;
                target = base;
            } else {
                target = base + dir() * (float) u;
            }
        }
        // Follow the push closely, settle with a little damping.
        float k = lane != null && !committed ? 0.75F : 0.35F;
        shown += (target - shown) * k;
        if (Math.abs(target - shown) < 1e-4F) shown = target;
    }

    private void serverTurn(Lane lane) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof TurnstileBlock)) return;
        boolean open = state.getValue(TurnstileBlock.OPEN);
        engage(lane);
        if (lane == null) return;
        Direction facing = state.getValue(TurnstileBlock.FACING);
        boolean allowed = open && !(oneWay && side > 0);
        if (!allowed) {
            // A closed rotor knocks once; a one-way lane keeps pushing back, because nothing else blocks that way.
            if (!committed && Math.abs(lane.along) < 0.7) {
                if (!open) {
                    if (cooldown == 0) shove(lane.body, facing, lane.along);
                } else {
                    resist(lane.body, facing, lane.along);
                }
                if (cooldown == 0) {
                    cooldown = BUMP_TICKS + 4;
                    click(0.25F, 2.0F);
                }
            }
            return;
        }
        // Count the passage when the body clears the arm, the same moment the rotor clicks home.
        if (!committed && progress(lane.along) >= 1) {
            committed = true;
            cooldown = TURN_TICKS + 2;
            passages++;
            if (pulseOut) {
                pulse = 20;
                level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            }
            if (credit > 0 && closeOnPass) {
                credit = 0;
                closeAfterPass = true;
            }
            click(0.35F, 1.6F);
            sync();
        }
    }

    private void click(float volume, float pitch) {
        if (clicks) level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, volume, pitch);
    }

    private void sync() {
        setChanged();
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
        level.updateNeighbourForOutputSignal(worldPosition, s.getBlock());
    }

    /** Push the body back to the side of the lane it came from. */
    private static void shove(LivingEntity body, Direction facing, double along) {
        double s = Math.abs(along) < 1e-4 ? -1 : Math.signum(along);
        body.knockback(0.45, -facing.getStepX() * s, -facing.getStepZ() * s);
        body.hurtMarked = true;
    }

    /** Nudge a body back out of a one-way lane, every tick, without launching it. */
    private static void resist(LivingEntity body, Direction facing, double along) {
        double fx = facing.getStepX(), fz = facing.getStepZ();
        Vec3 m = body.getDeltaMovement();
        double alongV = m.x * fx + m.z * fz;
        double out = Math.abs(along) < 1e-4 ? -1 : Math.signum(along);
        body.setDeltaMovement(m.x - alongV * fx + fx * out * 0.16, m.y, m.z - alongV * fz + fz * out * 0.16);
        body.hurtMarked = true;
    }

    /** Rotor angle in thirds of a turn at {@code partialTick}. */
    public float angle(float partialTick) {
        return prevShown + (shown - prevShown) * partialTick;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Credit", credit);
        if (!accessId.isEmpty()) tag.putString("AccessId", accessId);
        tag.putBoolean("OneWay", oneWay);
        tag.putInt("Hold", holdTicks);
        tag.putBoolean("CloseOnPass", closeOnPass);
        tag.putBoolean("Clicks", clicks);
        tag.putBoolean("PulseOut", pulseOut);
        tag.putInt("Passages", passages);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        credit = tag.getInt("Credit");
        accessId = AccessBadgeItem.sanitize(tag.getString("AccessId"));
        oneWay = tag.getBoolean("OneWay");
        holdTicks = tag.contains("Hold") ? Mth.clamp(tag.getInt("Hold"), 20, 400) : TurnstileBlock.BADGE_TICKS;
        closeOnPass = !tag.contains("CloseOnPass") || tag.getBoolean("CloseOnPass");
        clicks = !tag.contains("Clicks") || tag.getBoolean("Clicks");
        pulseOut = tag.getBoolean("PulseOut");
        passages = tag.getInt("Passages");
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

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5);
    }
}
