package com.sudh.accord.repository;

import com.sudh.accord.entity.PasswordResetToken;
import com.sudh.accord.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    Optional<PasswordResetToken> findByResetTokenHashAndUsedFalse(String resetTokenHash);

    // Only one live OTP per user at a time — a fresh forgot-password request
    // invalidates any earlier unused one.
    void deleteAllByUserAndUsedFalse(User user);
}