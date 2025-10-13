package com.example.demo.domain.valueobject.order;

public class RatingScore {
    private final int value;

    public RatingScore(int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5 stars.");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}