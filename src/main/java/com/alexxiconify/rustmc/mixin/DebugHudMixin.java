package com.alexxiconify.rustmc.mixin;

import com.alexxiconify.rustmc.ModBridge;
import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a compact frame-time sparkline graph in the F3 overlay when enabled.
 * The frame history is maintained in the Rust native core as a ring buffer
 * (240 samples). Each bar represents one frame's ms time.
 */
@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, CallbackInfo ci) {
        if (!RustMC.CONFIG.isEnableDebugHudGraph() || !RustMC.CONFIG.isUseNativeF3() || ModBridge.isHudOwned()) return;
        if (!NativeBridge.isReady()) return;

        float[] history = NativeBridge.invokeGetFrameHistory();
        if (history == null || history.length == 0) return;

        int graphW = Math.min(history.length, 240);
        int graphH = 40;
        int x0 = 2;
        int y0 = 2;

        // Background
        context.fill(x0 - 1, y0 - 1, x0 + graphW + 1, y0 + graphH + 1, 0x80000000);

        // 16.67ms target line (60 FPS)
        int targetY = y0 + graphH - (int)(16.67f / 50.0f * graphH);
        context.fill(x0, targetY, x0 + graphW, targetY + 1, 0x4000FF00);

        // 33.33ms target line (30 FPS)
        int target30Y = y0 + graphH - (int)(33.33f / 50.0f * graphH);
        context.fill(x0, target30Y, x0 + graphW, target30Y + 1, 0x40FFFF00);

        for (int i = 0; i < graphW; i++) {
            float ms = history[i];
            int barH = Math.min(graphH, Math.max(1, (int)(ms / 50.0f * graphH)));
            int barY = y0 + graphH - barH;
            int color;
            if (ms < 16.67f) {
                color = 0xFF00CC44; // Green — above 60 FPS
            } else if (ms < 33.33f) {
                color = 0xFFCCAA00; // Yellow — 30-60 FPS
            } else {
                color = 0xFFCC2222; // Red — below 30 FPS
            }
            context.fill(x0 + i, barY, x0 + i + 1, y0 + graphH, color);
        }
    }
}
