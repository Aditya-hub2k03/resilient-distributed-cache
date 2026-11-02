package com.yp.selfheal.dc.service;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.dto.CacheStatsResponse;
import com.yp.selfheal.dc.exception.KeyNotFoundException;
import com.yp.selfheal.dc.exception.NodeUnreachableException;
import com.yp.selfheal.dc.model.AuditLog;
import com.yp.selfheal.dc.model.CacheMetadata;
import com.yp.selfheal.dc.model.KeyStats;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.repository.AuditLogRepository;
import com.yp.selfheal.dc.repository.CacheMetadataRepository;
import com.yp.selfheal.dc.repository.KeyStatsRepository;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.security.NodeAuthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final CacheEngine cacheEngine;
    private final ConsistentHashRing hashRing;
    private final NodeRegistry nodeRegistry;
    private final NodeAuthValidator nodeAuthValidator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetadataRepository metadataRepo;
    private final KeyStatsRepository keyStatsRepo;
    private final AuditLogRepository auditRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    public CacheService(
            CacheEngine cacheEngine,
            ConsistentHashRing hashRing,
            NodeRegistry nodeRegistry,
            NodeAuthValidator nodeAuthValidator,
            RedisTemplate<String, Object> redisTemplate,
            CacheMetadataRepository metadataRepo,
            KeyStatsRepository keyStatsRepo,
            AuditLogRepository auditRepo) {
        this.cacheEngine = cacheEngine;
        this.hashRing = hashRing;
        this.nodeRegistry = nodeRegistry;
        this.nodeAuthValidator = nodeAuthValidator;
        this.redisTemplate = redisTemplate;
        this.metadataRepo = metadataRepo;
        this.keyStatsRepo = keyStatsRepo;
        this.auditRepo = auditRepo;
    }

    public Object get(String key) {
        String ownerNodeId = hashRing.routeKey(key).orElse(selfNodeId);

        // If local node owns key -> Read L1 -> Read L2 Redis
        if (selfNodeId.equals(ownerNodeId)) {
            return getLocalOrL2(key);
        }

        // Forward to remote ring owner
        return forwardGetToRemoteNode(ownerNodeId, key);
    }

    private Object getLocalOrL2(String key) {
        Optional<Object> localVal = cacheEngine.get(key);
        if (localVal.isPresent()) {
            updateKeyStats(key, true);
            return localVal.get();
        }

        // L1 Miss: Check Redis (L2 Cache)
        Object l2Val = redisTemplate.opsForValue().get("dc:l2:" + key);
        if (l2Val != null) {
            log.debug("L2 Redis cache hit for key {}", key);
            cacheEngine.put(key, l2Val, null); // populate L1 back
            updateKeyStats(key, true);
            return l2Val;
        }

        updateKeyStats(key, false);
        throw new KeyNotFoundException(key);
    }

    @Transactional
    public void put(String key, Object value, Long ttlSeconds) {
        String ownerNodeId = hashRing.routeKey(key).orElse(selfNodeId);

        if (!selfNodeId.equals(ownerNodeId)) {
            forwardPutToRemoteNode(ownerNodeId, key, value, ttlSeconds);
            return;
        }

        putLocalAndL2(key, value, ttlSeconds);
    }

    public void putLocalAndL2(String key, Object value, Long ttlSeconds) {
        // 1. Store in L1 local memory
        cacheEngine.put(key, value, ttlSeconds);

        // 2. Store in L2 Redis
        String redisKey = "dc:l2:" + key;
        if (ttlSeconds != null && ttlSeconds > 0) {
            redisTemplate.opsForValue().set(redisKey, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue().set(redisKey, value);
        }

        // 3. MySQL Metadata
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

        auditRepo.save(new AuditLog(null, "CACHE_PUT", "Key added/updated: " + key));
    }

    @Transactional
    public void delete(String key) {
        String ownerNodeId = hashRing.routeKey(key).orElse(selfNodeId);

        if (!selfNodeId.equals(ownerNodeId)) {
            forwardDeleteToRemoteNode(ownerNodeId, key);
            return;
        }

        cacheEngine.delete(key);
        redisTemplate.delete("dc:l2:" + key);
        metadataRepo.deleteByCacheKey(key);
        auditRepo.save(new AuditLog(null, "CACHE_DELETE", "Key deleted: " + key));
    }

    // Inter-Node Forwarding implementations
    @SuppressWarnings("unchecked")
    private Object forwardGetToRemoteNode(String targetNodeId, String key) {
        NodeRecord node = nodeRegistry.getNode(targetNodeId)
                .orElseThrow(() -> new NodeUnreachableException(targetNodeId));

        String url = String.format("http://%s:%d/api/v1/cache/%s", node.getHost(), node.getPort(), key);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return response.getBody().get("data");
            }
            throw new KeyNotFoundException(key);
        } catch (Exception ex) {
            log.error("Failed to forward GET key {} to remote node {}: {}", key, targetNodeId, ex.getMessage());
            throw new NodeUnreachableException(targetNodeId);
        }
    }

    private void forwardPutToRemoteNode(String targetNodeId, String key, Object value, Long ttlSeconds) {
        NodeRecord node = nodeRegistry.getNode(targetNodeId)
                .orElseThrow(() -> new NodeUnreachableException(targetNodeId));

        String url = String.format("http://%s:%d/api/v1/cache", node.getHost(), node.getPort());
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = Map.of("key", key, "value", value);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, Map.class);
        } catch (Exception ex) {
            log.error("Failed to forward PUT key {} to remote node {}: {}", key, targetNodeId, ex.getMessage());
            throw new NodeUnreachableException(targetNodeId);
        }
    }

    private void forwardDeleteToRemoteNode(String targetNodeId, String key) {
        NodeRecord node = nodeRegistry.getNode(targetNodeId)
                .orElseThrow(() -> new NodeUnreachableException(targetNodeId));

        String url = String.format("http://%s:%d/api/v1/cache/%s", node.getHost(), node.getPort(), key);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception ex) {
            log.error("Failed to forward DELETE key {} to remote node {}: {}", key, targetNodeId, ex.getMessage());
            throw new NodeUnreachableException(targetNodeId);
        }
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
