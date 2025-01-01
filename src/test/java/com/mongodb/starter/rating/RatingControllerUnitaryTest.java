package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.exceptions.ValidationException;
import com.mongodb.starter.student.StudentDto;
import com.mongodb.starter.student.UserService;
import com.mongodb.starter.util.MessageResponse;

@SpringBootTest
@AutoConfigureTestDatabase
@RunWith(MockitoJUnitRunner.class)
public class RatingControllerUnitaryTest {

    @Mock
    private RatingService ratingService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RatingController ratingController;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RatingValidator ratingValidator;
    
    @Mock
    private RatingConfig ratingConfig;

    @Mock
    private RestTemplate restTemplate;

    private Errors errors;

    private Rating invalidRating;

    private StudentDto studentDto = new StudentDto();


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        invalidRating = new Rating();
        invalidRating.setDescription(""); // Invalid description
        invalidRating.setRating(6); // Invalid rating
        invalidRating.setUserId(""); // Invalid userId
        invalidRating.setUsername(""); // Invalid username
        invalidRating.setCourseId(""); // Invalid courseId

        studentDto = new StudentDto();
        studentDto.setId("user1");
        studentDto.setUsername("user");
    }

    private Rating constructorRating(String id, String description, Integer rating_value, String userId, String courseId, String username){
        Rating rating = new Rating();
        rating.setId(id);
        rating.setDescription(description);
        rating.setRating(rating_value);
        rating.setUserId(userId);
        rating.setCourseId(courseId);
        rating.setDate(LocalDateTime.now());
        rating.setUsername(username);

        return rating; 
    }


    @Test
    public void testCreateRating() throws Exception {
    studentDto.setId("user1");
    studentDto.setUsername("user1");
    String courseId = "course1";
    Rating newRating = constructorRating(null, "Great course!", 5, "user1", courseId, "user");
    Rating savedRating = constructorRating("rating1", "Great course!", 5, "user1", courseId, "user");
    String token = "Bearer validToken";
    Errors errors = new BeanPropertyBindingResult(newRating, "rating");

    when(ratingConfig.isEnabled()).thenReturn(true);
    when(userService.extractUserId(token)).thenReturn("user");
    when(restTemplate.getForObject("/api/v1/students/me", StudentDto.class)).thenReturn(studentDto);
    when(ratingService.saveRating(any(Rating.class))).thenReturn(savedRating);
    when(ratingValidator.validateObject(any())).thenReturn(errors); 

    ResponseEntity<Rating> response = ratingController.create(courseId, token, newRating);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Rating returnedRating = response.getBody();
    assertNotNull(returnedRating);
    assertEquals("Great course!", returnedRating.getDescription());

    verify(ratingService, times(1)).saveRating(any(Rating.class));
    }

    @Test
    public void testDelete_Success() throws Exception {
        // Mock data
        String courseId = "course1";
        String token = "Bearer validToken";
        String ratingId = "rate1";

        // Mock behavior
        Rating existingRating = constructorRating(ratingId, "Great course!", 5, "user1", courseId, "user");
        when(userService.extractUserId(token)).thenReturn("user1");
        when(ratingService.findRatingById(ratingId)).thenReturn(existingRating);

        // Call the method
        ResponseEntity<MessageResponse> response = ratingController.delete(courseId,token,ratingId);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rating deleted!", response.getBody().getMessage());

    }

    @Test
    public void testUpdateRating() throws Exception {
        String courseId = "course1";
        String token = "Bearer validToken";
        String ratingId = "rating1";
        Rating existingRating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1", "user");
        Rating updatedRating = constructorRating("rate1", "Bueno, tampoco estaba tan mal", 2, "user1", "course1", "user");
        Errors errors = new BeanPropertyBindingResult(updatedRating, "rating");

        when(userService.extractUserId(token)).thenReturn("user1");
        when(ratingService.findRatingById(ratingId)).thenReturn(existingRating);
        when(ratingService.updateRating(any(Rating.class), eq(ratingId))).thenReturn(updatedRating);
        when(ratingValidator.validateObject(any())).thenReturn(errors); 

        ResponseEntity<Rating> response = ratingController.update(courseId, ratingId, token, updatedRating);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rating returnedRating = response.getBody();
        assertNotNull(returnedRating);
        assertEquals("Bueno, tampoco estaba tan mal", returnedRating.getDescription());

    }

    @Test
    public void testFindRatingById() throws Exception {
        Rating rating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1", "user");
        when(ratingService.findRatingById("rate1")).thenReturn(rating);

        ResponseEntity<Rating> response = ratingController.findById("rate1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Rating returnedRating = response.getBody();
        assertNotNull(returnedRating);
        assertEquals("No me ha gustado nada", returnedRating.getDescription());


    }

    @Test
    public void testFindRatingByCourse() throws Exception {

        Rating rating = constructorRating("rate1","No me ha gustado nada",1,"user1","course1", "user");
        when(ratingService.findAllRatingsByCourse("course1")).thenReturn((Arrays.asList(rating)));

        ResponseEntity<List<Rating>> response = ratingController.findAllByCourse("course1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Rating> result = response.getBody();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

}
