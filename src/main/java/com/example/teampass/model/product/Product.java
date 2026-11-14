package com.example.teampass.model.product;

import com.example.teampass.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {
    @Column(nullable = false)
    private String name;
    @Column
    private String description;

    @Column // not NOT NULLABLE because seats count can be unlimited
    private Integer maxSeatsCount;

    @Embedded private Price price;

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    @Column(nullable = false)
    private boolean active;
}
