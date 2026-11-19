package com.example.teampass.model.license;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "license_requests")
@Getter
@Setter
@NoArgsConstructor
public class LicenseRequest extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column
    private String message;
    @Column(nullable = false, updatable = false)
    private LocalDateTime at;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseRequestStatus status;
    @ManyToOne
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;


    @Builder
    public LicenseRequest(User user, Product product, String message) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(product, "product must not be null");

        this.user = user;
        this.product = product;
        this.message = message;

        this.department = user.getDepartment();
        this.at = LocalDateTime.now();
        this.status = LicenseRequestStatus.PENDING;
        this.reviewedBy = null;
    }
}
