package com.example.teampass.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Email(
        @Column(unique = true, nullable = false, updatable = false)
        String value
) {}
