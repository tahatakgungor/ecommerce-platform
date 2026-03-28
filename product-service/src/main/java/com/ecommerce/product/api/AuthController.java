package com.ecommerce.product.api;

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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        // Gelen isteği logla (Konsolda ne geldiğini gör)
        System.out.println("Giriş isteği: " + request.email());
        LoginResponse response = authService.login(request);
        return ApiResponse.ok(response, 1L);
    }

    @GetMapping("/all")
    public ApiResponse<?> getAllStaff() {
        return ApiResponse.ok(authService.getAllUsers(), (long) authService.getAllUsers().size());
    }

    @GetMapping("/get/{id}")
    public ApiResponse<User> getStaffById(@PathVariable("id") java.util.UUID id) {
        return ApiResponse.ok(authService.getUserById(id), 1L);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteStaff(@PathVariable("id") java.util.UUID id) {
        authService.deleteUser(id);
        return ApiResponse.ok("Personel başarıyla silindi.", 1L);
    }

    @PostMapping("/invite")
    public ApiResponse<Map<String, String>> inviteStaff(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String role = (String) request.get("role");
        boolean sendEmail = (boolean) request.getOrDefault("sendEmail", false);

        String inviteLink = invitationService.createInvitation(email, role);

        if (sendEmail) {
            emailService.sendInviteEmail(email, inviteLink);
        }

        return ApiResponse.ok(Map.of(
                "message", sendEmail ? "Davetiye gönderildi" : "Davetiye oluşturuldu",
                "link", inviteLink
        ), 1L);
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

    // AuthController.java içine eklenecek endpoint:

    @PatchMapping("/update-stuff/{id}")
    public ApiResponse<LoginResponse> updateStaff(
            @PathVariable("id") java.util.UUID id,
            @RequestBody RegisterRequest request) {

        LoginResponse updatedUser = authService.updateUser(id, request);
        return ApiResponse.ok(updatedUser, 1L);
    }
}