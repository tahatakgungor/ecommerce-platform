package com.ecommerce.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendInviteEmail(String toEmail, String inviteLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Personel Paneli Davetiyesi");
        message.setText("Merhaba, sisteme kayıt olmanız için davet edildiniz. \n\n" +
                "Kayıt bağlantınız: " + inviteLink + "\n\n" +
                "Bu bağlantı 24 saat geçerlidir.");
        mailSender.send(message);
    }
}