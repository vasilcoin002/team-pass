package com.example.teampass.service;

import com.example.teampass.exception.DuplicateLicenseRequestException;
import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.model.department.Department;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Price;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.Email;
import com.example.teampass.model.user.User;
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

    private Department department;
    private User user;
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
}
