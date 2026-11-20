package com.example.teampass.service;

import com.example.teampass.exception.*;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.Role;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.LicenseRepository;
import com.example.teampass.repository.LicenseRequestRepository;
import com.example.teampass.repository.ProductRepository;
import com.example.teampass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicenseRequestService {

    private final LicenseService licenseService;

    private final UserRepository userRepository;
    private final LicenseRequestRepository licenseRequestRepository;
    private final ProductRepository productRepository;
    private final LicenseRepository licenseRepository;


    @Transactional
    public LicenseRequest requestLicense(Long userId, Long productId, String message) {
        User user = getActiveUserOrElseThrow(userId);
        Product product = getProductOrElseThrow(productId);

        if (!product.isActive()) {
            throw new InactiveProductException("Product with id: "+ productId + " is not active");
        }

        boolean pendingRequestAlreadyExists = licenseRequestRepository.existsByUserIdAndProductIdAndStatus(
                userId, productId, LicenseRequestStatus.PENDING);
        if (pendingRequestAlreadyExists) {
            throw new DuplicateLicenseRequestException(
                    "You've already requested license for " + product.getName() + ", it's still pending"
            );
        }

        boolean activeLicenseAlreadyExists = licenseRepository.existsByUserIdAndProductIdAndStatus(
                userId, productId, LicenseStatus.ACTIVE);
        if (activeLicenseAlreadyExists) {
            throw new DuplicateLicenseRequestException(
                    "You already have license for " + product.getName()
            );
        }

        // todo implement acquiring license for managers by themselves

        LicenseRequest licenseRequest = LicenseRequest.builder()
                .user(user)
                .product(product)
                .message(message)
        .build();

        return licenseRequestRepository.save(licenseRequest);
    }

    @Transactional
    public LicenseRequest rejectRequest(Long requestId, Long managerId) {
        User manager = getActiveManagerOrThrow(managerId);
        LicenseRequest licenseRequest = getPendingRequestForManagerOrThrow(requestId, manager);

        licenseRequest.setStatus(LicenseRequestStatus.DENIED);
        licenseRequest.setReviewedBy(manager);

        return licenseRequestRepository.save(licenseRequest);
    }

    @Transactional
    public LicenseRequest approveRequest(Long requestId, Long managerId) {
        User manager = getActiveManagerOrThrow(managerId);
        LicenseRequest licenseRequest = getPendingRequestForManagerOrThrow(requestId, manager);

        User user = licenseRequest.getUser();
        if (!user.isActive()) {
            throw new InactiveUserException(
                    "User " + user.getEmail().value() + " is not active anymore"
            );
        }

        Product product = licenseRequest.getProduct();
        if (!product.isActive()) {
            throw new InactiveProductException(
                    "Product " + product.getName() + " is not active anymore"
            );
        }

        licenseRequest.setStatus(LicenseRequestStatus.ACCEPTED);
        licenseRequest.setReviewedBy(manager);
        licenseRequest = licenseRequestRepository.save(licenseRequest);

        licenseService.issueLicense(licenseRequest);

        return licenseRequest;
    }

    private User getActiveUserOrElseThrow(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        User user = optionalUser.orElseThrow(
                () -> new IllegalArgumentException("User with id: " + userId + " not found")
        );

        if (!user.isActive()) {
            throw new InactiveUserException("User with id: "+ userId + " is not active");
        }

        return user;
    }

    private User getActiveManagerOrThrow(Long managerId) {
        User manager = getActiveUserOrElseThrow(managerId);

        if (manager.getRole() != Role.DEPARTMENT_MANAGER) {
            throw new AccessDeniedException("DEPARTMENT_MANAGER role required to review license request");
        }

        return manager;
    }

    private LicenseRequest getLicenseRequestOrElseThrow(Long requestId) {
        Optional<LicenseRequest> optionalLicenseRequest = licenseRequestRepository.findById(requestId);
        return optionalLicenseRequest.orElseThrow(
                () -> new IllegalArgumentException("License request with id " + requestId + " not found")
        );
    }

    private LicenseRequest getPendingRequestForManagerOrThrow(Long requestId, User manager) {
        LicenseRequest licenseRequest = getLicenseRequestOrElseThrow(requestId);

        if (!manager.getDepartment().equals(licenseRequest.getDepartment())) {
            throw new AccessDeniedException(
                    "Manager has to be from the department " + licenseRequest.getDepartment().getName()
            );
        }

        if (licenseRequest.getStatus() != LicenseRequestStatus.PENDING) {
            throw new InvalidLicenseRequestStateException("License request has to be pending to review it");
        }

        return licenseRequest;
    }

    private Product getProductOrElseThrow(Long productId) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        return optionalProduct.orElseThrow(
                () -> new IllegalArgumentException("Product with id: " + productId + " not found")
        );
    }
}
