package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByToken(String token);

    // YENİ EKLE: Aynı maile sahip eski davetiyeleri kontrol etmek için
    Optional<Invitation> findByEmail(String email);
}