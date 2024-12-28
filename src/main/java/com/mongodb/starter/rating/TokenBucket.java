package com.mongodb.starter.rating;

import java.util.concurrent.TimeUnit;

public class TokenBucket {
    private final double capacity;
    private final double refillRate;
    @SuppressWarnings("unused")
    private final double requestsPerHour;
    private double tokens;
    private long lastRefillTimestamp;
    
    public TokenBucket(long burstSize, long requestsPerHour) {
        this(burstSize, requestsPerHour, System.currentTimeMillis());
    }
    
    public TokenBucket(long burstSize, long requestsPerHour, long initialTimestamp) {
        this.capacity = Math.max(burstSize, requestsPerHour);
        this.requestsPerHour = requestsPerHour;
        this.refillRate = (double) requestsPerHour / (3600.0 * 1000.0);
        this.tokens = burstSize;
        this.lastRefillTimestamp = initialTimestamp;
    }

    public synchronized boolean tryConsume(long currentTimeMillis) {
        refill(currentTimeMillis);
        
        if (tokens >= 1.0) {
            tokens --;
            return true;
        }
        return false;
    }

    private void refill(long currentTimeMillis) {
        long elapsedMillis = currentTimeMillis - lastRefillTimestamp;
    
        if (elapsedMillis <= 0) {
            return;
        }

        if (elapsedMillis >= TimeUnit.HOURS.toMillis(1)) {
            tokens = capacity;
        } else {
            double tokensToAdd = elapsedMillis * refillRate;
            tokens = Math.min(capacity, tokens + tokensToAdd);
        }
        
        lastRefillTimestamp = currentTimeMillis;
    }

    public long getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }
}