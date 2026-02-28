package com.alexxiconify.rustmc.mixin.minihud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.render.RenderUtils", remap = false)
public class RenderUtilsMixin {
    private RenderUtilsMixin() {}

    // In a production mod we'd probably use Redirects, but for direct hook injecting to
    // cancel massive wireframes, we intercept HEAD and cleanly cancel execution if too far away.
    
    @SuppressWarnings("all") // Suppress Mixin 13 parameter warning count which matches the target
    // Target drawOutlinedBox to cull MiniHUD/Litematica shapes that are too far away
    @Inject(method = "drawOutlinedBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDDDDFFFF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void cullOutlinedBox(MatrixStack matrices, VertexConsumer consumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            double centerX = (minX + maxX) * 0.5;
            double centerY = (minY + maxY) * 0.5;
            double centerZ = (minZ + maxZ) * 0.5;
            double distSq = camera.squaredDistanceTo(centerX, centerY, centerZ);
            double viewDist = mc.options.getClampedViewDistance() * 16.0;
            // Pad by 32 blocks so edges of render distance aren't harsh
            double cullDist = viewDist + 32.0;
            if (distSq > cullDist * cullDist) {
                ci.cancel();
            }
        }
    }

    @SuppressWarnings("all")
    @Inject(method = "drawBoxAllSides", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void drawBoxAllSides(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera == null) return;
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;
        double distSq = camera.squaredDistanceTo(centerX, centerY, centerZ);
        int rd = mc.options.getClampedViewDistance() * 16;
        if (distSq > (rd * rd)) ci.cancel();
    }

    @SuppressWarnings("all")
    @Inject(method = "drawLine", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void drawLine(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a,
            org.joml.Matrix4f matrix, org.joml.Matrix4f normalMatrix,
            VertexConsumer buffer, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            double centerX = (x1 + x2) * 0.5;
            double centerY = (y1 + y2) * 0.5;
            double centerZ = (z1 + z2) * 0.5;
            double distSq = camera.squaredDistanceTo(centerX, centerY, centerZ);
            double viewDist = mc.options.getClampedViewDistance() * 16.0;
            double cullDist = viewDist + 32.0;
            if (distSq > cullDist * cullDist) {
                ci.cancel();
            }
        }
    }
}
