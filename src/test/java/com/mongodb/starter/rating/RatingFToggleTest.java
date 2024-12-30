package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RatingFeatureToggleTest {

    @Mock
    private RatingRepository ratingRepository;
    
    @Mock
    private RatingConfig ratingConfig;
    
    @Mock
    private RatingThrottler ratingThrottler;
    
    private RatingService ratingService;
    private Rating testRating;
    
    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, ratingConfig, ratingThrottler);
        testRating = new Rating();
        testRating.setId("test-id");
        testRating.setDescription("Test description");
        testRating.setRating(4);
        testRating.setUserId("test-user");
        testRating.setCourseId("test-course");
        testRating.setDate(LocalDateTime.now());

        lenient().when(ratingRepository.findById("test-id")).thenReturn(Optional.of(testRating));
        lenient().when(ratingThrottler.allowRequest("test-user")).thenReturn(true);
    }
    
    @Test
    void shouldSaveRatingWhenFeatureEnabled() {
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingThrottler.allowRequest("test-user")).thenReturn(true);
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);

        Rating savedRating = ratingService.saveRating(testRating);

        assertNotNull(savedRating);
        assertEquals(testRating.getId(), savedRating.getId());
        verify(ratingRepository).save(testRating);
    }
    
    @Test
    void shouldAllowReadOperationsRegardlessOfFeatureToggle() {
        lenient().when(ratingConfig.isEnabled()).thenReturn(false);

        when(ratingRepository.findById("test-id")).thenReturn(Optional.of(testRating));
        when(ratingRepository.findAll()).thenReturn(java.util.List.of(testRating));

        assertDoesNotThrow(() -> {
            Rating result = ratingService.findRatingById("test-id");
            assertNotNull(result);

            Collection<Rating> allRatings = ratingService.findAll();
            assertFalse(allRatings.isEmpty());
        });

    verify(ratingRepository).findById("test-id");
    verify(ratingRepository).findAll();
}
    
    @Test
    void shouldThrowExceptionWhenDeletingWithFeatureDisabled() {
        when(ratingConfig.isEnabled()).thenReturn(false);
        
        assertThrows(RatingService.FeatureDisabledException.class, () -> {
            ratingService.deleteRating("test-id");
        });
        
        verify(ratingRepository, never()).delete(any(Rating.class));
    }

    @Test
    void shouldDeleteRatingWhenFeatureEnabled() {
        when(ratingConfig.isEnabled()).thenReturn(true);
        doNothing().when(ratingRepository).delete(any(Rating.class));
        when(ratingThrottler.allowRequest("test-user")).thenReturn(true);

        assertDoesNotThrow(() -> {
            ratingService.deleteRating("test-id");
        });

        verify(ratingRepository).delete(testRating);
    }
}