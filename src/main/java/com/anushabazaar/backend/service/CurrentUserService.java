package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthenticated");
        }
        String principal = authentication.getName().trim();

        // 1. Try finding by email
        java.util.Optional<User> userOpt = userRepository.findByEmail(principal.toLowerCase());
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 2. Try finding by userId directly
        userOpt = userRepository.findById(principal);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 3. Try finding by mobile number (exact or 10-digit)
        String digitsOnly = principal.replaceAll("\\D", "");
        if (digitsOnly.length() >= 10) {
            String last10 = digitsOnly.substring(digitsOnly.length() - 10);
            userOpt = userRepository.findByMobileNumberEndingWith(last10);
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }

        userOpt = userRepository.findByMobileNumber(principal);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 4. Try from JWT claims details
        if (authentication.getDetails() instanceof io.jsonwebtoken.Claims claims) {
            String userId = claims.get("userId", String.class);
            if (userId != null && !userId.isBlank()) {
                userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }
        }

        throw new ResponseStatusException(UNAUTHORIZED, "User not found");
    }
}
