package com.sudh.accord.service;

import com.sudh.accord.dto.ForgotPasswordRequest;
import com.sudh.accord.dto.ResetPasswordRequest;
import com.sudh.accord.dto.VerifyOtpRequest;
import com.sudh.accord.dto.VerifyOtpResponse;
import com.sudh.accord.entity.PasswordResetToken;
import com.sudh.accord.entity.RefreshToken;
import com.sudh.accord.entity.User;
import com.sudh.accord.repository.PasswordResetTokenRepository;
import com.sudh.accord.repository.RefreshTokenRepository;
import com.sudh.accord.repository.UserRepository;
import com.sudh.accord.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PasswordResetService {
    private static final int OTP_TTL_MINUTES = 10;
    private static final int RESET_TOKEN_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                JwtUtil jwtUtil,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Always succeeds from the caller's perspective, whether or not the email
    // is registered — avoids leaking which emails have accounts.
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        Optional<User> userOpt = userRepository.findByEmail(req.email());
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        // Only one live OTP per user at a time.
        passwordResetTokenRepository.deleteAllByUserAndUsedFalse(user);

        String otp = generateOtp();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        // hashRefreshToken is just a generic SHA-256 hash — reused here for
        // the OTP, same as it is for refresh tokens.
        token.setOtpHash(jwtUtil.hashRefreshToken(otp));
        token.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    // Verifies the OTP and, on success, issues a short-lived opaque reset
    // token — a separate secret from the OTP so the OTP itself is spent the
    // moment it's checked and can't be replayed against reset-password.
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid or expired code"));

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("Invalid or expired code"));

        if (token.getOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Invalid or expired code");

        if (!jwtUtil.hashRefreshToken(req.otp()).equals(token.getOtpHash()))
            throw new RuntimeException("Invalid or expired code");

        // generateRefreshToken is just a generic secure-random opaque token —
        // reused here for the reset token, same as it is for refresh tokens.
        String rawResetToken = jwtUtil.generateRefreshToken();
        token.setResetTokenHash(jwtUtil.hashRefreshToken(rawResetToken));
        token.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        passwordResetTokenRepository.save(token);

        return new VerifyOtpResponse(rawResetToken);
    }

    // @Transactional keeps the session open for token.getUser() (lazy) —
    // same reasoning as AuthService.refresh.
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String hash = jwtUtil.hashRefreshToken(req.resetToken());

        PasswordResetToken token = passwordResetTokenRepository.findByResetTokenHashAndUsedFalse(hash)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (token.getResetTokenExpiresAt() == null || token.getResetTokenExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Invalid or expired reset token");

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Password just changed — revoke every other active session, same
        // cascade-revocation used for refresh-token reuse detection.
        List<RefreshToken> active = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        active.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(active);
    }

    private String generateOtp() {
        int code = secureRandom.nextInt(1_000_000); // 0..999999
        return String.format("%06d", code);
    }
}