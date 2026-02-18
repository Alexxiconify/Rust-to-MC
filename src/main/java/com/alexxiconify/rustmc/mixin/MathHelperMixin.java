package com.alexxiconify.rustmc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.alexxiconify.rustmc.NativeBridge;

import net.minecraft.util.math.MathHelper;

@Mixin(MathHelper.class)
public class MathHelperMixin {
    private MathHelperMixin() {} // Private constructor to hide implicit public one
    @Overwrite public static float fastInvSqrt(float x) { return NativeBridge.fastInvSqrt(x); }
    @Overwrite public static float sin(float x) { return NativeBridge.invokeSin(x); }
    @Overwrite public static float cos(float x) { return NativeBridge.invokeCos(x); }
    @Overwrite public static float sqrt(float x) { return NativeBridge.invokeSqrt(x); }
}
