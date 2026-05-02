package com.alexxiconify.rustmc;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
//  NativeBridge handles all communication between Java and the Rust native core via JNI.
public class NativeBridge {
    private NativeBridge() {}
    public static final int CONTEXT_VANILLA = 0;
    public static final int CONTEXT_SODIUM = 1;
    public static final int CONTEXT_LUX = 2;
    public static final int CONTEXT_STARLIGHT = 3;
    private static boolean libLoaded;
    private static final AtomicBoolean noiseSeeded = new AtomicBoolean(false);
    // Counters managed externally; removed debug tracking overhead
    private static final long[] chunkIngestStats = new long[4];
    private static final long[] metricsSnapshot = new long[5];
    // Cache optional native symbol availability to avoid repeated exception fallbacks in hot culling paths.
    private static final AtomicBoolean supportsFrustumMarginTest = new AtomicBoolean(true);
    private static final AtomicBoolean supportsDhVerticalCull = new AtomicBoolean(true);
    private static final AtomicBoolean supportsDhFusedCull = new AtomicBoolean(true);
    private static final AtomicBoolean supportsDhOcclusionTest = new AtomicBoolean(true);
    private static final AtomicBoolean supportsDhOcclusionSubmit = new AtomicBoolean(true);
    private static final AtomicBoolean supportsChunkDataOffload = new AtomicBoolean(true);
    private static final AtomicBoolean supportsMemoryCleanup = new AtomicBoolean(true);
    private static final java.util.concurrent.atomic.AtomicLong chunkIngestAttempts = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong chunkIngestForwards = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong chunkIngestFailures = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong chunkIngestTotalNanos = new java.util.concurrent.atomic.AtomicLong(0L);
    // Lightweight local timing metrics (nanos)
    private static final java.util.concurrent.atomic.AtomicLong frustumCalls = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong frustumTotalNanos = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong particleCalls = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong particleTotalNanos = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong dhFusedCalls = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final java.util.concurrent.atomic.AtomicLong dhFusedTotalNanos = new java.util.concurrent.atomic.AtomicLong(0L);
    private static final int DNS_PARALLEL_THRESHOLD = 16;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    // Frame history tracking removed; use external telemetry if needed
    public static boolean isReady() { return libLoaded; }
    static {
        try {
            String libName = System.mapLibraryName("rust_mc_core");
            Path devPath = Paths.get("rust_mc_core/target/release/" + libName).toAbsolutePath();
            if (Files.exists(devPath)) {
                System.load(devPath.toString());
            } else {
                // Use a persistent cache path in the game config directory to avoid re-extracting every launch
                Path cacheDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("rustmc-bin");
                Files.createDirectories(cacheDir);
                byte[] bundledLib;
                try (java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName)) {
                    if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                    bundledLib = is.readAllBytes();
                }
                String cacheKey = fingerprintBytes(bundledLib);
                Path cachedLib = cacheDir.resolve(libName + "-" + cacheKey);
                cleanupStaleNativeCache(cacheDir, libName, cachedLib.getFileName().toString());
                if (!Files.exists(cachedLib)) {
                    Files.write(cachedLib, bundledLib, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
                }
                System.load(cachedLib.toString());
            }
            libLoaded = true;
            RustMC.LOGGER.info("[Rust-MC] Native library loaded successfully.");
        } catch (Exception t) {
            libLoaded = false;
            RustMC.LOGGER.error("[Rust-MC] Failed to load native library ({}). fallback to Java.", t.getMessage());
        }
    }

    private static String fingerprintBytes(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >>> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        }
    }

    private static void cleanupStaleNativeCache(Path cacheDir, String libName, String keepFileName) {
        try (java.util.stream.Stream<Path> files = Files.list(cacheDir)) {
            files.filter(path -> path.getFileName().toString().startsWith(libName + "-"))
                 .filter(path -> !path.getFileName().toString().equals(keepFileName))
                 .forEach(path -> {
                     try {
                         Files.deleteIfExists(path);
                     } catch (Exception ignored) {
                         // Ignore stale cache cleanup failures; load still works.
                     }
                 });
        } catch (Exception ignored) {
            // Ignore cleanup failures; cache rebuild still continues.
        }
    }

    // --- Native Methods ---
    private static native void rustNoiseInit(int seed);
    private static native void rustNoiseReset();
    private static native double rustNoise2d(double x, double y);
    private static native double rustNoise3d(double x, double y, double z);
    private static native float rustGetGhostHeight(double x, double z);
    private static native long[] rustGetSystemMemory();
    private static native int rustPropagateLightBulk(int[] data, int count, int context);
    private static native int rustPropagateLightDH(long[] tasks, int count);
    private static native byte[] rustCompress(byte[] input);
    private static native int[] rustGenerateGhostMap(double centerX, double centerZ, int size, double scale);
    private static native byte[] rustDecompress(byte[] input, int maxOutputSize);
    private static native int rustFindPath(int[] start, int[] end);
    private static native int rustExecuteCommand(byte[] cmd);
    private static native int rustProcessPacket(byte[] buf, int len);
    private static native void rustProcessChunkData(byte[] buf, int len, int chunkX, int chunkZ);
    private static native void rustRequestMemoryCleanup();
    //
     // Subverts Java-side chunk data parsing by offloading large byte buffers
     // directly to Rust's optimized decoder (PumpkinMC style).
    public static void processChunkData(byte[] buf, int chunkX, int chunkZ) {
        if (!libLoaded || buf == null || !supportsChunkDataOffload.get()) return;
        if (!RustMC.CONFIG.isEnableChunkIngestOffload()) return;
        chunkIngestAttempts.incrementAndGet();
        boolean trackTiming = RustMC.CONFIG.isEnableChunkIngestValidation();
        long start = trackTiming ? System.nanoTime() : 0L;
        try {
            rustProcessChunkData(buf, buf.length, chunkX, chunkZ);
            chunkIngestForwards.incrementAndGet();
        } catch (UnsatisfiedLinkError ignored) {
            chunkIngestFailures.incrementAndGet();
            supportsChunkDataOffload.set(false);
        } finally {
            if (trackTiming) {
                chunkIngestTotalNanos.addAndGet(System.nanoTime() - start);
            }
        }
    }

    public static synchronized long[] getChunkIngestStats() {
        long[] snapshot = chunkIngestStats;
        long attempts = chunkIngestAttempts.get();
        long forwards = chunkIngestForwards.get();
        long failures = chunkIngestFailures.get();
        long totalNanos = chunkIngestTotalNanos.get();
        long avgMicros = forwards > 0 ? (totalNanos / forwards) / 1_000L : 0L;
        snapshot[0] = attempts;
        snapshot[1] = forwards;
        snapshot[2] = failures;
        snapshot[3] = avgMicros;
        return snapshot;
    }
    public static void requestMemoryCleanup() {
        if (!libLoaded || !supportsMemoryCleanup.get()) return;
        try {
            rustRequestMemoryCleanup();
        } catch (UnsatisfiedLinkError ignored) {
            supportsMemoryCleanup.set(false);
        }
    }
    // Frustum state management
    private static native long rustFrustumCreate();
    private static native void rustFrustumUpdate(long ptr, float[] vpMatrix);
    private static native void rustFrustumUpdate(long ptr, float[] vpMatrix, double fovScale, double camX, double camY, double camZ);
    private static native void rustUpdateFrustumAndCave(long ptr, float[] vpMatrix, double fovScale, double camX, double camY, double camZ, boolean inCave);
    private static native boolean rustIsOutsideFrustum(long ptr, double x, double y, double z, double radius);
    private static native int rustCullEntities(long ptr, double[] positions, int count, boolean[] results, float margin);
    private static native int[] rustGenerateLodMeshGpu(int[] blocks, int chunkX, int chunkZ, int detail);
    private static native void rustFrustumDestroy(long ptr);
    // Updates the persistent Vanilla frustum in Rust's global context.
    // This avoids creating new frustum objects every frame.
    public static void updateVanillaFrustum(float[] vpMatrix) {
        if (!libLoaded || vpMatrix == null || vpMatrix.length < 16) return;
        ClientFrustumContext ctx = getClientFrustumContext();
        if (ctx != null) {
            updateVanillaFrustum(vpMatrix, ctx.fovScale(), ctx.camX(), ctx.camY(), ctx.camZ());
            return;
        }
        try { rustFrustumUpdate(0, vpMatrix); }
    catch (UnsatisfiedLinkError ignored) { /* */ }
    }
    public static void updateVanillaFrustum(float[] vpMatrix, double fovScale, double camX, double camY, double camZ) {
        if (!libLoaded || vpMatrix == null || vpMatrix.length < 16) return;
        try {
            rustFrustumUpdate(0, vpMatrix, fovScale, camX, camY, camZ);
        } catch (UnsatisfiedLinkError e) {
            try { rustFrustumUpdate(0, vpMatrix); }
            catch (UnsatisfiedLinkError ignored) { /* Fallback to legacy native update */ }
        }
    }
    public static void updateVanillaFrustumAndCave(float[] vpMatrix, boolean inCave) {
        if (!libLoaded || vpMatrix == null || vpMatrix.length < 16) return;
        ClientFrustumContext ctx = getClientFrustumContext();
        if (ctx != null) {
            try {
                long start = 0L;
                boolean track = RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.TIMING
                    || RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.ALL;
                if (track) start = System.nanoTime();
                rustUpdateFrustumAndCave(0, vpMatrix, ctx.fovScale(), ctx.camX(), ctx.camY(), ctx.camZ(), inCave);
                if (track) {
                    frustumCalls.incrementAndGet();
                    frustumTotalNanos.addAndGet(System.nanoTime() - start);
                }
                return;
            } catch (UnsatisfiedLinkError ignored) {
                // Fall through to separate native calls using the same captured context.
            }
            updateVanillaFrustum(vpMatrix, ctx.fovScale(), ctx.camX(), ctx.camY(), ctx.camZ());
            updateCaveStatus(inCave);
            return;
        }
        updateVanillaFrustum(vpMatrix);
        updateCaveStatus(inCave);
    }
    // Optimizes entity/particle culling by offloading frustum intersection checks to Rust.
    // Uses the persistent global frustum updated via 'updateVanillaFrustum'.
    public static boolean isOutsideFrustum(double x, double y, double z, double radius) {
        if (!libLoaded) return false;
        try {
            return rustIsOutsideFrustum(0, x, y, z, radius);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
    public static int cullEntities(double[] positions, boolean[] results) {
        if (positions == null || results == null) return 0;
        if (!libLoaded) return 0;
        try {
            float margin = com.alexxiconify.rustmc.compat.ImmediatelyFastCompat.getCullingDistanceMultiplier();
            int count = positions.length / 3;
            return rustCullEntities(0, positions, count, results, margin);
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }
    // Offloads heavy vertex transformations (EMF/ETF animations) to Rust. Processes XYZ and Normal arrays in parallel.
    public static void transformVertices(float[] vertices, float[] normals, float[] matrix) {
        if (!libLoaded || vertices == null || normals == null || matrix == null) return;
        rustTransformVertices(vertices, normals, matrix, vertices.length / 3);
    }
    public static void invokeMatrixMul(float[] left, float[] right, float[] result) {
        if (left == null || right == null || result == null || left.length < 16 || right.length < 16 || result.length < 16) return;
        if (!libLoaded) {
            multiplyMatrices(left, right, result);
            return;
        }
        try {
            rustMatrixMul(left, right, result);
        } catch (UnsatisfiedLinkError e) {
            multiplyMatrices(left, right, result);
        }
    }
    private static native void rustProcessMapTexture(int[] pixels, int width, int height);
    private static native void rustProcessMapTexturePtr(long ptr, int width, int height);
    private static native void rustProcessAudio(float[] samples, int count, float volume, float pan);
    private static native void rustTransformVertices(float[] vertices, float[] normals, float[] matrix, int count);
    private static native void rustMatrixMul(float[] left, float[] right, float[] result);
    private static native int[] rustSampleBiomes(long seed, int x, int z, int width, int height);
    public static int[] sampleBiomes(long seed, int x, int z, int width, int height) {
        if (!libLoaded || width <= 0 || height <= 0) return new int[0];
        return rustSampleBiomes(seed, x, z, width, height);
    }
    private static native void rustTickParticles(double[] positions, double[] velocities, int count, double gravity, double camX, double camY, double camZ, double maxDistSq);
    //
     // Parallelizes particle physics (gravity, velocity decay).
     // Ideal for mods that spawn thousands of environmental particles.
    public static void tickParticlesNative(double[] positions, double[] velocities, double gravity, double camX, double camY, double camZ, double maxDistSq) {
        if (!libLoaded) return;
        if (positions == null || velocities == null) return;
        if (positions.length == 0 || velocities.length == 0) return;
        int count = Math.min(positions.length, velocities.length) / 3;
        if ( count == 0 ) return;
        long start = 0L;
        boolean track = RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.TIMING
            || RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.ALL;
        if (track) start = System.nanoTime();
        try {
            rustTickParticles(positions, velocities, count, gravity, camX, camY, camZ, maxDistSq);
        } finally {
            if (track) {
                particleCalls.incrementAndGet();
                particleTotalNanos.addAndGet(System.nanoTime() - start);
            }
        }
    }

    public static void tickParticlesAdaptive(double[] positions, double[] velocities, double gravity) {
        com.alexxiconify.rustmc.util.ParticleTickDispatcher.tick(positions, velocities, gravity);
    }
    private static native void rustProcessSoundPhysics(float[] samples, int count, double distance, double occlusion);
    private static native int[] rustBlendBiomes(int[] biomeIds, int width, int height, int radius);
    //
     // Offloads sound occlusion and reverb math to Rust.
    public static void processSoundPhysics(float[] samples, double distance, double occlusion) {
        if (!libLoaded || samples == null) return;
        rustProcessSoundPhysics(samples, samples.length, distance, occlusion);
    }
    //
     // Multithreaded map texture processing.
     // Ideal for mods that render complex maps in item frames or UI.
    public static void processMapTexture(int[] pixels, int width, int height) {
        if (!libLoaded || pixels == null || pixels.length == 0) return;
        rustProcessMapTexture(pixels, width, height);
    }
    //
     // Zero-copy map texture processing using a direct memory pointer.
    public static void processMapTexturePtr(long ptr, int width, int height) {
        if (!libLoaded || ptr == 0) return;
        rustProcessMapTexturePtr(ptr, width, height);
    }
    //
     // SIMD Audio processing (Volume/Pan/Normalization).
     // Offloads sound buffer manipulation to Rust.
    public static void processAudio(float[] samples, float volume, float pan) {
        if (!libLoaded || samples == null || samples.length == 0) return;
        rustProcessAudio(samples, samples.length, volume, pan);
    }
    //
     // Multithreaded biome blending (supports Better Biome Blend).
    public static int[] blendBiomes(int[] biomeIds, int width, int height, int radius) {
        if (!libLoaded) return biomeIds;
        return rustBlendBiomes(biomeIds, width, height, radius);
    }
    private static native void rustFrustumSetFovScale(long ptr, double fovScale);
    private static native boolean rustFrustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin);
    private static native boolean rustFrustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native byte[] rustBatchFrustumTest(long ptr, double[] aabbs, int count, double margin);
    //
     // Conservative frustum test with margin (useful for DH chunks/LODs).
    public static boolean frustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin) {
        if (!libLoaded) return true;
        if (supportsFrustumMarginTest.get()) {
            try {
                return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin);
            } catch (UnsatisfiedLinkError e) {
                supportsFrustumMarginTest.set(false);
            }
        }
        try {
            return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ);
        } catch (UnsatisfiedLinkError ignored) {
            return true;
        }
    }
    //
     // Batch frustum test with margin.
    public static byte[] batchFrustumTest(long ptr, double[] aabbs, double margin) {
        if (aabbs == null) return EMPTY_BYTE_ARRAY;
        return batchFrustumTest(ptr, aabbs, aabbs.length / 6, margin);
    }
    private static native boolean rustDHCull(double minY, double maxY, double surfaceY);
    private static native boolean rustDHCullFused(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double surfaceY);
    private static native void rustSetCaveStatus(boolean inCave);
    private static native boolean rustRayIntersectsBox(double rx, double ry, double rz, double dx, double dy, double dz, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native boolean rustOcclusionTest(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native void rustOcclusionSubmit(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native float[] rustComputeAmbientOcclusion(float[] vertexData, int vertexCount);
    private static native float[] rustComputeAmbientOcclusionDirect(java.nio.ByteBuffer vertexData, int vertexCount);
    // DNS cache
    private static native String rustDnsResolve(String hostname);
    private static native String[] rustDnsBatchResolve(String[] hostnames);
    private static native void rustDnsCacheClear();
    private static native int rustDnsCacheSize();
    private static native String rustDnsCacheExport();
    private static native void rustDnsCacheImport(String json);
    private static native int rustInflateRaw(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int outputMaxLen);
    private static native long[] rustGetMetrics(boolean reset);
    // --- Wrapper Methods ---
    public static void noiseInit(long mcSeed) {
        if (!libLoaded) return;
        if (!noiseSeeded.compareAndSet(false, true)) return;
        try {
            rustNoiseInit((int) mcSeed);
        } catch (UnsatisfiedLinkError e) {
            RustMC.LOGGER.warn("[Rust-MC] rustNoiseInit not linked: {}", e.getMessage());
        }
    }
    public static void noiseReset() {
        if (!libLoaded) return;
        noiseSeeded.set(false);
        try { rustNoiseReset(); }
        catch (UnsatisfiedLinkError ignored) {
            // Optional native method; fallback to doing nothing if not linked
        }
    }

    // ── Math Constants ──
    private static final float DEGREES_FULL_CIRCLE = 360.0f;
    private static final float DEGREES_HALF_CIRCLE = 180.0f;
    private static final float NEGATIVE_DEGREES_HALF_CIRCLE = -180.0f;

    private static float wrapDegreesJava(float value) {
        float v = value % DEGREES_FULL_CIRCLE;
        if (v >= DEGREES_HALF_CIRCLE) v -= DEGREES_FULL_CIRCLE;
        else if (v < NEGATIVE_DEGREES_HALF_CIRCLE) v += DEGREES_FULL_CIRCLE;
        return v;
    }

    // All trivial math now direct to Java (no JNI overhead)
    public static float invokeSin(float x) { return (float) Math.sin(x); }
    public static float invokeCos(float x) { return (float) Math.cos(x); }
    public static float invokeSqrt(float x) { return (float) Math.sqrt(x); }
    public static double invokeAtan2(double y, double x) { return Math.atan2(y, x); }
    public static float invokeClamp(float value, float min, float max) { return Math.clamp(value, min, max); }
    public static double invokeLerp(double delta, double start, double end) { return start + delta * (end - start); }
    public static double invokeAbsMax(double a, double b) { return Math.max(Math.abs(a), Math.abs(b)); }
    public static float invokeWrapDegrees(float value) { return wrapDegreesJava(value); }
    public static double noise2d(double x, double y) {
        if (!libLoaded) return 0.0;
        try { return rustNoise2d(x, y); }
        catch (UnsatisfiedLinkError e) { return 0.0; }
    }
    public static double noise3d(double x, double y, double z) {
        if (!libLoaded) return 0.0;
        try { return rustNoise3d(x, y, z); }
        catch (UnsatisfiedLinkError e) { return 0.0; }
    }
    public static float getGhostHeight(double x, double z) {
        if (!libLoaded) return 64.0f;
        try { return rustGetGhostHeight(x, z); }
        catch (UnsatisfiedLinkError e) { return 64.0f; }
    }
    public static void getSystemMemory(long[] out) {
        if (!libLoaded || out == null || out.length < 2) return;
        try {
            long[] result = rustGetSystemMemory();
            if (result != null && result.length >= 2) {
                out[0] = result[0];
                out[1] = result[1];
            }
        } catch (UnsatisfiedLinkError e) { // Fallback when the native entrypoint is unavailable.
        }
    }
    public static int propagateLightBulk(int[] data, int len) {
        if (data == null || len == 0) return -1;
        int safeLen = Math.min(len, data.length);
        if (safeLen == 0) return -1;
        int context = CONTEXT_VANILLA;
        if (ModBridge.SCALABLELUX) {
            context = CONTEXT_LUX;
        } else if (ModBridge.STARLIGHT) {
            context = CONTEXT_STARLIGHT;
        } else if (ModBridge.SODIUM) {
            context = CONTEXT_SODIUM;
        }
        return propagateLightBulk(data, safeLen, context);
    }

    public static int propagateLightBulk(int[] data, int len, int context) {
        if (!libLoaded || data == null || len <= 0) return -1;
        int safeLen = Math.min(len, data.length);
        if ( safeLen == 0 ) return -1;
        try { return rustPropagateLightBulk(data, safeLen, context); }
        catch (UnsatisfiedLinkError e) { return -1; }
    }
    public static void propagateLightDH(long[] tasks, int len) {
        if (!libLoaded) return;
        try { rustPropagateLightDH(tasks, len); }
        catch (UnsatisfiedLinkError ignored) {/* */
        }
    }
    public static byte[] invokeCompress(byte[] input) {
        if (!libLoaded) return new byte[0];
        try { return rustCompress(input); }
        catch (UnsatisfiedLinkError e) { return new byte[0]; }
    }
    public static int[] generateGhostMap(double centerX, double centerZ, int size, double scale) {
        if (!libLoaded) return new int[size * size];
        try { return rustGenerateGhostMap(centerX, centerZ, size, scale); }
        catch (UnsatisfiedLinkError e) { return new int[size * size]; }
    }
    public static int inflateRaw(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int outputMaxLen) {
        if (!libLoaded || input == null || output == null) return -1;
        try { return rustInflateRaw(input, inputOffset, inputLen, output, outputOffset, outputMaxLen); }
        catch (UnsatisfiedLinkError e) { return -1; }
    }
    public static byte[] invokeDecompress(byte[] input, int maxOutputSize) {
        if (!libLoaded) return new byte[0];
        try { return rustDecompress(input, maxOutputSize); }
        catch (UnsatisfiedLinkError e) { return new byte[0]; }
    }
    // Reusable thread-local arrays for pathfinding avoid per-call allocation and remain thread-safe.
    @SuppressWarnings("java:S5164")
    private static final ThreadLocal<int[][]> PATH_SCRATCH = ThreadLocal.withInitial(() -> new int[][] {
        new int[3], new int[3]
    });
    public static int findPathRaw(int sx, int sy, int sz, int ex, int ey, int ez) {
        if (!libLoaded) return -1;
        try {
            int[][] scratch = PATH_SCRATCH.get();
            int[] pathStart = scratch[0];
            int[] pathEnd = scratch[1];
            pathStart[0] = sx; pathStart[1] = sy; pathStart[2] = sz;
            pathEnd[0] = ex; pathEnd[1] = ey; pathEnd[2] = ez;
            return rustFindPath(pathStart, pathEnd);
        } catch (UnsatisfiedLinkError e) { return -1; }
    }
    public static int executeCommand(byte[] cmd) {
        if (!libLoaded) return -1;
        try { return rustExecuteCommand(cmd); }
        catch (UnsatisfiedLinkError e) { return -1; }
    }
    public static int invokeProcessPacket(byte[] buf, int len) {
        if (!libLoaded) return -1;
        try { return rustProcessPacket(buf, len); }
        catch (UnsatisfiedLinkError e) { return -1; }
    }
    public static int invokeFrustumIntersect(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return libLoaded && minX <= maxX && minY <= maxY && minZ <= maxZ ? 1 : 0;
    }
    public static long createRustFrustum() {
        if (!libLoaded) return 0;
        try { return rustFrustumCreate(); }
        catch (UnsatisfiedLinkError e) { return 0; }
    }
    public static void updateRustFrustum(long ptr, float[] vpMatrix) {
        updateRustFrustumTracked(ptr, vpMatrix);
    }
    // Returns true only when a native frustum update call was successfully invoked.
    public static boolean updateRustFrustumTracked(long ptr, float[] vpMatrix) {
        if (!libLoaded || ptr == 0 || vpMatrix == null || vpMatrix.length < 16) return false;
        ClientFrustumContext ctx = getClientFrustumContext();
        if (ctx != null) {
            try {
                rustFrustumUpdate(ptr, vpMatrix, ctx.fovScale(), ctx.camX(), ctx.camY(), ctx.camZ());
                return true;
            } catch (UnsatisfiedLinkError ignored) { // Fallback to newer signature below.
            }
        }
        try { rustFrustumUpdate(ptr, vpMatrix); return true;}
        catch (UnsatisfiedLinkError ignored) { return false;}
    }
    public static void setRustFrustumFovScale(long ptr, double fovScale) {
        if (!libLoaded || ptr == 0) return;
        try { rustFrustumSetFovScale(ptr, fovScale); }
        catch (UnsatisfiedLinkError ignored) { // Optional native method.
        }
    }
    public static boolean testRustFrustum(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return testRustFrustum(ptr, minX, minY, minZ, maxX, maxY, maxZ, 0.0);
    }
    public static boolean testRustFrustum(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin) {
        if (!libLoaded || ptr == 0) return true;
        try {
            return supportsFrustumMarginTest.get()
                ? tryRustFrustumTestWithMargin(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin)
                : rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ);
        } catch (UnsatisfiedLinkError e) {
            return true;
        }
    }

    private static boolean tryRustFrustumTestWithMargin(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin) {
        try {
            return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin);
        } catch (UnsatisfiedLinkError e) {
            supportsFrustumMarginTest.set(false);
            return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
    private record ClientFrustumContext(double fovScale, double camX, double camY, double camZ) {}

    private static double getDhReferenceY() {
        // Quick fast-path check for null client first
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return Double.NaN;
        }
        try {
            return client.player.getY();
        } catch (Exception ignored) {
            return Double.NaN;
        }
    }

    private static ClientFrustumContext getClientFrustumContext() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null) return null;
            var camera = client.gameRenderer.getCamera();
            double fov = client.options.getFov().getValue();
            double aspect = client.getWindow().getFramebufferWidth() / Math.max(1.0, client.getWindow().getFramebufferHeight());
            double aspectBoost = Math.max(1.0, aspect / (16.0 / 9.0));
            double fovScale = Math.clamp(1.15 * (fov / 70.0) * Math.sqrt(aspectBoost), 0.8, 2.5);
            var pos = camera.getCameraPos();
            return new ClientFrustumContext(fovScale, pos.x, pos.y, pos.z);
        } catch (Exception ignored) { return null;}
    }
    public static void destroyRustFrustum(long ptr) {
        if (!libLoaded || ptr == 0) return;
        try { rustFrustumDestroy(ptr); }
        catch (UnsatisfiedLinkError ignored) { // Optional native method.
        }
    }
    // Tests multiple AABBs in one JNI call. aabbs is flat [minX,minY,minZ,maxX,maxY,maxZ,...].
    public static byte[] batchFrustumTest(long ptr, double[] aabbs, int count) {
        return batchFrustumTest(ptr, aabbs, count, 0.0);
    }
    public static byte[] batchFrustumTest(long ptr, double[] aabbs, int count, double margin) {
        if (aabbs == null || count <= 0) return EMPTY_BYTE_ARRAY;
        int safeCount = Math.min(count, aabbs.length / 6);
        if (safeCount == 0) return EMPTY_BYTE_ARRAY;
        if (!libLoaded || ptr == 0) {
            byte[] all = new byte[safeCount];
            java.util.Arrays.fill(all, (byte) 1);
            return all;
        }
        try {
            return rustBatchFrustumTest(ptr, aabbs, safeCount, margin);
        } catch (UnsatisfiedLinkError e) {
            byte[] all = new byte[safeCount];
            java.util.Arrays.fill(all, (byte) 1);
            return all;
        }
    }

    // Frustum result recording removed; use external profilers for telemetry
    public static boolean invokeDHCull(double minY, double maxY, double surfaceY) {
        if (!libLoaded) return true;
        if (!supportsDhVerticalCull.get()) return true;
        try {
            return rustDHCull(minY, maxY, surfaceY);
        } catch (UnsatisfiedLinkError e) {
            supportsDhVerticalCull.set(false);
            return true;
        }
    }
    public static boolean invokeDHCullFused(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double surfaceY) {
        if (!libLoaded || ptr == 0) return false;
        if (!supportsDhFusedCull.get()) return false;
        long start = 0L;
        boolean track = RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.TIMING
            || RustMC.CONFIG.getDiagnosticMode() == com.alexxiconify.rustmc.config.RustMCConfig.DiagnosticMode.ALL;
        if (track) start = System.nanoTime();
        try {
            boolean res = rustDHCullFused(ptr, minX, minY, minZ, maxX, maxY, maxZ, surfaceY);
            if (track) {
                dhFusedCalls.incrementAndGet();
                dhFusedTotalNanos.addAndGet(System.nanoTime() - start);
            }
            return res;
        } catch (UnsatisfiedLinkError e) {
            supportsDhFusedCull.set(false);
            return false;
        }
    }
    // Same as invokeDHCullFused but returns empty when native symbol is unavailable, allowing fallback logic.
    public static Optional<Boolean> tryDHCullFused(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double surfaceY) {
        if (!libLoaded || ptr == 0 || !supportsDhFusedCull.get()) return Optional.empty();
        try {
            return Optional.of(rustDHCullFused(ptr, minX, minY, minZ, maxX, maxY, maxZ, surfaceY));
        } catch (UnsatisfiedLinkError e) {
            supportsDhFusedCull.set(false);
            return Optional.empty();
        }
    }
    private static boolean isDHOccluded(double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        if (!libLoaded) return false;
        if (!supportsDhOcclusionTest.get()) return false;
        try {
            return rustOcclusionTest(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (UnsatisfiedLinkError ignored) {
            supportsDhOcclusionTest.set(false);
            return false;
        }
    }

    private static void submitDHOccluder(double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ) {
        if (!libLoaded) return;
        if (!supportsDhOcclusionSubmit.get()) return;
        try {
            rustOcclusionSubmit(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (UnsatisfiedLinkError ignored) {
            supportsDhOcclusionSubmit.set(false);
        }
    }

    private static boolean shouldCullDhBelowSurface(double refY, double surfaceY) {
        return RustMC.CONFIG.isEnableDhCaveCulling() && !Double.isNaN(refY) && refY < surfaceY;
    }

    private static boolean passesDhVerticalGate(boolean applyVerticalGate,
                                                double refY,
                                                double minY,
                                                double maxY,
                                                double surfaceY) {
        if (!applyVerticalGate) {
            return true;
        }
        if (shouldCullDhBelowSurface(refY, surfaceY)) {
            return false;
        }
        return invokeDHCull(minY, maxY, surfaceY);
    }

    // DH section visibility: frustum first, optional vertical gate for absolute space,
    // then DH-only occlusion where only frustum-kept chunks can occlude other DH chunks.
    public static boolean cullDistantHorizonsSection(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double surfaceY, double margin, boolean applyVerticalGate) {
        if (!libLoaded || ptr == 0) return true;

        // Fused path: handles frustum, vertical gate (if context is cave), and occlusion in one JNI call.
        // We use margin=0 for DH since they have their own adaptive margin logic usually.
        if (supportsDhFusedCull.get()) {
            return invokeDHCullFused(ptr, minX, minY, minZ, maxX, maxY, maxZ, surfaceY);
        }

        // Fallback path
        boolean visibleByFrustum = testRustFrustum(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin);
        if (!visibleByFrustum) return false;

        double refY = applyVerticalGate ? getDhReferenceY() : Double.NaN;
        if (!passesDhVerticalGate(applyVerticalGate, refY, minY, maxY, surfaceY)) return false;

        if (isDHOccluded(minX, minY, minZ, maxX, maxY, maxZ)) return false;
        submitDHOccluder(minX, minY, minZ, maxX, maxY, maxZ);
        return true;
    }
    public static void updateCaveStatus(boolean inCave) {
        if (!libLoaded) return;
        try { rustSetCaveStatus(inCave); }
        catch (UnsatisfiedLinkError ignored) { /* */ }
    }
    public static final float AVG_FPS = 0.0f;
    public static boolean invokeRayIntersectsBox(double rx, double ry, double rz, double dx, double dy, double dz, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!libLoaded) return true; // Safe fallback: assume intersection to trigger Vanilla calc
        try { return rustRayIntersectsBox(rx, ry, rz, dx, dy, dz, minX, minY, minZ, maxX, maxY, maxZ); }
        catch (UnsatisfiedLinkError e) { return true; }
    }
    public static float[] invokeComputeAmbientOcclusion(float[] vertexData, int vertexCount) {
        if (!libLoaded || vertexData == null || vertexCount <= 0) return new float[0];
        try { return rustComputeAmbientOcclusion(vertexData, vertexCount); }
        catch (UnsatisfiedLinkError e) { return new float[0]; }
    }
    public static float[] invokeComputeAmbientOcclusionDirect(java.nio.ByteBuffer vertexData, int vertexCount) {
        if (!libLoaded || vertexData == null || vertexCount <= 0 || !vertexData.isDirect()) return new float[0];
        try {
            return rustComputeAmbientOcclusionDirect(vertexData, vertexCount);
        } catch (UnsatisfiedLinkError e) {
            return new float[0];
        }
    }

    public static float[] invokeGetFrameHistory() { return new float[0]; }
    // Frame snapshot methods removed

    private static void multiplyMatrices(float[] left, float[] right, float[] result) {
        for (int col = 0; col < 4; col++) {
            int colBase = col * 4;
            float r0 = right[colBase];
            float r1 = right[colBase + 1];
            float r2 = right[colBase + 2];
            float r3 = right[colBase + 3];
            result[colBase] = left[0] * r0 + left[4] * r1 + left[8] * r2 + left[12] * r3;
            result[colBase + 1] = left[1] * r0 + left[5] * r1 + left[9] * r2 + left[13] * r3;
            result[colBase + 2] = left[2] * r0 + left[6] * r1 + left[10] * r2 + left[14] * r3;
            result[colBase + 3] = left[3] * r0 + left[7] * r1 + left[11] * r2 + left[15] * r3;
        }
    }
    public static int[] generateLodMeshGpu(int[] blocks, int chunkX, int chunkZ, int detail) {
        if (!libLoaded || blocks == null || blocks.length == 0) return new int[0];
        try {
            return rustGenerateLodMeshGpu(blocks, chunkX, chunkZ, detail);
        } catch (UnsatisfiedLinkError e) {
            return new int[0];
        }
    }
    public static synchronized long[] getMetrics(boolean reset) {
        long[] snapshot = metricsSnapshot;
        java.util.Arrays.fill(snapshot, 0L);
        if (!libLoaded) return snapshot;
        try {
            long[] metrics = rustGetMetrics(reset);
            if (metrics == null || metrics.length == 0) return snapshot;
            int len = Math.min(snapshot.length, metrics.length);
            System.arraycopy(metrics, 0, snapshot, 0, len);
            return snapshot;
        } catch (UnsatisfiedLinkError e) {
            return snapshot;
        }
    }

    /**
     * Returns lightweight local timing metrics collected on the Java side.
     * Array layout: [frustumCalls, frustumTotalNanos, particleCalls, particleTotalNanos, dhFusedCalls, dhFusedTotalNanos]
     */
    public static synchronized long[] getLocalTimingMetrics() {
        long[] out = new long[6];
        out[0] = frustumCalls.get();
        out[1] = frustumTotalNanos.get();
        out[2] = particleCalls.get();
        out[3] = particleTotalNanos.get();
        out[4] = dhFusedCalls.get();
        out[5] = dhFusedTotalNanos.get();
        return out;
    }
    // ─── DNS Cache Methods ──────────────────────────────────────────────────
    //
     // Resolves a hostname to an IP address using Rust's cached DNS resolver.
     // Results are cached permanently on disk to speed up repeated server list pings.
    public static void dnsResolve(String hostname) {
        if (!libLoaded || hostname == null || hostname.isEmpty()) return;
        try { rustDnsResolve(hostname); }
        catch (UnsatisfiedLinkError ignored) {
            // DNS resolution fallback
        }
    }
    //
     // Batch resolves multiple hostnames in parallel using Rust's rayon thread pool.
     // Much faster than sequential Java InetAddress.getByName() for server lists.
     // @return array of IPs (empty string for failed lookups), or empty array on error
    public static String[] dnsBatchResolve(String[] hostnames) {
        if (hostnames == null || hostnames.length == 0) return new String[0];
        if (libLoaded) {
            try {
                return rustDnsBatchResolve(hostnames);
            } catch (UnsatisfiedLinkError ignored) {
                // Fall through to Java resolver fallback.
            }
        }
        return dnsBatchResolveJava(hostnames);
    }

    private static String[] dnsBatchResolveJava(String[] hostnames) {
        // Reuse thread-local result array if possible, otherwise allocate fresh
        String[] results = new String[hostnames.length];  // safe: java.util doesn't offer a good pool here without complexity
        if (hostnames.length >= DNS_PARALLEL_THRESHOLD && Runtime.getRuntime().availableProcessors() > 2) {
            // Inline parallel loop avoids stream allocation + iterator overhead
            int cores = Runtime.getRuntime().availableProcessors();
            int chunkSize = (hostnames.length + cores - 1) / cores;
            java.util.concurrent.CompletableFuture<?>[] futures = new java.util.concurrent.CompletableFuture[cores];
            for (int c = 0; c < cores; c++) {
                final int start = c * chunkSize;
                final int end = Math.min(start + chunkSize, hostnames.length);
                futures[c] = java.util.concurrent.CompletableFuture.runAsync(() -> {
                    for (int i = start; i < end; i++) {
                        results[i] = resolveHostnameJava(hostnames[i]);
                    }
                });
            }
            java.util.concurrent.CompletableFuture.allOf(java.util.Arrays.copyOfRange(futures, 0, cores)).join();
            return results;
        }
        for (int i = 0; i < hostnames.length; i++) {
            results[i] = resolveHostnameJava(hostnames[i]);
        }
        return results;
    }

    private static String resolveHostnameJava(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            return "";
        }
        try {
            return java.net.InetAddress.getByName(hostname).getHostAddress();
        } catch (Exception ignored) {
            return "";
        }
    }
    //Clears the Rust DNS cache (memory + disk). // /
    public static void dnsCacheClear() {
        if (!libLoaded) return;
        try { rustDnsCacheClear(); }
        catch (UnsatisfiedLinkError ignored) {
            // DNS batch resolution fallback
        }
    }

    public static int dnsCacheSize() {
        if (!libLoaded) return 0;
        try { return rustDnsCacheSize(); }
        catch (UnsatisfiedLinkError e) { return 0; }
    }
    // ─── DNS Disk Persistence ────────────────────────────────────────────────
    private static final java.nio.file.Path DNS_CACHE_PATH =
        net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("rust-mc-dns-cache.json");
    //
     // Saves resolved hostname→IP pairs to disk so subsequent launches
     // can skip DNS lookups entirely. Called on world unload and game exit.
    public static void dnsCacheSave() {
        if (!libLoaded) return;
        try {
            String json = rustDnsCacheExport();
            if (json != null && !json.equals("{}")) {
                java.nio.file.Files.writeString(DNS_CACHE_PATH, json);
                RustMC.LOGGER.debug("[Rust-MC] DNS cache saved: {} entries", dnsCacheSize());
            }
        } catch (UnsatisfiedLinkError e) {
            RustMC.LOGGER.debug("[Rust-MC] DNS cache export not linked: {}", e.getMessage());
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to save DNS cache: {}", e.getMessage());
        }
    }
    //
     // Loads persisted hostname→IP pairs from disk into Rust's cache.
     // Called early at startup so the first server list open is instant.
    public static void dnsCacheLoad() {
        if (!libLoaded) return;
        try {
            if (java.nio.file.Files.exists(DNS_CACHE_PATH)) {
                String json = java.nio.file.Files.readString(DNS_CACHE_PATH);
                if (!json.isBlank()) {
                    rustDnsCacheImport(json);
                    RustMC.LOGGER.info("[Rust-MC] DNS cache loaded from disk ({} entries)", dnsCacheSize());
                }
            }
        } catch (UnsatisfiedLinkError e) {
            RustMC.LOGGER.debug("[Rust-MC] DNS cache import not linked: {}", e.getMessage());
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to load DNS cache: {}", e.getMessage());
        }
    }
}