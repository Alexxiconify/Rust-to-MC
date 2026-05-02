package com.alexxiconify.rustmc;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alexxiconify.rustmc.config.RustMCConfig;
import com.alexxiconify.rustmc.util.DnsCacheUtil;
import com.alexxiconify.rustmc.util.BlameLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class RustMC implements ModInitializer {
    public static final String MOD_ID = "rust-mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final RustMCConfig CONFIG = new RustMCConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("rust-mc.json");
    private static final Path BUILD_SIGNATURE_PATH = FabricLoader.getInstance().getConfigDir().resolve("rust-mc.build-id");
    private static final String BUILD_FINGERPRINT = computeBuildFingerprint();

    public static String getBuildFingerprint() {
        return BUILD_FINGERPRINT;
    }
    @Override
    public void onInitialize() {
        LOGGER.info("[Rust-MC] Initializing...");
        loadConfig(); // Safe to re-call: config was preloaded during DFU, this just ensures it's done
        ModBridgeCache.initialize(); // Cache mod detection results for hot-path efficiency
        ModBridge.initialize();
        // ...existing code...
        // Flush per-group mixin application timings into the blame chart
        MixinManager.flushBlameTimings();
        // Run independent compat initializations in parallel on platform daemon threads
        // These have no ordering dependencies on each other
        java.util.concurrent.CompletableFuture.runAsync(
            com.alexxiconify.rustmc.compat.ScalableLuxCompat::initialize,
            r -> Thread.ofPlatform().daemon(true).name("rustmc-compat-slx").start(r));
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.registerFrustumCuller();
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.optimizeLodThreading();
        }, r -> Thread.ofPlatform().daemon(true).name("rustmc-compat-dh").start(r));
        // Reflect real native status into config so ModMenu Status screen is accurate
        CONFIG.setNativeReady(NativeBridge.isReady());
        if (NativeBridge.isReady()) {
            LOGGER.info("[Rust-MC] Native optimizations ACTIVE.");
            // Load persisted DNS cache from disk for instant server list lookups - backgrounded
            Thread.ofPlatform().daemon(true).name("rustmc-dns-load").start(NativeBridge::dnsCacheLoad);
            // Seed noise on every world load so it matches the world seed
            ServerWorldEvents.LOAD.register((server, world) -> {
                BlameLog.begin("World Load (" + world.getRegistryKey().getValue() + ")");
                NativeBridge.noiseReset(); // allow re-seed on new world
                NativeBridge.noiseInit(world.getSeed());
                LOGGER.debug("[Rust-MC] Seeded noise with world seed {}", world.getSeed());
                // Pre-warm DH's LOD cache on a background thread to reduce initial pop-in
                com.alexxiconify.rustmc.compat.DistantHorizonsCompat.prefetchLodData();
                BlameLog.end();
            });
            // Cleanup resources on world unload
            ServerWorldEvents.UNLOAD.register((server, world) -> {
                BlameLog.begin("World Unload Cleanup");
                NativeCache.clear();
                DnsCacheUtil.persistDnsCache("world-unload");
                LOGGER.debug("[Rust-MC] Cache stats at unload: hits={}, misses={}, ratio={}%",
                        NativeCache.getHits(), NativeCache.getMisses(),
                        String.format("%.1f", NativeCache.getHitRatio() / 100));
                BlameLog.end();
                LOGGER.info("[Rust-MC] {}", BlameLog.summary());
            });
            // Save DNS cache on game exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> DnsCacheUtil.persistDnsCache("shutdown"), "rustmc-dns-save"));
        } else {
            LOGGER.warn("[Rust-MC] Native library not available – running in vanilla-fallback mode.");
        }
        // Background diagnostics logger (enabled when DiagnosticMode is TIMING or ALL)
        Thread.ofPlatform().daemon(true).name("rustmc-diagnostics").start(() -> {
            try {
                while (true) {
                    try {
                        var mode = CONFIG.getDiagnosticMode();
                        if (mode == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.TIMING
                                || mode == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.ALL) {
                            long[] local = NativeBridge.getLocalTimingMetrics();
                            long[] chunk = NativeBridge.getChunkIngestStats();
                            LOGGER.info("[Rust-MC][DIAG] frustumCalls={} frustumTotalNs={} particleCalls={} particleTotalNs={} dhFusedCalls={} dhFusedTotalNs={} | chunkAttempts={} forwards={} failures={} avgIngestMicros={}",
                                    local[0], local[1], local[2], local[3], local[4], local[5], chunk[0], chunk[1], chunk[2], chunk[3]);
                        }
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) { break; }
                }
            } catch (Throwable t) {
                LOGGER.debug("[Rust-MC] Diagnostics thread died: {}", t.getMessage());
            }
        });
        // Close Early Loading Bar if it's still open
        // Note: blame log finalization happens in detectGameReady() when "Game took" log fires
        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                PreLaunchWindow.remove());
        }
        LOGGER.info("[Rust-MC] Ready.");
    }
    private static volatile boolean configLoaded = false;
    public static synchronized void loadConfig() {
        if (configLoaded) return; // Already loaded by preload thread
        if (!Files.exists(CONFIG_PATH)) {
            saveConfigInternal(false);configLoaded = true;
            return;
        }
        try {
            String rawJson = Files.readString(CONFIG_PATH);
            RustMCConfig loaded = GSON.fromJson(rawJson, RustMCConfig.class);
            if (loaded != null
                && RustMCConfig.CURRENT_CONFIG_VERSION.equals(loaded.getConfigVersion())
                && isBuildSignatureCurrent()) {
                CONFIG.copyFrom(loaded);
            } else {
                backupAndResetConfig(loaded == null ? "missing-body" : "schema-or-build-mismatch");
                configLoaded = true;
                return;
            }
            LOGGER.debug("[Rust-MC] Config loaded from {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to read config file", e);
        } catch (Exception e) {
            LOGGER.error("[Rust-MC] Failed to parse config (malformed JSON?), resetting to defaults", e);
            saveConfigInternal(false);
        }
        configLoaded = true;
    }

    private static void backupAndResetConfig(String reason) {
        try {
            Path backupPath = CONFIG_PATH.resolveSibling("rust-mc.json.bak");
            if (Files.exists(CONFIG_PATH)) {
                Files.copy(CONFIG_PATH, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.warn("[Rust-MC] Config reset due to {}. Backup written to {}", reason, backupPath);
        } catch (IOException backupError) {
            LOGGER.warn("[Rust-MC] Failed to back up outdated config: {}", backupError.getMessage());
        }
        RustMCConfig defaults = new RustMCConfig();
        CONFIG.copyFrom(defaults);
        saveConfigInternal(false);
    }

    private static boolean isBuildSignatureCurrent() {
        try {
            return Files.exists(BUILD_SIGNATURE_PATH)
                && BUILD_FINGERPRINT.equals(Files.readString(BUILD_SIGNATURE_PATH));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String computeBuildFingerprint() {
        try {
            java.net.URL location = RustMC.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return "unknown";
            }
            Path source = Path.of(location.toURI());
            if (Files.exists(source)) {
                if (Files.isRegularFile(source)) {
                    return source.getFileName() + "|" + Files.size(source) + "|" + Files.getLastModifiedTime(source).toMillis();
                }
                return source.toAbsolutePath() + "|" + Files.getLastModifiedTime(source).toMillis();
            }
            return location.toExternalForm();
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    public static void saveConfig() {
        saveConfigInternal(true);
    }

    private static void saveConfigInternal(boolean persistElbConfig) {
        try {
            CONFIG.setConfigVersion(RustMCConfig.CURRENT_CONFIG_VERSION);
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
            Files.writeString(BUILD_SIGNATURE_PATH, BUILD_FINGERPRINT);
            if (persistElbConfig) {
                ElbConfig.getInstance().save();
            }
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to save config", e);
        }
    }
}