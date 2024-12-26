package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestPropertySource(properties = {
    "feature.rating.enabled=true",
    "feature.rating.requests-per-hour=10",
    "feature.rating.burst-size=10"
})
public class RatingThrottlerTest {

    @Autowired
    private RatingThrottler ratingThrottler;

    @Autowired
    private RatingConfig ratingConfig;

    @BeforeEach
    public void setUp() {
        ratingThrottler.getBuckets().clear();
        
        System.out.println("Test Configuration - Burst Size: " + ratingConfig.getBurstSize() + 
                          ", Requests Per Hour: " + ratingConfig.getRequestsPerHour());
        
        assertTrue(ratingConfig.getBurstSize() > 0, "Burst size should be greater than 0");
        assertTrue(ratingConfig.getRequestsPerHour() > 0, "Requests per hour should be greater than 0");
    }

    @Test
    public void testAllowRequestWithinBurstSize() {
        String userId = "testUser1";
        int burstSize = ratingConfig.getBurstSize();
        
        System.out.println("Starting burst size test with configured burst size: " + burstSize);
        
        for (int i = 0; i < burstSize; i++) {
            boolean allowed = ratingThrottler.allowRequest(userId);
            System.out.println("Request " + (i + 1) + " allowed: " + allowed);
            assertTrue(allowed, "Request " + (i + 1) + " should be allowed (within burst size)");
        }
    }

    @Test
    public void testAllowRequestBeyondBurstSize() {
        String userId = "testUser2";
        int burstSize = ratingConfig.getBurstSize();
        
        System.out.println("Testing requests beyond burst size: " + burstSize);
        
        for (int i = 0; i < burstSize; i++) {
            boolean allowed = ratingThrottler.allowRequest(userId);
            System.out.println("Initial request " + (i + 1) + " allowed: " + allowed);
            assertTrue(allowed, "Request " + (i + 1) + " should be allowed");
        }

        assertFalse(ratingThrottler.allowRequest(userId), "Request beyond burst size should be throttled");
    }

    @Test
    public void testAllowRequestExceedingRequestsPerHour() {
        String userId = "testUser3";
        int requestsPerHour = ratingConfig.getRequestsPerHour();
        
        System.out.println("Testing requests per hour limit: " + requestsPerHour);
        
        for (int i = 0; i < requestsPerHour; i++) {
            boolean allowed = ratingThrottler.allowRequest(userId);
            System.out.println("Request " + (i + 1) + " allowed: " + allowed);
            assertTrue(allowed, "Request " + (i + 1) + " should be allowed");
        }

        assertFalse(ratingThrottler.allowRequest(userId), "Request exceeding hourly limit should be throttled");
    }

    @Test
    public void testBucketCleaningAfterInactivity() throws Exception {
        String userId = "testUser4";
        int burstSize = ratingConfig.getBurstSize();
        
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < burstSize; i++) {
            assertTrue(ratingThrottler.allowRequest(userId, currentTime), 
                    "Initial request " + (i + 1) + " should be allowed");
        }

        long newTime = currentTime + TimeUnit.HOURS.toMillis(1);
        
        assertTrue(ratingThrottler.allowRequest(userId, newTime), 
                "Request after 1 hour should be allowed");
    }
}