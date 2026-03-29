package com.ecommerce.product.api.client;

import com.ecommerce.product.application.CustomerService;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.auth.CustomerLoginResponse;
import com.ecommerce.product.dto.auth.CustomerSignupRequest;
import com.ecommerce.product.dto.auth.CustomerUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> signup(@RequestBody CustomerSignupRequest request) {
        CustomerLoginResponse result = customerService.signup(request);
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("Kayıt başarılı!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        CustomerLoginResponse result = customerService.login(email, password);
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("Giriş başarılı!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerUserDto> getMe(Authentication auth) {
        CustomerUserDto user = customerService.getMe(auth.getName());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/confirmEmail/{token}")
    public ResponseEntity<ApiResponse<String>> confirmEmail(@PathVariable String token) {
        // Email doğrulama sistemi henüz uygulanmadı; başarılı döner
        ApiResponse<String> response = new ApiResponse<>(true, null, null);
        response.setMessage("Email confirmed.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/forget-password")
    public ResponseEntity<ApiResponse<String>> forgetPassword(@RequestBody Map<String, String> body) {
        customerService.forgetPassword(body.get("email"));
        ApiResponse<String> response = new ApiResponse<>(true, null, null);
        response.setMessage("Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/confirm-forget-password")
    public ResponseEntity<ApiResponse<String>> confirmForgetPassword(@RequestBody Map<String, String> body) {
        customerService.confirmForgetPassword(
                body.get("token"),
                body.get("password"),
                body.get("confirmPassword")
        );
        ApiResponse<String> response = new ApiResponse<>(true, null, null);
        response.setMessage("Şifreniz başarıyla güncellendi.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        customerService.changePassword(
                auth.getName(),
                body.get("currentPassword"),
                body.get("newPassword")
        );
        ApiResponse<String> response = new ApiResponse<>(true, null, null);
        response.setMessage("Şifreniz başarıyla değiştirildi.");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-user/{id}")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> updateUser(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        CustomerLoginResponse result = customerService.updateUser(id, body);
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("Profil güncellendi.");
        return ResponseEntity.ok(response);
    }
}
