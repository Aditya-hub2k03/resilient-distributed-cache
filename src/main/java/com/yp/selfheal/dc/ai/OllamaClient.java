package com.yp.selfheal.dc.ai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class OllamaClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestTemplate restTemplate;
    private final Timer aiLatencyTimer;

    @Value("${dc.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${dc.ai.ollama.model:llama3}")
    private String modelName;

    public OllamaClient(@Qualifier("ollamaRestTemplate") RestTemplate restTemplate, MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.aiLatencyTimer = Timer.builder("dc_ai_latency_seconds")
                .description("Inference latency of Ollama LLM requests")
                .register(registry);
    }

    public Optional<String> generate(String prompt) {
        Instant start = Instant.now();
        String url = ollamaBaseUrl + "/api/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> req = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req, headers);
            Map resp = restTemplate.postForObject(url, entity, Map.class);
            if (resp != null && resp.containsKey("response")) {
                return Optional.of((String) resp.get("response"));
            }
        } catch (Exception ex) {
            log.warn("Ollama call failed or timed out: {}", ex.getMessage());
        } finally {
            aiLatencyTimer.record(Duration.between(start, Instant.now()));
        }
        return Optional.empty();
    }
}
