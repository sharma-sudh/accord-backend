package com.sudh.accord.dto;

public record ResetPasswordRequest(String resetToken, String newPassword) {
}