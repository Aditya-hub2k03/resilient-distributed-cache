package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cache.CacheEntry;
import com.yp.selfheal.dc.cache.LocalStore;
import com.yp.selfheal.dc.ring.RingPartitioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MerkleTreeServiceTest {

    private MerkleTreeService merkleTreeService;
    private LocalStore<Object> localStore;

    @BeforeEach
    void setUp() {
        RingPartitioner partitioner = new RingPartitioner();
        localStore = new LocalStore<>();
        merkleTreeService = new MerkleTreeService(partitioner, localStore);
    }

    @Test
    void testIdenticalTreesYieldZeroMismatches() {
        localStore.put("k1", new CacheEntry<>("k1", "v1", null));
        localStore.put("k2", new CacheEntry<>("k2", "v2", null));

        MerkleNode tree1 = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);
        MerkleNode tree2 = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);

        assertEquals(tree1.getHash(), tree2.getHash());
        List<Long> diffs = merkleTreeService.findMismatchedRanges(tree1, tree2);
        assertTrue(diffs.isEmpty());
    }

    @Test
    void testDifferentTreesDetectMismatches() {
        localStore.put("k1", new CacheEntry<>("k1", "v1", null));
        MerkleNode tree1 = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);

        localStore.put("k1", new CacheEntry<>("k1", "v1-mutated", null));
        MerkleNode tree2 = merkleTreeService.buildTreeForRange(0, Long.MAX_VALUE);

        assertNotEquals(tree1.getHash(), tree2.getHash());
        List<Long> diffs = merkleTreeService.findMismatchedRanges(tree1, tree2);
        assertFalse(diffs.isEmpty());
    }
}
