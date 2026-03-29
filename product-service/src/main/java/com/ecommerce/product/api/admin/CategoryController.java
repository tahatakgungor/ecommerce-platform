package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.CategoryService;
import com.ecommerce.product.domain.Category;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Tüm kategorileri ham haliyle getirir (Admin Paneli Tablosu için)
     */
    @GetMapping("/all")
    public ApiResponse<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ApiResponse.ok(categories, (long) categories.size());
    }

    /**
     * ID ile kategori detayını getirir
     */
    @GetMapping("/{id}")
    public ApiResponse<Category> getCategoryById(@PathVariable UUID id) {
        return ApiResponse.ok(categoryService.getCategoryById(id), 1L);
    }

    /**
     * Yeni kategori ekler
     */
    @PostMapping("/add")
    public ApiResponse<Category> addCategory(@RequestBody Category category) {
        log.info("Admin yeni kategori ekliyor: {}", category.getName());
        return ApiResponse.ok(categoryService.createCategory(category), 1L);
    }

    /**
     * Mevcut kategoriyi günceller
     */
    @PutMapping("/update/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable UUID id, @RequestBody Category category) {
        log.info("Admin kategori güncelliyor: ID {}", id);
        return ApiResponse.ok(categoryService.updateCategory(id, category), 1L);
    }

    /**
     * Kategoriyi siler
     * Pom.xml'deki compiler-plugin sayesinde 'id' ismini otomatik eşleştirir.
     */
    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable UUID id) {
        log.info("Admin kategori siliyor: ID {}", id);
        categoryService.deleteCategory(id);
        return ApiResponse.ok("Kategori başarıyla silindi.", 1L);
    }
}