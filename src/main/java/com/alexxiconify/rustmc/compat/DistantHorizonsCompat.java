package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.RustMC;

import java.lang.reflect.Method;

public class DistantHorizonsCompat {
    private DistantHorizonsCompat() {}

    public static void disableFade() {
        try {
            // Attempt to use DH API to disable LOD fade as requested
            Class<?> apiClass = Class.forName("com.seibel.distanthorizons.api.DistantHorizonsAPI");
            
            // Accessing configuration through the generic interface hierarchy
            Object interfaces = apiClass.getMethod("getInterfaces").invoke(null);
            
            Method getConfigMethod = interfaces.getClass().getMethod("getConfig");
            Object config = getConfigMethod.invoke(interfaces);
            
            Object clientConfig = config.getClass().getMethod("getClient").invoke(config);
            Object graphicsConfig = clientConfig.getClass().getMethod("getGraphics").invoke(clientConfig);
            Object advanced = graphicsConfig.getClass().getMethod("getAdvancedGraphicsFeatures").invoke(graphicsConfig);
            
            disableFadeImpl(advanced);
            RustMC.LOGGER.info("[Rust-MC] Distant Horizons fade disabled via API.");
        } catch (Exception e) {
            RustMC.LOGGER.warn("[Rust-MC] Failed to disable Distant Horizons fade via API: {}", e.getMessage());
        }
    }

    private static void disableFadeImpl(Object advanced) {
        try {
            // Set the specific lod fade setting config value to false
            Object lodFadeSetting = advanced.getClass().getMethod("getLodFade").invoke(advanced);
            if (lodFadeSetting != null) {
                lodFadeSetting.getClass().getMethod("setValue", Object.class).invoke(lodFadeSetting, false);
            }
        } catch (Exception e) {
            RustMC.LOGGER.warn("[Rust-MC] Could not find getLodFade in DH API, trying fallback.", e);
        }
    }
}
