package com.yp.selfheal.dc.ai;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.dto.EvictionSuggestionDto;
import com.yp.selfheal.dc.model.KeyStats;
import com.yp.selfheal.dc.repository.KeyStatsRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IntelligentEvictionService {

    private final KeyStatsRepository keyStatsRepo;
    private final OllamaClient ollamaClient;
    private final CacheEngine cacheEngine;
    private final Counter aiEvictionCounter;

    public IntelligentEvictionService(
            KeyStatsRepository keyStatsRepo,
            OllamaClient ollamaClient,
            CacheEngine cacheEngine,
            MeterRegistry registry) {
        this.keyStatsRepo = keyStatsRepo;
        this.ollamaClient = ollamaClient;
        this.cacheEngine = cacheEngine;
        this.aiEvictionCounter = Counter.builder("dc_evictions_total")
                .tag("reason", "ai")
                .description("Total evictions guided by AI")
                .register(registry);
    }

    public EvictionSuggestionDto getEvictionSuggestions() {
        List<KeyStats> candidates = keyStatsRepo.findAll(PageRequest.of(0, 10)).getContent();
        if (candidates.isEmpty()) {
            return new EvictionSuggestionDto(List.of(), "No active key stats available for analysis.", false);
        }

        StringBuilder sb = new StringBuilder("Rank least valuable cache keys to evict based on (hits, misses, recompute_cost):\n");
        for (KeyStats k : candidates) {
            sb.append(String.format("Key: %s, Hits: %d, Misses: %d, CostMs: %d\n",
                    k.getCacheKey(), k.getHitCount(), k.getMissCount(), k.getRecomputeCostMs()));
        }

        Optional<String> aiResp = ollamaClient.generate(sb.toString());
        List<String> suggestedKeys = candidates.stream()
                .sorted((a, b) -> Long.compare(a.getHitCount(), b.getHitCount()))
                .map(KeyStats::getCacheKey)
                .limit(3)
                .toList();

        return new EvictionSuggestionDto(suggestedKeys, aiResp.orElse("Ranked by lowest frequency hit/miss ratio."), aiResp.isPresent());
    }

    public boolean executeAiEviction(String key) {
        boolean evicted = cacheEngine.delete(key);
        if (evicted) {
            aiEvictionCounter.increment();
        }
        return evicted;
    }
}
