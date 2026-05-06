package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RustMC implements ModInitializer {
    public static final String MOD_ID = "rust-mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Config CONFIG = new Config();
    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    public static final Path BIN_DIR = FabricLoader.getInstance().getConfigDir().resolve("rustmc-bin");
    public static final Path DNS_CACHE_PATH = BIN_DIR.resolve("rust-mc-dns-cache.json");
    private static final Path CONFIG_PATH = BIN_DIR.resolve("rust-mc.json");
    private static final Path BUILD_SIGNATURE_PATH = BIN_DIR.resolve("rust-mc.build-id");

    private static final ScheduledExecutorService DIAGNOSTIC_EXECUTOR = Executors.newScheduledThreadPool(1, task ->
        Thread.ofVirtual().name("rustmc-diagnostics").unstarted(task));
    private static final String BUILD_FINGERPRINT = computeBuildFingerprint();
    private static volatile boolean configLoaded = false;

    public static String getBuildFingerprint() {
        return BUILD_FINGERPRINT;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[Rust-MC] Initializing...");
        try {
            Files.createDirectories(BIN_DIR);
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to create bin directory: {}", e.getMessage());
        }
        loadConfig();
        ModBridgeCache.initialize();
        ModBridge.initialize();
        MixinManager.flushBlameTimings();

        java.util.concurrent.CompletableFuture.runAsync(
          com.alexxiconify.rustmc.compat.ScalableLuxCompat::initialize,
          r -> Thread.ofPlatform().daemon(true).name("rustmc-compat-slx").start(r));

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.registerFrustumCuller();
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.optimizeLodThreading();
        }, r -> Thread.ofPlatform().daemon(true).name("rustmc-compat-dh").start(r));

        CONFIG.setNativeReady(NativeBridge.isReady());
        if (NativeBridge.isReady()) {
            setupNativeFeatures();
        } else {
            LOGGER.warn("[Rust-MC] Native library not available – running in vanilla-fallback mode.");
        }

        setupDiagnosticTask();

        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->  PreLaunchWindow.remove());
        }
        LOGGER.info("[Rust-MC] Ready.");
    }

    private void setupNativeFeatures() {
        LOGGER.info("[Rust-MC] Native optimizations ACTIVE.");
        Thread.ofPlatform().daemon(true).name("rustmc-dns-load").start(NativeBridge::dnsCacheLoad);

        ServerWorldEvents.LOAD.register((server, world) -> {
            BlameLog.begin("World Load (" + world.getRegistryKey().getValue() + ")");
            NativeBridge.noiseReset();
            NativeBridge.noiseInit(world.getSeed());
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.prefetchLodData();
            BlameLog.end();
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            BlameLog.begin("World Unload Cleanup");
            NativeCache.clear();
            NativeBridge.persistDnsCache("world-unload");
            LOGGER.debug("[Rust-MC] Cache stats at unload: hits={}, misses={}, ratio={}%",
            NativeCache.getHits(), NativeCache.getMisses(),
            String.format("%.1f", NativeCache.getHitRatio() / 100));
            BlameLog.end();
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> NativeBridge.persistDnsCache("shutdown"), "rustmc-dns-save"));
    }

    public static class ManagedExecutor implements AutoCloseable {
        private final ScheduledExecutorService executor;
        public ManagedExecutor(ScheduledExecutorService executor) { this.executor = executor; }
        @Override public void close() { executor.shutdown(); }
        public ScheduledExecutorService get() { return executor; }
    }

    public static final class BlameLog {
        private BlameLog() {}
        public record Entry(String phase, long startMs, long endMs) {
            public long durationMs() { return endMs - startMs; }
        }
        private static final java.util.List<Entry> entries = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        private static final long JVM_START_MS = System.currentTimeMillis();
        private static volatile long currentPhaseStart = 0;
        private static volatile String currentPhase = null;
        private static volatile long gameReadyMs = 0;

        public static void begin(String phase) {
            long now = System.currentTimeMillis();
            endCurrent(now);
            currentPhase = phase;
            currentPhaseStart = now;
        }

        public static void end() {
            long now = System.currentTimeMillis();
            endCurrent(now);
            gameReadyMs = now;
        }

        private static void endCurrent(long now) {
            if (currentPhase != null && currentPhaseStart > 0) {
                entries.add(new Entry(currentPhase, currentPhaseStart, now));
                LOGGER.debug("[Blame] {} took {}ms", currentPhase, now - currentPhaseStart);
                currentPhase = null;
                currentPhaseStart = 0;
            }
        }

        public static java.util.List<Entry> getEntries() {
            endCurrent(System.currentTimeMillis());
            return java.util.List.copyOf(entries);
        }

        public static long trackedMs() {
            long sum = 0;
            for (Entry e : getEntries()) sum += e.durationMs();
            return sum;
        }

        public static long wallClockMs() {
            long end = gameReadyMs > 0 ? gameReadyMs : System.currentTimeMillis();
            return end - JVM_START_MS;
        }

        public static String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%n=== Rust-MC Blame Log ===%n"));
            long tracked = 0;
            String fmt = "  %-35s %6dms%n";
            for (Entry e : getEntries()) {
                sb.append(String.format(fmt, e.phase(), e.durationMs()));
                tracked += e.durationMs();
            }
            sb.append(String.format(fmt, "TRACKED TOTAL", tracked));
            sb.append(String.format(fmt, "WALL CLOCK", wallClockMs()));
            return sb.toString();
        }

        public static List<Entry> getEntriesWithGaps() {
            List<Entry> original = getEntries();
            List<Entry> withGaps = new java.util.ArrayList<>();
            long lastEnd = JVM_START_MS;
            
            for (Entry e : original) {
                if (e.startMs() - lastEnd > 50) {
                    withGaps.add(new Entry("⚠ Untracked Gap", lastEnd, e.startMs()));
                }
                withGaps.add(e);
                lastEnd = e.endMs();
            }
            
            long wallClockEnd = gameReadyMs > 0 ? gameReadyMs : System.currentTimeMillis();
            if (wallClockEnd - lastEnd > 50) {
                withGaps.add(new Entry("⚠ Untracked Gap", lastEnd, wallClockEnd));
            }
            
            return withGaps;
        }
    }

    private void setupDiagnosticTask() {
        DIAGNOSTIC_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                var mode = CONFIG.getDiagnosticMode();
                if (mode == Config.DiagnosticMode.TIMING || mode == Config.DiagnosticMode.ALL) {
                    long[] local = NativeBridge.getLocalTimingMetrics();
                    long[] chunk = NativeBridge.getChunkIngestStats();
                    LOGGER.info("[Rust-MC][DIAG] frustumCalls={} frustumTotalNs={} particleCalls={} particleTotalNs={} dhFusedCalls={} dhFusedTotalNs={} | chunkAttempts={} forwards={} failures={} avgIngestMicros={}",
                                local[0], local[1], local[2], local[3], local[4], local[5], chunk[0], chunk[1], chunk[2], chunk[3]);
                }
            } catch (Exception e) {
                LOGGER.debug("[Rust-MC] Diagnostics task failed: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DIAGNOSTIC_EXECUTOR.shutdown();
                if (!DIAGNOSTIC_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                    DIAGNOSTIC_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                DIAGNOSTIC_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                LOGGER.info("[Rust-MC] Diagnostic executor cleanup complete.");
            }
        }));
    }

    public static synchronized void loadConfig() {
        if (configLoaded) return;
        if (!Files.exists(CONFIG_PATH)) {
            saveConfigInternal(false);
            configLoaded = true;
            return;
        }
        try {
            String rawJson = Files.readString(CONFIG_PATH);
            Config loaded = java.util.Objects.requireNonNull(GSON.fromJson(rawJson, Config.class));
            if (Config.CURRENT_CONFIG_VERSION.equals(loaded.getConfigVersion()) && isBuildSignatureCurrent()) {
                CONFIG.copyFrom(loaded);
            } else {
                backupAndResetConfig(loaded == null ? "missing-body" : "schema-or-build-mismatch");
            }
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to read config file", e);
        } catch (Exception e) {
            LOGGER.error("[Rust-MC] Failed to parse config, resetting to defaults", e);
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
        } catch (IOException e) {
            LOGGER.warn("[Rust-MC] Failed to back up outdated config: {}", e.getMessage());
        }
        CONFIG.copyFrom(new Config());
        saveConfigInternal(false);
    }

    private static boolean isBuildSignatureCurrent() {
        try {
            return Files.exists(BUILD_SIGNATURE_PATH) && BUILD_FINGERPRINT.equals(Files.readString(BUILD_SIGNATURE_PATH));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String computeBuildFingerprint() {
        try {
            java.net.URL location = RustMC.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) return "unknown";
            Path source = Path.of(location.toURI());
            if (Files.exists(source)) {
                return (Files.isRegularFile(source) ? source.getFileName() : source.toAbsolutePath()) + "|" + Files.getLastModifiedTime(source).toMillis();
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
            CONFIG.setConfigVersion(Config.CURRENT_CONFIG_VERSION);
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
            Files.writeString(BUILD_SIGNATURE_PATH, BUILD_FINGERPRINT);
            if (persistElbConfig) {
                ElbConfig.getInstance().save();
            }
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to save config", e);
        }
    }

    public static class Config {
        public static final String CURRENT_CONFIG_VERSION = "2.7.1";
        private String configVersion = CURRENT_CONFIG_VERSION;
        private boolean enableSparklineGraph = true;
        private boolean useNativeLighting = true;
        private boolean useFastLoadingScreen = true;
        private boolean enableParticleCulling = true;
        private boolean enableChunkBuilderExpand = true;
        private boolean enableTickSyncCompat = true;
        private boolean enableBBECompat = true;
        private boolean enableEMFCompat = true;
        private boolean enableETFCompat = true;
        private boolean enableAppleSkinCompat = true;
        private boolean enableEntityCullingCompat = true;
        private boolean enableImmediatelyFastCompat = true;
        private boolean enableClientRedstoneSkip = true;
        private boolean enableDhCaveCulling = true;
        private double dhSurfaceY = getDhSurfaceY();
        private int particleCullingDistance = 64;

        public enum HardwarePreset { LOW_END_IGPU, MID_RANGE, HIGH_END_DGPU }
        private HardwarePreset hardwarePreset = HardwarePreset.MID_RANGE;

        public enum DiagnosticMode { HIDDEN, TIMING, NATIVE, ALL }
        private DiagnosticMode diagnosticMode = DiagnosticMode.HIDDEN;

        private boolean enableDnsCache = true;
        private boolean enableChunkIngestOffload = false;
        private boolean enableChunkIngestValidation = false;
        private boolean bridgeSodium = true;
        private boolean bridgeLithium = true;
        private int loadingBarBgColor = 0xFF1A1A1A;
        private int loadingBarLowColor = 0xFF22AA44;
        private int loadingBarMidColor = 0xFFCCAA00;
        private int loadingBarHighColor = 0xFFCC2222;
        private int loadingBarTextColor = 0xDDFFFFFF;
        private int loadingBarSubtextColor = 0x9900FFFF;
        private boolean silenceLogs = true;
        private boolean nativeReady = false;
        private boolean experimentalCoexistEnabled = true;

        public void copyFrom(Config o) {
            this.configVersion = o.configVersion;
            this.enableSparklineGraph = o.enableSparklineGraph;
            this.useNativeLighting = o.useNativeLighting;
            this.useFastLoadingScreen = o.useFastLoadingScreen;
            this.enableParticleCulling = o.enableParticleCulling;
            this.enableChunkBuilderExpand = o.enableChunkBuilderExpand;
            this.enableTickSyncCompat = o.enableTickSyncCompat;
            this.enableBBECompat = o.enableBBECompat;
            this.enableEMFCompat = o.enableEMFCompat;
            this.enableETFCompat = o.enableETFCompat;
            this.enableAppleSkinCompat = o.enableAppleSkinCompat;
            this.enableEntityCullingCompat = o.enableEntityCullingCompat;
            this.enableImmediatelyFastCompat = o.enableImmediatelyFastCompat;
            this.enableClientRedstoneSkip = o.enableClientRedstoneSkip;
            this.enableDhCaveCulling = o.enableDhCaveCulling;
            this.dhSurfaceY = o.dhSurfaceY;
            this.particleCullingDistance = o.particleCullingDistance;
            this.hardwarePreset = o.hardwarePreset;
            this.diagnosticMode = o.diagnosticMode;
            this.enableDnsCache = o.enableDnsCache;
            this.enableChunkIngestOffload = o.enableChunkIngestOffload;
            this.enableChunkIngestValidation = o.enableChunkIngestValidation;
            this.bridgeSodium = o.bridgeSodium;
            this.bridgeLithium = o.bridgeLithium;
            this.loadingBarBgColor = o.loadingBarBgColor;
            this.loadingBarLowColor = o.loadingBarLowColor;
            this.loadingBarMidColor = o.loadingBarMidColor;
            this.loadingBarHighColor = o.loadingBarHighColor;
            this.loadingBarTextColor = o.loadingBarTextColor;
            this.loadingBarSubtextColor = o.loadingBarSubtextColor;
            this.silenceLogs = o.silenceLogs;
            this.nativeReady = o.nativeReady;
            this.experimentalCoexistEnabled = o.experimentalCoexistEnabled;
        }

        public String getConfigVersion() { return configVersion; }
        public boolean isEnableSparklineGraph() { return enableSparklineGraph; }
        public boolean isUseNativeLighting() { return useNativeLighting; }
        public boolean isUseFastLoadingScreen() { return useFastLoadingScreen; }
        public boolean isEnableParticleCulling() { return enableParticleCulling; }
        public boolean isEnableChunkBuilderExpand() { return enableChunkBuilderExpand; }
        public boolean isEnableTickSyncCompat() { return enableTickSyncCompat; }
        public boolean isEnableBBECompat() { return enableBBECompat; }
        public boolean isEnableEMFCompat() { return enableEMFCompat; }
        public boolean isEnableETFCompat() { return enableETFCompat; }
        public boolean isEnableAppleSkinCompat() { return enableAppleSkinCompat; }
        public boolean isEnableEntityCullingCompat() { return enableEntityCullingCompat; }
        public boolean isEnableImmediatelyFastCompat() { return enableImmediatelyFastCompat; }
        public boolean isEnableClientRedstoneSkip() { return enableClientRedstoneSkip; }
        public boolean isEnableDhCaveCulling() { return enableDhCaveCulling; }
        public double getDhSurfaceY() { return dhSurfaceY; }
        public int getParticleCullingDistance() { return particleCullingDistance; }
        public HardwarePreset getHardwarePreset() { return hardwarePreset; }
        public DiagnosticMode getDiagnosticMode() { return diagnosticMode; }
        public boolean isEnableDnsCache() { return enableDnsCache; }
        public boolean isEnableChunkIngestOffload() { return enableChunkIngestOffload; }
        public boolean isEnableChunkIngestValidation() { return enableChunkIngestValidation; }
        public boolean isBridgeSodium() { return bridgeSodium; }
        public boolean isBridgeLithium() { return bridgeLithium; }
        public int getLoadingBarBgColor() { return loadingBarBgColor; }
        public int getLoadingBarLowColor() { return loadingBarLowColor; }
        public int getLoadingBarMidColor() { return loadingBarMidColor; }
        public int getLoadingBarHighColor() { return loadingBarHighColor; }
        public int getLoadingBarTextColor() { return loadingBarTextColor; }
        public int getLoadingBarSubtextColor() { return loadingBarSubtextColor; }
        public boolean isSilenceLogs() { return silenceLogs; }
        public boolean isNativeReady() { return nativeReady; }
        public boolean isExperimentalCoexistEnabled() { return experimentalCoexistEnabled; }

        public void setConfigVersion(String v) { configVersion = v; }
        public void setEnableSparklineGraph(boolean v) { enableSparklineGraph = v; }
        public void setUseNativeLighting(boolean v) { useNativeLighting = v; }
        public void setUseFastLoadingScreen(boolean v) { useFastLoadingScreen = v; }
        public void setEnableParticleCulling(boolean v) { enableParticleCulling = v; }
        public void setEnableChunkBuilderExpand(boolean v) { enableChunkBuilderExpand = v; }
        public void setEnableTickSyncCompat(boolean v) { enableTickSyncCompat = v; }
        public void setEnableBBECompat(boolean v) { enableBBECompat = v; }
        public void setEnableEMFCompat(boolean v) { enableEMFCompat = v; }
        public void setEnableETFCompat(boolean v) { enableETFCompat = v; }
        public void setEnableAppleSkinCompat(boolean v) { enableAppleSkinCompat = v; }
        public void setEnableEntityCullingCompat(boolean v) { enableEntityCullingCompat = v; }
        public void setEnableImmediatelyFastCompat(boolean v) { enableImmediatelyFastCompat = v; }
        public void setEnableClientRedstoneSkip(boolean v) { enableClientRedstoneSkip = v; }
        public void setEnableDhCaveCulling(boolean v) { enableDhCaveCulling = v; }
        public void setDhSurfaceY(double v) { dhSurfaceY = v; }
        public void setParticleCullingDistance(int v) { particleCullingDistance = v; }
        public void setHardwarePreset(HardwarePreset v) { hardwarePreset = v; }
        public void setDiagnosticMode(DiagnosticMode v) { diagnosticMode = v; }
        public void setEnableDnsCache(boolean v) { enableDnsCache = v; }
        public void setEnableChunkIngestOffload(boolean v) { enableChunkIngestOffload = v; }
        public void setEnableChunkIngestValidation(boolean v) { enableChunkIngestValidation = v; }
        public void setBridgeSodium(boolean v) { bridgeSodium = v; }
        public void setBridgeLithium(boolean v) { bridgeLithium = v; }
        public void setLoadingBarBgColor(int v) { loadingBarBgColor = v; }
        public void setLoadingBarLowColor(int v) { loadingBarLowColor = v; }
        public void setLoadingBarMidColor(int v) { loadingBarMidColor = v; }
        public void setLoadingBarHighColor(int v) { loadingBarHighColor = v; }
        public void setLoadingBarTextColor(int v) { loadingBarTextColor = v; }
        public void setLoadingBarSubtextColor(int v) { loadingBarSubtextColor = v; }
        public void setSilenceLogs(boolean v) { silenceLogs = v; }
        public void setNativeReady(boolean v) { nativeReady = v; }
        public void setExperimentalCoexistEnabled(boolean v) { experimentalCoexistEnabled = v; }
    }
}



