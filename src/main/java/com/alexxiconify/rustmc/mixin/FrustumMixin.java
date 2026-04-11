package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Synchronizes the active renderer frustum with the native Rust core.
@SuppressWarnings({"java:S116", "java:S100"})
@Mixin(net.minecraft.client.render.Frustum.class)
public class FrustumMixin {
    @Unique
    private final float[] rustmc$matrixBuf = new float[16];

    @Unique
    private final Matrix4f rustmc$combined = new Matrix4f();

    @Inject(method = "init", at = @At("RETURN"))
    private void rustmc$onInit(Matrix4f viewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeCulling()) {
            rustmc$combined.set(projectionMatrix).mul(viewMatrix);
            rustmc$combined.get(rustmc$matrixBuf);
            // Cave detection for DH culling
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                var world = client.world;
                var cameraEntity = client.getCameraEntity();
                if (world != null && cameraEntity != null) {
                    NativeBridge.updateVanillaFrustum(rustmc$matrixBuf, cameraEntity.getX(), cameraEntity.getY(), cameraEntity.getZ());
                    
                    BlockPos bpos = cameraEntity.getBlockPos();
                    boolean inCave = world.getLightLevel(LightType.SKY, bpos) == 0 && bpos.getY() < 50;
                    NativeBridge.updateCaveStatus(inCave);
                }
            }
        }
    }
}