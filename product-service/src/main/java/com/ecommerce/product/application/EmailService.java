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

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("E-posta Adresinizi Doğrulayın - SERRAVİT");
            message.setText("Merhaba,\n\n" +
                    "Hesabınızı aktif hale getirmek için aşağıdaki bağlantıya tıklayın:\n\n" +
                    verificationLink + "\n\n" +
                    "Bu bağlantı 24 saat geçerlidir.\n" +
                    "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin.\n\n" +
                    "SERRAVİT Ekibi");
            mailSender.send(message);
            log.info("Doğrulama e-postası gönderildi: {}", toEmail);
        } catch (Exception e) {
            log.error("Doğrulama e-postası gönderilemedi: {}", e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Şifre Sıfırlama");
            message.setText("Merhaba,\n\n" +
                    "Şifrenizi sıfırlamak için aşağıdaki token'ı kullanın:\n\n" +
                    resetToken + "\n\n" +
                    "Not: Bu token 24 saat geçerlidir.\n" +
                    "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin.");
            mailSender.send(message);
            log.info("Şifre sıfırlama e-postası gönderildi: {}", toEmail);
        } catch (Exception e) {
            log.error("E-posta gönderimi sırasında hata oluştu: {}", e.getMessage());
        }
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String messageBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(this.fromEmail);
            message.setTo(this.fromEmail); // İletişim formu kendi e-postamıza gelir
            message.setReplyTo(fromEmail);
            message.setSubject("[İletişim Formu] " + subject);
            message.setText(
                    "Gönderen: " + fromName + "\n" +
                    "E-posta: " + fromEmail + "\n\n" +
                    "Mesaj:\n" + messageBody
            );
            mailSender.send(message);
            log.info("İletişim formu e-postası gönderildi. Gönderen: {}", fromEmail);
        } catch (Exception e) {
            log.error("İletişim formu e-postası gönderilemedi: {}", e.getMessage());
        }
    }

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