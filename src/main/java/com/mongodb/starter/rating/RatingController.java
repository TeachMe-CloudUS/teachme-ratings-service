package com.mongodb.starter.rating;

import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.mongodb.starter.student.StudentDto;
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
    
    @Autowired
    public RatingController(RatingService ratingService, RestTemplate restTemplate, RatingConfig ratingConfig) {
        this.ratingService = ratingService;
        this.restTemplate = restTemplate;
        this.ratingConfig = ratingConfig;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "createRating", fallbackMethod = "controllerFallback")
    public ResponseEntity<Rating> create(@PathVariable("courseId") String courseId, 
                                       @RequestParam("studentId") String studentId, 
                                       @RequestBody @Valid Rating rating) {
        if (!ratingConfig.isEnabled()) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }

        Rating newRating = new Rating();
        BeanUtils.copyProperties(rating, newRating, "id");
        newRating.setCourseId(courseId);
        newRating.setUserId(studentId); // Añadido para evitar userId null

        try {
            StudentDto studentDto = restTemplate.getForObject("/api/v1/students/{studentId}", 
                                                            StudentDto.class, studentId);
            if (studentDto == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            
            newRating.setUsername(studentDto.getUsername());
            Rating savedRating = this.ratingService.saveRating(newRating);
            Double mean = this.ratingService.ratingMean(courseId);
            
            return new ResponseEntity<>(savedRating, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new ResourceAccessException("Service Unavailable");
        }
    }

    public ResponseEntity<String> controllerFallback(String courseId, String studentId, 
                                                   Rating rating, Throwable throwable) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                           .body("Fallback Circuit Breaker Activo: " + throwable.getMessage());
    }


    //DELETE	
	@DeleteMapping("{ratingId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<MessageResponse> delete(@PathVariable("courseId") String courseId, @PathVariable("ratingId") String ratingId) {
		//TODO: petición a microservicio user para obtener el usuario loggeado
		Rating rating = RestPreconditions.checkNotNull(ratingService.findRatingById(ratingId), "Rating", "ID", ratingId);
		ratingService.deleteRating(ratingId);
		Double mean = this.ratingService.ratingMean(courseId);
		//TODO: petición asíncrona a microservicio course para actualizar rating
		return new ResponseEntity<>(new MessageResponse("Rating deleted!"), HttpStatus.OK);
	}

    //UPDATE

	@PutMapping("{ratingId}")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Rating> update(@PathVariable("courseId") String courseId, @PathVariable("ratingId") String ratingId, @RequestBody @Valid Rating rating) {
		Rating aux = RestPreconditions.checkNotNull(ratingService.findRatingById(ratingId), "Rating", "ID", ratingId);
		//TODO: petición a microservicio user para obtener el usuario loggeado
		// Integer id = Integer.parseInt(userId);
		// User loggedUser = userService.findUser(id);
		// 	User paperUser = aux.getUser();
		// 	if (loggedUser.getId().equals(paperUser.getId())) {
		Rating res = ratingService.updateRating(rating, ratingId);
		Double mean = this.ratingService.ratingMean(courseId);
		//TODO: petición asíncrona a microservicio course para actualizar rating
		return new ResponseEntity<>(res, HttpStatus.OK);
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


}
