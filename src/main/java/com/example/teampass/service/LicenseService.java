package com.example.teampass.service;

import com.example.teampass.exception.NoAvailableSeatsException;
import com.example.teampass.model.license.License;
import com.example.teampass.model.license.LicenseRequest;
import com.example.teampass.model.license.LicenseStatus;
import com.example.teampass.model.product.Product;
import com.example.teampass.model.user.User;
import com.example.teampass.repository.LicenseRepository;
import com.example.teampass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final ExchangeRateService exchangeRateService;

    private final UserRepository userRepository;
    private final LicenseRepository licenseRepository;


    @Transactional
    public License issueLicense(User user, Product product) {
        // other checks

        long currentSeatsCount = licenseRepository.countByProductIdAndStatus(
                product.getId(), LicenseStatus.ACTIVE
        );
        if (product.getMaxSeatsCount() != null && currentSeatsCount >= product.getMaxSeatsCount()) {
            throw new NoAvailableSeatsException("There are no available seats left for the product " + product.getName());
        }


        // todo implement properly
        return new License();
    }

    @Transactional
    public License issueLicense(LicenseRequest licenseRequest) {
        return issueLicense(licenseRequest.getUser(), licenseRequest.getProduct());
    }

}
