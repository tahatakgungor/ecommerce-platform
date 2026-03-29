package com.ecommerce.product.api.client;

import com.ecommerce.product.application.CategoryService;
import com.ecommerce.product.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryClientController {

    private final CategoryService categoryService;

    @GetMapping("/show")
    public Map<String, Object> getPublicCategories() {
        List<Category> categories = categoryService.getAllCategories();
        Map<String, Object> response = new HashMap<>();
        response.put("categories", categories);
        return response;
    }
}