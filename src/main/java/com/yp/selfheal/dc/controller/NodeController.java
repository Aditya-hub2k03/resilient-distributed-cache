package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.dto.NodeInfoDto;
import com.yp.selfheal.dc.dto.NodeRegisterRequest;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.service.NodeLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@Tag(name = "Node Lifecycle & Registry", description = "Endpoints for registering and listing cluster nodes")
public class NodeController {

    private final NodeRegistry nodeRegistry;
    private final NodeLifecycleService nodeLifecycleService;

    public NodeController(NodeRegistry nodeRegistry, NodeLifecycleService nodeLifecycleService) {
        this.nodeRegistry = nodeRegistry;
        this.nodeLifecycleService = nodeLifecycleService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new node", description = "Registers and joins a node to the cluster")
    public ResponseEntity<ApiResponse<NodeInfoDto>> registerNode(@Valid @RequestBody NodeRegisterRequest request) {
        NodeRecord node = nodeLifecycleService.registerAndJoin(request.nodeId(), request.host(), request.port());
        NodeInfoDto dto = new NodeInfoDto(
                node.getNodeId(), node.getHost(), node.getPort(), node.getStatus(), node.getJoinedAt(), node.getLastHeartbeat()
        );
        return ResponseEntity.ok(ApiResponse.ok("Node successfully registered and joined", dto));
    }

    @GetMapping
    @Operation(summary = "List cluster nodes", description = "Retrieves all currently active nodes")
    public ResponseEntity<ApiResponse<List<NodeInfoDto>>> listNodes() {
        List<NodeInfoDto> dtoList = nodeRegistry.getAllActiveNodes().stream()
                .map(n -> new NodeInfoDto(n.getNodeId(), n.getHost(), n.getPort(), n.getStatus(), n.getJoinedAt(), n.getLastHeartbeat()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Cluster nodes fetched", dtoList));
    }
}
