package com.ecommerce.product.api.client;

import com.ecommerce.product.application.CustomerService;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.auth.CustomerLoginResponse;
import com.ecommerce.product.dto.auth.CustomerSignupRequest;
import com.ecommerce.product.dto.auth.CustomerUserDto;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> login(
            @RequestBody Map<String, String> body,
            HttpServletResponse httpResponse) {
        String email = body.get("email");
        String password = body.get("password");
        CustomerLoginResponse result = customerService.login(email, password);
        // httpOnly cookie: token JS tarafından okunamaz
        setAuthCookie(httpResponse, result.token());
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("Giriş başarılı!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpResponse) {
        clearAuthCookie(httpResponse);
        ApiResponse<Void> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Çıkış yapıldı.");
        return ResponseEntity.ok(response);
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .secure(true)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .secure(true)
                .build();
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerUserDto> getMe(Authentication auth) {
        CustomerUserDto user = customerService.getMe(auth.getName());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/confirmEmail/{token}")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> confirmEmail(
            @PathVariable String token,
            HttpServletResponse httpResponse) {
        CustomerLoginResponse result = customerService.confirmEmail(token);
        // Standart access_token cookie'sini set et (JwtAuthenticationFilter ile uyumlu)
        setAuthCookie(httpResponse, result.token());
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("E-posta adresiniz doğrulandı. Giriş yapılıyor...");
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

    @PutMapping("/update-user")
    public ResponseEntity<ApiResponse<CustomerLoginResponse>> updateUser(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        CustomerLoginResponse result = customerService.updateUser(auth.getName(), body);
        ApiResponse<CustomerLoginResponse> response = new ApiResponse<>(true, result, null);
        response.setMessage("Profil güncellendi.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/newsletter")
    public ResponseEntity<ApiResponse<String>> subscribeNewsletter(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        customerService.subscribeNewsletter(email);
        ApiResponse<String> response = new ApiResponse<>(true, null, null);
        response.setMessage("Bültenimize başarıyla abone oldunuz!");
        return ResponseEntity.ok(response);
    }
}
