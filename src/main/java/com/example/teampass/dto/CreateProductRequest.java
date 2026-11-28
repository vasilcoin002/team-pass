package com.example.teampass.dto;

import com.example.teampass.model.product.Category;
import com.example.teampass.model.product.Price;
import lombok.NonNull;

import java.util.Set;

public record CreateProductRequest(

        @NonNull String name,
        String description,
        Integer maxSeatsCount,
        @NonNull Price price,
        Set<Category> categories

) {}
