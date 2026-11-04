package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.ai.CapacityPlanningService;
import com.yp.selfheal.dc.ai.FailurePredictionService;
import com.yp.selfheal.dc.ai.IntelligentEvictionService;
import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.dto.CapacityPlanDto;
import com.yp.selfheal.dc.dto.EvictionSuggestionDto;
import com.yp.selfheal.dc.dto.FailurePredictionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Insights & Operations", description = "Endpoints for predictive failure, intelligent eviction, and capacity planning")
public class AiInsightController {

    private final FailurePredictionService failurePredictionService;
    private final IntelligentEvictionService evictionService;
    private final CapacityPlanningService capacityPlanningService;

    public AiInsightController(
            FailurePredictionService failurePredictionService,
            IntelligentEvictionService evictionService,
            CapacityPlanningService capacityPlanningService) {
        this.failurePredictionService = failurePredictionService;
        this.evictionService = evictionService;
        this.capacityPlanningService = capacityPlanningService;
    }

    @GetMapping("/failure-predictions/{nodeId}")
    @Operation(summary = "Predictive node failure risk", description = "Sidecar scoring and Ollama assessment of node failure likelihood")
    public ResponseEntity<ApiResponse<FailurePredictionDto>> getFailurePrediction(@PathVariable String nodeId) {
        return ResponseEntity.ok(ApiResponse.ok("Failure assessment completed", failurePredictionService.assessNode(nodeId)));
    }

    @GetMapping("/eviction-suggestions")
    @Operation(summary = "AI-guided cache eviction suggestions", description = "Suggests victim keys by predicted future value")
    public ResponseEntity<ApiResponse<EvictionSuggestionDto>> getEvictionSuggestions() {
        return ResponseEntity.ok(ApiResponse.ok("Eviction insights generated", evictionService.getEvictionSuggestions()));
    }

    @PostMapping("/evict/{key}")
    @Operation(summary = "Execute AI-guided eviction", description = "Explicitly evicts key and increments AI eviction counters")
    public ResponseEntity<ApiResponse<Boolean>> evictKey(@PathVariable String key) {
        boolean ok = evictionService.executeAiEviction(key);
        return ResponseEntity.ok(ApiResponse.ok("AI eviction executed", ok));
    }

    @GetMapping("/capacity-plan")
    @Operation(summary = "AI capacity planning report", description = "Generates rebalance and scaling recommendations based on load")
    public ResponseEntity<ApiResponse<CapacityPlanDto>> getCapacityPlan() {
        return ResponseEntity.ok(ApiResponse.ok("Capacity plan synthesized", capacityPlanningService.generateCapacityPlan()));
    }
}
