package com.example.teampass.service;

import com.example.teampass.exception.*;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.department.DepartmentMonthlyBudget;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.DepartmentMonthlyBudgetRepository;
import com.example.teampass.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final ExchangeRateService exchangeRateService;

    private final LicenseRepository licenseRepository;
    private final DepartmentMonthlyBudgetRepository budgetRepository;


    @Transactional
    public License issueLicense(User user, Product product) {
        if (!user.isActive()) {
            throw new InactiveUserException("User is not active anymore");
        }

        if (!product.isActive()) {
            throw new InactiveProductException("Product is not active anymore");
        }

        long currentSeatsCount = licenseRepository.countByProductIdAndStatus(
                product.getId(), LicenseStatus.ACTIVE
        );
        if (product.getMaxSeatsCount() != null && currentSeatsCount >= product.getMaxSeatsCount()) {
            throw new NoAvailableSeatsException("There are no available seats left for the product " + product.getName());
        }

        boolean alreadyHasActiveLicense
                = licenseRepository.existsByUserIdAndProductIdAndStatus(
                        user.getId(), product.getId(), LicenseStatus.ACTIVE
                );
        if (alreadyHasActiveLicense) {
            throw new DuplicateLicenseRequestException("User already has license for the product " + product.getName());
        }

        DepartmentMonthlyBudget budget = getCurrentDepartmentMonthlyBudgetOrElseGet(user.getDepartment());
        BigDecimal czkProductCost = exchangeRateService.convertToCzk(product.getPrice());

        if (czkProductCost.compareTo(budget.getLimit().subtract(budget.getSpent())) > 0) {
            throw new InsufficientBudgetException(
                "Department budget is insufficient to issue licence for product " + product.getName()
                    + " to user " + user.getEmail().value()
            );
        }

        budget.setSpent(budget.getSpent().add(czkProductCost));
        budgetRepository.save(budget);

        return licenseRepository.save(
            License.builder()
                .user(user)
                .product(product)
                .lockedPrice(czkProductCost)
                .status(LicenseStatus.ACTIVE)
            .build()
        );
    }

    @Transactional
    public License issueLicense(LicenseRequest licenseRequest) {
        return issueLicense(licenseRequest.getUser(), licenseRequest.getProduct());
    }

    private DepartmentMonthlyBudget getCurrentDepartmentMonthlyBudgetOrElseGet(Department department) {
        LocalDateTime now = LocalDateTime.now();
        Optional<DepartmentMonthlyBudget> optionalBudget
            = budgetRepository.findByDepartmentIdAndBudgetYearAndBudgetMonth(
                    department.getId(), now.getYear(), now.getMonthValue()
            );

        return optionalBudget.orElseGet(
                () -> budgetRepository.save(
                        DepartmentMonthlyBudget.builder()
                                .department(department)
                                .limit(department.getDefaultMonthlyLimit())
                                .spent(BigDecimal.ZERO)
                        .build()
                )
        );
    }

}
