package com.example.teampass.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Embeddable
public record Price(
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        Currency currency,
        @Column(nullable = false)
        BigDecimal amount
) {}