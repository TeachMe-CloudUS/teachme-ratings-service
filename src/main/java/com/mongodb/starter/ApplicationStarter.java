package com.mongodb.starter;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Random;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.starter.rating.Rating;
import com.mongodb.starter.rating.RatingRepository;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableMongoRepositories
public class ApplicationStarter {

    private final RatingRepository ratingRepository;

    public ApplicationStarter(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStarter.class, args);
    }

    @PostConstruct
    public void init() { 

        Boolean testMode = false;
        if (testMode && ratingRepository.findAllRatingsByCourse("course1").size() < 3) {    
            
            Rating rating1 = new Rating();
            rating1.setDescription("Excellent course");
            rating1.setRating(5);
            rating1.setUserId("user1");
            rating1.setCourseId("course1");
            rating1.setUsername("User1");
            rating1.setDate(generateRandomDate());  
    
            Rating rating2 = new Rating();
            rating2.setDescription("Very good, but could use more examples");
            rating2.setRating(4);
            rating2.setUserId("user2");
            rating2.setCourseId("course1");
            rating2.setUsername("User2");
            rating2.setDate(generateRandomDate());  
    
            Rating rating3 = new Rating();
            rating3.setDescription("Good course, learned a lot");
            rating3.setRating(4);
            rating3.setUserId("user3");
            rating3.setCourseId("course1");
            rating3.setUsername("User3");
            rating3.setDate(generateRandomDate());  
    
            Rating rating4 = new Rating();
            rating4.setDescription("Not bad, but too basic for me");
            rating4.setRating(3);
            rating4.setUserId("user4");
            rating4.setCourseId("course1");
            rating4.setUsername("User4");
            rating4.setDate(generateRandomDate());  
    
            Rating rating5 = new Rating();
            rating5.setDescription("It's the worst course I've ever tried");
            rating5.setRating(1);
            rating5.setUserId("user5");
            rating5.setCourseId("course1");
            rating5.setUsername("User5");
            rating5.setDate(generateRandomDate()); 
    
            // Guarda los ratings en MongoDB
            ratingRepository.save(rating1);
            ratingRepository.save(rating2);
            ratingRepository.save(rating3);
            ratingRepository.save(rating4);
            ratingRepository.save(rating5);
    
            System.out.println("Se han generado 5 ratings para course1");
        } 
        if (testMode && ratingRepository.findAllRatingsByCourse("course2").isEmpty()) {
            
            Rating rating1 = new Rating();
            rating1.setDescription("Great course!");
            rating1.setRating(5);
            rating1.setUserId("user1");
            rating1.setCourseId("course2");
            rating1.setUsername("User1");
            rating1.setDate(generateRandomDate()); 
            System.out.println("Se han generado 1 rating para course2");
        }
    }
    
    LocalDateTime generateRandomDate() {
        Random random = new Random();
        int day = random.nextInt(31) + 1;  
        int hour = random.nextInt(24);     
        int minute = random.nextInt(60);   
        return LocalDateTime.of(2024, Month.DECEMBER, day, hour, minute);
    }

}
