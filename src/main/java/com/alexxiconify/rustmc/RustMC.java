package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alexxiconify.rustmc.config.RustMCConfig;
import com.alexxiconify.rustmc.util.BlameLog;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RustMC implements ModInitializer {
    public static final String MOD_ID = "rust-mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final RustMCConfig CONFIG = new RustMCConfig();
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.properties");

    @Override
    public void onInitialize() {
        LOGGER.info("[Rust-MC] Initializing...");
        new Thread(null, () -> {
            loadConfig(); // Safe to re-call: config was preloaded during DFU, this just ensures it's done
            syncSystemState();
            NativeBridge.setConfigDir(CONFIG_DIR.toAbsolutePath().toString());
            ModBridge.initialize();

            // Flush per-group mixin application timings into the blame chart
            MixinManager.flushBlameTimings();
        }, "rustmc-init").start();

        // Run independent compat initializations in parallel on platform threads These have no ordering dependencies on each other
        java.util.concurrent.CompletableFuture.runAsync(
            com.alexxiconify.rustmc.compat.ScalableLuxCompat::initialize,
            r -> new Thread(null, r, "rustmc-compat-slx").start());

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            if (!NativeBridge.isReady()) return;
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.registerFrustumCuller();
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.optimizeLodThreading();
        }, r -> new Thread(null, r, "rustmc-compat-dh").start());

        // Reflect real native status into config so ModMenu Status screen is accurate
        CONFIG.setNativeReady(NativeBridge.isReady());

        if (NativeBridge.isReady() && !com.alexxiconify.rustmc.ModBridge.isInteractionOwned()) {
            LOGGER.info("[Rust-MC] Native optimizations ACTIVE.");
            // Load persisted DNS cache from disk for instant server list lookups - backgrounded
            new Thread(null, NativeBridge::dnsCacheLoad, "rustmc-dns-load").start();

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
                NativeBridge.dnsCacheSave(); // Persist DNS IPs to disk
                LOGGER.debug("[Rust-MC] Cache stats at unload: hits={}, misses={}, ratio={}%",
                        NativeCache.getHits(), NativeCache.getMisses(),
                        String.format("%.1f", NativeCache.getHitRatio() * 100));
                BlameLog.end();
                LOGGER.info("[Rust-MC] {}", BlameLog.summary());
            });

            // Save DNS cache on game exit
            Runtime.getRuntime().addShutdownHook(new Thread( NativeBridge :: dnsCacheSave , "rustmc-dns-save"));
        } else {
            LOGGER.warn("[Rust-MC] Native library not available – running in vanilla-fallback mode.");
        }

        // Close Early Loading Bar if it's still open Note: blame log finalization happens in detectGameReady() when "Game took" log fires
        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                com.iafenvoy.elb.gui.PreLaunchWindow.remove());
        }

        LOGGER.info("[Rust-MC] Ready.");
    }

    private static volatile boolean configLoaded = false;

    public static synchronized void loadConfig() {
        if (configLoaded) return; // Already loaded by preload thread
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to create config directory", e);
        }
        
        if (!Files.exists(CONFIG_PATH)) {
            // Check for legacy json config file
            Path legacyPath = FabricLoader.getInstance().getConfigDir().resolve("rust-mc.json");
            Path legacyPath2 = CONFIG_DIR.resolve("config.json");
            if (Files.exists(legacyPath) || Files.exists(legacyPath2)) {
                LOGGER.info("[Rust-MC] Legacy json config detected, migrating to .properties is not supported, overriding");
            }
            saveConfig();
            configLoaded = true;
            return;
        }

        try {
            CONFIG.load(CONFIG_PATH);
            NativeBridge.configUpdate(CONFIG.toJson());
            LOGGER.debug("[Rust-MC] Config loaded from {}", CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("[Rust-MC] Failed to parse config, resetting to defaults", e);
            saveConfig();
        }
        configLoaded = true;
    }

    public static void saveConfig() {
        try {
            CONFIG.save(CONFIG_PATH);
            NativeBridge.configUpdate(CONFIG.toJson());
            com.iafenvoy.elb.config.ElbConfig.getInstance().save();
        } catch (Exception e) {
            LOGGER.error("[Rust-MC] Failed to save config", e);
        }
    }

    private static void syncSystemState() {
        CONFIG.setModSodiumLoaded(ModBridge.SODIUM);
        CONFIG.setModLithiumLoaded(ModBridge.LITHIUM);
        CONFIG.setModIrisLoaded(ModBridge.IRIS);
        CONFIG.setModStarlightLoaded(ModBridge.STARLIGHT);
        CONFIG.setModC2MELoaded(ModBridge.C2ME);
        CONFIG.setModDistantHorizonsLoaded(ModBridge.DISTANT_HORIZONS);
        CONFIG.setModImmediatelyFastLoaded(ModBridge.IMMEDIATELYFAST);
        CONFIG.setModEntityCullingLoaded(ModBridge.ENTITYCULLING);
        CONFIG.setNativeReady(NativeBridge.isReady());
    }
}