package com.sudh.accord.dto;

import java.util.UUID;

public record AuthResponse(String accessToken, String refreshToken, UUID userId, String email, boolean isNewUser) {}