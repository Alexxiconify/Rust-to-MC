package com.alexxiconify.rustmc;

/**
 * Version compatibility shim.
 * Detects at runtime which MC version we are running on and adapts calls
 * that changed between 1.21.11 (old versioning) and 26.1.x (new calendar versioning).
 *
 * Usage: replace direct MC API calls that differ between versions with
 * VersionCompat.xxx() so the shared source compiles and works on both.
 */
public final class VersionCompat {

    /** Detected at class-load time. */
    public static final boolean IS_26_X;

    static {
        String ver = net.fabricmc.loader.api.FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("0");
        // Calendar versioning starts with "26.", "27.", etc.
        IS_26_X = !ver.startsWith("1.");
    }

    private VersionCompat() {}

    /**
     * Returns the current framebuffer width.
     * Safe across 1.21.x and 26.x (API stable here).
     */
    public static int getFramebufferWidth(net.minecraft.client.MinecraftClient mc) {
        return mc.getWindow().getFramebufferWidth();
    }

    /**
     * Returns the current framebuffer height.
     */
    public static int getFramebufferHeight(net.minecraft.client.MinecraftClient mc) {
        return mc.getWindow().getFramebufferHeight();
    }
}
