package com.example.teampass.model.department;

import com.example.teampass.model.BaseEntity;
import com.example.teampass.model.product.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    @Column(name = "budget_limit", nullable = false)
    private BigDecimal limit;
    @Column(nullable = false)
    private BigDecimal spent;

    @Column(nullable = false)
    private int budgetYear;
    @Column(nullable = false)
    private int budgetMonth;

}
