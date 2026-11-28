package com.example.teampass.dto;

public record UpdateUserRequest (

        String email,
        String password,

        String name,
        String surname,
        String personId,
        String description,

        Long departmentId

) {
    public boolean isEmpty() {
        return email == null &&
                password == null &&
                name == null &&
                surname == null &&
                personId == null &&
                description == null &&
                departmentId == null;
    }
}
