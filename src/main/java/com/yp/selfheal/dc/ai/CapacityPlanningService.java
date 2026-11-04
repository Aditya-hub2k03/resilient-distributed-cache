package com.yp.selfheal.dc.ai;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.dto.CapacityPlanDto;
import org.springframework.stereotype.Service;

@Service
public class CapacityPlanningService {

    private final NodeRegistry nodeRegistry;
    private final CacheEngine cacheEngine;
    private final OllamaClient ollamaClient;

    public CapacityPlanningService(NodeRegistry nodeRegistry, CacheEngine cacheEngine, OllamaClient ollamaClient) {
        this.nodeRegistry = nodeRegistry;
        this.cacheEngine = cacheEngine;
        this.ollamaClient = ollamaClient;
    }

    public CapacityPlanDto generateCapacityPlan() {
        int nodes = Math.max(1, nodeRegistry.getAllActiveNodes().size());
        int currentKeys = cacheEngine.getCurrentSize();
        int capacity = cacheEngine.getCapacity();
        double utilization = capacity == 0 ? 0.0 : ((double) currentKeys / capacity) * 100.0;

        String prompt = String.format("Generate a short capacity planning recommendation: Active nodes: %d, Utilization: %.1f%%, Total keys cached: %d.",
                nodes, utilization, currentKeys);

        String advice = ollamaClient.generate(prompt).orElse(
                utilization > 80.0 ? "Scale up: add 1-2 cluster nodes to prevent eviction cascades." : "Capacity nominal."
        );

        return new CapacityPlanDto(nodes, currentKeys, Math.round(utilization * 10.0) / 10.0,
                utilization > 80.0 ? "SCALE_OUT" : "MAINTAIN", advice);
    }
}
