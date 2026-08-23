package com.sudh.accord.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final long ACCESS_TOKEN_TTL_MS = 1000L * 60;          // 30 min
    private static final int REFRESH_TOKEN_BYTES = 64;                        // 512-bit opaque token
    public static final int REFRESH_TOKEN_TTL_DAYS = 30;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Short-lived, sent on every request. Carries identity claims.
    public String generateAccessToken(UUID userId, String email){
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
                .signWith(getSigningKey())
                .compact();
    }

    // Long-lived, opaque (not a JWT — carries no claims, can't be inspected/forged
    // client-side). Caller is responsible for persisting its hash + a ~30-day
    // expiresAt via RefreshToken, and for revocation checks on use.
    public String generateRefreshToken(){
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Expiry to persist alongside a freshly issued refresh token.
    public LocalDateTime getRefreshTokenExpiry(){
        return LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS);
    }

    // One-way, deterministic hash for storing/looking up refresh tokens.
    // Deterministic (unlike BCrypt) so RefreshTokenRepository.findByTokenHash
    // can look it up directly; the token itself is 512 bits of SecureRandom
    // output, so it isn't brute-forceable even unsalted.
    public String hashRefreshToken(String rawToken){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Claims validateToken(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}