package com.mongodb.starter.rating;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
//import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository extends MongoRepository<Rating,String> {
    
    @Query("{ 'courseId': ?0 }")
    List<Rating> findAllRatingsByCourse(@Param("courseId") String courseId);
    
}
