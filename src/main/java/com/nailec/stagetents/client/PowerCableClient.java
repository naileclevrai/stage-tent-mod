package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nailec.stagetents.StageTents;
import com.nailec.stagetents.furniture.CableToolItem;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.PowerCable;
import com.nailec.stagetents.furniture.PowerLinks;
import com.nailec.stagetents.furniture.PowerSockets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Placed leads, and the blue socket marks while the cabling tool is in hand. */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PowerCableClient {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "block/white_wool");
    private static final int LEAD = 0xFF1A1C20;
    private static final int PREVIEW = 0xFF31445C;
    private static final int DOT = 0xFF2F7BFF;
    private static final int DOT_CORE = 0xFFD7ECFF;
    private static final int ARMED = 0xFF9FD4FF;
    private static final int AIMED = 0xFF6EB6FF;
    private static final int FULL = 0x00F000F0;
    private static final List<BlockPos> NEARBY = new ArrayList<>();

    private PowerCableClient() {}

    /** Dark lead from each feeding socket. Called from the block-entity renderers, so it shows without the tool. */
    public static void drawCables(BlockEntity be, PoseStack ps, MultiBufferSource buffers) {
        Level level = be.getLevel();
        PowerLinks cables = PowerSockets.cables(be);
        if (level == null || cables == null || cables.links().isEmpty()) return;
        BlockState state = be.getBlockState();
        List<PowerSockets.Socket> sockets = PowerSockets.of(state);
        if (sockets == null) return;
        Direction facing = state.getValue(FurnitureBlock.FACING);
        BlockPos origin = be.getBlockPos();
        TextureAtlasSprite wool = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(WOOL);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = ps.last();
        for (PowerLinks.Link link : cables.links()) {
            if (link.socket() < 0 || link.socket() >= sockets.size()) continue;
            if (!sockets.get(link.socket()).role().feeds()) continue;
            if (!level.isLoaded(link.other())) continue;
            BlockState otherState = level.getBlockState(link.other());
            List<PowerSockets.Socket> otherSockets = PowerSockets.of(otherState);
            if (otherSockets == null || link.otherSocket() < 0 || link.otherSocket() >= otherSockets.size()) continue;
            Vec3 from = PowerSockets.anchor(origin, facing, sockets.get(link.socket()));
            Vec3 to = PowerSockets.anchor(link.other(), otherState.getValue(FurnitureBlock.FACING), otherSockets.get(link.otherSocket()));
            Vec3 local = new Vec3(origin.getX(), origin.getY(), origin.getZ());
            DraftHoseRenderer.tube(vc, pose, wool, List.of(from.subtract(local), to.subtract(local)), LEAD, 0.011F, FULL);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;
        ItemStack tool = tool(player);
        if (tool == null) return;

        Vec3 cam = event.getCamera().getPosition();
        org.joml.Vector3f lookV = event.getCamera().getLookVector();
        Vec3 look = new Vec3(lookV.x(), lookV.y(), lookV.z());
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 camUp = right.cross(look).normalize();

        BlockPos armedPos = null;
        int armedIndex = -1;
        CompoundTag tag = tool.getTag();
        if (tag != null && tag.contains(PowerCable.POS_KEY)) {
            armedPos = BlockPos.of(tag.getLong(PowerCable.POS_KEY));
            armedIndex = tag.getInt(PowerCable.SOCKET_KEY);
        }
        double reach = player.isCreative() ? 5.0 : 4.5;
        Vec3 blockPoint = null;
        BlockPos blockAt = null;
        if (mc.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            blockPoint = block.getLocation();
            blockAt = block.getBlockPos();
        }
        PowerSockets.Hit aimed = PowerSockets.pick(level, player, reach, blockPoint, blockAt);
        boolean aimedOk = aimed != null && armedPos != null && previewOk(level, armedPos, armedIndex, aimed);

        TextureAtlasSprite wool = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(WOOL);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(type);
        PoseStack.Pose pose = event.getPoseStack().last();

        PowerSockets.collect(level, player.blockPosition(), 8, NEARBY);
        if (armedPos != null && level.isLoaded(armedPos) && player.blockPosition().distSqr(armedPos) <= 24 * 24
                && !NEARBY.contains(armedPos)) {
            NEARBY.add(armedPos);
        }
        for (BlockPos pos : NEARBY) {
            BlockState state = level.getBlockState(pos);
            List<PowerSockets.Socket> sockets = PowerSockets.of(state);
            if (sockets == null) continue;
            Direction facing = state.getValue(FurnitureBlock.FACING);
            for (int i = 0; i < sockets.size(); i++) {
                boolean armed = pos.equals(armedPos) && i == armedIndex;
                boolean hot = aimedOk && aimed.pos().equals(pos) && aimed.index() == i;
                Vec3 mark = PowerSockets.marker(pos, facing, sockets.get(i)).subtract(cam);
                float size = armed ? 0.030F : hot ? 0.024F : 0.016F;
                int color = armed ? ARMED : hot ? AIMED : DOT;
                billboard(vc, pose, wool, mark, right, camUp, size, color);
                billboard(vc, pose, wool, mark, right, camUp, size * 0.45F, DOT_CORE);
            }
        }
        if (aimedOk) {
            BlockState fromState = level.getBlockState(armedPos);
            List<PowerSockets.Socket> fromSockets = PowerSockets.of(fromState);
            if (fromSockets != null && armedIndex >= 0 && armedIndex < fromSockets.size()) {
                Vec3 from = PowerSockets.anchor(armedPos, fromState.getValue(FurnitureBlock.FACING), fromSockets.get(armedIndex));
                DraftHoseRenderer.tube(vc, pose, wool, List.of(from.subtract(cam), aimed.anchor().subtract(cam)), PREVIEW, 0.010F, FULL);
            }
        }
        buffers.endBatch(type);
    }

    private static boolean previewOk(Level level, BlockPos armedPos, int armedIndex, PowerSockets.Hit aimed) {
        if (armedPos.equals(aimed.pos())) return false;
        BlockState state = level.getBlockState(armedPos);
        List<PowerSockets.Socket> sockets = PowerSockets.of(state);
        if (sockets == null || armedIndex < 0 || armedIndex >= sockets.size()) return false;
        PowerSockets.Socket from = sockets.get(armedIndex);
        if (from.role().feeds() == aimed.socket().role().feeds()) return false;
        Vec3 anchor = PowerSockets.anchor(armedPos, state.getValue(FurnitureBlock.FACING), from);
        if (anchor.distanceTo(aimed.anchor()) > PowerSockets.REACH) return false;
        PowerLinks a = PowerSockets.cables(level, armedPos);
        PowerLinks b = PowerSockets.cables(level, aimed.pos());
        if (a != null && a.find(armedIndex) != null) return false;
        return b == null || b.find(aimed.index()) == null;
    }

    @javax.annotation.Nullable
    private static ItemStack tool(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CableToolItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CableToolItem) return off;
        return null;
    }

    private static void billboard(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                  Vec3 center, Vec3 right, Vec3 up, float size, int color) {
        Vec3 a = center.add(right.scale(-size)).add(up.scale(-size));
        Vec3 b = center.add(right.scale(size)).add(up.scale(-size));
        Vec3 c = center.add(right.scale(size)).add(up.scale(size));
        Vec3 d = center.add(right.scale(-size)).add(up.scale(size));
        quad(vc, pose, sprite, a, b, c, d, color);
        quad(vc, pose, sprite, d, c, b, a, color);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (color >> 16) & 255, g = (color >> 8) & 255, bl = color & 255;
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL).normal(n, 0, 1, 0).endVertex();
    }
}
