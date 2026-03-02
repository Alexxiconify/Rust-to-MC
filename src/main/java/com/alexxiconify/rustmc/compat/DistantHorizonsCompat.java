package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;

public class DistantHorizonsCompat {
    private static final String DH_MOD_ID = "distanthorizons";

    private DistantHorizonsCompat() {}

    public static void disableFade() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        try {
            // DH config API: try the modern accessor path (DH 2.3+)
            Class<?> apiClass = Class.forName("com.seibel.distanthorizons.api.DhApi");
            Object dhApi = apiClass.getField("Inst").get(null);
            Object overrides = dhApi.getClass().getMethod("overrides").invoke(dhApi);
            // Attempt to disable fade via the public API if available
            overrides.getClass().getMethod("setFadeNearbyLods", boolean.class).invoke(overrides, false);
            RustMC.LOGGER.info("[Rust-MC] Disabled Distant Horizons chunk fade via API.");
        } catch (Exception e) {
            // DH API not available or changed — non-critical, skip silently
            RustMC.LOGGER.debug("[Rust-MC] Could not disable DH fade ({}), skipping.", e.getMessage());
        }
    }

    private static long rustFrustumPtr = 0;
    private static int currentMinY = -64;
    private static int currentMaxY = 320;

    private static java.lang.reflect.Method getValuesAsArrayMethod = null;

    @SuppressWarnings("java:S3776")
    public static void registerFrustumCuller() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) return;
        try {
            rustFrustumPtr = com.alexxiconify.rustmc.NativeBridge.createRustFrustum();
            if (rustFrustumPtr == 0) return;

            Class<?> apiClass = Class.forName("com.seibel.distanthorizons.api.DhApi");
            Object overridesInjector = apiClass.getField("overrides").get(null);
            
            java.lang.reflect.Method bindMethod = null;
            for (java.lang.reflect.Method m : overridesInjector.getClass().getMethods()) {
                if (m.getName().equals("bind") && m.getParameterCount() == 2) {
                    bindMethod = m;
                    break;
                }
            }

            Class<?> cullingFrustumClass = Class.forName("com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiCullingFrustum");
            
            java.lang.reflect.InvocationHandler handler = new java.lang.reflect.InvocationHandler() {
                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    if (name.equals("getPriority")) {
                        return Integer.MAX_VALUE;
                    } else if (name.equals("update") && args.length == 3) {
                        currentMinY = (int) args[0];
                        currentMaxY = (int) args[1];
                        Object mat = args[2];
                        if (getValuesAsArrayMethod == null) {
                            getValuesAsArrayMethod = mat.getClass().getMethod("getValuesAsArray");
                        }
                        float[] vpArray = (float[]) getValuesAsArrayMethod.invoke(mat);
                        com.alexxiconify.rustmc.NativeBridge.updateRustFrustum(rustFrustumPtr, vpArray);
                        return null;
                    } else if (name.equals("intersects") && args.length == 4) {
                        int minX = (int) args[0];
                        int minZ = (int) args[1];
                        int width = (int) args[2];
                        return com.alexxiconify.rustmc.NativeBridge.testRustFrustum(rustFrustumPtr, minX, currentMinY, minZ, (double) minX + width, currentMaxY, (double) minZ + width);
                    }
                    return null;
                }
            };

            Object proxyInstance = java.lang.reflect.Proxy.newProxyInstance(
                DistantHorizonsCompat.class.getClassLoader(),
                new Class<?>[]{cullingFrustumClass},
                handler
            );

            if (bindMethod != null) {
                bindMethod.invoke(overridesInjector, cullingFrustumClass, proxyInstance);
            }
        } catch (Exception e) {
            if (rustFrustumPtr != 0) {
                com.alexxiconify.rustmc.NativeBridge.destroyRustFrustum(rustFrustumPtr);
                rustFrustumPtr = 0;
            }
        }
    }

    /**
     * Public API hook for DH vertex builders or shaders to offload Ambient Occlusion 
     * calculations to the Rust wgpu Compute Shader pipeline.
     * Expects vertices formatted as contiguous floats: [posX, posY, posZ, pad, normX, normY, normZ, pad].
     */
    public static float[] computeRustAmbientOcclusion(float[] vertexData) {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) {
            return new float[0];
        }
        int vertexCount = vertexData.length / 8; // 8 floats per vertex struct in WGSL
        if (vertexCount == 0) return new float[0];
        
        return com.alexxiconify.rustmc.NativeBridge.invokeComputeAmbientOcclusion(vertexData, vertexCount);
    }
}
