package com.nailec.stagetents.client;

import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.furniture.AccessBadgeItem;
import com.nailec.stagetents.furniture.TurnstileBlockEntity;
import com.nailec.stagetents.tent.TentShape;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;

public final class ClientHooks {
    private static final int SEARCH_CHUNKS = 5;
    private static final Map<BlockPos, GeneratorSoundInstance> GENERATORS = new HashMap<>();

    private ClientHooks() {}

    /** Starts the diesel idle for a running generator. A second call while it plays does nothing. */
    public static void playGenerator(Level level, BlockPos pos) {
        BlockPos key = pos.immutable();
        GeneratorSoundInstance existing = GENERATORS.get(key);
        if (existing != null && !existing.isStopped()) return;
        GeneratorSoundInstance sound = new GeneratorSoundInstance(level, key);
        GENERATORS.put(key, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void stopGenerator(BlockPos pos) {
        GENERATORS.remove(pos);
    }

    public static void openTentScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof TentBlockEntity be) {
            mc.setScreen(new TentScreen(be));
        }
    }

    public static void openGrandstandScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new GrandstandScreen(pos));
    }

    public static void openTurnstileScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof TurnstileBlockEntity be) {
            mc.setScreen(new TurnstileScreen(be));
        }
    }

    public static void openCannonScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CannonScreen(pos));
    }

    public static void openCableRampScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CableRampScreen(pos));
    }

    public static void openDrapeScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.setScreen(new DrapeScreen(pos, mc.level.getBlockState(pos)));
    }

    public static void openBadgeScreen(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        if (!(stack.getItem() instanceof AccessBadgeItem)) return;
        mc.setScreen(new BadgeScreen(hand, AccessBadgeItem.idOf(stack), AccessBadgeItem.kind(stack)));
    }

    /** Opens the settings of the tent the player is standing in, if any. */
    public static void openTentScreenAround() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.level == null || player == null) return;
        TentBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        int pcx = player.chunkPosition().x, pcz = player.chunkPosition().z;
        for (int cx = pcx - SEARCH_CHUNKS; cx <= pcx + SEARCH_CHUNKS; cx++) {
            for (int cz = pcz - SEARCH_CHUNKS; cz <= pcz + SEARCH_CHUNKS; cz++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;
                for (BlockEntity e : chunk.getBlockEntities().values()) {
                    if (!(e instanceof TentBlockEntity be)) continue;
                    BlockPos p = be.getBlockPos();
                    TentShape s = be.shape();
                    double wx = player.getX() - (p.getX() + 0.5), wz = player.getZ() - (p.getZ() + 0.5);
                    double dy = player.getY() - p.getY();
                    if (dy < -1 || dy > s.H + 1 || !s.contains(s.toLocalX(wx, wz), s.toLocalZ(wx, wz))) continue;
                    double d = wx * wx + wz * wz;
                    if (d < bestDist) {
                        bestDist = d;
                        best = be;
                    }
                }
            }
        }
        if (best != null) {
            mc.setScreen(new TentScreen(best));
        } else {
            player.displayClientMessage(Component.translatable("message.stagetents.no_tent"), true);
        }
    }
}
