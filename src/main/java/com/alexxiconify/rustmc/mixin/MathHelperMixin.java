package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.RustMC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {

    @Unique
    private static final float[] SINE_TABLE = new float[65536];
    static {
        for (int i = 0; i < 65536; ++i)
            SINE_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
    }

    private MathHelperMixin() {}

    /** @author Alexxiconify @reason LUT sin or vanilla fallback */
    @Overwrite
    public static float sin(double value) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSine())
            return SINE_TABLE[(int)(value * 10430.378) & 65535];
        return (float) Math.sin(value);
    }

    /** @author Alexxiconify @reason LUT cos or vanilla fallback */
    @Overwrite
    public static float cos(double value) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeCos())
            return SINE_TABLE[(int)(value * 10430.378 + 16384.0) & 65535];
        return (float) Math.cos(value);
    }

    /**
     * @author Alexxiconify
     * @reason Quake III fast inv-sqrt or vanilla fallback.
     *         MC deprecated this, but it still exists in 1.21.11.
     * @deprecated Vanilla deprecated since 1.21; kept for backward compat.
     */
    @Deprecated(since = "1.21")
    @Overwrite
    public static double fastInverseSqrt(double x) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeInvSqrt()) {
            double half = 0.5 * x;
            long bits = Double.doubleToLongBits(x);
            bits = 0x5FE6EB50C7B537A9L - (bits >> 1);
            x = Double.longBitsToDouble(bits);
            return x * (1.5 - half * x * x);
        }
        return 1.0 / Math.sqrt(x);
    }

    /** @author Alexxiconify @reason Native sqrt via Rust JNI when enabled */
    @Overwrite
    public static float sqrt(float f) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeSqrt())
            return com.alexxiconify.rustmc.NativeBridge.invokeSqrt(f);
        return (float) Math.sqrt(f);
    }

    /** @author Alexxiconify @reason Fast atan2 polynomial approximation */
    @Overwrite
    public static double atan2(double y, double x) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeAtan2())
            return fastAtan2(y, x);
        return Math.atan2(y, x);
    }

    @Unique
    private static double fastAtan2(double y, double x) {
        double absY = Math.abs(y) + 1e-10;
        double r;
        double angle;
        if (x >= 0) {
            r = (x - absY) / (x + absY);
            angle = 0.7853981633974483; // PI/4
        } else {
            r = (x + absY) / (absY - x);
            angle = 2.356194490192345; // 3*PI/4
        }
        angle -= (0.1963 * r * r - 0.9817) * r;
        return y < 0 ? -angle : angle;
    }

    /** @author Alexxiconify @reason Bitwise floor */
    @Overwrite
    public static int floor(double d) {
        if (ModBridge.isMathOwned() && RustMC.CONFIG.isUseNativeFloor()) {
            int i = (int) d;
            return d < i ? i - 1 : i;
        }
        return (int) Math.floor(d);
    }

    /** @author Alexxiconify @reason Delegate to Math.clamp for all paths */
    @Overwrite
    public static float clamp(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }

    /** @author Alexxiconify @reason FMA lerp for better precision */
    @Overwrite
    public static double lerp(double delta, double start, double end) {
        if (ModBridge.isMathOwned())
            return Math.fma(delta, end - start, start);
        return start + delta * (end - start);
    }

    /** @author Alexxiconify @reason Inline absMax */
    @Overwrite
    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    /** @author Alexxiconify @reason Native wrapDegrees */
    @Overwrite
    public static float wrapDegrees(float value) {
        return com.alexxiconify.rustmc.NativeBridge.invokeWrapDegrees(value);
    }
}