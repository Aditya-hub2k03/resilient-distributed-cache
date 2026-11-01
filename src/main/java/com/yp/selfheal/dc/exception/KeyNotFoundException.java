package com.yp.selfheal.dc.exception;

public class KeyNotFoundException extends RuntimeException {
    public KeyNotFoundException(String key) {
        super("Cache key not found or expired: " + key);
    }
}
