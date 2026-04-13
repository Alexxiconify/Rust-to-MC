package com.alexxiconify.rustmc.mixin.screen;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.util.RamBarRenderer;
import net.fabricmc.loader.api.FabricLoader;
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
    @Unique private static final long TEXT_UPDATE_INTERVAL_MS = 100L;
    @Unique private static long lastTextUpdateMs = -1L;
    @Unique private static int cachedProgressPct = -1;
    @Unique private static String cachedProgressText = "0%";
    @Unique private static String cachedStageText = "Initializing...";
    @Unique private static String cachedElapsedText = "0.0s";
    @Unique private static String cachedModLine = "Rust-MC  •  0 mods";
    @Unique private static boolean cachedModLineReady;
    @Unique
    private static void initStartTime() {
        if (splashStartMs == 0) {
            splashStartMs = System.currentTimeMillis();
        }
        if (!cachedModLineReady) {
            cachedModLine = "Rust-MC  •  " + FabricLoader.getInstance().getAllMods().size() + " mods";
            cachedModLineReady = true;
        }
    }
    @Unique
    private static void refreshTextCache(float progress) {
        long now = System.currentTimeMillis();
        int pct = Math.clamp((int) (progress * 100.0f), 0, 100);
        if (pct == cachedProgressPct && now - lastTextUpdateMs < TEXT_UPDATE_INTERVAL_MS) {
            return;
        }
        cachedProgressPct = pct;
        cachedProgressText = pct + "%";
        if (pct >= 95) cachedStageText = "Finishing up...";
        else if (pct >= 60) cachedStageText = "Loading resources...";
        else if (pct >= 20) cachedStageText = "Building resource graph...";
        else cachedStageText = "Initializing...";
        cachedElapsedText = formatElapsedSeconds(Math.max(0L, now - splashStartMs));
        lastTextUpdateMs = now;
    }
    @Unique
    private static String formatElapsedSeconds(long elapsedMs) {
        long elapsedTenths = (elapsedMs + 50L) / 100L;
        long wholeSeconds = elapsedTenths / 10L;
        long tenths = elapsedTenths % 10L;
        return wholeSeconds + "." + tenths + "s";
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
        refreshTextCache(progress);
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
        context.drawCenteredTextWithShadow(this.client.textRenderer, cachedProgressText, w / 2, by - 12, 0xFFDDDDEE);
        context.drawCenteredTextWithShadow(this.client.textRenderer, cachedStageText, w / 2, by + barH + 4, 0xFF46BEFF);
        context.drawCenteredTextWithShadow(this.client.textRenderer, cachedElapsedText, w / 2, by + barH + 16, 0xFF787890);
        // ── Compact RAM bar at very bottom ──
        RamBarRenderer.drawRamBar(context, this.client.textRenderer, w, h, 0xFF1A1A1A);
        // Mod count line
        int ramBarY = h - 22;
        context.drawCenteredTextWithShadow(this.client.textRenderer,
                cachedModLine, w / 2, ramBarY - 9,
                RustMC.CONFIG.getLoadingBarSubtextColor());
    }
}