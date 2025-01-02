package com.mongodb.starter.rating;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.starter.student.StudentDto;
import com.mongodb.starter.student.UserService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(properties = {
    "resilience4j.circuitbreaker.instances.createRating.registerHealthIndicator=true",
    "resilience4j.circuitbreaker.instances.createRating.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.createRating.minimumNumberOfCalls=5",
    "resilience4j.circuitbreaker.instances.createRating.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.createRating.waitDurationInOpenState=5s",
    "resilience4j.circuitbreaker.instances.createRating.permittedNumberOfCallsInHalfOpenState=3",
    "resilience4j.circuitbreaker.instances.createRating.automaticTransitionFromOpenToHalfOpenEnabled=true",
    "feature.rating.enabled=true",
    "feature.rating.requests-per-hour=100",
    "feature.rating.burst-size=50"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RatingControllerIntegrationTest {

    private static final String VALID_TOKEN = "Bearer validToken";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RatingRepository ratingRepository;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RatingConfig ratingConfig;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        circuitBreakerRegistry.circuitBreaker("createRating").reset();
        
        // Configure UserService mock
        when(userService.extractUserId(VALID_TOKEN)).thenReturn("testUser");

        // Asegurar que el feature toggle está activado y con límites altos para testing
        ReflectionTestUtils.setField(ratingConfig, "enabled", true);
        ReflectionTestUtils.setField(ratingConfig, "requestsPerHour", 100);
        ReflectionTestUtils.setField(ratingConfig, "burstSize", 50);
    }

    @Test
    void createRatingWithCircuitBreaker() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("createRating");
        circuitBreaker.reset();

        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);
        rating.setUserId("testUser");

        StudentDto mockStudent = new StudentDto();
        mockStudent.setUsername("username1");

        // Configurar el mock para una llamada exitosa inicial
        when(restTemplate.getForObject(
                eq("/api/v1/students/me"),
                eq(StudentDto.class)
        )).thenReturn(mockStudent);

        // Primera llamada exitosa
        mockMvc.perform(post("/api/v1/course/course1/ratings/")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rating)))
                .andExpect(status().isCreated())
                .andReturn();

        // Configurar el mock para simular fallos en llamadas posteriores
        when(restTemplate.getForObject(
                eq("/api/v1/students/me"),
                eq(StudentDto.class)
        )).thenThrow(new ResourceAccessException("Service Unavailable"));

        // Realizar llamadas fallidas para abrir el CircuitBreaker
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                            .header("Authorization", VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isServiceUnavailable());
        }

        // Verificar que el CircuitBreaker está en estado abierto
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void testCircuitBreakerRecovery() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("createRating");
        circuitBreaker.reset();

        Rating rating = new Rating();
        rating.setDescription("description");
        rating.setRating(4);
        rating.setUserId("testUser");

        StudentDto mockStudent = new StudentDto();
        mockStudent.setUsername("username1");

        // Configurar fallos iniciales
        when(restTemplate.getForObject(
            eq("/api/v1/students/me"),
            eq(StudentDto.class)
        )).thenThrow(new ResourceAccessException("Service Unavailable"));

        // Provocar apertura del circuit breaker
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                    .header("Authorization", VALID_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isServiceUnavailable());
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Configurar éxito para la recuperación
        when(restTemplate.getForObject(
            eq("/api/v1/students/me"),
            eq(StudentDto.class)
        )).thenReturn(mockStudent);

        // Esperar a que el circuit breaker cambie a HALF_OPEN
        await()
            .atMost(7, TimeUnit.SECONDS)
            .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);

        // Realizar llamadas exitosas en estado HALF_OPEN
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/course/course1/ratings/")
                    .header("Authorization", VALID_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rating)))
                    .andExpect(status().isCreated());
        }

        // Verificar que el circuit breaker se cierra después de las llamadas exitosas
        await()
            .atMost(5, TimeUnit.SECONDS)
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
        rating.setUsername("user");
        return rating;
    }

    @Test
    void handleConcurrentUpdates() throws Exception {
        Rating rating = createTestRating("course1");
        rating = ratingRepository.save(rating);

        when(userService.extractUserId(VALID_TOKEN)).thenReturn("testUser");

        Rating updatedRating1 = new Rating();
        updatedRating1.setDescription("Updated 1");
        updatedRating1.setRating(5);
        updatedRating1.setUserId("testUser"); 
        updatedRating1.setCourseId("course1");
        updatedRating1.setUsername("user1");

        Rating updatedRating2 = new Rating();
        updatedRating2.setDescription("Updated 2");
        updatedRating2.setRating(3);
        updatedRating2.setUserId("user2");
        updatedRating2.setCourseId("course1");
        updatedRating2.setUsername("user1");

        mockMvc.perform(put("/api/v1/course/course1/ratings/{ratingId}", rating.getId())
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedRating1)))
                .andExpect(status().isOk());

        Rating updatedRating = ratingRepository.findById(rating.getId()).orElseThrow();
        assertThat(updatedRating.getDescription()).isEqualTo("Updated 1");
        assertThat(updatedRating.getRating()).isEqualTo(5);
    }

    
}