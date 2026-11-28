package com.example.teampass.service;

import com.example.teampass.dto.CreateProductRequest;
import com.example.teampass.dto.UpdateProductRequest;
import com.example.teampass.exception.AlreadyExistsException;
import com.example.teampass.exception.InactiveProductException;
import com.example.teampass.exception.ProductWithoutCategoryException;
import com.example.teampass.model.product.Product;
import com.example.teampass.repository.ProductRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product getActiveProductOrThrow(@NonNull Long activeProductId) {
        Product product = getProductOrThrow(activeProductId);

        if (!product.isActive()) {
            throw new InactiveProductException("Product with id: "+ activeProductId + " is not active");
        }

        return product;
    }

    @Transactional
    public Product getProductOrThrow(@NonNull Long productId) {
        Optional<Product> optionalProduct = productRepository.findById(productId);

        return optionalProduct.orElseThrow(
                () -> new IllegalArgumentException("Product with id: " + productId + " not found")
        );
    }

    @Transactional
    public Product createProduct(@NonNull CreateProductRequest request) {
        if (productRepository.existsByName(request.name())) {
            throw new AlreadyExistsException("Product with name " + request.name() + " already exists");
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .maxSeatsCount(request.maxSeatsCount())
                .price(request.price())
                .categories(request.categories())
        .build();

        product = productRepository.save(product);
        return product;
    }

    @Transactional
    public Product updateProduct(@NonNull Long productId , @NonNull UpdateProductRequest request) {
        Product product = getProductOrThrow(productId);

        if (request.isEmpty()) {
            return product;
        }

        assignCorrectArgumentsIntoProduct(product, request);

        product = productRepository.save(product);
        return product;
    }

    private void assignCorrectArgumentsIntoProduct(Product product, UpdateProductRequest request) {
        if (request.name() != null && !Objects.equals(product.getName(), request.name())) {
            if (productRepository.existsByName(request.name())) {
                throw new AlreadyExistsException("Product with name " + request.name() + " already exists");
            }
            product.setName(request.name());
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.maxSeatsCount() != null) product.setMaxSeatsCount(request.maxSeatsCount());
        if (request.price() != null) product.setPrice(request.price());
        if (request.isActive() != null) product.setActive(request.isActive());

        // todo check if the message in exception is correct
        if (request.categories() != null) {
            if (request.categories().isEmpty()) {
                throw new ProductWithoutCategoryException("Product can't be without category");
            }

            product.setCategories(request.categories());
        }
    }

    // todo add method deleteProduct to orchestra class
}
