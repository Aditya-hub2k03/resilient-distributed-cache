package com.yp.selfheal.dc.cache;

public interface EvictionPolicy {
    void recordAccess(String key);
    void recordPut(String key);
    void recordRemove(String key);
    String evictKey();
    void clear();
}
