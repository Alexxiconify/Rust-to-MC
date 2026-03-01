package com.alexxiconify.rustmc.mixin.screen;

import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears the window to a dark color right after GL context init to prevent
 * the white flash before the splash overlay appears.
 */
@Mixin(Window.class)
public class WindowMixin {

    private WindowMixin() {}

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clearOnInit(CallbackInfo ci) {
        try {
            GL11.glClearColor(0.05f, 0.05f, 0.05f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        } catch (IllegalStateException ignored) {
            // No GL context on this thread yet — skip the clear safely
        }
    }
}
