package com.ecommerce.product.application;

import com.ecommerce.product.domain.ContactMessage;
import com.ecommerce.product.dto.ContactRequest;
import com.ecommerce.product.repository.ContactMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactMessageServiceTest {

    @Mock
    private ContactMessageRepository contactMessageRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ContactMessageService contactMessageService;

    @Test
    void create_shouldPersistAsNewAndNormalizeEmail() {
        ContactRequest request = new ContactRequest();
        request.setName("Taha");
        request.setEmail("TAHA@MAIL.COM");
        request.setPhone("5555555555");
        request.setCompany("Serravit");
        request.setMessage("Test mesajı");

        ContactMessage saved = new ContactMessage();
        saved.setId(UUID.randomUUID());
        saved.setName("Taha");
        saved.setEmail("taha@mail.com");
        saved.setStatus("NEW");
        saved.setMessage("Test mesajı");

        when(contactMessageRepository.save(any(ContactMessage.class))).thenReturn(saved);

        Map<String, Object> result = contactMessageService.create(request);

        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(contactMessageRepository).save(captor.capture());
        assertEquals("taha@mail.com", captor.getValue().getEmail());
        assertEquals("NEW", captor.getValue().getStatus());
        assertEquals("NEW", result.get("status"));
    }

    @Test
    void getAllForAdmin_shouldReturnPagedStructure() {
        ContactMessage item = new ContactMessage();
        item.setId(UUID.randomUUID());
        item.setName("Müşteri");
        item.setEmail("user@mail.com");
        item.setStatus("NEW");
        item.setMessage("Merhaba");

        when(contactMessageRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Map<String, Object> result = contactMessageService.getAllForAdmin("ALL", 1, 50);

        assertEquals(1L, result.get("total"));
        assertEquals(1, result.get("page"));
    }

    @Test
    void updateStatus_shouldRejectInvalidStatus() {
        UUID id = UUID.randomUUID();
        ContactMessage existing = new ContactMessage();
        existing.setId(id);
        existing.setEmail("user@mail.com");
        existing.setStatus("NEW");
        existing.setMessage("Mesaj");
        when(contactMessageRepository.findById(id)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> contactMessageService.updateStatus(id, "DONE", null, "admin@mail.com")
        );
        assertEquals("Geçersiz durum. NEW, IN_PROGRESS veya RESOLVED olmalı.", ex.getMessage());
    }

    @Test
    void updateStatus_shouldPersistAndReturnUpdatedMessage() {
        UUID id = UUID.randomUUID();
        ContactMessage existing = new ContactMessage();
        existing.setId(id);
        existing.setName("Müşteri");
        existing.setEmail("user@mail.com");
        existing.setStatus("NEW");
        existing.setMessage("Yardım talebi");

        when(contactMessageRepository.findById(id)).thenReturn(Optional.of(existing));
        when(contactMessageRepository.save(any(ContactMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = contactMessageService.updateStatus(id, "RESOLVED", "Çözüldü", "admin@mail.com");

        assertEquals("RESOLVED", result.get("status"));
        assertEquals("Çözüldü", result.get("adminNote"));
        verify(activityLogService).log(eq("CONTACT_MESSAGE_STATUS_UPDATED"), eq("INFO"), any(), eq("admin@mail.com"), eq("CONTACT_MESSAGE"), eq(id.toString()), any());
    }
}
