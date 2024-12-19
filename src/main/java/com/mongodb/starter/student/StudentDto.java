package com.mongodb.starter.student;

import lombok.Data;

@Data
public class StudentDto {
    private String id; 
    private String userName; 

    public StudentDto() {}

    public StudentDto(String id, String userName) {
        this.id = id;
        this.userName = userName;
    }
}
