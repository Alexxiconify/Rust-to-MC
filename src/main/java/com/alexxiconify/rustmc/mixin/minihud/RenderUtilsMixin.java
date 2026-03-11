package com.alexxiconify.rustmc.mixin.minihud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.render.RenderUtils", remap = false)
public class RenderUtilsMixin {
    private RenderUtilsMixin() {}

    /** Returns viewDist² (with +32 block pad), or -1 if no camera entity. */
    @Unique
    private static double cullRadiusSq() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null) return -1;
        double d = mc.options.getClampedViewDistance() * 16.0 + 32.0;
        return d * d;
    }

    /** Checks if the center point is beyond cull distance. Returns true if it should be culled. */
    @Unique
    private static boolean shouldCullCenter(double rSq, double cx, double cy, double cz) {
        Entity cam = MinecraftClient.getInstance().getCameraEntity();
        return cam != null && cam.squaredDistanceTo(cx, cy, cz) > rSq;
    }

    @SuppressWarnings("all")
    @Inject(method = "drawOutlinedBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDDDDFFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void cullOutlinedBox(MatrixStack matrices, VertexConsumer consumer,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            float r, float g, float b, float a, CallbackInfo ci) {
        double rSq = cullRadiusSq();
        if (rSq < 0) return;
        if (shouldCullCenter(rSq, (minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5)) {
            ci.cancel();
        }
    }

    @SuppressWarnings("all")
    @Inject(method = "drawBoxAllSides", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void cullDrawBoxAllSides(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            CallbackInfo ci) {
        double rSq = cullRadiusSq();
        if (rSq < 0) return;
        if (shouldCullCenter(rSq, (minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5)) {
            ci.cancel();
        }
    }

    @SuppressWarnings("all")
    @Inject(method = "drawLine", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void cullDrawLine(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a,
            org.joml.Matrix4f matrix, org.joml.Matrix4f normalMatrix,
            VertexConsumer buffer, CallbackInfo ci) {
        double rSq = cullRadiusSq();
        if (rSq < 0) return;
        if (shouldCullCenter(rSq, (x1 + x2) * 0.5, (y1 + y2) * 0.5, (z1 + z2) * 0.5)) {
            ci.cancel();
        }
    }
}