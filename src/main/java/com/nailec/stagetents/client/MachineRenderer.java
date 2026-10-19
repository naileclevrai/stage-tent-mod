package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.MachineBlock;
import com.nailec.stagetents.furniture.MachineBlockEntity;
import com.nailec.stagetents.furniture.MachineKind;
import com.nailec.stagetents.furniture.MachineKind.Scissor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Forklift and scissor lifts. Static pieces are baked per paint, the moving ones per lift position. */
public class MachineRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    private static final int CACHE_SIZE = 96;
    private static final Map<Long, MeshKit.Mesh> CACHE = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, MeshKit.Mesh> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public MachineRenderer(BlockEntityRendererProvider.Context ctx) {}

    /** Everything the renderer needs, read from a block entity or made up for the item. */
    public record Pose(MachineKind kind, DyeColor color, double rise, int tilt, int load, float out, float deck) {}

    @Override
    public void render(MachineBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof MachineBlock block)) return;
        Direction facing = state.getValue(FurnitureBlock.FACING);
        Pose pose = new Pose(block.kind, state.getValue(FurnitureBlock.COLOR), be.rise(partialTick), be.tilt(), be.load(),
                be.outPos(partialTick), be.deckPos(partialTick));
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);
        BlockPos front = pos.relative(facing);
        draw(pose, ps, buffers, dy -> level == null ? light : brightest(level, front.above(Math.max(0, dy)), light),
                level != null && be.moving() && (level.getGameTime() / 6) % 2 == 0);
        ps.popPose();
    }

    interface Light {
        int at(int dy);
    }

    /** Draws a machine in model space at the current pose. {@code flash} lights the beacons. */
    static void draw(Pose p, PoseStack ps, MultiBufferSource buffers, Light light, boolean flash) {
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite plain = atlas.getSprite(MeshKit.PLAIN);
        TextureAtlasSprite decals = atlas.getSprite(MeshKit.DECALS);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        int paint = MachineMeshes.paint(p.color());
        int dye = p.color().getId();
        int base = light.at(0);
        if (p.kind() == MachineKind.FORKLIFT) {
            int q = (int) Math.round(p.rise() * 64);
            double rise = q / 64.0;
            double inner = MachineKind.innerRise(rise);
            mesh(key(p.kind(), 0, dye, 0), b -> MachineMeshes.forkliftBody(b, paint)).draw(ps.last(), vc, plain, decals, base);
            mesh(key(p.kind(), 1, 0, p.tilt() + 32), b -> MachineMeshes.forkliftTiltRams(b, p.tilt())).draw(ps.last(), vc, plain, decals, base);
            ps.pushPose();
            ps.translate(0, 0.30, 1.30);
            ps.mulPose(Axis.XP.rotationDegrees(p.tilt()));
            ps.translate(0, -0.30, -1.30);
            int high = light.at((int) (rise + 1));
            mesh(key(p.kind(), 2, dye, 0), b -> MachineMeshes.forkliftOuterMast(b, paint)).draw(ps.last(), vc, plain, decals, base);
            mesh(key(p.kind(), 3, 0, q), b -> MachineMeshes.forkliftInnerMast(b, inner)).draw(ps.last(), vc, plain, decals, high);
            mesh(key(p.kind(), 4, p.load(), q), b -> {
                MachineMeshes.forkliftCarriage(b, rise, p.load());
                MachineMeshes.forkliftChains(b, rise, inner);
            }).draw(ps.last(), vc, plain, decals, high);
            ps.popPose();
            if (flash) beacon(ps, vc, plain, decals, 0.82, MachineKind.FL_GUARD_TOP + 0.03, 3.12, 0.058, 0.13);
            return;
        }
        Scissor s = p.kind().scissor();
        int q = (int) Math.round(p.rise() * 64);
        double rise = q / 64.0;
        boolean potholes = rise > 0.02;
        mesh(key(p.kind(), 0, dye, potholes ? 1 : 0), b -> MachineMeshes.scissorBase(b, p.kind(), paint, potholes))
                .draw(ps.last(), vc, plain, decals, base);
        if (p.kind().hasOutriggers()) {
            int oq = Math.round(p.out() * 40);
            mesh(key(p.kind(), 1, 0, oq), b -> MachineMeshes.outriggers(b, oq / 40f)).draw(ps.last(), vc, plain, decals, base);
        }
        mesh(key(p.kind(), 2, 0, q), b -> MachineMeshes.scissorStack(b, p.kind(), rise)).draw(ps.last(), vc, plain, decals,
                light.at((int) (s.deck(rise) * 0.5)));
        double deckY = s.deck(rise);
        int top = light.at((int) Math.floor(deckY + 0.5));
        ps.pushPose();
        ps.translate(0, deckY, 0);
        mesh(key(p.kind(), 3, dye, 0), b -> MachineMeshes.scissorPlatform(b, p.kind(), paint)).draw(ps.last(), vc, plain, decals, top);
        ps.translate(0, 0, -s.extension() * p.deck());
        mesh(key(p.kind(), 4, dye, 0), b -> MachineMeshes.scissorDeck(b, p.kind(), paint)).draw(ps.last(), vc, plain, decals, top);
        ps.popPose();
        if (flash) {
            if (p.kind() == MachineKind.RT_SCISSOR) {
                for (double x : new double[]{0.30, 1.70}) beacon(ps, vc, plain, decals, x, s.chassisTop() + 0.04, 0.42, 0.048, 0.10);
            } else {
                beacon(ps, vc, plain, decals, s.x1() - 0.12, s.chassisTop() + 0.03, MachineKind.SL08_LENGTH - 0.12, 0.038, 0.08);
            }
        }
    }

    /** Lit amber dome drawn over a beacon while the machine moves. */
    private static void beacon(PoseStack ps, VertexConsumer vc, TextureAtlasSprite plain, TextureAtlasSprite decals,
                               double x, double y, double z, double r, double h) {
        mesh((1L << 61) | (((long) (x * 1000) & 0xFFFF) << 32) | (((long) (y * 1000) & 0xFFFF) << 16) | ((long) (z * 1000) & 0xFFFF), b ->
                b.cyl(x, y - 0.002, z, x, y + h, z, r, r * 0.8, 10, 0xFFFFC040, true, MeshKit.GLOW))
                .draw(ps.last(), vc, plain, decals, 0xF000F0);
    }

    private static long key(MachineKind kind, int part, int a, int b) {
        return kind.ordinal() | ((long) part << 2) | ((long) a << 8) | ((long) b << 16) | (1L << 62);
    }

    private static MeshKit.Mesh mesh(long key, Consumer<MeshKit.Builder> build) {
        MeshKit.Mesh m = CACHE.get(key);
        if (m == null) {
            MeshKit.Builder b = new MeshKit.Builder();
            build.accept(b);
            m = b.finish();
            CACHE.put(key, m);
        }
        return m;
    }

    private static int brighter(int a, int b) {
        int sa = (a >> 20) & 15, sb = (b >> 20) & 15;
        int ba = (a >> 4) & 15, bb = (b >> 4) & 15;
        return sa + ba >= sb + bb ? a : b;
    }

    private static int brightest(Level level, BlockPos pos, int fallback) {
        int light = brighter(fallback, LevelRenderer.getLightColor(level, pos));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            light = brighter(light, LevelRenderer.getLightColor(level, cursor.setWithOffset(pos, dir)));
        }
        return light;
    }

    @Override
    public boolean shouldRenderOffScreen(MachineBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
