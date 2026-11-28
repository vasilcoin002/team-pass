package com.example.teampass.dto;

import lombok.NonNull;

import java.math.BigDecimal;

public record CreateDepartmentRequest(
        @NonNull String name,
        @NonNull BigDecimal defaultMonthlyLimit
) {}
