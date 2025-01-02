package com.mongodb.starter.student;

import lombok.Data;

@Data
public class ContactInformation {
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private String country;
}
