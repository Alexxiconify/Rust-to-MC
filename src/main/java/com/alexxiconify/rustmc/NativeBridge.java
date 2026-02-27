package com.alexxiconify.rustmc;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("preview")
public class NativeBridge {
    private NativeBridge() {}

    private static final Linker LINKER = Linker.nativeLinker();
    private static SymbolLookup LOOKUP = null;

    public static MethodHandle FAST_INV_SQRT = null;
    public static MethodHandle SIN = null;
    public static MethodHandle COS = null;
    public static MethodHandle SQRT = null;

    public static MethodHandle COMPRESS = null;
    public static MethodHandle DECOMPRESS = null;
    public static MethodHandle PROCESS_PACKET = null;

    public static MethodHandle NOISE_2D = null;
    public static MethodHandle PROPAGATE_LIGHT_BULK = null;
    public static MethodHandle FIND_PATH = null;
    public static MethodHandle EXECUTE_COMMAND = null;

    private static boolean libLoaded = false;

    public static boolean isReady() { return libLoaded; }

    static {
        try {
            String libName = System.mapLibraryName("rust_mc_core");
            Path devPath = Paths.get("rust_mc_core/target/release/" + libName).toAbsolutePath();

            if (java.nio.file.Files.exists(devPath)) {
                System.load(devPath.toString());
            } else {
                java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName);
                if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                Path tmp = java.nio.file.Files.createTempFile("rust_mc_", "_" + libName);
                java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(tmp.toString());
                tmp.toFile().deleteOnExit();
            }

            LOOKUP = SymbolLookup.loaderLookup();

            FAST_INV_SQRT = createHandle("rust_fast_inv_sqrt", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            SIN = createHandle("rust_sin", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            COS = createHandle("rust_cos", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));
            SQRT = createHandle("rust_sqrt", FunctionDescriptor.of(ValueLayout.JAVA_FLOAT, ValueLayout.JAVA_FLOAT));

            COMPRESS = createHandle("rust_compress", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            DECOMPRESS = createHandle("rust_decompress", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            PROCESS_PACKET = createHandle("rust_process_packet", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            NOISE_2D = createHandle("rust_noise_2d", FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE));
            PROPAGATE_LIGHT_BULK = createHandle("rust_propagate_light_bulk", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            FIND_PATH = createHandle("rust_find_path", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            EXECUTE_COMMAND = createHandle("rust_execute_command", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

            libLoaded = true;
            RustMC.LOGGER.info("[Rust-MC] Native library loaded successfully.");
        } catch (Throwable t) {
            libLoaded = false;
            // Use System.err here since RustMC.LOGGER may not exist yet during early class-init
            System.err.println("[Rust-MC] WARNING: Failed to load native library – all Rust optimizations disabled. Cause: " + t.getMessage());
        }
    }

    private static MethodHandle createHandle(String name, FunctionDescriptor desc) {
        if (LOOKUP == null) return null;
        return LOOKUP.find(name)
                .map(addr -> LINKER.downcallHandle(addr, desc))
                .orElse(null);
    }

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

    public static int invokeCompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) COMPRESS.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }
    public static int invokeDecompress(MemorySegment in, int inL, MemorySegment out, int outL) {
        try { return (int) DECOMPRESS.invokeExact(in, inL, out, outL); }
        catch (Throwable t) { return -1; }
    }

    public static double noise2d(double x, double y) {
        try { return (double) NOISE_2D.invokeExact(x, y); }
        catch (Throwable t) { return 0.0; }
    }
    public static int executeCommand(MemorySegment cmd, int len) {
        try { return (int) EXECUTE_COMMAND.invokeExact(cmd, len); }
        catch (Throwable t) { return -1; }
    }

    public static int propagateLightBulk(MemorySegment data, int len) {
        try { return (int) PROPAGATE_LIGHT_BULK.invokeExact(data, len); }
        catch (Throwable t) { return -1; }
    }
    public static int findPath(MemorySegment start, MemorySegment end, MemorySegment world, int limit) {
        try { return (int) FIND_PATH.invokeExact(start, end, world, limit); }
        catch (Throwable t) { return -1; }
    }

    public static int findPathRaw(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        if (FIND_PATH == null) return -1;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment start = arena.allocateFrom(ValueLayout.JAVA_INT, startX, startY, startZ);
            MemorySegment end   = arena.allocateFrom(ValueLayout.JAVA_INT, endX,   endY,   endZ);
            // Pass a minimal dummy world segment (Rust only uses it if non-null in future work)
            MemorySegment world = arena.allocate(ValueLayout.JAVA_INT, 1);
            return (int) FIND_PATH.invokeExact(start, end, world, 0);
        } catch (Throwable t) { return -1; }
    }
}
