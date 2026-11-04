package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.security.NodeAuthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AntiEntropyService {
    private static final Logger log = LoggerFactory.getLogger(AntiEntropyService.class);

    private final MerkleTreeService merkleTreeService;
    private final NodeRegistry nodeRegistry;
    private final NodeAuthValidator authValidator;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    public AntiEntropyService(
            MerkleTreeService merkleTreeService,
            NodeRegistry nodeRegistry,
            NodeAuthValidator authValidator) {
        this.merkleTreeService = merkleTreeService;
        this.nodeRegistry = nodeRegistry;
        this.authValidator = authValidator;
    }

    @Scheduled(fixedDelayString = "${dc.healing.anti-entropy.interval-ms:60000}")
    public void runAntiEntropySync() {
        List<NodeRecord> activePeers = nodeRegistry.getAllActiveNodes().stream()
                .filter(n -> !n.getNodeId().equals(selfNodeId))
                .toList();

        if (activePeers.isEmpty()) return;

        for (NodeRecord peer : activePeers) {
            try {
                reconcileWithPeer(peer);
            } catch (Exception ex) {
                log.warn("Anti-entropy sync failed against node {}: {}", peer.getNodeId(), ex.getMessage());
            }
        }
    }

    public int reconcileWithPeer(NodeRecord peer) {
        MerkleNode localTree = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);
        String url = String.format("http://%s:%d/api/v1/cluster/merkle-tree", peer.getHost(), peer.getPort());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", authValidator.getClusterApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        MerkleNode remoteTree = restTemplate.postForObject(url, entity, MerkleNode.class);
        if (remoteTree == null) return 0;

        List<Long> divergentRanges = merkleTreeService.findMismatchedRanges(localTree, remoteTree);
        if (!divergentRanges.isEmpty()) {
            log.info("Detected {} divergent partition ranges between {} and {}", divergentRanges.size(), selfNodeId, peer.getNodeId());
        }
        return divergentRanges.size();
    }
}
