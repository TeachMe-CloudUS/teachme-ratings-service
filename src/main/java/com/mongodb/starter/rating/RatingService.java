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
    private final RatingConfig ratingConfig;
    private final RatingThrottler ratingThrottler;

    @Autowired
    public RatingService(RatingRepository ratingRepository, RatingConfig ratingConfig, RatingThrottler ratingThrottler) {
        this.ratingRepository = ratingRepository;
        this.ratingConfig = ratingConfig;
        this.ratingThrottler = ratingThrottler;
    }

    @Transactional(readOnly = true)
    public Collection<Rating> findAll() {
        return (List<Rating>) ratingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Rating findRatingById(String id) throws DataAccessException {
        return ratingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rating", "ID", id));
    }

    @Transactional(readOnly = true)
    public List<Rating> findAllRatingsByCourse(String courseId) throws DataAccessException {
        return ratingRepository.findAllRatingsByCourse(courseId);
    }

    @Transactional
    public Rating saveRating(Rating rating) {
        if (!ratingConfig.isEnabled()) {
            throw new FeatureDisabledException("Rating feature is currently disabled");
        }

        if (!ratingThrottler.allowRequest(rating.getUserId())) {
            throw new ThrottlingException("Rate limit exceeded for user: " + rating.getUserId());
        }

        return ratingRepository.save(rating);
    }

    @Transactional
    public Rating updateRating(Rating rating, String id) {
        if (!ratingConfig.isEnabled()) {
            throw new FeatureDisabledException("Rating feature is currently disabled");
        }

        Rating toUpdate = findRatingById(id);

        if (!ratingThrottler.allowRequest(rating.getUserId())) {
            throw new ThrottlingException("Rate limit exceeded for user: " + rating.getUserId());
        }

        BeanUtils.copyProperties(rating, toUpdate, "id");
        return ratingRepository.save(toUpdate);
    }

    @Transactional
    public void deleteRating(String id) throws DataAccessException {
        if (!ratingConfig.isEnabled()) {
            throw new FeatureDisabledException("Rating feature is currently disabled");
        }
        
        Rating toDelete = findRatingById(id);
        
        if (!ratingThrottler.allowRequest(toDelete.getUserId())) {
            throw new ThrottlingException("Rate limit exceeded for user: " + toDelete.getUserId());
        }
        
        ratingRepository.delete(toDelete);
    }

	@Transactional(readOnly =true)
	public Double ratingMean(String courseId){
		List<Integer> ratings = findAllRatingsByCourse(courseId).stream().map(x -> x.getRating()).toList();
		Double mean = ratings.stream().mapToDouble(Integer:: doubleValue).average().orElse(0.0);
		return mean;
	}

	// Excepción para feature toggle
	public class FeatureDisabledException extends RuntimeException {
		public FeatureDisabledException(String message) {
			super(message);
		}
	}

	// Excepción para throttling
	public class ThrottlingException extends RuntimeException {
		public ThrottlingException(String message) {
			super(message);
		}
	}
}


