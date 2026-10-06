package com.nailec.stagetents;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

/** Runs world repairs after startup, never from inside a chunk-load callback. */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldRepairQueue {
    private static final int PER_TICK = 128;
    private static final Map<ServerLevel, ArrayDeque<Runnable>> JOBS = new WeakHashMap<>();

    private WorldRepairQueue() {}

    public static synchronized void submit(ServerLevel level, Runnable job) {
        JOBS.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(job);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (int i = 0; i < PER_TICK; i++) {
            Runnable job;
            synchronized (WorldRepairQueue.class) {
                ArrayDeque<Runnable> queue = JOBS.get(level);
                if (queue == null) return;
                job = queue.pollFirst();
                if (queue.isEmpty()) JOBS.remove(level);
            }
            if (job != null) job.run();
        }
    }
}
