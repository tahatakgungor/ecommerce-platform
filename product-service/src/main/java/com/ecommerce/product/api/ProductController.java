package com.ecommerce.product.api;

import com.ecommerce.product.application.ProductService;
import com.ecommerce.product.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.save(product);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable("id") UUID id) { // ("id") eklendi
        return productService.findById(id);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable("id") UUID id, @RequestBody Product product) { // ("id") eklendi
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable("id") UUID id) {
        productService.delete(id);
        return ResponseEntity.ok("Ürün başarıyla imha edildi! ID: " + id);
    }
}