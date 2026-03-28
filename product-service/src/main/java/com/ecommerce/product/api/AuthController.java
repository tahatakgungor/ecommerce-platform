package com.ecommerce.product.api;

import com.ecommerce.product.application.AuthService;
import com.ecommerce.product.application.InvitationService;
import com.ecommerce.product.domain.Invitation;
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
@CrossOrigin(origins = {"http://localhost:3000", "https://ecommerce-frontend-xryc.vercel.app"})
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;

    // AuthController.java içine ekle

    @GetMapping("/all")
    public ApiResponse<?> getAllStaff() {
        // AuthService üzerinden tüm kullanıcıları çekiyoruz
        // (AuthService içinde getAllUsers metodunun olduğunu varsayıyorum)
        return ApiResponse.ok(authService.getAllUsers(), 0L);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/invite")
    public ApiResponse<Map<String, String>> inviteStaff(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String role = request.get("role");
        String inviteLink = invitationService.createInvitation(email, role);

        // Map dönüyoruz, total 1L
        return ApiResponse.ok(Map.of("message", "Davetiye oluşturuldu", "link", inviteLink), 1L);
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request, @RequestParam String token) {
        Invitation invite = invitationService.validateAndGetInvite(token);

        if (!invite.getEmail().equalsIgnoreCase(request.getEmail())) {
            // Hata fırlatıyoruz, GlobalExceptionHandler bunu ApiResponse.error'a çevirecek
            throw new RuntimeException("Sadece davet edildiğiniz e-posta ile kayıt olabilirsiniz.");
        }

        authService.registerWithRole(request, invite.getRole());
        invitationService.markAsUsed(invite);

        // Başarılı kayıt mesajı
        return ApiResponse.ok("Kayıt başarıyla tamamlandı. Artık giriş yapabilirsiniz.", 1L);
    }
}