package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
 //  Updates native cave-status hints from the active camera position.
@Mixin(net.minecraft.client.render.Frustum.class)
public class FrustumMixin {
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(Matrix4f viewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        var world = client.world;
        var cameraEntity = client.getCameraEntity();
        if (world != null && cameraEntity != null) {
            BlockPos pos = cameraEntity.getBlockPos();
            boolean inCave = world.getLightLevel(LightType.SKY, pos) == 0 && pos.getY() < 50;
            NativeBridge.updateCaveStatus(inCave);
        } else {
            NativeBridge.updateCaveStatus(false);
        }
    }
}