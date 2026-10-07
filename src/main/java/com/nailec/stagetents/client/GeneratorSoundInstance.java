package com.nailec.stagetents.client;

import com.nailec.stagetents.ModSounds;
import com.nailec.stagetents.furniture.GeneratorBlock;
import com.nailec.stagetents.furniture.LightTowerBlock;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Diesel idle that follows one running generator and stops when it is switched off or removed. */
public class GeneratorSoundInstance extends AbstractTickableSoundInstance {
    private final Level level;
    private final BlockPos pos;

    public GeneratorSoundInstance(Level level, BlockPos pos) {
        super(ModSounds.GENERATOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.pos = pos;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.85F;
        this.pitch = 1.0F;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GeneratorBlock && state.getValue(GeneratorBlock.RUNNING) && !isStopped()) return;
        if (state.getBlock() instanceof LightTowerBlock && state.getValue(LightTowerBlock.RUNNING) && !isStopped()) return;
        ClientHooks.stopGenerator(pos);
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
