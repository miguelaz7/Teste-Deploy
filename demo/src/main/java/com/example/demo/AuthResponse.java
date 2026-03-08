package com.example.demo;

public class AuthResponse {

    private final String message;
    private final String firstName;
    private final String lastName;

    public AuthResponse(String message) {
        this.message = message;
        this.firstName = null;
        this.lastName = null;
    }

    public AuthResponse(String message, String firstName, String lastName) {
        this.message = message;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getMessage() {
        return message;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
