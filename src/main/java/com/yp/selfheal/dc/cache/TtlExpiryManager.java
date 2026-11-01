package com.yp.selfheal.dc.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TtlExpiryManager {
    private static final Logger log = LoggerFactory.getLogger(TtlExpiryManager.class);
    private final LocalStore<Object> localStore;
    private final EvictionPolicy evictionPolicy;

    public TtlExpiryManager(LocalStore<Object> localStore, EvictionPolicy evictionPolicy) {
        this.localStore = localStore;
        this.evictionPolicy = evictionPolicy;
    }

    /**
     * Active expiration cleanup running every 5 seconds.
     */
    @Scheduled(fixedDelayString = "${dc.cache.ttl-cleanup-interval-ms:5000}")
    public void purgeExpiredEntries() {
        List<String> expiredKeys = new ArrayList<>();
        localStore.rawEntries().forEach((key, entry) -> {
            if (entry.isExpired()) {
                expiredKeys.add(key);
            }
        });

        if (!expiredKeys.isEmpty()) {
            log.debug("Purging {} expired keys", expiredKeys.size());
            for (String key : expiredKeys) {
                localStore.remove(key);
                evictionPolicy.recordRemove(key);
            }
        }
    }
}
