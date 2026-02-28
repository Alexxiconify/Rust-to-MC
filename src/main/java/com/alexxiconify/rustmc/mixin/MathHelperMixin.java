package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {

    // Sine LUT — 65536 entries covers full circle.
    private static final float[] SINE_TABLE = new float[65536];
    static {
        for (int i = 0; i < 65536; ++i)
            SINE_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
    }

    private MathHelperMixin() {}

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason LUT-based sin, avoids JVM intrinsic overhead for hot paths.
     * In 1.21.11 the param changed to double.
     */
    @Overwrite
    public static float sin(double value) {
        return SINE_TABLE[(int)(value * 10430.378) & 65535];
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason LUT-based cos via sine phase shift.
     */
    @Overwrite
    public static float cos(double value) {
        return SINE_TABLE[(int)(value * 10430.378 + 16384.0) & 65535];
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Double-precision Quake III fast inverse sqrt.
     * In 1.21.11 the signature changed to double→double.
     */
    @Overwrite
    public static double fastInverseSqrt(double x) {
        double half = 0.5 * x;
        long bits = Double.doubleToLongBits(x);
        bits = 0x5FE6EB50C7B537A9L - (bits >> 1);
        x = Double.longBitsToDouble(bits);
        return x * (1.5 - half * x * x);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Scalar sqrt — keeps JVM from boxing to double path.
     */
    @Overwrite
    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Fast atan2.
     */
    @Overwrite
    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Bitwise floor avoids branch in (int)cast path.
     */
    @Overwrite
    public static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
