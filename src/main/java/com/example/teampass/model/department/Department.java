package com.example.teampass.model.department;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "department")
    private Set<User> users;

    @Column(nullable = false)
    private BigDecimal defaultMonthlyLimit;
    @OneToMany(mappedBy = "department")
    private List<DepartmentMonthlyBudget> budgets;

    @OneToMany(mappedBy = "department")
    private List<LicenseRequest> licenseRequests;


    @Builder
    public Department(String name, BigDecimal defaultMonthlyLimit) {
        this.name = name;
        this.defaultMonthlyLimit = defaultMonthlyLimit;

        this.users = new HashSet<>();
        this.budgets = new ArrayList<>();
        this.licenseRequests = new ArrayList<>();
    }
}
