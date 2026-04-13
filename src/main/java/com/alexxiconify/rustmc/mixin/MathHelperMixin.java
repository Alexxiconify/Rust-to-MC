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

    @Overwrite
    public static float sin(double value) {
        if (!ModBridge.isMathConflict() && RustMC.CONFIG.isUseNativeSine())
            return SINE_TABLE[(int)(value * 10430.378) & 65535];
        return (float) Math.sin(value);
    }

    @Overwrite
    public static float cos(double value) {
        if (!ModBridge.isMathConflict() && RustMC.CONFIG.isUseNativeCos())
            return SINE_TABLE[(int)(value * 10430.378 + 16384.0) & 65535];
        return (float) Math.cos(value);
    }



    @Overwrite
    public static double atan2(double y, double x) {
        if (!ModBridge.isMathConflict() && RustMC.CONFIG.isUseNativeAtan2())
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

    @Overwrite
    public static int floor(double d) {
        if (!ModBridge.isMathConflict() && RustMC.CONFIG.isUseNativeFloor()) {
            int i = (int) d;
            return d < i ? i - 1 : i;
        }
        return (int) Math.floor(d);
    }

    @Overwrite
    public static float clamp(float value, float min, float max) {
        return Math.clamp(value, min, max);
    }

    @Overwrite
    public static double lerp(double delta, double start, double end) {
        if (!ModBridge.isMathConflict())
            return Math.fma(delta, end - start, start);
        return start + delta * (end - start);
    }

    @Overwrite
    public static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }


}