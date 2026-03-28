package com.ecommerce.product.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j // Loglama için eklendi
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendInviteEmail(String toEmail, String inviteLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // Gönderen adresi config'den alıyoruz
            message.setTo(toEmail);
            message.setSubject("Personel Paneli Davetiyesi");
            message.setText("Merhaba,\n\n" +
                    "Sisteme kayıt olmanız için davet edildiniz. " +
                    "Aşağıdaki bağlantıyı kullanarak kaydınızı tamamlayabilirsiniz:\n\n" +
                    inviteLink + "\n\n" +
                    "Not: Bu bağlantı 24 saat geçerlidir.\n" +
                    "İyi çalışmalar.");

            mailSender.send(message);
            log.info("Davet e-postası başarıyla gönderildi: {}", toEmail);
        } catch (Exception e) {
            log.error("E-posta gönderimi sırasında hata oluştu: {}", e.getMessage());
            // Önemli: Burada hata fırlatmıyoruz ki kayıt süreci kesilmesin.
            // Admin panelde zaten linki manuel kopyalayabiliyorsun.
        }
    }
}