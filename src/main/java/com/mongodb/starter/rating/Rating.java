package com.mongodb.starter.rating;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Document
public class Rating {

    @NotBlank
    @Column(name="description", length = 500)
    String description;
    @NotNull
    Integer rating;
    String user;
    String course;
    LocalDateTime date = LocalDateTime.now();

    @Id
    private String id;
}
