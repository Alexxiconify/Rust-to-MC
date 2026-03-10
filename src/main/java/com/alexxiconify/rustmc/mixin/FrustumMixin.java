package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin is kept as a placeholder for when stateless frustum testing is implemented.
 */
@Mixin(Frustum.class)
public class FrustumMixin {
    // No-op: stateless frustum hook disabled to save ~0.2ms/frame of JNI overhead.
    // DH frustum culling is handled by DistantHorizonsCompat.registerFrustumCuller().
}