package com.yp.selfheal.dc.ring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe consistent hash ring with virtual nodes.
 */
@Component
public class ConsistentHashRing {

    private final RingPartitioner partitioner;
    private final int virtualNodesPerNode;
    private final ConcurrentSkipListMap<Long, VirtualNode> ring = new ConcurrentSkipListMap<>();
    private final Set<String> physicalNodes = Collections.synchronizedSet(new HashSet<>());
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ConsistentHashRing(
            RingPartitioner partitioner,
            @Value("${dc.cluster.virtual-nodes-per-node:100}") int virtualNodesPerNode) {
        this.partitioner = partitioner;
        this.virtualNodesPerNode = virtualNodesPerNode;
    }

    public void addNode(String nodeId) {
        rwLock.writeLock().lock();
        try {
            if (physicalNodes.add(nodeId)) {
                for (int i = 0; i < virtualNodesPerNode; i++) {
                    VirtualNode vNode = new VirtualNode(nodeId, i);
                    long hash = partitioner.hash(vNode.getNodeKey());
                    ring.put(hash, vNode);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeNode(String nodeId) {
        rwLock.writeLock().lock();
        try {
            if (physicalNodes.remove(nodeId)) {
                for (int i = 0; i < virtualNodesPerNode; i++) {
                    VirtualNode vNode = new VirtualNode(nodeId, i);
                    long hash = partitioner.hash(vNode.getNodeKey());
                    ring.remove(hash);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Optional<String> routeKey(String key) {
        rwLock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return Optional.empty();
            }
            long keyHash = partitioner.hash(key);
            Map.Entry<Long, VirtualNode> entry = ring.ceilingEntry(keyHash);
            if (entry == null) {
                entry = ring.firstEntry();
            }
            return Optional.of(entry.getValue().getNodeId());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<String> getSuccessorNodes(String key, int count) {
        rwLock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return Collections.emptyList();
            }
            Set<String> selectedNodes = new LinkedHashSet<>();
            long keyHash = partitioner.hash(key);

            NavigableMap<Long, VirtualNode> tailMap = ring.tailMap(keyHash);
            for (VirtualNode vNode : tailMap.values()) {
                selectedNodes.add(vNode.getNodeId());
                if (selectedNodes.size() == count || selectedNodes.size() == physicalNodes.size()) {
                    return new ArrayList<>(selectedNodes);
                }
            }

            for (VirtualNode vNode : ring.values()) {
                selectedNodes.add(vNode.getNodeId());
                if (selectedNodes.size() == count || selectedNodes.size() == physicalNodes.size()) {
                    break;
                }
            }
            return new ArrayList<>(selectedNodes);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<Long, String> getRingSnapshot() {
        rwLock.readLock().lock();
        try {
            Map<Long, String> snapshot = new LinkedHashMap<>();
            ring.forEach((hash, vnode) -> snapshot.put(hash, vnode.getNodeKey()));
            return snapshot;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Set<String> getPhysicalNodes() {
        return new HashSet<>(physicalNodes);
    }

    public int size() {
        return physicalNodes.size();
    }
}
