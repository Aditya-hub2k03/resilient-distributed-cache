package com.yp.selfheal.dc.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU Eviction Policy using a doubly-linked list + concurrent map index.
 */
public class LruEvictionPolicy implements EvictionPolicy {

    private static class Node {
        final String key;
        Node prev;
        Node next;

        Node(String key) {
            this.key = key;
        }
    }

    private final ConcurrentHashMap<String, Node> nodeMap = new ConcurrentHashMap<>();
    private final Node head = new Node(null);
    private final Node tail = new Node(null);
    private final ReentrantLock lock = new ReentrantLock();

    public LruEvictionPolicy() {
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public void recordAccess(String key) {
        lock.lock();
        try {
            Node node = nodeMap.get(key);
            if (node != null) {
                moveToHead(node);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recordPut(String key) {
        lock.lock();
        try {
            Node existing = nodeMap.get(key);
            if (existing != null) {
                moveToHead(existing);
            } else {
                Node newNode = new Node(key);
                nodeMap.put(key, newNode);
                addFirst(newNode);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void recordRemove(String key) {
        lock.lock();
        try {
            Node node = nodeMap.remove(key);
            if (node != null) {
                removeNode(node);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String evictKey() {
        lock.lock();
        try {
            if (tail.prev == head) {
                return null;
            }
            Node victim = tail.prev;
            removeNode(victim);
            nodeMap.remove(victim.key);
            return victim.key;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            nodeMap.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.unlock();
        }
    }

    private void addFirst(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addFirst(node);
    }
}
