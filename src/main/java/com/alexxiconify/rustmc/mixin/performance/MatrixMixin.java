package com.alexxiconify.rustmc.mixin.performance;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.ModBridge;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Offloads JOML matrix multiplications to Rust SIMD where native path is active.
@Mixin(value = Matrix4f.class, remap = false)
public abstract class MatrixMixin {
    @Unique
    private final float[] rustmc$leftBuf = new float[16];
    @Unique
    private final float[] rustmc$rightBuf = new float[16];
    @Unique
    private final float[] rustmc$resBuf = new float[16];
    @Inject(method = "mul(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void rustmc$onMul(org.joml.Matrix4fc right, Matrix4f dest, CallbackInfoReturnable<Matrix4f> cir) {
        // Check conditions in order of cost: isReady() is cached, isMathOwned() requires lookup
        if (!NativeBridge.isReady() || ModBridge.isMathOwned()) return;
        Matrix4f self = (Matrix4f) (Object) this;
        self.get(rustmc$leftBuf);
        right.get(rustmc$rightBuf);
        NativeBridge.invokeMatrixMul(rustmc$leftBuf, rustmc$rightBuf, rustmc$resBuf);
        dest.set(rustmc$resBuf);
        cir.setReturnValue(dest);
    }
}