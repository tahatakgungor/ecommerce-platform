package com.ecommerce.product.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
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
        SimpleMailMessage message = createMessage(
                toEmail,
                "E-posta Adresinizi Doğrulayın - SERRAVİT",
                "Merhaba,\n\n" +
                        "Hesabınızı aktif hale getirmek için aşağıdaki bağlantıya tıklayın:\n\n" +
                        verificationLink + "\n\n" +
                        "Bu bağlantı 24 saat geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin.\n\n" +
                        "SERRAVİT Ekibi"
        );
        sendOrThrow(message, toEmail, "Doğrulama e-postası");
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = createMessage(
                toEmail,
                "Şifre Sıfırlama",
                "Merhaba,\n\n" +
                        "Şifrenizi sıfırlamak için aşağıdaki token'ı kullanın:\n\n" +
                        resetToken + "\n\n" +
                        "Not: Bu token 24 saat geçerlidir.\n" +
                        "Eğer bu isteği siz yapmadıysanız bu e-postayı görmezden gelin."
        );
        sendOrThrow(message, toEmail, "Şifre sıfırlama e-postası");
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String messageBody) {
        SimpleMailMessage message = createMessage(
                this.fromEmail,
                "[İletişim Formu] " + subject,
                "Gönderen: " + fromName + "\n" +
                        "E-posta: " + fromEmail + "\n\n" +
                        "Mesaj:\n" + messageBody
        );
        message.setReplyTo(fromEmail);
        sendOrThrow(message, fromEmail, "İletişim formu e-postası");
    }

    public void sendInviteEmail(String toEmail, String inviteLink) {
        SimpleMailMessage message = createMessage(
                toEmail,
                "Personel Paneli Davetiyesi",
                "Merhaba,\n\n" +
                        "Sisteme kayıt olmanız için davet edildiniz. " +
                        "Aşağıdaki bağlantıyı kullanarak kaydınızı tamamlayabilirsiniz:\n\n" +
                        inviteLink + "\n\n" +
                        "Not: Bu bağlantı 24 saat geçerlidir.\n" +
                        "İyi çalışmalar."
        );
        sendOrThrow(message, toEmail, "Davet e-postası");
    }

    private SimpleMailMessage createMessage(String toEmail, String subject, String text) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME tanımlı değil. E-posta gönderimi yapılamıyor.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    private void sendOrThrow(SimpleMailMessage message, String targetEmail, String emailType) {
        try {
            mailSender.send(message);
            log.info("{} başarıyla gönderildi: {}", emailType, targetEmail);
        } catch (MailException e) {
            log.error("{} gönderilemedi: {}", emailType, e.getMessage(), e);
            throw new MailSendException(emailType + " gönderilemedi. Mail ayarlarını kontrol edin.");
        }
    }
}
