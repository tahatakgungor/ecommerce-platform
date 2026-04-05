package com.ecommerce.product.application;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı!"));
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public Category createCategory(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new RuntimeException("Bu kategori zaten mevcut!");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public Category updateCategory(UUID id, Category details) {
        Category category = getCategoryById(id);
        category.setName(details.getName());
        category.setDescription(details.getDescription());
        category.setImage(details.getImage());
        category.setChildren(details.getChildren());
        return categoryRepository.save(category);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')")
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Kategori bulunamadı.");
        }
        categoryRepository.deleteById(id);
    }
}
