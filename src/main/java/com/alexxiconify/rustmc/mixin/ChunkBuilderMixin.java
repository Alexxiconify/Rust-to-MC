package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {

    // Increase the priority of ChunkBuilder worker threads to speed up chunk compilation.
    // This is a common performance tweak that prioritize chunk rendering over other background tasks.
    @Inject(method = "getWorkerCount", at = @At("HEAD"), cancellable = true)
    private void optimizeWorkerCount(CallbackInfoReturnable<Integer> cir) {
        // Use all available processors minus 1 to leave room for the main thread and GC.
        // Provides a solid performance boost on multi-core CPU setups for vanilla processing.
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        cir.setReturnValue(cores);
    }
}
