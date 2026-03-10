package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Reflection-based integration with Distant Horizons.
 * API methods like {@link #computeRustAmbientOcclusion} and {@link #computeRustAmbientOcclusionDirect}
 * are public API for DH vertex builders to offload AO to Rust's wgpu compute pipeline.
 */
@SuppressWarnings("unused")
public class DistantHorizonsCompat {
    private static final String DH_MOD_ID = "distanthorizons";

    private DistantHorizonsCompat() {}

    public static void disableFade() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        try {
            Class<?> apiClass = Class.forName("com.seibel.distanthorizons.api.DhApi");
            Object dhApi = apiClass.getField("Inst").get(null);
            Object overrides = dhApi.getClass().getMethod("overrides").invoke(dhApi);
            overrides.getClass().getMethod("setFadeNearbyLods", boolean.class).invoke(overrides, false);
            RustMC.LOGGER.info("[Rust-MC] Disabled Distant Horizons chunk fade via API.");
        } catch (Exception e) {
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

            java.lang.reflect.Method bindMethod = findBindMethod(overridesInjector);

            Class<?> cullingFrustumClass = Class.forName("com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiCullingFrustum");

            Object proxyInstance = java.lang.reflect.Proxy.newProxyInstance(
                DistantHorizonsCompat.class.getClassLoader(),
                new Class<?>[]{cullingFrustumClass},
                DistantHorizonsCompat::handleFrustumProxy
            );

            if (bindMethod != null) {
                bindMethod.invoke(overridesInjector, cullingFrustumClass, proxyInstance);
                RustMC.LOGGER.info("[Rust-MC] Registered Rust frustum culler with Distant Horizons.");
            }
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not register DH frustum culler ({}), skipping.", e.getMessage());
            if (rustFrustumPtr != 0) {
                com.alexxiconify.rustmc.NativeBridge.destroyRustFrustum(rustFrustumPtr);
                rustFrustumPtr = 0;
            }
        }
    }

    private static java.lang.reflect.Method findBindMethod(Object overridesInjector) {
        for (java.lang.reflect.Method m : overridesInjector.getClass().getMethods()) {
            if (m.getName().equals("bind") && m.getParameterCount() == 2) {
                return m;
            }
        }
        return null;
    }

    @SuppressWarnings("java:S112") // Reflection methods throw many checked exception types
    private static Object handleFrustumProxy(Object proxy, java.lang.reflect.Method method, Object[] args) throws Exception {
        String name = method.getName();
        return switch (name) {
            case "getPriority" -> Integer.MAX_VALUE;
            case "update" -> {
                if (args != null && args.length == 3) {
                    currentMinY = (int) args[0];
                    currentMaxY = (int) args[1];
                    Object mat = args[2];
                    if (getValuesAsArrayMethod == null) {
                        getValuesAsArrayMethod = mat.getClass().getMethod("getValuesAsArray");
                    }
                    float[] vpArray = (float[]) getValuesAsArrayMethod.invoke(mat);
                    com.alexxiconify.rustmc.NativeBridge.updateRustFrustum(rustFrustumPtr, vpArray);
                }
                yield null;
            }
            case "intersects" -> {
                if (args != null && args.length == 4) {
                    int minX = (int) args[0];
                    int minZ = (int) args[1];
                    int width = (int) args[2];
                    yield com.alexxiconify.rustmc.NativeBridge.testRustFrustum(
                        rustFrustumPtr, minX, currentMinY, minZ,
                        (double) minX + width, currentMaxY, (double) minZ + width);
                }
                yield true; // default: visible
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "RustMC-DH-FrustumCuller";
            default -> null;
        };
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

    public static float[] computeRustAmbientOcclusionDirect(java.nio.ByteBuffer vertexData, int vertexCount) {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) {
            return new float[0];
        }
        if (vertexCount == 0) return new float[0];

        return com.alexxiconify.rustmc.NativeBridge.invokeComputeAmbientOcclusionDirect(vertexData, vertexCount);
    }
}