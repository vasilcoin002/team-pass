package com.example.teampass.dto;

import java.math.BigDecimal;

public record UpdateDepartmentRequest(

        String name,
        BigDecimal defaultMonthlyLimit

) {
    public boolean isEmpty() {
        return name == null && defaultMonthlyLimit == null;
    }
}
