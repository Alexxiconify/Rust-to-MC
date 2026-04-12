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
        }
    }

    @Inject(method = "setPosition", at = @At("RETURN"))
    private void rustmc$onSetPosition(double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        if (NativeBridge.isReady() && RustMC.CONFIG.isUseNativeCulling()) {
            MinecraftClient client = MinecraftClient.getInstance();
            double fovScale = 1.0;
            if (client != null) {
                double fov = client.options.getFov().getValue();
                double aspect = client.getWindow().getFramebufferWidth() / Math.max(1.0, client.getWindow().getFramebufferHeight());
                double aspectBoost = Math.max(1.0, aspect / (16.0 / 9.0));
                fovScale = Math.clamp(1.15 * (fov / 70.0) * Math.sqrt(aspectBoost), 0.8, 2.5);

                var world = client.world;
                if (world != null) {
                    BlockPos bpos = BlockPos.ofFloored(cameraX, cameraY, cameraZ);
                    boolean inCave = world.getLightLevel(LightType.SKY, bpos) == 0 && bpos.getY() < 50;
                    NativeBridge.updateCaveStatus(inCave);
                }
            }
            NativeBridge.updateVanillaFrustum(rustmc$matrixBuf, fovScale, cameraX, cameraY, cameraZ);
        }
    }
}