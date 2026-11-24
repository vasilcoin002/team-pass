package com.example.teampass.service;

import com.example.teampass.exception.AccessDeniedException;
import com.example.teampass.exception.DuplicateLicenseRequestException;
import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.exception.InsufficientBudgetException;
import com.example.teampass.exception.NoAvailableSeatsException;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.department.DepartmentMonthlyBudget;
import com.example.teampass.model.license.License;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LicenseServiceTest {

    @Autowired
    private LicenseService licenseService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private LicenseRequestRepository licenseRequestRepository;
    @Autowired
    private DepartmentMonthlyBudgetRepository budgetRepository;

    private Department department;
    private User employee;
    private User manager;
    private Product product;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setName("Platform");
        department.setDefaultMonthlyLimit(new BigDecimal("10000.00"));
        department = departmentRepository.save(department);

        employee = User.builder()
                .email(new Email("emp.self@teampass.test"))
                .passwordHash("hashed-password")
                .name("Jan")
                .surname("Novak")
                .personId("EMP-SELF-001")
                .department(department)
                .build();
        employee = userRepository.save(employee);

        manager = User.builder()
                .email(new Email("mgr.self@teampass.test"))
                .passwordHash("hashed-password")
                .name("Petr")
                .surname("Svoboda")
                .personId("MGR-SELF-001")
                .department(department)
                .build();
        manager.setRole(Role.DEPARTMENT_MANAGER);
        manager = userRepository.save(manager);

        product = new Product();
        product.setName("GitHub Enterprise");
        product.setMaxSeatsCount(5);
        product.setPrice(new Price(Currency.EUR, new BigDecimal("25.00")));
        product.setActive(true);
        product = productRepository.save(product);
    }

    @Test
    void acquireLicenseForSelf_validManager_issuesActiveLicenseAndUpdatesBudget() {
        License license = licenseService.acquireLicenseForSelf(manager.getId(), product.getId());

        assertNotNull(license.getId());
        assertEquals(LicenseStatus.ACTIVE, license.getStatus());
        assertEquals(manager.getId(), license.getUser().getId());
        assertEquals(product.getId(), license.getProduct().getId());
        assertEquals(0, new BigDecimal("625.00").compareTo(license.getLockedPrice()));

        LocalDateTime now = LocalDateTime.now();
        DepartmentMonthlyBudget budget = budgetRepository
                .findByDepartmentIdAndBudgetYearAndBudgetMonth(department.getId(), now.getYear(), now.getMonthValue())
                .orElseThrow();
        assertEquals(0, new BigDecimal("625.00").compareTo(budget.getSpent()));
    }

    @Test
    void acquireLicenseForSelf_withExistingPendingRequest_cancelsPendingRequestAndIssuesLicense() {
        LicenseRequest pendingRequest = LicenseRequest.builder()
                .user(manager)
                .product(product)
                .message("Requested before promotion")
                .build();
        pendingRequest = licenseRequestRepository.save(pendingRequest);

        License license = licenseService.acquireLicenseForSelf(manager.getId(), product.getId());

        assertEquals(LicenseStatus.ACTIVE, license.getStatus());
        LicenseRequest updatedRequest = licenseRequestRepository.findById(pendingRequest.getId()).orElseThrow();
        assertEquals(LicenseRequestStatus.CANCELLED, updatedRequest.getStatus());
    }

    @Test
    void acquireLicenseForSelf_nonManager_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () ->
                licenseService.acquireLicenseForSelf(employee.getId(), product.getId()));
    }

    @Test
    void acquireLicenseForSelf_inactiveManager_throwsInactiveUserException() {
        manager.setActive(false);
        userRepository.save(manager);

        assertThrows(InactiveUserException.class, () ->
                licenseService.acquireLicenseForSelf(manager.getId(), product.getId()));
    }

    @Test
    void acquireLicenseForSelf_inactiveProduct_throwsInactiveProductException() {
        product.setActive(false);
        productRepository.save(product);

        assertThrows(InactiveProductException.class, () ->
                licenseService.acquireLicenseForSelf(manager.getId(), product.getId()));
    }

    @Test
    void acquireLicenseForSelf_alreadyHasActiveLicense_throwsDuplicateLicenseRequestException() {
        licenseService.acquireLicenseForSelf(manager.getId(), product.getId());

        assertThrows(DuplicateLicenseRequestException.class, () ->
                licenseService.acquireLicenseForSelf(manager.getId(), product.getId()));
    }

    @Test
    void acquireLicenseForSelf_noAvailableSeats_throwsNoAvailableSeatsException() {
        product.setMaxSeatsCount(1);
        productRepository.save(product);

        License existing = License.builder()
                .user(employee)
                .product(product)
                .lockedPrice(new BigDecimal("625.00"))
                .status(LicenseStatus.ACTIVE)
                .build();
        licenseRepository.save(existing);

        assertThrows(NoAvailableSeatsException.class, () ->
                licenseService.acquireLicenseForSelf(manager.getId(), product.getId()));
    }

    @Test
    void acquireLicenseForSelf_insufficientBudget_throwsInsufficientBudgetException() {
        department.setDefaultMonthlyLimit(new BigDecimal("100.00"));
        departmentRepository.save(department);

        assertThrows(InsufficientBudgetException.class, () ->
                licenseService.acquireLicenseForSelf(manager.getId(), product.getId()));
    }
}
