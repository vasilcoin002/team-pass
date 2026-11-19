package com.example.teampass.service;

import com.example.teampass.exception.DuplicateLicenseRequestException;
import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.exception.InactiveUserException;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseRequestStatus;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Product;
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

    private final UserRepository userRepository;
    private final LicenseRequestRepository licenseRequestRepository;
    private final ProductRepository productRepository;
    private final LicenseRepository licenseRepository;

    @Transactional
    public LicenseRequest requestLicense(Long userId, Long productId, String message) {
        Optional<User> optionalUser = userRepository.findById(userId);
        User user = optionalUser.orElseThrow(
                () -> new IllegalArgumentException("User with id: " + userId + " not found")
        );

        if (!user.isActive()) {
            throw new InactiveUserException("User with id: "+ userId + " is not active");
        }

        Optional<Product> optionalProduct = productRepository.findById(productId);
        Product product = optionalProduct.orElseThrow(
                () -> new IllegalArgumentException("Product with id: " + productId + " not found")
        );

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

        LicenseRequest licenseRequest = LicenseRequest.builder()
                .user(user)
                .product(product)
                .message(message)
        .build();

        return licenseRequestRepository.save(licenseRequest);
    }


//    public LicenseRequest approveRequest() {

//        long currentProductUsersCount = licenseRepository.countByProductIdAndStatus(productId, LicenseStatus.ACTIVE);
//
//        if (product.getMaxSeatsCount() != null && currentProductUsersCount >= product.getMaxSeatsCount()) {
//            throw new NoAvailableSeatsException(
//                    "There are no available seats for the product " +
//                            product.getName() + " for now. " +
//                            "Max seats count : " + product.getMaxSeatsCount() +
//                            ", current product users count: " + currentProductUsersCount
//            );
//        }
//    }
}
