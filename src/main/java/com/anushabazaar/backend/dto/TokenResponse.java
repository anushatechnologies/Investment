package com.anushabazaar.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String token,
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String userId,
        String role,
        String status,
        String message
) {
    public static TokenResponse of(String token, String refreshToken, String userId, String role) {
        return new TokenResponse(
                token,
                token,
                refreshToken,
                "Bearer",
                86400L,
                userId,
                role,
                "SUCCESS",
                "Sign completed successfully"
        );
    }
}
