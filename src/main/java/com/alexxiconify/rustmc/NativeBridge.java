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
    private static SymbolLookup LOOKUP = null;

    // Math
    public static MethodHandle FAST_INV_SQRT = null;
    public static MethodHandle SIN            = null;
    public static MethodHandle COS            = null;
    public static MethodHandle SQRT           = null;
    public static MethodHandle TAN            = null;
    public static MethodHandle ATAN2          = null;
    public static MethodHandle FLOOR          = null;

    // Network
    public static MethodHandle COMPRESS       = null;
    public static MethodHandle DECOMPRESS     = null;
    public static MethodHandle PROCESS_PACKET = null;

    // World-gen / Noise
    public static MethodHandle NOISE_INIT     = null;
    public static MethodHandle NOISE_2D       = null;
    public static MethodHandle NOISE_3D       = null;

    // Lighting / Pathfinding / Commands
    public static MethodHandle PROPAGATE_LIGHT_BULK = null;
    public static MethodHandle FIND_PATH            = null;
    public static MethodHandle EXECUTE_COMMAND      = null;

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

            LOOKUP = SymbolLookup.loaderLookup();

            FAST_INV_SQRT = createHandle("rust_fast_inv_sqrt",
                    FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            SIN  = createHandle("rust_sin",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            COS  = createHandle("rust_cos",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            SQRT = createHandle("rust_sqrt", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            TAN  = createHandle("rust_tan",  FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            ATAN2 = createHandle("rust_atan2", FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
            FLOOR = createHandle("rust_floor", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_DOUBLE));

            COMPRESS = createHandle("rust_compress",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            DECOMPRESS = createHandle("rust_decompress",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            PROCESS_PACKET = createHandle("rust_process_packet",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            NOISE_INIT = createHandle("rust_noise_init",
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
            NOISE_2D = createHandle("rust_noise_2d",
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
            NOISE_3D = createHandle("rust_noise_3d",
                    FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));

            PROPAGATE_LIGHT_BULK = createHandle("rust_propagate_light_bulk",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            FIND_PATH = createHandle("rust_find_path",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            EXECUTE_COMMAND = createHandle("rust_execute_command",
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            libLoaded = true;
            RustMC.LOGGER.info("[Rust-MC] Native library loaded successfully.");
        } catch (Throwable t) {
            libLoaded = false;
            System.err.println("[Rust-MC] WARNING: Failed to load native library – all Rust optimizations disabled. Cause: " + t.getMessage());
        }

        // Register a shutdown hook to clean up the temp file even on abnormal exit
        if (tmpLib != null) {
            final Path finalTmp = tmpLib;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { Files.deleteIfExists(finalTmp); } catch (Exception ignored) {}
            }, "rust-mc-tmplib-cleanup"));
        }

        // Register shutdown hook to close shared arena
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { SHARED_ARENA.close(); } catch (Exception ignored) {}
        }, "rust-mc-arena-close"));
    }

    private static MethodHandle createHandle(String name, FunctionDescriptor desc) {
        if (LOOKUP == null) return null;
        return LOOKUP.find(name)
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
        if (!libLoaded || NOISE_INIT == null) return;
        if (!noiseSeeded.compareAndSet(false, true)) return; // only seed once
        try {
            NOISE_INIT.invokeExact((int) (mcSeed & 0xFFFFFFFFL));
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
        try { return (float) FAST_INV_SQRT.invokeExact(x); }
        catch (Throwable t) { return 1.0f / (float) Math.sqrt(x); }
    }
    public static float invokeSin(float x) {
        try { return (float) SIN.invokeExact(x); }
        catch (Throwable t) { return (float) Math.sin(x); }
    }
    public static float invokeCos(float x) {
        try { return (float) COS.invokeExact(x); }
        catch (Throwable t) { return (float) Math.cos(x); }
    }
    public static float invokeSqrt(float x) {
        try { return (float) SQRT.invokeExact(x); }
        catch (Throwable t) { return (float) Math.sqrt(x); }
    }
    public static float invokeTan(float x) {
        try { return (float) TAN.invokeExact(x); }
        catch (Throwable t) { return (float) Math.tan(x); }
    }
    public static double invokeAtan2(double y, double x) {
        try { return (double) ATAN2.invokeExact(y, x); }
        catch (Throwable t) { return Math.atan2(y, x); }
    }
    public static int invokeFloor(double x) {
        try { return (int) FLOOR.invokeExact(x); }
        catch (Throwable t) { 
            int xi = (int) x;
            return x < (double) xi ? xi - 1 : xi;
        }
    }

    // ── Compression helpers ────────────────────────────────────────────────────
    public static int invokeCompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) COMPRESS.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }
    public static int invokeDecompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) DECOMPRESS.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }
    public static int invokeProcessPacket(MemorySegment in, int len) {
        try { return (int) PROCESS_PACKET.invokeExact(in, len); }
        catch (Throwable t) { return -1; }
    }

    // ── Noise helpers ──────────────────────────────────────────────────────────
    public static double noise2d(double x, double y) {
        try { return (double) NOISE_2D.invokeExact(x, y); }
        catch (Throwable t) { return 0.0; }
    }
    public static double noise3d(double x, double y, double z) {
        try { return (double) NOISE_3D.invokeExact(x, y, z); }
        catch (Throwable t) { return 0.0; }
    }

    // ── Lighting / Pathfinding / Command helpers ───────────────────────────────
    public static int propagateLightBulk(MemorySegment data, int len) {
        try { return (int) PROPAGATE_LIGHT_BULK.invokeExact(data, len); }
        catch (Throwable t) { return -1; }
    }
    public static int findPath(MemorySegment start, MemorySegment end, MemorySegment world, int limit) {
        try { return (int) FIND_PATH.invokeExact(start, end, world, limit); }
        catch (Throwable t) { return -1; }
    }
    public static int executeCommand(MemorySegment cmd, int len) {
        try { return (int) EXECUTE_COMMAND.invokeExact(cmd, len); }
        catch (Throwable t) { return -1; }
    }

    /**
     * Convenience: allocates start/end i32[3] segments on the confined arena,
     * calls rust_find_path, and returns the path length (0 = at target, −1 = error).
     */
    public static int findPathRaw(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        if (FIND_PATH == null) return -1;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment start = arena.allocateFrom(ValueLayout.JAVA_INT, startX, startY, startZ);
            MemorySegment end   = arena.allocateFrom(ValueLayout.JAVA_INT, endX,   endY,   endZ);
            MemorySegment world = arena.allocate(ValueLayout.JAVA_INT, 1); // stub – future: pass block grid
            return (int) FIND_PATH.invokeExact(start, end, world, 0);
        } catch (Throwable t) { return -1; }
    }
}
