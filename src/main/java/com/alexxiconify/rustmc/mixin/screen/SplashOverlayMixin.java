package com.alexxiconify.rustmc.mixin.screen;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RamBarRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
 //  Replaces vanilla's SplashOverlay rendering with a dark background + real progress bar
 //  + compact RAM bar underneath.  The HEAD injection fills a dark bg to eliminate the
 //  white flash.  The TAIL injection draws progress info on top.
 //  <p>
 //  This also speeds up loads because we avoid the vanilla logo/orange gradient rendering,
 //  freeing up GPU draw calls that would otherwise compete with resource upload.
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Unique private static long splashStartMs = 0;
    @Unique
    private static void initStartTime() {
        if (splashStartMs == 0) {
            splashStartMs = System.currentTimeMillis();
        }
    }
    @Inject(at = @At("HEAD"), method = "render")
    public void renderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!RustMC.CONFIG.isUseFastLoadingScreen()) return;
        initStartTime();
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        // Dark background — eliminates vanilla white flash and reduces GPU work
        context.fill(0, 0, w, h, 0xFF0D0D0D);
    }
    @Inject(at = @At("TAIL"), method = "render")
    public void renderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!RustMC.CONFIG.isUseFastLoadingScreen()) return;
        if (this.client.textRenderer == null) return;
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        // ── Actual resource loading progress ──
        float progress = 0f;
        try {
            progress = this.reload.getProgress();
        } catch (Exception ignored) { // Reload might not be started yet.
        }
        int pct = (int)(progress * 100);
        // Progress bar (center of screen)
        int barW = Math.min(300, w - 40);
        int barH = 6;
        int bx = (w - barW) / 2;
        int by = h / 2 + 10;
        // Track
        context.fill(bx, by, bx + barW, by + barH, 0xFF1A1A1A);
        // Fill — green-to-cyan gradient approximation
        int fillW = (int)(barW * progress);
        if (fillW > 0) {
            int r = (int)(40 + progress * (25 - 40));
            int g = (int)(170 + progress * (200 - 170));
            int b = (int)(95 + progress * (255 - 95));
            int fillColor = 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            context.fill(bx, by, bx + fillW, by + barH, fillColor);
        }
        // Progress text — centered above bar
        String progressText = pct + "%";
        context.drawCenteredTextWithShadow(this.client.textRenderer, progressText, w / 2, by - 12, 0xFFDDDDEE);
        // Stage label
        String stage;
        if (pct >= 95) stage = "Finishing up...";
        else if (pct >= 60) stage = "Loading resources...";
        else if (pct >= 20) stage = "Building resource graph...";
        else stage = "Initializing...";
        context.drawCenteredTextWithShadow(this.client.textRenderer, stage, w / 2, by + barH + 4, 0xFF46BEFF);
        // Elapsed time
        long elapsedMs = System.currentTimeMillis() - splashStartMs;
        String elapsed = String.format("%.1fs", elapsedMs / 1000.0);
        context.drawCenteredTextWithShadow(this.client.textRenderer, elapsed, w / 2, by + barH + 16, 0xFF787890);
        // ── Compact RAM bar at very bottom ──
        RamBarRenderer.drawRamBar(context, this.client.textRenderer, w, h, 0xFF1A1A1A);
        // Mod count line
        int modCount = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
        int ramBarY = h - 22;
        context.drawCenteredTextWithShadow(this.client.textRenderer,
                "Rust-MC  •  " + modCount + " mods", w / 2, ramBarY - 9,
                RustMC.CONFIG.getLoadingBarSubtextColor());
    }
}