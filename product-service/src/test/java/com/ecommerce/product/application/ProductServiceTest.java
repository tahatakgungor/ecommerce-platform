package com.ecommerce.product.application;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void save_shouldApplyDefaultValues() {
        Product input = new Product();
        input.setName("Yeni Ürün");
        input.setPrice(null);
        input.setOriginalPrice(null);
        input.setStatus(null);
        input.setSku(null);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = productService.save(input);

        assertNotNull(saved.getSku());
        assertEquals("Active", saved.getStatus());
        assertEquals(0.0, saved.getPrice());
        assertEquals(0.0, saved.getOriginalPrice());
    }

    @Test
    void update_whenOnlyPriceProvided_shouldKeepExistingOriginalPrice() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Eski Ürün");
        existing.setPrice(100.0);
        existing.setOriginalPrice(140.0);

        Product patch = new Product();
        patch.setName("Yeni Ürün");
        patch.setPrice(120.0);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.update(id, patch);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("Yeni Ürün", saved.getName());
        assertEquals(120.0, saved.getPrice());
        assertEquals(140.0, saved.getOriginalPrice());
    }

    @Test
    void update_whenOriginalPriceExplicitlyProvided_shouldUseProvidedOriginalPrice() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setPrice(100.0);
        existing.setOriginalPrice(140.0);

        Product patch = new Product();
        patch.setPrice(90.0);
        patch.setOriginalPrice(150.0);

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.update(id, patch);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals(90.0, saved.getPrice());
        assertEquals(150.0, saved.getOriginalPrice());
    }

    @Test
    void findById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> productService.findById(id));
        assertEquals("Ürün bulunamadı! ID: " + id, ex.getMessage());
    }

    @Test
    void delete_notExists_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> productService.delete(id));
        assertEquals("Silinecek ürün bulunamadı!", ex.getMessage());
    }

    @Test
    void delete_exists_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.delete(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void update_whenRelatedImagesProvided_shouldReplaceRelatedImages() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setName("Urun");
        existing.setRelatedImages(List.of("https://old.example.com/1.jpg"));

        Product patch = new Product();
        patch.setRelatedImages(List.of(
                "https://cdn.example.com/new-1.jpg",
                "https://cdn.example.com/new-2.jpg"
        ));

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.update(id, patch);

        assertEquals(2, updated.getRelatedImages().size());
        assertEquals("https://cdn.example.com/new-1.jpg", updated.getRelatedImages().get(0));
        assertEquals("https://cdn.example.com/new-2.jpg", updated.getRelatedImages().get(1));
    }

    @Test
    void update_whenCategoryFieldsProvided_shouldPersistChildCategory() {
        UUID id = UUID.randomUUID();
        Product existing = new Product();
        existing.setId(id);
        existing.setParentCategory("Eski Parent");
        existing.setCategoryName("Eski Kategori");
        existing.setChildCategory("Eski Child");

        Product patch = new Product();
        patch.setParentCategory("Tarım");
        patch.setCategoryName("Diğer");
        patch.setChildCategory("Diğer");

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = productService.update(id, patch);

        assertEquals("Tarım", updated.getParentCategory());
        assertEquals("Diğer", updated.getCategoryName());
        assertEquals("Diğer", updated.getChildCategory());
    }
}
