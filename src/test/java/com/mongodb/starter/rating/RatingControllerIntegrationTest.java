package com.mongodb.starter.rating;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
/*import static org.mockito.Mockito.mock;*/
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.web.client.ResourceAccessException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.awaitility.Awaitility.await;


@SpringBootTest(properties = {
    "resilience4j.circuitbreaker.instances.createRating.registerHealthIndicator=true",
    "resilience4j.circuitbreaker.instances.createRating.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.createRating.minimumNumberOfCalls=5",
    "resilience4j.circuitbreaker.instances.createRating.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.createRating.waitDurationInOpenState=5s",
    "resilience4j.circuitbreaker.instances.createRating.permittedNumberOfCallsInHalfOpenState=3"
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

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
    }

    @Test
    void createRatingWithCircuitBreaker() throws Exception {
        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);

        StudentDto mockStudent = new StudentDto();
        mockStudent.setUserName("userName1");

        // Configuramos el comportamiento exitoso inicial
        when(restTemplate.getForObject("/api/v1/students/{studentId}", 
                                    StudentDto.class, "testUser"))
                .thenReturn(mockStudent);

        // Primera llamada exitosa
        mockMvc.perform(post("/api/v1/course/course1/ratings/")
                .param("studentId", "testUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rating)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("userName1"));

        // Cambiamos el comportamiento para simular fallos
        when(restTemplate.getForObject("/api/v1/students/{studentId}", 
                                    StudentDto.class, "testUser"))
                .thenThrow(new RuntimeException("Service Unavailable"));

        // Realizamos llamadas para activar el circuit breaker
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                    .param("studentId", "testUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().string(containsString("Fallback Circuit Breaker Activo")));
            
            // Añadimos una pequeña pausa entre llamadas
            Thread.sleep(100);
        }
    }

    @Test
    void testCircuitBreakerRecovery() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("createRating");

        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);

        when(restTemplate.getForObject(anyString(), eq(StudentDto.class), anyString()))
            .thenThrow(new ResourceAccessException("Service Unavailable"));

        // Simula llamadas fallidas
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                    .param("studentId", "testUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isServiceUnavailable());
        }

        // Verifica que el Circuit Breaker esté abierto
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Configura una respuesta exitosa
        StudentDto mockStudent = new StudentDto();
        mockStudent.setUserName("userName1");
        when(restTemplate.getForObject(eq("/api/v1/students/{studentId}"), 
                                        eq(StudentDto.class), 
                                        eq("testUser")))
            .thenReturn(mockStudent);

        // Espera el estado HALF-OPEN y realiza llamadas exitosas
        await().atMost(7, TimeUnit.SECONDS)
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                    .param("studentId", "testUser")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isCreated());
        }

        // Verifica que el Circuit Breaker esté cerrado
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.CLOSED);
    }



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