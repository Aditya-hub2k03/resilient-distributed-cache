package com.yp.selfheal.dc.ring;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5/Murmur-like 64-bit integer partitioner for placement on the hash ring.
 */
@Component
public class RingPartitioner {

    public long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // Extract low 64-bits
            long h = 0;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (digest[i] & 0xFF);
            }
            return h & 0x7FFFFFFFFFFFFFFFL; // Positive 63-bit range
        } catch (NoSuchAlgorithmException e) {
            return (long) key.hashCode() & 0x7FFFFFFFFFFFFFFFL;
        }
    }
}
