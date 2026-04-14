package com.alexxiconify.rustmc;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
//
 //  NativeCache provides bounded LRU storage for frequently accessed data.
 //  Evicts oldest entries when capacity is exceeded to prevent unbounded RAM growth.
 //  Tracks hit/miss statistics for performance monitoring.
 //  <p>
 //  Methods like {@code store}, {@code has}, {@code get} form the public API surface
 //  used by mod compat hooks and future extensions.
@SuppressWarnings("unused")
public class NativeCache {
    private NativeCache() {}
    private static final int MAX_ENTRIES = 1024;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final AtomicLong HITS = new AtomicLong(0);
    private static final AtomicLong MISSES = new AtomicLong(0);
    private static final LinkedHashMap<String, byte[]> CACHE = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return super.size() > MAX_ENTRIES;
        }
    };
    //
     // Stores a byte array and returns the key.
    public static String store(String key, byte[] data) {
        LOCK.writeLock().lock();
        try {
            CACHE.put(key, data);
        } finally {
            LOCK.writeLock().unlock();
        }
        return key;
    }
    public static byte[] get(String key) {
        // Must use writeLock because accessOrder=true mutates the linked list on get()
        LOCK.writeLock().lock();
        try {
            byte[] val = CACHE.get(key);
            if (val != null) {
                HITS.incrementAndGet();
            } else {
                MISSES.incrementAndGet();
            }
            return val;
        } finally {
            LOCK.writeLock().unlock();
        }
    }
    public static boolean has(String key) {
        LOCK.readLock().lock();
        try {
            return CACHE.containsKey(key);
        } finally {
            LOCK.readLock().unlock();
        }
    }
    public static void clear() {
        LOCK.writeLock().lock();
        try {
            CACHE.clear();
            HITS.set(0);
            MISSES.set(0);
        } finally {
            LOCK.writeLock().unlock();
        }
    }
    public static int size() {
        LOCK.readLock().lock();
        try {
            return CACHE.size();
        } finally {
            LOCK.readLock().unlock();
        }
    }
    //Returns cache hit count since last clear. // /
    public static long getHits() { return HITS.get(); }
    //Returns cache miss count since last clear. // /
    public static long getMisses() { return MISSES.get(); }
    //Returns the cache hit ratio (0.0 - 1.0). // /
    public static float getHitRatio() {
        long h = HITS.get();
        long m = MISSES.get();
        long total = h + m;
        return total == 0 ? 0.0f : (float) h / total;
    }
}