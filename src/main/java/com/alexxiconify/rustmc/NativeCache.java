package com.alexxiconify.rustmc;

import java.util.concurrent.ConcurrentHashMap;

/**
 * NativeCache provides storage for frequently accessed data.
 */
public class NativeCache {
    private NativeCache() {}
    
    private static final ConcurrentHashMap<String, byte[]> CACHE = new ConcurrentHashMap<>();
    
    /**
     * Stores a byte array and returns the key.
     * In a full JNI implementation, this would involve passing to Rust.
     */
    public static String store(String key, byte[] data) {
        CACHE.put(key, data);
        return key;
    }

    public static byte[] get(String key) {
        return CACHE.get(key);
    }

    public static boolean has(String key) {
        return CACHE.containsKey(key);
    }
}
