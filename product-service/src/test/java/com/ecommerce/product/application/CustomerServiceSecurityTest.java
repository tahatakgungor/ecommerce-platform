package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.NewsletterRepository;
import com.ecommerce.product.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NewsletterRepository newsletterRepository;

    @Mock
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void forgetPassword_shouldNotLeakWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> customerService.forgetPassword("missing@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgetPassword_shouldIgnoreNonCustomerRole() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole("Admin");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> customerService.forgetPassword("admin@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgetPassword_shouldStoreHashedTokenForCustomerAndSendRawToken() {
        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setRole("Customer");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.forgetPassword("customer@example.com");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(org.mockito.Mockito.eq("customer@example.com"), rawTokenCaptor.capture());

        String rawToken = rawTokenCaptor.getValue();
        assertNotNull(rawToken);
        assertNotEquals(rawToken, savedUser.getPasswordResetToken());
        assertTrue(savedUser.getPasswordResetToken().matches("^[a-f0-9]{64}$"));
        assertNotNull(savedUser.getPasswordResetTokenExpiresAt());
    }

    @Test
    void confirmForgetPassword_shouldRejectShortPassword() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.confirmForgetPassword("token", "123", "123"));

        assertEquals("Yeni şifre en az 6 karakter olmalıdır!", ex.getMessage());
    }

    @Test
    void confirmForgetPassword_shouldSupportLegacyPlainTokenLookup() {
        User customer = new User();
        customer.setPasswordResetToken("legacyPlainToken");
        customer.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByPasswordResetToken(anyString()))
                .thenReturn(Optional.empty(), Optional.of(customer));
        when(passwordEncoder.encode("new-password")).thenReturn("ENCODED");

        customerService.confirmForgetPassword("legacyPlainToken", "new-password", "new-password");

        assertEquals("ENCODED", customer.getPassword());
        assertEquals(null, customer.getPasswordResetToken());
        assertEquals(null, customer.getPasswordResetTokenExpiresAt());
        verify(userRepository).save(customer);
    }

    @Test
    void confirmForgetPassword_shouldInvalidateExpiredToken() {
        User customer = new User();
        customer.setPasswordResetToken("hash");
        customer.setPasswordResetTokenExpiresAt(LocalDateTime.now().minusMinutes(2));

        when(userRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.of(customer));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> customerService.confirmForgetPassword("token", "new-password", "new-password"));

        assertEquals("Sıfırlama bağlantısının süresi doldu. Lütfen yeniden talep oluşturun.", ex.getMessage());
        assertEquals(null, customer.getPasswordResetToken());
        assertEquals(null, customer.getPasswordResetTokenExpiresAt());
        verify(userRepository).save(customer);
    }
}
