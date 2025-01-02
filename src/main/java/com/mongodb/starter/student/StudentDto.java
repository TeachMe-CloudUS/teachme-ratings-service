package com.mongodb.starter.student;

import java.util.List;

import lombok.Data;

@Data
public class StudentDto {
    private String id;
    private String userId;
    private ContactInformation contactInformation;
    private ProfileInformation profileInformation;
    private List<String> enrolledCourses;
    private List<String> completedCourses;
    private List<String> forumPosts;
}