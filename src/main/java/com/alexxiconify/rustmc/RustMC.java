package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alexxiconify.rustmc.config.RustMCConfig;
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

        // Reflect real native status into config so ModMenu Status screen is accurate
        CONFIG.setNativeReady(NativeBridge.isReady());

        if (NativeBridge.isReady()) {
            LOGGER.info("[Rust-MC] Native optimizations ACTIVE.");
            // Seed noise on every world load so it matches the world seed
            ServerWorldEvents.LOAD.register((server, world) -> {
                NativeBridge.noiseReset(); // allow re-seed on new world
                NativeBridge.noiseInit(world.getSeed());
                LOGGER.debug("[Rust-MC] Seeded noise with world seed {}", world.getSeed());
            });
        } else {
            LOGGER.warn("[Rust-MC] Native library not available – running in vanilla-fallback mode.");
        }

        LOGGER.info("[Rust-MC] Ready.");
    }

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
            LOGGER.error("[Rust-MC] Failed to load config", e);
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
        } catch (IOException e) {
            LOGGER.error("[Rust-MC] Failed to save config", e);
        }
    }
}
