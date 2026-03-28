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
// GÜNCELLEME: Railway ve Vercel adreslerini ekledik, CORS hatalarını önlemek için tüm metodlara izin verdik.
@CrossOrigin(origins = {
        "http://localhost:3000",
        "https://ecommerce-frontend-xryc.vercel.app"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PATCH})
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;

    // GÜNCELLEME: Staff listesinin boş görünmemesi için GET metodunu ekledik.
    @GetMapping("/all")
    public ApiResponse<?> getAllStaff() {
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

        return ApiResponse.ok(Map.of("message", "Davetiye oluşturuldu", "link", inviteLink), 1L);
    }

    // GÜNCELLEME: "Ensure that the compiler uses the '-parameters' flag" hatasını
    // çözmek için @RequestParam içine açıkça ("token") yazdık.
    @PostMapping("/register")
    public ApiResponse<String> register(
            @RequestBody RegisterRequest request,
            @RequestParam("token") String token) { // <-- Burası kritik!

        Invitation invite = invitationService.validateAndGetInvite(token);

        if (!invite.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Sadece davet edildiğiniz e-posta ile kayıt olabilirsiniz.");
        }

        authService.registerWithRole(request, invite.getRole());
        invitationService.markAsUsed(invite);

        return ApiResponse.ok("Kayıt başarıyla tamamlandı. Artık giriş yapabilirsiniz.", 1L);
    }
}