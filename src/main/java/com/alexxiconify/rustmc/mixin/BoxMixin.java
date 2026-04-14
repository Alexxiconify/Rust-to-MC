package com.alexxiconify.rustmc.mixin;
import com.alexxiconify.rustmc.NativeBridge;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
@Mixin(Box.class)
public abstract class BoxMixin {
    @Final
    @Shadow public double minX;
    @Final
    @Shadow public double minY;
    @Final
    @Shadow public double minZ;
    @Final
    @Shadow public double maxX;
    @Final
    @Shadow public double maxY;
    @Final
    @Shadow public double maxZ;
    @Inject(method = "raycast(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void rustRaycast(Vec3d min, Vec3d max, CallbackInfoReturnable<Optional<Vec3d>> cir) {
        if (NativeBridge.isReady() && !com.alexxiconify.rustmc.ModBridge.isInteractionOwned()) {
            // "min" is actually the ray origin, "max" is the ray destination (vanilla named it poorly)
            double dirX = max.x - min.x;
            double dirY = max.y - min.y;
            double dirZ = max.z - min.z;
            // Optional: If Rust says no intersection, we return Optional.empty immediately and skip vanilla math.
            if (!NativeBridge.invokeRayIntersectsBox(min.x, min.y, min.z, dirX, dirY, dirZ, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)) {
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}