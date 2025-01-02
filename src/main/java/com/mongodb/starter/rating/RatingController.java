package com.mongodb.starter.rating;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mongodb.starter.exceptions.ValidationException;
import com.mongodb.starter.student.RatingUpdateDto;
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
    
    private final RatingService ratingService;
    private final RestTemplate restTemplate;
    private final RatingConfig ratingConfig;
    private final UserService userService;
    private final RatingValidator ratingValidator;

    @Value("${courses.url}")
    private String coursesURL;

    @Value("${rating.url}")
    private String ratingUrl;

    @Autowired
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
            StudentDto studentDto = restTemplate.getForObject("/api/v1/students/me", 
                                                            StudentDto.class);
            if (studentDto == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            newRating.setUsername(studentDto.getUsername());
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

    public ResponseEntity<String> controllerFallback(String courseId, String token, 
                                                   Rating rating, Throwable throwable) {
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
            RatingUpdateDto ratingUpdate = new RatingUpdateDto(mean);
            restTemplate.put(coursesURL + courseId + ratingUrl, ratingUpdate);
        } catch (RestClientException e) {
            System.err.println("Failed to update course rating: " + e.getMessage());
        }
    }

}
