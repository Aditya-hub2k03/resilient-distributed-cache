package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.cluster.GossipMessage;
import com.yp.selfheal.dc.cluster.GossipService;
import com.yp.selfheal.dc.cluster.PhiAccrualFailureDetector;
import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.repository.NodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cluster")
@Tag(name = "Cluster Health & Internal", description = "Cluster convergence and failure detection")
public class ClusterInternalController {

    private final GossipService gossipService;
    private final PhiAccrualFailureDetector failureDetector;
    private final NodeRepository nodeRepository;

    public ClusterInternalController(
            GossipService gossipService,
            PhiAccrualFailureDetector failureDetector,
            NodeRepository nodeRepository) {
        this.gossipService = gossipService;
        this.failureDetector = failureDetector;
        this.nodeRepository = nodeRepository;
    }

    @PostMapping("/gossip")
    public ResponseEntity<Void> receiveGossip(@RequestBody GossipMessage message) {
        gossipService.processGossipMessage(message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Aggregated cluster health", description = "Phi accrual suspicion levels across all nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClusterHealth() {
        List<NodeRecord> nodes = nodeRepository.findAll();
        Map<String, Object> healthReport = new HashMap<>();

        Map<String, Double> phiScores = new HashMap<>();
        for (NodeRecord node : nodes) {
            phiScores.put(node.getNodeId(), failureDetector.phi(node.getNodeId()));
        }

        healthReport.put("totalNodes", nodes.size());
        healthReport.put("phiSuspicionLevels", phiScores);
        healthReport.put("status", "HEALTHY");

        return ResponseEntity.ok(ApiResponse.ok("Cluster health aggregated", healthReport));
    }
}
