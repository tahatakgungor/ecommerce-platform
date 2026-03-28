package com.ecommerce.product.application;

import com.ecommerce.product.domain.Invitation;
import com.ecommerce.product.repository.InvitationRepository;
import com.ecommerce.product.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    @Transactional
    public String createInvitation(String email, String role) {
        // 1. Kullanıcı zaten sistemde var mı?
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Bu e-posta adresi zaten sisteme kayıtlı!");
        }

        // 2. YENİ EKLE: Zaten gönderilmiş ama kullanılmamış bir davetiyesi varsa eskisini silelim
        invitationRepository.findByEmail(email).ifPresent(oldInvite -> {
            if (!oldInvite.isUsed()) {
                invitationRepository.delete(oldInvite);
            }
        });

        // 3. Yeni davetiyeyi oluştur
        String token = UUID.randomUUID().toString();

        Invitation invite = new Invitation();
        invite.setEmail(email);
        invite.setToken(token);
        invite.setRole(role != null ? role : "STAFF");
        invite.setExpiryDate(LocalDateTime.now().plusHours(24));
        invite.setUsed(false);

        invitationRepository.save(invite);

        return "https://ecommerce-frontend-xryc.vercel.app/register?token=" + token;
    }

    public Invitation validateAndGetInvite(String token) {
        Invitation invite = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Geçersiz veya sahte davetiye kodu!"));

        if (invite.isUsed()) {
            throw new RuntimeException("Bu davetiye daha önce kullanılmış.");
        }

        if (invite.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Davetiyenin süresi dolmuş (24 saat geçti).");
        }

        return invite;
    }

    @Transactional
    public void markAsUsed(Invitation invite) {
        invite.setUsed(true);
        invitationRepository.save(invite);
    }
}