package com.alexxiconify.rustmc.mixin.screen;

import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
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
            // GL.getCapabilities() throws IllegalStateException if no context is current.
            // We must check capabilities exist before calling any GL function, otherwise
            // LWJGL dereferences a null function pointer causing a native SIGSEGV crash.
            GLCapabilities caps = GL.getCapabilities();
            if ( caps.glClearColor == 0L ) return;

            GL11.glClearColor(0.05f, 0.05f, 0.05f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        } catch (IllegalStateException | NullPointerException ignored) {
            // No GL context on this thread yet — skip the clear safely
        }
    }
}