package com.alexxiconify.rustmc.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

// Tick cancellation was removed — MC manages its own 20TPS tick rate internally,
// and cancelling tick() drops key inputs (F3+1, etc.) and breaks debug overlays.
@Mixin(MinecraftClient.class)
public class TickLimiterMixin {
}

