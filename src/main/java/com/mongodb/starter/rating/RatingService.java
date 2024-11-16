package com.mongodb.starter.rating;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import org.springframework.transaction.annotation.Transactional;
import com.mongodb.starter.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RatingService {
    
    private RatingRepository ratingRepository;

    @Autowired
    public RatingService(RatingRepository ratingRepository) {
		this.ratingRepository = ratingRepository;
	}

    @Transactional(readOnly = true)
	public Collection<Rating> findAll() {
		return (List<Rating>) ratingRepository.findAll();
	}

    @Transactional(readOnly = true)
	public Rating findRatingById(String id) throws DataAccessException {
		return ratingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Paper", "ID", id));
	}

	@Transactional(readOnly = true)
	public List<Rating> findAllRatingsByCourse(String courseId) throws DataAccessException {
		return ratingRepository.findAllRatingsByCourse(courseId);
	}

    @Transactional
	public Rating saveRating(Rating rating){
        ratingRepository.save(rating);
		return rating;
	}

    @Transactional
	public Rating updateRating(Rating rating, String id) {
		Rating toUpdate = findRatingById(id);
		BeanUtils.copyProperties(rating, toUpdate, "id");
		return saveRating(toUpdate);
	}

	@Transactional
	public void deleteRating(String id) throws DataAccessException {
		Rating toDelete = findRatingById(id);
		ratingRepository.delete(toDelete);
	}

}
