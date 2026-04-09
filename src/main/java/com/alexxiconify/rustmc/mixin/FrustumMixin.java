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

/**
 * Synchronizes the active renderer frustum with the native Rust core.
 */
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
            NativeBridge.updateVanillaFrustum(rustmc$matrixBuf);

            // Cave detection for DH culling
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                var world = client.world;
                var player = client.player;
                if (world != null && player != null) {
                    BlockPos pos = player.getBlockPos();
                    boolean inCave = world.getLightLevel(LightType.SKY, pos) == 0 && pos.getY() < 50;
                    NativeBridge.updateCaveStatus(inCave);
                }
            }
        }
    }
}