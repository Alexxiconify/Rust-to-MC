package com.alexxiconify.rustmc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NativeBridge handles all communication between Java and the Rust native core via JNI.
 * <p>
 * Many wrapper methods appear "unused" in static analysis because they form the public API
 * surface for other mods (ImmediatelyFast, Distant Horizons, etc.) and our own mixins.
 * The Rust-side JNI functions are always kept in sync with these wrappers.
 */
@SuppressWarnings({"unused", "java:S1135"})
public class NativeBridge {
    private NativeBridge() {}

    public static final int CONTEXT_VANILLA = 0;
    public static final int CONTEXT_SODIUM = 1;
    public static final int CONTEXT_LUX = 2;
    public static final int CONTEXT_STARLIGHT = 3;

    private static boolean libLoaded;
    private static final AtomicBoolean noiseSeeded = new AtomicBoolean(false);
    
    // ── Debug Counters ──
    public static final java.util.concurrent.atomic.AtomicInteger frustumChecksThisFrame = new java.util.concurrent.atomic.AtomicInteger(0);

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
                Path cachedLib = cacheDir.resolve(libName + "-" + RustMC.class.getPackage().getImplementationVersion());

                if (!Files.exists(cachedLib)) {
                    try (java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName)) {
                        if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                        Files.copy(is, cachedLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
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

    // --- Native Methods ---
    private static native void rustNoiseInit(int seed);
    private static native void rustNoiseReset();
    private static native float rustFastInvSqrt(float x);
    private static native float rustSin(float x);
    private static native float rustCos(float x);
    private static native float rustSqrt(float x);
    private static native double rustAtan2(double y, double x);
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

    /**
     * Subverts Java-side chunk data parsing by offloading large byte buffers 
     * directly to Rust's optimized decoder (PumpkinMC style).
     */
    public static void processChunkData(byte[] buf, int chunkX, int chunkZ) {
        if (!libLoaded || buf == null) return;
        rustProcessChunkData(buf, buf.length, chunkX, chunkZ);
    }

    public static void requestMemoryCleanup() {
        if (libLoaded) rustRequestMemoryCleanup();
    }
    // Frustum state management
    private static native long rustFrustumCreate();
    private static native void rustFrustumUpdate(long ptr, float[] vpMatrix);
    private static native boolean rustIsOutsideFrustum(long ptr, double x, double y, double z, double radius);
    private static native int rustCullEntities(long ptr, double[] positions, int count, boolean[] results, float margin);
    private static native void rustFrustumDestroy(long ptr);
    
    private static long activeFrustum = 0;

    /** 
     * Updates the persistent Vanilla frustum in Rust's global context.
     * This avoids creating new frustum objects every frame.
     */
    public static void updateVanillaFrustum(float[] vpMatrix) {
        if (!libLoaded || vpMatrix == null || vpMatrix.length < 16) return;
        try { rustFrustumUpdate(0, vpMatrix); }
        catch (UnsatisfiedLinkError ignored) {}
    }

    /** 
     * Optimizes entity/particle culling by offloading frustum intersection checks to Rust.
     * Uses the persistent global frustum updated via 'updateVanillaFrustum'.
     */
    public static boolean isOutsideFrustum(double x, double y, double z, double radius) {
        frustumChecksThisFrame.incrementAndGet();
        if (!libLoaded) return false;
        try {
            return rustIsOutsideFrustum(0, x, y, z, radius);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    public static int cullEntities(double[] positions, boolean[] results) {
        if (positions == null || results == null) return 0;
        frustumChecksThisFrame.addAndGet(positions.length / 3);
        if (!libLoaded) return 0;
        try {
            float margin = com.alexxiconify.rustmc.compat.ImmediatelyFastCompat.getCullingDistanceMultiplier();
            return rustCullEntities(0, positions, positions.length / 3, results, margin);
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    /** 
     * Offloads heavy vertex transformations (EMF/ETF animations) to Rust.
     * Processes XYZ and Normal arrays in parallel.
     */
    public static void transformVertices(float[] vertices, float[] normals, float[] matrix) {
        if (!libLoaded || vertices == null || normals == null || matrix == null) return;
        rustTransformVertices(vertices, normals, matrix, vertices.length / 3);
    }

    public static void invokeMatrixMul(float[] left, float[] right, float[] result) {
        if (!libLoaded) return;
        rustMatrixMul(left, right, result);
    }

    private static native void rustProcessMapTexture(int[] pixels, int width, int height);
    private static native void rustProcessMapTexturePtr(long ptr, int width, int height);
    private static native void rustProcessAudio(float[] samples, int count, float volume, float pan);
    private static native void rustTransformVertices(float[] vertices, float[] normals, float[] matrix, int count);
    private static native void rustMatrixMul(float[] left, float[] right, float[] result);
    private static native int[] rustSampleBiomes(long seed, int x, int z, int width, int height);

    public static int[] sampleBiomes(long seed, int x, int z, int width, int height) {
        if (!libLoaded) return new int[width * height];
        return rustSampleBiomes(seed, x, z, width, height);
    }

    private static native void rustTickParticles(double[] positions, double[] velocities, int count, double gravity);
    /**
     * Parallelizes particle physics (gravity, velocity decay).
     * Ideal for mods that spawn thousands of environmental particles.
     */
    public static void tickParticles(double[] positions, double[] velocities, double gravity) {
        if (!libLoaded || positions == null || velocities == null || positions.length == 0) return;
        rustTickParticles(positions, velocities, positions.length / 3, gravity);
    }

    private static native void rustProcessSoundPhysics(float[] samples, int count, double distance, double occlusion);
    private static native int[] rustBlendBiomes(int[] biomeIds, int width, int height, int radius);

    /**
     * Offloads sound occlusion and reverb math to Rust.
     */
    public static void processSoundPhysics(float[] samples, double distance, double occlusion) {
        if (!libLoaded || samples == null) return;
        rustProcessSoundPhysics(samples, samples.length, distance, occlusion);
    }

    /**
     * Multi-threaded map texture processing.
     * Ideal for mods that render complex maps in item frames or UI.
     */
    public static void processMapTexture(int[] pixels, int width, int height) {
        if (!libLoaded || pixels == null || pixels.length == 0) return;
        rustProcessMapTexture(pixels, width, height);
    }

    /**
     * Zero-copy map texture processing using a direct memory pointer.
     */
    public static void processMapTexturePtr(long ptr, int width, int height) {
        if (!libLoaded || ptr == 0) return;
        rustProcessMapTexturePtr(ptr, width, height);
    }

    /**
     * SIMD Audio processing (Volume/Pan/Normalization).
     * Offloads sound buffer manipulation to Rust.
     */
    public static void processAudio(float[] samples, float volume, float pan) {
        if (!libLoaded || samples == null || samples.length == 0) return;
        rustProcessAudio(samples, samples.length, volume, pan);
    }

    /**
     * Multi-threaded biome blending (supports Better Biome Blend).
     */
    public static int[] blendBiomes(int[] biomeIds, int width, int height, int radius) {
        if (!libLoaded) return biomeIds;
        return rustBlendBiomes(biomeIds, width, height, radius);
    }

    private static native void rustFrustumSetFovScale(long ptr, double fovScale);
    private static native boolean rustFrustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin);
    private static native byte[] rustBatchFrustumTest(long ptr, double[] aabbs, int count, double margin);

    /**
     * Conservative frustum test with margin (useful for DH chunks/LODs).
     */
    public static boolean frustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin) {
        frustumChecksThisFrame.incrementAndGet();
        if (!libLoaded) return true;
        return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin);
    }

    /**
     * Batch frustum test with margin.
     */
    public static byte[] batchFrustumTest(long ptr, double[] aabbs, double margin) {
        if (aabbs == null) return new byte[0];
        frustumChecksThisFrame.addAndGet(aabbs.length / 6);
        if (!libLoaded) return new byte[0];
        return rustBatchFrustumTest(ptr, aabbs, aabbs.length / 6, margin);
    }

    private static native boolean rustDHCull(double minY, double maxY, double surfaceY);
    private static native void rustSetCaveStatus(boolean inCave);
    private static native float rustGetAvgFps();
    private static native float rustClamp(float value, float min, float max);
    private static native double rustLerp(double delta, double start, double end);
    private static native double rustAbsMax(double a, double b);
    private static native float rustWrapDegrees(float value);
    @SuppressWarnings("java:S107")
    private static native boolean rustRayIntersectsBox(double rx, double ry, double rz, double dx, double dy, double dz, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native float[] rustComputeAmbientOcclusion(float[] vertexData, int vertexCount);
    private static native float[] rustComputeAmbientOcclusionDirect(java.nio.ByteBuffer vertexData, int vertexCount);
    private static native void rustAddFrameTime(long nanos);
    private static native float[] rustGetFrameHistory();
    // DNS cache
    private static native String rustDnsResolve(String hostname);
    private static native String[] rustDnsBatchResolve(String[] hostnames);
    private static native void rustDnsCacheClear();
    private static native int rustDnsCacheSize();
    private static native String rustDnsCacheExport();
    private static native void rustDnsCacheImport(String json);
    private static native int rustInflateRaw(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int outputMaxLen);

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

    public static float fastInvSqrt(float x) {
        if (!libLoaded) return 1.0f / (float) Math.sqrt(x);
        try { return rustFastInvSqrt(x); }
        catch (UnsatisfiedLinkError e) { return 1.0f / (float) Math.sqrt(x); }
    }

    public static float invokeSin(float x) {
        if (!libLoaded) return (float) Math.sin(x);
        try { return rustSin(x); }
        catch (UnsatisfiedLinkError e) { return (float) Math.sin(x); }
    }

    public static float invokeCos(float x) {
        if (!libLoaded) return (float) Math.cos(x);
        try { return rustCos(x); }
        catch (UnsatisfiedLinkError e) { return (float) Math.cos(x); }
    }

    public static float invokeSqrt(float x) {
        if (!libLoaded) return (float) Math.sqrt(x);
        try { return rustSqrt(x); }
        catch (UnsatisfiedLinkError e) { return (float) Math.sqrt(x); }
    }

    public static double invokeAtan2(double y, double x) {
        if (!libLoaded) return Math.atan2(y, x);
        try { return rustAtan2(y, x); }
        catch (UnsatisfiedLinkError e) { return Math.atan2(y, x); }
    }

    public static float invokeClamp(float value, float min, float max) {
        if (!libLoaded) return Math.clamp(value, min, max);
        try { return rustClamp(value, min, max); }
        catch (UnsatisfiedLinkError e) { return Math.clamp(value, min, max); }
    }

    public static double invokeLerp(double delta, double start, double end) {
        if (!libLoaded) return start + delta * (end - start);
        try { return rustLerp(delta, start, end); }
        catch (UnsatisfiedLinkError e) { return start + delta * (end - start); }
    }

    public static double invokeAbsMax(double a, double b) {
        double max = Math.max ( Math.abs ( a ) , Math.abs ( b ) );
        if (!libLoaded) return max;
        try { return rustAbsMax(a, b); }
        catch (UnsatisfiedLinkError e) { return max; }
    }

    public static float invokeWrapDegrees(float value) {
        if (!libLoaded) {
            float v = value % 360.0f;
            if (v >= 180.0f) v -= 360.0f;
            else if (v < -180.0f) v += 360.0f;
            return v;
        }
        try { return rustWrapDegrees(value); }
        catch (UnsatisfiedLinkError e) { 
            float v = value % 360.0f;
            if (v >= 180.0f) v -= 360.0f;
            else if (v < -180.0f) v += 360.0f;
            return v;
        }
    }

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
        } catch (UnsatisfiedLinkError e) { /* fallback */ }
    }

    public static int propagateLightBulk(int[] data, int len) {
        if (!libLoaded) return -1;
        int context = CONTEXT_VANILLA;
        
        if (ModBridge.SCALABLELUX) {
            context = CONTEXT_LUX;
        } else if (ModBridge.STARLIGHT) {
            context = CONTEXT_STARLIGHT;
        } else if (ModBridge.SODIUM) {
            context = CONTEXT_SODIUM;
        }

        try { return rustPropagateLightBulk(data, len, context); }
        catch (UnsatisfiedLinkError e) { return -1; }
    }

    public static int propagateLightDH(long[] tasks, int len) {
        if (!libLoaded) return -1;
        try { return rustPropagateLightDH(tasks, len); }
        catch (UnsatisfiedLinkError e) { return -1; }
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

    // Reusable arrays for pathfinding — avoids per-call allocation.
    // Safe because pathfinding is only called from the server tick thread.
    private static final int[] PATH_START = new int[3];
    private static final int[] PATH_END = new int[3];

    public static int findPathRaw(int sx, int sy, int sz, int ex, int ey, int ez) {
        if (!libLoaded) return -1;
        try {
            PATH_START[0] = sx; PATH_START[1] = sy; PATH_START[2] = sz;
            PATH_END[0] = ex; PATH_END[1] = ey; PATH_END[2] = ez;
            return rustFindPath(PATH_START, PATH_END);
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

    @SuppressWarnings("unused")
    public static int invokeFrustumIntersect(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return -1; // Fallback to Vanilla for now since stateful frustums are only implemented for DH
    }

    public static long createRustFrustum() {
        if (!libLoaded) return 0;
        try { return rustFrustumCreate(); }
        catch (UnsatisfiedLinkError e) { return 0; }
    }

    public static void updateRustFrustum(long ptr, float[] vpMatrix) {
        if (!libLoaded || ptr == 0 || vpMatrix == null || vpMatrix.length < 16) return;
        try { rustFrustumUpdate(ptr, vpMatrix); }
        catch (UnsatisfiedLinkError ignored) { /* Optional native method */ }
    }

    public static void setRustFrustumFovScale(long ptr, double fovScale) {
        if (!libLoaded || ptr == 0) return;
        try { rustFrustumSetFovScale(ptr, fovScale); }
        catch (UnsatisfiedLinkError ignored) { /* Optional native method */ }
    }

    public static boolean testRustFrustum(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return testRustFrustum(ptr, minX, minY, minZ, maxX, maxY, maxZ, 0.0);
    }

    public static boolean testRustFrustum(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double margin) {
        frustumChecksThisFrame.incrementAndGet();
        if (!libLoaded || ptr == 0) return true; // Default to visible if Rust is not available or ptr is 0
        try { return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ, margin); }
        catch (UnsatisfiedLinkError e) { return true; }
    }

    public static void destroyRustFrustum(long ptr) {
        if (!libLoaded || ptr == 0) return;
        try { rustFrustumDestroy(ptr); }
        catch (UnsatisfiedLinkError ignored) { /* Optional native method */ }
    }

    /** Tests multiple AABBs in one JNI call. aabbs is flat [minX,minY,minZ,maxX,maxY,maxZ,...]. */
    public static byte[] batchFrustumTest(long ptr, double[] aabbs, int count) {
        return batchFrustumTest(ptr, aabbs, count, 0.0);
    }

    public static byte[] batchFrustumTest(long ptr, double[] aabbs, int count, double margin) {
        frustumChecksThisFrame.addAndGet(count);
        if (!libLoaded || ptr == 0 || aabbs == null || count <= 0) {
            byte[] all = new byte[count];
            java.util.Arrays.fill(all, (byte) 1);
            return all;
        }
        try { return rustBatchFrustumTest(ptr, aabbs, count, margin); }
        catch (UnsatisfiedLinkError e) {
            byte[] all = new byte[count];
            java.util.Arrays.fill(all, (byte) 1);
            return all;
        }
    }

    public static boolean invokeDHCull(double minY, double maxY, double surfaceY) {
        if (!libLoaded) return true;
        try { return rustDHCull(minY, maxY, surfaceY); }
        catch (UnsatisfiedLinkError e) { return true; }
    }

    public static void updateCaveStatus(boolean inCave) {
        if (!libLoaded) return;
        try { rustSetCaveStatus(inCave); }
        catch (UnsatisfiedLinkError ignored) { }
    }

    /** Returns smoothed avg FPS from the Rust frame-time ring buffer (240-frame window). */
    public static float invokeGetAvgFps() {
        if (!libLoaded) return 0;
        try { return rustGetAvgFps(); }
        catch (UnsatisfiedLinkError e) { return 0; }
    }

    @SuppressWarnings("java:S107")
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

    public static void invokeAddFrameTime(long nanos) {
        if (!libLoaded) return;
        try {
            rustAddFrameTime(nanos);
        } catch (UnsatisfiedLinkError ignored) {
            // Optional native method
        }
    }

    public static float[] invokeGetFrameHistory() {
        if (!libLoaded) return new float[0];
        try {
            return rustGetFrameHistory();
        } catch (UnsatisfiedLinkError e) {
            return new float[0];
        }
    }

    // ─── DNS Cache Methods ──────────────────────────────────────────────────

    /**
     * Resolves a hostname to an IP address using Rust's cached DNS resolver.
     * Results are cached for 5 minutes to speed up repeated server list pings.
     * @return resolved IP, or null if resolution fails or native is unavailable
     */
    public static String dnsResolve(String hostname) {
        if (!libLoaded || hostname == null || hostname.isEmpty()) return null;
        try { return rustDnsResolve(hostname); }
        catch (UnsatisfiedLinkError e) { return null; }
    }

    /**
     * Batch resolves multiple hostnames in parallel using Rust's rayon thread pool.
     * Much faster than sequential Java InetAddress.getByName() for server lists.
     * @return array of IPs (empty string for failed lookups), or empty array on error
     */
    public static String[] dnsBatchResolve(String[] hostnames) {
        if (!libLoaded || hostnames == null || hostnames.length == 0) return new String[0];
        try { return rustDnsBatchResolve(hostnames); }
        catch (UnsatisfiedLinkError e) { return new String[0]; }
    }

    /** Clears the Rust DNS cache (memory + disk). */
    public static void dnsCacheClear() {
        if (!libLoaded) return;
        try { rustDnsCacheClear(); }
        catch (UnsatisfiedLinkError ignored) { /* optional */ }
    }

    /** Returns the number of cached DNS entries. */
    public static int dnsCacheSize() {
        if (!libLoaded) return 0;
        try { return rustDnsCacheSize(); }
        catch (UnsatisfiedLinkError e) { return 0; }
    }

    // ─── DNS Disk Persistence ────────────────────────────────────────────────

    private static final java.nio.file.Path DNS_CACHE_PATH =
        net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("rust-mc-dns-cache.json");

    /**
     * Saves resolved hostname→IP pairs to disk so subsequent launches
     * can skip DNS lookups entirely. Called on world unload and game exit.
     */
    public static void dnsCacheSave() {
        if (!libLoaded) return;
        try {
            String json = rustDnsCacheExport();
            if (json != null && !json.equals("{}")) {
                java.nio.file.Files.writeString(DNS_CACHE_PATH, json);
                RustMC.LOGGER.debug("[Rust-MC] DNS cache saved: {} entries", dnsCacheSize());
            }
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to save DNS cache: {}", e.getMessage());
        }
    }

    /**
     * Loads persisted hostname→IP pairs from disk into Rust's cache.
     * Called early at startup so the first server list open is instant.
     */
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
        } catch (Exception e) {
            RustMC.LOGGER.debug("[Rust-MC] Failed to load DNS cache: {}", e.getMessage());
        }
    }
}