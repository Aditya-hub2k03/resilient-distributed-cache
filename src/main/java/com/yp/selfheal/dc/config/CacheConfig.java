package com.yp.selfheal.dc.config;

import com.yp.selfheal.dc.cache.EvictionPolicy;
import com.yp.selfheal.dc.cache.LocalStore;
import com.yp.selfheal.dc.cache.LruEvictionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public LocalStore<Object> localStore() {
        return new LocalStore<>();
    }

    @Bean
    public EvictionPolicy evictionPolicy() {
        return new LruEvictionPolicy();
    }
}
