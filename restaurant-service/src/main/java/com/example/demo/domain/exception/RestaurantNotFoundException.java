package com.example.demo.domain.exception;

public class RestaurantNotFoundException extends RuntimeException {
    public RestaurantNotFoundException(String message) {
        super(message);
    }
}