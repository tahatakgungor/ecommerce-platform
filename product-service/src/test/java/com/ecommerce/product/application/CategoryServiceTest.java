package com.ecommerce.product.application;

import com.ecommerce.product.domain.Category;
import com.ecommerce.product.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_duplicateName_shouldThrow() {
        Category category = new Category();
        category.setName("Detoks");

        when(categoryRepository.findByName("Detoks")).thenReturn(Optional.of(new Category()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> categoryService.createCategory(category));
        assertEquals("Bu kategori zaten mevcut!", ex.getMessage());
    }

    @Test
    void createCategory_uniqueName_shouldSave() {
        Category category = new Category();
        category.setName("Yeni Kategori");

        when(categoryRepository.findByName("Yeni Kategori")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = categoryService.createCategory(category);
        assertEquals("Yeni Kategori", saved.getName());
    }

    @Test
    void updateCategory_shouldPersistFields() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Old");

        Category patch = new Category();
        patch.setName("New");
        patch.setDescription("Açıklama");
        patch.setImage("image.png");
        patch.setChildren(List.of("Gıda Takviyesi", "Kozmetik"));

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.updateCategory(id, patch);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertEquals("New", saved.getName());
        assertEquals("Açıklama", saved.getDescription());
        assertEquals("image.png", saved.getImage());
        assertEquals(List.of("Gıda Takviyesi", "Kozmetik"), saved.getChildren());
    }

    @Test
    void deleteCategory_notExists_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> categoryService.deleteCategory(id));
        assertEquals("Kategori bulunamadı.", ex.getMessage());
    }

    @Test
    void deleteCategory_exists_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(true);

        categoryService.deleteCategory(id);

        verify(categoryRepository).deleteById(id);
    }
}
