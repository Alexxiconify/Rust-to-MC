package com.alexxiconify.rustmc.compat;
import com.alexxiconify.rustmc.RustMC;
import com.alexxiconify.rustmc.NativeBridge;
import net.fabricmc.loader.api.FabricLoader;
public class DistantHorizonsCompat {
    private static final String DH_MOD_ID = "distanthorizons";
    private static final String DH_API_CLASS = "com.seibel.distanthorizons.api.DhApi";
    private static final String MATRIX_VALUES_AS_ARRAY_METHOD = "getValuesAsArray";
    private static final String MATRIX_TO_ARRAY_METHOD = "toArray";
    private static final double DH_SURFACE_Y = 54.0;
    private static final double COORD_CHANGE_THRESHOLD = 1.0e-6;
    private static final float ROTATION_CHANGE_THRESHOLD = 1.0e-4f;
    private static final double OPTICS_CHANGE_THRESHOLD = 1.0e-4;
    private static final double MATRIX_QUANTIZATION_SCALE = 1_000_000.0;
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
 private static long lastVpFingerprint = 0L;
    private static boolean hasLastVpFingerprint = false;
    private static java.lang.reflect.Method matrixToArrayMethod = null;
    private static java.lang.reflect.Method matrixGetMethod = null;
    private static double lastFov = Double.NaN;
    private static Object dhOverridesInjector = null;
    private static java.lang.reflect.Method dhBindMethod = null;
    private static Class<?> dhCullingFrustumClass = null;
    private static Object dhProxyInstance = null;
    private static long lastRebindNanos = 0L;
 private static final long REBIND_INTERVAL_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    // Bounded cache with max size to prevent unbounded growth on large maps
    private static final java.util.concurrent.ConcurrentHashMap<Long, Boolean> VISIBILITY_CACHE = new java.util.concurrent.ConcurrentHashMap<>(1024) {
        private static final int MAX_SIZE = 8192;
        @Override
        public Boolean putIfAbsent(@org.jetbrains.annotations.NotNull Long key, @org.jetbrains.annotations.NotNull Boolean value) {
            if (size() > MAX_SIZE) {
                clear();
            }
            return super.putIfAbsent(key, value);
        }
    };
    private static final float[] SHADOW_PLANES = new float[24]; // 6 planes * 4 components
    public static String getLastRefreshReason() {
     return "INIT"; }
    public static boolean isFrustumInitialized() { return frustumInitialized; }
    public static double getLastCameraMoveSq() {
     return 0.0; }
    private static boolean isDhMissing() {return !FabricLoader.getInstance().isModLoaded(DH_MOD_ID);}
    private static boolean isDhNativeReady() {return isDhMissing() || !NativeBridge.isReady();}
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
    private record SectionBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }
    private static final java.util.concurrent.ConcurrentHashMap<java.lang.reflect.Method, ProxyMethodKind> PROXY_METHOD_KIND_CACHE =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, BoundsFields> BOUNDS_FIELD_CACHE =
        new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicBoolean dhApiShapeLogged = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final java.util.concurrent.atomic.AtomicInteger unknownProxyMethodCount = new java.util.concurrent.atomic.AtomicInteger(0);
    @SuppressWarnings("java:S3776")
    public static void registerFrustumCuller() {
        if (isDhNativeReady()) return;
        try {
            rustFrustumPtr = NativeBridge.createRustFrustum();
            if (rustFrustumPtr == 0) return;
            hasLastCameraState = false;
            frustumInitialized = false;
            hasLastVpFingerprint = false;
            Class<?> apiClass = Class.forName(DH_API_CLASS);
            dhOverridesInjector = apiClass.getField("overrides").get(null);
            dhBindMethod = findBindMethod(dhOverridesInjector);
            dhCullingFrustumClass = Class.forName("com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiCullingFrustum");
            logDhApiMethodShapes(dhCullingFrustumClass);
            dhProxyInstance = java.lang.reflect.Proxy.newProxyInstance(
                DistantHorizonsCompat.class.getClassLoader(),
                new Class<?>[]{dhCullingFrustumClass},
                DistantHorizonsCompat::handleFrustumProxy
            );
            if (dhBindMethod != null) {
                dhBindMethod.invoke(dhOverridesInjector, dhCullingFrustumClass, dhProxyInstance);
                lastRebindNanos = System.nanoTime();
                RustMC.LOGGER.info("[Rust-MC] Registered Rust frustum culler with Distant Horizons.");
            }
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Could not register DH frustum culler ({}), skipping.", e.getMessage());
            if (rustFrustumPtr != 0) {
                NativeBridge.destroyRustFrustum(rustFrustumPtr);
                rustFrustumPtr = 0;
            }
            frustumInitialized = false;
            dhOverridesInjector = null;
            dhBindMethod = null;
            dhCullingFrustumClass = null;
            dhProxyInstance = null;
            lastRebindNanos = 0L;
        }
    }

    // Rebind periodically to keep Rust-MC culler ownership if another compat replaces binding.
    private static void ensureFrustumCullerBound() {
        if (dhBindMethod == null || dhOverridesInjector == null || dhCullingFrustumClass == null || dhProxyInstance == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastRebindNanos < REBIND_INTERVAL_NANOS) {
            return;
        }
        try {
            dhBindMethod.invoke(dhOverridesInjector, dhCullingFrustumClass, dhProxyInstance);
            lastRebindNanos = now;
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] DH culler rebind skipped: {}", e.getMessage());
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
    @SuppressWarnings("java:S3824")
    private static Object handleFrustumProxy(Object proxy, java.lang.reflect.Method method, Object[] args) throws ReflectiveOperationException {
        ProxyMethodKind kind = PROXY_METHOD_KIND_CACHE.computeIfAbsent(method, DistantHorizonsCompat::resolveProxyMethodKind);
        return switch (kind) {
            case PRIORITY -> Integer.MAX_VALUE;
            case UPDATE -> handleFrustumUpdate(args);
            case INTERSECT -> handleFrustumIntersects(args);
            case EQUALS -> args != null && args.length == 1 && proxy == args[0];
            case HASHCODE -> System.identityHashCode(proxy);
            case TOSTRING -> "RustMC-DH-FrustumCuller";
            default -> {
                int misses = unknownProxyMethodCount.incrementAndGet();
                if (misses <= 3) {
                    RustMC.LOGGER.debug("[Rust-MC] DH proxy method unresolved: {} (params={})", method.getName(), method.getParameterCount());
                }
                yield null;
            }
        };
    }

    private static void logDhApiMethodShapes(Class<?> cullingFrustumClass) {
        if (!dhApiShapeLogged.compareAndSet(false, true)) {
            return;
        }
        try {
            java.lang.reflect.Method[] methods = cullingFrustumClass.getMethods();
            RustMC.LOGGER.debug("[Rust-MC] DH culling API methods detected: {}", methods.length);
            for (java.lang.reflect.Method m : methods) {
                String params = java.util.Arrays.stream(m.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(java.util.stream.Collectors.joining(","));
                RustMC.LOGGER.debug("[Rust-MC]   {}({}) -> {}", m.getName(), params, m.getReturnType().getSimpleName());
            }
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to introspect DH culling API: {}", e.getMessage());
        }
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

    private static Object handleFrustumUpdate(Object[] args) throws ReflectiveOperationException {
        ensureFrustumCullerBound();
        if (args == null || args.length == 0) return null;
        if (args.length >= 3 && args[0] instanceof Number minY && args[1] instanceof Number maxY) {
            currentMinY = minY.intValue();
            currentMaxY = maxY.intValue();
        }
        Object mat = args[args.length - 1];
        float[] vpArray = extractMatrixValues(mat);
        if (vpArray.length < 16) return null;
        if (!frustumInitialized || shouldRefreshFrustum(vpArray)) {
            float[] vpSnapshot = java.util.Arrays.copyOf(vpArray, vpArray.length);
            if (NativeBridge.updateRustFrustumTracked(rustFrustumPtr, vpSnapshot)) {
                frustumInitialized = true;
                VISIBILITY_CACHE.clear();
                updateShadowPlanes(vpSnapshot);
            }
        }
        return null;
    }

    private static float[] extractMatrixValues(Object mat) throws ReflectiveOperationException {
        if (mat == null) return new float[0];
        float[] values = tryInvokeNoArgFloatArray(mat, MATRIX_VALUES_AS_ARRAY_METHOD);
        if (values.length >= 16) return values;
        values = tryInvokeNoArgFloatArray(mat, MATRIX_TO_ARRAY_METHOD);
        if (values.length >= 16) return values;
        return tryInvokeMatrixGet(mat);
    }

    private static float[] tryInvokeNoArgFloatArray(Object mat, String methodName) throws ReflectiveOperationException {
        java.lang.reflect.Method method = switch (methodName) {
            case MATRIX_VALUES_AS_ARRAY_METHOD -> getValuesAsArrayMethod;
            case MATRIX_TO_ARRAY_METHOD -> matrixToArrayMethod;
            default -> null;
        };
        if (method == null) {
            method = resolveNoArgMethod(mat, methodName);
            if (method == null) return new float[0];
            if (MATRIX_VALUES_AS_ARRAY_METHOD.equals(methodName)) getValuesAsArrayMethod = method;
            else if (MATRIX_TO_ARRAY_METHOD.equals(methodName)) matrixToArrayMethod = method;
        }
        Object out = method.invoke(mat);
        return (out instanceof float[] arr && arr.length >= 16) ? arr : new float[0];
    }

    private static java.lang.reflect.Method resolveNoArgMethod(Object mat, String methodName) {
        try {
            return mat.getClass().getMethod(methodName);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static float[] tryInvokeMatrixGet(Object mat) throws ReflectiveOperationException {
        if (matrixGetMethod == null) {
            try {
                matrixGetMethod = mat.getClass().getMethod("get", float[].class);
            } catch (NoSuchMethodException ignored) {
                return new float[0];
            }
        }
        float[] out = new float[16];
        Object ret = matrixGetMethod.invoke( mat, ( Object ) out );
        if (ret instanceof float[] arr && arr.length >= 16) {
            return arr;
        }
        return out;
    }

    private static long fingerprintMatrix(float[] vpArray) {
        int len = Math.min(16, vpArray.length);
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < len; i++) {
            int quantized = Math.round(vpArray[i] * (float) MATRIX_QUANTIZATION_SCALE);
            hash ^= (quantized * 0x9E3779B9L) ^ i;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static boolean shouldRefreshFrustum(float[] vpArray) {
        net.minecraft.client.MinecraftClient mc2 = net.minecraft.client.MinecraftClient.getInstance();
        if (mc2 == null || mc2.world == null) {
            return false;
        }
        var camera = mc2.gameRenderer.getCamera();
        var pos = camera.getCameraPos();
        double cx = pos.x;
        double cy = pos.y;
        double cz = pos.z;
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double fov = mc2.options.getFov().getValue();

        boolean moved = !hasLastCameraState || Math.hypot(Math.hypot(cx - lastCameraX, cy - lastCameraY), cz - lastCameraZ) > COORD_CHANGE_THRESHOLD;
        boolean rotated = !hasLastCameraState || Math.abs(yaw - lastCameraYaw) > ROTATION_CHANGE_THRESHOLD || Math.abs(pitch - lastCameraPitch) > ROTATION_CHANGE_THRESHOLD;
        boolean opticsChanged = Double.isNaN(lastFov) || Math.abs(fov - lastFov) > OPTICS_CHANGE_THRESHOLD;

        lastCameraX = cx;
        lastCameraY = cy;
        lastCameraZ = cz;
        lastCameraYaw = yaw;
        lastCameraPitch = pitch;
        lastFov = fov;
        hasLastCameraState = true;

        // Only compute fingerprint if camera/optics changed
        long vpFingerprint = fingerprintMatrix(vpArray);
        boolean projectionChanged = !hasLastVpFingerprint || (vpFingerprint != lastVpFingerprint);

        if (projectionChanged) {
            lastVpFingerprint = vpFingerprint;
            hasLastVpFingerprint = true;
        }

        return moved || rotated || opticsChanged || projectionChanged;
    }
    private static Object handleFrustumIntersects(Object[] args) {
        ensureFrustumCullerBound();
        // Treat sections as visible before first successful matrix update.
        if (!frustumInitialized || rustFrustumPtr == 0) {
            return true;
        }
        if (args == null || args.length == 0) {
            return true;
        }
        return resolveIntersectBounds(args)
                .map(bounds -> {
                    long key = hashBounds(bounds);
                    return VISIBILITY_CACHE.computeIfAbsent(key, k -> {
                        double minX = Math.min(bounds.minX(), bounds.maxX());
                        double minY = Math.min(bounds.minY(), bounds.maxY());
                        double minZ = Math.min(bounds.minZ(), bounds.maxZ());
                        double maxX = Math.max(bounds.minX(), bounds.maxX());
                        double maxY = Math.max(bounds.minY(), bounds.maxY());
                        double maxZ = Math.max(bounds.minZ(), bounds.maxZ());

                        if (isOutsideShadowFrustum(minX, minY, minZ, maxX, maxY, maxZ)) {
                            return false;
                        }

                        return cullDhSectionInAnySpace(minX, minY, minZ, maxX, maxY, maxZ);
                    });
                })
                .orElse(true);
    }

    private static long hashBounds(SectionBounds b) {
        long h = Double.doubleToRawLongBits(b.minX());
        h = h * 31 + Double.doubleToRawLongBits(b.minY());
        h = h * 31 + Double.doubleToRawLongBits(b.minZ());
        return h;
    }

    private static java.util.Optional<SectionBounds> resolveIntersectBounds(Object[] args) {
        if (args.length == 1 && args[0] != null) {
            return resolveSingleArgBounds(args[0]);
        }
        if (args.length >= 6) {
            return resolveSixArgBounds(args);
        }
        if (args.length >= 4) {
            return resolveFourArgBounds(args);
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<SectionBounds> resolveSixArgBounds(Object[] args) {
        if (args[0] instanceof Number minX && args[1] instanceof Number minY && args[2] instanceof Number minZ &&
            args[3] instanceof Number maxX && args[4] instanceof Number maxY && args[5] instanceof Number maxZ) {
            return java.util.Optional.of(new SectionBounds(
                minX.doubleValue(), minY.doubleValue(), minZ.doubleValue(),
                maxX.doubleValue(), maxY.doubleValue(), maxZ.doubleValue()
            ));
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<SectionBounds> resolveFourArgBounds(Object[] args) {
        if (args[0] instanceof Number minX && args[1] instanceof Number minZ &&
            args[2] instanceof Number maxX && args[3] instanceof Number maxZ) {
            return java.util.Optional.of(new SectionBounds(
                minX.doubleValue(), currentMinY, minZ.doubleValue(),
                maxX.doubleValue(), currentMaxY, maxZ.doubleValue()
            ));
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<SectionBounds> resolveSingleArgBounds(Object arg) {
        // Fast path: arrays of numbers (most common)
        if (arg instanceof double[] vals && vals.length >= 6) {
            return java.util.Optional.of(new SectionBounds(vals[0], vals[1], vals[2], vals[3], vals[4], vals[5]));
        }
        if (arg instanceof float[] vals && vals.length >= 6) {
            return java.util.Optional.of(new SectionBounds(vals[0], vals[1], vals[2], vals[3], vals[4], vals[5]));
        }
        if (arg instanceof Number[] vals && vals.length >= 6) {
            return java.util.Optional.of(new SectionBounds(
                vals[0].doubleValue(), vals[1].doubleValue(), vals[2].doubleValue(),
                vals[3].doubleValue(), vals[4].doubleValue(), vals[5].doubleValue()
            ));
        }

        // Cache field access only on first call per class
        BoundsFields fields;
        try {
            fields = BOUNDS_FIELD_CACHE.computeIfAbsent(arg.getClass(), DistantHorizonsCompat::resolveBoundsFields);
            return java.util.Optional.of(new SectionBounds(
                fields.minX().getDouble(arg), fields.minY().getDouble(arg), fields.minZ().getDouble(arg),
                fields.maxX().getDouble(arg), fields.maxY().getDouble(arg), fields.maxZ().getDouble(arg)
            ));
        } catch (RuntimeException | IllegalAccessException ignored) {
            return java.util.Optional.empty();
        }
    }

    // Bounds field resolution streamlined; no separate getter fallback needed

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

    // Force single camera-minus transform path (best-performing mode).
    private static boolean cullDhSectionInAnySpace(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return testCameraMinusSpace(minX, minY, minZ, maxX, maxY, maxZ);
    }


    private static boolean testCameraMinusSpace(double minX, double minY, double minZ,
                                                double maxX, double maxY, double maxZ) {
        if (!hasLastCameraState || rustFrustumPtr == 0) {
            return true;
        }
        return NativeBridge.cullDistantHorizonsSection(
            rustFrustumPtr, minX, minY, minZ, maxX, maxY, maxZ,
            DH_SURFACE_Y, 0.0, RustMC.CONFIG.isEnableDhCaveCulling()
        );
    }


    //
      // Public API hook for DH vertex builders to offload Ambient Occlusion.
    public static float[] computeRustAmbientOcclusion(float[] vertexData) {
        if (isDhNativeReady()) {
            return new float[0];
        }
        int vertexCount = vertexData.length / 8; // 8 floats per vertex struct in WGSL
        if (vertexCount == 0) return new float[0];
        return NativeBridge.invokeComputeAmbientOcclusion(vertexData, vertexCount);
    }
    public static float[] computeRustAmbientOcclusionDirect(java.nio.ByteBuffer vertexData, int vertexCount) {
        if (isDhNativeReady()) {
            return new float[0];
        }
        if (vertexCount == 0) return new float[0];
        return NativeBridge.invokeComputeAmbientOcclusionDirect(vertexData, vertexCount);
    }
    // ── LOD Loading Optimization ────────────────────────────────────────────
    //
      // Hints DH API to use higher thread count for LOD generation.
    public static void optimizeLodThreading() {
        if (isDhMissing()) return;
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
      // Pre-warms DH LOD file cache for current world.
    public static void prefetchLodData() {
        if (isDhMissing()) return;
        Thread.ofPlatform().daemon(true).name("rustmc-dh-prefetch").start(() -> {
            try {
                // Trigger DH's internal data cache warmup by touching the API
                Class<?> apiClass = Class.forName(DH_API_CLASS);
                Object dhApi = apiClass.getField("Inst").get(null);
                // getWorldProxy() initializes cache.
                dhApi.getClass().getMethod("getWorldProxy").invoke(dhApi);
                RustMC.LOGGER.debug("[Rust-MC] DH LOD data pre-fetched.");
            } catch (Exception e) {
                RustMC.LOGGER.debug("[Rust-MC] DH prefetch skipped: {}", e.getMessage());
            }
        });
    }
    //
      // Offloads Distant Horizons "Ghost" lighting tasks to Rust.
    public static void optimizeLighting(long[] lightTasks) {
        if (lightTasks == null || lightTasks.length == 0) return;
        if ( isDhNativeReady ( ) ) return;
        // Never mutate DH/user cache task buffers in native relight paths.
        long[] taskSnapshot = java.util.Arrays.copyOf(lightTasks, lightTasks.length);
        NativeBridge.propagateLightDH(taskSnapshot, taskSnapshot.length);
    }
    //
     // Offloads DH LOD meshing to Rust GPU path when detail level is high-value for batching.
    public static int[] generateGpuLod(int[] blocks, int chunkX, int chunkZ, int detail) {
        int[] blockSnapshot = java.util.Arrays.copyOf(blocks, blocks.length);
        return NativeBridge.generateLodMeshGpu(blockSnapshot, chunkX, chunkZ, detail);
    }

    private static void updateShadowPlanes(float[] vp) {
        // Compute 6 planes in fewer loops + fused magnitude calc
        float vp3 = vp[3];
        float vp7 = vp[7];
        float vp11 = vp[11];
        float vp15 = vp[15];

        // Planes 0,1 (left/right, X)
        SHADOW_PLANES[0] = vp3 + vp[0]; SHADOW_PLANES[1] = vp7 + vp[4]; SHADOW_PLANES[2] = vp11 + vp[8];  SHADOW_PLANES[3] = vp15 + vp[12];
        SHADOW_PLANES[4] = vp3 - vp[0]; SHADOW_PLANES[5] = vp7 - vp[4]; SHADOW_PLANES[6] = vp11 - vp[8];  SHADOW_PLANES[7] = vp15 - vp[12];

        // Planes 2,3 (bottom/top, Y)
        SHADOW_PLANES[8] = vp3 + vp[1]; SHADOW_PLANES[9] = vp7 + vp[5]; SHADOW_PLANES[10] = vp11 + vp[9]; SHADOW_PLANES[11] = vp15 + vp[13];
        SHADOW_PLANES[12]= vp3 - vp[1]; SHADOW_PLANES[13]= vp7 - vp[5]; SHADOW_PLANES[14]= vp11 - vp[9]; SHADOW_PLANES[15]= vp15 - vp[13];

        // Planes 4,5 (near/far, Z)
        SHADOW_PLANES[16]= vp3 + vp[2]; SHADOW_PLANES[17]= vp7 + vp[6]; SHADOW_PLANES[18]= vp11 + vp[10]; SHADOW_PLANES[19]= vp15 + vp[14];
        SHADOW_PLANES[20]= vp3 - vp[2]; SHADOW_PLANES[21]= vp7 - vp[6]; SHADOW_PLANES[22]= vp11 - vp[10]; SHADOW_PLANES[23]= vp15 - vp[14];

        // Fast 1-pass normalization using fused mag calculation
        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float x = SHADOW_PLANES[o], y = SHADOW_PLANES[o+1], z = SHADOW_PLANES[o+2];
            float magSq = x*x + y*y + z*z;
            if (magSq > 1e-12f) {
                float invMag = (float)Math.sqrt(1.0f / magSq);
                SHADOW_PLANES[o] *= invMag;
                SHADOW_PLANES[o+1] *= invMag;
                SHADOW_PLANES[o+2] *= invMag;
                SHADOW_PLANES[o+3] *= invMag;
            }
        }
    }

    private static boolean isOutsideShadowFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        // Cache local references to avoid repeated array indexing in tight loop
        float[] planes = SHADOW_PLANES;
        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float nx = planes[o];
            float ny = planes[o+1];
            float nz = planes[o+2];
            float d = planes[o+3];

            float px = nx >= 0 ? (float)maxX : (float)minX;
            float py = ny >= 0 ? (float)maxY : (float)minY;
            float pz = nz >= 0 ? (float)maxZ : (float)minZ;

            if (nx * px + ny * py + nz * pz + d < -0.5f) {
                return true;
            }
        }
        return false;
    }
}