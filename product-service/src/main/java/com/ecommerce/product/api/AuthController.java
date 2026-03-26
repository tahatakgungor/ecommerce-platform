package com.ecommerce.product.api;

import com.ecommerce.product.application.AuthService;
import com.ecommerce.product.dto.LoginRequest;
import com.ecommerce.product.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin") // Burayı Harri'nin istediği gibi yaptık
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}