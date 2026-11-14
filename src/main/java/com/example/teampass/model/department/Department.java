package com.example.teampass.model.department;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.user.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "department")
    private List<Employee> employees;

    @Column(nullable = false)
    private BigDecimal defaultMonthlyLimit;
    @OneToMany(mappedBy = "department")
    private List<DepartmentMonthlyBudget> budgets;

    @OneToMany(mappedBy = "department")
    private List<LicenseRequest> licenseRequests;
}
