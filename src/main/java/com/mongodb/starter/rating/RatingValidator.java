package com.mongodb.starter.rating;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class RatingValidator implements Validator{
    
    private static final String REQUIRED = "required";

    
	@Override
	public void validate(Object obj, Errors errors) {
		Rating rating = (Rating) obj;
		String description = rating.getDescription();
        Integer rating_number = rating.getRating();
        String userId = rating.getUserId();
        String username = rating.getUsername();
        String courseId = rating.getCourseId();

		// description validation
		if (!StringUtils.hasLength(description) || description.length()>500 || description.length()<1) {
			errors.rejectValue("description", REQUIRED+" and between 1 and 500 characters");
		}
        
        // userId validation
		if (!StringUtils.hasLength(userId)) {
			errors.rejectValue("userId", REQUIRED+"");
		}

        // username validation
		if (!StringUtils.hasLength(username)) {
			errors.rejectValue("username", REQUIRED+"");
		}

        // courseId validation
		if (!StringUtils.hasLength(courseId)) {
			errors.rejectValue("courseId", REQUIRED+"");
		}

		// rating validation
		if (rating_number>5 || rating_number<0) {
			errors.rejectValue("rating", REQUIRED+" and between 1 and 5");
		}
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Rating.class.isAssignableFrom(clazz);
	}

}
