package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {

    // 65536-entry LUT covers full circle
    private static final float[] SINE_TABLE = new float[65536];
    static {
        for (int i = 0; i < 65536; ++i)
            SINE_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
    }

    private MathHelperMixin() {}

    /** @author Alexxiconify @reason LUT sin or vanilla fallback */
    @Overwrite
    public static float sin(double value) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSine())
            return SINE_TABLE[(int)(value * 10430.378) & 65535];
        return (float) Math.sin(value);
    }

    /** @author Alexxiconify @reason LUT cos or vanilla fallback */
    @Overwrite
    public static float cos(double value) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeCos())
            return SINE_TABLE[(int)(value * 10430.378 + 16384.0) & 65535];
        return (float) Math.cos(value);
    }

    /** @author Alexxiconify @reason Quake III fast inv-sqrt or vanilla fallback */
    @Overwrite
    public static double fastInverseSqrt(double x) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeInvSqrt()) {
            double half = 0.5 * x;
            long bits = Double.doubleToLongBits(x);
            bits = 0x5FE6EB50C7B537A9L - (bits >> 1);
            x = Double.longBitsToDouble(bits);
            return x * (1.5 - half * x * x);
        }
        return 1.0 / Math.sqrt(x);
    }

    /** @author Alexxiconify @reason Scalar sqrt */
    @Overwrite
    public static float sqrt(float f) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeInvSqrt()) {
            return com.alexxiconify.rustmc.NativeBridge.invokeSqrt(f);
        }
        return (float) Math.sqrt(f);
    }

    /** @author Alexxiconify @reason Fast atan2 */
    @Overwrite
    public static double atan2(double y, double x) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeInvSqrt()) {
            return com.alexxiconify.rustmc.NativeBridge.invokeAtan2(y, x);
        }
        return Math.atan2(y, x);
    }

    /** @author Alexxiconify @reason Bitwise floor */
    @Overwrite
    public static int floor(double d) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeFloor()) {
            int i = (int) d;
            return d < i ? i - 1 : i;
        }
        return (int) Math.floor(d);
    }

    /** @author Alexxiconify @reason Native clamp */
    @Overwrite
    public static float clamp(float value, float min, float max) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSqrt()) {
            return com.alexxiconify.rustmc.NativeBridge.invokeClamp(value, min, max);
        }
        return Math.clamp(value, min, max);
    }

    /** @author Alexxiconify @reason Native lerp */
    @Overwrite
    public static double lerp(double delta, double start, double end) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSqrt()) {
            return com.alexxiconify.rustmc.NativeBridge.invokeLerp(delta, start, end);
        }
        return start + delta * (end - start);
    }

    /** @author Alexxiconify @reason Native absMax */
    @Overwrite
    public static double absMax(double a, double b) {
        if (!ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSqrt()) {
            return com.alexxiconify.rustmc.NativeBridge.invokeAbsMax(a, b);
        }
        return Math.max(Math.abs(a), Math.abs(b));
    }
}
