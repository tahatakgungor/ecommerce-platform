package com.ecommerce.product.api;

import com.ecommerce.product.application.CategoryService;
import com.ecommerce.product.domain.Category;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ApiResponse.ok(categories, (long) categories.size());
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> getCategoryById(@PathVariable UUID id) {
        return ApiResponse.ok(categoryService.getCategoryById(id), 1L);
    }

    @PostMapping("/add")
    public ApiResponse<Category> addCategory(@RequestBody Category category) {
        return ApiResponse.ok(categoryService.createCategory(category), 1L);
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable UUID id, @RequestBody Category category) {
        return ApiResponse.ok(categoryService.updateCategory(id, category), 1L);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ApiResponse.ok("Kategori başarıyla silindi.", 1L);
    }
}