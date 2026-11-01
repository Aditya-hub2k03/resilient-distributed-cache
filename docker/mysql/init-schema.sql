CREATE DATABASE IF NOT EXISTS selfheal_dc;
USE selfheal_dc;

CREATE TABLE IF NOT EXISTS nodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(64) NOT NULL UNIQUE,
    host VARCHAR(128) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    joined_at DATETIME NOT NULL,
    last_heartbeat DATETIME
);

CREATE TABLE IF NOT EXISTS cache_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) NOT NULL UNIQUE,
    value_hash VARCHAR(64),
    size_bytes INT,
    created_at DATETIME NOT NULL,
    expires_at DATETIME
);

CREATE TABLE IF NOT EXISTS key_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(255) NOT NULL UNIQUE,
    hit_count BIGINT NOT NULL DEFAULT 0,
    miss_count BIGINT NOT NULL DEFAULT 0,
    recompute_cost_ms INT DEFAULT 0,
    last_accessed_at DATETIME
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT,
    event_type VARCHAR(64) NOT NULL,
    detail TEXT,
    event_time DATETIME NOT NULL
);
