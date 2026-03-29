package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.AuthService;
import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.application.InvitationService;
import com.ecommerce.product.domain.Invitation;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.auth.LoginRequest;
import com.ecommerce.product.dto.auth.LoginResponse;
import com.ecommerce.product.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final EmailService emailService;

    // --- GİRİŞ VE KAYIT ---

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("Giriş isteği: " + request.email());
        LoginResponse response = authService.login(request);
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
    public ApiResponse<?> getAllStaff() {
        return ApiResponse.ok(authService.getAllUsers(), (long) authService.getAllUsers().size());
    }

    @GetMapping("/get/{id}")
    public ApiResponse<User> getStaffById(@PathVariable("id") UUID id) {
        return ApiResponse.ok(authService.getUserById(id), 1L);
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
}