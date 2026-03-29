package com.ecommerce.product.api.client;

import com.ecommerce.product.application.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/category") // Frontend 'api/category' bekliyor
@RequiredArgsConstructor
public class CategoryClientController {

    private final CategoryService categoryService;

  /*  // Frontend: useGetCategoriesQuery -> '/api/category/show'
    @GetMapping("/show")
    public List<CategoryResponse> getCategories() {
        return categoryService.getAllCategoriesWithChildren();
    }*/
}