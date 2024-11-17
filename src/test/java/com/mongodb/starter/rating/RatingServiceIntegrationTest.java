package com.mongodb.starter.rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.mongodb.starter.config.TestContainersConfig;
import com.mongodb.starter.exceptions.ResourceNotFoundException;

@SpringBootTest
class RatingServiceIntegrationTest extends TestContainersConfig {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private RatingRepository ratingRepository;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        Rating existingRating = new Rating();
        existingRating.setDescription("description");
        existingRating.setRating(3);
        existingRating.setUser("testUser");
        existingRating.setCourse("course1");
        
        ratingRepository.save(existingRating);
    }

    @Test
    void findAllRatings() {
        ratingRepository.deleteAll();

        Rating rating1 = createTestRating("course1");
        Rating rating2 = createTestRating("course2");
        ratingRepository.saveAll(List.of(rating1, rating2));

        Collection<Rating> foundRatings = ratingService.findAll();

        assertThat(foundRatings)
            .hasSize(2)
            .extracting("course")
            .containsExactlyInAnyOrder("course1", "course2");
    }

    @Test
    void findRatingById() {
        Rating rating = ratingRepository.save(createTestRating("course1"));
        Rating foundRating = ratingService.findRatingById(rating.getId());

        assertThat(foundRating)
            .isNotNull()
            .satisfies(found -> {
                assertThat(found.getId()).isEqualTo(rating.getId());
                assertThat(found.getDescription()).isEqualTo(rating.getDescription());
                assertThat(found.getRating()).isEqualTo(rating.getRating());
                assertThat(found.getUser()).isEqualTo(rating.getUser());
                assertThat(found.getCourse()).isEqualTo(rating.getCourse());
            });
    }

    @Test
    void throwExceptionNonExistentRating() {
        assertThrows(ResourceNotFoundException.class, 
            () -> ratingService.findRatingById("nonexistent-id"));
    }

    @Test
    void saveRating() {
        Rating rating = createTestRating("course1");
        Rating savedRating = ratingService.saveRating(rating);

        assertThat(savedRating.getId()).isNotNull();
        assertThat(ratingRepository.findById(savedRating.getId()))
            .isPresent()
            .get()
            .satisfies(found -> {
                assertThat(found.getDescription()).isEqualTo(rating.getDescription());
                assertThat(found.getRating()).isEqualTo(rating.getRating());
                assertThat(found.getUser()).isEqualTo(rating.getUser());
                assertThat(found.getCourse()).isEqualTo(rating.getCourse());
            });
    }

    @Test
    void updateRating() {
        Rating existingRating = ratingRepository.save(createTestRating("course1"));
        
        // System.out.println("\n\nExistingRating: " + existingRating.getCourse());

        Rating updateData = new Rating();
        updateData.setDescription("Updated description");
        updateData.setRating(5);
        updateData.setUser("updatedUser");
        updateData.setCourse(existingRating.getCourse());

        // System.out.println("\n\nUpdateData: " + updateData.getCourse());

        Rating updatedRating = ratingService.updateRating(updateData, existingRating.getId());
        //System.out.println("\n\nUpdatedRating: " + updatedRating.getCourse());

        assertThat(updatedRating)
            .satisfies(rating -> {
                assertThat(rating.getId()).isEqualTo(existingRating.getId());
                assertThat(rating.getDescription()).isEqualTo(updateData.getDescription());
                assertThat(rating.getRating()).isEqualTo(updateData.getRating());
                assertThat(rating.getUser()).isEqualTo(updateData.getUser());
                assertThat(rating.getCourse()).isEqualTo(existingRating.getCourse());
            });
    }

    @Test
    void throwExceptionWhenUpdatingNonExistentRating() {
        Rating updateData = createTestRating("course1");

        assertThrows(ResourceNotFoundException.class, 
            () -> ratingService.updateRating(updateData, "nonexistent-id"));
    }

    @Test
    void deleteRating() {
        Rating rating = ratingRepository.save(createTestRating("course1"));
        ratingService.deleteRating(rating.getId());
        assertThat(ratingRepository.findById(rating.getId())).isEmpty();
    }

    @Test
    void throwExceptionWhenDeletingNonExistentRating() {
        assertThrows(ResourceNotFoundException.class, 
            () -> ratingService.deleteRating("nonexistent-id"));
    }

    private Rating createTestRating(String course) {
        Rating rating = new Rating();
        rating.setDescription("Test description");
        rating.setRating(4);
        rating.setUser("testUser");
        rating.setCourse(course);
        rating.setDate(LocalDateTime.now());
        return rating;
    }

    @Test
    void saveRatingWithMinimumValidValues() {
        Rating minimumValidRating = new Rating();
        minimumValidRating.setDescription("description");
        minimumValidRating.setRating(1);
        minimumValidRating.setUser("user1");
        minimumValidRating.setCourse("course1");
    
        Rating savedRating = ratingService.saveRating(minimumValidRating);
    
        assertThat(savedRating.getId()).isNotNull();
        assertThat(ratingRepository.findById(savedRating.getId())).isPresent();
    }

    @Test
    void saveRatingWithCurrentDate() {
        Rating rating = createTestRating("course1");
        Rating savedRating = ratingService.saveRating(rating);

        assertThat(savedRating.getDate()).isNotNull();
        assertThat(savedRating.getDate()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    /*TODO Nuevos métodos que generan excepciones*/

    /*
    @Test
    void shouldThrowValidationExceptionWhenSavingRatingWithNullFields() {
        Rating nullFieldsRating = new Rating();
        nullFieldsRating.setRating(4); // Rating válido, pero otros campos son nulos

        assertThrows(ConstraintViolationException.class, 
        () -> ratingService.saveRating(nullFieldsRating));
    }

    @Test
    void shouldThrowValidationExceptionWhenSavingRatingWithInvalidScore() {
        Rating invalidScoreRating = new Rating();
        invalidScoreRating.setDescription("Invalid score");
        invalidScoreRating.setRating(0); // Valor menor al mínimo permitido
        invalidScoreRating.setUser("user1");
        invalidScoreRating.setCourse("course1");

        assertThrows(ValidationException.class, 
            () -> ratingService.saveRating(invalidScoreRating));
    }

    @Test
    void shouldThrowValidationExceptionWhenUpdatingRatingWithInvalidScore() {
        Rating existingRating = ratingRepository.save(createTestRating("course1"));

        Rating updateData = new Rating();
        updateData.setDescription("Updated description");
        updateData.setRating(0); // Valor inválido
        updateData.setUser("updatedUser");
        updateData.setCourse("course1");

        assertThrows(ValidationException.class, 
            () -> ratingService.updateRating(updateData, existingRating.getId()));
    }

    */
    
}

