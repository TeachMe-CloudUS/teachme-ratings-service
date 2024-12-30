package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.util.MessageResponse;

@SpringBootTest
@AutoConfigureTestDatabase
@RunWith(MockitoJUnitRunner.class)
public class RatingControllerUnitaryTest {

    @Mock
    private RatingService ratingService;

    @InjectMocks
    private RatingController ratingController;

    @Mock
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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


    @Test
    public void testCreateRating() throws Exception {
    String courseId = "course1";
    String studentId = "student1";
    Rating newRating = constructorRating(null, "Great course!", 5, "user1", courseId);
    Rating savedRating = constructorRating("rating1", "Great course!", 5, "user1", courseId);

    when(ratingService.saveRating(any(Rating.class))).thenReturn(savedRating);
    ResponseEntity<Rating> response = ratingController.create(courseId, studentId, newRating);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Rating returnedRating = response.getBody();
    assertNotNull(returnedRating);
    assertEquals("Great course!", returnedRating.getDescription());

    verify(ratingService, times(1)).saveRating(any(Rating.class));
    }

    @Test
    public void testDelete_Success() throws Exception {
        // Mock data
        String courseId = "course1";
        String ratingId = "rate1";

        // Mock behavior
        Rating existingRating = constructorRating(ratingId, "Great course!", 5, "user1", courseId);

        when(ratingService.findRatingById(ratingId)).thenReturn(existingRating);

        // Call the method
        ResponseEntity<MessageResponse> response = ratingController.delete(courseId,ratingId);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rating deleted!", response.getBody().getMessage());

    }

    @Test
    public void testUpdateRating() throws Exception {
        String courseId = "course1";
        String ratingId = "rating1";
        Rating existingRating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        Rating updatedRating = constructorRating("rate1", "Bueno, tampoco estaba tan mal", 2, "user1", "course1");

        when(ratingService.findRatingById(ratingId)).thenReturn(existingRating);
        when(ratingService.updateRating(any(Rating.class), eq(ratingId))).thenReturn(updatedRating);

        ResponseEntity<Rating> response = ratingController.update(courseId, ratingId, updatedRating);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rating returnedRating = response.getBody();
        assertNotNull(returnedRating);
        assertEquals("Bueno, tampoco estaba tan mal", returnedRating.getDescription());

    }

    @Test
    public void testFindRatingById() throws Exception {
        Rating rating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        when(ratingService.findRatingById("rate1")).thenReturn(rating);

        ResponseEntity<Rating> response = ratingController.findById("rate1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rating returnedRating = response.getBody();
        assertNotNull(returnedRating);
        assertEquals("No me ha gustado nada", returnedRating.getDescription());


    }

    @Test
    public void testFindRatingByCourse() throws Exception {

        Rating rating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1");
        when(ratingService.findAllRatingsByCourse("course1")).thenReturn((Arrays.asList(rating)));

        ResponseEntity<List<Rating>> response = ratingController.findAllByCourse("course1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Rating> result = response.getBody();
        assertNotNull(result);
        assertEquals(1, result.size());


    }


}
