package com.example.teampass.repository;

import com.example.teampass.model.department.DepartmentMonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentMonthlyBudgetRepository extends JpaRepository<DepartmentMonthlyBudget, Long> {

    Optional<DepartmentMonthlyBudget> findByDepartmentIdAndBudgetYearAndBudgetMonth(Long departmentId, int budgetYear, int budgetMonth);

    List<DepartmentMonthlyBudget> findAllByDepartmentId(Long departmentId);

}
