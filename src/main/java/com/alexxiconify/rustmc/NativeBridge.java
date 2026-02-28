package com.alexxiconify.rustmc;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("preview")
public class NativeBridge {
    private NativeBridge() {}

    private static final Linker LINKER = Linker.nativeLinker();
    private static SymbolLookup lookup = null;

    // Math
    private static MethodHandle fastInvSqrt = null;
    private static MethodHandle sin            = null;
    private static MethodHandle cos            = null;
    private static MethodHandle sqrt           = null;
    private static MethodHandle tan            = null;
    private static MethodHandle atan2          = null;
    private static MethodHandle floor          = null;

    // Network
    private static MethodHandle compress       = null;
    private static MethodHandle decompress     = null;
    private static MethodHandle processPacket = null;

    // World-gen / Noise
    private static MethodHandle noiseInit     = null;
    private static MethodHandle noise2d       = null;
    private static MethodHandle noise3d       = null;

    // Lighting / Pathfinding / Commands
    private static MethodHandle propagateLightBulk = null;
    private static MethodHandle findPath            = null;
    private static MethodHandle executeCommand      = null;
    private static MethodHandle frustumIntersect    = null;
    private static MethodHandle getSystemMemory     = null;

    /** Shared confined arena for hot-path allocations (single-threaded callers). */
    public static final Arena SHARED_ARENA = Arena.ofShared();

    private static boolean libLoaded = false;
    private static final AtomicBoolean noiseSeeded = new AtomicBoolean(false);

    public static boolean isReady() { return libLoaded; }

    static {
        Path tmpLib = null;
        try {
            String libName = System.mapLibraryName("rust_mc_core");
            Path devPath = Paths.get("rust_mc_core/target/release/" + libName).toAbsolutePath();

            if (Files.exists(devPath)) {
                System.load(devPath.toString());
            } else {
                try (java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName)) {
                    if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                    tmpLib = Files.createTempFile("rust_mc_", "_" + libName);
                    Files.copy(is, tmpLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmpLib.toString());
                }
            }

            lookup = SymbolLookup.loaderLookup();

            fastInvSqrt = createHandle("rust_fast_inv_sqrt",
                    FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            sin  = createHandle("rust_sin",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            cos  = createHandle("rust_cos",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            sqrt = createHandle("rust_sqrt", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            tan  = createHandle("rust_tan",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            atan2 = createHandle("rust_atan2", FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
            floor = createHandle("rust_floor", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE));

            compress = createHandle("rust_compress",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            decompress = createHandle("rust_decompress",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            processPacket = createHandle("rust_process_packet",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            noiseInit = createHandle("rust_noise_init",
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            noise2d = createHandle("rust_noise_2d",
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
            noise3d = createHandle("rust_noise_3d",
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));

            propagateLightBulk = createHandle("rust_propagate_light_bulk",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            findPath = createHandle("rust_find_path",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            executeCommand = createHandle("rust_execute_command",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            frustumIntersect = createHandle("rust_frustum_intersect",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE)); // Added frustumIntersect lookup
            getSystemMemory = createHandle("rust_get_system_memory",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            libLoaded = true;
            RustMC.LOGGER.info("[Rust-MC] Native library loaded successfully.");
        } catch (Exception t) {
            libLoaded = false;
            RustMC.LOGGER.error("[Rust-MC] WARNING: Failed to load native library – all Rust optimizations disabled. Cause: {}", t.getMessage());
        }

        // Register a shutdown hook to clean up the temp file even on abnormal exit
        if (tmpLib != null) {
            final Path finalTmp = tmpLib;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { Files.deleteIfExists(finalTmp); } catch (Exception ignored) { /* Ignore during shutdown */ }
            }, "rust-mc-tmplib-cleanup"));
        }

        // Register shutdown hook to close shared arena
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { SHARED_ARENA.close(); } catch (Exception ignored) { /* Ignore during shutdown */ }
        }, "rust-mc-arena-close"));
    }

    private static MethodHandle createHandle(String name, FunctionDescriptor desc) {
        if (lookup == null) return null;
        return lookup.find(name)
                .map(addr -> LINKER.downcallHandle(addr, desc))
                .orElse(null);
    }

    // ── Noise ──────────────────────────────────────────────────────────────────
    /**
     * Seeds the Rust Simplex noise generator with the world seed.
     * Only the first call per process takes effect (OnceLock in Rust).
     * Call at world load time.
     */
    public static void noiseInit(long mcSeed) {
        if (!libLoaded || noiseInit == null) return;
        if (!noiseSeeded.compareAndSet(false, true)) return; // only seed once
        try {
            noiseInit.invokeExact((int) (mcSeed & 0xFFFFFFFFL));
        } catch (Throwable t) {
            RustMC.LOGGER.warn("[Rust-MC] Failed to seed noise: {}", t.getMessage());
        }
    }

    /** Resets the seed-once flag so the next world load can re-seed. */
    public static void noiseReset() {
        noiseSeeded.set(false);
    }

    // ── Math helpers ───────────────────────────────────────────────────────────
    public static float fastInvSqrt(float x) {
        try { return (float) fastInvSqrt.invokeExact(x); }
        catch (Throwable t) { return 1.0f / (float) Math.sqrt(x); }
    }
    public static float invokeSin(float x) {
        try { return (float) sin.invokeExact(x); }
        catch (Throwable t) { return (float) Math.sin(x); }
    }
    public static float invokeCos(float x) {
        try { return (float) cos.invokeExact(x); }
        catch (Throwable t) { return (float) Math.cos(x); }
    }
    public static float invokeSqrt(float x) {
        try { return (float) sqrt.invokeExact(x); }
        catch (Throwable t) { return (float) Math.sqrt(x); }
    }
    public static float invokeTan(float x) {
        try { return (float) tan.invokeExact(x); }
        catch (Throwable t) { return (float) Math.tan(x); }
    }
    public static double invokeAtan2(double y, double x) {
        try { return (double) atan2.invokeExact(y, x); }
        catch (Throwable t) { return Math.atan2(y, x); }
    }
    public static int invokeFloor(double x) {
        try { return (int) floor.invokeExact(x); }
        catch (Throwable t) { 
            int xi = (int) x;
            return x < xi ? xi - 1 : xi;
        }
    }

    // ── Compression helpers ────────────────────────────────────────────────────
    public static int invokeCompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) compress.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }
    public static int invokeDecompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) decompress.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }
    public static int invokeProcessPacket(MemorySegment in, int len) {
        try { return (int) processPacket.invokeExact(in, len); }
        catch (Throwable t) { return -1; }
    }

    public static int invokeFrustumIntersect(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        try { return (int) frustumIntersect.invokeExact(minX, minY, minZ, maxX, maxY, maxZ); }
        catch (Throwable t) { return -1; }
    }

    // ── Noise helpers ──────────────────────────────────────────────────────────
    public static double noise2d(double x, double y) {
        try { return (double) noise2d.invokeExact(x, y); }
        catch (Throwable t) { return 0.0; }
    }
    public static double noise3d(double x, double y, double z) {
        try { return (double) noise3d.invokeExact(x, y, z); }
        catch (Throwable t) { return 0.0; }
    }

    // ── Lighting / Pathfinding / Command helpers ───────────────────────────────
    public static int propagateLightBulk(MemorySegment data, int len) {
        try { return (int) propagateLightBulk.invokeExact(data, len); }
        catch (Throwable t) { return -1; }
    }
    public static int findPath(MemorySegment start, MemorySegment end, MemorySegment world, int limit) {
        try { return (int) findPath.invokeExact(start, end, world, limit); }
        catch (Throwable t) { return -1; }
    }
    public static int executeCommand(MemorySegment cmd, int len) {
        try { return (int) executeCommand.invokeExact(cmd, len); }
        catch (Throwable t) { return -1; }
    }

    public static void getSystemMemory(MemorySegment outTotal, MemorySegment outUsed) {
        if (!libLoaded || getSystemMemory == null) return;
        try { getSystemMemory.invokeExact(outTotal, outUsed); }
        catch (Throwable ignored) { /* Native call failed */ }
    }

    /**
     * Convenience: allocates start/end i32[3] segments on the confined arena,
     * calls rust_find_path, and returns the path length (0 = at target, −1 = error).
     */
    public static int findPathRaw(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        if (findPath == null) return -1;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment start = arena.allocate(ValueLayout.JAVA_INT, 3);
            start.setAtIndex(ValueLayout.JAVA_INT, 0, startX);
            start.setAtIndex(ValueLayout.JAVA_INT, 1, startY);
            start.setAtIndex(ValueLayout.JAVA_INT, 2, startZ);
            
            MemorySegment end = arena.allocate(ValueLayout.JAVA_INT, 3);
            end.setAtIndex(ValueLayout.JAVA_INT, 0, endX);
            end.setAtIndex(ValueLayout.JAVA_INT, 1, endY);
            end.setAtIndex(ValueLayout.JAVA_INT, 2, endZ);
            MemorySegment world = arena.allocate(ValueLayout.JAVA_INT, 1); // stub – future: pass block grid
            return (int) findPath.invokeExact(start, end, world, 0);
        } catch (Throwable t) { return -1; }
    }
}
