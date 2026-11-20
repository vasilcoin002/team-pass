package com.example.teampass.service;

import com.example.teampass.exception.AccessDeniedException;
import com.example.teampass.exception.DuplicateLicenseRequestException;
import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.exception.InsufficientBudgetException;
import com.example.teampass.exception.InvalidLicenseRequestStateException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LicenseRequestServiceTest {

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

    private Department department;
    private User user;
    private User manager;
    private Product product;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setName("Engineering");
        department.setDefaultMonthlyLimit(new BigDecimal("10000.00"));
        department = departmentRepository.save(department);

        user = User.builder()
                .email(new Email("employee@teampass.test"))
                .passwordHash("hashed-password")
                .name("Jan")
                .surname("Novak")
                .personId("EMP-001")
                .department(department)
                .build();
        user = userRepository.save(user);

        manager = User.builder()
                .email(new Email("manager@teampass.test"))
                .passwordHash("hashed-password")
                .name("Petr")
                .surname("Svoboda")
                .personId("MGR-001")
                .department(department)
                .build();
        manager.setRole(Role.DEPARTMENT_MANAGER);
        manager = userRepository.save(manager);

        product = new Product();
        product.setName("JetBrains All Products");
        product.setMaxSeatsCount(5);
        product.setPrice(new Price(Currency.EUR, new BigDecimal("25.00")));
        product.setActive(true);
        product = productRepository.save(product);
    }

    @Test
    void requestLicense_validInput_createsPendingRequest() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need IDE for backend work");

        assertNotNull(request.getId());
        assertEquals(LicenseRequestStatus.PENDING, request.getStatus());
        assertEquals(user.getId(), request.getUser().getId());
        assertEquals(product.getId(), request.getProduct().getId());
        assertEquals(department.getId(), request.getDepartment().getId());
        assertEquals("Need IDE for backend work", request.getMessage());
        assertNotNull(request.getAt());
        assertNull(request.getReviewedBy());
    }

    @Test
    void requestLicense_userNotFound_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                licenseRequestService.requestLicense(999999L, product.getId(), "Test"));
    }

    @Test
    void requestLicense_inactiveUser_throwsInactiveUserException() {
        user.setActive(false);
        userRepository.save(user);

        assertThrows(InactiveUserException.class, () ->
                licenseRequestService.requestLicense(user.getId(), product.getId(), "Test"));
    }

    @Test
    void requestLicense_productNotFound_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                licenseRequestService.requestLicense(user.getId(), 999999L, "Test"));
    }

    @Test
    void requestLicense_inactiveProduct_throwsInactiveProductException() {
        product.setActive(false);
        productRepository.save(product);

        assertThrows(InactiveProductException.class, () ->
                licenseRequestService.requestLicense(user.getId(), product.getId(), "Test"));
    }

    @Test
    void requestLicense_pendingRequestAlreadyExists_throwsDuplicateLicenseRequestException() {
        licenseRequestService.requestLicense(user.getId(), product.getId(), "First request");

        assertThrows(DuplicateLicenseRequestException.class, () ->
                licenseRequestService.requestLicense(user.getId(), product.getId(), "Duplicate request"));
    }

    @Test
    void requestLicense_activeLicenseAlreadyExists_throwsDuplicateLicenseRequestException() {
        License activeLicense = new License();
        activeLicense.setUser(user);
        activeLicense.setProduct(product);
        activeLicense.setLockedPrice(new BigDecimal("625.00"));
        activeLicense.setAssignedAt(LocalDateTime.now());
        activeLicense.setStatus(LicenseStatus.ACTIVE);
        licenseRepository.save(activeLicense);

        assertThrows(DuplicateLicenseRequestException.class, () ->
                licenseRequestService.requestLicense(user.getId(), product.getId(), "Need another"));
    }

    @Test
    void requestLicense_previousDeniedRequestAndReleasedLicense_allowsNewRequest() {
        LicenseRequest deniedRequest = LicenseRequest.builder()
                .user(user)
                .product(product)
                .message("Old request")
                .build();
        deniedRequest.setStatus(LicenseRequestStatus.DENIED);
        licenseRequestRepository.save(deniedRequest);

        License releasedLicense = new License();
        releasedLicense.setUser(user);
        releasedLicense.setProduct(product);
        releasedLicense.setLockedPrice(new BigDecimal("625.00"));
        releasedLicense.setAssignedAt(LocalDateTime.now().minusMonths(2));
        releasedLicense.setReleasedAt(LocalDateTime.now().minusMonths(1));
        releasedLicense.setStatus(LicenseStatus.RELEASED);
        licenseRepository.save(releasedLicense);

        LicenseRequest newRequest = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Re-requesting now");

        assertNotNull(newRequest.getId());
        assertEquals(LicenseRequestStatus.PENDING, newRequest.getStatus());
    }

    // --- Slice 2: rejectRequest ---

    @Test
    void rejectRequest_validManagerSameDepartment_setsDeniedAndReviewer() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");

        LicenseRequest rejected = licenseRequestService.rejectRequest(request.getId(), manager.getId());

        assertEquals(LicenseRequestStatus.DENIED, rejected.getStatus());
        assertEquals(manager.getId(), rejected.getReviewedBy().getId());
        assertEquals(0, licenseRepository.countByProductIdAndStatus(product.getId(), LicenseStatus.ACTIVE));
    }

    @Test
    void rejectRequest_nonManager_throwsAccessDeniedException() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");

        assertThrows(AccessDeniedException.class, () ->
                licenseRequestService.rejectRequest(request.getId(), user.getId()));
    }

    @Test
    void rejectRequest_inactiveManager_throwsInactiveUserException() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");
        manager.setActive(false);
        userRepository.save(manager);

        assertThrows(InactiveUserException.class, () ->
                licenseRequestService.rejectRequest(request.getId(), manager.getId()));
    }

    @Test
    void rejectRequest_differentDepartmentManager_throwsAccessDeniedException() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");

        Department otherDept = new Department();
        otherDept.setName("Marketing");
        otherDept.setDefaultMonthlyLimit(new BigDecimal("5000.00"));
        otherDept = departmentRepository.save(otherDept);

        User otherManager = User.builder()
                .email(new Email("other.manager@teampass.test"))
                .passwordHash("hashed-password")
                .name("Eva")
                .surname("Kralova")
                .personId("MGR-002")
                .department(otherDept)
                .build();
        otherManager.setRole(Role.DEPARTMENT_MANAGER);
        otherManager = userRepository.save(otherManager);

        Long otherManagerId = otherManager.getId();
        assertThrows(AccessDeniedException.class, () ->
                licenseRequestService.rejectRequest(request.getId(), otherManagerId));
    }

    @Test
    void rejectRequest_nonPendingRequest_throwsInvalidLicenseRequestStateException() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");
        licenseRequestService.rejectRequest(request.getId(), manager.getId());

        assertThrows(InvalidLicenseRequestStateException.class, () ->
                licenseRequestService.rejectRequest(request.getId(), manager.getId()));
    }

    // --- Slice 3: approveRequest ---

    @Test
    void approveRequest_validManagerSufficientSeatsAndBudget_acceptsIssuesLicenseAndUpdatesBudget() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need IDE");

        LicenseRequest approved = licenseRequestService.approveRequest(request.getId(), manager.getId());

        assertEquals(LicenseRequestStatus.ACCEPTED, approved.getStatus());
        assertEquals(manager.getId(), approved.getReviewedBy().getId());

        List<License> activeLicenses = licenseRepository.findAllUserActiveLicenses(user.getEmail().value());
        assertEquals(1, activeLicenses.size());
        License license = activeLicenses.getFirst();
        assertEquals(0, new BigDecimal("625.00").compareTo(license.getLockedPrice()));
        assertEquals(LicenseStatus.ACTIVE, license.getStatus());
        assertEquals(product.getId(), license.getProduct().getId());

        LocalDateTime now = LocalDateTime.now();
        DepartmentMonthlyBudget budget = budgetRepository
                .findByDepartmentIdAndBudgetYearAndBudgetMonth(department.getId(), now.getYear(), now.getMonthValue())
                .orElseThrow();
        assertEquals(0, new BigDecimal("625.00").compareTo(budget.getSpent()));
    }

    @Test
    void approveRequest_unlimitedSeats_succeeds() {
        product.setMaxSeatsCount(null);
        productRepository.save(product);

        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Unlimited seats product");

        LicenseRequest approved = licenseRequestService.approveRequest(request.getId(), manager.getId());

        assertEquals(LicenseRequestStatus.ACCEPTED, approved.getStatus());
        assertEquals(1, licenseRepository.countByProductIdAndStatus(product.getId(), LicenseStatus.ACTIVE));
    }

    @Test
    void approveRequest_noAvailableSeats_throwsNoAvailableSeatsException() {
        product.setMaxSeatsCount(1);
        productRepository.save(product);

        User otherUser = User.builder()
                .email(new Email("colleague@teampass.test"))
                .passwordHash("hashed-password")
                .name("Karel")
                .surname("Dvorak")
                .personId("EMP-002")
                .department(department)
                .build();
        otherUser = userRepository.save(otherUser);

        License existingLicense = License.builder()
                .user(otherUser)
                .product(product)
                .lockedPrice(new BigDecimal("625.00"))
                .status(LicenseStatus.ACTIVE)
                .build();
        licenseRepository.save(existingLicense);

        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need last seat");

        assertThrows(NoAvailableSeatsException.class, () ->
                licenseRequestService.approveRequest(request.getId(), manager.getId()));
    }

    @Test
    void approveRequest_differentDepartmentManager_throwsAccessDeniedException() {
        LicenseRequest request = licenseRequestService.requestLicense(
                user.getId(), product.getId(), "Need license");

        Department otherDept = new Department();
        otherDept.setName("Sales");
        otherDept.setDefaultMonthlyLimit(new BigDecimal("5000.00"));
        otherDept = departmentRepository.save(otherDept);

        User otherManager = User.builder()
                .email(new Email("sales.manager@teampass.test"))
                .passwordHash("hashed-password")
                .name("Lucie")
                .surname("Nova")
                .personId("MGR-003")
                .department(otherDept)
                .build();
        otherManager.setRole(Role.DEPARTMENT_MANAGER);
        otherManager = userRepository.save(otherManager);

        Long otherManagerId = otherManager.getId();
        assertThrows(AccessDeniedException.class, () ->
                licenseRequestService.approveRequest(request.getId(), otherManagerId));
    }
}
