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
    @SuppressWarnings("java:S5164") // ThreadLocal is pooled for the lifetime of rendering threads
    private static final ThreadLocal<float[][]> RUSTMC_BUFFER_POOL = ThreadLocal.withInitial(() -> new float[][] {
        new float[16], new float[16], new float[16]
    });
    @Inject(method = "mul(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void rustmcOnMul(org.joml.Matrix4fc right, Matrix4f dest, CallbackInfoReturnable<Matrix4f> cir) {
        if (!NativeBridge.isReady() || ModBridge.isMathOwned()) return;
        
        float[][] buffers = RUSTMC_BUFFER_POOL.get();
        float[] leftArr = buffers[0];
        float[] rightArr = buffers[1];
        float[] resArr = buffers[2];

        Matrix4f self = (Matrix4f) (Object) this;
        self.get(leftArr);
        right.get(rightArr);
        
        NativeBridge.invokeMatrixMul(leftArr, rightArr, resArr);
        dest.set(resArr);
        cir.setReturnValue(dest);
    }
}