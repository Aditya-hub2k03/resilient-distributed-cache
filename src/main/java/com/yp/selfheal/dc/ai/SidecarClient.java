package com.yp.selfheal.dc.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class SidecarClient {
    private static final Logger log = LoggerFactory.getLogger(SidecarClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dc.ai.sidecar.url:http://localhost:8000}")
    private String sidecarUrl;

    @SuppressWarnings("unchecked")
    public Optional<Double> getFailureScore(String nodeId, Map<String, Object> telemetryPayload) {
        try {
            String url = sidecarUrl + "/api/v1/score/" + nodeId;
            Map<String, Object> resp = restTemplate.postForObject(url, telemetryPayload, Map.class);
            if (resp != null && resp.containsKey("failure_risk_score")) {
                Number score = (Number) resp.get("failure_risk_score");
                return Optional.of(score.doubleValue());
            }
        } catch (Exception e) {
            log.debug("Python sidecar scoring unavailable: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
