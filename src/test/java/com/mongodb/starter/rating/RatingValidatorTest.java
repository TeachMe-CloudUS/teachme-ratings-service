package com.mongodb.starter.rating;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@SpringBootTest
public class RatingValidatorTest {
    
    private final RatingValidator validator = new RatingValidator();

    @Test
    public void validate_shouldPassForValidRating() {
        Rating rating = new Rating();
        rating.setDescription("A valid description.");
        rating.setRating(4);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertFalse(errors.hasErrors(), "Expected no validation errors for a valid rating.");
    }

    @Test
    public void validate_shouldFailForNullDescription() {
        Rating rating = new Rating();
        rating.setDescription(null);
        rating.setRating(3);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("description"), "Expected validation error for empty description.");
    }

    @Test
    public void validate_shouldFailForEmptyDescription() {
        Rating rating = new Rating();
        rating.setDescription("");
        rating.setRating(3);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("description"), "Expected validation error for empty description.");
    }

    @Test
    public void validate_shouldFailForTooLongDescription() {
        Rating rating = new Rating();
        rating.setDescription("A".repeat(501));
        rating.setRating(3);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("description"), "Expected validation error for description exceeding 500 characters.");
    }

    @Test
    public void validate_shouldFailForInvalidRatingNumber() {
        Rating rating = new Rating();
        rating.setDescription("Valid description.");
        rating.setRating(6);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("rating"), "Expected validation error for rating number outside valid range.");
    }

    @Test
    public void validate_shouldFailForInvalidRatingNumber2() {
        Rating rating = new Rating();
        rating.setDescription("Valid description.");
        rating.setRating(0);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("rating"), "Expected validation error for rating number outside valid range.");
    }

    @Test
    public void validate_shouldFailForEmptyUserId() {
        Rating rating = new Rating();
        rating.setDescription("Valid description.");
        rating.setRating(4);
        rating.setUserId("");
        rating.setUsername("validUsername");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("userId"), "Expected validation error for empty userId.");
    }

    @Test
    public void validate_shouldFailForEmptyUsername() {
        Rating rating = new Rating();
        rating.setDescription("Valid description.");
        rating.setRating(4);
        rating.setUserId("validUserId");
        rating.setUsername("");
        rating.setCourseId("validCourseId");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("username"), "Expected validation error for empty username.");
    }

    @Test
    public void validate_shouldFailForEmptyCourseId() {
        Rating rating = new Rating();
        rating.setDescription("Valid description.");
        rating.setRating(4);
        rating.setUserId("validUserId");
        rating.setUsername("validUsername");
        rating.setCourseId("");

        Errors errors = new BeanPropertyBindingResult(rating, "rating");
        validator.validate(rating, errors);

        assertTrue(errors.hasFieldErrors("courseId"), "Expected validation error for empty courseId.");
    }
}
