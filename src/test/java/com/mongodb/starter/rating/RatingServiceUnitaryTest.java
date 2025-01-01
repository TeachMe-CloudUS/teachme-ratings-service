package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
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

    




}
