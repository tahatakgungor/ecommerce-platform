package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.AuthService;
import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.application.InvitationService;
import com.ecommerce.product.application.SecurityRateLimitService;
import com.ecommerce.product.domain.Invitation;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.auth.LoginRequest;
import com.ecommerce.product.dto.auth.LoginResponse;
import com.ecommerce.product.dto.auth.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final EmailService emailService;
    private final SecurityRateLimitService rateLimitService;

    // --- GİRİŞ, KAYIT VE ŞİFRE SIFIRLAMA ---

    @PatchMapping("/forget-password")
    public ApiResponse<String> forgetPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String email = request.get("email");
        String clientIp = extractClientIp(httpRequest);
        String limitKey = "admin-forgot-password:" + normalize(email) + ":" + clientIp;
        Duration window = Duration.ofMinutes(30);
        rateLimitService.assertAllowed(
                limitKey,
                5,
                window,
                "Çok fazla şifre sıfırlama denemesi yapıldı. Lütfen daha sonra tekrar deneyin."
        );

        try {
            authService.forgetPassword(email);
            rateLimitService.clearFailures(limitKey);
        } catch (RuntimeException ex) {
            rateLimitService.registerFailure(limitKey, window);
            throw ex;
        }
        return ApiResponse.ok("Şifre sıfırlama talimatları e-posta adresinize gönderildi.", 1L);
    }

    @PatchMapping("/confirm-forget-password")
    public ApiResponse<String> confirmForgetPassword(@RequestBody Map<String, String> request) {
        authService.confirmForgetPassword(request.get("token"), request.get("password"));
        return ApiResponse.ok("Şifreniz başarıyla güncellendi.", 1L);
    }

    @PatchMapping("/change-password")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        authService.changePassword(
                authentication.getName(),
                request.get("oldPass"),
                request.get("newPass")
        );
        return ApiResponse.ok("Şifreniz başarıyla güncellendi.", 1L);
    }

    @PatchMapping("/change-password/request")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> requestChangePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        authService.requestPasswordChangeVerification(
                authentication.getName(),
                request.get("oldPass"),
                request.get("newPass")
        );
        return ApiResponse.ok("Doğrulama kodu e-posta adresinize gönderildi.", 1L);
    }

    @PatchMapping("/change-password/confirm")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<String> confirmChangePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        authService.confirmPasswordChangeVerification(authentication.getName(), request.get("code"));
        return ApiResponse.ok("Şifreniz başarıyla güncellendi.", 1L);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String limitKey = "admin-login:" + normalize(request.email()) + ":" + clientIp;
        Duration window = Duration.ofMinutes(15);
        rateLimitService.assertAllowed(
                limitKey,
                10,
                window,
                "Çok fazla giriş denemesi yapıldı. Lütfen 15 dakika sonra tekrar deneyin."
        );

        LoginResponse response;
        try {
            response = authService.login(request);
            rateLimitService.clearFailures(limitKey);
        } catch (RuntimeException ex) {
            rateLimitService.registerFailure(limitKey, window);
            throw ex;
        }
        return ApiResponse.ok(response, 1L);
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request, @RequestParam("token") String token) {
        Invitation invite = invitationService.validateAndGetInvite(token);
        if (!invite.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Sadece davet edildiğiniz e-posta ile kayıt olabilirsiniz.");
        }
        authService.registerWithRole(request, invite.getRole());
        invitationService.markAsUsed(invite);
        return ApiResponse.ok("Kayıt başarıyla tamamlandı.", 1L);
    }

    // --- PERSONEL LİSTELEME ---

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<?> getAllStaff() {
        return ApiResponse.ok(authService.getAllUsers(), (long) authService.getAllUsers().size());
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<User> getStaffById(@PathVariable("id") UUID id) {
        return ApiResponse.ok(authService.getUserById(id), 1L);
    }

    // --- MÜŞTERİ LİSTELEME ---

    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public ApiResponse<?> getAllCustomers() {
        var customers = authService.getAllCustomers();
        return ApiResponse.ok(customers, (long) customers.size());
    }

    // --- YETKİ GEREKTİREN İŞLEMLER (Sadece "Admin") ---

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<String> deleteStaff(@PathVariable("id") UUID id) {
        authService.deleteUser(id);
        return ApiResponse.ok("Personel başarıyla silindi.", 1L);
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<Map<String, String>> inviteStaff(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String role = (String) request.get("role"); // Gelen "Admin" veya "Staff"
        boolean sendEmail = (boolean) request.getOrDefault("sendEmail", false);

        String inviteLink = invitationService.createInvitation(email, role);

        if (sendEmail) {
            emailService.sendInviteEmail(email, inviteLink);
        }

        return ApiResponse.ok(Map.of(
                "message", sendEmail ? "Davetiye e-posta ile gönderildi" : "Davetiye linki oluşturuldu",
                "link", inviteLink
        ), 1L);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<String> addStaff(@RequestBody RegisterRequest request) {
        authService.registerWithRole(request, request.getRole());
        return ApiResponse.ok("Personel başarıyla eklendi.", 1L);
    }

    /**
     * Personel bilgilerini (Ad, Soyad vb.) güncellemek için.
     */
    @PatchMapping("/update-stuff/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<LoginResponse> updateStaff(
            @PathVariable("id") UUID id,
            @RequestBody RegisterRequest request) {

        LoginResponse updatedUser = authService.updateUser(id, request);
        return ApiResponse.ok(updatedUser, 1L);
    }

    /**
     * SADECE PERSONELİN ROLÜNÜ DEĞİŞTİRMEK İÇİN (Hızlı Rol Güncelleme)
     * Body: { "role": "Admin" } veya { "role": "Staff" }
     */
    @PatchMapping("/update-role/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ApiResponse<String> updateRole(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request) {

        String newRole = request.get("role");
        authService.updateUserRole(id, newRole); // AuthService içinde bu metodu yazmalısın
        return ApiResponse.ok("Personel rolü başarıyla " + newRole + " olarak güncellendi.", 1L);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
