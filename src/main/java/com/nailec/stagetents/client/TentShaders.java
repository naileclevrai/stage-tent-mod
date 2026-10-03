package com.nailec.stagetents.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.nailec.stagetents.StageTents;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * The tent shader: entity lighting and fog, plus the wind ripple, so a tent can stay in GPU memory and cost almost
 * nothing per frame. Shader packs replace the whole pipeline, so with one active tents go back to the CPU path.
 */
@Mod.EventBusSubscriber(modid = StageTents.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TentShaders {
    private static ShaderInstance tent;
    private static Method irisInstance, irisInUse;
    private static boolean irisLooked;

    private TentShaders() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(StageTents.MOD_ID, "tent"),
                DefaultVertexFormat.NEW_ENTITY), s -> tent = s);
    }

    /** The shader, or null when the GPU path can't be used. */
    static ShaderInstance tent() {
        return tent == null || shaderPackInUse() ? null : tent;
    }

    /** Iris and Oculus share this API. */
    private static boolean shaderPackInUse() {
        if (!irisLooked) {
            irisLooked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisInstance = api.getMethod("getInstance");
                irisInUse = api.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisInstance = null;
            }
        }
        if (irisInstance == null) return false;
        try {
            return (Boolean) irisInUse.invoke(irisInstance.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
