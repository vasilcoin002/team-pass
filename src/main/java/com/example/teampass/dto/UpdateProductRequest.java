package com.example.teampass.dto;

import com.example.teampass.model.product.Category;
import com.example.teampass.model.product.Price;

import java.util.Set;

public record UpdateProductRequest (

    String name,
    String description,
    Integer maxSeatsCount,
    Price price,
    Set<Category> categories,
    Boolean isActive

) {
    public boolean isEmpty() {
        return name == null &&
                description == null &&
                maxSeatsCount == null &&
                price == null &&
                categories == null &&
                isActive == null;
    }
}
