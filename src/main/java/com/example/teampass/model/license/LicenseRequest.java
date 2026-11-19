package com.example.teampass.model.license;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
}
