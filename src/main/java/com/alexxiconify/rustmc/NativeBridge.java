package com.alexxiconify.rustmc;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("preview")
public class NativeBridge {
    private NativeBridge() {} // Private constructor to hide implicit public one
    private static final SymbolLookup LOOKUP;
    private static final Linker LINKER = Linker.nativeLinker();

    public static final MethodHandle FAST_INV_SQRT;
    public static final MethodHandle SIN;
    public static final MethodHandle COS;
    public static final MethodHandle SQRT;

    public static final MethodHandle COMPRESS;
    public static final MethodHandle DECOMPRESS;
    public static final MethodHandle PROCESS_PACKET;

    public static final MethodHandle NOISE_2D;
    public static final MethodHandle PROPAGATE_LIGHT_BULK;
    public static final MethodHandle FIND_PATH;
    public static final MethodHandle EXECUTE_COMMAND;

    static {
        String libName = System.mapLibraryName("rust_mc_core");
        Path devPath = Paths.get("rust_mc_core/target/release/" + libName).toAbsolutePath();
        
        if (java.nio.file.Files.exists(devPath)) {
            System.load(devPath.toString());
        } else {
            try {
                // Try loading from resources (production)
                java.io.InputStream is = NativeBridge.class.getResourceAsStream("/" + libName);
                if (is == null) throw new IllegalStateException("Library " + libName + " not found in dev path or resources");
                
                Path tmp = java.nio.file.Files.createTempFile("rust_mc_", "_" + libName);
                java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(tmp.toString());
                tmp.toFile().deleteOnExit();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed to load native library", e);
            }
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
    }

    private static MethodHandle createHandle(String name, FunctionDescriptor desc) {
        return LINKER.downcallHandle(LOOKUP.find(name).orElseThrow(), desc);
    }

    public static float fastInvSqrt(float x) { try { return (float) FAST_INV_SQRT.invokeExact(x); } catch (Throwable t) { return 1.0f / (float)Math.sqrt(x); } }
    public static float invokeSin(float x) { try { return (float) SIN.invokeExact(x); } catch (Throwable t) { return (float)Math.sin(x); } }
    public static float invokeCos(float x) { try { return (float) COS.invokeExact(x); } catch (Throwable t) { return (float)Math.cos(x); } }
    public static float invokeSqrt(float x) { try { return (float) SQRT.invokeExact(x); } catch (Throwable t) { return (float)Math.sqrt(x); } }
    
    public static int invokeCompress(MemorySegment in, int inL, MemorySegment out, int outL) { try { return (int) COMPRESS.invokeExact(in, inL, out, outL); } catch (Throwable t) { return -1; } }
    public static int invokeDecompress(MemorySegment in, int inL, MemorySegment out, int outL) { try { return (int) DECOMPRESS.invokeExact(in, inL, out, outL); } catch (Throwable t) { return -1; } }
    
    public static double noise2d(double x, double y) { try { return (double) NOISE_2D.invokeExact(x, y); } catch (Throwable t) { return 0.0; } }
    public static int executeCommand(MemorySegment cmd, int len) { try { return (int) EXECUTE_COMMAND.invokeExact(cmd, len); } catch (Throwable t) { return -1; } }
    
    public static int propagateLightBulk(MemorySegment data, int len) { try { return (int) PROPAGATE_LIGHT_BULK.invokeExact(data, len); } catch (Throwable t) { return -1; } }
    public static int findPath(MemorySegment start, MemorySegment end, MemorySegment world, int limit) {
        try { return (int) FIND_PATH.invokeExact(start, end, world, limit); } catch (Throwable t) { return -1; }
    }
    
    public static int findPathRaw(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment start = arena.allocate(ValueLayout.JAVA_INT, 3);
            start.setAtIndex(ValueLayout.JAVA_INT, 0, startX);
            start.setAtIndex(ValueLayout.JAVA_INT, 1, startY);
            start.setAtIndex(ValueLayout.JAVA_INT, 2, startZ);
            
            MemorySegment end = arena.allocate(ValueLayout.JAVA_INT, 3);
            end.setAtIndex(ValueLayout.JAVA_INT, 0, endX);
            end.setAtIndex(ValueLayout.JAVA_INT, 1, endY);
            end.setAtIndex(ValueLayout.JAVA_INT, 2, endZ);
            
            return (int) FIND_PATH.invokeExact(start, end, MemorySegment.NULL, 0);
        } catch (Throwable t) { return -1; }
    }
}
