package com.ecommerce.product.application;

import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.repository.BrandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    @Test
    void createBrand_duplicateName_shouldThrow() {
        Brand brand = new Brand();
        brand.setName("SERRAVIT");

        when(brandRepository.findByName("SERRAVIT")).thenReturn(Optional.of(new Brand()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> brandService.createBrand(brand));
        assertEquals("Bu marka zaten kayıtlı!", ex.getMessage());
    }

    @Test
    void createBrand_uniqueName_shouldSave() {
        Brand brand = new Brand();
        brand.setName("Humidone");

        when(brandRepository.findByName("Humidone")).thenReturn(Optional.empty());
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Brand saved = brandService.createBrand(brand);
        assertEquals("Humidone", saved.getName());
    }

    @Test
    void updateBrand_shouldPersistAllChangedFields() {
        UUID id = UUID.randomUUID();
        Brand existing = new Brand();
        existing.setId(id);
        existing.setName("Old");

        Brand patch = new Brand();
        patch.setName("New");
        patch.setEmail("info@example.com");
        patch.setWebsite("https://example.com");
        patch.setLocation("Istanbul");
        patch.setDescription("Desc");
        patch.setLogo("logo.png");
        patch.setStatus("Active");

        when(brandRepository.findById(id)).thenReturn(Optional.of(existing));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        brandService.updateBrand(id, patch);

        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        Brand saved = captor.getValue();
        assertEquals("New", saved.getName());
        assertEquals("info@example.com", saved.getEmail());
        assertEquals("https://example.com", saved.getWebsite());
        assertEquals("Istanbul", saved.getLocation());
        assertEquals("Desc", saved.getDescription());
        assertEquals("logo.png", saved.getLogo());
        assertEquals("Active", saved.getStatus());
    }

    @Test
    void deleteBrand_notExists_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(brandRepository.existsById(id)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> brandService.deleteBrand(id));
        assertEquals("Silinecek marka bulunamadı.", ex.getMessage());
    }

    @Test
    void deleteBrand_exists_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(brandRepository.existsById(id)).thenReturn(true);

        brandService.deleteBrand(id);

        verify(brandRepository).deleteById(id);
    }
}
