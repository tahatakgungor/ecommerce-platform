package com.ecommerce.product.api;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://ecommerce-frontend-xryc.vercel.app"})
public class CategoryController {

    // private final CategoryService categoryService; // İleride aktif edilecek

    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        // Şimdilik mock veri, ama ApiResponse formatı kesinleştirildi
        List<Category> categories = List.of(
                createMockCategory("Electronics", List.of("Mobile", "Laptop")),
                createMockCategory("Fashion", List.of("Shoes", "Clothing"))
        );
        return new ApiResponse<>(true, categories, (long) categories.size());
    }

    private Category createMockCategory(String name, List<String> subs) {
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setParentName(name);
        c.setChildren(subs);
        return c;
    }
}