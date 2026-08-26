package com.sudh.accord.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Store only a hash of the OTP (e.g. SHA-256), never the raw value —
    // same reasoning as RefreshToken.tokenHash.
    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime otpExpiresAt;

    // Null until the OTP is verified. Set by verifyOtp() once the code
    // checks out, so reset-password can be reached only via a verified OTP.
    private String resetTokenHash;

    private LocalDateTime resetTokenExpiresAt;

    // Marks the row fully spent once the password has actually been reset —
    // distinct from OTP verification, so a verified-but-unused reset token
    // can still be looked up by resetTokenHash.
    @Column(nullable = false)
    private boolean used;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(UUID id,
                              User user,
                              String otpHash,
                              LocalDateTime otpExpiresAt,
                              String resetTokenHash,
                              LocalDateTime resetTokenExpiresAt,
                              boolean used,
                              LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.otpHash = otpHash;
        this.otpExpiresAt = otpExpiresAt;
        this.resetTokenHash = resetTokenHash;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public LocalDateTime getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PasswordResetToken that = (PasswordResetToken) o;
        return used == that.used && Objects.equals(id, that.id) && Objects.equals(user, that.user)
                && Objects.equals(otpHash, that.otpHash) && Objects.equals(otpExpiresAt, that.otpExpiresAt)
                && Objects.equals(resetTokenHash, that.resetTokenHash)
                && Objects.equals(resetTokenExpiresAt, that.resetTokenExpiresAt)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, otpHash, otpExpiresAt, resetTokenHash, resetTokenExpiresAt, used, createdAt);
    }
}