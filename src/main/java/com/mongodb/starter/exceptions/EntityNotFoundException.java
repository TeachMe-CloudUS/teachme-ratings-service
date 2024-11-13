package com.mongodb.starter.exceptions;

import java.util.Arrays;

public class EntityNotFoundException {
        private final String[] args;

    public EntityNotFoundException(String... args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "EntityNotFoundException " + Arrays.toString(args);
    }
}
