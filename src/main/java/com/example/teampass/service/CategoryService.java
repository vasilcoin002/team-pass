package com.example.teampass.service;

import com.example.teampass.exception.AlreadyExistsException;
import com.example.teampass.model.product.Category;
import com.example.teampass.repository.CategoryRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Category getCategoryOrThrow(@NonNull Long categoryId) {
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
        return optionalCategory.orElseThrow(
                () -> new IllegalArgumentException("Category with id: " + categoryId + " not found")
        );
    }

    @Transactional(readOnly = true)
    public Category getCategoryOrThrow(@NonNull String name) {
        Optional<Category> optionalCategory = categoryRepository.findByName(name);
        return optionalCategory.orElseThrow(
                () -> new IllegalArgumentException("Category with name: " + name + " not found")
        );
    }

    @Transactional
    public Category createCategory(@NonNull String name) {
        if (categoryRepository.existsByName(name)) {
            throw new AlreadyExistsException("Category with name " + name + " already exists");
        }

        Category category = new Category(name);
        category = categoryRepository.save(category);

        return category;
    }

    @Transactional
    public Category updateCategory(@NonNull Long categoryId, @NonNull String newName) {
        Category category = getCategoryOrThrow(categoryId);

        if (Objects.equals(category.getName(), newName)) {
            return category;
        }

        category.setName(newName);

        category = categoryRepository.save(category);
        return category;
    }

    @Transactional
    public boolean deleteCategory(@NonNull Long categoryId) {
        boolean categoryExists = categoryRepository.existsById(categoryId);

        if (!categoryExists) {
            throw new IllegalArgumentException("Category with id " + categoryId + " not found");
        }

        categoryRepository.deleteById(categoryId);
        return true;
    }
}
