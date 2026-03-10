package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alexxiconify.rustmc.config.RustMCConfig;
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

    @Override
    public void onInitialize() {
        LOGGER.info("[Rust-MC] Initializing...");
        loadConfig();
        ModBridge.initialize();

        // Initialize ScalableLux compat if present
        com.alexxiconify.rustmc.compat.ScalableLuxCompat.initialize();

        // Attempt to disable DH fade if enabled and DH is present
        if (CONFIG.isDisableDhFade()) {
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.disableFade();
        }

        if (CONFIG.isUseNativeCulling()) {
            com.alexxiconify.rustmc.compat.DistantHorizonsCompat.registerFrustumCuller();
        }

        // Reflect real native status into config so ModMenu Status screen is accurate
        CONFIG.setNativeReady(NativeBridge.isReady());

        if (NativeBridge.isReady()) {
            LOGGER.info("[Rust-MC] Native optimizations ACTIVE.");
            // Seed noise on every world load so it matches the world seed
            ServerWorldEvents.LOAD.register((server, world) -> {
                BlameLog.begin("World Load (" + world.getRegistryKey().getValue() + ")");
                NativeBridge.noiseReset(); // allow re-seed on new world
                NativeBridge.noiseInit(world.getSeed());
                LOGGER.debug("[Rust-MC] Seeded noise with world seed {}", world.getSeed());
                BlameLog.end();
            });
            // Cleanup resources on world unload
            ServerWorldEvents.UNLOAD.register((server, world) -> {
                BlameLog.begin("World Unload Cleanup");
                NativeCache.clear();
                NativeBridge.dnsCacheClear(); // Free stale DNS entries to reduce memory
                LOGGER.debug("[Rust-MC] Cache stats at unload: hits={}, misses={}, ratio={}%",
                        NativeCache.getHits(), NativeCache.getMisses(),
                        String.format("%.1f", NativeCache.getHitRatio() * 100));
                com.alexxiconify.rustmc.compat.XaeroGhostMapCompat.cleanup();
                BlameLog.end();
                LOGGER.info("[Rust-MC] {}", BlameLog.summary());
            });
        } else {
            LOGGER.warn("[Rust-MC] Native library not available – running in vanilla-fallback mode.");
        }

        // Close Early Loading Bar if it's still open
        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                com.iafenvoy.elb.gui.PreLaunchWindow.remove());
        }

        LOGGER.info("[Rust-MC] Ready.");
    }

    @SuppressWarnings("null")
    public static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            saveConfig();
            return;
        }
        try {
            RustMCConfig loaded = GSON.fromJson(Files.readString(CONFIG_PATH), RustMCConfig.class);
            if (loaded != null) {
                CONFIG.copyFrom(loaded);
            }
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to read config file", e);
        } catch (Exception e) {
            LOGGER.error("[Rust-MC] Failed to parse config (malformed JSON?), resetting to defaults", e);
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
            com.iafenvoy.elb.config.ElbConfig.getInstance().save();
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to save config", e);
        }
    }
}