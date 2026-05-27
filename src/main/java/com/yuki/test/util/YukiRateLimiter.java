package com.yuki.test.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class YukiRateLimiter {

    private final ConcurrentHashMap<Long, AtomicInteger> concurrentMap = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long userId, int maxConcurrent) {
        AtomicInteger count = concurrentMap.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int current;
        do {
            current = count.get();
            if (current >= maxConcurrent) {
                return false;
            }
        } while (!count.compareAndSet(current, current + 1));
        return true;
    }

    public void release(Long userId) {
        AtomicInteger count = concurrentMap.get(userId);
        if (count == null) {
            return;
        }
        int remaining = count.decrementAndGet();
        if (remaining <= 0) {
            concurrentMap.remove(userId, count);
        }
    }
}
