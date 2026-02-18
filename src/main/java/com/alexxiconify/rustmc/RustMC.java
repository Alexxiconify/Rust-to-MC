package com.alexxiconify.rustmc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RustMC implements ModInitializer {
    public static final String MOD_ID = "rust-mc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public void onInitialize() {
        LOGGER.info("Rust to MC initializing...");
    }
}
