package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;

// getWorkerCount was removed in 1.21.11; this mixin is a no-op stub.
@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {
}

