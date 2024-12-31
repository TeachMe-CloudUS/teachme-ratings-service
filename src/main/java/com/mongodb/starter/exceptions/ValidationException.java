package com.mongodb.starter.exceptions;

public class ValidationException extends RuntimeException {

    private final String message;
    
    public ValidationException(String message) {
        super(message);
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
}
