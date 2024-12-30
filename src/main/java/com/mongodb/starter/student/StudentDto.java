package com.mongodb.starter.student;

import lombok.Data;

@Data
public class StudentDto {
    private String id; 
    private String username; 

    public StudentDto() {}

    public StudentDto(String id, String username) {
        this.id = id;
        this.username = username;
    }
}
