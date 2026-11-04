package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cache.LocalStore;
import com.yp.selfheal.dc.ring.RingPartitioner;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class MerkleTreeService {

    private final RingPartitioner partitioner;
    private final LocalStore<Object> localStore;

    public MerkleTreeService(RingPartitioner partitioner, LocalStore<Object> localStore) {
        this.partitioner = partitioner;
        this.localStore = localStore;
    }

    public MerkleNode buildTreeForRange(long startHash, long endHash) {
        List<Map.Entry<Long, String>> leafEntries = new ArrayList<>();

        localStore.rawEntries().forEach((key, entry) -> {
            long hash = partitioner.hash(key);
            if (hash >= startHash && hash <= endHash) {
                String valHash = computeSha256(String.valueOf(entry.getValue()));
                leafEntries.add(Map.entry(hash, valHash));
            }
        });

        leafEntries.sort(Comparator.comparingLong(Map.Entry::getKey));

        if (leafEntries.isEmpty()) {
            return new MerkleNode(computeSha256("EMPTY_RANGE"), startHash, endHash);
        }

        return constructSubTree(leafEntries, startHash, endHash);
    }

    private MerkleNode constructSubTree(List<Map.Entry<Long, String>> entries, long start, long end) {
        if (entries.size() == 1) {
            return new MerkleNode(entries.get(0).getValue(), start, end);
        }

        int mid = entries.size() / 2;
        long midToken = entries.get(mid).getKey();

        MerkleNode left = constructSubTree(entries.subList(0, mid), start, midToken);
        MerkleNode right = constructSubTree(entries.subList(mid, entries.size()), midToken + 1, end);

        String parentHash = computeSha256(left.getHash() + ":" + right.getHash());
        return new MerkleNode(parentHash, start, end, left, right);
    }

    public List<Long> findMismatchedRanges(MerkleNode local, MerkleNode remote) {
        List<Long> divergentRanges = new ArrayList<>();
        compareNodes(local, remote, divergentRanges);
        return divergentRanges;
    }

    private void compareNodes(MerkleNode n1, MerkleNode n2, List<Long> diffs) {
        if (n1 == null || n2 == null) return;
        if (n1.getHash().equals(n2.getHash())) return;

        if (n1.isLeaf() || n2.isLeaf()) {
            diffs.add(n1.getRangeStart());
            return;
        }

        compareNodes(n1.getLeft(), n2.getLeft(), diffs);
        compareNodes(n1.getRight(), n2.getRight(), diffs);
    }

    private String computeSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(b);
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
