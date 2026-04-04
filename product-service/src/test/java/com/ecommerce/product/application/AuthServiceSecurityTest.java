package com.ecommerce.product.application;

import com.ecommerce.product.domain.User;
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
class AuthServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void forgetPassword_shouldNotLeakWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgetPassword("missing@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgetPassword_shouldNotLeakWhenCustomerRole() {
        User customer = new User();
        customer.setEmail("customer@example.com");
        customer.setRole("Customer");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));

        assertDoesNotThrow(() -> authService.forgetPassword("customer@example.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgetPassword_shouldStoreHashedTokenAndSendRawToken() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole("Admin");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.forgetPassword("admin@example.com");

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(org.mockito.Mockito.eq("admin@example.com"), rawTokenCaptor.capture());

        String rawToken = rawTokenCaptor.getValue();
        assertNotNull(rawToken);
        assertNotEquals(rawToken, savedUser.getPasswordResetToken());
        assertTrue(savedUser.getPasswordResetToken().matches("^[a-f0-9]{64}$"));
        assertNotNull(savedUser.getPasswordResetTokenExpiresAt());
        assertTrue(savedUser.getPasswordResetTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(29)));
    }

    @Test
    void confirmForgetPassword_shouldRejectShortPassword() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.confirmForgetPassword("token", "123"));

        assertEquals("Yeni şifre en az 6 karakter olmalıdır!", ex.getMessage());
    }

    @Test
    void confirmForgetPassword_shouldSupportLegacyPlainTokenLookup() {
        User user = new User();
        user.setPassword("old");
        user.setPasswordResetToken("legacyPlainToken");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByPasswordResetToken(anyString()))
                .thenReturn(Optional.empty(), Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("ENCODED");

        authService.confirmForgetPassword("legacyPlainToken", "new-password");

        assertEquals("ENCODED", user.getPassword());
        assertEquals(null, user.getPasswordResetToken());
        assertEquals(null, user.getPasswordResetTokenExpiresAt());
        verify(userRepository).save(user);
    }

    @Test
    void confirmForgetPassword_shouldInvalidateExpiredToken() {
        User user = new User();
        user.setPasswordResetToken("hash");
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByPasswordResetToken(anyString())).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.confirmForgetPassword("token", "new-password"));

        assertEquals("Sıfırlama bağlantısının süresi doldu. Lütfen yeniden talep oluşturun.", ex.getMessage());
        assertEquals(null, user.getPasswordResetToken());
        assertEquals(null, user.getPasswordResetTokenExpiresAt());
        verify(userRepository).save(user);
    }
}
