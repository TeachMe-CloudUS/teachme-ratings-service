package com.mongodb.starter.rating;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.config.TestContainersConfig;

@SpringBootTest
@AutoConfigureMockMvc
class RatingControllerIntegrationTest extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
    }

    @Test
    void createRating() throws Exception {
        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);
        rating.setUser("testUser");
        rating.setCourse("course1");

        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rating)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("description"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.user").value("testUser"))
                .andExpect(jsonPath("$.course").value("course1"));
    }

    @Test
    void getAllRatings() throws Exception {
        Rating rating1 = createTestRating("course1");
        Rating rating2 = createTestRating("course2");
        ratingRepository.saveAll(List.of(rating1, rating2));

        mockMvc.perform(get("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(2)))
                .andExpect(jsonPath("$[*].course", containsInAnyOrder("course1", "course2")));
    }

    @Test
    void returnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/{ratingId}", "nonexistent-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private Rating createTestRating(String course) {
        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);
        rating.setUser("testUser");
        rating.setCourse(course);
        return rating;
    }

    @Test
    void handleConcurrentUpdates() throws Exception {
        Rating rating = ratingRepository.save(createTestRating("course1"));

        Rating updatedRating1 = new Rating();
        updatedRating1.setDescription("Updated 1");
        updatedRating1.setRating(5);
        updatedRating1.setUser("user1");
        updatedRating1.setCourse("course1");

        Rating updatedRating2 = new Rating();
        updatedRating2.setDescription("Updated 2");
        updatedRating2.setRating(3);
        updatedRating2.setUser("user2");
        updatedRating2.setCourse("course1");

        mockMvc.perform(put("/api/v1/ratings/{ratingId}", rating.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedRating1)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/ratings/{ratingId}", rating.getId()) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedRating2)))
                .andExpect(status().isOk());
    }
    
    /*TODO: Excepción por falta de datos*/
    /*
    @Test
    void rejectInvalidRating() throws Exception {
        Rating invalidRating = new Rating();

        mockMvc.perform(post("/api/v1/ratings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRating)))
                .andExpect(status().isOk());  
    }
    */
    
}
