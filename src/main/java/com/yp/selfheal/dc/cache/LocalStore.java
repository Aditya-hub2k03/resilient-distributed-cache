package com.yp.selfheal.dc.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe concurrent storage backing L1 cache.
 */
public class LocalStore<V> {
    private final ConcurrentHashMap<String, CacheEntry<V>> store = new ConcurrentHashMap<>();

    public void put(String key, CacheEntry<V> entry) {
        store.put(key, entry);
    }

    public Optional<CacheEntry<V>> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    public CacheEntry<V> remove(String key) {
        return store.remove(key);
    }

    public boolean containsKey(String key) {
        return store.containsKey(key);
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }

    public ConcurrentHashMap<String, CacheEntry<V>> rawEntries() {
        return store;
    }
}
