package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nailec.stagetents.furniture.ControlTowerBlock;
import com.nailec.stagetents.furniture.ControlTowerBlockEntity;
import com.nailec.stagetents.furniture.FurnitureBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;

/** Scaffolding control tower. One baked mesh per size, roof, sheets and dye. */
public class ControlTowerRenderer implements BlockEntityRenderer<ControlTowerBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final ResourceLocation METAL = new ResourceLocation("minecraft", "block/iron_block");
    private static final ResourceLocation PLANKS = new ResourceLocation("minecraft", "block/oak_planks");

    private static final int GALV = 0xFFD5D8DE;
    private static final int GALV_DIM = 0xFF9AA1AA;
    private static final int GALV_DARK = 0xFF6E757E;
    private static final int JACK = 0xFFE0B34A;
    private static final int PLATE = 0xFF3E4348;
    private static final int WOOD = 0xFFC6A36C;
    private static final int WOOD_DIM = 0xFF8D6844;
    private static final int WOOD_EDGE = 0xFF6B4E32;

    private static final Mesh[] CACHE = new Mesh[8];
    private static int cacheCursor;

    public ControlTowerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ControlTowerBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof ControlTowerBlock)) return;
        Level level = be.getLevel();
        DyeColor dye = state.getValue(FurnitureBlock.COLOR);
        float[] rgb = dye.getTextureDiffuseColors();
        int tint = 0xFF000000 | ((int) (rgb[0] * 255) << 16) | ((int) (rgb[1] * 255) << 8) | (int) (rgb[2] * 255);
        // The dye sits in the high bits. Roof, sheets and the bay counts stay in the low bits, so a toggle
        // cannot be hidden by the colour.
        long key = be.baysW()
                | ((long) be.baysD() << 4)
                | ((long) be.levels() << 8)
                | (be.roof() ? 1L << 12 : 0L)
                | (be.skirt() ? 1L << 13 : 0L)
                | ((long) (tint & 0xFFFFFF) << 16);
        Mesh mesh = cached(key, be, tint);

        Direction facing = state.getValue(FurnitureBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        ps.translate(-0.5, 0, -0.5);

        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        TextureAtlasSprite metal = atlas.getSprite(METAL);
        TextureAtlasSprite planks = atlas.getSprite(PLANKS);
        TextureAtlasSprite wool = atlas.getSprite(WOOL);
        int top = be.levels() * ControlTowerBlock.LIFT + 3;
        // Sample in front of the tower. A block placed on the deck must not zero the light of the whole floor.
        BlockPos outside = be.getBlockPos().relative(facing);
        int[] lights = new int[top + 1];
        for (int dy = 0; dy <= top; dy++) {
            lights[dy] = level == null ? light : brighter(light, brightest(level, outside.above(dy)));
        }
        mesh.draw(ps.last(), vc, metal, planks, wool, lights);
        ps.popPose();
    }

    /** Packed uv2 light. Sky is in the high bits, block light in the low bits. */
    private static int brighter(int a, int b) {
        int sa = (a >> 20) & 15, sb = (b >> 20) & 15;
        int ba = (a >> 4) & 15, bb = (b >> 4) & 15;
        return sa + ba >= sb + bb ? a : b;
    }

    private static int brightest(Level level, BlockPos pos) {
        int light = LevelRenderer.getLightColor(level, pos);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            light = brighter(light, LevelRenderer.getLightColor(level, cursor.setWithOffset(pos, dir)));
        }
        return light;
    }

    private static Mesh cached(long key, ControlTowerBlockEntity be, int tint) {
        for (Mesh mesh : CACHE) if (mesh != null && mesh.key == key) return mesh;
        Mesh mesh = Mesh.build(key, be.baysW(), be.baysD(), be.levels(), be.roof(), be.skirt(), tint);
        CACHE[cacheCursor] = mesh;
        cacheCursor = (cacheCursor + 1) % CACHE.length;
        return mesh;
    }

    /** Baked quads in model space. Material 0 metal, 1 planks, 2 wool. */
    private static final class Mesh {
        final long key;
        final float[] xyz;
        final float[] uv;
        final byte[] mat;
        final int[] color;
        final int quads;

        private Mesh(long key, float[] xyz, float[] uv, byte[] mat, int[] color, int quads) {
            this.key = key;
            this.xyz = xyz;
            this.uv = uv;
            this.mat = mat;
            this.color = color;
            this.quads = quads;
        }

        void draw(PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite metal, TextureAtlasSprite planks, TextureAtlasSprite wool, int[] lights) {
            Matrix4f m = pose.pose();
            Matrix3f n = pose.normal();
            for (int q = 0; q < quads; q++) {
                TextureAtlasSprite sprite = switch (mat[q]) {
                    case 1 -> planks;
                    case 2 -> wool;
                    default -> metal;
                };
                int argb = color[q];
                int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
                int base = q * 12;
                float y = xyz[base + 1];
                int lit = lights[Math.min(lights.length - 1, Math.max(0, (int) y))];
                float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
                for (int v = 0; v < 4; v++) {
                    int i = base + v * 3;
                    float uu = u0 + uv[q * 8 + v * 2] * (u1 - u0);
                    float vv = v0 + uv[q * 8 + v * 2 + 1] * (v1 - v0);
                    vc.vertex(m, xyz[i], xyz[i + 1], xyz[i + 2]).color(r, g, b, 255).uv(uu, vv)
                            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lit).normal(n, 0, 1, 0).endVertex();
                }
            }
        }

        static Mesh build(long key, int baysW, int baysD, int levels, boolean roof, boolean skirt, int tint) {
            Builder b = new Builder();
            double w = baysW * (double) ControlTowerBlock.BAY;
            double d = baysD * (double) ControlTowerBlock.BAY;
            double lift = ControlTowerBlock.LIFT;
            double top = levels * lift;
            double crown = roof ? top + lift - 0.45 : top + 1.22;
            double tube = 0.042;
            double ledger = 0.028;

            for (int ix = 0; ix <= baysW; ix++) {
                for (int iz = 0; iz <= baysD; iz++) {
                    if (!shell(ix, iz, baysW, baysD)) continue;
                    double x = ix * bay(), z = iz * bay();
                    b.box(x - 0.11, 0, z - 0.11, x + 0.11, 0.025, z + 0.11, PLATE, 0);
                    b.tube(x, 0.02, z, x, 0.22, z, 0.03, JACK, 0);
                    b.tube(x, 0.16, z, x, crown, z, tube, GALV, 0);
                    for (double y = 0.5; y < crown - 0.05; y += 0.5) {
                        b.disc(x, y, z, 0.095, 0.016, GALV_DIM);
                    }
                }
            }

            for (int k = 0; k <= levels; k++) {
                double y = k == 0 ? 0.28 : k * lift - 0.06;
                grid(b, baysW, baysD, y, ledger, GALV_DARK, !skirt);
            }
            braces(b, baysW, baysD, levels, lift, ledger * 0.85, skirt);

            for (int k = 1; k <= levels; k++) {
                double y = k * lift;
                for (int ix = 0; ix < baysW; ix++) {
                    for (int iz = 0; iz < baysD; iz++) deckFrame(b, ix, iz, y);
                }
                planks(b, baysW, baysD, y);
                rails(b, baysW, baysD, y, ledger, skirt);
            }
            ladder(b, d, top, ledger);

            if (roof) roof(b, baysW, baysD, w, d, crown, ledger, tint);
            if (skirt) sheets(b, baysW, baysD, top, tint);
            return b.finish(key);
        }

        private static double bay() {
            return ControlTowerBlock.BAY;
        }

        /** A standard sits on the outer rectangle. The room inside stays clear. */
        private static boolean shell(int ix, int iz, int baysW, int baysD) {
            return ix == 0 || ix == baysW || iz == 0 || iz == baysD;
        }

        /** One ledger along a whole face, clamped onto every standard it crosses. */
        private static void ledger(Builder b, double x0, double y, double z0, double x1, double y2, double z1, int bays, boolean alongX, double r, int col) {
            b.tube(x0, y, z0, x1, y2, z1, r, col, 0);
            double bay = bay();
            for (int i = 0; i <= bays; i++) {
                if (!alongX && (i == 0 || i == bays)) continue;
                double x = alongX ? x0 + i * bay : x0;
                double z = alongX ? z0 : z0 + i * bay;
                b.box(x - 0.055, y - 0.04, z - 0.055, x + 0.055, y + 0.04, z + 0.055, GALV_DIM, 0);
            }
        }

        /** Ledgers on the sides and the back. The front ledger closes the frame only on an open scaffold. */
        private static void grid(Builder b, int baysW, int baysD, double y, double r, int col, boolean front) {
            double bay = bay();
            double w = baysW * bay, d = baysD * bay;
            ledger(b, 0, y, d, w, y, d, baysW, true, r, col);
            if (front) ledger(b, 0, y, 0, w, y, 0, baysW, true, r, col);
            ledger(b, 0, y, 0, 0, y, d, baysD, false, r, col);
            ledger(b, w, y, 0, w, y, d, baysD, false, r, col);
        }

        /**
         * Open scaffold only. Every bay on all four faces gets a cross pinned to the ledgers
         * and the standards, so the wall reads as one frame.
         */
        private static void braces(Builder b, int baysW, int baysD, int levels, double lift, double r, boolean skirt) {
            if (skirt) return;
            double bay = bay();
            double w = baysW * bay, d = baysD * bay;
            for (int k = 0; k < levels; k++) {
                double y0 = k == 0 ? 0.28 : k * lift - 0.06;
                double y1 = (k + 1) * lift - 0.06;
                double mid = (y0 + y1) * 0.5;
                for (int ix = 0; ix < baysW; ix++) {
                    double x0 = ix * bay, x1 = x0 + bay;
                    wallBay(b, x0, y0, 0, x1, y1, 0, mid, true, r);
                    wallBay(b, x0, y0, d, x1, y1, d, mid, true, r);
                }
                for (int iz = 0; iz < baysD; iz++) {
                    double z0 = iz * bay, z1 = z0 + bay;
                    wallBay(b, 0, y0, z0, 0, y1, z1, mid, false, r);
                    wallBay(b, w, y0, z0, w, y1, z1, mid, false, r);
                }
            }
        }

        /** One bay of an outer wall. Along X when {@code alongX}, otherwise the bay runs along Z. */
        private static void wallBay(Builder b, double x0, double y0, double z0, double x1, double y1, double z1, double mid, boolean alongX, double r) {
            b.tube(x0, y0, z0, x1, y1, z1, r, GALV, 0);
            b.tube(x1, y0, z1, x0, y1, z0, r, GALV_DIM, 0);
            if (alongX) {
                b.tube(x0, mid, z0, x1, mid, z1, r * 0.9, GALV_DARK, 0);
                double mx = (x0 + x1) * 0.5;
                b.tube(mx, y0, z0, mx, y1, z1, r * 0.75, GALV, 0);
            } else {
                b.tube(x0, mid, z0, x1, mid, z1, r * 0.9, GALV_DARK, 0);
                double mz = (z0 + z1) * 0.5;
                b.tube(x0, y0, mz, x1, y1, mz, r * 0.75, GALV, 0);
            }
            double cx = (x0 + x1) * 0.5, cy = (y0 + y1) * 0.5, cz = (z0 + z1) * 0.5;
            b.box(cx - 0.05, cy - 0.05, cz - 0.05, cx + 0.05, cy + 0.05, cz + 0.05, GALV_DARK, 0);
        }

        private static void deckFrame(Builder b, int ix, int iz, double y) {
            double bay = bay();
            double x0 = ix * bay, x1 = x0 + bay, z0 = iz * bay, z1 = z0 + bay;
            b.box(x0 + 0.04, y - 0.12, z0 + 0.06, x0 + 0.09, y - 0.064, z1 - 0.06, GALV_DARK, 0);
            b.box(x1 - 0.09, y - 0.12, z0 + 0.06, x1 - 0.04, y - 0.064, z1 - 0.06, GALV_DARK, 0);
            b.box(x0 + 0.12, y - 0.105, z0 + 0.06, x1 - 0.12, y - 0.07, z0 + 0.11, GALV, 0);
            b.box(x0 + 0.12, y - 0.105, (z0 + z1) * 0.5 - 0.025, x1 - 0.12, y - 0.07, (z0 + z1) * 0.5 + 0.025, GALV, 0);
            b.box(x0 + 0.12, y - 0.105, z1 - 0.11, x1 - 0.12, y - 0.07, z1 - 0.06, GALV, 0);
        }

        /** The same narrow boards as before, enough of them to fill a bay of {@link ControlTowerBlock#BAY} blocks. */
        private static void planks(Builder b, int baysW, int baysD, double y) {
            double bay = bay();
            int boards = Math.max(7, (int) Math.round(7.0 * bay / 2.0));
            double gap = 0.02;
            for (int ix = 0; ix < baysW; ix++) {
                for (int iz = 0; iz < baysD; iz++) {
                    double x0 = ix * bay, x1 = x0 + bay, z0 = iz * bay, z1 = z0 + bay;
                    boolean hatch = ix == 0 && iz == baysD - 1;
                    double hatchZ = z1 - 1.02;
                    double inner0 = x0 + 0.12, inner1 = x1 - 0.12;
                    double span = (inner1 - inner0 - gap * (boards - 1)) / boards;
                    for (int p = 0; p < boards; p++) {
                        double a = inner0 + p * (span + gap);
                        double c = a + span;
                        int col = p % 2 == 0 ? WOOD : shade(WOOD, 0.9f);
                        if (hatch && a < x0 + 0.98) board(b, a, y, z0 + 0.08, c, hatchZ, col);
                        else board(b, a, y, z0 + 0.08, c, z1 - 0.08, col);
                    }
                }
            }
        }

        /** One timber board. The top stops just under the walking surface so it does not flicker against a placed block. */
        private static void board(Builder b, double x0, double y, double z0, double x1, double z1, int col) {
            double top = y - 0.008;
            double bot = top - 0.05;
            double cap = 0.035;
            double mid = (x0 + x1) * 0.5;
            double g = 0.007;
            b.box(x0, bot, z0, x1, top, z0 + cap, WOOD_EDGE, 1);
            b.box(x0, bot, z1 - cap, x1, top, z1, WOOD_EDGE, 1);
            b.box(x0, bot, z0 + cap, mid - g, top, z1 - cap, col, 1);
            b.box(mid + g, bot, z0 + cap, x1, top, z1 - cap, col, 1);
            b.box(mid - g, bot, z0 + cap, mid + g, top - 0.014, z1 - cap, shade(col, 0.72f), 1);
            b.box(x0, bot - 0.016, z0, x1, bot - 0.004, z0 + 0.04, GALV_DIM, 0);
            b.box(x0, bot - 0.016, z1 - 0.04, x1, bot - 0.004, z1, GALV_DIM, 0);
        }

        private static void rails(Builder b, int baysW, int baysD, double y, double r, boolean skirt) {
            double bay = bay();
            double w = baysW * bay, d = baysD * bay;
            double mid = y + 0.52, high = y + 1.08;
            double z0 = skirt ? 0.08 : 0;
            railRun(b, 1.05, mid, d, w, mid, d, r, baysW, true);
            railRun(b, 1.05, high, d, w, high, d, r, baysW, true);
            railRun(b, 0, mid, z0, 0, mid, d, r, baysD, false);
            railRun(b, 0, high, z0, 0, high, d, r, baysD, false);
            railRun(b, w, mid, z0, w, mid, d, r, baysD, false);
            railRun(b, w, high, z0, w, high, d, r, baysD, false);
            if (!skirt) {
                railRun(b, 0, mid, 0, w, mid, 0, r, baysW, true);
                railRun(b, 0, high, 0, w, high, 0, r, baysW, true);
                toe(b, 0.06, y + 0.05, -0.03, w - 0.06, y + 0.2, 0.035);
                toe(b, 1.05, y + 0.05, d - 0.035, w - 0.06, y + 0.2, d + 0.03);
                toe(b, -0.03, y + 0.05, z0, 0.035, y + 0.2, d - 0.06);
                toe(b, w - 0.035, y + 0.05, z0, w + 0.03, y + 0.2, d - 0.06);
            }
            for (int ix = 0; ix < baysW; ix++) {
                double x = (ix + 0.5) * bay;
                if (!skirt) b.tube(x, y + 0.06, 0, x, high, 0, r * 0.7, GALV, 0);
                if (x > 1.2) b.tube(x, y + 0.06, d, x, high, d, r * 0.7, GALV, 0);
            }
            for (int iz = 0; iz < baysD; iz++) {
                double z = (iz + 0.5) * bay;
                b.tube(0, y + 0.06, z, 0, high, z, r * 0.7, GALV, 0);
                b.tube(w, y + 0.06, z, w, high, z, r * 0.7, GALV, 0);
            }
        }

        private static void railRun(Builder b, double x0, double y0, double z0, double x1, double y1, double z1, double r, int bays, boolean alongX) {
            b.tube(x0, y0, z0, x1, y1, z1, r, GALV, 0);
            for (int i = 1; i < bays; i++) {
                if (alongX) {
                    double x = x0 + (x1 - x0) * i / bays;
                    b.box(x - 0.035, y0 - 0.03, z0 - 0.035, x + 0.035, y0 + 0.03, z0 + 0.035, GALV_DARK, 0);
                } else {
                    double z = z0 + (z1 - z0) * i / bays;
                    b.box(x0 - 0.035, y0 - 0.03, z - 0.035, x0 + 0.035, y0 + 0.03, z + 0.035, GALV_DARK, 0);
                }
            }
        }

        private static void toe(Builder b, double x0, double y0, double z0, double x1, double y1, double z1) {
            b.box(x0, y0, z0, x1, y1, z1, WOOD_DIM, 1);
            b.box(x0, y1 - 0.015, z0, x1, y1, z1, WOOD_EDGE, 1);
        }

        private static void roof(Builder b, int baysW, int baysD, double w, double d, double crown, double ledger, int tint) {
            b.box(-0.45, crown, -0.45, w + 0.45, crown + 0.045, d + 0.45, tint, 2);
            b.box(-0.45, crown - 0.1, -0.45, w + 0.45, crown, -0.38, shade(tint, 0.78f), 2);
            b.box(-0.45, crown - 0.1, d + 0.38, w + 0.45, crown, d + 0.45, shade(tint, 0.78f), 2);
            b.box(-0.45, crown - 0.1, -0.45, -0.38, crown, d + 0.45, shade(tint, 0.7f), 2);
            b.box(w + 0.38, crown - 0.1, -0.45, w + 0.45, crown, d + 0.45, shade(tint, 0.7f), 2);
            b.box(-0.45, crown - 0.5, -0.45, w + 0.45, crown - 0.1, -0.38, shade(tint, 0.62f), 2);
            for (int ix = 0; ix <= baysW; ix++) {
                b.tube(ix * bay(), crown - 0.04, -0.2, ix * bay(), crown - 0.04, d + 0.2, ledger, GALV_DARK, 0);
            }
            for (int iz = 0; iz <= baysD; iz++) {
                b.tube(-0.2, crown - 0.02, iz * bay(), w + 0.2, crown - 0.02, iz * bay(), ledger * 0.8, GALV, 0);
            }
            b.tube(-0.42, crown + 0.02, -0.42, w + 0.42, crown + 0.02, -0.42, ledger * 0.7, GALV_DIM, 0);
            b.tube(-0.42, crown + 0.02, d + 0.42, w + 0.42, crown + 0.02, d + 0.42, ledger * 0.7, GALV_DIM, 0);
        }

        /**
         * Side and back tarps hung outside the frame. Each bay is its own sheet: hem, folds and eyelets,
         * with the scaffold hidden behind it.
         */
        private static void sheets(Builder b, int baysW, int baysD, double top, int tint) {
            int cloth = shade(tint, 1.0f);
            int hem = shade(tint, 0.62f);
            int fold = shade(tint, 0.84f);
            int eye = 0xFF2C3136;
            double bay = bay();
            double w = baysW * bay, d = baysD * bay;
            double y0 = 0.22, y1 = top - 0.08;
            for (int iz = 0; iz < baysD; iz++) {
                double z0 = iz * bay + 0.06, z1 = (iz + 1) * bay - 0.06;
                tarpZ(b, -0.09, z0, y0, z1, y1, -1, cloth, hem, fold, eye);
                tarpZ(b, w + 0.09, z0, y0, z1, y1, 1, cloth, hem, fold, eye);
            }
            for (int ix = 0; ix < baysW; ix++) {
                double x0 = ix * bay + 0.06, x1 = (ix + 1) * bay - 0.06;
                tarpX(b, x0, y0, d + 0.09, x1, y1, cloth, hem, fold, eye);
            }
        }

        private static void tarpZ(Builder b, double x, double z0, double y0, double z1, double y1, int out, int cloth, int hem, int fold, int eye) {
            double t = 0.018;
            double ox = out * 0.012;
            b.box(x - t + ox, y0, z0, x + t + ox, y1, z1, cloth, 2);
            b.box(x - t - 0.008 + ox, y0, z0, x + t + 0.008 + ox, y0 + 0.07, z1, hem, 2);
            b.box(x - t - 0.008 + ox, y1 - 0.07, z0, x + t + 0.008 + ox, y1, z1, hem, 2);
            b.box(x - t - 0.006 + ox, y0, z0, x + t + 0.006 + ox, y1, z0 + 0.045, hem, 2);
            b.box(x - t - 0.006 + ox, y0, z1 - 0.045, x + t + 0.006 + ox, y1, z1, hem, 2);
            for (int i = 1; i <= 3; i++) {
                double z = z0 + (z1 - z0) * i / 4.0;
                b.box(x - t + ox + out * 0.01, y0 + 0.1, z - 0.03, x + t + ox + out * 0.01, y1 - 0.1, z + 0.03, i % 2 == 0 ? fold : shade(cloth, 1.08f), 2);
            }
            for (double y = y0 + 0.18; y < y1 - 0.05; y += 0.45) {
                eyelet(b, x + out * 0.02, y, z0 + 0.02, eye);
                eyelet(b, x + out * 0.02, y, z1 - 0.02, eye);
            }
        }

        private static void tarpX(Builder b, double x0, double y0, double z, double x1, double y1, int cloth, int hem, int fold, int eye) {
            double t = 0.018;
            b.box(x0, y0, z - t, x1, y1, z + t, cloth, 2);
            b.box(x0, y0, z - t - 0.008, x1, y0 + 0.07, z + t + 0.008, hem, 2);
            b.box(x0, y1 - 0.07, z - t - 0.008, x1, y1, z + t + 0.008, hem, 2);
            b.box(x0, y0, z - t - 0.006, x0 + 0.045, y1, z + t + 0.006, hem, 2);
            b.box(x1 - 0.045, y0, z - t - 0.006, x1, y1, z + t + 0.006, hem, 2);
            for (int i = 1; i <= 3; i++) {
                double x = x0 + (x1 - x0) * i / 4.0;
                b.box(x - 0.03, y0 + 0.1, z + 0.01, x + 0.03, y1 - 0.1, z + t + 0.012, i % 2 == 0 ? fold : shade(cloth, 1.08f), 2);
            }
            for (double y = y0 + 0.18; y < y1 - 0.05; y += 0.45) {
                eyelet(b, x0 + 0.02, y, z + 0.02, eye);
                eyelet(b, x1 - 0.02, y, z + 0.02, eye);
            }
        }

        private static void eyelet(Builder b, double x, double y, double z, int col) {
            b.box(x - 0.025, y - 0.025, z - 0.025, x + 0.025, y + 0.025, z + 0.025, col, 0);
            b.box(x - 0.012, y - 0.012, z - 0.012, x + 0.012, y + 0.012, z + 0.012, 0xFF8A9096, 0);
        }

        private static void ladder(Builder b, double d, double top, double r) {
            double z = d - 0.16;
            b.tube(0.28, 0.2, z, 0.28, top + 0.1, z, r, GALV_DARK, 0);
            b.tube(0.72, 0.2, z, 0.72, top + 0.1, z, r, GALV_DARK, 0);
            for (double y = 0.4; y < top; y += 0.28) b.tube(0.28, y, z, 0.72, y, z, r * 0.75, GALV, 0);
        }

        private static int shade(int rgb, float f) {
            int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
            int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
            int bl = Math.min(255, (int) ((rgb & 255) * f));
            return 0xFF000000 | (r << 16) | (g << 8) | bl;
        }
    }

    /** Grows a quad list. UVs are 0..1 inside the sprite. */
    private static final class Builder {
        private float[] xyz = new float[12 * 256];
        private float[] uv = new float[8 * 256];
        private byte[] mat = new byte[256];
        private int[] color = new int[256];
        private int quads;

        private void grow() {
            if (quads < color.length) return;
            int n = color.length * 2;
            xyz = Arrays.copyOf(xyz, 12 * n);
            uv = Arrays.copyOf(uv, 8 * n);
            mat = Arrays.copyOf(mat, n);
            color = Arrays.copyOf(color, n);
        }

        void box(double x0, double y0, double z0, double x1, double y1, double z1, int col, int material) {
            quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, col, material);
            quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, col, material);
            quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, shade(col, 1.08f), material);
            quad(x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, shade(col, 0.62f), material);
            quad(x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, shade(col, 0.84f), material);
            quad(x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, shade(col, 0.84f), material);
        }

        void tube(double x0, double y0, double z0, double x1, double y1, double z1, double r, int col, int material) {
            double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-4) return;
            dx /= len;
            dy /= len;
            dz /= len;
            double ax = Math.abs(dy) < 0.85 ? 0 : 1, ay = Math.abs(dy) < 0.85 ? 1 : 0, az = 0;
            double sx = dy * az - dz * ay, sy = dz * ax - dx * az, sz = dx * ay - dy * ax;
            double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl;
            sy /= sl;
            sz /= sl;
            double ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
            int n = 6;
            double[] px = new double[n], py = new double[n], pz = new double[n];
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                double c = Math.cos(a) * r, s = Math.sin(a) * r;
                px[i] = sx * c + ux * s;
                py[i] = sy * c + uy * s;
                pz[i] = sz * c + uz * s;
            }
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double ny = (py[i] + py[j]) * 0.5;
                int face = shade(col, 0.78f + 0.28f * (float) Math.max(-1, Math.min(1, ny / r)));
                quad(x0 + px[i], y0 + py[i], z0 + pz[i], x0 + px[j], y0 + py[j], z0 + pz[j],
                        x1 + px[j], y1 + py[j], z1 + pz[j], x1 + px[i], y1 + py[i], z1 + pz[i], face, material);
            }
        }

        /** Horizontal rosette, the ring a ledger locks into. */
        void disc(double x, double y, double z, double r, double h, int col) {
            int n = 8;
            for (int i = 0; i < n; i++) {
                double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
                double x0 = x + Math.cos(a0) * r, z0 = z + Math.sin(a0) * r;
                double x1 = x + Math.cos(a1) * r, z1 = z + Math.sin(a1) * r;
                quad(x0, y + h, z0, x1, y + h, z1, x1, y, z1, x0, y, z0, col, 0);
                double ix0 = x + Math.cos(a0) * r * 0.45, iz0 = z + Math.sin(a0) * r * 0.45;
                double ix1 = x + Math.cos(a1) * r * 0.45, iz1 = z + Math.sin(a1) * r * 0.45;
                quad(ix0, y + h, iz0, x0, y + h, z0, x1, y + h, z1, ix1, y + h, iz1, shade(col, 1.12f), 0);
            }
        }

        void quad(double x0, double y0, double z0, double x1, double y1, double z1,
                  double x2, double y2, double z2, double x3, double y3, double z3, int col, int material) {
            grow();
            int q = quads++;
            int p = q * 12;
            xyz[p] = (float) x0; xyz[p + 1] = (float) y0; xyz[p + 2] = (float) z0;
            xyz[p + 3] = (float) x1; xyz[p + 4] = (float) y1; xyz[p + 5] = (float) z1;
            xyz[p + 6] = (float) x2; xyz[p + 7] = (float) y2; xyz[p + 8] = (float) z2;
            xyz[p + 9] = (float) x3; xyz[p + 10] = (float) y3; xyz[p + 11] = (float) z3;
            int t = q * 8;
            uv[t] = 0; uv[t + 1] = 1;
            uv[t + 2] = 1; uv[t + 3] = 1;
            uv[t + 4] = 1; uv[t + 5] = 0;
            uv[t + 6] = 0; uv[t + 7] = 0;
            mat[q] = (byte) material;
            color[q] = col;
        }

        Mesh finish(long key) {
            return new Mesh(key, Arrays.copyOf(xyz, quads * 12), Arrays.copyOf(uv, quads * 8),
                    Arrays.copyOf(mat, quads), Arrays.copyOf(color, quads), quads);
        }

        private static int shade(int rgb, float f) {
            int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * f)));
            int g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * f)));
            int b = Math.min(255, Math.max(0, (int) ((rgb & 255) * f)));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
}
