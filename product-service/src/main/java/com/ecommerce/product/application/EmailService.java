package com.ecommerce.product.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final ObjectMapper objectMapper;

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
                null,
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
                "Davet e-postası"
        );
    }

    private void sendEmail(String toEmail, String subject, String text, String explicitReplyTo, String emailType) {
        validateConfig();

        String effectiveReplyTo = explicitReplyTo;
        if ((effectiveReplyTo == null || effectiveReplyTo.isBlank()) && replyToEmail != null && !replyToEmail.isBlank()) {
            effectiveReplyTo = replyToEmail;
        }

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", List.of(toEmail),
                "subject", subject,
                "text", text,
                "reply_to", effectiveReplyTo == null || effectiveReplyTo.isBlank() ? List.of() : List.of(effectiveReplyTo)
        );

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
            throw new RuntimeException(emailType + " gönderilemedi: " + error.message());
        } catch (IOException e) {
            log.error("{} gönderilemedi: {}", emailType, e.getMessage(), e);
            throw new RuntimeException(emailType + " gönderilemedi. Resend yanıtı okunamadı.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{} gönderilemedi: {}", emailType, e.getMessage(), e);
            throw new RuntimeException(emailType + " gönderilemedi. İşlem kesintiye uğradı.");
        }
    }

    private void validateConfig() {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY tanımlı değil. E-posta gönderimi yapılamıyor.");
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("RESEND_FROM_EMAIL tanımlı değil. E-posta gönderimi yapılamıyor.");
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
