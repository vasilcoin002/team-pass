package com.example.teampass.service;

import com.example.teampass.exception.InsufficientBudgetException;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Price;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.Email;
import com.example.teampass.model.user.Role;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.DepartmentMonthlyBudgetRepository;
import com.example.teampass.repository.DepartmentRepository;
import com.example.teampass.repository.LicenseRepository;
import com.example.teampass.repository.LicenseRequestRepository;
import com.example.teampass.repository.ProductRepository;
import com.example.teampass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LicenseRequestServiceRollbackTest {

    @Autowired
    private LicenseRequestService licenseRequestService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private LicenseRequestRepository licenseRequestRepository;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private DepartmentMonthlyBudgetRepository budgetRepository;

    @Test
    void approveRequest_insufficientBudget_throwsAndRollsBackRequestStatus() {
        Department lowBudgetDept = new Department();
        lowBudgetDept.setName("LowBudgetDept");
        lowBudgetDept.setDefaultMonthlyLimit(new BigDecimal("100.00"));
        lowBudgetDept = departmentRepository.save(lowBudgetDept);

        User emp = User.builder()
                .email(new Email("lowbudget.emp@teampass.test"))
                .passwordHash("hashed-password")
                .name("Tomas")
                .surname("Horak")
                .personId("EMP-RB-001")
                .department(lowBudgetDept)
                .build();
        emp = userRepository.save(emp);

        User mgr = User.builder()
                .email(new Email("lowbudget.mgr@teampass.test"))
                .passwordHash("hashed-password")
                .name("Martin")
                .surname("Prochazka")
                .personId("MGR-RB-001")
                .department(lowBudgetDept)
                .build();
        mgr.setRole(Role.DEPARTMENT_MANAGER);
        mgr = userRepository.save(mgr);

        Product expensiveProduct = new Product();
        expensiveProduct.setName("ExpensiveTool-" + System.nanoTime());
        expensiveProduct.setMaxSeatsCount(5);
        expensiveProduct.setPrice(new Price(Currency.EUR, new BigDecimal("25.00")));
        expensiveProduct.setActive(true);
        expensiveProduct = productRepository.save(expensiveProduct);

        LicenseRequest request = licenseRequestService.requestLicense(
                emp.getId(), expensiveProduct.getId(), "Too expensive");

        Long requestId = request.getId();
        Long managerId = mgr.getId();
        Long productId = expensiveProduct.getId();
        Long deptId = lowBudgetDept.getId();

        try {
            assertThrows(InsufficientBudgetException.class, () ->
                    licenseRequestService.approveRequest(requestId, managerId));

            LicenseRequest persisted = licenseRequestRepository.findById(requestId).orElseThrow();
            assertEquals(LicenseRequestStatus.PENDING, persisted.getStatus());
            assertNull(persisted.getReviewedBy());
            assertEquals(0, licenseRepository.countByProductIdAndStatus(productId, LicenseStatus.ACTIVE));
        } finally {
            licenseRequestRepository.deleteById(requestId);
            budgetRepository.deleteAll(budgetRepository.findAllByDepartmentId(deptId));
            productRepository.deleteById(productId);
            userRepository.deleteById(emp.getId());
            userRepository.deleteById(mgr.getId());
            departmentRepository.deleteById(deptId);
        }
    }
}
