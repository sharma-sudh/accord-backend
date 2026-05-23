package com.sudh.accord.service;

import com.sudh.accord.dto.AuthResponse;
import com.sudh.accord.dto.GoogleAuthRequest;
import com.sudh.accord.dto.LoginRequest;
import com.sudh.accord.dto.RegisterRequest;
import com.sudh.accord.entity.User;
import com.sudh.accord.repository.UserRepository;
import com.sudh.accord.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client-id}")
    private String googleClientId;


    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.password(), user.getPasswordHash()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    public AuthResponse googleAuth(GoogleAuthRequest req) {
        Map<String, Object> googleUser = verifyGoogleToken(req.idToken());

        String googleId = (String) googleUser.get("sub");
        String email    = (String) googleUser.get("email");
        String name     = (String) googleUser.get("name");

        // find by googleId first, then email (handles linking accounts)
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setName(name);
                            return newUser;
                        }));

        user.setGoogleId(googleId);   // link even if they registered via email earlier
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        // calls Google's tokeninfo endpoint — simple, no extra deps
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            // IMPORTANT: verify the token was meant for your app
            String aud = (String) body.get("aud");
            if (!googleClientId.equals(aud))
                throw new RuntimeException("Token audience mismatch");

            return body;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Invalid Google token");
        }
    }
}
