package com.mongodb.starter.student;

public class RatingUpdateDto {
    private Double mean;

    public RatingUpdateDto(Double mean) {
        this.mean = mean;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }
}