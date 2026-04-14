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
    private static final double DH_SURFACE_Y = 54.0;
    private static final double DH_AGGRESSIVE_MARGIN = -2.0;
    private DistantHorizonsCompat() {}
    private static long rustFrustumPtr = 0;
    private static int currentMinY = -64;
    private static int currentMaxY = 320;
    private static boolean frustumInitialized = false;
    private static java.lang.reflect.Method getValuesAsArrayMethod = null;
    private static boolean hasLastCameraState = false;
    private static double lastCameraX = 0.0;
    private static double lastCameraY = 0.0;
    private static double lastCameraZ = 0.0;
    private static float lastCameraYaw = 0.0f;
    private static float lastCameraPitch = 0.0f;
    private static double lastFov = Double.NaN;
    private static double lastAspect = Double.NaN;
    private static final int VP_MATRIX_SIZE = 16;
    private static final float[] lastVpArray = new float[VP_MATRIX_SIZE];
    private static boolean hasLastVpArray = false;

    private enum ProxyMethodKind {
        PRIORITY,
        UPDATE,
        INTERSECT,
        EQUALS,
        HASHCODE,
        TOSTRING,
        UNKNOWN
    }

    private record BoundsFields(java.lang.reflect.Field minX,
                                java.lang.reflect.Field minY,
                                java.lang.reflect.Field minZ,
                                java.lang.reflect.Field maxX,
                                java.lang.reflect.Field maxY,
                                java.lang.reflect.Field maxZ) {
    }

    private static final java.util.concurrent.ConcurrentHashMap<java.lang.reflect.Method, ProxyMethodKind> PROXY_METHOD_KIND_CACHE =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, BoundsFields> BOUNDS_FIELD_CACHE =
        new java.util.concurrent.ConcurrentHashMap<>();
    @SuppressWarnings("java:S3776")
    public static void registerFrustumCuller() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID) || !com.alexxiconify.rustmc.NativeBridge.isReady()) return;
        try {
            rustFrustumPtr = com.alexxiconify.rustmc.NativeBridge.createRustFrustum();
            if (rustFrustumPtr == 0) return;
            hasLastVpArray = false;
            hasLastCameraState = false;
            frustumInitialized = false;
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
            frustumInitialized = false;
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
        ProxyMethodKind kind = PROXY_METHOD_KIND_CACHE.computeIfAbsent(method, DistantHorizonsCompat::resolveProxyMethodKind);
        return switch (kind) {
            case PRIORITY -> Integer.MAX_VALUE;
            case UPDATE -> handleFrustumUpdate(args);
            case INTERSECT -> handleFrustumIntersects(args);
            case EQUALS -> proxy == args[0];
            case HASHCODE -> System.identityHashCode(proxy);
            case TOSTRING -> "RustMC-DH-FrustumCuller";
            default -> null;
        };
    }

    private static ProxyMethodKind resolveProxyMethodKind(java.lang.reflect.Method method) {
        String name = method.getName().toLowerCase(java.util.Locale.ROOT);
        switch (name) {
            case "getpriority":
                return ProxyMethodKind.PRIORITY;
            case "equals":
                return ProxyMethodKind.EQUALS;
            case "hashcode":
                return ProxyMethodKind.HASHCODE;
            case "tostring":
                return ProxyMethodKind.TOSTRING;
            default:
                break;
        }
        if (name.contains("update")) {
            return ProxyMethodKind.UPDATE;
        }
        if (name.contains("intersect") || name.contains("visible") || name.contains("contain")) {
            return ProxyMethodKind.INTERSECT;
        }
        return ProxyMethodKind.UNKNOWN;
    }

    @SuppressWarnings("java:S112")
    private static Object handleFrustumUpdate(Object[] args) throws Exception {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args.length >= 3 && args[0] instanceof Number minY && args[1] instanceof Number maxY) {
            currentMinY = minY.intValue();
            currentMaxY = maxY.intValue();
        }
        Object mat = args[args.length - 1];
        if (getValuesAsArrayMethod == null) {
            getValuesAsArrayMethod = mat.getClass().getMethod("getValuesAsArray");
        }
        float[] vpArray = (float[]) getValuesAsArrayMethod.invoke(mat);
        if (!frustumInitialized || shouldRefreshFrustum(vpArray)) {
            cacheVpArray(vpArray);
            frustumInitialized = com.alexxiconify.rustmc.NativeBridge.updateRustFrustumTracked(rustFrustumPtr, vpArray);
        }
        return null;
    }

    private static void cacheVpArray(float[] vpArray) {
        if (vpArray == null || vpArray.length < VP_MATRIX_SIZE) {
            hasLastVpArray = false;
            return;
        }
        System.arraycopy(vpArray, 0, lastVpArray, 0, VP_MATRIX_SIZE);
        hasLastVpArray = true;
    }

    private static boolean matrixChanged(float[] vpArray) {
        if (vpArray == null || vpArray.length < VP_MATRIX_SIZE) {
            return hasLastVpArray;
        }
        if (!hasLastVpArray) {
            return true;
        }
        for (int i = 0; i < VP_MATRIX_SIZE; i++) {
            if (lastVpArray[i] != vpArray[i]) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldRefreshFrustum(float[] vpArray) {
        net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
        boolean matrixChanged = matrixChanged(vpArray);
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
        // Keep fallback behavior: before first successful matrix update, treat sections as visible.
        if (!frustumInitialized || rustFrustumPtr == 0) {
            return true;
        }
        if (args == null || args.length == 0) {
            return true;
        }
        double minX;
        double minY;
        double minZ;
        double maxX;
        double maxY;
        double maxZ;
        if (args.length == 1 && args[0] != null) {
            Object box = args[0];
            try {
                BoundsFields fields = BOUNDS_FIELD_CACHE.computeIfAbsent(box.getClass(), DistantHorizonsCompat::resolveBoundsFields);
                minX = fields.minX().getDouble(box);
                minY = fields.minY().getDouble(box);
                minZ = fields.minZ().getDouble(box);
                maxX = fields.maxX().getDouble(box);
                maxY = fields.maxY().getDouble(box);
                maxZ = fields.maxZ().getDouble(box);
            } catch (Exception e) {
                return true;
            }
        } else if (args.length >= 6
            && args[0] instanceof Number minXArg
            && args[1] instanceof Number minYArg
            && args[2] instanceof Number minZArg
            && args[3] instanceof Number maxXArg
            && args[4] instanceof Number maxYArg
            && args[5] instanceof Number maxZArg) {
            minX = minXArg.doubleValue();
            minY = minYArg.doubleValue();
            minZ = minZArg.doubleValue();
            maxX = maxXArg.doubleValue();
            maxY = maxYArg.doubleValue();
            maxZ = maxZArg.doubleValue();
        } else if (args.length >= 4
            && args[0] instanceof Number minXArg
            && args[1] instanceof Number minZArg
            && args[2] instanceof Number maxXArg
            && args[3] instanceof Number maxZArg) {
            minX = minXArg.doubleValue();
            minY = currentMinY;
            minZ = minZArg.doubleValue();
            maxX = maxXArg.doubleValue();
            maxY = currentMaxY;
            maxZ = maxZArg.doubleValue();
        } else {
            return true;
        }
        return cullDhSectionInAnySpace(
            Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
            Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ)
        );
    }

    private static BoundsFields resolveBoundsFields(Class<?> cls) {
        try {
            return new BoundsFields(
                cls.getField("minX"),
                cls.getField("minY"),
                cls.getField("minZ"),
                cls.getField("maxX"),
                cls.getField("maxY"),
                cls.getField("maxZ")
            );
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not resolve DH bounds fields for " + cls.getName(), e);
        }
    }

    // DH versions may feed absolute world AABBs or camera-relative AABBs. We test
    // absolute first, then signed camera-relative variants to avoid direction-space mismatches.
    private static boolean cullDhSectionInAnySpace(double minX, double minY, double minZ,
                                                   double maxX, double maxY, double maxZ) {
        String mode = com.alexxiconify.rustmc.RustMC.CONFIG.getDhCullingSpaceMode();
        if (com.alexxiconify.rustmc.config.RustMCConfig.DH_CULLING_SPACE_ABSOLUTE.equals(mode)) {
            return testAbsoluteSpace(minX, minY, minZ, maxX, maxY, maxZ);
        }
        if (com.alexxiconify.rustmc.config.RustMCConfig.DH_CULLING_SPACE_PLUS_CAMERA.equals(mode)) {
            return testCameraRelativeSpace(minX, minY, minZ, maxX, maxY, maxZ, 1.0);
        }
        if (com.alexxiconify.rustmc.config.RustMCConfig.DH_CULLING_SPACE_MINUS_CAMERA.equals(mode)) {
            return testCameraRelativeSpace(minX, minY, minZ, maxX, maxY, maxZ, -1.0);
        }
        boolean visibleAbsolute = testAbsoluteSpace(minX, minY, minZ, maxX, maxY, maxZ);
        if (visibleAbsolute) {
            return true;
        }
        boolean visibleCameraPlus = testCameraRelativeSpace(minX, minY, minZ, maxX, maxY, maxZ, 1.0);
        if (visibleCameraPlus) {
            return true;
        }
        return testCameraRelativeSpace(minX, minY, minZ, maxX, maxY, maxZ, -1.0);
    }

    private static boolean testAbsoluteSpace(double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ) {
        return com.alexxiconify.rustmc.NativeBridge.cullDistantHorizonsSection(
            rustFrustumPtr,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            DH_SURFACE_Y,
            DH_AGGRESSIVE_MARGIN
        );
    }

    private static boolean testCameraRelativeSpace(double minX, double minY, double minZ,
                                                   double maxX, double maxY, double maxZ,
                                                   double cameraSign) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        double cx = mc.player.getX() * cameraSign;
        double cy = mc.player.getY() * cameraSign;
        double cz = mc.player.getZ() * cameraSign;
        return com.alexxiconify.rustmc.NativeBridge.cullDistantHorizonsSection(
            rustFrustumPtr,
            minX + cx, minY + cy, minZ + cz,
            maxX + cx, maxY + cy, maxZ + cz,
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
     // Pre-warms DH's LOD file cache for the current world on a platform daemon thread.
     // Called when connecting to a world/server to reduce initial LOD pop-in.
    public static void prefetchLodData() {
        if (!FabricLoader.getInstance().isModLoaded(DH_MOD_ID)) return;
        Thread.ofPlatform().daemon(true).name("rustmc-dh-prefetch").start(() -> {
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
        if (!com.alexxiconify.rustmc.NativeBridge.isReady() || detail > 2 || blocks == null || blocks.length == 0) {
            return new int[0];
        }
        return com.alexxiconify.rustmc.NativeBridge.generateLodMeshGpu(blocks, chunkX, chunkZ, detail);
    }
}