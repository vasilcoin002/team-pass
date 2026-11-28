package com.example.teampass.dto;

import java.math.BigDecimal;

public record UpdateDepartmentRequest(
        String name,
        BigDecimal defaultMonthlyLimit
) {}
