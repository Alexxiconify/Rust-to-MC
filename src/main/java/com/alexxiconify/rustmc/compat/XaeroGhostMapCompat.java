package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the ghost map texture for Xaero's Minimap/WorldMap overlay.
 * Generates the texture on a background thread to avoid blocking the render thread.
 * Uses a 128x128 texture (GPU upscales) to reduce compute and memory cost.
 */
public class XaeroGhostMapCompat {
    // Single-writer (GHOST_EXECUTOR), single-reader (render thread) — volatile is sufficient
    @SuppressWarnings("java:S3077") // volatile is intentional for single-writer pattern
    private static volatile NativeImageBackedTexture ghostTexture = null;
    private static final Identifier GHOST_TEXTURE_ID = Identifier.of(RustMC.MOD_ID, "textures/gui/ghost_map.png");
    private static double lastUpdateX = -10000;
    private static double lastUpdateZ = -10000;
    private static final int TEXTURE_SIZE = 128;
    private static final double MOVE_THRESHOLD = 16.0;
    private static final AtomicBoolean generating = new AtomicBoolean(false);
    @SuppressWarnings("java:S3077") // volatile is intentional for single-writer pattern
    private static volatile int[] pendingPixels = null;

    private static final ExecutorService GHOST_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rustmc-ghost-map-gen");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private XaeroGhostMapCompat() {}

    /**
     * Returns the ghost map texture Identifier, triggering an async update if needed.
     * Must be called from the render thread.
     */
    public static Identifier getGhostTexture() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || !NativeBridge.isReady()) return null;

        if ( !RustMC.CONFIG.isGhostMapEnabled ( ) ) return null;

        double px = player.getX();
        double pz = player.getZ();

        // Upload pending pixels from background thread if available (atomic local capture)
        int[] pixels = pendingPixels;
        if (pixels != null) {
            pendingPixels = null;
            uploadPixels(pixels);
        }

        // Trigger async regen if player moved far enough
        if (Math.abs(px - lastUpdateX) > MOVE_THRESHOLD
                || Math.abs(pz - lastUpdateZ) > MOVE_THRESHOLD
                || ghostTexture == null) {
            lastUpdateX = px;
            lastUpdateZ = pz;
            requestAsyncUpdate(px, pz);
        }

        return ghostTexture != null ? GHOST_TEXTURE_ID : null;
    }

    private static void requestAsyncUpdate(double centerX, double centerZ) {
        if (!generating.compareAndSet(false, true)) return;

        // Seed handling for multiplayer — apply server-specific seed if available
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && !mc.isInSingleplayer()) {
            net.minecraft.client.network.ServerInfo serverInfo = mc.getCurrentServerEntry();
            if (serverInfo != null && serverInfo.address != null) {
                if (!applyServerSeed(serverInfo.address)) {
                    // No seed configured for this server — skip ghost map generation
                    generating.set(false);
                    return;
                }
            } else {
                // No server info — can't generate map
                generating.set(false);
                return;
            }
        }

        GHOST_EXECUTOR.submit(() -> {
            try {
                int[] pixels = NativeBridge.generateGhostMap(centerX, centerZ, TEXTURE_SIZE, 1.0);
                for (int i = 0; i < pixels.length; i++) {
                    int p = pixels[i];
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    int a = 0x73; // ~45% of 0xFF
                    pixels[i] = (a << 24) | (b << 16) | (g << 8) | r;
                }
                pendingPixels = pixels;
            } finally {
                generating.set(false);
            }
        });
    }

    private static void uploadPixels(int[] pixels) {
        if (ghostTexture == null) {
            ghostTexture = new NativeImageBackedTexture("rustmc_ghost_map", TEXTURE_SIZE, TEXTURE_SIZE, false);
            MinecraftClient.getInstance().getTextureManager().registerTexture(GHOST_TEXTURE_ID, ghostTexture);
        }

        NativeImage image = ghostTexture.getImage();
        if (image != null) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    image.setColor(x, y, pixels[y * TEXTURE_SIZE + x]);
                }
            }
            ghostTexture.upload();
        }
    }

    /**
     * Call on world disconnect to free native texture memory.
     */
    public static void cleanup() {
        if (ghostTexture != null) {
            ghostTexture.close();
            ghostTexture = null;
        }
        pendingPixels = null;
        lastUpdateX = -10000;
        lastUpdateZ = -10000;
    }

    private static boolean applyServerSeed(String ip) {
        String configSeeds = RustMC.CONFIG.getCustomGhostMapSeed();
        if (configSeeds == null || configSeeds.isEmpty()) return false;

        for (String entry : configSeeds.split(",")) {
            if (tryApplySeedEntry(entry, ip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryApplySeedEntry(String entry, String ip) {
        int splitObj = entry.lastIndexOf('=');
        if (splitObj == -1) splitObj = entry.lastIndexOf(':');

        if (splitObj > 0 && splitObj < entry.length() - 1) {
            String serverPart = entry.substring(0, splitObj).trim();
            String seedPart = entry.substring(splitObj + 1).trim();

            if (isIpMatch(ip, serverPart)) {
                applySeedValue(seedPart);
                return true;
            }
        }
        return false;
    }

    private static boolean isIpMatch(String ip, String serverPart) {
        String trimmedIp = ip.trim();
        return trimmedIp.equalsIgnoreCase(serverPart)
            || trimmedIp.contains(serverPart)
            || serverPart.contains(trimmedIp);
    }

    private static void applySeedValue(String seedPart) {
        NativeBridge.noiseReset();
        try {
            NativeBridge.noiseInit(Long.parseLong(seedPart));
        } catch (NumberFormatException e) {
            NativeBridge.noiseInit(seedPart.hashCode());
        }
    }
}