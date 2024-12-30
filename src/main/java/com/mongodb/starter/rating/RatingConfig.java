package com.mongodb.starter.rating;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "feature.rating")
@Getter
@Setter
public class RatingConfig {
    private boolean enabled;
    private int requestsPerHour;  
    private int burstSize;   

}