package com.alexxiconify.rustmc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NativeBridge handles all communication between Java and the Rust native core via JNI.
 */
public class NativeBridge {
    private NativeBridge() {}

    private static boolean libLoaded;
    private static final AtomicBoolean noiseSeeded = new AtomicBoolean(false);

    public static boolean isReady() { return libLoaded; }

    static {
        try {
            String libName = System.mapLibraryName("rust_mc_core");
            Path devPath = Paths.get("rust_mc_core/target/release/" + libName).toAbsolutePath();

            if (Files.exists(devPath)) {
                System.load(devPath.toString());
            } else {
                try (java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName)) {
                    if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                    Path tmpLib = Files.createTempFile("rust_mc_" + System.currentTimeMillis() + "_", "_" + libName);
                    Files.copy(is, tmpLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmpLib.toString());

                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try { Files.deleteIfExists(tmpLib); } catch (Exception ignored) {
                            // Best effort cleanup during shutdown
                        }
                    }, "rust-mc-tmplib-cleanup"));
                }
            }
            libLoaded = true;
            RustMC.LOGGER.info("[Rust-MC] Native library loaded successfully via JNI.");
        } catch (Exception t) {
            libLoaded = false;
            RustMC.LOGGER.error("[Rust-MC] WARNING: Failed to load native library – all Rust optimizations disabled. Cause: {}", t.getMessage());
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
    private static native int rustPropagateLightBulk(int[] data, int count);
    private static native byte[] rustCompress(byte[] input);
    private static native int[] rustGenerateGhostMap(double centerX, double centerZ, int size, double scale);
    private static native byte[] rustDecompress(byte[] input, int maxOutputSize);
    private static native int rustFindPath(int[] start, int[] end);
    private static native int rustExecuteCommand(byte[] cmd);
    private static native int rustProcessPacket(byte[] buf, int len);
    // Frustum state management
    private static native long rustFrustumCreate();
    private static native void rustFrustumUpdate(long ptr, float[] vpMatrix);
    private static native boolean rustFrustumTest(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    private static native void rustFrustumDestroy(long ptr);
    private static native float rustClamp(float value, float min, float max);
    private static native double rustLerp(double delta, double start, double end);
    private static native double rustAbsMax(double a, double b);
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
        try { return rustPropagateLightBulk(data, len); }
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

    public static byte[] invokeDecompress(byte[] input, int maxOutputSize) {
        if (!libLoaded) return new byte[0];
        try { return rustDecompress(input, maxOutputSize); }
        catch (UnsatisfiedLinkError e) { return new byte[0]; }
    }

    public static int findPathRaw(int sx, int sy, int sz, int ex, int ey, int ez) {
        if (!libLoaded) return -1;
        try { return rustFindPath(new int[]{sx, sy, sz}, new int[]{ex, ey, ez}); }
        catch (UnsatisfiedLinkError e) { return -1; }
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

    public static boolean testRustFrustum(long ptr, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!libLoaded || ptr == 0) return true; // Default to visible if Rust is not available or ptr is 0
        try { return rustFrustumTest(ptr, minX, minY, minZ, maxX, maxY, maxZ); }
        catch (UnsatisfiedLinkError e) { return true; }
    }

    public static void destroyRustFrustum(long ptr) {
        if (!libLoaded || ptr == 0) return;
        try { rustFrustumDestroy(ptr); }
        catch (UnsatisfiedLinkError ignored) { /* Optional native method */ }
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

    /** Clears the Rust DNS cache. */
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
}