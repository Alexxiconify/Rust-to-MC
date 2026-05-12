package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class DistantHorizonsCompat {
    private static final String DH_MOD_ID = "distanthorizons";
    private static final String DH_API_CLASS = "com.seibel.distanthorizons.api.DhApi";
    private static final String LAST_REFRESH_REASON = "FRUSTUM";
    private static final double LAST_CAMERA_MOVE_SQ = 0.0;
    private static final int SHADOW_PLANE_ARRAY_SIZE = 24;

    private static final ThreadLocal<float[]> THREAD_SHADOW_PLANES =
        ThreadLocal.withInitial(() -> new float[SHADOW_PLANE_ARRAY_SIZE]);

    // Cached reflection accessors to avoid repeated method lookups during matrix extraction (thread-safe)
    private static final java.util.concurrent.atomic.AtomicReference<java.lang.reflect.Method> cachedGetValuesAsArray =
        new java.util.concurrent.atomic.AtomicReference<>(null);
    private static final java.util.concurrent.atomic.AtomicReference<java.lang.reflect.Method> cachedToArray =
        new java.util.concurrent.atomic.AtomicReference<>(null);
    private static final java.util.concurrent.atomic.AtomicReference<java.lang.reflect.Method> cachedMatrixGetMethod =
        new java.util.concurrent.atomic.AtomicReference<>(null);
    private static final java.util.concurrent.atomic.AtomicReference<java.util.Map<String, java.lang.reflect.Field>> cachedFieldMap =
        new java.util.concurrent.atomic.AtomicReference<>(null);

    private static volatile boolean frustumInitialized;

    private DistantHorizonsCompat() {}

    public static String getLastRefreshReason() {
        return LAST_REFRESH_REASON;
    }

    public static boolean isFrustumInitialized() {
        return frustumInitialized;
    }

    public static double getLastCameraMoveSq() {
        return LAST_CAMERA_MOVE_SQ;
    }

    private static boolean isDhMissing() {
        return !FabricLoader.getInstance().isModLoaded(DH_MOD_ID);
    }

    public static void registerFrustumCuller() {
        if (isDhMissing()) {
            resetFrustumState();
            return;
        }
        try {
            Class<?> apiClass = Class.forName(DH_API_CLASS);
            Object overrides = apiClass.getField("overrides").get(null);
            Method bindMethod = findBindMethod(overrides);
            Class<?> frustumClass = Class.forName("com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiCullingFrustum");
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                DistantHorizonsCompat.class.getClassLoader(),
                new Class<?>[]{frustumClass},
                DistantHorizonsCompat::handleFrustumProxy
            );
            if (bindMethod != null) {
                bindMethod.invoke(overrides, frustumClass, proxy);
                frustumInitialized = true;
                RustMC.LOGGER.info("[Rust-MC] Registered DH frustum stub.");
            }
        } catch (Exception e) {
            resetFrustumState();
            RustMC.LOGGER.debug("[Rust-MC] DH frustum stub registration skipped: {}", e.getMessage());
        }
    }

    private static void resetFrustumState() {
        frustumInitialized = false;
        THREAD_SHADOW_PLANES.remove();
    }

    public static void optimizeLodThreading() {
        if (!isDhMissing()) {
            RustMC.LOGGER.debug("[Rust-MC] DH threading tuning skipped.");
        }
    }

    public static void prefetchLodData() {
        if (!isDhMissing()) {
            RustMC.LOGGER.debug("[Rust-MC] DH prefetch skipped.");
        }
    }

    public static void optimizeLighting(long[] lightTasks) {
        if (lightTasks == null || lightTasks.length == 0 || isDhMissing()) {
            return;
        }
        long[] taskSnapshot = java.util.Arrays.copyOf(lightTasks, lightTasks.length);
        NativeBridge.propagateLightDH(taskSnapshot, taskSnapshot.length);
    }

    public static int[] generateGpuLod(int[] blocks, int chunkX, int chunkZ, int detail) {
        if (blocks == null) {
            return new int[0];
        }
        // First check if this mesh was already generated asynchronously and cached
        int[] cached = NativeBridge.fetchCachedLodMesh(chunkX, chunkZ, detail);
        if (cached.length > 0) {
            return cached;
        }
        int[] blockSnapshot = java.util.Arrays.copyOf(blocks, blocks.length);
        // Avoid blocking render thread: if current thread appears to be a render thread,
        // schedule async generation and return empty so DH can fall back. This is a conservative
        // non-blocking integration example; users may replace with a proper cache/update callback.
        String tname = Thread.currentThread().getName().toLowerCase(java.util.Locale.ROOT);
        boolean isRenderLike = tname.contains("render") || tname.contains("game thread") || tname.contains("client");
        if (isRenderLike) {
            NativeBridge.generateLodMeshGpuAsync(blockSnapshot, chunkX, chunkZ, detail)
                .thenAccept(mesh -> {
                    // Mesh is already cached by generateLodMeshGpuAsync; just ignore the future
                });
            return new int[0];
        }
        return NativeBridge.generateLodMeshGpu(blockSnapshot, chunkX, chunkZ, detail);
    }

    /** Non-blocking version returning a CompletableFuture. */
    public static java.util.concurrent.CompletableFuture<int[]> generateGpuLodAsync(int[] blocks, int chunkX, int chunkZ, int detail) {
        if (blocks == null) return java.util.concurrent.CompletableFuture.completedFuture(new int[0]);
        int[] blockSnapshot = java.util.Arrays.copyOf(blocks, blocks.length);
        return NativeBridge.generateLodMeshGpuAsync(blockSnapshot, chunkX, chunkZ, detail);
    }

    private static Method findBindMethod(Object overridesInjector) {
        if (overridesInjector == null) {
            return null;
        }
        for (Method method : overridesInjector.getClass().getMethods()) {
            if (method.getName().equals("bind") && method.getParameterCount() == 2) {
                return method;
            }
        }
        return null;
    }

    private static Object handleFrustumProxy(Object proxy, Method method, Object[] args) {
        String name = method.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "getpriority" -> Integer.MAX_VALUE;
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            case "hashcode" -> System.identityHashCode(proxy);
            case "tostring" -> "RustMC-DH-FrustumCuller";
            default -> {
                if (name.contains("update")) {
                        handleFrustumUpdate(args);
                        yield null;
                }
                if (name.contains("intersect") || name.contains("visible") || name.contains("contain")) {
                    yield handleFrustumIntersects(args);
                }
                yield null;
            }
        };
    }

    private static void handleFrustumUpdate(Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        Object matrix = args[args.length - 1];
        float[] values = extractMatrixValues(matrix);
        if (values.length >= 16) {
            updateShadowPlanes(values);
            frustumInitialized = true;
        }
    }

    private static Object handleFrustumIntersects(Object[] args) {
        if (!frustumInitialized || args == null || args.length == 0) {
            return true;
        }
        SectionBounds bounds = resolveBounds(args);
        if (bounds == null) {
            return true;
        }
        if (shouldCullBelowPlayerY(bounds)) {
            return false;
        }
        return !isOutsideFrustum(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    private static boolean shouldCullBelowPlayerY(SectionBounds bounds) {
        if (!RustMC.CONFIG.isEnableDhCaveCulling()) {
            return false;
        }
        double refY = getDhReferenceY();
        return !Double.isNaN(refY) && bounds.maxY() < refY;
    }

    @SuppressWarnings("null")
    private static double getDhReferenceY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Double.NaN;
        }
        return client.player.getY();
    }

    private static float[] extractMatrixValues(Object mat) {
        if (mat == null) {
            return new float[0];
        }
        if (mat instanceof float[] values) {
            return values;
        }
        float[] values = tryInvokeNoArgFloatArray(mat, "getValuesAsArray");
        if (values.length >= 16) {
            return values;
        }
        values = tryInvokeNoArgFloatArray(mat, "toArray");
        if (values.length >= 16) {
            return values;
        }
        return tryInvokeMatrixGet(mat);
    }

    private static float[] tryInvokeNoArgFloatArray(Object mat, String methodName) {
        try {
            Method method;
            if ("getValuesAsArray".equals(methodName)) {
                method = cachedGetValuesAsArray.get();
                if (method == null) {
                    method = mat.getClass().getMethod(methodName);
                    cachedGetValuesAsArray.set(method);
                }
            } else if ("toArray".equals(methodName)) {
                method = cachedToArray.get();
                if (method == null) {
                    method = mat.getClass().getMethod(methodName);
                    cachedToArray.set(method);
                }
            } else {
                // Unknown method name, try it anyway
                method = mat.getClass().getMethod(methodName);
            }
            Object ret = method.invoke(mat);
            return ret instanceof float[] arr ? arr : new float[0];
        } catch (ReflectiveOperationException ignored) {
            return new float[0];
        }
    }

    private static float[] tryInvokeMatrixGet(Object mat) {
        try {
            // Cache the method to avoid repeated reflection
            Method method = cachedMatrixGetMethod.get();
            if (method == null || !method.getDeclaringClass().equals(mat.getClass())) {
                method = mat.getClass().getMethod("get", int.class, int.class);
                cachedMatrixGetMethod.set(method);
            }
            float[] out = new float[16];
            int idx = 0;
            for (int c = 0; c < 4; c++) {
                for (int r = 0; r < 4; r++) {
                    Object ret = method.invoke(mat, c, r);
                    if (!(ret instanceof Number n)) {
                        return new float[0];
                    }
                    out[idx++] = n.floatValue();
                }
            }
            return out;
        } catch (ReflectiveOperationException ignored) {
            return new float[0];
        }
    }

    private static void updateShadowPlanes(float[] vp) {
        float[] planes = THREAD_SHADOW_PLANES.get();
        float vp3 = vp[3];
        float vp7 = vp[7];
        float vp11 = vp[11];
        float vp15 = vp[15];

        planes[0] = vp3 + vp[0]; planes[1] = vp7 + vp[4]; planes[2] = vp11 + vp[8];  planes[3] = vp15 + vp[12];
        planes[4] = vp3 - vp[0]; planes[5] = vp7 - vp[4]; planes[6] = vp11 - vp[8];  planes[7] = vp15 - vp[12];
        planes[8] = vp3 + vp[1]; planes[9] = vp7 + vp[5]; planes[10] = vp11 + vp[9]; planes[11] = vp15 + vp[13];
        planes[12] = vp3 - vp[1]; planes[13] = vp7 - vp[5]; planes[14] = vp11 - vp[9]; planes[15] = vp15 - vp[13];
        planes[16] = vp3 + vp[2]; planes[17] = vp7 + vp[6]; planes[18] = vp11 + vp[10]; planes[19] = vp15 + vp[14];
        planes[20] = vp3 - vp[2]; planes[21] = vp7 - vp[6]; planes[22] = vp11 - vp[10]; planes[23] = vp15 - vp[14];

        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float x = planes[o];
            float y = planes[o + 1];
            float z = planes[o + 2];
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            if (len > 1.0e-6f) {
                float inv = 1.0f / len;
                planes[o] *= inv;
                planes[o + 1] *= inv;
                planes[o + 2] *= inv;
                planes[o + 3] *= inv;
            }
        }
    }

    private static boolean isOutsideFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float[] planes = THREAD_SHADOW_PLANES.get();
        for (int i = 0; i < 6; i++) {
            int o = i * 4;
            float nx = planes[o];
            float ny = planes[o + 1];
            float nz = planes[o + 2];
            float d = planes[o + 3];
            double px = nx >= 0 ? maxX : minX;
            double py = ny >= 0 ? maxY : minY;
            double pz = nz >= 0 ? maxZ : minZ;
            if (nx * px + ny * py + nz * pz + d < 0.0f) {
                return true;
            }
        }
        return false;
    }

    private static SectionBounds resolveBounds(Object[] args) {
        for (Object arg : args) {
            SectionBounds bounds = resolveBounds(arg);
            if (bounds != null) {
                return bounds;
            }
        }
        return null;
    }

    private static SectionBounds resolveBounds(Object arg) {
        return switch (arg) {
            case null -> null;
            case SectionBounds bounds -> bounds;
            case double[] values when values.length >= 6 -> new SectionBounds(values[0], values[1], values[2], values[3], values[4], values[5]);
            case float[] values when values.length >= 6 -> new SectionBounds(values[0], values[1], values[2], values[3], values[4], values[5]);
            case Object[] values when values.length >= 6
                && values[0] instanceof Number a
                && values[1] instanceof Number b
                && values[2] instanceof Number c
                && values[3] instanceof Number d
                && values[4] instanceof Number e
                && values[5] instanceof Number f -> new SectionBounds(a.doubleValue(), b.doubleValue(), c.doubleValue(), d.doubleValue(), e.doubleValue(), f.doubleValue());
            default -> resolveBoundsFromFields(arg);
        };
    }

    private static SectionBounds resolveBoundsFromFields(Object arg) {
        try {
            Class<?> type = arg.getClass();
            double minX = readField(type, arg, "minX");
            double minY = readField(type, arg, "minY");
            double minZ = readField(type, arg, "minZ");
            double maxX = readField(type, arg, "maxX");
            double maxY = readField(type, arg, "maxY");
            double maxZ = readField(type, arg, "maxZ");
            return new SectionBounds(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
    private static double readField(Class<?> type, Object target, String name) throws ReflectiveOperationException {
        // Cache fields per type to avoid repeated reflection
        java.util.Map<String, Field> map = cachedFieldMap.get();
        if (map == null) {
            synchronized (DistantHorizonsCompat.class) {
                map = cachedFieldMap.get();
                if (map == null) {
                    map = new java.util.concurrent.ConcurrentHashMap<>();
                    cachedFieldMap.set(map);
                }
            }
        }
        String key = type.getName() + "." + name;
        Field field = map.computeIfAbsent(key, k -> {
            try {
                return type.getField(name);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        });
        Object value = field.get(target);
        return value instanceof Number number ? number.doubleValue() : Double.NaN;
    }

    private record SectionBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}
}