package com.nailec.stagetents.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.tent.TentShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

public class TentRenderer implements BlockEntityRenderer<TentBlockEntity> {
    public static final ResourceLocation CANVAS_TEXTURE = new ResourceLocation(StageTents.MOD_ID, "textures/entity/canvas.png");
    private static final ResourceLocation WOOD_TEXTURE = new ResourceLocation(StageTents.MOD_ID, "textures/entity/floor_wood.png");
    private static final ResourceLocation CARPET_TEXTURE = new ResourceLocation(StageTents.MOD_ID, "textures/entity/floor_carpet.png");
    private static final ResourceLocation TILES_TEXTURE = new ResourceLocation(StageTents.MOD_ID, "textures/entity/floor_tiles.png");
    private static final int RELIGHT_TICKS = 40;
    /** Vertices relit per frame and per tent; a full pass of a big tent spreads over a few frames. */
    private static final int RELIGHT_BUDGET = 6000;
    /** Rebuild at most this often while settings change continuously (dragging a slider). */
    private static final long REBUILD_NANOS = 60_000_000L;
    /** Distances past the edge of the tent, in blocks. */
    private static final double WIND_RANGE = 40, DETAIL_RANGE = 72, INNER_RANGE = 64, RELIGHT_RANGE = 160;
    private static final int FLAG_SEGMENTS = 6;

    public TentRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(TentBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;
        MeshBuilder old = be.clientMesh instanceof MeshBuilder m ? m : null;
        MeshBuilder mesh = old;
        boolean stale = old == null || old.version != be.clientVersion() || old.facing != be.facing();
        // While a slider is dragged the settings change every frame; keep the previous mesh for a few frames.
        if (stale && (old == null || System.nanoTime() - old.builtAt > REBUILD_NANOS)) {
            mesh = TentMeshes.build(be.shape(), be.clientVersion());
            be.clientMesh = mesh;
            if (old != null) old.close();
        }
        if (mesh == null) return;
        BlockPos origin = be.getBlockPos();
        TentShape shape = mesh.g;

        // Where is the camera relative to the tent?
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double wx = cam.x - (origin.getX() + 0.5), wz = cam.z - (origin.getZ() + 0.5), wy = cam.y - origin.getY();
        double beyond = Math.max(0, Math.sqrt(wx * wx + wz * wz) - shape.footprint());
        double lx = shape.toLocalX(wx, wz), lz = shape.toLocalZ(wx, wz);
        double roof = beyond > 0 ? Double.NaN : shape.roofHeight(lx, lz);
        // Inside the envelope every outside face of the canvas points away from the camera.
        boolean inside = !Double.isNaN(roof) && wy < roof - 0.2 && wy > -1 && shape.perimeterDistance(lx, lz) < -0.3;

        long time = level.getGameTime();
        if (!mesh.lit) mesh.relightAll(level, origin, time);
        else if (beyond < RELIGHT_RANGE) mesh.relightTick(level, origin, time, RELIGHT_TICKS, RELIGHT_BUDGET);

        float t = time + partialTick;
        float wind = beyond < WIND_RANGE ? windAmplitude(level, partialTick) : 0;
        PoseStack.Pose pose = poseStack.last();
        RenderType canvas = RenderType.entityCutout(CANVAS_TEXTURE);
        ShaderInstance shader = TentShaders.tent();
        Layers draw;
        MeshBuilder m = mesh;
        if (shader != null) {
            // GPU path: the mesh lives in vertex buffers, only a few uniforms change per frame.
            org.joml.Matrix4f modelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.pose());
            var camera = shader.getUniform("TentCamera");
            if (camera != null) camera.set((float) (cam.x - origin.getX()), (float) (cam.y - origin.getY()), (float) (cam.z - origin.getZ()));
            var windUniform = shader.getUniform("TentWind");
            if (windUniform != null) {
                float speed = 0.12F + wind * 2.5F;
                // Both waves (x1 and x2.3) repeat after 20 pi: keeps the phase small enough for float precision.
                windUniform.set(wind, (float) ((double) t * speed % (20 * Math.PI)));
            }
            draw = (layer, type) -> m.draw(layer, type, modelView, shader);
        } else {
            draw = (layer, type) -> m.emit(layer, pose, buffers.getBuffer(type), packedOverlay, t, wind);
        }
        draw.layer(MeshBuilder.SOLID, canvas);
        if (!inside) draw.layer(MeshBuilder.OUTER, canvas);
        // From far away the inside only shows through rolled-up walls.
        if (inside || beyond < INNER_RANGE || be.params().walls == com.nailec.stagetents.tent.WallMode.OPEN) {
            draw.layer(MeshBuilder.INNER, canvas);
        }
        if (beyond < DETAIL_RANGE) draw.layer(MeshBuilder.DETAIL, canvas);
        if (be.params().flags) {
            flags(mesh.g, origin, level, t, wind, pose, buffers.getBuffer(canvas), packedOverlay);
        }
        if (be.params().sign && shape instanceof com.nailec.stagetents.tent.RectShape rect && beyond < DETAIL_RANGE) {
            signText(rect, origin, level, poseStack, buffers);
        }
        if (mesh.hasLayer(MeshBuilder.FLOOR)) {
            ResourceLocation tex = switch (be.params().floor) {
                case WOOD, DARK_WOOD -> WOOD_TEXTURE;
                case WHITE -> TILES_TEXTURE;
                default -> CARPET_TEXTURE;
            };
            draw.layer(MeshBuilder.FLOOR, RenderType.entityCutout(tex));
        }
        if (mesh.hasLayer(MeshBuilder.DECK)) {
            draw.layer(MeshBuilder.DECK, RenderType.entityCutout(CARPET_TEXTURE));
        }
        // Glass stays on the batched path: translucent panes must be drawn after everything behind them.
        if (mesh.hasLayer(MeshBuilder.GLASS)) {
            mesh.emit(MeshBuilder.GLASS, poseStack.last(), buffers.getBuffer(TentRenderTypes.glass(CANVAS_TEXTURE)), packedOverlay, t, wind);
        }
    }

    @FunctionalInterface
    private interface Layers {
        void layer(int layer, RenderType type);
    }

    /** Lettering on the front sign: a big title and a smaller line, centred and fitted to the board. */
    private static void signText(com.nailec.stagetents.tent.RectShape g, BlockPos origin, Level level, PoseStack ps, MultiBufferSource buffers) {
        var p = g.params;
        if (p.signTitle.isEmpty() && p.signText.isEmpty()) return;
        double[] b = g.signBoard();
        var font = Minecraft.getInstance().font;
        double fx = b[0] + 0.006, width = (b[4] - b[3]) * 0.92, height = b[2] - b[1], cz = (b[3] + b[4]) / 2;
        double wx = g.toWorldX(fx, cz), wz = g.toWorldZ(fx, cz);
        double nx = g.toWorldX(1, 0), nz = g.toWorldZ(1, 0);
        int light = LevelRenderer.getLightColor(level, origin.offset((int) Math.round(wx + nx), (int) Math.floor(b[1] + height / 2), (int) Math.round(wz + nz)));
        int color = 0xFF000000 | p.signTextColor;
        ps.pushPose();
        ps.translate(0.5 + wx, b[1] + height / 2, 0.5 + wz);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(nx, nz))));
        boolean both = !p.signTitle.isEmpty() && !p.signText.isEmpty();
        if (!p.signTitle.isEmpty()) {
            float s = fit(font.width(p.signTitle), width, height * (both ? 0.58 : 0.8));
            ps.pushPose();
            ps.translate(0, both ? height * 0.14 : 0, 0);
            ps.scale(s, -s, s);
            font.drawInBatch(p.signTitle, -font.width(p.signTitle) / 2F, -4F, color, false, ps.last().pose(), buffers,
                    net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, light);
            ps.popPose();
        }
        if (!p.signText.isEmpty()) {
            float s = fit(font.width(p.signText), width, height * (both ? 0.24 : 0.5));
            ps.pushPose();
            ps.translate(0, both ? -height * 0.3 : 0, 0);
            ps.scale(s, -s, s);
            font.drawInBatch(p.signText, -font.width(p.signText) / 2F, -4F, color, false, ps.last().pose(), buffers,
                    net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, light);
            ps.popPose();
        }
        ps.popPose();
    }

    /** Scale (blocks per font pixel) that fits a line of {@code pixels} into width x line height. */
    private static float fit(int pixels, double width, double lineHeight) {
        return (float) Math.min(lineHeight / 9.0, width / Math.max(1, pixels));
    }

    /** Flutter amplitude from the weather: a breath when calm, clearly moving in rain, flapping in a storm. */
    static float windAmplitude(Level level, float partialTick) {
        float rain = level.getRainLevel(partialTick), thunder = level.getThunderLevel(partialTick);
        return 0.012F + 0.04F * rain + 0.09F * thunder;
    }

    /** Pennants on the tent's peaks, waving with time. */
    private static void flags(TentShape g, BlockPos origin, Level level, float t, float wind, PoseStack.Pose pose, VertexConsumer vc, int overlay) {
        List<double[]> mounts = g.flagMounts();
        if (mounts.isEmpty()) return;
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int col = MeshBuilder.opaque(g.params.stripes ? g.params.colorA : g.params.colorB);
        int back = MeshBuilder.shade(col, 0.8F);
        double scale = g.H > 10 ? 1.0 : 0.6;
        for (int f = 0; f < mounts.size(); f++) {
            double[] mount = mounts.get(f);
            double mx = mount[0], mz = mount[2];
            double top = mount[1] - 0.02, bottom = top - 0.45 * scale, len = 1.3 * scale;
            int light = LevelRenderer.getLightColor(level, origin.offset(
                    (int) Math.round(g.toWorldX(mx, mz)), (int) Math.ceil(top), (int) Math.round(g.toWorldZ(mx, mz))));
            float phase = t * (0.18F + wind * 3F) + f * 1.7F;
            double[] prev = null;
            for (int k = 0; k <= FLAG_SEGMENTS; k++) {
                double u = k / (double) FLAG_SEGMENTS;
                double x = mx + 0.08 + u * len;
                double z = mz + (0.16 + wind * 2.5) * scale * u * Math.sin(phase + u * 4.0);
                double hy = (top - bottom) / 2 * (1 - u); // narrows to a point
                double mid = (top + bottom) / 2 - 0.08 * u * scale;
                double[] cur = {x, mid + hy, mid - hy, z, u};
                if (prev != null) {
                    double dz = cur[3] - prev[3], dx = cur[0] - prev[0];
                    double nl = Math.hypot(dx, dz);
                    double nlx = -dz / nl, nlz = dx / nl;
                    flagQuad(g, m, n, vc, prev, cur, nlx, nlz, col, light, overlay, false);
                    flagQuad(g, m, n, vc, prev, cur, -nlx, -nlz, back, light, overlay, true);
                }
                prev = cur;
            }
        }
    }

    private static void flagQuad(TentShape g, Matrix4f m, Matrix3f n, VertexConsumer vc, double[] a, double[] b,
                                 double nlx, double nlz, int col, int light, int overlay, boolean reverse) {
        double[][] q = {{a[0], a[2], a[3], a[4]}, {b[0], b[2], b[3], b[4]}, {b[0], b[1], b[3], b[4]}, {a[0], a[1], a[3], a[4]}};
        float nx = (float) g.toWorldX(nlx, nlz), nz = (float) g.toWorldZ(nlx, nlz);
        for (int k = 0; k < 4; k++) {
            double[] v = q[reverse ? 3 - k : k];
            vc.vertex(m, (float) (0.5 + g.toWorldX(v[0], v[2])), (float) v[1], (float) (0.5 + g.toWorldZ(v[0], v[2])))
                    .color((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 0xFF)
                    .uv((float) v[3], (float) v[1] / 2F)
                    .overlayCoords(overlay)
                    .uv2(light)
                    .normal(n, nx, 0, nz)
                    .endVertex();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TentBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
