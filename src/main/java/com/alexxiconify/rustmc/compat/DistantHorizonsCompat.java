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
    private static final String DH_API_CLASS = "com.seibel.distanthorizons.api.DhApi";

    private DistantHorizonsCompat() {}

    public static void disableFade() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        try {
            Class<?> apiClass = Class.forName(DH_API_CLASS);
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

            Class<?> apiClass = Class.forName(DH_API_CLASS);
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

    private static float[] lastVpArray = null;

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
                    
                    if (lastVpArray == null || !java.util.Arrays.equals(lastVpArray, vpArray)) {
                        lastVpArray = vpArray != null ? vpArray.clone() : null;
                        com.alexxiconify.rustmc.NativeBridge.updateRustFrustum(rustFrustumPtr, vpArray);

                        // FOV+aspect-aware scale to prevent aggressive DH LOD clipping.
                        net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
                        double fov = mc2.options.getFov().getValue();
                        double aspect = mc2.getWindow().getFramebufferWidth()
                            / Math.max(1.0, mc2.getWindow().getFramebufferHeight());
                        // Ultrawide screens have narrower vertical FOV — boost scale slightly so
                        // tall DH sections at the horizontal edges are not incorrectly culled.
                        double aspectBoost = Math.max(1.0, aspect / (16.0 / 9.0));
                        double fovScale = 1.15 * (fov / 70.0) * Math.sqrt(aspectBoost);
                        fovScale = Math.clamp(fovScale, 0.8, 2.5);
                        com.alexxiconify.rustmc.NativeBridge.setRustFrustumFovScale(rustFrustumPtr, fovScale);
                    }
                }
                yield null;
            }
            case "intersects" -> {
                if (args != null && args.length == 4) {
                    int minX = (int) args[0];
                    int minZ = (int) args[1];
                    int maxX = (int) args[2];
                    int maxZ = (int) args[3];
                    // Native side-plane culling (hides vertical geometry below surface)
                    // We treat currentMinY as a rough surface proxy if it's below sea level
                    if (!com.alexxiconify.rustmc.NativeBridge.invokeDHCull(currentMinY, currentMaxY, 62.0)) {
                        yield false; 
                    }
                    // Use native margin support instead of manual inflation for better performance
                    // margin = 4.0 (base) + distance-based expansion
                    double margin = 4.0;

                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    net.minecraft.entity.Entity cam = client.getCameraEntity();
                    if (cam != null) {
                        double camX = cam.getX();
                        double camZ = cam.getZ();
                        // Use minX/minZ directly for distance check to avoid 'aggressive' center-based culling
                        double distSq = (minX - camX) * (minX - camX) + (minZ - camZ) * (minZ - camZ);
                        margin += Math.sqrt(distSq) * 0.08; // Increased expansion factor
                    }

                    yield com.alexxiconify.rustmc.NativeBridge.testRustFrustum(
                        rustFrustumPtr,
                        Math.min(minX, maxX), currentMinY, Math.min(minZ, maxZ),
                        Math.max(minX, maxX), currentMaxY, Math.max(minZ, maxZ),
                        margin);
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

    // ── LOD Loading Optimization ────────────────────────────────────────────

    /**
     * Hints the DH API to use a higher thread count for LOD generation/loading.
     * Called during init. DH's default thread count is conservative — on modern
     * CPUs we can afford more threads for disk I/O and LOD meshing.
     */
    public static void optimizeLodThreading() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        try {
            Class<?> apiClass = Class.forName(DH_API_CLASS);
            Object dhApi = apiClass.getField("Inst").get(null);
            Object configs = dhApi.getClass().getMethod("configs").invoke(dhApi);
            Object threading = configs.getClass().getMethod("threading").invoke(configs);

            int cores = Runtime.getRuntime().availableProcessors();
            int lodThreads = Math.max(2, cores / 2);
            setLodBuilderThreads(threading, lodThreads);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not optimize DH LOD threading ({})", e.getMessage());
        }
    }

    private static void setLodBuilderThreads(Object threading, int lodThreads) {
        try {
            java.lang.reflect.Method setThreadCount = threading.getClass().getMethod("setNumberOfLodBuilderThreads", int.class);
            setThreadCount.invoke(threading, lodThreads);
            RustMC.LOGGER.info("[Rust-MC] Set DH LOD builder threads to {}", lodThreads);
        } catch (NoSuchMethodException e) {
            RustMC.LOGGER.debug("[Rust-MC] DH API doesn't expose thread count setter.");
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not set DH LOD threads: {}", e.getMessage());
        }
    }

    /**
     * Pre-warms DH's LOD file cache for the current world on a virtual thread.
     * Called when connecting to a world/server to reduce initial LOD pop-in.
     */
    public static void prefetchLodData() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        Thread.ofVirtual().name("rustmc-dh-prefetch").start(() -> {
            try {
                // Trigger DH's internal data cache warmup by touching the API
                Class<?> apiClass = Class.forName(DH_API_CLASS);
                Object dhApi = apiClass.getField("Inst").get(null);
                // getWorldProxy() initializes DH's world-level caches
                dhApi.getClass().getMethod("getWorldProxy").invoke(dhApi);
                RustMC.LOGGER.debug("[Rust-MC] DH LOD data pre-fetched.");
            } catch (Exception e) {
                RustMC.LOGGER.debug("[Rust-MC] DH prefetch skipped: {}", e.getMessage());
            }
        });
    }

    /**
     * Offloads Distant Horizons "Ghost" lighting tasks to Rust.
     * DH uses a separate light engine for LODs that can be run in parallel
     * without touching vanilla world state.
     */
    public static void optimizeLighting(long[] lightTasks) {
        if (lightTasks == null || lightTasks.length == 0) return;
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) return;

        com.alexxiconify.rustmc.NativeBridge.propagateLightDH(lightTasks, lightTasks.length);
    }
}