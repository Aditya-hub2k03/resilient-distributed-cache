package com.yp.selfheal.dc.service;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.dto.CacheStatsResponse;
import com.yp.selfheal.dc.exception.KeyNotFoundException;
import com.yp.selfheal.dc.model.AuditLog;
import com.yp.selfheal.dc.model.CacheMetadata;
import com.yp.selfheal.dc.model.KeyStats;
import com.yp.selfheal.dc.repository.AuditLogRepository;
import com.yp.selfheal.dc.repository.CacheMetadataRepository;
import com.yp.selfheal.dc.repository.KeyStatsRepository;
import com.yp.selfheal.dc.replication.ReplicationService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class CacheService {
    private final CacheEngine cacheEngine;
    private final ReplicationService replicationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetadataRepository metadataRepo;
    private final KeyStatsRepository keyStatsRepo;
    private final AuditLogRepository auditRepo;

    public CacheService(
            CacheEngine cacheEngine,
            ReplicationService replicationService,
            RedisTemplate<String, Object> redisTemplate,
            CacheMetadataRepository metadataRepo,
            KeyStatsRepository keyStatsRepo,
            AuditLogRepository auditRepo) {
        this.cacheEngine = cacheEngine;
        this.replicationService = replicationService;
        this.redisTemplate = redisTemplate;
        this.metadataRepo = metadataRepo;
        this.keyStatsRepo = keyStatsRepo;
        this.auditRepo = auditRepo;
    }

    public Object get(String key) {
        // Phase 3: Quorum-backed distributed read (R + W > N)
        Optional<Object> quorumVal = replicationService.quorumRead(key);

        if (quorumVal.isPresent()) {
            updateKeyStats(key, true);
            return quorumVal.get();
        }

        // L2 Redis fallback
        Object l2Val = redisTemplate.opsForValue().get("dc:l2:" + key);
        if (l2Val != null) {
            cacheEngine.put(key, l2Val, null);
            updateKeyStats(key, true);
            return l2Val;
        }

        updateKeyStats(key, false);
        throw new KeyNotFoundException(key);
    }

    @Transactional
    public void put(String key, Object value, Long ttlSeconds) {
        // Phase 3: Quorum-backed distributed replication write
        replicationService.replicatePut(key, value, ttlSeconds);

        // Populate L2 Redis
        String redisKey = "dc:l2:" + key;
        if (ttlSeconds != null && ttlSeconds > 0) {
            redisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue().set(redisKey, value);
        }

        // MySQL Metadata
        String valueHash = computeHash(String.valueOf(value));
        LocalDateTime expiresAt = (ttlSeconds != null && ttlSeconds > 0)
                ? LocalDateTime.now().plusSeconds(ttlSeconds) : null;

        metadataRepo.findByCacheKey(key).ifPresentOrElse(meta -> {
            meta.setValueHash(valueHash);
            meta.setExpiresAt(expiresAt);
            metadataRepo.save(meta);
        }, () -> {
            CacheMetadata meta = new CacheMetadata(key, valueHash, String.valueOf(value).length(), expiresAt);
            metadataRepo.save(meta);
        });

        auditRepo.save(new AuditLog(null, "CACHE_PUT", "Key added/updated via quorum: " + key));
    }

    @Transactional
    public void delete(String key) {
        cacheEngine.delete(key);
        redisTemplate.delete("dc:l2:" + key);
        metadataRepo.deleteByCacheKey(key);
        auditRepo.save(new AuditLog(null, "CACHE_DELETE", "Key deleted: " + key));
    }

    public CacheStatsResponse getStats() {
        long hits = cacheEngine.getHitCount();
        long misses = cacheEngine.getMissCount();
        long total = hits + misses;
        double rate = total == 0 ? 0.0 : ((double) hits / total) * 100.0;
        return new CacheStatsResponse(
                hits,
                misses,
                cacheEngine.getEvictionCount(),
                cacheEngine.getCurrentSize(),
                cacheEngine.getCapacity(),
                Math.round(rate * 100.0) / 100.0
        );
    }

    private void updateKeyStats(String key, boolean hit) {
        keyStatsRepo.findByCacheKey(key).ifPresentOrElse(stats -> {
            if (hit) stats.incrementHits(); else stats.incrementMisses();
            keyStatsRepo.save(stats);
        }, () -> {
            KeyStats stats = new KeyStats(key);
            if (hit) stats.incrementHits(); else stats.incrementMisses();
            keyStatsRepo.save(stats);
        });
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "hash-fallback";
        }
    }
}
