package com.ecommerce.product.api;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {

    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        List<Category> categories = new ArrayList<>();

        // 1. Kategori
        Category cat1 = new Category();
        cat1.setId(UUID.randomUUID());
        cat1.setParentName("Electronics"); // Ekranda görünecek ana başlık
        cat1.setChildren(List.of("Mobile", "Laptop", "Tablet")); // Alt kategoriler (String)

        // 2. Kategori
        Category cat2 = new Category();
        cat2.setId(UUID.randomUUID());
        cat2.setParentName("Fashion");
        cat2.setChildren(List.of("Shoes", "Clothing"));

        categories.add(cat1);
        categories.add(cat2);

        return new ApiResponse<>(true, categories, 2L);
    }
}