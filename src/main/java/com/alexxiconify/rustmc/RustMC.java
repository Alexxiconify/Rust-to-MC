package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
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
        LOGGER.info("Rust to MC initializing...");
        loadConfig();
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
            LOGGER.error("Failed to load config", e);
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(CONFIG));
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
