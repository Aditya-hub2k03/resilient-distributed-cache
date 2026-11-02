package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.dto.RingStateDto;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.ring.RingRebalancer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cluster")
@Tag(name = "Cluster & Ring Management", description = "Endpoints for inspecting ring topology and triggering rebalance")
public class ClusterController {

    private final ConsistentHashRing ring;
    private final RingRebalancer ringRebalancer;

    public ClusterController(ConsistentHashRing ring, RingRebalancer ringRebalancer) {
        this.ring = ring;
        this.ringRebalancer = ringRebalancer;
    }

    @GetMapping("/ring")
    @Operation(summary = "Get hash ring state", description = "Returns active virtual nodes and physical ring snapshot")
    public ResponseEntity<ApiResponse<RingStateDto>> getRingState() {
        Map<Long, String> snapshot = ring.getRingSnapshot();
        RingStateDto dto = new RingStateDto(
                ring.size(),
                snapshot.size(),
                ring.getPhysicalNodes(),
                snapshot
        );
        return ResponseEntity.ok(ApiResponse.ok("Ring state fetched", dto));
    }

    @PostMapping("/rebalance")
    @Operation(summary = "Trigger manual rebalance", description = "Broadcasts a rebalance event over Kafka")
    public ResponseEntity<ApiResponse<String>> triggerRebalance(@RequestParam(defaultValue = "admin") String triggerNodeId) {
        String rebalanceId = ringRebalancer.triggerRebalance(triggerNodeId, "MANUAL");
        return ResponseEntity.ok(ApiResponse.ok("Rebalance triggered successfully", rebalanceId));
    }
}
