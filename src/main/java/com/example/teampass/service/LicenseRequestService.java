package com.example.teampass.service;

import com.example.teampass.exception.*;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.LicenseRepository;
import com.example.teampass.repository.LicenseRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicenseRequestService {

    private final LicenseService licenseService;
    private final UserService userService;
    private final ProductService productService;

    private final LicenseRequestRepository licenseRequestRepository;
    private final LicenseRepository licenseRepository;


    @Transactional
    public LicenseRequest requestLicense(Long userId, Long productId, String message) {
        User user = userService.getActiveUserOrThrow(userId);
        Product product = productService.getActiveProductOrThrow(productId);

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

        LicenseRequest licenseRequest = LicenseRequest.builder()
                .user(user)
                .product(product)
                .message(message)
        .build();

        return licenseRequestRepository.save(licenseRequest);
    }

    @Transactional
    public LicenseRequest rejectRequest(Long requestId, Long managerId) {
        User manager = userService.getActiveManagerOrThrow(managerId);
        LicenseRequest licenseRequest = getPendingRequestForManagerOrThrow(requestId, manager);

        licenseRequest.setStatus(LicenseRequestStatus.DENIED);
        licenseRequest.setReviewedBy(manager);

        return licenseRequestRepository.save(licenseRequest);
    }

    @Transactional
    public LicenseRequest approveRequest(Long requestId, Long managerId) {
        User manager = userService.getActiveManagerOrThrow(managerId);
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

    private LicenseRequest getLicenseRequestOrThrow(Long requestId) {
        Optional<LicenseRequest> optionalLicenseRequest = licenseRequestRepository.findById(requestId);
        return optionalLicenseRequest.orElseThrow(
                () -> new IllegalArgumentException("License request with id " + requestId + " not found")
        );
    }

    private LicenseRequest getPendingRequestForManagerOrThrow(Long requestId, User manager) {
        LicenseRequest licenseRequest = getLicenseRequestOrThrow(requestId);

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
}
