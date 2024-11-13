package com.mongodb.starter.rating;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Document
public class Rating {

    String description;
    Integer rating;
    String user;
    String course;
    LocalDateTime date;

    @Id
    private String id;;
}
