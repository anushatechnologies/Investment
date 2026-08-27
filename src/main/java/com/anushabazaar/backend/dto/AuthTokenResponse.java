package com.anushabazaar.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthTokenResponse(
        String status,
        Boolean success,
        String message,
        String tokenType,
        String accessToken,
        String token,
        String refreshToken,
        Long expiresIn,
        String userId,
        String role,
        String nextStep,
        Map<String, Object> user
) {
    public static AuthTokenResponse of(
            String accessToken,
            String refreshToken,
            String userId,
            String role,
            Map<String, Object> user
    ) {
        return new AuthTokenResponse(
                "SUCCESS",
                true,
                "Authentication successful",
                "Bearer",
                accessToken,
                accessToken,
                refreshToken,
                86400L,
                userId,
                role,
                "DASHBOARD",
                user
        );
    }
}
