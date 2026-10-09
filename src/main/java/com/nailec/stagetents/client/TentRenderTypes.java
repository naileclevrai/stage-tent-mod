package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Render types the vanilla set doesn't offer. Extends RenderType only to reach its protected state shards. */
final class TentRenderTypes extends RenderType {
    private static RenderType glass;
    private static final Map<ResourceLocation, RenderType> CUTOUT = new HashMap<>();

    private TentRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling,
                            boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /**
     * Window panes: translucent, sorted, back faces culled and no depth writes, so a near pane never hides the panes
     * and walls behind it.
     */
    static RenderType glass(ResourceLocation texture) {
        if (glass == null) {
            glass = create("stagetents_glass", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true));
        }
        return glass;
    }

    /**
     * Opaque cutout with back faces culled. The canvas is stored as two skins wound opposite ways, so each side
     * shows only the skin that faces the camera. The vanilla entity cutout draws both and the lining then paints
     * over the outside cloth.
     */
    static RenderType cutout(ResourceLocation texture) {
        return CUTOUT.computeIfAbsent(texture, tex -> create(
                "stagetents_cutout_" + tex.getNamespace() + "_" + tex.getPath().replace('/', '_'),
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                        .setTextureState(new TextureStateShard(tex, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true)));
    }
}
