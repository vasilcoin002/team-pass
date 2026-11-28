package com.example.teampass.dto;

import lombok.NonNull;

public record CreateUserRequest (

    @NonNull String email,
    @NonNull String password,

    @NonNull String name,
    @NonNull String surname,
    @NonNull String personId,
    String description,

    @NonNull Long departmentId

) {}
