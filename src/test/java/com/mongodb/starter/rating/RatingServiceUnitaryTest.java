package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import com.mongodb.starter.exceptions.ResourceNotFoundException;

@SpringBootTest
@AutoConfigureTestDatabase
@RunWith(MockitoJUnitRunner.class)
public class RatingServiceUnitaryTest{

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingConfig ratingConfig;

    @Mock
    private RatingThrottler ratingThrottler;

    @InjectMocks
    private RatingService ratingService;


    private Rating constructorRating(String id, String description, Integer rating_value, String userId, String courseId){
       Rating rating = new Rating();
        rating.setId(id);
        rating.setDescription(description);
        rating.setRating(rating_value);
        rating.setUserId(userId);
        rating.setCourseId(courseId);
        rating.setDate(LocalDateTime.now());

        return rating; 
    }

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.openMocks(this);
        Rating rating1 = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating rating2 = constructorRating("rate2","No estaba mal",3,"user2","course1");
        Rating rating3 = constructorRating("rate3","Me ha encantado",5,"user3","course2");
        ratingRepository.save(rating1);
        ratingRepository.save(rating2);
        ratingRepository.save(rating3);
    }
    
    @Test
    public void shouldReturnAllRatings(){
        Rating rating1 = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating rating2 = constructorRating("rate2","No estaba mal",3,"user2","course1");
        Rating rating3 = constructorRating("rate3","Me ha encantado",5,"user3","course2");
        when(ratingRepository.findAll()).thenReturn(Arrays.asList(rating1, rating2, rating3));

        Collection<Rating> result = ratingService.findAll();

        assertEquals(3, result.size());
        verify(ratingRepository, times(1)).findAll();
    }

    @Test
    public void shouldReturnAllRatingsByCourse(){
        Rating rating1 = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating rating2 = constructorRating("rate2","No estaba mal",3,"user2","course1");
        when(ratingRepository.findAllRatingsByCourse("course1")).thenReturn(Arrays.asList(rating1, rating2));

        Collection<Rating> result = ratingService.findAllRatingsByCourse("course1");

        assertEquals(2, result.size());
        verify(ratingRepository, times(1)).findAllRatingsByCourse("course1");
    }

    @Test
    public void shouldSaveRating(){
        Rating rating1 = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");

        when(ratingRepository.save(rating1)).thenReturn(rating1);
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingThrottler.allowRequest(any())).thenReturn(true);

        Rating result = ratingService.saveRating(rating1);

        assertEquals(rating1, result);
        verify(ratingRepository, times(1)).save(rating1);
    }

    @Test
    public void shouldUpdateRating(){
        Rating existingRating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating updatedRating = constructorRating("rate1", "Bueno, tampoco estaba tan mal", 2, "user1", "course1");
        when(ratingRepository.findById("rate1")).thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(existingRating)).thenReturn(updatedRating);
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingThrottler.allowRequest(any())).thenReturn(true);

        Rating result = ratingService.updateRating(updatedRating,"rate1");

        assertEquals("Bueno, tampoco estaba tan mal", result.getDescription());
        assertEquals(2, result.getRating());
        assertEquals("user1", result.getUserId());
        assertEquals("course1", result.getCourseId());

        verify(ratingRepository, times(1)).findById("rate1");
        verify(ratingRepository, times(1)).save(existingRating);

    }

    @Test
    public void testDeleteRating() {
        String ratingId = "rate1";
        Rating existingRating = new Rating();
        existingRating.setId(ratingId);

        when(ratingRepository.findById(ratingId)).thenReturn(Optional.of(existingRating));
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingThrottler.allowRequest(any())).thenReturn(true);
        
        doNothing().when(ratingRepository).delete(existingRating);

        ratingService.deleteRating(ratingId);

        verify(ratingRepository, times(1)).findById(ratingId);
        verify(ratingRepository, times(1)).delete(existingRating);
    }

    @Test
    public void testRatingMean() {
        String courseId = "course1";
        Rating rating1 = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating rating2 = constructorRating("rate2","No estaba mal",2,"user2","course1");

        when(ratingService.findAllRatingsByCourse(courseId)).thenReturn(Arrays.asList(rating1, rating2));

        Double result = ratingService.ratingMean(courseId);

        assertEquals(1.5, result);

    }


//CASOS NEGATIVOS

    private static final String RATING_ID = "rating123";
    private static final String COURSE_ID = "course123";
    private static final String USER_ID = "user123";

    @Test
    public void findRatingById_ShouldThrowResourceNotFoundException_WhenRatingNotFound() {
        // Given
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

        // When/Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> ratingService.findRatingById(RATING_ID)
        );

        assertEquals("Rating not found with ID: 'rating123'", exception.getMessage());
        verify(ratingRepository).findById(RATING_ID);
    }

    @Test
    public void saveRating_ShouldThrowFeatureDisabledException_WhenFeatureDisabled() {
        // Given
        Rating rating = new Rating();
        rating.setUserId(USER_ID);
        when(ratingConfig.isEnabled()).thenReturn(false);

        // When/Then
        RatingService.FeatureDisabledException exception = assertThrows(
            RatingService.FeatureDisabledException.class,
            () -> ratingService.saveRating(rating)
        );

        assertEquals("Rating feature is currently disabled", exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingThrottler, never()).allowRequest(any());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    public void saveRating_ShouldThrowThrottlingException_WhenRateLimitExceeded() {
        // Given
        Rating rating = new Rating();
        rating.setUserId(USER_ID);
        
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingThrottler.allowRequest(USER_ID)).thenReturn(false);

        // When/Then
        RatingService.ThrottlingException exception = assertThrows(
            RatingService.ThrottlingException.class,
            () -> ratingService.saveRating(rating)
        );

        assertEquals("Rate limit exceeded for user: " + USER_ID, exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingThrottler).allowRequest(USER_ID);
        verify(ratingRepository, never()).save(any());
    }

    @Test
    public void updateRating_ShouldThrowResourceNotFoundException_WhenRatingNotFound() {
        // Given
        Rating rating = new Rating();
        rating.setUserId(USER_ID);
        
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

        // When/Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> ratingService.updateRating(rating, RATING_ID)
        );

        verify(ratingConfig).isEnabled();
        verify(ratingRepository).findById(RATING_ID);
        verify(ratingThrottler, never()).allowRequest(any());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    public void updateRating_ShouldThrowFeatureDisabledException_WhenFeatureDisabled() {
        // Given
        Rating rating = new Rating();
        rating.setUserId(USER_ID);
        
        when(ratingConfig.isEnabled()).thenReturn(false);

        // When/Then
        RatingService.FeatureDisabledException exception = assertThrows(
            RatingService.FeatureDisabledException.class,
            () -> ratingService.updateRating(rating, RATING_ID)
        );

        assertEquals("Rating feature is currently disabled", exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingRepository, never()).findById(any());
        verify(ratingThrottler, never()).allowRequest(any());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    public void updateRating_ShouldThrowThrottlingException_WhenRateLimitExceeded() {
        // Given
        Rating rating = new Rating();
        rating.setUserId(USER_ID);
        
        Rating existingRating = new Rating();
        existingRating.setId(RATING_ID);
        
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(existingRating));
        when(ratingThrottler.allowRequest(USER_ID)).thenReturn(false);

        // When/Then
        RatingService.ThrottlingException exception = assertThrows(
            RatingService.ThrottlingException.class,
            () -> ratingService.updateRating(rating, RATING_ID)
        );

        assertEquals("Rate limit exceeded for user: " + USER_ID, exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingRepository).findById(RATING_ID);
        verify(ratingThrottler).allowRequest(USER_ID);
        verify(ratingRepository, never()).save(any());
    }

    @Test
    public void deleteRating_ShouldThrowFeatureDisabledException_WhenFeatureDisabled() {
        // Given
        when(ratingConfig.isEnabled()).thenReturn(false);

        // When/Then
        RatingService.FeatureDisabledException exception = assertThrows(
            RatingService.FeatureDisabledException.class,
            () -> ratingService.deleteRating(RATING_ID)
        );

        assertEquals("Rating feature is currently disabled", exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingRepository, never()).findById(any());
        verify(ratingThrottler, never()).allowRequest(any());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    public void deleteRating_ShouldThrowResourceNotFoundException_WhenRatingNotFound() {
        // Given
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

        // When/Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> ratingService.deleteRating(RATING_ID)
        );

        verify(ratingConfig).isEnabled();
        verify(ratingRepository).findById(RATING_ID);
        verify(ratingThrottler, never()).allowRequest(any());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    public void deleteRating_ShouldThrowThrottlingException_WhenRateLimitExceeded() {
        // Given
        Rating existingRating = new Rating();
        existingRating.setId(RATING_ID);
        existingRating.setUserId(USER_ID);
        
        when(ratingConfig.isEnabled()).thenReturn(true);
        when(ratingRepository.findById(RATING_ID)).thenReturn(Optional.of(existingRating));
        when(ratingThrottler.allowRequest(USER_ID)).thenReturn(false);

        // When/Then
        RatingService.ThrottlingException exception = assertThrows(
            RatingService.ThrottlingException.class,
            () -> ratingService.deleteRating(RATING_ID)
        );

        assertEquals("Rate limit exceeded for user: " + USER_ID, exception.getMessage());
        verify(ratingConfig).isEnabled();
        verify(ratingRepository).findById(RATING_ID);
        verify(ratingThrottler).allowRequest(USER_ID);
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    public void ratingMean_ShouldReturnZero_WhenNoRatingsExist() {
        // Given
        when(ratingRepository.findAllRatingsByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        // When
        Double result = ratingService.ratingMean(COURSE_ID);

        // Then
        assertEquals(0.0, result);
        verify(ratingRepository).findAllRatingsByCourse(COURSE_ID);
    }

    @Test
    public void findAllRatingsByCourse_ShouldReturnEmptyList_WhenNoRatingsExist() {
        // Given
        when(ratingRepository.findAllRatingsByCourse(COURSE_ID)).thenReturn(Collections.emptyList());

        // When
        List<Rating> result = ratingService.findAllRatingsByCourse(COURSE_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(ratingRepository).findAllRatingsByCourse(COURSE_ID);
    }




}
