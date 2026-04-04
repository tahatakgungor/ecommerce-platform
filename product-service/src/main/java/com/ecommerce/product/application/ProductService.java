package com.ecommerce.product.application;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı! ID: " + id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin ekleyebilir
    public Product save(Product product) {
        applyDefaultValues(product);
        Product savedProduct = productRepository.save(product);
        log.info("Yeni ürün Admin tarafından kaydedildi: {}", savedProduct.getName());
        return savedProduct;
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin güncelleyebilir
    public Product update(UUID id, Product details) {
        Product existing = findById(id);

        if (details.getName() != null) existing.setName(details.getName());
        if (details.getDescription() != null) existing.setDescription(details.getDescription());
        if (details.getPrice() != null) {
            existing.setPrice(details.getPrice());
        }
        if (details.getOriginalPrice() != null && details.getOriginalPrice() > 0) {
            existing.setOriginalPrice(details.getOriginalPrice());
        }
        if (details.getStockQuantity() != null) existing.setStockQuantity(details.getStockQuantity());
        if (details.getSku() != null) existing.setSku(details.getSku());
        if (details.getImage() != null) existing.setImage(details.getImage());
        if (details.getStatus() != null) existing.setStatus(details.getStatus());
        if (details.getParentCategory() != null) existing.setParentCategory(details.getParentCategory());
        if (details.getCategoryName() != null) existing.setCategoryName(details.getCategoryName());
        if (details.getBrandName() != null) existing.setBrandName(details.getBrandName());
        if (details.getTags() != null) existing.setTags(details.getTags());
        if (details.getColors() != null) existing.setColors(details.getColors());

        // İndirim yüzdesinin negatif olmaması için güvenlik:
        // originalPrice hiçbir durumda price'dan küçük kalmasın.
        if (existing.getPrice() != null && existing.getOriginalPrice() != null
                && existing.getOriginalPrice() < existing.getPrice()) {
            existing.setOriginalPrice(existing.getPrice());
        }

        return productRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAuthority('Admin')") // GÜVENLİK: Sadece Admin silebilir
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Silinecek ürün bulunamadı!");
        }
        productRepository.deleteById(id);
        log.info("Ürün başarıyla silindi. ID: {}", id);
    }

    private void applyDefaultValues(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (product.getStatus() == null) product.setStatus("Active");
        if (product.getPrice() == null) product.setPrice(0.0);
        if (product.getOriginalPrice() == null) product.setOriginalPrice(product.getPrice());
    }
}
