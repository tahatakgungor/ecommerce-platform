package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.mail.resend.from-email:}")
    private String fromEmail;

    @Value("${app.mail.resend.reply-to:}")
    private String replyToEmail;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        sendEmail(
                toEmail,
                "E-posta Adresinizi Doğrulayın - SERRAVİT",
                "Merhaba,\n\n" +
                        "Hesabınızı aktif hale getirmek için aşağıdaki bağlantıya tıklayın:\n\n" +
                        verificationLink + "\n\n" +
                        "Bu bağlantı 24 saat geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin.\n\n" +
                        "SERRAVİT Ekibi",
                null, // html
                null, // replyTo
                "Doğrulama e-postası"
        );
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        sendEmail(
                toEmail,
                "Şifre Sıfırlama",
                "Merhaba,\n\n" +
                        "Şifrenizi sıfırlamak için aşağıdaki token'ı kullanın:\n\n" +
                        resetToken + "\n\n" +
                        "Not: Bu token 24 saat geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin.",
                null,
                null,
                "Şifre sıfırlama e-postası"
        );
    }

    public void sendPasswordChangeVerificationEmail(String toEmail, String verificationCode) {
        sendEmail(
                toEmail,
                "Şifre Değiştirme Doğrulama Kodu",
                "Merhaba,\n\n" +
                        "Şifre değiştirme işlemini onaylamak için doğrulama kodunuz:\n\n" +
                        verificationCode + "\n\n" +
                        "Not: Bu kod 10 dakika geçerlidir.\n" +
                        "Eğer bu işlemi siz başlatmadıysanız hesabınızın şifresini hemen değiştirin.\n\n" +
                        "SERRAVİT Ekibi",
                null,
                null,
                "Şifre değiştirme doğrulama e-postası"
        );
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String messageBody) {
        sendEmail(
                this.fromEmail,
                "[İletişim Formu] " + subject,
                "Gönderen: " + fromName + "\n" +
                        "E-posta: " + fromEmail + "\n\n" +
                        "Mesaj:\n" + messageBody,
                null,
                fromEmail,
                "İletişim formu e-postası"
        );
    }

    public void sendInviteEmail(String toEmail, String inviteLink) {
        sendEmail(
                toEmail,
                "Personel Paneli Davetiyesi",
                "Merhaba,\n\n" +
                        "Sisteme kayıt olmanız için davet edildiniz. " +
                        "Aşağıdaki bağlantıyı kullanarak kaydınızı tamamlayabilirsiniz:\n\n" +
                        inviteLink + "\n\n" +
                        "Not: Bu bağlantı 24 saat geçerlidir.\n" +
                        "İyi çalışmalar.",
                null,
                null,
                "Davet e-postası"
        );
    }

    // --- Sipariş Bildirimleri ---

    public void sendOrderConfirmation(Order order) {
        String to = order.getEmail() != null ? order.getEmail() : order.getGuestEmail();
        if (to == null || to.isBlank()) return;

        try {
            Context context = new Context();
            context.setVariable("order", order);
            
            // Deserialize cart for template iteration
            List<Map<String, Object>> cartItems = deserializeCart(order.getCart());
            context.setVariable("cartItems", cartItems);
            
            String htmlContent = templateEngine.process("order-placed", context);
            
            sendEmail(to, "Siparişiniz Alındı - SERRAVİT", null, htmlContent, null, "Sipariş onayı e-postası");
        } catch (Exception e) {
            log.error("Sipariş onayı e-postası gönderilemedi: {}", e.getMessage());
        }
    }

    public void sendShippingUpdate(Order order) {
        String to = order.getEmail() != null ? order.getEmail() : order.getGuestEmail();
        if (to == null || to.isBlank()) return;

        try {
            Context context = new Context();
            context.setVariable("order", order);
            
            // Deserialize cart for template iteration
            List<Map<String, Object>> cartItems = deserializeCart(order.getCart());
            context.setVariable("cartItems", cartItems);
            
            String htmlContent = templateEngine.process("order-shipped", context);
            
            sendEmail(to, "Siparişiniz Kargoya Verildi - SERRAVİT", null, htmlContent, null, "Kargo takip e-postası");
        } catch (Exception e) {
            log.error("Kargo takip e-postası gönderilemedi: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> deserializeCart(String cartJson) {
        if (cartJson == null || cartJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(cartJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Cart JSON deserialization failed for email: {}", e.getMessage());
            return List.of();
        }
    }

    private void sendEmail(String toEmail, String subject, String text, String html, String explicitReplyTo, String emailType) {
        validateConfig();

        String effectiveReplyTo = explicitReplyTo;
        if ((effectiveReplyTo == null || effectiveReplyTo.isBlank()) && replyToEmail != null && !replyToEmail.isBlank()) {
            effectiveReplyTo = replyToEmail;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromEmail);
        payload.put("to", List.of(toEmail));
        payload.put("subject", subject);
        if (text != null) payload.put("text", text);
        if (html != null) payload.put("html", html);
        if (effectiveReplyTo != null && !effectiveReplyTo.isBlank()) {
            payload.put("reply_to", List.of(effectiveReplyTo));
        }

        String requestBody = toJson(payload, emailType);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ResendSendEmailResponse success = objectMapper.readValue(response.body(), ResendSendEmailResponse.class);
                log.info("{} başarıyla gönderildi: {} | id={}", emailType, toEmail, success.id());
                return;
            }

            ResendErrorResponse error = parseErrorResponse(response.body());
            log.error("{} gönderilemedi. status={} error={}", emailType, response.statusCode(), error.message());
        } catch (IOException e) {
            log.error("{} gönderilemedi: {}", emailType, e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} gönderilemedi: {}", emailType, e.getMessage(), e);
        }
    }

    private void validateConfig() {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY tanımlı değil. E-posta gönderimi yapılamıyor.");
            return;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("RESEND_FROM_EMAIL tanımlı değil. E-posta gönderimi yapılamıyor.");
            return;
        }
    }

    private String toJson(Map<String, Object> payload, String emailType) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("{} payload JSON'a çevrilemedi: {}", emailType, e.getMessage(), e);
            throw new RuntimeException(emailType + " gönderilemedi. İstek verisi hazırlanamadı.");
        }
    }

    private ResendErrorResponse parseErrorResponse(String body) {
        try {
            return objectMapper.readValue(body, ResendErrorResponse.class);
        } catch (Exception ignored) {
            return new ResendErrorResponse("Resend API hatası", body);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResendSendEmailResponse(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResendErrorResponse(
            @JsonProperty("name") String name,
            @JsonProperty("message") String message
    ) {
    }
}
