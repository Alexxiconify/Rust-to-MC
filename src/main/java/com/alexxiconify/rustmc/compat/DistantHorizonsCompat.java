package com.alexxiconify.rustmc.compat;
import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;
//
 //  Reflection-based integration with Distant Horizons.
 //  API methods like {@link #computeRustAmbientOcclusion} and {@link #computeRustAmbientOcclusionDirect}
 //  are public API for DH vertex builders to offload AO to Rust's wgpu compute pipeline.
@SuppressWarnings("unused")
public class DistantHorizonsCompat {
    private static final String DH_MOD_ID = "distanthorizons";
    private static final String DH_API_CLASS = "com.seibel.distanthorizons.api.DhApi";
    private static final double DH_SURFACE_Y = 62.0;
    private static final double DH_AGGRESSIVE_MARGIN = -2.0;
    private DistantHorizonsCompat() {}
    private static long rustFrustumPtr = 0;
    private static int currentMinY = -64;
    private static int currentMaxY = 320;
    private static java.lang.reflect.Method getValuesAsArrayMethod = null;
    private static boolean hasLastCameraState = false;
    private static double lastCameraX = 0.0;
    private static double lastCameraY = 0.0;
    private static double lastCameraZ = 0.0;
    private static float lastCameraYaw = 0.0f;
    private static float lastCameraPitch = 0.0f;
    private static double lastFov = Double.NaN;
    private static double lastAspect = Double.NaN;
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
            case "update" -> handleFrustumUpdate(args);
            case "intersects" -> handleFrustumIntersects(args);
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "RustMC-DH-FrustumCuller";
            default -> null;
        };
    }

    @SuppressWarnings("java:S112")
    private static Object handleFrustumUpdate(Object[] args) throws Exception {
        if (args == null || args.length != 3) {
            return null;
        }
        currentMinY = (int) args[0];
        currentMaxY = (int) args[1];
        Object mat = args[2];
        if (getValuesAsArrayMethod == null) {
            getValuesAsArrayMethod = mat.getClass().getMethod("getValuesAsArray");
        }
        float[] vpArray = (float[]) getValuesAsArrayMethod.invoke(mat);
        if (shouldRefreshFrustum(vpArray)) {
            lastVpArray = vpArray != null ? vpArray.clone() : null;
            com.alexxiconify.rustmc.NativeBridge.updateRustFrustum(rustFrustumPtr, vpArray);
        }
        return null;
    }

    private static boolean shouldRefreshFrustum(float[] vpArray) {
        net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
        boolean matrixChanged = lastVpArray == null || !java.util.Arrays.equals(lastVpArray, vpArray);
        if (mc2 == null) {
            return matrixChanged;
        }
        net.minecraft.entity.Entity camera = mc2.getCameraEntity();
        if (camera == null) {
            return matrixChanged;
        }

        double cx = camera.getX();
        double cy = camera.getY();
        double cz = camera.getZ();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double fov = mc2.options.getFov().getValue();
        double aspect = mc2.getWindow().getFramebufferWidth() / Math.max(1.0, mc2.getWindow().getFramebufferHeight());

        boolean moved = true;
        boolean rotated = true;
        boolean opticsChanged = true;
        if (hasLastCameraState) {
            double dx = cx - lastCameraX;
            double dy = cy - lastCameraY;
            double dz = cz - lastCameraZ;
            moved = (dx * dx + dy * dy + dz * dz) > 1.0e-6;
            rotated = Math.abs(yaw - lastCameraYaw) > 1.0e-4f || Math.abs(pitch - lastCameraPitch) > 1.0e-4f;
            opticsChanged = Math.abs(fov - lastFov) > 1.0e-4 || Math.abs(aspect - lastAspect) > 1.0e-4;
        }

        lastCameraX = cx;
        lastCameraY = cy;
        lastCameraZ = cz;
        lastCameraYaw = yaw;
        lastCameraPitch = pitch;
        lastFov = fov;
        lastAspect = aspect;
        hasLastCameraState = true;

        // Refresh when any camera state changes, not only when position+matrix both changed.
        return matrixChanged || moved || rotated || opticsChanged;
    }
    private static Object handleFrustumIntersects(Object[] args) {
        if (args == null || args.length < 4) {
            return true;
        }
        int minX = (int) args[0];
        int minZ = (int) args[1];
        int maxX = (int) args[2];
        int maxZ = (int) args[3];
        return com.alexxiconify.rustmc.NativeBridge.cullDistantHorizonsSection(
            rustFrustumPtr,
            Math.min(minX, maxX), currentMinY, Math.min(minZ, maxZ),
            Math.max(minX, maxX), currentMaxY, Math.max(minZ, maxZ),
            DH_SURFACE_Y,
            DH_AGGRESSIVE_MARGIN
        );
    }
    //
     // Public API hook for DH vertex builders or shaders to offload Ambient Occlusion
     // calculations to the Rust wgpu Compute Shader pipeline.
     // Expects vertices formatted as contiguous floats: [posX, posY, posZ, pad, normX, normY, normZ, pad].
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
    //
     // Hints the DH API to use a higher thread count for LOD generation/loading.
     // Called during init. DH's default thread count is conservative — on modern
     // CPUs we can afford more threads for disk I/O and LOD meshing.
    public static void optimizeLodThreading() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        try {
            Class<?> apiClass = Class.forName(DH_API_CLASS);
            Object dhApi = apiClass.getField("Inst").get(null);
            Object configs = dhApi.getClass().getMethod("configs").invoke(dhApi);
            Object threading = configs.getClass().getMethod("threading").invoke(configs);
            int cores = Runtime.getRuntime().availableProcessors();
            int lodThreads = Math.max(2, cores - 1);
            setDhThreadingConfig(threading, "setNumberOfLodBuilderThreads", lodThreads, "LOD builder");
            setDhThreadingConfig(threading, "setNumberOfWorldGenerationThreads", Math.max(1, cores / 2), "world-gen");
            setDhThreadingConfig(threading, "setNumberOfFileSaveThreads", Math.max(2, cores / 2), "file-save");
            Object overrides = dhApi.getClass().getMethod("overrides").invoke(dhApi);
            setDhGenerationPriority(overrides);
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not optimize DH LOD threading ({})", e.getMessage());
        }
    }
    private static void setDhGenerationPriority(Object overrides) {
        try {
            overrides.getClass().getMethod("setLodBuilderPriority", int.class).invoke(overrides, 0);
            RustMC.LOGGER.info("[Rust-MC] DH LOD builder priority set to HIGH.");
        } catch (NoSuchMethodException ignored) {
            // DH version may not expose this setting.
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to set DH priority: {}", e.getMessage());
        }
    }
    private static void setDhThreadingConfig(Object threading, String method, int count, String label) {
        try {
            threading.getClass().getMethod(method, int.class).invoke(threading, count);
            RustMC.LOGGER.info("[Rust-MC] DH {} threads = {}", label, count);
        } catch (NoSuchMethodException ignored) {
            // DH version may not expose this setter.
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not set DH {} threads: {}", label, e.getMessage());
        }
    }
    //
     // Pre-warms DH's LOD file cache for the current world on a virtual thread.
     // Called when connecting to a world/server to reduce initial LOD pop-in.
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
    //
     // Offloads Distant Horizons "Ghost" lighting tasks to Rust.
     // DH uses a separate light engine for LODs that can be run in parallel
     // without touching vanilla world state.
    public static void optimizeLighting(long[] lightTasks) {
        if (lightTasks == null || lightTasks.length == 0) return;
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) return;
        com.alexxiconify.rustmc.NativeBridge.propagateLightDH(lightTasks, lightTasks.length);
    }
    //
     // Offloads DH LOD meshing to Rust GPU path when detail level is high-value for batching.
    public static int[] generateGpuLod(int[] blocks, int chunkX, int chunkZ, int detail) {
        if (!com.alexxiconify.rustmc.NativeBridge.isReady() || detail > 2 || blocks == null) {
            return new int[0];
        }
        return com.alexxiconify.rustmc.NativeBridge.generateLodMeshGpu(blocks, chunkX, chunkZ, detail);
    }
}