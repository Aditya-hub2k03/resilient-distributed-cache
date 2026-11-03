package com.yp.selfheal.dc.replication;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.security.NodeAuthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ReplicationService {
    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    private final ReplicaRouter replicaRouter;
    private final QuorumCoordinator quorumCoordinator;
    private final HintedHandoffStore hintedHandoffStore;
    private final NodeRegistry nodeRegistry;
    private final NodeAuthValidator nodeAuthValidator;
    private final CacheEngine cacheEngine;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService replicationExecutor = Executors.newCachedThreadPool();

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    public ReplicationService(
            ReplicaRouter replicaRouter,
            QuorumCoordinator quorumCoordinator,
            HintedHandoffStore hintedHandoffStore,
            NodeRegistry nodeRegistry,
            NodeAuthValidator nodeAuthValidator,
            CacheEngine cacheEngine) {
        this.replicaRouter = replicaRouter;
        this.quorumCoordinator = quorumCoordinator;
        this.hintedHandoffStore = hintedHandoffStore;
        this.nodeRegistry = nodeRegistry;
        this.nodeAuthValidator = nodeAuthValidator;
        this.cacheEngine = cacheEngine;
    }

    public void replicatePut(String key, Object value, Long ttlSeconds) {
        List<String> replicaNodeIds = replicaRouter.getReplicaNodes(key);
        if (replicaNodeIds.isEmpty()) {
            replicaNodeIds = List.of(selfNodeId);
        }

        List<CompletableFuture<Boolean>> writeFutures = new ArrayList<>();

        for (String targetNodeId : replicaNodeIds) {
            if (selfNodeId.equals(targetNodeId)) {
                cacheEngine.put(key, value, ttlSeconds);
                writeFutures.add(CompletableFuture.completedFuture(true));
            } else {
                writeFutures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        sendReplicaWrite(targetNodeId, key, value, ttlSeconds);
                        return true;
                    } catch (Exception ex) {
                        hintedHandoffStore.storeHint(targetNodeId, key, value, ttlSeconds);
                        return false;
                    }
                }, replicationExecutor));
            }
        }

        CompletableFuture.allOf(writeFutures.toArray(new CompletableFuture[0])).join();

        long successAcks = writeFutures.stream()
                .filter(f -> {
                    try { return f.get(); } catch (Exception e) { return false; }
                }).count();

        quorumCoordinator.validateWriteQuorum((int) successAcks);
    }

    public Optional<Object> quorumRead(String key) {
        List<String> replicaNodeIds = replicaRouter.getReplicaNodes(key);
        if (replicaNodeIds.isEmpty()) {
            return cacheEngine.get(key);
        }

        List<CompletableFuture<Optional<Object>>> readFutures = new ArrayList<>();

        for (String targetNodeId : replicaNodeIds) {
            if (selfNodeId.equals(targetNodeId)) {
                readFutures.add(CompletableFuture.completedFuture(cacheEngine.get(key)));
            } else {
                readFutures.add(CompletableFuture.supplyAsync(() -> fetchReplicaRead(targetNodeId, key), replicationExecutor));
            }
        }

        CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0])).join();

        List<Object> validResponses = new ArrayList<>();
        for (var f : readFutures) {
            try {
                Optional<Object> res = f.get();
                res.ifPresent(validResponses::add);
            } catch (Exception ignored) {}
        }

        quorumCoordinator.validateReadQuorum(validResponses.size());

        if (validResponses.isEmpty()) {
            return Optional.empty();
        }

        // Return latest value from majority read response
        return Optional.of(validResponses.get(0));
    }

    private void sendReplicaWrite(String targetNodeId, String key, Object value, Long ttlSeconds) {
        NodeRecord node = nodeRegistry.getNode(targetNodeId).orElseThrow();
        String url = String.format("http://%s:%d/api/v1/cache/replica/put", node.getHost(), node.getPort());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("value", value);
        body.put("ttlSeconds", ttlSeconds);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
    }

    @SuppressWarnings("unchecked")
    private Optional<Object> fetchReplicaRead(String targetNodeId, String key) {
        try {
            NodeRecord node = nodeRegistry.getNode(targetNodeId).orElseThrow();
            String url = String.format("http://%s:%d/api/v1/cache/replica/get/%s", node.getHost(), node.getPort(), key);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (res.getBody() != null && res.getBody().containsKey("data")) {
                return Optional.ofNullable(res.getBody().get("data"));
            }
        } catch (Exception ex) {
            log.warn("Replica read failed against node {}: {}", targetNodeId, ex.getMessage());
        }
        return Optional.empty();
    }
}
