package com.yp.selfheal.dc.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeAuthValidator {

    @Value("${dc.security.api-key:secret-cluster-key}")
    private String clusterApiKey;

    public boolean isValidApiKey(String apiKey) {
        return clusterApiKey != null && clusterApiKey.equals(apiKey);
    }

    public String getClusterApiKey() {
        return clusterApiKey;
    }
}
