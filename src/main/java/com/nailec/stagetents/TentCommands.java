package com.nailec.stagetents;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.tent.TentPart;
import com.nailec.stagetents.tent.TentShape;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** {@code /stagetents clean [radius]}: removes orphaned invisible tent cells and rebuilds the tents nearby. */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID)
public final class TentCommands {
    private static final int DEFAULT_RADIUS = 48, MAX_RADIUS = 128, TENT_REACH = 72;

    private TentCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal(StageTents.MOD_ID)
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clean")
                        .executes(ctx -> clean(ctx, DEFAULT_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                .executes(ctx -> clean(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int clean(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockPos center = BlockPos.containing(src.getPosition());

        // Every cell that belongs to a tent that could reach into the area.
        List<TentBlockEntity> tents = new ArrayList<>();
        LongOpenHashSet keep = new LongOpenHashSet();
        int reach = radius + TENT_REACH;
        for (int cx = (center.getX() - reach) >> 4; cx <= (center.getX() + reach) >> 4; cx++) {
            for (int cz = (center.getZ() - reach) >> 4; cz <= (center.getZ() + reach) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity e : chunk.getBlockEntities().values()) {
                    if (!(e instanceof TentBlockEntity be)) continue;
                    tents.add(be);
                    BlockPos o = be.getBlockPos();
                    be.placedShape().forEachCell(new TentShape.CellSink() {
                        @Override
                        public void cell(int dx, int dy, int dz, TentPart part, int lvl, int shift) {
                            keep.add(BlockPos.asLong(o.getX() + dx, o.getY() + dy, o.getZ() + dz));
                        }

                        @Override
                        public void light(int dx, int dy, int dz, int lvl) {}
                    });
                }
            }
        }

        int removed = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int y0 = Math.max(level.getMinBuildHeight(), center.getY() - 24);
        int y1 = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 64);
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
                for (int y = y0; y <= y1; y++) {
                    mp.set(x, y, z);
                    if (level.getBlockState(mp).is(ModRegistry.CANVAS.get()) && !keep.contains(mp.asLong())) {
                        level.setBlock(mp, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        removed++;
                    }
                }
            }
        }

        int rebuilt = 0;
        for (TentBlockEntity be : tents) {
            if (be.getBlockPos().distSqr(center) <= (double) reach * reach) {
                be.rebuildCells();
                rebuilt++;
            }
        }
        int r = removed, t = rebuilt;
        src.sendSuccess(() -> Component.translatable("command.stagetents.clean", r, t), true);
        return removed;
    }
}
