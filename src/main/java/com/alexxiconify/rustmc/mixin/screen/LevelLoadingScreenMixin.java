package com.alexxiconify.rustmc.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.alexxiconify.rustmc.RustMC;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    private boolean initialJoin = true;

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (RustMC.CONFIG.isUseFastLoadingScreen() && !initialJoin) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "removed")
    public void removed(CallbackInfo ci) {
        if (RustMC.CONFIG.isUseFastLoadingScreen()) {
            initialJoin = false;
        }
    }
}
