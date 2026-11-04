package com.yp.selfheal.dc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OllamaConfig {

    @Value("${dc.ai.ollama.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${dc.ai.ollama.read-timeout-ms:10000}")
    private int readTimeout;

    @Bean(name = "ollamaRestTemplate")
    public RestTemplate ollamaRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
