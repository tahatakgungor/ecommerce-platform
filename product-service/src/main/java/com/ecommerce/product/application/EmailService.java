package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.OrderReturn;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    @Value("${app.mail.resend.max-attempts:2}")
    private int resendMaxAttempts;

    @Value("${app.mail.order-alert-recipients:}")
    private String orderAlertRecipients;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.admin-frontend-url:http://localhost:3001}")
    private String adminFrontendUrl;

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

    public void sendContactEmail(String fromName, String fromEmail, String subject, String messageBody, String supportEmail) {
        List<String> recipients = parseAlertRecipients(supportEmail);
        for (String to : recipients) {
            sendEmail(
                    to,
                    "[İletişim Formu] " + subject,
                    "Gönderen: " + fromName + "\n" +
                            "E-posta: " + fromEmail + "\n\n" +
                            "Mesaj:\n" + messageBody,
                    null,
                    fromEmail,
                    "İletişim formu e-postası"
            );
        }
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
        String to = resolveRecipientEmail(order);
        if (to == null || to.isBlank()) return;

        try {
            log.info("[Email] Preparing order confirmation mail. invoice={}, recipient={}", order.getInvoice(), to);
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("trackingUrl", buildTrackingUrl(order));
            context.setVariable("orderViewUrl", buildOrderViewUrl(order));
            context.setVariable("registerUrl", buildRegisterUrl(order));
            
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
        String to = resolveRecipientEmail(order);
        if (to == null || to.isBlank()) {
            log.warn("[Email] Shipping update mail skipped: recipient is empty. invoice={}", order.getInvoice());
            return;
        }

        try {
            log.info("[Email] Preparing shipping update mail. invoice={}, recipient={}, carrier={}, tracking={}",
                    order.getInvoice(), to, order.getShippingCarrier(), order.getTrackingNumber());
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

    public void sendDeliveryConfirmation(Order order) {
        String to = resolveRecipientEmail(order);
        if (to == null || to.isBlank()) return;

        try {
            log.info("[Email] Preparing delivery confirmation mail. invoice={}, recipient={}", order.getInvoice(), to);
            Context context = new Context();
            context.setVariable("order", order);
            context.setVariable("orderViewUrl", buildOrderViewUrl(order));
            context.setVariable("storeFeedbackUrl", frontendUrl + "/contact");

            String htmlContent = templateEngine.process("order-delivered", context);
            sendEmail(to, "Siparişiniz Teslim Edildi - SERRAVİT", null, htmlContent, null, "Teslim bildirimi e-postası");
        } catch (Exception e) {
            log.error("Teslim bildirimi e-postası gönderilemedi: {}", e.getMessage());
        }
    }

    private String resolveRecipientEmail(Order order) {
        if (order == null) return null;
        if (order.getEmail() != null && !order.getEmail().isBlank()) {
            return order.getEmail().trim();
        }
        if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) {
            return order.getGuestEmail().trim();
        }
        return null;
    }

    private String buildOrderViewUrl(Order order) {
        if (order == null || order.getId() == null) {
            return frontendUrl + "/order-lookup";
        }
        String base = frontendUrl + "/order/" + order.getId();
        String invoice = order.getInvoice();
        String email = resolveRecipientEmail(order);

        if (invoice == null || invoice.isBlank() || email == null || email.isBlank()) {
            return base;
        }
        return base
                + "?invoice=" + URLEncoder.encode(invoice, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    private String buildRegisterUrl(Order order) {
        String email = resolveRecipientEmail(order);
        if (email == null || email.isBlank()) {
            return frontendUrl + "/register";
        }
        return frontendUrl + "/register?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    private String buildTrackingUrl(Order order) {
        String carrier = order != null && order.getShippingCarrier() != null
                ? order.getShippingCarrier().trim().toLowerCase(Locale.ROOT)
                : "";
        String trackingNumber = order != null && order.getTrackingNumber() != null
                ? order.getTrackingNumber().trim()
                : "";
        String encoded = URLEncoder.encode(trackingNumber, StandardCharsets.UTF_8);

        if (carrier.contains("aras")) return "https://kargotakip.araskargo.com.tr/mainpage.aspx?code=" + encoded;
        if (carrier.contains("yurt")) return "https://www.yurticikargo.com/tr/online-islemler/gonderi-sorgula?code=" + encoded;
        if (carrier.contains("mng")) return "https://www.mngkargo.com.tr/gonderi-sorgula?code=" + encoded;
        if (carrier.contains("ptt")) return "https://gonderitakip.ptt.gov.tr/Track/Verify?q=" + encoded;
        if (carrier.contains("sürat") || carrier.contains("surat")) return "https://www.suratkargo.com.tr/KargoTakip/?code=" + encoded;
        if (carrier.contains("ups")) return "https://www.ups.com/track?tracknum=" + encoded;
        if (carrier.contains("dhl")) return "https://www.dhl.com/tr-tr/home/tracking/tracking-express.html?submit=1&tracking-id=" + encoded;

        return "https://www.google.com/search?q=" + URLEncoder.encode(trackingNumber + " kargo takip", StandardCharsets.UTF_8);
    }

    // --- İade Bildirimleri ---

    public void sendReturnRequestConfirmation(OrderReturn orderReturn, Order order) {
        String to = orderReturn.getUserEmail();
        if (to == null || to.isBlank()) return;
        try {
            log.info("[Email] Preparing return request confirmation. returnId={}, recipient={}", orderReturn.getId(), to);
            Context context = new Context();
            context.setVariable("orderReturn", toReturnMap(orderReturn));
            context.setVariable("order", order);
            context.setVariable("orderViewUrl", buildOrderViewUrl(order));
            String html = templateEngine.process("return-request-customer", context);
            sendEmail(to, "İade Talebiniz Alındı - SERRAVİT", null, html, null, "İade talebi onay e-postası");
        } catch (Exception e) {
            log.warn("[Email] İade talebi onay e-postası gönderilemedi (returnId={}): {}", orderReturn.getId(), e.getMessage());
        }
    }

    public void sendReturnStatusUpdate(OrderReturn orderReturn, Order order) {
        String to = orderReturn.getUserEmail();
        if (to == null || to.isBlank()) return;
        try {
            log.info("[Email] Preparing return status update. returnId={}, status={}, recipient={}", orderReturn.getId(), orderReturn.getStatus(), to);
            Context context = new Context();
            context.setVariable("orderReturn", toReturnMap(orderReturn));
            context.setVariable("order", order);
            context.setVariable("newStatus", orderReturn.getStatus().name());
            context.setVariable("orderViewUrl", buildOrderViewUrl(order));
            String html = templateEngine.process("return-status-update", context);
            String subject = buildReturnStatusSubject(orderReturn.getStatus().name());
            sendEmail(to, subject, null, html, null, "İade durum güncelleme e-postası");
        } catch (Exception e) {
            log.warn("[Email] İade durum güncelleme e-postası gönderilemedi (returnId={}): {}", orderReturn.getId(), e.getMessage());
        }
    }

    public void sendReturnAdminAlert(OrderReturn orderReturn, Order order) {
        List<String> recipients = parseAlertRecipients(null);

        for (String to : recipients) {
            try {
                Context context = new Context();
                context.setVariable("orderReturn", toReturnMap(orderReturn));
                context.setVariable("order", order);
                context.setVariable("adminReturnsUrl", adminFrontendUrl + "/returns");
                String html = templateEngine.process("return-admin-alert", context);
                sendEmail(to, "Yeni İade Talebi - " + (order.getInvoice() != null ? order.getInvoice() : "SERRAVIT"),
                        null, html, null, "Yeni iade talebi admin bildirimi");
            } catch (Exception e) {
                log.warn("[Email] Yeni iade talebi admin bildirimi gönderilemedi ({}): {}", to, e.getMessage());
            }
        }
    }

    private Map<String, Object> toReturnMap(OrderReturn r) {
        Map<String, Object> m = new HashMap<>();
        m.put("_id", r.getId() != null ? r.getId().toString() : "");
        m.put("orderId", r.getOrderId() != null ? r.getOrderId().toString() : "");
        m.put("userEmail", r.getUserEmail());
        m.put("reason", r.getReason());
        m.put("customerNote", r.getCustomerNote());
        m.put("adminNote", r.getAdminNote());
        m.put("status", r.getStatus() != null ? r.getStatus().name() : "");
        m.put("createdAt", formatDateTime(r.getCreatedAt()));
        m.put("updatedAt", formatDateTime(r.getUpdatedAt()));
        return m;
    }

    private static final DateTimeFormatter TR_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", new java.util.Locale("tr", "TR"));

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        try {
            return dt.format(TR_DATE_FORMAT);
        } catch (Exception e) {
            return dt.toString();
        }
    }

    private String buildReturnStatusSubject(String status) {
        return switch (status) {
            case "APPROVED"  -> "İade Talebiniz Onaylandı - SERRAVİT";
            case "REJECTED"  -> "İade Talebiniz Hakkında Bilgilendirme - SERRAVİT";
            case "RECEIVED"  -> "İade Ürününüz Alındı - SERRAVİT";
            case "REFUNDED"  -> "Para İadeniz Gerçekleştirildi - SERRAVİT";
            default          -> "İade Durumunuz Güncellendi - SERRAVİT";
        };
    }

    public void sendNewOrderAlertToAdmins(Order order) {
        List<String> recipients = parseAlertRecipients(null);

        for (String to : recipients) {
            try {
                Context context = new Context();
                context.setVariable("order", order);
                String htmlContent = templateEngine.process("order-admin-alert", context);
                sendEmail(
                        to,
                        "Yeni Sipariş Alındı - " + (order.getInvoice() != null ? order.getInvoice() : "SERRAVIT"),
                        null,
                        htmlContent,
                        null,
                        "Yeni sipariş admin bildirimi"
                );
            } catch (Exception e) {
                log.error("Yeni sipariş admin bildirimi gönderilemedi ({}): {}", to, e.getMessage());
            }
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

    private List<String> parseAlertRecipients(String preferredSupportEmail) {
        java.util.LinkedHashSet<String> recipients = new java.util.LinkedHashSet<>();
        if (preferredSupportEmail != null && !preferredSupportEmail.isBlank()) {
            recipients.add(preferredSupportEmail.trim());
        }
        Arrays.stream((orderAlertRecipients == null ? "" : orderAlertRecipients).split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(recipients::add);

        String fallback = (replyToEmail != null && !replyToEmail.isBlank()) ? replyToEmail : fromEmail;
        if ((recipients.isEmpty()) && fallback != null && !fallback.isBlank()) {
            recipients.add(fallback.trim());
        }
        return recipients.stream().toList();
    }

    private void sendEmail(String toEmail, String subject, String text, String html, String explicitReplyTo, String emailType) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("{} gönderimi atlandı: alıcı e-posta boş.", emailType);
            return;
        }
        if (!isConfigValid()) {
            log.warn("{} gönderimi atlandı: mail konfigürasyonu eksik. recipient={}", emailType, toEmail);
            return;
        }

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

        int maxAttempts = Math.max(1, resendMaxAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    ResendSendEmailResponse success = objectMapper.readValue(response.body(), ResendSendEmailResponse.class);
                    log.info("{} başarıyla gönderildi: {} | id={} | attempt={}/{}", emailType, toEmail, success.id(), attempt, maxAttempts);
                    return;
                }

                ResendErrorResponse error = parseErrorResponse(response.body());
                if (response.statusCode() >= 500 && attempt < maxAttempts) {
                    log.warn("{} geçici hata aldı. status={} error={} attempt={}/{}. Yeniden denenecek.",
                            emailType, response.statusCode(), error.message(), attempt, maxAttempts);
                    continue;
                }
                log.error("{} gönderilemedi. status={} error={} attempt={}/{}",
                        emailType, response.statusCode(), error.message(), attempt, maxAttempts);
                return;
            } catch (IOException e) {
                if (attempt < maxAttempts) {
                    log.warn("{} gönderimi IO hatası aldı (attempt={}/{}): {}. Yeniden denenecek.",
                            emailType, attempt, maxAttempts, e.getMessage());
                    continue;
                }
                log.error("{} gönderilemedi (IO): {}", emailType, e.getMessage(), e);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("{} gönderilemedi (interrupt): {}", emailType, e.getMessage(), e);
                return;
            }
        }
    }

    private boolean isConfigValid() {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY tanımlı değil. E-posta gönderimi yapılamıyor.");
            return false;
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("RESEND_FROM_EMAIL tanımlı değil. E-posta gönderimi yapılamıyor.");
            return false;
        }
        return true;
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
