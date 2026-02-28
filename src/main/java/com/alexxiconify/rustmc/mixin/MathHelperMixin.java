package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {

    // Fast Math lookup table (Lithium style)
    private static final float[] SINE_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; ++i) {
            SINE_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0 / 65536.0);
        }
    }

    private MathHelperMixin() {}

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Uses fast math lookup tables if 'Native Sine' is enabled, avoiding JNI overhead.
     */
    @Overwrite
    public static float sin(float f) {
        return SINE_TABLE[(int)(f * 10430.378f) & 65535];
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Uses fast math lookup tables if 'Native Cosine' is enabled.
     */
    @Overwrite
    public static float cos(float f) {
        return SINE_TABLE[(int)(f * 10430.378f + 16384.0f) & 65535];
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Quake III fast inverse square root wrapped in fast Java to avoid JNI lag.
     */
    @Overwrite
    public static float fastInverseSqrt(float x) {
        float halfX = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1); // Quake 3 magic number
        x = Float.intBitsToFloat(i);
        return x * (1.5f - halfX * x * x);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Replaces JIT sqrt.
     */
    @Overwrite
    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Replaces tan with native call block (JVM handles it faster without JNI).
     */
    @Overwrite
    public static float tan(float f) {
        return (float) Math.tan(f);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Fast atan2 block.
     */
    @Overwrite
    public static double atan2(double y, double x) {
        return Math.atan2(y, x);
    }

    /**
     * @author Alexxiconify (Rust-MC)
     * @reason Fast bitwise floor block.
     */
    @Overwrite
    public static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }
}
