package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;

public class DistantHorizonsCompat {
    private DistantHorizonsCompat() {}

    public static void disableFade() {
        if (!FabricLoader.getInstance().isModLoaded("distant-horizons")) return;
        try {
            // Target: Config.CLIENT.graphics.advanced.fadeNearbyDistantHorizonsLODs
            Class<?> configClass = Class.forName("com.seibel.distanthorizons.core.config.Config");
            Object clientConfig = configClass.getField("CLIENT").get(null);
            Object graphicsConfig = clientConfig.getClass().getField("graphics").get(clientConfig);
            Object advancedConfig = graphicsConfig.getClass().getField("advanced").get(graphicsConfig);
            java.lang.reflect.Field fadeField = advancedConfig.getClass().getField("fadeNearbyDistantHorizonsLODs");
            
            fadeField.set(advancedConfig, false);
            RustMC.LOGGER.info("[Rust-MC] Set Distant Horizons chunk fade-out to FALSE.");
        } catch (Exception e) {
            RustMC.LOGGER.warn("[Rust-MC] Failed to disable Distant Horizons chunk fade via reflection: {}", e.getMessage());
        }
    }
}
