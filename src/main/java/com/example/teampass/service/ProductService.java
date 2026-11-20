package com.example.teampass.service;

import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.model.product.Product;
import com.example.teampass.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product getActiveProductOrThrow(Long productId) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        Product product = optionalProduct.orElseThrow(
                () -> new IllegalArgumentException("Product with id: " + productId + " not found")
        );

        if (!product.isActive()) {
            throw new InactiveProductException("Product with id: "+ productId + " is not active");
        }

        return product;
    }
}
