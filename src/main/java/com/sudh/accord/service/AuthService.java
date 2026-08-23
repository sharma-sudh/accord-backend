package com.sudh.accord.service;

import com.sudh.accord.dto.AuthResponse;
import com.sudh.accord.dto.GoogleAuthRequest;
import com.sudh.accord.dto.LoginRequest;
import com.sudh.accord.dto.RefreshRequest;
import com.sudh.accord.dto.RegisterRequest;
import com.sudh.accord.entity.RefreshToken;
import com.sudh.accord.entity.User;
import com.sudh.accord.repository.RefreshTokenRepository;
import com.sudh.accord.repository.UserRepository;
import com.sudh.accord.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new RuntimeException("Email already registered");

        User user = new User();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        return issueTokens(user, false);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.password(), user.getPasswordHash()))
            throw new RuntimeException("Invalid credentials");

        return issueTokens(user, false);
    }

    public AuthResponse googleAuth(GoogleAuthRequest req) {
        Map<String, Object> googleUser = verifyGoogleToken(req.idToken());

        String googleId = (String) googleUser.get("sub");
        String email    = (String) googleUser.get("email");
        String name     = (String) googleUser.get("name");

        // find by googleId first, then fall back to email (handles account linking)
        Optional<User> existing = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email));

        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            return newUser;
        });

        user.setGoogleId(googleId);   // link even if they registered via email earlier
        userRepository.save(user);

        return issueTokens(user, isNewUser);
    }

    // Rotates the refresh token: validates the presented one, revokes it, and
    // issues a brand-new access + refresh token pair. Rotation means a stolen
    // refresh token is only usable once before the legitimate client's next
    // refresh invalidates it (and this is also where reuse detection could
    // later hook in — a revoked token being presented again is a strong signal
    // the token was compromised).
    public AuthResponse refresh(RefreshRequest req) {
        String hash = jwtUtil.hashRefreshToken(req.refreshToken());

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (existing.isRevoked())
            throw new RuntimeException("Refresh token has been revoked");

        if (existing.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Refresh token has expired");

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return issueTokens(existing.getUser(), false);
    }

    // Issues an access token + a fresh refresh token, persisting the refresh
    // token's hash (never the raw value) so it can be looked up and revoked later.
    private AuthResponse issueTokens(User user, boolean isNewUser) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = jwtUtil.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken(
                null,
                jwtUtil.hashRefreshToken(rawRefreshToken),
                user,
                jwtUtil.getRefreshTokenExpiry(),
                false,
                null
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, user.getId(), user.getEmail(), isNewUser);
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            String aud = (String) body.get("aud");
            if (!googleClientId.equals(aud))
                throw new RuntimeException("Token audience mismatch");

            return body;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Invalid Google token");
        }
    }
}