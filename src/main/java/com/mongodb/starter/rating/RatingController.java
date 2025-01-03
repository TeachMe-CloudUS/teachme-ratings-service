package com.mongodb.starter.rating;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.mongodb.starter.exceptions.ValidationException;
import com.mongodb.starter.student.StudentDto;
import com.mongodb.starter.student.UserService;
import com.mongodb.starter.util.MessageResponse;
import com.mongodb.starter.util.RestPreconditions;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/course/{courseId}/ratings/")
@Tag(name = "Ratings", description = "The ratings management API")
public class RatingController {
    
    @Autowired
    private final RatingService ratingService;
    
    @Autowired
    private final RestTemplate restTemplate;
    
    @Autowired
    private final RatingConfig ratingConfig;
    
    @Autowired
    private final UserService userService;
    
    @Autowired
    private final RatingValidator ratingValidator;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${courses.url}")
    private String coursesURL;

    @Value("${rating.url}")
    private String ratingUrl;

    @Value("${student.url}")
    private String studentServiceUrl;

    public RatingController(RatingService ratingService, RestTemplate restTemplate, RatingConfig ratingConfig, RatingValidator ratingValidator,  UserService userService) {
        this.ratingService = ratingService;
        this.restTemplate = restTemplate;
        this.ratingConfig = ratingConfig;
        this.ratingValidator = ratingValidator;
        this.userService = userService;
    }

  //CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "createRating", fallbackMethod = "controllerFallback")
    public ResponseEntity<Rating> create(@PathVariable("courseId") String courseId,  
                                         @RequestHeader("Authorization") String token,
                                         @RequestBody @Valid Rating rating) {
        if (!ratingConfig.isEnabled()) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }

        token = token.trim();
        String userId = userService.extractUserId(token);

        Rating newRating = new Rating();
        BeanUtils.copyProperties(rating, newRating, "id");
        newRating.setCourseId(courseId);

        try {
            HttpHeaders headers = new HttpHeaders();
            String tokenToUse = token;
            if (!token.startsWith("Bearer ")) {
                tokenToUse = "Bearer " + token;
            }
            headers.set("Authorization", tokenToUse);

            // Crea una entidad HTTP con los encabezados
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Llama al servicio con RestTemplate
            ResponseEntity<StudentDto> response = restTemplate.exchange(
            studentServiceUrl,  // URL del servicio
            HttpMethod.GET,     // Método HTTP
            entity,             // Entidad con encabezados
            StudentDto.class    // Tipo de respuesta esperada
        );

        // Obtener el objeto StudentDto de la respuesta
        StudentDto student = response.getBody();

        // Acceder al campo "name"
        if (student != null && student.getContactInformation() != null) {
            String name = student.getContactInformation().getName();
            newRating.setUsername(name);
            System.out.println("Name: " + name);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
                       
            newRating.setUserId(userId);
            Errors errors = ratingValidator.validateObject(newRating);
            if(errors.hasErrors()) {
                StringBuilder errorMessage = new StringBuilder();
                for (org.springframework.validation.FieldError error : errors.getFieldErrors()) {
                    errorMessage.append(error.getField())
                                .append(": ")
                                .append(error.getDefaultMessage())
                                .append("\n");
                }
                throw new ValidationException(errorMessage.toString());
            }
            Rating savedRating = this.ratingService.saveRating(newRating);
            Double mean = this.ratingService.ratingMean(courseId);
            updateCourseRating(courseId, mean);
            
            return new ResponseEntity<>(savedRating, HttpStatus.CREATED);
        } catch (ResourceAccessException  e) {
            throw new ResourceAccessException("Service Unavailable");
        } 
    }

    public ResponseEntity<String> controllerFallback(Throwable throwable) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fallback Circuit Breaker Activo: " + throwable.getMessage());
    }

    //UPDATE
    @PutMapping("{ratingId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Rating> update(@PathVariable("courseId") String courseId, @PathVariable("ratingId") String ratingId, @RequestHeader("Authorization") String token, @RequestBody @Valid Rating rating) {
        
        token = token.trim();
        String userId = userService.extractUserId(token);
        Rating aux = RestPreconditions.checkNotNull(ratingService.findRatingById(ratingId), "Rating", "ID", ratingId);

        if(rating.getUserId().equals(userId)){
            Errors errors = ratingValidator.validateObject(aux);
            if(errors.hasErrors()) {
                StringBuilder errorMessage = new StringBuilder();
                for (org.springframework.validation.FieldError error : errors.getFieldErrors()) {
                    errorMessage.append(error.getField())
                                .append(": ")
                                .append(error.getDefaultMessage())
                                .append("\n");
                }
                throw new ValidationException(errorMessage.toString());
            }
            Rating res = ratingService.updateRating(rating, ratingId);
            Double mean = this.ratingService.ratingMean(courseId);
            updateCourseRating(courseId, mean);

            return new ResponseEntity<>(res, HttpStatus.OK);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
    
    //DELETE	
	@DeleteMapping("{ratingId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("courseId") String courseId, @RequestHeader("Authorization") String token, @PathVariable("ratingId") String ratingId) {
		
        token = token.trim();
        String userId = userService.extractUserId(token);
		Rating rating = RestPreconditions.checkNotNull(ratingService.findRatingById(ratingId), "Rating", "ID", ratingId);
		
        if(rating.getUserId().equals(userId)){
            ratingService.deleteRating(ratingId);
            Double mean = this.ratingService.ratingMean(courseId);
            updateCourseRating(courseId, mean);

            return new ResponseEntity<>(new MessageResponse("Rating deleted!"), HttpStatus.OK);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
	}


    //GET BY ID
	@GetMapping("{ratingId}")
	public ResponseEntity<Rating> findById(@PathVariable("ratingId") String ratingId) {
		Rating rating = RestPreconditions.checkNotNull(ratingService.findRatingById(ratingId), "Paper", "ID", ratingId);
			return new ResponseEntity<>(rating, HttpStatus.OK);
	} 

    @GetMapping
	public ResponseEntity<List<Rating>> findAllByCourse(@PathVariable("courseId") String courseId) {
	    return new ResponseEntity<>((List<Rating>) this.ratingService.findAllRatingsByCourse(courseId), HttpStatus.OK);

	}

    private void updateCourseRating(String courseId, Double mean) {
        try {
            // Realizar la solicitud PATCH
            webClientBuilder.build().patch()
                    .uri(coursesURL+courseId+ratingUrl)
                    .bodyValue(Map.of("rating", mean))
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Bloquea la operación para simplificar la ejecución

            
        } catch (WebClientResponseException e) {
            // Manejar errores específicos de HTTP
            System.err.println("Error HTTP: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // Manejar otros errores
            System.err.println("Error inesperado: " + e.getMessage());
        }
    }

}
