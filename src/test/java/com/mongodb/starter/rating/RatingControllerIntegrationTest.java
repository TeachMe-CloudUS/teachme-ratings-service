package com.mongodb.starter.rating;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
/*import static org.mockito.Mockito.mock;*/
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.student.StudentDto;
import org.springframework.boot.test.mock.mockito.MockBean;


@SpringBootTest(properties = {
    "resilience4j.circuitbreaker.instances.circuit_active.registerHealthIndicator=false",
    "resilience4j.circuitbreaker.instances.circuit_active.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.circuit_active.minimumNumberOfCalls=5",
    "resilience4j.circuitbreaker.instances.circuit_active.failureRateThreshold=100"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RatingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RatingRepository ratingRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
    }

    /*
    @Test
    void createRating() throws Exception {
        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);

        StudentDto mockStudent = new StudentDto();
        mockStudent.setUserName("userName1");

        when(restTemplate.getForObject("/api/v1/students/{studentId}", 
                               StudentDto.class, "testUser"))
        .thenReturn(mockStudent);

        mockMvc.perform(post("/api/v1/course/course1/ratings/")
                .param("studentId", "testUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rating)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("description"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.courseId").value("course1"))
                .andExpect(jsonPath("$.userName").value("userName1"));

        verify(restTemplate).getForObject("/api/v1/students/{studentId}", 
                                        StudentDto.class, "testUser");
    }
    */


    @Test
    void getAllRatings() throws Exception {
        Rating rating1 = createTestRating("course1");
        ratingRepository.saveAll(List.of(rating1));

        mockMvc.perform(get("/api/v1/course/course1/ratings/")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()", is(1)))
                .andExpect(jsonPath("$[*].courseId", containsInAnyOrder("course1")));
    }

    @Test
    void returnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/course/course1/ratings/{ratingId}", "nonexistent-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private Rating createTestRating(String course) {
        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);
        rating.setUserId("testUser");
        rating.setCourseId(course);
        return rating;
    }

    @Test
    void handleConcurrentUpdates() throws Exception {
        Rating rating = ratingRepository.save(createTestRating("course1"));

        Rating updatedRating1 = new Rating();
        updatedRating1.setDescription("Updated 1");
        updatedRating1.setRating(5);
        updatedRating1.setUserId("user1");
        updatedRating1.setCourseId("course1");

        Rating updatedRating2 = new Rating();
        updatedRating2.setDescription("Updated 2");
        updatedRating2.setRating(3);
        updatedRating2.setUserId("user2");
        updatedRating2.setCourseId("course1");

        mockMvc.perform(put("/api/v1/course/course1/ratings/{ratingId}", rating.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedRating1)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/course/course1/ratings/{ratingId}", rating.getId()) 
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedRating2)))
                .andExpect(status().isOk());
    }
    
    /*TODO: Excepción por falta de datos*/
    /*
    @Test
    void rejectInvalidRating() throws Exception {
        Rating invalidRating = new Rating();

        mockMvc.perform(post("/api/v1/course/course1/ratings/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRating)))
                .andExpect(status().isOk());  
    }
    */
    
}
