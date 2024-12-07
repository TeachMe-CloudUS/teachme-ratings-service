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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mongodb.starter.util.MessageResponse;
import com.mongodb.starter.util.RestPreconditions;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/course/{courseId}/ratings/")
@Tag(name = "Ratings", description = "The ratings management API")
public class RatingController {
    
    private final RatingService ratingService;

    @Autowired
	public RatingController(RatingService ratingService) {
		this.ratingService = ratingService;
	}


    //CREATE	

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Rating> create(@PathVariable("courseId") String courseId,@RequestBody @Valid Rating rating){
		//TODO: petición a microservicio user para obtener el usuario loggeado
		Rating newRating = new Rating();
		Rating savedRating;
		BeanUtils.copyProperties(rating, newRating, "id");
		newRating.setCourseId(courseId);
		savedRating = this.ratingService.saveRating(newRating);
		Double mean = this.ratingService.ratingMean(courseId);
		//TODO: petición asíncrona a microservicio course para actualizar rating
		return new ResponseEntity<>(savedRating, HttpStatus.OK);
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
