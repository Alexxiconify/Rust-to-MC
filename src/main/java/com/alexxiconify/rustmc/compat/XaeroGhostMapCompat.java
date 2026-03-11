package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.ClientPlayerEntity;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the ghost map texture for Xaero's Minimap/WorldMap overlay.
 * Generates terrain from the world seed via Rust noise on a background thread.
 * <p>
 * Seed sources (in priority order):
 * <ol>
 *   <li>Singleplayer — uses the integrated server world seed directly</li>
 *   <li>MapLink/Bluemap — reads the server seed from MapLink's config if available</li>
 *   <li>Manual config — {@code customGhostMapSeed} in rust-mc.json, format: {@code server=seed, server2=seed2}</li>
 * </ol>
 * Uses a 128×128 texture (GPU upscales) to reduce compute and memory cost.
 */
public class XaeroGhostMapCompat {
    @SuppressWarnings("java:S3077")
    private static volatile NativeImageBackedTexture ghostTexture = null;
    private static final Identifier GHOST_TEXTURE_ID = Identifier.of(RustMC.MOD_ID, "textures/gui/ghost_map.png");
    private static double lastUpdateX = -10000;
    private static double lastUpdateZ = -10000;
    private static final int TEXTURE_SIZE = 128;
    private static final double MOVE_THRESHOLD = 16.0;
    private static final AtomicBoolean generating = new AtomicBoolean(false);
    @SuppressWarnings("java:S3077")
    private static volatile int[] pendingPixels = null;

    /** Tracks whether we already seeded noise for this session (avoids re-seeding every frame). */
    private static volatile boolean sessionSeeded = false;

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
        if (!RustMC.CONFIG.isGhostMapEnabled()) return null;

        double px = player.getX();
        double pz = player.getZ();

        // Upload pending pixels from background thread if available
        int[] pixels = pendingPixels;
        if (pixels != null) {
            pendingPixels = null;
            uploadPixels(pixels);
        }

        // Trigger async regen if player moved far enough or texture doesn't exist
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

        // Seed the noise generator if not yet done this session
        if (!sessionSeeded) {
            if (!trySeedNoise()) {
                generating.set(false);
                return;
            }
            sessionSeeded = true;
        }

        GHOST_EXECUTOR.submit(() -> {
            try {
                int[] pixels = NativeBridge.generateGhostMap(centerX, centerZ, TEXTURE_SIZE, 1.0);
                if (pixels == null || pixels.length == 0) return;
                // Convert ARGB → ABGR for NativeImage and apply 45% alpha
                for (int i = 0; i < pixels.length; i++) {
                    int p = pixels[i];
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    int a = 0x73; // ~45% opacity
                    pixels[i] = (a << 24) | (b << 16) | (g << 8) | r;
                }
                pendingPixels = pixels;
            } finally {
                generating.set(false);
            }
        });
    }

    /**
     * Attempts to seed the Rust noise generator for ghost map generation.
     * Tries sources in order: singleplayer world seed → MapLink config → manual config.
     * @return true if a seed was applied
     */
    private static boolean trySeedNoise() {
        MinecraftClient mc = MinecraftClient.getInstance();

        // 1) Singleplayer — use the actual world seed directly
        var integratedServer = mc.getServer();
        if (mc.isInSingleplayer() && integratedServer != null) {
            long worldSeed = integratedServer.getOverworld().getSeed();
            applySeed(worldSeed);
            return true;
        }

        // 2) Multiplayer — try to find the server's seed
        net.minecraft.client.network.ServerInfo serverInfo = mc.getCurrentServerEntry();
        if (serverInfo == null || serverInfo.address == null) return false;
        String serverAddr = serverInfo.address.trim();

        // 2a) Try MapLink/Bluemap config seed
        Long mapLinkSeed = readMapLinkSeed(serverAddr);
        if (mapLinkSeed != null) {
            applySeed(mapLinkSeed);
            return true;
        }

        // 2b) Try manual config entries (format: "server=seed, server2=seed2")
        return applyConfigSeed(serverAddr);
    }

    /**
     * Reads the world seed from MapLink's Bluemap configuration.
     * MapLink stores per-server Bluemap URLs in its config. We look for a matching
     * server entry and try to extract seed info from the Bluemap map data.
     * <p>
     * MapLink config is typically at: .minecraft/config/maplink.json
     * The config contains Bluemap server URLs that we can parse for seed data.
     */
    private static Long readMapLinkSeed(String serverAddr) {
        try {
            Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
            Path mapLinkConfig = configDir.resolve("maplink.json");
            if (!Files.exists(mapLinkConfig)) return null;

            String content = Files.readString(mapLinkConfig);
            // MapLink config is JSON — look for a Bluemap URL associated with this server
            // Format varies but typically contains "url" fields paired with server addresses
            // We search for the server address and try to extract seed from nearby Bluemap data
            if (!content.contains(serverAddr) && !containsServerMatch(content, serverAddr)) {
                return null;
            }

            // Try to find seed in MapLink's Bluemap data directory
            Path mapLinkData = configDir.resolve("maplink");
            if (Files.isDirectory(mapLinkData)) {
                return scanMapLinkDataForSeed(mapLinkData);
            }
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not read MapLink config: {}", e.getMessage());
        }
        return null;
    }

    private static boolean containsServerMatch(String content, String serverAddr) {
        // Strip port for matching
        String host = serverAddr.contains(":") ? serverAddr.substring(0, serverAddr.lastIndexOf(':')) : serverAddr;
        return content.contains(host);
    }

    /**
     * Scans MapLink's data directory for Bluemap JSON map data containing a seed.
     * Bluemap map configs can contain world seed information.
     */
    private static Long scanMapLinkDataForSeed(Path dataDir) {
        try (var files = Files.walk(dataDir, 3)) {
            return files
                .filter(p -> p.toString().endsWith(".json"))
                .map(XaeroGhostMapCompat::extractSeedFromJson)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not scan MapLink data: {}", e.getMessage());
            return null;
        }
    }

    /** Extracts a "seed" value from a JSON file (simple key search, no full parser needed). */
    private static Long extractSeedFromJson(Path jsonFile) {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Look for "seed": 12345 or "seed": "12345"
                int seedIdx = line.indexOf("\"seed\"");
                if (seedIdx < 0) continue;
                String after = line.substring(seedIdx + 6).replaceAll("[\":, \\t]", "").trim();
                Long parsed = parseLongOrNull(after);
                if (parsed != null) return parsed;
            }
        } catch (Exception ignored) {
            // File unreadable
        }
        return null;
    }

    /** Parses a Long from a string, returning null instead of throwing on failure. */
    private static Long parseLongOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Tries to apply a seed from the manual config entries.
     * Format: "server1=seed1, server2=seed2"
     * The '=' separator is used to avoid ambiguity with ':' in hostnames/ports.
     */
    private static boolean applyConfigSeed(String serverAddr) {
        String configSeeds = RustMC.CONFIG.getCustomGhostMapSeed();
        if (configSeeds == null || configSeeds.isEmpty()) return false;

        for (String entry : configSeeds.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0 && eqIdx < trimmed.length() - 1) {
                String serverPart = trimmed.substring(0, eqIdx).trim();
                String seedPart = trimmed.substring(eqIdx + 1).trim();

                if (isServerMatch(serverAddr, serverPart)) {
                    applySeedFromString(seedPart);
                    return true;
                }
            }
        }
        return false;
    }

    /** Checks if the server address matches the config entry (case-insensitive, partial match). */
    private static boolean isServerMatch(String serverAddr, String configServer) {
        String addrLower = serverAddr.toLowerCase(java.util.Locale.ROOT);
        String configLower = configServer.toLowerCase(java.util.Locale.ROOT);
        return addrLower.equals(configLower)
            || addrLower.contains(configLower)
            || configLower.contains(addrLower);
    }

    private static void applySeed(long seed) {
        NativeBridge.noiseReset();
        NativeBridge.noiseInit(seed);
    }

    private static void applySeedFromString(String seedStr) {
        try {
            applySeed(Long.parseLong(seedStr));
        } catch (NumberFormatException e) {
            applySeed(seedStr.hashCode());
        }
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
     * Call on world disconnect to free native texture memory and reset session state.
     */
    public static void cleanup() {
        if (ghostTexture != null) {
            ghostTexture.close();
            ghostTexture = null;
        }
        pendingPixels = null;
        lastUpdateX = -10000;
        lastUpdateZ = -10000;
        sessionSeeded = false;
    }
}