package com.mongodb.starter.rating;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class RatingThrottler {
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final RatingConfig config;

    public RatingThrottler(RatingConfig config) {
        this.config = config;
    }

    public ConcurrentHashMap<String, TokenBucket> getBuckets() {
        return buckets;
    }

    public boolean allowRequest(String userId) {
        return allowRequest(userId, System.currentTimeMillis());
    }

    public boolean allowRequest(String userId, long currentTimeMillis) {
        if (userId == null) {
            return false;
        }

        TokenBucket bucket = buckets.computeIfAbsent(userId,
            k -> new TokenBucket(config.getBurstSize(), config.getRequestsPerHour(), currentTimeMillis));
        
        cleanOldBuckets(currentTimeMillis);
        return bucket.tryConsume(currentTimeMillis);
    }

    private void cleanOldBuckets(long currentTimeMillis) {
        long oneHourAgo = currentTimeMillis - TimeUnit.HOURS.toMillis(1);
        buckets.entrySet().removeIf(entry -> 
            entry.getValue().getLastRefillTimestamp() < oneHourAgo);
    }
}

