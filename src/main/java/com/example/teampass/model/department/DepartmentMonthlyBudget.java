package com.example.teampass.model.department;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.product.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "department_monthly_budgets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_budget_dept_year_month",
                columnNames = {"department_id", "budget_year", "budget_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class DepartmentMonthlyBudget extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    final static Currency currency = Currency.CZK;
    @Column(name = "budget_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal limit;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal spent;

    @Column(nullable = false)
    private int budgetYear;
    @Column(nullable = false)
    private int budgetMonth;


    @Builder
    public DepartmentMonthlyBudget(Department department, BigDecimal limit, BigDecimal spent) {
        this.department = department;
        this.limit = limit;
        this.spent = spent;

        LocalDateTime now = LocalDateTime.now();
        this.budgetYear = now.getYear();
        this.budgetMonth = now.getMonthValue();
    }
}
