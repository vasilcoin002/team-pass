package com.example.teampass.model.license;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
public class License extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    final static Currency currency = Currency.CZK;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lockedPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;
    @Column
    private LocalDateTime releasedAt;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseStatus status;
}
