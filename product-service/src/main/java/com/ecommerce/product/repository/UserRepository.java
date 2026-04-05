package com.ecommerce.product.repository;

import com.ecommerce.product.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    Optional<User> findByPasswordChangeVerificationCodeHash(String passwordChangeVerificationCodeHash);
    Optional<User> findByEmailVerificationToken(String emailVerificationToken);

    List<User> findByRoleIn(List<String> roles);
    List<User> findByRole(String role);
}
