package com.alexxiconify.rustmc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * NativeCache provides bounded LRU storage for frequently accessed data.
 * Evicts oldest entries when capacity is exceeded to prevent unbounded RAM growth.
 */
public class NativeCache {
    private NativeCache() {}

    private static final int MAX_ENTRIES = 512;
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    @SuppressWarnings("serial")
    private static final LinkedHashMap<String, byte[]> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    /**
     * Stores a byte array and returns the key.
     */
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
            return CACHE.get(key);
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
}