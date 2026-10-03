package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Turnstile state. Server side: badge credit and the cooldown of one passage. Client side: the rotor angle, eased
 * through a third of a turn whenever the server reports someone going through.
 */
public class TurnstileBlockEntity extends BlockEntity {
    public static final int EVENT_TURN = 1;
    public static final int EVENT_BUMP = 2;
    /** Ticks a bump against a locked rotor takes. */
    private static final int BUMP_TICKS = 6;
    /** Ticks a third of a turn takes. */
    public static final int TURN_TICKS = 10;

    private int credit;
    private int cooldown;
    private boolean closeAfterPass;

    // Client animation, in turns of 120 degrees.
    private float from, to;
    private int animTick = TURN_TICKS;
    private int bumpTick = BUMP_TICKS;
    private float bumpDir;

    public TurnstileBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.TURNSTILE_BE.get(), pos, state);
    }

    public int credit() {
        return credit;
    }

    public boolean busy() {
        return cooldown > 0;
    }

    /** Server: a badge was read. */
    public void grant(int ticks) {
        credit = ticks;
    }

    /** Server: somebody pushed the locked rotor. */
    public void bumped() {
        cooldown = BUMP_TICKS + 4;
    }

    /** Server: somebody went through. */
    public void passed() {
        cooldown = TURN_TICKS + 2;
        if (credit > 0) {
            credit = 0;
            closeAfterPass = true;
        }
    }

    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_BUMP) {
            if (level != null && level.isClientSide) {
                bumpTick = 0;
                bumpDir = param == 1 ? 1 : -1;
            }
            return true;
        }
        if (id != EVENT_TURN) return false;
        if (level != null && level.isClientSide) {
            from = angle(1);
            to = Math.round(to) + (param == 1 ? 1 : -1);
            animTick = 0;
        }
        return true;
    }

    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            if (animTick < TURN_TICKS) animTick++;
            if (bumpTick < BUMP_TICKS) bumpTick++;
            return;
        }
        if (cooldown > 0 && --cooldown == 0 && closeAfterPass) {
            closeAfterPass = false;
            close();
        }
        if (credit > 0 && --credit == 0) close();
    }

    private void close() {
        BlockState s = getBlockState();
        if (s.getBlock() instanceof TurnstileBlock && s.getValue(TurnstileBlock.OPEN) && !TurnstileBlock.shouldBeOpen(s)) {
            level.setBlock(worldPosition, s.setValue(TurnstileBlock.OPEN, false), Block.UPDATE_ALL);
        }
    }

    /** Rotor angle in thirds of a turn, eased, at {@code partialTick}. */
    public float angle(float partialTick) {
        float t = Math.min(1F, (animTick + partialTick) / TURN_TICKS);
        // Smooth start and a little settle at the end, like a damped mechanism.
        float e = t < 1 ? 1 - (float) Math.pow(1 - t, 3) : 1;
        float bump = 0;
        if (bumpTick < BUMP_TICKS) {
            float b = (bumpTick + partialTick) / BUMP_TICKS;
            bump = bumpDir * 0.045F * (float) Math.sin(Math.PI * Math.min(1, b));
        }
        return from + (to - from) * e + bump;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Credit", credit);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        credit = tag.getInt("Credit");
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5);
    }
}
