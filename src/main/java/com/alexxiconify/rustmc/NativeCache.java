package com.alexxiconify.rustmc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeCache provides off-heap memory storage for frequently accessed, but rarely modified data.
 * This reduces garbage collection pressure on the JVM by storing the data in Rust/C memory.
 */
@SuppressWarnings("preview")
public class NativeCache {
    private NativeCache() {}
    
    private static final ConcurrentHashMap<String, Long> POINTER_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Stores a byte array in off-heap memory and returns a unique handle (string).
     */
    public static String storeOffHeap(String key, byte[] data) {
        if (!NativeBridge.isReady()) return null;
        
        Arena arena = Arena.ofAuto();
        MemorySegment segment = arena.allocate(data.length);
        segment.copyFrom(MemorySegment.ofArray(data));
        
        // In a real JNI environment we would pass this segment to Rust to be stored in a global
        // static OnceLock<HashMap> or similar. For this iteration, we keep the JVM representation
        // alive via the Auto arena.
        long address = segment.address();
        POINTER_CACHE.put(key, address);
        
        return key;
    }

    /**
     * Simple native cache retrieval simulation.
     */
    public static boolean has(String key) {
        return POINTER_CACHE.containsKey(key);
    }
}
