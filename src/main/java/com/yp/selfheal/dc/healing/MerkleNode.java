package com.yp.selfheal.dc.healing;

import java.io.Serializable;

public class MerkleNode implements Serializable {
    private final String hash;
    private final long rangeStart;
    private final long rangeEnd;
    private final MerkleNode left;
    private final MerkleNode right;

    public MerkleNode(String hash, long rangeStart, long rangeEnd, MerkleNode left, MerkleNode right) {
        this.hash = hash;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.left = left;
        this.right = right;
    }

    public MerkleNode(String hash, long rangeStart, long rangeEnd) {
        this(hash, rangeStart, rangeEnd, null, null);
    }

    public String getHash() { return hash; }
    public long getRangeStart() { return rangeStart; }
    public long getRangeEnd() { return rangeEnd; }
    public MerkleNode getLeft() { return left; }
    public MerkleNode getRight() { return right; }
    public boolean isLeaf() { return left == null && right == null; }
}
