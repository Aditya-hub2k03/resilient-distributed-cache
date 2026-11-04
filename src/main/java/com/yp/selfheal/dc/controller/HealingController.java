package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.healing.AntiEntropyService;
import com.yp.selfheal.dc.healing.MerkleNode;
import com.yp.selfheal.dc.healing.MerkleTreeService;
import com.yp.selfheal.dc.healing.QuarantineManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/cluster")
@Tag(name = "Self-Healing Engine", description = "Endpoints for triggering anti-entropy, inspecting quarantine and Merkle trees")
public class HealingController {

    private final AntiEntropyService antiEntropyService;
    private final MerkleTreeService merkleTreeService;
    private final QuarantineManager quarantineManager;

    public HealingController(
            AntiEntropyService antiEntropyService,
            MerkleTreeService merkleTreeService,
            QuarantineManager quarantineManager) {
        this.antiEntropyService = antiEntropyService;
        this.merkleTreeService = merkleTreeService;
        this.quarantineManager = quarantineManager;
    }

    @PostMapping("/heal")
    @Operation(summary = "Force anti-entropy run", description = "Forces Merkle-tree anti-entropy synchronization cycle")
    public ResponseEntity<ApiResponse<String>> forceHeal() {
        antiEntropyService.runAntiEntropySync();
        return ResponseEntity.ok(ApiResponse.ok("Anti-entropy repair sweep triggered", "OK"));
    }

    @PostMapping("/merkle-tree")
    @Operation(summary = "Get Merkle tree", description = "Returns Merkle-tree snapshot for active hash ranges")
    public ResponseEntity<MerkleNode> getMerkleTree() {
        MerkleNode tree = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/quarantine")
    @Operation(summary = "List quarantined nodes", description = "Returns set of nodes currently under quarantine")
    public ResponseEntity<ApiResponse<Set<String>>> getQuarantinedNodes() {
        return ResponseEntity.ok(ApiResponse.ok("Quarantined nodes fetched", quarantineManager.getQuarantinedNodes()));
    }
}
