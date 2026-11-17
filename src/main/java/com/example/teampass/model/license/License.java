package com.example.teampass.model.license;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
public class License extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

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
