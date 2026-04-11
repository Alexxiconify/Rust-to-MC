package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Box.class)
public abstract class BoxMixin {

    @Shadow public double minX;
    @Shadow public double minY;
    @Shadow public double minZ;
    @Shadow public double maxX;
    @Shadow public double maxY;
    @Shadow public double maxZ;

    @Inject(method = "raycast(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void rustRaycast(Vec3d min, Vec3d max, CallbackInfoReturnable<Optional<Vec3d>> cir) {
        if (RustMC.CONFIG.isUseNativeCulling() && NativeBridge.isReady() && !com.alexxiconify.rustmc.ModBridge.isInteractionOwned()) {
            // "min" is actually the ray origin, "max" is the ray destination (vanilla named it poorly)
            double dirX = max.x - min.x;
            double dirY = max.y - min.y;
            double dirZ = max.z - min.z;

            // Optional: If Rust says no intersection, we return Optional.empty immediately and skip vanilla math. If Rust says yes, we fall back to vanilla to calculate the exact impact point.
            if (!NativeBridge.invokeRayIntersectsBox(min.x, min.y, min.z, dirX, dirY, dirZ, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)) {
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}