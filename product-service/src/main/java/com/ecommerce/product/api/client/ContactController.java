package com.ecommerce.product.api.client;

import com.ecommerce.product.application.EmailService;
import com.ecommerce.product.dto.ApiResponse;
import com.ecommerce.product.dto.ContactRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendContactMessage(
            @Valid @RequestBody ContactRequest request
    ) {
        String subject = request.getCompany() != null && !request.getCompany().isBlank()
                ? request.getCompany()
                : "Genel İletişim";
        String body = (request.getPhone() != null ? "Telefon: " + request.getPhone() + "\n" : "") +
                "\n" + request.getMessage();
        emailService.sendContactEmail(
                request.getName(),
                request.getEmail(),
                subject,
                body
        );
        ApiResponse<Void> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Mesajınız başarıyla iletildi. En kısa sürede size dönüş yapacağız.");
        return ResponseEntity.ok(response);
    }
}
