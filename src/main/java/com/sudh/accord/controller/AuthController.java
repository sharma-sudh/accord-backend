package com.sudh.accord.controller;

import com.sudh.accord.dto.AuthResponse;
import com.sudh.accord.dto.ForgotPasswordRequest;
import com.sudh.accord.dto.GoogleAuthRequest;
import com.sudh.accord.dto.LoginRequest;
import com.sudh.accord.dto.RefreshRequest;
import com.sudh.accord.dto.RegisterRequest;
import com.sudh.accord.dto.ResetPasswordRequest;
import com.sudh.accord.dto.VerifyOtpRequest;
import com.sudh.accord.dto.VerifyOtpResponse;
import com.sudh.accord.service.AuthService;
import com.sudh.accord.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest){
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest){
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@RequestBody GoogleAuthRequest googleAuthRequest){
        return ResponseEntity.ok(authService.googleAuth(googleAuthRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest refreshRequest){
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest){
        passwordResetService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest){
        return ResponseEntity.ok(passwordResetService.verifyOtp(verifyOtpRequest));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest){
        passwordResetService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok().build();
    }
}